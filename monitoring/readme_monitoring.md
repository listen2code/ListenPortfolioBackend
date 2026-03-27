# 📊 监控配置文件说明

## 🎯 概述

本目录包含了 Portfolio 应用监控系统的所有配置文件，实现了 **开箱即用** 的监控体验。

## 📁 文件结构

```
monitoring/
├── README.md                           # 本说明文档
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
  - job_name: 'portfolio-app'  # 监控 Portfolio 应用
    metrics_path: '/actuator/prometheus'  # Spring Boot 指标端点
    scrape_interval: 10s        # 应用指标抓取间隔
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

**作用**: Portfolio 应用专用监控仪表板
- 📈 **6 个监控面板**: 覆盖性能、资源、安全等关键指标
- 🎯 **专门设计**: 针对 Spring Boot 应用优化
- 🔄 **实时刷新**: 每 5 秒刷新数据
- 🏷️ **标签分类**: 便于仪表板管理和搜索

**监控面板**:
1. **HTTP Response Time** - API 响应时间趋势
2. **HTTP Request Rate** - 请求速率监控
3. **System CPU Usage** - CPU 使用率
4. **JVM Heap Memory Usage** - 堆内存使用率
5. **Spring Security Processing Time** - 安全处理时间
6. **Garbage Collection Pause Time** - GC 暂停时间

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
- job_name: 'portfolio-app'
  scrape_interval: 5s  # 更频繁的抓取
```

## 📚 最佳实践

### 性能优化
- 🎯 **合理设置抓取频率**: 避免过于频繁的指标抓取
- 💾 **数据保留策略**: 根据需求调整 Prometheus 数据保留时间
- 🔄 **仪表板刷新**: 根据需要设置仪表板刷新频率

### 安全考虑
- 🔒 **网络隔离**: 使用自定义 Docker 网络
- 🛡️ **访问控制**: 限制监控端点的访问权限
- 🔐 **认证配置**: 配置 Grafana 和 Prometheus 的认证

### 维护管理
- 📝 **配置版本控制**: 将配置文件纳入版本控制
- 💾 **定期备份**: 备份 Grafana 配置和 Prometheus 数据
- 🔄 **定期更新**: 保持 Prometheus 和 Grafana 版本更新

## 🆘 故障排除

### 常见问题

1. **Prometheus 无法抓取指标**
   - 检查网络连通性
   - 验证指标端点可访问性
   - 查看 Prometheus 日志

2. **Grafana 无数据显示**
   - 检查数据源配置
   - 验证 Prometheus 连接
   - 查看仪表板查询语句

3. **服务启动失败**
   - 检查端口占用
   - 查看容器日志
   - 验证配置文件语法

### 调试命令

```bash
# 查看服务状态
docker-compose ps

# 查看服务日志
docker-compose logs prometheus
docker-compose logs grafana
docker-compose logs app

# 测试指标端点
curl http://localhost:8080/actuator/prometheus

# 测试 Prometheus 连接
curl http://localhost:9090/api/v1/query?query=up
```

---

*最后更新: 2026-03-26*  
*维护者: Development Team*
