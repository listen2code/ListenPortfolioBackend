# 📌 开发者核心备忘录与运维实战速查手册 (Developer CheatSheet & Ops Guide)

**Status**: `Active & Maintained`

本文档为 Listen Portfolio 后端工程日常开发、联调测试、构建打包、容器编排以及线上排障的**全景核心备忘录与速查手册 (CheatSheet)**，旨在帮助开发者与运维人员快速检索核心服务入口、执行高频命令并高效排查各类端口及进程冲突。

---

## 一、核心服务与交互门户矩阵 (Core Service & Portal URL Matrix)

| 服务 / 模块名称 | 本地访问入口 (Local URL) | 云端生产入口 (Production URL) | 认证凭据 / 鉴权方式 | 职责说明与核心用途 |
| :--- | :--- | :--- | :--- | :--- |
| **Swagger UI 文档** | `http://localhost:8080/swagger-ui/index.html` | `https://listen2code.is-a.dev/api/swagger-ui/index.html` | 免登录（支持全局 Bearer Token 注入） | OpenAPI 3.0 可视化交互控制台，支持在线免 Postman 接口联调 |
| **OpenAPI v3 Schema** | `http://localhost:8080/v3/api-docs` | `https://listen2code.is-a.dev/api/v3/api-docs` | 公开放行 | 符合 OpenAPI 规范的元数据描述 JSON，供前端代码生成器使用 |
| **静态图片资产测试** | `http://localhost:8080/images/project1.jpg` | `https://listen2code.is-a.dev/api/images/project1.jpg` | 公开放行 | 验证后端类路径静态资源映射与 Nginx `^~ /api/` 反向代理穿透 |
| **密码重置静态 Web 页** | `http://localhost:8080/password-reset-out-email.html` | `https://listen2code.is-a.dev/password-reset-out-email.html` | URL 携带 Query Token | 邮件内重置链接的落地 Web 表单页，直接调用重置 API |
| **Actuator 健康探针** | `http://localhost:8080/actuator/health` | (生产仅限内网访问) | 基础组件状态明细 (`show-details=always`) | 容器存活 (Liveness) 与就绪 (Readiness) 自动化健康探测 |
| **Prometheus 原生指标**| `http://localhost:8080/actuator/prometheus` | (生产仅限内网访问) | OpenMetrics 标准文本格式 | Spring Boot Actuator 暴露的 JVM、HTTP 延时、QPS 原生指标 |
| **Prometheus 控制台** | `http://localhost:9090` | (生产未开放公网) | 无需认证 | Prometheus 时序数据库 Web 管理界面，支持 PromQL 即时查询与 Target 状态查看 |
| **Grafana 监控大盘** | `http://localhost:3000` | (生产未开放公网) | `admin` / `admin123` | 自动化装配 (Provisioning) 的可视化大盘，实时监测性能指标 |
| **MailHog 虚拟邮件箱** | `http://localhost:8025` | (本地联调专用) | 1025 为 SMTP 端口 | 本地虚拟发信拦截器，免绑真实邮箱预览 HTML 邮件渲染与格式 |

---

## 二、Gradle 本地构建与工程化生命周期指令 (Gradle Lifecycle Commands)

> [!TIP]
> Windows 终端建议直接执行 `.\gradlew.bat <task>`；macOS / Linux 终端请使用 `./gradlew <task>`。系统已全面固化基于 **Gradle 8.5 Wrapper** 的标准构建流水线。

### 常用构建与运行指令表：

```bash
# 1. 本地直接启动 Spring Boot 后端主应用 (开发调试)
.\gradlew.bat bootRun

# 2. 清理历史构建产物与缓存 (清理 build/ 与 target/ 目录)
.\gradlew.bat clean

# 3. 快速编译 Java 源码 (增量编译，快速定位语法错误)
.\gradlew.bat compileJava

# 4. 执行全量自动化单元测试与集成测试 (自动拉起 H2 内存库)
.\gradlew.bat test

# 5. 快速执行全量测试 (排除耗时较长的并发性能压测)
.\gradlew.bat test -Dtest=!PerformanceTest

# 6. 单独执行指定测试类 (支持精准调试单个业务组件)
.\gradlew.bat test --tests "com.listen.portfolio.service.AuthServiceTest"
.\gradlew.bat test --tests "com.listen.portfolio.service.EmailServiceTest"

# 7. 打包生成可执行生产 WAR 归档文件 (位于 target/portfolio-0.0.1-SNAPSHOT.war)
.\gradlew.bat bootWar

# 8. 生成 JaCoCo 单元测试覆盖率 HTML 分析报告 (输出至 build/reports/jacoco/test/html/index.html)
.\gradlew.bat test jacocoTestReport

# 9. 触发 SpotBugs 静态代码质量审查与潜在缺陷扫描 (输出至 build/reports/spotbugs/main.html)
.\gradlew.bat spotbugsMain
```

#### 💡 核心设计细节：为什么打包任务采用 `bootWar` 而非 `bootJar`？
本项目在 `build.gradle` 中引入了 `apply plugin: 'war'` 插件，使用 `bootWar` 打包产物既能作为独立可执行文件直接通过 `java -jar app.war` 在 Docker 容器内部启动（见 `Dockerfile`），同时又保持了标准 J2EE / Servlet 3.0+ 外部容器热部署的完全兼容性，满足双轨部署需求。

---

## 三、Docker 容器化全栈编排与运维速查 (Docker Compose Cheatsheet)

### 1. 本地一键启动与停机脚本

```powershell
# 方式 A：使用自动化一键部署脚本 (自动检查镜像、数据库健康等待、探针就绪)
.\docker_deploy.ps1

# 方式 B：使用原生 Docker Compose 启动 local profile (App + MySQL + Redis + Prometheus + Grafana)
docker-compose --profile local up -d --build

# 方式 C：平滑停止并清理所有容器网络与占用
.\docker_stop.ps1
```

### 2. 容器集群状态与资源占用监控

```bash
# 查看当前所有微服务容器运行状态与端口映射
docker-compose ps

# 实时查看各个容器的 CPU、内存占用率与网络 I/O
docker stats
```

### 3. 实时日志追踪命令

```bash
# 追踪后端 Spring Boot 应用日志 (输出 JSON 结构化日志与业务 SQL)
docker-compose logs -f app

# 追踪 MySQL 8.0 数据库启动与连接日志
docker-compose logs -f db

# 追踪 Redis 缓存容器日志
docker-compose logs -f redis

# 追踪 Prometheus 时序指标抓取日志
docker-compose logs -f prometheus

# 追踪 Grafana 面板渲染日志
docker-compose logs -f grafana
```

### 4. 快速进入容器交互环境

```bash
# 进入后端 Spring Boot 应用容器交互 Shell
docker exec -it listen_portfolio_app sh

# 进入 Redis 容器并打开 redis-cli 命令行
docker exec -it listen_portfolio_redis redis-cli

# 进入 MySQL 容器并使用专用账号登录数据库
docker exec -it listen_portfolio_db mysql -uportfolio_user -pportfolio_password portfolio_db
```

---

## 四、数据库 (MySQL/Flyway) 与缓存 (Redis) 交互实战

### 1. Flyway 数据库迁移状态自查

应用启动时会通过 `FlywayConfig` 自动同步执行 `src/main/resources/db/migration/` 下的 SQL 脚本。如需检查版本迁移基线：

```sql
-- 登录数据库后查询 Flyway 历史版本记录
SELECT 
    installed_rank AS '序号',
    version        AS '版本号',
    description    AS '描述',
    type           AS '类型',
    script         AS '脚本名',
    execution_time AS '执行耗时(ms)',
    success        AS '状态'
FROM flyway_schema_history
ORDER BY installed_rank DESC;
```

### 2. Redis 关键凭证与限流 Key 实时排查

```bash
# 查看所有被加入黑名单的失效 JWT
redis-cli keys "blacklist:*"

# 查看所有在线用户持有的长效 Refresh Token
redis-cli keys "refresh_token:*"

# 查看当前已签发且有效的密码重置 Token
redis-cli keys "password_reset:*"

# 查询指定重置 Token 的剩余有效 TTL 秒数 (默认 3600 秒)
redis-cli ttl "password_reset:<your_token>"

# 查看当前触发了滑动时间窗口限流的 IP 与邮箱
redis-cli keys "rate_limit:*"
```

---

## 五、跨平台进程管理与端口冲突排查急救 (Port Conflict & Process Management)

在本地开发调试中，经常遭遇 `Port 8080 is already in use`（端口被历史失控进程占用）导致应用启动崩溃。

### 1. Windows 环境 (PowerShell / CMD)

```powershell
# 1. 查找占用 8080 端口的进程 PID
netstat -ano | findstr :8080

# 输出示例: TCP    0.0.0.0:8080    0.0.0.0:0    LISTENING    18424
# 最后一列数字 (18424) 即为进程 PID

# 2. 根据 PID 强制终止占用端口的进程
taskkill /F /PID 18424

# 3. 终极大招：一键强制关闭所有失控的 Java 僵尸进程
taskkill /F /IM java.exe

# 4. 检查 Docker MySQL 映射端口 (3307) 是否冲突
netstat -ano | findstr :3307
```

### 2. Linux / macOS 环境 (Bash)

```bash
# 1. 定位占用 8080 端口的进程与应用名
lsof -i :8080
# 或使用 netstat
netstat -tlpn | grep 8080

# 2. 根据 PID 杀死进程
kill -9 <PID>

# 3. 快速杀死所有 Java 进程
killall -9 java
```

---

## 六、生产服务器与 Nginx 网关运维速查 (Production Server & Nginx Ops)

针对 AWS EC2 生产实例（`13.218.192.181` / `listen2code.is-a.dev`）的日常维护：

```bash
# 1. 测试 Nginx 配置文件语法是否合法
sudo nginx -t

# 2. 零停机平滑热重载 Nginx 配置
sudo nginx -s reload
# 或
sudo systemctl reload nginx

# 3. 检查 Let's Encrypt SSL 证书续约演练 (Dry-run)
sudo certbot renew --dry-run

# 4. 查看生产 Docker 后端容器最后 100 行实时日志
sudo docker logs --tail 100 -f portfolio-backend-prod

# 5. 重启生产后端微服务容器
sudo docker restart portfolio-backend-prod
```

---

## 七、架构演进与待办治理规划 (Roadmap)

基于当前日常开发命令零散、缺少集成化脚手架的现状，在 [`docs/todo.md`](file:///c:/Users/liste/Downloads/github/ListenPortfolioBackend/docs/todo.md) **第 19 章节（研发运维工具链与自查备忘协同演进）** 中建立了以下 4 项提升规划：

1. **统一跨平台开发者 CLI 脚本工具箱 (Unified Developer CLI & Justfile)**：
   - 引入 `justfile`，将 `note.md` 中的常用指令封装为 `just dev`、`just test`、`just kill-port` 等跨平台标准化指令。
2. **本地与生产环境变量漂移自动探测与校验 (Config Drift Validator)**：
   - 编写启动期自动对比脚本，拦截缺失关键配置环境变量的无效启动。
3. **数据库一键种子填充与测试数据重置脚本 (Automated DB Seed & Reset Tooling)**：
   - 提供快速重置测试基线命令，支持清空脏数据并重新回滚执行 Flyway V2 迁移。
4. **自动化全端点健康巡检与冒烟测试脚本 (Automated Health & Smoke Probe Script)**：
   - 编写一键冒烟脚本并发探测本文档记录的 8 大核心端点，自动生成健康检查矩阵。
