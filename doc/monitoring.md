# 📊 Portfolio 应用监控系统

## 🎯 概述

本项目集成了 **Grafana + Prometheus** 监控系统，提供实时的应用性能监控、健康检查和可视化仪表板。

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
# 启动所有监控服务
.\start-monitoring-en.ps1
```

脚本会自动完成：
- ✅ 停止现有容器
- ✅ 构建应用镜像
- ✅ 启动所有服务
- ✅ 检查健康状态
- ✅ 显示访问地址

### 📱 启动成功后的访问地址

| 服务 | 地址 | 账号 | 说明 |
|------|------|------|------|
| **Portfolio App** | http://localhost:8080 | - | 主应用 |
| **Prometheus** | http://localhost:9090 | - | 指标收集和查询 |
| **Grafana** | http://localhost:3000 | admin / admin123 | 可视化监控面板 |

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
docker-compose up --build -d

# 3. 等待服务启动
Start-Sleep -Seconds 30

# 4. 检查状态
docker-compose ps
```

## 🎯 如何使用监控系统

### � Grafana 可视化面板

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
docker-compose up --build -d

# 仅重新构建应用
docker-compose up --build -d app
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
docker-compose up --build -d app
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

## 📈 Grafana 仪表板

### 主要面板

1. **HTTP Response Time** - HTTP 响应时间趋势
2. **HTTP Request Rate** - 请求速率监控
3. **System CPU Usage** - CPU 使用率
4. **JVM Heap Memory Usage** - 堆内存使用率
5. **Spring Security Processing Time** - 安全处理时间
6. **Garbage Collection Pause Time** - GC 暂停时间

### 自定义查询示例

```promql
# 平均响应时间
rate(http_server_requests_seconds_sum[5m]) / rate(http_server_requests_seconds_count[5m])

# 请求速率
rate(http_server_requests_seconds_count[5m])

# 错误率
rate(http_server_requests_seconds_count{status=~"5.."}[5m]) / rate(http_server_requests_seconds_count[5m])

# 内存使用率
jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"}
```

## 🔧 优化配置

### 1. JVM 优化

```yaml
# docker-compose.yml
environment:
  JAVA_OPTS: "-Xms256m -Xmx512m -XX:+UseG1GC -XX:MaxGCPauseMillis=200"
```

### 2. 指标收集优化

```properties
# application.properties
management.metrics.export.prometheus.step=15s
management.metrics.bindings.jvm.enabled=true
management.metrics.bindings.web.enabled=true
```

### 3. 健康检查优化

- 缓存健康检查结果（1分钟）
- 异步检查组件状态
- 快速响应机制

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