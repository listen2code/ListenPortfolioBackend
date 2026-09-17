# ===================================================================
# Portfolio 应用全栈停止与资源深度清理脚本 (Windows PowerShell)
# ===================================================================
# 使用方式：
#   .\docker_stop.ps1                 # 默认只停止本地 Java 进程与 Docker 容器、清理网络
#   .\docker_stop.ps1 -RemoveImages   # 额外删除构建的应用与中间缓存镜像
#   .\docker_stop.ps1 -Force          # 强制清理 Docker 数据卷（注意：会删除数据库持久化数据！）
#
# 设计目的：
#   在本地开发或 CI/CD 测试时，经常会遇到端口冲突（如 8080/3307/6379 占用）、
#   悬空容器未正常退出、本地 IDEA 启动的 Java 进程与 Docker 容器争抢端口等问题。
#   本脚本通过分层扫描、终止进程、下线容器、回收网络与可选清理镜像/卷，提供干净的重启环境。
# ===================================================================

param(
    # 是否同步删除相关 Docker 镜像（默认为 false）
    [Parameter(Mandatory=$false)]
    [switch]$RemoveImages,
    
    # 是否强制删除数据卷（含数据库数据，默认为 false，请谨慎使用）
    [Parameter(Mandatory=$false)]
    [switch]$Force,
    
    # 对应的 Docker Compose Profile（默认 local）
    [Parameter(Mandatory=$false)]
    [string]$Profile = "local"
)

Write-Host "Portfolio Application Stop and Cleanup Script" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green

# -------------------------------------------------------------------
# 检查 Docker 守护进程状态
# -------------------------------------------------------------------
try {
    docker version 2>$null | Out-Null
    if ($LASTEXITCODE -ne 0) {
        Write-Host "[ERROR] Docker 服务未运行或尚未安装！" -ForegroundColor Red
        Write-Host "[INFO] 请先启动 Docker Desktop 后重试。" -ForegroundColor Yellow
        exit 1
    }
} catch {
    Write-Host "[ERROR] 无法连接到 Docker 守护进程！" -ForegroundColor Red
    Write-Host "[INFO] 请确认 Docker Desktop 已启动。" -ForegroundColor Yellow
    exit 1
}

Write-Host "[START] 开始执行清理流程..." -ForegroundColor Cyan
Write-Host "[INFO] Profile 配置: $Profile" -ForegroundColor Cyan
Write-Host "[INFO] 清理镜像 (-RemoveImages): $RemoveImages" -ForegroundColor Cyan
Write-Host "[INFO] 强制清理卷 (-Force): $Force" -ForegroundColor Cyan
Write-Host ""

# -------------------------------------------------------------------
# 步骤 1：排查并停止本地运行的 Spring Boot Java 进程
# -------------------------------------------------------------------
# 说明：若开发者此前使用 IDEA 或命令行本地启动了 Spring Boot，会占用 8080 端口，
# 导致后续 Docker 启动 app 容器时出现 "bind: address already in use" 报错。
Write-Host "[STOP] 正在扫描宿主机本地运行的 Spring Boot Java 进程..." -ForegroundColor Cyan
$javaProcesses = Get-Process -Name java -ErrorAction SilentlyContinue | Where-Object { 
    $_.ProcessName -and 
    ($_.MainWindowTitle -like "*spring*" -or 
     $_.MainWindowTitle -like "*portfolio*" -or
     $_.CommandLine -like "*spring*" -or
     $_.CommandLine -like "*portfolio*")
}

$stoppedProcesses = @()
foreach ($process in $javaProcesses) {
    try {
        Write-Host "[STOP] 终止本地 Java 进程 (PID: $($process.Id))" -ForegroundColor Cyan
        $process.Kill()
        $stoppedProcesses += $process.Id
        Start-Sleep -Seconds 2
    } catch {
        $errorMsg = $_.Exception.Message
        Write-Host "[ERROR] 终止本地进程 $($process.Id) 失败: $errorMsg" -ForegroundColor Red
    }
}

if ($stoppedProcesses.Count -gt 0) {
    $processList = $stoppedProcesses -join ", "
    Write-Host "[SUCCESS] 已成功停止本地 Java 进程 PID: $processList" -ForegroundColor Green
} else {
    Write-Host "[INFO] 未发现本地残留的 Spring Boot 冲突进程" -ForegroundColor Cyan
}

# -------------------------------------------------------------------
# 步骤 2：停止并删除关联的 Docker 容器
# -------------------------------------------------------------------
# 说明：按应用名模式遍历相关容器，先优雅停止 (docker stop)，后销毁容器实例 (docker rm)
Write-Host ""
Write-Host "[STOP] 正在扫描并优雅停止已启动的 Docker 容器..." -ForegroundColor Cyan

$containerPatterns = @("portfolio-app", "portfolio-backend", "app", "mysql", "db", "redis", "prometheus", "grafana")
$stoppedContainers = @()
$removedContainers = @()

foreach ($pattern in $containerPatterns) {
    $containers = docker ps -a --filter "name=$pattern" --format "{{.Names}}" 2>$null
    if ($containers) {
        foreach ($container in $containers) {
            try {
                $isRunning = docker ps --filter "name=$container" --format "{{.Names}}" 2>$null
                if ($isRunning -contains $container) {
                    Write-Host "[STOP] 正在停止容器: $container" -ForegroundColor Cyan
                    docker stop $container 2>$null
                    $stoppedContainers += $container
                    Start-Sleep -Seconds 2
                }
                
                Write-Host "[REMOVE] 正在删除容器: $container" -ForegroundColor Cyan
                docker rm $container 2>$null
                $removedContainers += $container
                Start-Sleep -Seconds 1
                
            } catch {
                $errorMsg = $_.Exception.Message
                Write-Host "[ERROR] 停止或删除容器失败: $container" -ForegroundColor Red
                Write-Host "[ERROR] $errorMsg" -ForegroundColor Red
            }
        }
    }
}

if ($stoppedContainers.Count -gt 0) {
    $containerList = $stoppedContainers -join ", "
    Write-Host "[SUCCESS] 已停止容器列表: $containerList" -ForegroundColor Green
}

if ($removedContainers.Count -gt 0) {
    $containerList = $removedContainers -join ", "
    Write-Host "[SUCCESS] 已删除容器列表: $containerList" -ForegroundColor Green
}

if ($stoppedContainers.Count -eq 0 -and $removedContainers.Count -eq 0) {
    Write-Host "[INFO] 未发现正在运行或残留的目标容器" -ForegroundColor Cyan
}

# 3. 清理 Docker 网络
# -------------------------------------------------------------------
# 步骤 3：清理孤立的 Docker 自定义网络
# -------------------------------------------------------------------
# 说明：若旧网络仍残留，重新启动时可能会提示 "network already exists" 或路由冲突
Write-Host ""
Write-Host "[CLEANUP] 正在清理 Portfolio 专属 Docker 桥接网络..." -ForegroundColor Cyan

$networks = docker network ls --filter "name=portfolio" --format "{{.Name}}" 2>$null
$networks += docker network ls --filter "name=portfolio-network" --format "{{.Name}}" 2>$null

$removedNetworks = @()
foreach ($network in $networks) {
    try {
        Write-Host "[REMOVE] 正在删除自定义网络: $network" -ForegroundColor Cyan
        docker network rm $network 2>$null
        $removedNetworks += $network
    } catch {
        $errorMsg = $_.Exception.Message
        Write-Host "[ERROR] 删除网络失败 ${network}: $errorMsg" -ForegroundColor Red
    }
}

if ($removedNetworks.Count -gt 0) {
    $networkList = $removedNetworks -join ", "
    Write-Host "[SUCCESS] 已清理网络列表: $networkList" -ForegroundColor Green
} else {
    Write-Host "[INFO] 未检测到残留的待清理网络" -ForegroundColor Cyan
}

# -------------------------------------------------------------------
# 步骤 4：清理关联 Docker 镜像（可选开关：-RemoveImages）
# -------------------------------------------------------------------
# 说明：如果不传递 -RemoveImages 开关，默认跳过此步以保留镜像层缓存，加快下次启动速度
Write-Host ""
if ($RemoveImages) {
    Write-Host "[CLEANUP] 正在清理相关 Docker 镜像..." -ForegroundColor Cyan
    
    $imagePatterns = @("portfolio*", "spring*", "mysql*", "redis*", "prometheus*", "grafana*")
    $removedImages = @()
    
    foreach ($pattern in $imagePatterns) {
        $images = docker images --filter "reference=$pattern" --format "{{.Repository}}:{{.Tag}}" 2>$null
        if ($images) {
            foreach ($image in $images) {
                try {
                    Write-Host "[REMOVE] 正在移除镜像: $image" -ForegroundColor Cyan
                    docker rmi $image 2>$null
                    $removedImages += $image
                } catch {
                    $errorMsg = $_.Exception.Message
                    Write-Host "[ERROR] 移除镜像失败 ${image}: $errorMsg" -ForegroundColor Red
                }
            }
        }
    }
    
    if ($removedImages.Count -gt 0) {
        $imageList = $removedImages -join ", "
        Write-Host "[SUCCESS] 已移除镜像列表: $imageList" -ForegroundColor Green
    } else {
        Write-Host "[INFO] 未匹配到需要删除的关联镜像" -ForegroundColor Cyan
    }
} else {
    Write-Host "[SKIP] 已跳过镜像清理（如需清空镜像，请添加 -RemoveImages 参数执行）" -ForegroundColor Cyan
}

# -------------------------------------------------------------------
# 步骤 5：清理持久化数据卷（危险开关：-Force）
# -------------------------------------------------------------------
# 说明：数据卷内保存着 MySQL 表数据、Redis 缓存与监控时序数据。
# 仅在需要彻底重置测试环境、或者排除持久化数据损坏故障时才使用 -Force 参数。
Write-Host ""
if ($Force) {
    Write-Host "[CLEANUP] 正在执行数据卷强制清理..." -ForegroundColor Yellow
    Write-Host "[WARN] 警告：这将彻底清空 MySQL 数据库与 Redis 的全部持久化数据！" -ForegroundColor Yellow
    
    $volumes = docker volume ls --filter "name=portfolio" --format "{{.Name}}" 2>$null
    $volumes += docker volume ls --filter "name=mysql" --format "{{.Name}}" 2>$null
    $volumes += docker volume ls --filter "name=redis" --format "{{.Name}}" 2>$null
    
    $removedVolumes = @()
    foreach ($volume in $volumes) {
        try {
            Write-Host "[REMOVE] 正在删除持久化卷: $volume" -ForegroundColor Cyan
            docker volume rm $volume 2>$null
            $removedVolumes += $volume
        } catch {
            $errorMsg = $_.Exception.Message
            Write-Host "[ERROR] 删除持久化卷失败 ${volume}: $errorMsg" -ForegroundColor Red
        }
    }
    
    if ($removedVolumes.Count -gt 0) {
        $volumeList = $removedVolumes -join ", "
        Write-Host "[SUCCESS] 已彻底删除持久化卷: $volumeList" -ForegroundColor Green
    } else {
        Write-Host "[INFO] 未检测到残留的数据卷" -ForegroundColor Cyan
    }
} else {
    Write-Host "[SKIP] 已保留持久化数据卷（如需格式化清空数据，请添加 -Force 参数执行）" -ForegroundColor Cyan
}

Write-Host ""
Write-Host "[SUCCESS] 全栈资源清理流程顺利完成！" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green

# -------------------------------------------------------------------
# 步骤 6：最终端口占用与就绪状态扫描
# -------------------------------------------------------------------
# 说明：通过 Socket 连接测试 8080(App)、3307(MySQL)、6379(Redis)、9090(Prometheus)、3000(Grafana)
# 确保所有目标端口已彻底释放，防止下次启动报错
Write-Host ""
Write-Host "[FINAL] 执行最终环境与端口状态核对:" -ForegroundColor Cyan
Write-Host "[CHECK] 当前前台残留容器:" -ForegroundColor Cyan
docker ps -a 2>$null | Select-Object -First 5

Write-Host ""
Write-Host "[CHECK] 核心端口占用情况校验:" -ForegroundColor Cyan
$ports = @(8080, 3307, 6379, 9090, 3000)
foreach ($port in $ports) {
    try {
        $connection = New-Object System.Net.Sockets.TcpClient
        $connection.Connect("localhost", $port)
        $connection.Close()
        Write-Host "[WARN] 端口 ${port}: 仍被占用中 (IN USE)" -ForegroundColor Yellow
    } catch {
        Write-Host "[OK] 端口 ${port}: 已完全空闲 (FREE)" -ForegroundColor Green
    }
}

Write-Host ""
Write-Host "[MANAGE] 后续操作引导:" -ForegroundColor Cyan
Write-Host "   - 重新部署启动: .\docker_deploy.ps1" -ForegroundColor White
Write-Host "   - 查看服务日志: docker-compose logs" -ForegroundColor White
Write-Host "   - 查看容器列表: docker ps" -ForegroundColor White
