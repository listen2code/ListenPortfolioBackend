# ===================================================================
# Portfolio 应用统一部署脚本
# ===================================================================
# 说明：集成了环境配置生成和部署功能
# 支持：local, test, staging, prod
# 功能：生成 .env 文件 + 启动相应服务
# ===================================================================

param(
    [Parameter(Mandatory=$false)]
    [ValidateSet("local", "test", "staging", "prod")]
    [string]$DeployType = "local"
)

# ===================================================================
# 环境配置生成函数
# ===================================================================
function New-Environment {
    param([string]$Environment)
    
    $ConfigFile = "env-config.json"
    
    Write-Host "[CONFIG] Generating environment for: $Environment" -ForegroundColor Cyan
    
    if (-not (Test-Path $ConfigFile)) {
        Write-Host "[ERROR] Configuration file $ConfigFile not found" -ForegroundColor Red
        exit 1
    }
    
    try {
        $Config = Get-Content $ConfigFile -Raw | ConvertFrom-Json
    } catch {
        Write-Host "[ERROR] Failed to parse $ConfigFile" -ForegroundColor Red
        exit 1
    }
    
    # 根据环境生成配置
    switch ($Environment) {
        "local" {
            Write-Host "[GENERATE] Creating .env file for local development" -ForegroundColor Green
            
            $DatabaseUrl = $Config.database.local.url
            $DatabaseUsername = $Config.database.local.username
            $DatabasePassword = $Config.database.local.password
            $JvmOptions = $Config.jvm.local
            $AwsRegion = $Config.aws_region
            $EnvName = "local"
            $AppPort = $Config.ports.app
            $PrometheusPort = $Config.ports.prometheus
            $PrometheusHost = $Config.prometheus.host
            $PrometheusTargetHost = $Config.prometheus.target_host
            $PrometheusScrapeInterval = $Config.prometheus.scrape_interval
            $PrometheusScrapeTimeout = $Config.prometheus.scrape_timeout
            $PrometheusGlobalInterval = $Config.prometheus.global_interval
            $PrometheusEvalInterval = $Config.prometheus.eval_interval
            $GrafanaPort = $Config.ports.grafana
            $GrafanaUser = $Config.grafana.user
            $GrafanaPassword = $Config.grafana.password
            $GrafanaDomain = $Config.grafana.local.domain
            $GrafanaRootUrl = $Config.grafana.local.root_url
            $GrafanaFolder = $Config.grafana.folder
            $GrafanaUpdateInterval = $Config.grafana.update_interval
            $GrafanaDashboardsPath = $Config.grafana.dashboards_path
            $PrometheusRetention = $Config.prometheus.retention
            $RedisPort = $Config.ports.redis
        }
        
        "test" {
            Write-Host "[GENERATE] Creating .env file for test deployment" -ForegroundColor Green
            
            $DatabaseUrl = $Config.database.test.url
            $DatabaseUsername = $Config.database.test.username
            $DatabasePassword = $Config.database.test.password
            $JvmOptions = $Config.jvm.test
            $AwsRegion = $Config.aws_region
            $EnvName = "test"
            $AppPort = $Config.ports.app
            $PrometheusPort = $Config.ports.prometheus
            $PrometheusHost = $Config.prometheus.host
            $PrometheusTargetHost = $Config.prometheus.target_host
            $PrometheusScrapeInterval = $Config.prometheus.scrape_interval
            $PrometheusScrapeTimeout = $Config.prometheus.scrape_timeout
            $PrometheusGlobalInterval = $Config.prometheus.global_interval
            $PrometheusEvalInterval = $Config.prometheus.eval_interval
            $GrafanaPort = $Config.ports.grafana
            $GrafanaUser = $Config.grafana.user
            $GrafanaPassword = $Config.grafana.password
            $GrafanaDomain = $Config.grafana.test.domain
            $GrafanaRootUrl = $Config.grafana.test.root_url
            $GrafanaFolder = $Config.grafana.folder
            $GrafanaUpdateInterval = $Config.grafana.update_interval
            $GrafanaDashboardsPath = $Config.grafana.dashboards_path
            $PrometheusRetention = $Config.prometheus.retention
            $RedisPort = $Config.ports.redis
        }
        
        "staging" {
            Write-Host "[GENERATE] Creating .env file for staging deployment" -ForegroundColor Green
            
            $DatabaseUrl = $Config.database.staging.url
            $DatabaseUsername = $Config.database.staging.username
            $DatabasePassword = $Config.database.staging.password
            $JvmOptions = $Config.jvm.staging
            $AwsRegion = $Config.aws_region
            $EnvName = "staging"
            $AppPort = $Config.ports.app
            $PrometheusPort = $Config.ports.prometheus
            $PrometheusHost = $Config.prometheus.host
            $PrometheusTargetHost = $Config.prometheus.target_host
            $PrometheusScrapeInterval = $Config.prometheus.scrape_interval
            $PrometheusScrapeTimeout = $Config.prometheus.scrape_timeout
            $PrometheusGlobalInterval = $Config.prometheus.global_interval
            $PrometheusEvalInterval = $Config.prometheus.eval_interval
            $GrafanaPort = $Config.ports.grafana
            $GrafanaUser = $Config.grafana.user
            $GrafanaPassword = $Config.grafana.password
            $GrafanaDomain = $Config.grafana.staging.domain
            $GrafanaRootUrl = $Config.grafana.staging.root_url
            $GrafanaFolder = $Config.grafana.folder
            $GrafanaUpdateInterval = $Config.grafana.update_interval
            $GrafanaDashboardsPath = $Config.grafana.dashboards_path
            $PrometheusRetention = $Config.prometheus.retention
            $RedisPort = $Config.ports.redis
        }
        
        "prod" {
            Write-Host "[GENERATE] Creating .env file for production deployment" -ForegroundColor Green
            
            $DatabaseUrl = $Config.database.prod.url
            $DatabaseUsername = $Config.database.prod.username
            $DatabasePassword = $Config.database.prod.password
            $JvmOptions = $Config.jvm.prod
            $AwsRegion = $Config.aws_region
            $EnvName = "production"
            $AppPort = $Config.ports.app
            $PrometheusPort = $Config.ports.prometheus
            $PrometheusHost = $Config.prometheus.host
            $PrometheusTargetHost = $Config.prometheus.target_host
            $PrometheusScrapeInterval = $Config.prometheus.scrape_interval
            $PrometheusScrapeTimeout = $Config.prometheus.scrape_timeout
            $PrometheusGlobalInterval = $Config.prometheus.global_interval
            $PrometheusEvalInterval = $Config.prometheus.eval_interval
            $GrafanaPort = $Config.ports.grafana
            $GrafanaUser = $Config.grafana.user
            $GrafanaPassword = $Config.grafana.password
            $GrafanaDomain = $Config.grafana.prod.domain
            $GrafanaRootUrl = $Config.grafana.prod.root_url
            $GrafanaFolder = $Config.grafana.folder
            $GrafanaUpdateInterval = $Config.grafana.update_interval
            $GrafanaDashboardsPath = $Config.grafana.dashboards_path
            $PrometheusRetention = $Config.prometheus.retention
            $RedisPort = $Config.ports.redis
        }
        
        default {
            Write-Host "[ERROR] Unsupported environment: $Environment" -ForegroundColor Red
            Write-Host "[INFO] Supported environments: local, test, staging, prod" -ForegroundColor Yellow
            exit 1
        }
    }
    
    # 生成 .env 文件内容
    $EnvContent = @"
# Generated from env-config.json for $Environment environment
DATABASE_URL=$DatabaseUrl
DATABASE_USERNAME=$DatabaseUsername
DATABASE_PASSWORD=$DatabasePassword
JAVA_OPTS=$JvmOptions
AWS_REGION=$AwsRegion
ENVIRONMENT=$EnvName
APP_PORT=$AppPort
PROMETHEUS_PORT=$PrometheusPort
PROMETHEUS_HOST=$PrometheusHost
PROMETHEUS_TARGET_HOST=$PrometheusTargetHost
PROMETHEUS_SCRAPE_INTERVAL=$PrometheusScrapeInterval
PROMETHEUS_SCRAPE_TIMEOUT=$PrometheusScrapeTimeout
PROMETHEUS_GLOBAL_INTERVAL=$PrometheusGlobalInterval
PROMETHEUS_EVAL_INTERVAL=$PrometheusEvalInterval
GRAFANA_PORT=$GrafanaPort
GRAFANA_USER=$GrafanaUser
GRAFANA_PASSWORD=$GrafanaPassword
GRAFANA_DOMAIN=$GrafanaDomain
GRAFANA_ROOT_URL=$GrafanaRootUrl
GRAFANA_FOLDER=$GrafanaFolder
GRAFANA_UPDATE_INTERVAL=$GrafanaUpdateInterval
GRAFANA_DASHBOARDS_PATH=$GrafanaDashboardsPath
PROMETHEUS_RETENTION=$PrometheusRetention
REDIS_PORT=$RedisPort
"@
    
    # 写入 .env 文件
    Write-Host "[WRITE] Writing to .env file" -ForegroundColor Yellow
    $EnvContent | Out-File -FilePath ".env" -Encoding UTF8
    
    Write-Host "[SUCCESS] .env file generated successfully!" -ForegroundColor Green
    Write-Host ""
    Write-Host "[CONFIG] Generated configuration:" -ForegroundColor Cyan
    Write-Host "   Environment: $EnvName" -ForegroundColor White
    Write-Host "   AWS Region: $AwsRegion" -ForegroundColor White
    Write-Host "   Database: $DatabaseUsername@$($DatabaseUrl.Split('?')[0])..." -ForegroundColor White
    Write-Host "   App Port: $AppPort" -ForegroundColor White
    Write-Host "   Redis Port: $RedisPort" -ForegroundColor White
    Write-Host "   Prometheus Host: $PrometheusHost" -ForegroundColor White
    Write-Host "   Prometheus Target Host: $PrometheusTargetHost" -ForegroundColor White
}

# ===================================================================
# 主部署逻辑
# ===================================================================

Write-Host "Starting deployment for: $DeployType" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green

# 生成环境配置
New-Environment -Environment $DeployType

# 根据部署类型执行相应逻辑
if ($DeployType -eq "local") {
    Write-Host "Local deployment - Starting complete Docker stack" -ForegroundColor Cyan
    
    # 启动完整 Docker 栈
    Write-Host "[START] Starting local Docker stack..." -ForegroundColor Green
    & docker-compose --profile local up -d
    
    # 等待服务启动
    Write-Host "[WAIT] Waiting for services to start..." -ForegroundColor Yellow
    Start-Sleep -Seconds 30
    
    # 健康检查
    Write-Host "[HEALTH] Checking service health..." -ForegroundColor Cyan
    $maxRetries = 5
    $retryCount = 0
    $appHealthy = $false
    
    while ($retryCount -lt $maxRetries -and -not $appHealthy) {
        try {
            $response = Invoke-WebRequest -Uri "http://localhost:8080/actuator/health" -UseBasicParsing -TimeoutSec 10
            $content = $response.Content
            # 处理可能的字节数组响应
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
                if ($retryCount -lt $maxRetries) {
                    Start-Sleep -Seconds 10
                }
            }
        } catch {
            Write-Host "[ERROR] Application health check failed (attempt $($retryCount + 1)/$maxRetries)" -ForegroundColor Red
            $retryCount++
            if ($retryCount -lt $maxRetries) {
                Start-Sleep -Seconds 10
            }
        }
    }
    
    if (-not $appHealthy) {
        Write-Host "[ERROR] Application: Unhealthy after $maxRetries attempts" -ForegroundColor Red
        Write-Host "[INFO] Check application logs: docker-compose logs app" -ForegroundColor Yellow
    }
    
    # 监控服务健康检查
    try {
        Invoke-WebRequest -Uri "http://localhost:9090/-/healthy" -UseBasicParsing | Out-Null
        Write-Host "[OK] Prometheus: Healthy" -ForegroundColor Green
    } catch {
        Write-Host "[ERROR] Prometheus: Unhealthy" -ForegroundColor Red
        Write-Host "[INFO] Check Prometheus logs: docker-compose logs prometheus" -ForegroundColor Yellow
    }
    
    try {
        Invoke-WebRequest -Uri "http://localhost:3000/api/health" -UseBasicParsing | Out-Null
        Write-Host "[OK] Grafana: Healthy" -ForegroundColor Green
    } catch {
        Write-Host "[ERROR] Grafana: Unhealthy" -ForegroundColor Red
        Write-Host "[INFO] Check Grafana logs: docker-compose logs grafana" -ForegroundColor Yellow
    }
}
elseif ($DeployType -eq "test") {
    Write-Host "Test deployment" -ForegroundColor Cyan
    Write-Host "[INFO] Test environment configured" -ForegroundColor Yellow
    Write-Host "[INFO] Please deploy manually or use your CI/CD pipeline" -ForegroundColor Cyan
}
elseif ($DeployType -eq "staging") {
    Write-Host "Staging deployment" -ForegroundColor Cyan
    Write-Host "[INFO] Staging environment configured" -ForegroundColor Yellow
    Write-Host "[INFO] Please deploy manually or use your CI/CD pipeline" -ForegroundColor Cyan
}
elseif ($DeployType -eq "prod") {
    Write-Host "Production deployment" -ForegroundColor Cyan
    Write-Host "[INFO] Production environment configured" -ForegroundColor Yellow
    Write-Host "[INFO] Please deploy manually or use your CI/CD pipeline" -ForegroundColor Cyan
}
else {
    Write-Host "Unknown deployment type: $DeployType" -ForegroundColor Red
    Write-Host "Supported: local, test, staging, prod" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "Deployment completed successfully!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green

# 显示访问信息
Write-Host "[ACCESS] Service URLs:" -ForegroundColor Cyan
if ($DeployType -eq "local") {
    Write-Host "   Application:   http://localhost:8080 (Docker)" -ForegroundColor White
    Write-Host "   Database:      localhost:3307" -ForegroundColor White
    Write-Host "   Redis:         localhost:6379" -ForegroundColor White
} elseif ($DeployType -eq "test") {
    Write-Host "   Application:   http://localhost:8080 (test)" -ForegroundColor White
    Write-Host "   Database:      localhost:3307" -ForegroundColor White
    Write-Host "   Redis:         localhost:6379" -ForegroundColor White
} elseif ($DeployType -eq "staging") {
    Write-Host "   Application:   http://localhost:8080 (staging)" -ForegroundColor White
    Write-Host "   Database:      localhost:3307" -ForegroundColor White
    Write-Host "   Redis:         localhost:6379" -ForegroundColor White
} elseif ($DeployType -eq "prod") {
    Write-Host "   Application:   http://localhost:8080 (production)" -ForegroundColor White
    Write-Host "   Database:      localhost:3307" -ForegroundColor White
    Write-Host "   Redis:         localhost:6379" -ForegroundColor White
}
Write-Host "   Prometheus:    http://localhost:9090" -ForegroundColor White
Write-Host "   Grafana:       http://localhost:3000" -ForegroundColor White
Write-Host "   Grafana Login: admin / admin123" -ForegroundColor Yellow

Write-Host ""
Write-Host "[MANAGE] Management Commands:" -ForegroundColor Cyan
Write-Host "   Stop all:    docker-compose down" -ForegroundColor White
Write-Host "   Restart:     docker-compose restart" -ForegroundColor White
Write-Host "   Check status: docker-compose ps" -ForegroundColor White
Write-Host "   View logs:    docker-compose logs -f" -ForegroundColor White
