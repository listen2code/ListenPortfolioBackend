# 📊 Portfolio 应用监控系统

**Status**: `Implemented for Local Docker Profile`

> 当前监控栈以根目录 `docker-compose.yml` 为准，Prometheus / Grafana 配置文件位于 `monitoring/` 目录。
> 如与本文档冲突，请优先参考 `docker-compose.yml`、`monitoring/prometheus.yml` 与 `monitoring/grafana/**`。

## 🎯 概述

本项目集成了 **Grafana + Prometheus** 监控系统，提供实时的应用性能监控、健康检查和可视化仪表板。

## 📁 文件结构

```
monitoring/
├── prometheus.yml                      # Prometheus 监控配置
└── grafana/
    ├── provisioning/
    │   ├── datasources/
    │   │   └── datasource.yml          # Grafana 数据源自动配置
    │   └── dashboards/
    │       └── dashboards.yml          # Grafana 仪表板自动配置
    └── dashboards/
        └── portfolio-dashboard.json     # Portfolio 应用监控仪表板
```

## 🚀 快速启动

### 📋 前置条件

确保您的系统已安装：
- **Docker Desktop** - 下载地址: https://www.docker.com/products/docker-desktop
- **PowerShell** - Windows 自带

### 🔄 启动步骤

#### **步骤 1: 检查 Docker 状态**

```powershell
# 检查 Docker 是否正常运行
docker --version
```

如果看到 `Docker version 29.3.0...`，说明 Docker 已就绪。

#### **步骤 2: 启动监控系统**

```powershell
# 使用本地 profile 启动应用 + MySQL + Redis + Prometheus + Grafana
docker-compose --profile local up -d --build
```

当前仓库中用于本地监控的最小启动路径就是上面的 `docker-compose` 命令。
若需要脚本化部署，可参考根目录 `docker_deploy.ps1`，但文档中的监控栈说明默认基于 `docker-compose.yml`。

### 📱 启动成功后的访问地址

| 服务 | 地址 | 账号 | 说明 |
|------|------|------|------|
| **Portfolio App** | http://localhost:8080 | - | 主应用 |
| **Prometheus** | http://localhost:9090 | - | 指标收集和查询 |
| **Grafana** | http://localhost:3000 | admin / admin123 | 可视化监控面板 |

> Grafana 默认账号密码来自 `docker-compose.yml` 中的环境变量默认值，可通过环境变量覆盖。

### ⚡ 快速验证

启动完成后，验证服务状态：

```powershell
# 检查应用健康状态
Invoke-WebRequest -Uri "http://localhost:8080/actuator/health" -UseBasicParsing

# 检查 Prometheus
Invoke-WebRequest -Uri "http://localhost:9090/-/healthy" -UseBasicParsing

# 检查 Grafana
Invoke-WebRequest -Uri "http://localhost:3000/api/health" -UseBasicParsing
```

### 🛠️ 手动启动（备选方案）

如果脚本出现问题，可以手动启动：

```powershell
# 1. 构建应用
.\mvnw.cmd clean package -DskipTests

# 2. 启动服务
docker-compose --profile local up -d --build

# 3. 等待服务启动
Start-Sleep -Seconds 30

# 4. 检查状态
docker-compose ps
```

## 🔧 配置文件详解

### 1. docker-compose.yml (根目录)

**作用**: 定义完整的监控栈服务
- 📦 **Portfolio App**: Spring Boot 应用 (端口 8080)
- 🗄️ **MySQL**: 数据库服务 (端口 3307)
- 📊 **Prometheus**: 指标收集服务 (端口 9090)
- 📈 **Grafana**: 可视化面板 (端口 3000)

**关键特性**:
- ✅ **服务依赖**: 应用等待数据库健康后启动
- ✅ **网络隔离**: 自定义 Docker 网络确保服务间通信
- ✅ **数据持久化**: 所有数据都持久化到 Docker 卷
- ✅ **JVM 优化**: 配置了 G1GC 和内存参数
- ✅ **Profile 启动**: 应用、MySQL、Redis、Prometheus、Grafana 都挂在 `local` profile 下
- ✅ **环境变量**: 应用容器默认设置 `SPRING_PROFILES_ACTIVE=docker`

### 2. prometheus.yml

**作用**: Prometheus 监控配置文件
- 🎯 **监控目标**: Portfolio 应用和 Prometheus 自身
- ⏱️ **抓取频率**: 应用指标 10秒，全局默认 15秒
- 📊 **指标路径**: `/actuator/prometheus` (Spring Boot Actuator)
- 🔔 **告警支持**: 预留告警规则配置

**配置详情**:
```yaml
global:
  scrape_interval: 15s          # 全局抓取间隔
  evaluation_interval: 15s      # 告警评估间隔

scrape_configs:
  - job_name: 'prometheus'      # 监控 Prometheus 自身
  - job_name: 'portfolio-app-local'  # 监控 Portfolio 应用 (本地模式)
    metrics_path: '/actuator/prometheus'  # Spring Boot 指标端点
    scrape_interval: 10s        # 应用指标抓取间隔
  - job_name: 'portfolio-app-docker'  # 监控 Portfolio 应用 (Docker 模式)
    metrics_path: '/actuator/prometheus'
    scrape_interval: 10s
```

### 3. grafana/provisioning/datasources/datasource.yml

**作用**: Grafana 数据源自动配置
- 🔄 **自动加载**: Grafana 启动时自动创建数据源
- 🔗 **连接 Prometheus**: 配置 Prometheus 作为数据源
- 🛡️ **代理访问**: 通过 Grafana 代理避免跨域问题
- ⭐ **默认数据源**: 设为默认数据源

**配置效果**:
- 无需手动配置数据源
- 开箱即用的监控体验
- 自动连接到 Prometheus 服务

### 4. grafana/provisioning/dashboards/dashboards.yml

**作用**: Grafana 仪表板自动部署
- 📁 **自动扫描**: 扫描指定目录下的仪表板文件
- 🔄 **热更新**: 每 10 秒检查文件变化
- 📊 **JSON 格式**: 支持 JSON 格式的仪表板定义
- ✏️ **允许编辑**: 支持在 UI 中修改仪表板

**配置效果**:
- 仪表板自动导入
- 支持仪表板版本控制
- 无需手动导入仪表板

### 5. grafana/dashboards/portfolio-dashboard.json

**作用**: 仓库中预置的 Grafana 仪表板定义文件
- 📈 **已提交 JSON 配置**: 可随仓库版本一起管理
- 🔄 **自动导入**: 由 Grafana provisioning 在启动时加载
- 🎯 **面向 Spring Boot 指标**: 查询 Prometheus 中的 HTTP / JVM / 系统类指标

> 仪表板中的具体 panel 数量、标题和查询语句请以 `portfolio-dashboard.json` 或 Grafana UI 中当前导入结果为准。

## 🚀 工作原理

### 启动流程

1. **Docker Compose 启动**
   - 按依赖顺序启动服务
   - 创建网络和数据卷
   - 挂载配置文件

2. **服务初始化**
   - MySQL 启动并创建数据库
   - Portfolio App 等待数据库健康后启动
   - Prometheus 加载配置文件
   - Grafana 加载 provisioning 配置

3. **监控配置生效**
   - Grafana 自动创建 Prometheus 数据源
   - Grafana 自动导入 Portfolio 仪表板
   - Prometheus 开始抓取应用指标
   - 仪表板显示实时数据

### 数据流向

```
Portfolio App → /actuator/prometheus → Prometheus → Grafana → 仪表板
```

## 🎯 如何使用监控系统

### 📊 Grafana 可视化面板

#### **访问 Grafana**

1. 打开浏览器访问: http://localhost:3000
2. 使用账号登录: **admin / admin123**
3. 您将看到预配置的 "Portfolio App Monitoring" 仪表板

#### **仪表板功能**

| 面板名称 | 监控内容 | 正常范围 | 异常处理 |
|----------|----------|----------|----------|
| **HTTP Response Time** | API 响应时间 | < 500ms | > 1s 需要优化 |
| **HTTP Request Rate** | 每秒请求数 | 根据业务量 | 突增需检查 |
| **System CPU Usage** | CPU 使用率 | < 70% | > 80% 需要扩容 |
| **JVM Heap Memory Usage** | 堆内存使用率 | < 80% | > 90% 需要优化 |
| **Spring Security Processing Time** | 安全处理时间 | < 100ms | > 200ms 需要优化 |
| **Garbage Collection Pause Time** | GC 暂停时间 | < 50ms | > 100ms 需要调优 |

#### **自定义查询**

在 Grafana 中可以创建自定义查询：

```promql
# 平均响应时间 (按端点)
rate(http_server_requests_seconds_sum[5m]) / rate(http_server_requests_seconds_count[5m])

# 错误率
rate(http_server_requests_seconds_count{status=~"5.."}[5m]) / rate(http_server_requests_seconds_count[5m])

# 请求量排行
topk(10, rate(http_server_requests_seconds_count[5m]))

# 内存使用率
jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"}
```

### 📊 Prometheus 指标查询

#### **访问 Prometheus**

1. 打开浏览器访问: http://localhost:9090
2. 在查询框中输入指标名称
3. 选择时间范围查看趋势

#### **常用指标查询**

```promql
# 查看所有 HTTP 请求指标
http_server_requests_seconds_count

# 查看特定端点的响应时间
http_server_requests_seconds_sum{uri="/actuator/prometheus"}

# 查看 JVM 内存使用
jvm_memory_used_bytes

# 查看 CPU 使用率
system_cpu_usage

# 查看 GC 指标
jvm_gc_pause_seconds
```

### 🔍 应用健康检查

#### **健康检查端点**

```powershell
# 基本健康状态
Invoke-WebRequest -Uri "http://localhost:8080/actuator/health" -UseBasicParsing | ConvertFrom-Json

# 存活探针
Invoke-WebRequest -Uri "http://localhost:8080/actuator/health/liveness" -UseBasicParsing

# 就绪探针
Invoke-WebRequest -Uri "http://localhost:8080/actuator/health/readiness" -UseBasicParsing
```

#### **健康状态说明**

| 状态 | 含义 | 处理方式 |
|------|------|----------|
| **UP** | 服务正常 | 继续监控 |
| **DOWN** | 服务异常 | 查看日志，重启服务 |
| **OUT_OF_SERVICE** | 维护中 | 等待恢复 |
| **UNKNOWN** | 状态未知 | 检查配置 |

### 📱 实时监控和日志

#### **查看实时日志**

```powershell
# 查看应用日志
docker-compose logs -f app

# 查看 Prometheus 日志
docker-compose logs -f prometheus

# 查看 Grafana 日志
docker-compose logs -f grafana

# 查看数据库日志
docker-compose logs -f db
```

#### **查看服务状态**

```powershell
# 查看所有服务状态
docker-compose ps

# 查看资源使用情况
docker stats

# 查看网络连接
docker network ls
```

### ⚙️ 监控配置管理

#### **重启服务**

```powershell
# 重启单个服务
docker-compose restart app
docker-compose restart prometheus
docker-compose restart grafana

# 重启所有服务
docker-compose restart
```

#### **更新配置**

```powershell
# 修改配置后重新构建
docker-compose --profile local up --build -d

# 仅重新构建应用
docker-compose --profile local up --build -d app
```

#### **数据备份**

```powershell
# 备份 Prometheus 数据
docker exec prometheus tar -czf /tmp/prometheus-backup.tar.gz /prometheus
docker cp prometheus:/tmp/prometheus-backup.tar.gz ./prometheus-backup.tar.gz

# 备份 Grafana 配置
docker exec grafana tar -czf /tmp/grafana-backup.tar.gz /var/lib/grafana
docker cp grafana:/tmp/grafana-backup.tar.gz ./grafana-backup.tar.gz
```

## 📊 监控指标详解

### 1. 应用性能指标

| 指标名称 | 说明 | 用途 | 正常范围 |
|----------|------|------|----------|
| `http_server_requests_seconds` | HTTP 请求响应时间 | API 性能监控 | < 500ms |
| `http_server_requests_seconds_count` | HTTP 请求计数 | 吞吐量统计 | 根据业务量 |
| `system_cpu_usage` | 系统 CPU 使用率 | 资源监控 | < 70% |
| `jvm_memory_used_bytes` | JVM 内存使用量 | 内存监控 | < 80% |
| `jvm_gc_pause_seconds` | GC 暂停时间 | 垃圾回收监控 | < 50ms |

### 2. 业务指标

| 指标名称 | 说明 | 用途 |
|----------|------|------|
| `user_registrations_total` | 用户注册总数 | 业务增长 |
| `user_logins_total` | 用户登录总数 | 用户活跃度 |
| `api_calls_total` | API 调用总数 | API 使用统计 |
| `api_errors_total` | API 错误总数 | 错误率监控 |

### 3. 安全指标

| 指标名称 | 说明 | 用途 |
|----------|------|------|
| `spring_security_http_secured_requests_seconds` | 安全处理时间 | 安全性能 |
| `spring_security_filterchains_seconds` | 过滤器处理时间 | 安全链路监控 |

## 🎯 使用场景

### 开发环境
- 🔍 **实时调试**: 查看应用性能指标
- 📊 **性能分析**: 分析 API 响应时间和资源使用
- 🐛 **问题定位**: 通过指标快速定位问题

### 测试环境
- 📈 **性能测试**: 监控负载测试期间的性能表现
- 🔎 **回归测试**: 确保代码变更不影响性能
- 📋 **测试报告**: 生成性能测试报告

### 生产环境
- 🚨 **实时监控**: 7x24 小时监控应用状态
- 📊 **容量规划**: 基于历史数据规划资源
- 🔔 **告警通知**: 配置指标告警（预留功能）

## 🔧 自定义配置

### 添加新的监控目标

在 `prometheus.yml` 中添加新的 `scrape_configs`:

```yaml
- job_name: 'new-service'
  static_configs:
    - targets: ['new-service:8080']
  metrics_path: '/actuator/prometheus'
  scrape_interval: 10s
```

### 创建新的仪表板

1. 在 Grafana UI 中创建仪表板
2. 导出为 JSON 文件
3. 放入 `grafana/dashboards/` 目录
4. Grafana 会自动加载新仪表板

### 修改监控频率

在 `prometheus.yml` 中调整 `scrape_interval`:

```yaml
- job_name: 'portfolio-app-local'
  scrape_interval: 5s  # 更频繁的抓取
```

## 🚨 故障排除

### 常见问题及解决方案

#### **问题 1: 应用无法启动**

**症状**: `Portfolio App: Unhealthy`

**排查步骤**:
```powershell
# 1. 查看应用日志
docker-compose logs app

# 2. 检查端口占用
netstat -an | findstr 8080

# 3. 重新构建应用
docker-compose --profile local up --build -d app
```

**常见原因**:
- 端口 8080 被占用
- 应用构建失败
- 数据库连接问题

#### **问题 2: Prometheus 无法收集指标**

**症状**: `Prometheus: Unhealthy`

**排查步骤**:
```powershell
# 1. 检查指标端点
Invoke-WebRequest -Uri "http://localhost:8080/actuator/prometheus" -UseBasicParsing

# 2. 查看 Prometheus 日志
docker-compose logs prometheus

# 3. 检查网络连通性
docker exec prometheus ping app
```

**常见原因**:
- 应用指标端点不可访问
- Prometheus 配置错误
- 网络连接问题

#### **问题 3: Grafana 无法连接 Prometheus**

**症状**: Grafana 仪表板显示 "No data"

**排查步骤**:
```powershell
# 1. 检查 Grafana 数据源配置
curl -u admin:admin123 http://localhost:3000/api/datasources

# 2. 查看 Grafana 日志
docker-compose logs grafana

# 3. 测试 Prometheus 连接
curl http://localhost:9090/api/v1/query?query=up
```

**常见原因**:
- 数据源配置错误
- Prometheus 服务未启动
- 网络连接问题

#### **问题 4: 内存使用过高**

**症状**: JVM 内存使用率 > 80%

**排查步骤**:
```powershell
# 1. 查看内存指标
curl http://localhost:8080/actuator/prometheus | grep jvm_memory

# 2. 分析内存泄漏
docker stats app

# 3. 调整 JVM 参数
# 编辑 docker-compose.yml 中的 JAVA_OPTS
```

**解决方案**:
- 调整堆内存大小
- 优化代码减少内存使用
- 增加内存限制

### 性能优化建议

#### **应用层面优化**

1. **数据库优化**
   - 配置连接池大小
   - 启用查询缓存
   - 优化 SQL 查询

2. **缓存策略**
   - 启用 Redis 缓存
   - 配置本地缓存
   - 实现缓存预热

3. **异步处理**
   - 使用 @Async 注解
   - 配置线程池
   - 实现异步 API

#### **JVM 层面优化**

```yaml
# docker-compose.yml 中的 JVM 优化参数
environment:
  JAVA_OPTS: >-
    -Xms512m
    -Xmx1024m
    -XX:+UseG1GC
    -XX:MaxGCPauseMillis=100
    -XX:+PrintGCDetails
    -XX:+PrintGCTimeStamps
    -Xloggc:/app/gc.log
```

#### **监控层面优化**

```properties
# application.properties 中的监控优化
management.metrics.export.prometheus.step=30s
management.endpoint.health.cache.time-to-live=60s
management.endpoint.info.cache.time-to-live=60s
```

## 📈 最佳实践

### 1. 监控策略

- **设置告警阈值**: 根据业务需求设置合理的告警阈值
- **建立基线**: 记录正常运行时的指标基线
- **定期巡检**: 每日检查关键指标趋势
- **容量规划**: 基于历史数据预测资源需求

### 2. 日志管理

- **结构化日志**: 使用 JSON 格式便于分析
- **日志级别**: 生产环境使用 WARN 或 ERROR
- **日志轮转**: 配置日志文件大小和保留策略
- **集中收集**: 使用 ELK 或 Loki 收集日志

### 3. 安全考虑

- **访问控制**: 限制监控端点的访问权限
- **数据加密**: 传输敏感指标时使用 HTTPS
- **审计日志**: 记录监控系统的访问日志
- **定期更新**: 保持 Prometheus 和 Grafana 版本更新

### 4. 备份策略

- **配置备份**: 定期备份 Grafana 仪表板和配置
- **数据备份**: 备份 Prometheus 历史数据
- **版本控制**: 将监控配置纳入版本控制
- **灾难恢复**: 制定监控系统故障恢复方案

## 🚨 告警配置

### Prometheus 告警规则示例

```yaml
# alerts.yml
groups:
  - name: portfolio.rules
    rules:
      - alert: HighResponseTime
        expr: rate(http_server_requests_seconds_sum[5m]) / rate(http_server_requests_seconds_count[5m]) > 1
        for: 2m
        labels:
          severity: warning
        annotations:
          summary: "High response time detected"
          
      - alert: HighCPUUsage
        expr: system_cpu_usage > 0.8
        for: 5m
        labels:
          severity: critical
        annotations:
          summary: "High CPU usage detected"
          
      - alert: HighMemoryUsage
        expr: jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"} > 0.8
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "High memory usage detected"
```

## 🔍 故障排查

### 1. 服务无法启动

```bash
# 检查日志
docker-compose logs app

# 检查端口占用
netstat -an | grep 8080
netstat -an | grep 9090
netstat -an | grep 3000
```

### 2. Prometheus 无法收集指标

```bash
# 检查指标端点
curl http://localhost:8080/actuator/prometheus

# 检查 Prometheus 配置
curl http://localhost:9090/targets
```

### 3. Grafana 无法连接 Prometheus

```bash
# 检查 Grafana 数据源配置
curl -u admin:admin123 http://localhost:3000/api/datasources

# 检查网络连通性
docker exec prometheus ping grafana
```

## 📊 性能调优建议

### 1. 应用层面

- **数据库连接池**: 优化连接池配置
- **缓存策略**: 启用适当的缓存机制
- **异步处理**: 使用异步处理提高并发性能

### 2. JVM 层面

- **内存分配**: 根据实际需求调整堆内存大小
- **GC 策略**: 使用 G1GC 减少停顿时间
- **线程池**: 优化线程池配置

### 3. 监控层面

- **指标采样**: 调整指标收集频率
- **数据保留**: 配置适当的数据保留策略
- **告警阈值**: 根据业务需求调整告警阈值

## 🔄 数据备份

### Prometheus 数据备份

```bash
# 备份 Prometheus 数据
docker exec prometheus tar -czf /tmp/prometheus-backup.tar.gz /prometheus

# 恢复 Prometheus 数据
docker exec prometheus tar -xzf /tmp/prometheus-backup.tar.gz -C /
```

### Grafana 配置备份

```bash
# 备份 Grafana 配置
docker exec grafana tar -czf /tmp/grafana-backup.tar.gz /var/lib/grafana

# 恢复 Grafana 配置
docker exec grafana tar -xzf /tmp/grafana-backup.tar.gz -C /
```

## 📚 相关文档

- [Prometheus 官方文档](https://prometheus.io/docs/)
- [Grafana 官方文档](https://grafana.com/docs/)
- [Spring Boot Actuator 文档](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html)
- [Micrometer 文档](https://micrometer.io/docs/)

## 🆘 技术支持

如果遇到问题，可以：

1. **查看日志**: `docker-compose logs -f [service-name]`
2. **检查配置**: 确认配置文件正确性
3. **重启服务**: `docker-compose restart [service-name]`
4. **社区支持**: 查阅相关技术文档和社区

---

*最后更新: 2026-03-27*  
*维护者: Development Team*
