# ===================================================================
# Portfolio Docker 全栈部署脚本
# ===================================================================
# 使用方式：.\docker_deploy.ps1
# 功能：Maven 打包 → Docker 构建 → 启动全栈 → 健康检查
# 配置来源：
#   - application.properties（基础配置 + 本地开发默认值）
#   - application-docker.properties（Docker hostname 覆盖）
#   - .env（可选，覆盖密钥；参考 .env.example）
# ===================================================================

Write-Host "========================================" -ForegroundColor Green
Write-Host "Portfolio Docker Deployment" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green

# 检查 .env 文件
if (-not (Test-Path ".env")) {
    Write-Host "[INFO] No .env file found, using default values from docker-compose.yml" -ForegroundColor Yellow
    Write-Host "[INFO] To customize secrets, copy .env.example to .env and edit it" -ForegroundColor Yellow
}

# 1. Maven 打包
Write-Host "[BUILD] Packaging application WAR..." -ForegroundColor Green
& .\mvnw.cmd clean package -DskipTests
if ($LASTEXITCODE -ne 0) {
    Write-Host "[ERROR] Maven package failed, deployment aborted" -ForegroundColor Red
    exit 1
}

# 2. 启动 Docker 全栈
Write-Host "[START] Starting Docker stack (app + db + redis + monitoring)..." -ForegroundColor Green
& docker-compose --profile local up -d --build

# 3. 等待服务启动
Write-Host "[WAIT] Waiting for services to start..." -ForegroundColor Yellow
Start-Sleep -Seconds 30

# 4. 健康检查
Write-Host "[HEALTH] Checking service health..." -ForegroundColor Cyan
$maxRetries = 5
$retryCount = 0
$appHealthy = $false

while ($retryCount -lt $maxRetries -and -not $appHealthy) {
    try {
        $response = Invoke-WebRequest -Uri "http://localhost:8080/actuator/health" -UseBasicParsing -TimeoutSec 10
        $content = $response.Content
        if ($content -is [byte[]]) {
            $content = [System.Text.Encoding]::UTF8.GetString($content)
        }
        $healthData = $content | ConvertFrom-Json
        if ($healthData.status -eq "UP") {
            Write-Host "[OK] Application: Healthy" -ForegroundColor Green
            $appHealthy = $true
        } else {
            Write-Host "[WARN] Application status: $($healthData.status) (attempt $($retryCount + 1)/$maxRetries)" -ForegroundColor Yellow
            $retryCount++
            if ($retryCount -lt $maxRetries) { Start-Sleep -Seconds 10 }
        }
    } catch {
        Write-Host "[ERROR] Application health check failed (attempt $($retryCount + 1)/$maxRetries)" -ForegroundColor Red
        $retryCount++
        if ($retryCount -lt $maxRetries) { Start-Sleep -Seconds 10 }
    }
}

if (-not $appHealthy) {
    Write-Host "[ERROR] Application: Unhealthy after $maxRetries attempts" -ForegroundColor Red
    Write-Host "[INFO] Check logs: docker-compose logs app" -ForegroundColor Yellow
}

try {
    Invoke-WebRequest -Uri "http://localhost:9090/-/healthy" -UseBasicParsing | Out-Null
    Write-Host "[OK] Prometheus: Healthy" -ForegroundColor Green
} catch {
    Write-Host "[WARN] Prometheus: Unhealthy (check: docker-compose logs prometheus)" -ForegroundColor Yellow
}

try {
    Invoke-WebRequest -Uri "http://localhost:3000/api/health" -UseBasicParsing | Out-Null
    Write-Host "[OK] Grafana: Healthy" -ForegroundColor Green
} catch {
    Write-Host "[WARN] Grafana: Unhealthy (check: docker-compose logs grafana)" -ForegroundColor Yellow
}

# 5. 显示访问信息
Write-Host ""
Write-Host "Deployment completed!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
Write-Host "[ACCESS] Service URLs:" -ForegroundColor Cyan
Write-Host "   Application:   http://localhost:8080" -ForegroundColor White
Write-Host "   Swagger UI:    http://localhost:8080/swagger-ui.html" -ForegroundColor White
Write-Host "   Database:      localhost:3307 (host) / db:3306 (internal)" -ForegroundColor White
Write-Host "   Redis:         localhost:6379" -ForegroundColor White
Write-Host "   Prometheus:    http://localhost:9090" -ForegroundColor White
Write-Host "   Grafana:       http://localhost:3000 (admin/admin123)" -ForegroundColor White
Write-Host ""
Write-Host "[MANAGE] Commands:" -ForegroundColor Cyan
Write-Host "   Stop:    docker-compose --profile local down" -ForegroundColor White
Write-Host "   Restart: docker-compose --profile local restart" -ForegroundColor White
Write-Host "   Status:  docker-compose ps" -ForegroundColor White
Write-Host "   Logs:    docker-compose logs -f app" -ForegroundColor White
