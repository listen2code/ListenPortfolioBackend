# ===================================================================
# Portfolio Docker 全栈一键部署自动化脚本 (Windows PowerShell)
# ===================================================================
# 使用方式：.\docker_deploy.ps1
#
# 设计思路与执行阶段：
#   1. 环境配置前置检查：探测是否存在 .env 文件，若无则使用 docker-compose.yml 中的默认回退值
#   2. 编译打包 (Build Artifact)：调用 Gradle Wrapper 执行 bootWar 打包出标准的 production war 包
#   3. 容器编排构建与启动 (Docker Compose)：
#      - 携带 --profile local 参数，按需拉起 local profile 下的全量微服务群
#      - 依赖关系严格按健康状态级联：Redis -> MySQL (健康探针通过) -> Spring Boot App -> Prometheus -> Grafana
#   4. 轮询健康探针 (Polling Health Check)：
#      - 避免因服务尚未完成数据库迁移（Flyway）或 Spring 容器初始化导致用户请求 502
#      - 使用循环轮询 http://localhost:8080/actuator/health，最多重试 5 次，探测 status 是否为 UP
#      - 同步探测 Prometheus (/-/healthy) 和 Grafana (/api/health)
#   5. 状态汇报与端点信息展示：打印所有可访问的端点 URL、Swagger 文档地址与常用排错命令
# ===================================================================

Write-Host "========================================" -ForegroundColor Green
Write-Host "Portfolio Docker Deployment" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green

# -------------------------------------------------------------------
# 阶段 1：检查本地环境变量文件 (.env)
# -------------------------------------------------------------------
# 说明：.env 文件用于覆盖 docker-compose 中的敏感密钥（如 DB_PASSWORD, JWT_SECRET, MAIL_PASSWORD）
if (-not (Test-Path ".env")) {
    Write-Host "[INFO] 未检测到 .env 配置文件，将使用 docker-compose.yml 中的默认开发环境变量" -ForegroundColor Yellow
    Write-Host "[INFO] 如需自定义密钥配置，请复制 .env.example 为 .env 并根据需要修改" -ForegroundColor Yellow
}

# -------------------------------------------------------------------
# 阶段 2：通过 Gradle Wrapper 打包应用产物 (bootWar)
# -------------------------------------------------------------------
# 说明：先在宿主机本地编译打包出 target/portfolio-0.0.1-SNAPSHOT.war
# 优点：避免在 Docker 镜像构建中下载几百兆 Gradle 依赖，大幅缩短 Docker 构建时间和减小临时层
Write-Host "[BUILD] 正在执行 Gradle 打包 (bootWar)..." -ForegroundColor Green
& .\gradlew.bat bootWar
if ($LASTEXITCODE -ne 0) {
    Write-Host "[ERROR] Gradle 打包失败，部署流程已终止！" -ForegroundColor Red
    exit 1
}

# -------------------------------------------------------------------
# 阶段 3：启动 Docker 全栈编排服务群
# -------------------------------------------------------------------
# 参数解释：
#   --profile local: 激活 docker-compose 中定义为 profiles: [local] 的服务
#   up -d: 后台守护进程模式运行
#   --build: 若 Dockerfile 或依赖发生变更，强制重新构建应用镜像
Write-Host "[START] 正在构建并拉起 Docker 服务群 (App + DB + Redis + 监控全栈)..." -ForegroundColor Green
& docker-compose --profile local up -d --build

# -------------------------------------------------------------------
# 阶段 4：等待服务初始化与健康探针轮询
# -------------------------------------------------------------------
# 说明：MySQL 数据库初始化、Redis 预热、Flyway 数据表自动迁移通常需要 15~30 秒
Write-Host "[WAIT] 等待各容器服务初始化启动中 (初次等待 30 秒)..." -ForegroundColor Yellow
Start-Sleep -Seconds 30

Write-Host "[HEALTH] 开始检测各微服务健康探针..." -ForegroundColor Cyan
$maxRetries = 5
$retryCount = 0
$appHealthy = $false

# 轮询 Spring Boot Actuator 健康端点
while ($retryCount -lt $maxRetries -and -not $appHealthy) {
    try {
        $response = Invoke-WebRequest -Uri "http://localhost:8080/actuator/health" -UseBasicParsing -TimeoutSec 10
        $content = $response.Content
        if ($content -is [byte[]]) {
            $content = [System.Text.Encoding]::UTF8.GetString($content)
        }
        $healthData = $content | ConvertFrom-Json
        # 当 Spring 启动完成且各组件（DiskSpace, DB, Redis）状态正常时返回 UP
        if ($healthData.status -eq "UP") {
            Write-Host "[OK] Spring Boot 主应用: 状态正常 (Healthy / UP)" -ForegroundColor Green
            $appHealthy = $true
        } else {
            Write-Host "[WARN] 应用就绪状态为: $($healthData.status) (重试探测中 $($retryCount + 1)/$maxRetries)..." -ForegroundColor Yellow
            $retryCount++
            if ($retryCount -lt $maxRetries) { Start-Sleep -Seconds 10 }
        }
    } catch {
        Write-Host "[ERROR] 应用健康探测失败或超时 (重试探测中 $($retryCount + 1)/$maxRetries)..." -ForegroundColor Red
        $retryCount++
        if ($retryCount -lt $maxRetries) { Start-Sleep -Seconds 10 }
    }
}

if (-not $appHealthy) {
    Write-Host "[ERROR] 应用在 $maxRetries 次健康探测后仍未就绪！" -ForegroundColor Red
    Write-Host "[INFO] 请执行查看容器启动日志排查: docker-compose logs app" -ForegroundColor Yellow
}

# 探测 Prometheus 状态
try {
    Invoke-WebRequest -Uri "http://localhost:9090/-/healthy" -UseBasicParsing | Out-Null
    Write-Host "[OK] Prometheus 监控服务: 状态正常 (Healthy)" -ForegroundColor Green
} catch {
    Write-Host "[WARN] Prometheus 尚未完全就绪 (查看日志: docker-compose logs prometheus)" -ForegroundColor Yellow
}

# 探测 Grafana 状态
try {
    Invoke-WebRequest -Uri "http://localhost:3000/api/health" -UseBasicParsing | Out-Null
    Write-Host "[OK] Grafana 可视化看板: 状态正常 (Healthy)" -ForegroundColor Green
} catch {
    Write-Host "[WARN] Grafana 尚未完全就绪 (查看日志: docker-compose logs grafana)" -ForegroundColor Yellow
}

# -------------------------------------------------------------------
# 阶段 5：打印接入端点与运维指令
# -------------------------------------------------------------------
Write-Host ""
Write-Host "部署流程执行完毕!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
Write-Host "[ACCESS] 服务访问端点清单:" -ForegroundColor Cyan
Write-Host "   Application API: http://localhost:8080" -ForegroundColor White
Write-Host "   Swagger UI 文档: http://localhost:8080/swagger-ui.html" -ForegroundColor White
Write-Host "   MySQL Database:  localhost:3307 (宿主机映射) / db:3306 (Docker内部网络)" -ForegroundColor White
Write-Host "   Redis Cache:     localhost:6379 (宿主机映射) / redis:6379 (Docker内部网络)" -ForegroundColor White
Write-Host "   Prometheus:      http://localhost:9090" -ForegroundColor White
Write-Host "   Grafana 仪表盘:  http://localhost:3000 (默认账号密码: admin / admin123)" -ForegroundColor White
Write-Host ""
Write-Host "[MANAGE] 常用管理指令速查:" -ForegroundColor Cyan
Write-Host "   停止全栈容器:   docker-compose --profile local down" -ForegroundColor White
Write-Host "   重启全栈服务:   docker-compose --profile local restart" -ForegroundColor White
Write-Host "   查看运行状态:   docker-compose ps" -ForegroundColor White
Write-Host "   实时跟踪日志:   docker-compose logs -f app" -ForegroundColor White
