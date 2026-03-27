# ===================================================================
# Portfolio 应用停止和清理脚本
# ===================================================================

param(
    [Parameter(Mandatory=$false)]
    [switch]$RemoveImages,
    
    [Parameter(Mandatory=$false)]
    [switch]$Force,
    
    [Parameter(Mandatory=$false)]
    [string]$Profile = "local"
)

Write-Host "Portfolio Application Stop and Cleanup Script" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green

# 检查 Docker 是否运行
try {
    docker version 2>$null | Out-Null
    if ($LASTEXITCODE -ne 0) {
        Write-Host "[ERROR] Docker is not running or not installed" -ForegroundColor Red
        Write-Host "[INFO] Please start Docker and try again" -ForegroundColor Yellow
        exit 1
    }
} catch {
    Write-Host "[ERROR] Docker is not running or not installed" -ForegroundColor Red
    Write-Host "[INFO] Please start Docker and try again" -ForegroundColor Yellow
    exit 1
}

Write-Host "[START] Starting cleanup process..." -ForegroundColor Cyan
Write-Host "[INFO] Profile: $Profile" -ForegroundColor Cyan
Write-Host "[INFO] Remove Images: $RemoveImages" -ForegroundColor Cyan
Write-Host "[INFO] Force Cleanup: $Force" -ForegroundColor Cyan
Write-Host ""

# 1. 停止本地 Spring Boot 进程
Write-Host "[STOP] Checking for local Spring Boot processes..." -ForegroundColor Cyan
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
        Write-Host "[STOP] Stopping Java process (PID: $($process.Id))" -ForegroundColor Cyan
        $process.Kill()
        $stoppedProcesses += $process.Id
        Start-Sleep -Seconds 2
    } catch {
        $errorMsg = $_.Exception.Message
        Write-Host "[ERROR] Failed to stop process $($process.Id): $errorMsg" -ForegroundColor Red
    }
}

if ($stoppedProcesses.Count -gt 0) {
    $processList = $stoppedProcesses -join ", "
    Write-Host "[SUCCESS] Stopped Java processes: $processList" -ForegroundColor Green
} else {
    Write-Host "[INFO] No local Spring Boot processes found" -ForegroundColor Cyan
}

# 2. 停止和删除 Docker 容器
Write-Host ""
Write-Host "[STOP] Stopping and removing containers..." -ForegroundColor Cyan

$containerPatterns = @("portfolio-app", "portfolio-backend", "app", "mysql", "db", "prometheus", "grafana")
$stoppedContainers = @()
$removedContainers = @()

foreach ($pattern in $containerPatterns) {
    $containers = docker ps -a --filter "name=$pattern" --format "{{.Names}}" 2>$null
    if ($containers) {
        foreach ($container in $containers) {
            try {
                $isRunning = docker ps --filter "name=$container" --format "{{.Names}}" 2>$null
                if ($isRunning -contains $container) {
                    Write-Host "[STOP] Stopping container: $container" -ForegroundColor Cyan
                    docker stop $container 2>$null
                    $stoppedContainers += $container
                    Start-Sleep -Seconds 2
                }
                
                Write-Host "[REMOVE] Removing container: $container" -ForegroundColor Cyan
                docker rm $container 2>$null
                $removedContainers += $container
                Start-Sleep -Seconds 1
                
            } catch {
                $errorMsg = $_.Exception.Message
                Write-Host "[ERROR] Failed to stop/remove container: $container" -ForegroundColor Red
                Write-Host "[ERROR] $errorMsg" -ForegroundColor Red
            }
        }
    }
}

if ($stoppedContainers.Count -gt 0) {
    $containerList = $stoppedContainers -join ", "
    Write-Host "[SUCCESS] Stopped containers: $containerList" -ForegroundColor Green
}

if ($removedContainers.Count -gt 0) {
    $containerList = $removedContainers -join ", "
    Write-Host "[SUCCESS] Removed containers: $containerList" -ForegroundColor Green
}

if ($stoppedContainers.Count -eq 0 -and $removedContainers.Count -eq 0) {
    Write-Host "[INFO] No running containers found to stop" -ForegroundColor Cyan
}

# 3. 清理 Docker 网络
Write-Host ""
Write-Host "[CLEANUP] Cleaning up Docker networks..." -ForegroundColor Cyan

$networks = docker network ls --filter "name=portfolio" --format "{{.Name}}" 2>$null
$networks += docker network ls --filter "name=portfolio-network" --format "{{.Name}}" 2>$null

$removedNetworks = @()
foreach ($network in $networks) {
    try {
        Write-Host "[REMOVE] Removing network: $network" -ForegroundColor Cyan
        docker network rm $network 2>$null
        $removedNetworks += $network
    } catch {
        $errorMsg = $_.Exception.Message
        Write-Host "[ERROR] Failed to remove network ${network}: $errorMsg" -ForegroundColor Red
    }
}

if ($removedNetworks.Count -gt 0) {
    $networkList = $removedNetworks -join ", "
    Write-Host "[SUCCESS] Removed networks: $networkList" -ForegroundColor Green
} else {
    Write-Host "[INFO] No networks to remove" -ForegroundColor Cyan
}

# 4. 清理 Docker 镜像（可选）
Write-Host ""
if ($RemoveImages) {
    Write-Host "[CLEANUP] Cleaning up Docker images..." -ForegroundColor Cyan
    
    $imagePatterns = @("portfolio*", "spring*", "mysql*", "prometheus*", "grafana*")
    $removedImages = @()
    
    foreach ($pattern in $imagePatterns) {
        $images = docker images --filter "reference=$pattern" --format "{{.Repository}}:{{.Tag}}" 2>$null
        if ($images) {
            foreach ($image in $images) {
                try {
                    Write-Host "[REMOVE] Removing image: $image" -ForegroundColor Cyan
                    docker rmi $image 2>$null
                    $removedImages += $image
                } catch {
                    $errorMsg = $_.Exception.Message
                    Write-Host "[ERROR] Failed to remove image ${image}: $errorMsg" -ForegroundColor Red
                }
            }
        }
    }
    
    if ($removedImages.Count -gt 0) {
        $imageList = $removedImages -join ", "
        Write-Host "[SUCCESS] Removed images: $imageList" -ForegroundColor Green
    } else {
        Write-Host "[INFO] No images to remove" -ForegroundColor Cyan
    }
} else {
    Write-Host "[SKIP] Skipping image cleanup (use -RemoveImages flag)" -ForegroundColor Cyan
}

# 5. 清理 Docker 卷（可选）
Write-Host ""
if ($Force) {
    Write-Host "[CLEANUP] Cleaning up Docker volumes..." -ForegroundColor Yellow
    Write-Host "[WARN] This will delete all data in volumes!" -ForegroundColor Yellow
    
    $volumes = docker volume ls --filter "name=portfolio" --format "{{.Name}}" 2>$null
    $volumes += docker volume ls --filter "name=mysql" --format "{{.Name}}" 2>$null
    
    $removedVolumes = @()
    foreach ($volume in $volumes) {
        try {
            Write-Host "[REMOVE] Removing volume: $volume" -ForegroundColor Cyan
            docker volume rm $volume 2>$null
            $removedVolumes += $volume
        } catch {
            $errorMsg = $_.Exception.Message
            Write-Host "[ERROR] Failed to remove volume ${volume}: $errorMsg" -ForegroundColor Red
        }
    }
    
    if ($removedVolumes.Count -gt 0) {
        $volumeList = $removedVolumes -join ", "
        Write-Host "[SUCCESS] Removed volumes: $volumeList" -ForegroundColor Green
    } else {
        Write-Host "[INFO] No volumes to remove" -ForegroundColor Cyan
    }
} else {
    Write-Host "[SKIP] Skipping volume cleanup (use -Force flag)" -ForegroundColor Cyan
}

Write-Host ""
Write-Host "[SUCCESS] Cleanup completed successfully!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green

# 显示最终状态
Write-Host ""
Write-Host "[FINAL] Final status check:" -ForegroundColor Cyan
Write-Host "[CHECK] Docker containers:" -ForegroundColor Cyan
docker ps -a 2>$null | Select-Object -First 5

Write-Host ""
Write-Host "[CHECK] Port usage:" -ForegroundColor Cyan
$ports = @(8080, 3307, 9090, 3000)
foreach ($port in $ports) {
    try {
        $connection = New-Object System.Net.Sockets.TcpClient
        $connection.Connect("localhost", $port)
        $connection.Close()
        Write-Host "[WARN] Port ${port}: IN USE" -ForegroundColor Yellow
    } catch {
        Write-Host "[OK] Port ${port}: FREE" -ForegroundColor Green
    }
}

Write-Host ""
Write-Host "[MANAGE] Next steps:" -ForegroundColor Cyan
Write-Host "   - To restart: .\deploy.ps1 $Profile" -ForegroundColor White
Write-Host "   - To check logs: docker-compose logs" -ForegroundColor White
Write-Host "   - To check status: docker ps" -ForegroundColor White
