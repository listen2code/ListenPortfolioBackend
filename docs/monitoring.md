# 📊 应用监控与可观测性系统深度指南 (Portfolio Monitoring & Observability Guide)

**Status**: `Production-Ready Architecture (Prometheus + Grafana + Spring Boot Actuator)`

> 本监控栈遵循云原生可观测性规范构建。指标采集、时序存储与数据可视化配置文件位于 `monitoring/` 目录，基础设施编排以根目录 `docker-compose.yml` 为准。

---

## 一、架构设计理念与技术全貌

在企业级微服务与现代化 Web 架构中，**可观测性 (Observability)** 由三大支柱构成：**指标 (Metrics)**、**日志 (Logs)** 与 **调用链路 (Tracing)**。本项目重点落盘了以 **Spring Boot Actuator + Micrometer + Prometheus + Grafana** 为核心的工业级时序指标监控系统。

```
┌────────────────────────────────────────────────────────────────────────────────────────────────────────┐
│                                   Portfolio 全链路时序监控拓扑架构                                        │
└────────────────────────────────────────────────────────────────────────────────────────────────────────┘

    [ 业务请求流量 ]
           │
           ▼
    ┌───────────────────────────────┐
    │ Spring Boot 3 Portfolio App   │
    │ (端口: 8080)                  │
    ├───────────────────────────────┤
    │ • Spring MVC / Tomcat 线程池   │
    │ • Spring Security 认证过滤链   │
    │ • MyBatis-Plus / HikariCP     │
    │ • RedisTemplate 缓存交互      │
    ├───────────────────────────────┤
    │ Micrometer 核心度量门面       │ ── 内存中维护 Counter, Timer, Gauge, DistributionSummary
    ├───────────────────────────────┤
    │ Spring Boot Actuator 端点     │
    │ 暴露: /actuator/prometheus    │ ── 输出符合 OpenMetrics / Prometheus 文本标准的度量快照
    └───────────────────────────────┘
                   ▲
                   │ (1. HTTP GET 定时拉取 / 10s 间隔)
                   │ 容器网络 (portfolio-network) 或 host.docker.internal
    ┌───────────────────────────────┐
    │ Prometheus TSDB (时序数据库)  │
    │ (端口: 9090)                  │
    ├───────────────────────────────┤
    │ • Scrape Engine (抓取引擎)    │ ── 解析 prometheus.yml，定时触发抓取任务
    │ • TSDB 本地时序存储           │ ── 数据持久化于 prometheus_data 数据卷 (默认保留 24h)
    │ • PromQL 算子计算引擎         │ ── 支持 rate()、sum()、topk()、分位数计算
    │ • Web Console / HTTP API      │ ── 提供 /-/healthy 与 /api/v1/query 端点
    └───────────────────────────────┘
                   ▲
                   │ (2. HTTP 数据源查询 PromQL)
                   │ 通过 Docker 内部网络: http://prometheus:9090
    ┌───────────────────────────────┐
    │ Grafana 可视化服务器          │
    │ (端口: 3000)                  │
    ├───────────────────────────────┤
    │ • Provisioning 自动装配机制   │ ── 启动时免密自动注入 Prometheus 数据源与 Dashboard
    │ • JSON 仪表板动态解析渲染     │ ── 实时加载 portfolio-dashboard.json
    │ • 告警与面板渲染引擎          │ ── QPS、P99 延时、JVM 堆内存、GC 暂停、CPU 饱和度
    └───────────────────────────────┘
                   ▲
                   │ (3. HTTPS / HTTP 浏览器访问)
                   │ http://localhost:3000 (admin / admin123)
    ┌───────────────────────────────┐
    │ 开发者 / 运维工程师 / 浏览器    │
    └───────────────────────────────┘
```

### 核心架构设计哲学 (Architectural Decisions)

1. **Pull 抓取模型 vs Push 推送模型**：
   - 系统采用 Prometheus 经典的 **Pull 拉取模型**。应用无须关注监控组件的 IP 和部署状态，仅在自身内存中累加计数器，并通过无状态的 `/actuator/prometheus` 端点对外暴露；
   - 监控系统（Prometheus）全权控制采样频率（10s）与超时策略（5s），避免应用因向外部不可用的监控组件推送数据而引发线程阻塞或级联雪崩；
   - 具备天然的存活感知能力：若 Prometheus 连续抓取失败，指标 `up{job="portfolio-app"} == 0` 会瞬间反映应用离线，无需额外的独立心跳报文。
2. **零手动配置的 GitOps 自动装配 (Zero-Touch Provisioning)**：
   - 摒弃了在 Grafana UI 界面手动点击“创建数据源”和“导入 JSON 文件”的易错方式；
   - 采用 Grafana 官方 **Provisioning 机制**，通过 `datasource.yml` 与 `dashboards.yml` 实现启动时全自动化加载，代码与监控资产 100% 纳入 Git 版本控制。
3. **分层网络隔离与纵深防御 (Defense in Depth)**：
   - 容器编排使用专用的 Docker 桥接网络（`portfolio-network`），Prometheus 与 Grafana 之间通过内部容器名通信（`http://prometheus:9090`）；
   - 在生产环境，`/actuator/**` 严禁直接暴露于公网，仅允许宿主机 Nginx 网关反向代理公网业务流量，内网端口与监控端点受到严格的 VPC 安全组隔离。

---

## 二、文件目录与组件职责

```
monitoring/
├── prometheus.yml                      # Prometheus 核心抓取配置（支持 local 与 docker 双轨模式）
└── grafana/
    ├── provisioning/                   # Grafana 自动装配目录（启动时自动扫描挂载）
    │   ├── datasources/
    │   │   └── datasource.yml          # 数据源自动配置（将 Prometheus 设为默认数据源）
    │   └── dashboards/
    │       └── dashboards.yml          # 仪表板提供者配置（定义扫描路径与 10s 热刷新间隔）
    └── dashboards/
        └── portfolio-dashboard.json    # 预置的 Spring Boot 性能大盘（包含 6 大核心面板）
```

---

## 三、核心配置文件深度解析

### 1. Spring Boot Actuator & 指标配置 (`application.properties`)

```properties
# ===================================================================
# Spring Boot Actuator 端点暴露与安全管控
# ===================================================================
# 严格遵循最小特权原则，仅暴露基础运行状况与指标端点
# 绝不暴露敏感高危端点（如 env、shutdown、heapdump、beans）
management.endpoints.web.exposure.include=health,info,prometheus

# 健康检查详情展示策略：always 表示返回各组件（DB、Redis、磁盘等）的详细就绪状态
management.endpoint.health.show-details=always

# 开启 Kubernetes/容器编排专用的存活 (liveness) 与就绪 (readiness) 探针
management.endpoint.health.probes.enabled=true

# ===================================================================
# Micrometer & Prometheus 指标导出优化
# ===================================================================
# 开启分布式追踪上下文注入
management.tracing.enabled=true

# 启用 Prometheus 格式指标导出适配器
management.metrics.export.prometheus.enabled=true

# 指标统计步长（Step Window），定义直方图与分位数的统计滑动周期
management.metrics.export.prometheus.step=15s

# 显式激活核心度量绑定器
management.metrics.bindings.jvm.enabled=true         # JVM 内存、GC、线程
management.metrics.bindings.web.enabled=true         # HTTP 请求延时与状态码
management.metrics.bindings.processor.enabled=true   # 系统 CPU 核心使用率
```

### 2. Prometheus 抓取配置 (`monitoring/prometheus.yml`)

Prometheus 配置文件创新性地设计了 **本地开发 (Local)** 与 **容器化 (Docker)** 双轨支持模式：

```yaml
global:
  scrape_interval: 15s      # 全局默认抓取频率
  evaluation_interval: 15s  # 告警规则评估周期

scrape_configs:
  # 1. Prometheus 自身性能监控
  - job_name: 'prometheus'
    static_configs:
      - targets: ['localhost:9090']

  # 2. 本地宿主机运行模式 (开发/调试)
  # 容器内的 Prometheus 通过 host.docker.internal 反向穿透访问宿主机 8080
  - job_name: 'portfolio-app-local'
    static_configs:
      - targets: ['host.docker.internal:8080']
    metrics_path: '/actuator/prometheus'
    scrape_interval: 10s
    scrape_timeout: 5s
    params:
      environment: ['local']

  # 3. Docker 容器完整栈模式 (生产/部署)
  # 通过 Docker 内部网络直接解析服务名 app:8080
  - job_name: 'portfolio-app-docker'
    static_configs:
      - targets: ['app:8080']
    metrics_path: '/actuator/prometheus'
    scrape_interval: 10s
    scrape_timeout: 5s
    params:
      environment: ['docker']
```

### 3. Grafana 数据源自动装配 (`monitoring/grafana/provisioning/datasources/datasource.yml`)

```yaml
apiVersion: 1

datasources:
  - name: Prometheus
    type: prometheus
    access: proxy  # 由 Grafana 后端服务器代理请求，彻底根除浏览器跨域 (CORS) 问题
    url: http://${PROMETHEUS_HOST:-prometheus}:${PROMETHEUS_PORT:-9090}
    isDefault: true
    editable: true
```

### 4. Grafana 仪表盘自动装配 (`monitoring/grafana/provisioning/dashboards/dashboards.yml`)

```yaml
apiVersion: 1

providers:
  - name: 'default'
    orgId: 1
    type: file
    disableDeletion: false
    updateIntervalSeconds: 10  # 每 10 秒检测一次磁盘 JSON 文件更新，支持热重载
    allowUiUpdates: true
    options:
      path: /var/lib/grafana/dashboards  # 挂载 portfolio-dashboard.json 的容器内路径
```

---

## 四、重点与难点指标与 PromQL 算法深度剖析

预置在 [`portfolio-dashboard.json`](file:///c:/Users/liste/Downloads/github/ListenPortfolioBackend/monitoring/grafana/dashboards/portfolio-dashboard.json) 中的 6 大核心面板采用了高度严谨的 PromQL 算子，以下为每个面板的数学原理与生产排障指引：

---

### 面板 1：HTTP 接口平均响应延时 (HTTP Response Time)

#### 📈 PromQL 表达式：
```promql
rate(http_server_requests_seconds_sum[5m]) / rate(http_server_requests_seconds_count[5m])
```

#### 🧠 数学原理与底层实现难点：
- **为什么不能直接计算均值？**
  Micrometer 为 HTTP 请求维护了两个累加计数器（Cumulative Counter）：`http_server_requests_seconds_sum`（请求耗时总秒数累加）与 `http_server_requests_seconds_count`（请求总次数累加）。这两个数值随应用启动持续单调递增；
- 若直接相除（`sum / count`），得到的是应用从启动至今所有历史请求的“全局历史平均”，无法反映系统在最近 5 分钟内的性能波动；
- **`rate(...[5m])` 算子的妙用**：
  `rate()` 会在 5 分钟的滑动时间窗口内计算两个相邻采样点之间的导数（每秒增量），即使应用重启发生计数器重置（Counter Reset），`rate()` 也能自动识别并平滑处理；
- 将【每秒耗时增量】除以【每秒请求增量】，精确计算出**当前窗口内单次 HTTP 请求的瞬时平均耗时**（单位：秒）。

#### 🎯 阈值与排障基线：
- `< 200ms`：性能卓越（常规查询）；
- `200ms ~ 500ms`：正常区间；
- `> 1000ms`：性能劣化预警，需排查慢 SQL、第三方 SMTP 邮件同步等待或 Redis 网络锁。

---

### 面板 2：每秒请求吞吐量 (HTTP Request Rate / QPS)

#### 📈 PromQL 表达式：
```promql
rate(http_server_requests_seconds_count[5m])
```

#### 🧠 原理解析：
- 对接口请求总数执行每秒速率运算，即时反映系统的每秒并发事务吞吐能力（Queries Per Second, QPS）；
- 可通过 Label 维度扩展下钻，例如按接口分组：`sum by (uri) (rate(http_server_requests_seconds_count[5m]))`。

---

### 面板 3：系统 CPU 使用率 (System CPU Usage)

#### 📈 PromQL 表达式：
```promql
system_cpu_usage
```

#### 🧠 原理解析：
- 由 Micrometer 的 `ProcessorMetrics` 绑定器调用操作系统底层 JMX `OperatingSystemMXBean` 获取；
- 数值范围在 `0.0` 至 `1.0` 之间（表示 0% ~ 100%）。持续超过 `0.80`（80%）提示系统可能遭遇密集死循环、暴力破解或严重垃圾回收。

---

### 面板 4：JVM 堆内存饱和度 (JVM Heap Memory Usage)

#### 📈 PromQL 表达式：
```promql
jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"}
```

#### 🧠 原理解析：
- 严格过滤 Label `area="heap"`，排除 Metaspace（元空间）与 Code Cache（代码缓存区）等非堆内存；
- 计算已用堆内存与最大可用堆内存（`-Xmx`）的比率。若比率持续高于 `0.85` 且在 Full GC 后依然无法回落，表明存在严重的内存泄漏（Memory Leak）。

---

### 面板 5：Spring Security 认证耗时 (Security Processing Time)

#### 📈 PromQL 表达式：
```promql
rate(spring_security_http_secured_requests_seconds_sum[5m]) / rate(spring_security_http_secured_requests_seconds_count[5m])
```

#### 🧠 原理解析：
- 专门监控 Spring Security 过滤器链（包括 `JwtRequestFilter` 解析、Token 校验、上下文构建及 BCrypt 密码校验）的独立耗时；
- 帮助开发者精准定位性能瓶颈究竟是出现在业务 Controller / DAO 层，还是出现在安全认证与拦截链路。

---

### 面板 6：JVM GC 停顿时间 (Garbage Collection Pause Time)

#### 📈 PromQL 表达式：
```promql
rate(jvm_gc_pause_seconds_sum[5m]) / rate(jvm_gc_pause_seconds_count[5m])
```

#### 🧠 原理解析：
- 计算 G1GC（Garbage-First）垃圾收集器导致的 Stop-The-World (STW) 线程暂停平均耗时；
- 若 GC 暂停时间突增至 `> 200ms`，说明 Young 区过小、晋升过快或发生了 Mixed GC / Full GC，需调优 `-XX:MaxGCPauseMillis` 参数。

---

## 五、安全设计与网络隔离方案

### 1. Actuator 端点放行机制与纵深防御

在 [`SecurityConfig.java`](file:///c:/Users/liste/Downloads/github/ListenPortfolioBackend/src/main/java/com/listen/portfolio/common/config/SecurityConfig.java) 中，配置了如下放行规则：

```java
// Spring Boot Actuator 监控与健康探针端点放行
.requestMatchers("/actuator/health", "/actuator/health/**", "/actuator/prometheus").permitAll()
```

#### 🔒 安全防御分析：
1. **为什么必须 permitAll()？**
   Prometheus 抓取器与 Docker/K8s 容器探针是基础设施级自动化程序，无法持有动态签发的业务 JWT 令牌。若要求携带 Bearer Token，探针会因 401 失败而导致容器被编排引擎强制杀死；
2. **最小暴露范围防御**：
   在 `application.properties` 中只暴露 `health,info,prometheus`。攻击者即便能访问该端口，也无法调用高危端点（如 `/actuator/env` 刺探数据库账号密码，或 `/actuator/shutdown` 远程关停服务）；
3. **网关层物理阻断（生产推荐）**：
   在外部 Nginx 网关中添加如下配置，仅放行内网监控网段：
   ```nginx
   location /actuator/ {
       allow 172.18.0.0/16;    # Docker 容器内部子网
       allow 127.0.0.1;        # 宿主机回环地址
       deny all;               # 阻断所有公网请求
   }
   ```

---

## 六、快速启动与验证实操

### 1. 本地一键拉起监控全栈

```powershell
# 1. 编译可执行 WAR 产物
./gradlew bootWar

# 2. 启动包含 Prometheus 与 Grafana 的 local profile 容器栈
docker-compose --profile local up -d --build
```

### 2. 验证各服务健康状态

```powershell
# 验证后端 App 健康状态
Invoke-WebRequest -Uri "http://localhost:8080/actuator/health" -UseBasicParsing

# 验证 Prometheus 就绪状态
Invoke-WebRequest -Uri "http://localhost:9090/-/healthy" -UseBasicParsing

# 验证 Grafana API 就绪状态
Invoke-WebRequest -Uri "http://localhost:3000/api/health" -UseBasicParsing
```

### 3. 服务访问控制台

| 服务 | 本地访问 URL | 默认凭据 | 职责说明 |
| :--- | :--- | :--- | :--- |
| **Portfolio App** | `http://localhost:8080` | - | Spring Boot 业务主应用 |
| **Actuator 指标** | `http://localhost:8080/actuator/prometheus` | 无需凭据 | Prometheus 原生文本指标快照 |
| **Prometheus Web** | `http://localhost:9090` | 无需凭据 | 时序指标查询与 Target 状态查看 |
| **Grafana 仪表板** | `http://localhost:3000` | `admin` / `admin123` | 性能大盘与可视化图表分析 |

---

## 七、生产高频排障手册 (Troubleshooting)

### 1. Prometheus 抓取目标显示 DOWN (`context deadline exceeded` 或 `connection refused`)
- **排查步骤**：
  1. 访问 `http://localhost:9090/targets`，查看对应 Target 的 Error 信息；
  2. 若在容器内使用本地模式，确保使用的是 `host.docker.internal:8080`；在 Linux 环境需配置 `--add-host=host.docker.internal:host-gateway`；
  3. 若在容器完整栈模式，确保两者处于同一 Docker 网络（`portfolio-network`），且应用服务名为 `app:8080`；
  4. 检查应用的 `SecurityConfig` 是否放行了 `/actuator/prometheus`。

### 2. Grafana 仪表板显示 "No Data"
- **排查步骤**：
  1. 打开 Grafana ➔ Configuration ➔ Data Sources ➔ 点击 Prometheus ➔ 点击 "Save & Test"，确认返回 `Data source is working`；
  2. 进入面板编辑模式，复制 PromQL 语句粘贴到 Prometheus 原生界面（`http://localhost:9090/graph`）中直接执行；
  3. 确认右上角时间选择器未选在“无数据产生的历史时间区间”（建议选择 `Last 15 minutes`）。

### 3. Prometheus 配置动态热加载（无需重启容器）
Prometheus 镜像已在 `docker-compose.yml` 中声明启用了 `--web.enable-lifecycle`，修改 `monitoring/prometheus.yml` 后可直接通过 HTTP 发送热加载信号：
```bash
curl -X POST http://localhost:9090/-/reload
```

---

## 八、监控架构演进与治理待办 (Roadmap)

根据本项目监控现状，已在 [`docs/todo.md`](file:///c:/Users/liste/Downloads/github/ListenPortfolioBackend/docs/todo.md) **第 17 章节（应用监控与可观测性体系深度治理）** 中建立以下 4 项演进目标：

1. **Actuator 监控端点网络安全隔离与鉴权 (Actuator Endpoint Security)**：
   - 在 Nginx 层配置白名单阻断公网爬虫，或引入独立的 management 端口隔离内外部流量。
2. **Prometheus 告警规则持久化与 Alertmanager 多通道通知闭环 (Alertmanager Pipelines)**：
   - 固化 `portfolio-alerts.yml` 告警规则，编排 Alertmanager 服务，打通企业微信/钉钉/邮件报警。
3. **Micrometer 业务级核心指标埋点与 Grafana 专用大盘 (Custom Business Metrics)**：
   - 埋点用户登录、密码重置发信、限流拦截等业务计数器，构建真实反映业务活跃度的可视化大盘。
4. **日志、指标与分布式链路追踪黄金三要素统一联动 (Loki & Tempo Observability)**：
   - 接入 Grafana Loki 与 Tempo，打通“指标异常 ➔ 日志下钻 ➔ 全链路追踪”的云原生可观测闭环。
