# 📋 TODO 清单

## 项目现状判断

### 已确认的当前状态

- 分层结构、JWT、Redis 黑名单、邮件服务、监控、日志、Flyway、Docker 基础能力都已具备
- 这个项目已经是一个**可运行、可联调、可继续维护**的支撑型后端
- 当前最需要补的不是“再加很多功能”，而是**契约收口、可靠性、文档可信度与自动化**

### 当前主要风险与注意项

- 生产环境 HTTPS 证书（基于 `is-a.dev` 免费域名）待上游 PR 合并后一键激活
- 部分高阶安全纵深设计（如异常流量自动封禁、复杂威胁自适应响应等）属于长期架构规划，当前优先保持核心链路稳定高效
- 生产环境敏感密钥通过环境变量注入，需持续规避本地开发明文泄漏风险

## Now

### 1. Flutter 前后端 API 对齐

**现状**：已完成。Flutter model、后端 DTO、mock 数据三方已对齐，简要检查清单均已标 ✅。  
**目标**：Flutter model、后端 DTO、mock 数据三方最终一致。  
**验收标准**：Flutter dev 环境调用后端 API 时，无解析异常、无字段歧义、无双标准。

### 2. Refresh Token 持久化与吊销

**现状**：已完成。`RefreshTokenService` 已实现基于 Redis 的 Refresh Token 存储、校验、单个吊销和全部吊销。  
**目标**：将 Refresh Token 持久化到 Redis，支持主动吊销。  
**验收标准**：
- [x] 修改密码或注销后，旧 refresh token 失效。
- [x] `RefreshTokenService.revokeAllRefreshTokens()` 支持一键清除用户所有会话。

### 3. delete-account 软删除修复

**现状**：已完成。`UserService.deleteAccount()` 已改为软删除（`setDeleted(true)` + 释放邮箱/用户名唯一索引），userId=1 种子用户受保护。  
**目标**：改为软删除，并避免种子用户数据被误删。  
**验收标准**：userId=1 不会被硬删除，已删除用户默认不再出现在查询结果中。

### 4. ORM 架构升级（从 JPA 迁移至 MyBatis-Plus 3.5.7）

**现状**：已完成。全工程全面舍弃 Hibernate/JPA，无缝重构为 **MyBatis-Plus (v3.5.7)**。
- 依赖替换：完全移除 `spring-boot-starter-data-jpa`，接入 `mybatis-plus-spring-boot3-starter:3.5.7`、`spring-boot-starter-jdbc` 与 `spring-boot-starter-aop`。
- 实体改造：7 个实体类全部迁移为 MyBatis-Plus 规范注解（`@TableName`、`@TableId(type = IdType.AUTO)`、`@TableLogic`、`@TableField`）。
- Mapper 数据访问层：创建 7 个 `BaseMapper<T>` 接口，管理子表关联（`user_certifications`、`project_tech_stack`、`skill_items`、`stat_tags`）。
- 业务逻辑层：`UserService`、`AuthService`、`ProjectService`、`AboutMeService` 全面使用强类型 `LambdaQueryWrapper`，彻底根除 JPA N+1 与 Open-Session-In-View 性能陷阱。
- 测试保障：369 个单元与集成测试全部 100% 绿色通过，自动生成 `schema-h2.sql` 支持纯内存 H2 隔离运行。

### 5. GitHub Actions CI

**现状**：已成功搭建，包含编译校验、单元测试、JaCoCo 报告以及基于 SSH / SCP 的 AWS EC2 自动构建部署流。已加入 `ConnectTimeout 30` 与 `ConnectionAttempts 5` 超时重试防抖机制。  
**目标**：建立最小可信 CI：测试 + JaCoCo + SpotBugs + 自动 CD 部署。  
**为什么现在做**：保证 Push / PR 自动完成防抖校验与部署上线。  
**验收标准**：
- [x] Push / PR 自动触发校验；README 挂载状态。
- [x] 增加 SSH `ConnectTimeout` 与 `ConnectionAttempts` 避免 EC2 瞬态握手超时。

### 6. 头像数据安全防腐与图片魔数深度校验 (Avatar Payload Security)

**现状**：已完成。已在 Controller 与 Service 层建立多级防护，彻底解决异常/恶意 Base64 Payload 导致客户端 UI 假死问题。  
**目标**：对上传的头像数据进行体积限制、Base64 格式校验与底层魔数深度检测，拒绝伪造损坏数据入库。  
**验收标准**：
- [x] `Constants.java` 增加标准错误码 `ERR_INVALID_IMAGE = "BIZ_0507"`。
- [x] `UserService.isValidAvatarData` 校验 Base64 长度（<= 3MB）、Data URI 前缀、Base64 解码有效性、二进制体积（<= 2MB）及图片魔数白名单（PNG/JPEG/GIF/WebP/SVG）。
- [x] `UserController.uploadAvatar` 预拦截并返回 HTTP 400 及 `BIZ_0507`，种子管理员账户（userId=1）鉴权保护。
- [x] 补充 `UserServiceTest` 与 `UserControllerTest` 全量单元测试，生产环境验证通过。

### 7. 云端 Docker 孤儿构建层自动深度清理引擎 (Automated Docker GraphDriver Cleanup)

**现状**：已完成。针对 AWS EC2 `t2.micro` 8GB 磁盘在多次 CI/CD 构建后 `overlay2` 孤儿层堆积问题，开发并部署了拓扑反查清理引擎。  
**目标**：在不停止当前服务容器的前提下，精准识别并清除无引用的中间构建层。  
**验收标准**：
- [x] 编写 `tools/clean_docker_orphans.py`，通过 `docker inspect` 反查所有活跃容器/镜像依赖的 GraphDriver 目录树，安全物理清除未引用孤儿层。
- [x] 配置 `systemd/docker-cleanup.service` 与 `docker-cleanup.timer`，实现每日凌晨自动执行系统垃圾回收与日志轮转。
- [x] 在 CI/CD 流水线（`.github/workflows/ci.yml`）与本地部署脚本（`docker_deploy.ps1`）中完成挂接。

## Next

### 1. 测试补强与质量门禁落地

**现状**：已完成。全量测试已覆盖 Controller、Service、Mapper、Entity、AOP 切面、Security、JWT 与异常体系。
- **测试总览**: 全工程共 **369 个测试用例，33 个测试套件，100% 通过 (0 failures, 0 errors, 0 skipped)**。
- **覆盖率指标**: JaCoCo 行覆盖率 **92.05%** (1,367/1,485 行)，指令覆盖率 **92.30%** (5,347/5,793)，方法覆盖率 **96.74%** (386/399)，类覆盖率 **95.12%** (39/41)。
- **测试隔离**: 引入 `BaseIntegrationTest` 与 `EmbeddedRedisTestConfig`，实现 H2 纯内存库与内嵌 Redis 隔离，单测无需外部中间件依赖。
**目标**：关键链路全面覆盖，JaCoCo 报告具备高可信度度量。  
**验收标准**：
- [x] 新增 `AuthControllerRefreshTest`、`AuthServiceTest`、`ProjectServiceTest` 等核心用例
- [x] 全局单元测试 100% 绿色通过，行覆盖率突破 90% 基线要求

### 2. V2 迁移脚本与 Flyway 整合

**现状**：已完成。Flyway 脚本精简整合为 `V1`（纯 DDL 建表，含多语言扩展字段与 `LONGTEXT`）与 `V2`（纯 DML 填充中/英/日三语简历与项目测试数据）。  
**目标**：为真实简历数据更新提供 Flyway 迁移脚本。  
**验收标准**：执行 V1 与 V2 脚本后与当前真实简历及多语言数据保持一致。

### 3. 限流算法升级

**现状**：当前为 Redis INCR 固定窗口。  
**目标**：如确有必要，再升级为滑动窗口或令牌桶。  
**验收标准**：只有在真实需要更高精度时再推进，不为“概念更高级”而升级。

### 4. 数据库动态内容国际化与 App 拦截器解耦 (ListenCore 0.0.49)

**现状**：已完成。后端多语言解析、表结构扩展（`_zh` / `_ja`）与 App 端拦截器请求头重构已全部落地。ListenCore 升级至 `0.0.49`，实现了 `onInjectAuthHeader`（仅注入 Authorization）与 `onInjectCommonHeaders`（无条件注入 Accept-Language）的完全解耦。  
**目标**：实现后端多语言字段的解耦与 Locale 动态分发及 App 拦截器职责分离。  
**验收标准**：
- [x] 编写 Flyway Migration 迁移脚本（整合为 `V1` 建表与 `V2` 测试数据），为 `users`、`projects`、`experiences`、`education`、`languages` 表添加 `_zh` / `_ja` 多语言列
- [x] 后端 Java Entity 扩展多语言属性及 getter/setter 映射
- [x] Service 业务层引入 Locale 动态解析（利用 `LocaleContextHolder` 与 `I18nUtils`），实现 DTO 的对应语言文本自动转换装配
- [x] 注册 `AcceptHeaderLocaleResolver` 与拦截器处理客户端 Dio 传入的 `Accept-Language` 请求头
- [x] ListenCore 升级 `0.0.49`，解耦 `onInjectCommonHeaders` 拦截器，确保公开/访客接口（如 `/v1/projects`）也注入 `Accept-Language`
- [x] 核心技能 6 大维度多语言支持落地：扩展 `skills`（`category_zh`, `category_ja`, `score`）与 `skill_items`（`item_name_zh`, `item_name_ja`）多语言表结构，创建 `SkillItemEntity` 并通过 `I18nUtils` 动态解析 Locale，单测 100% 跑通并发布上线

### 5. Nginx 反向代理与 Let's Encrypt HTTPS 部署

**现状**：已在 AWS EC2 上顺利安装并启动 Nginx，并已向 `is-a.dev` 提交 `listen2code.is-a.dev` 免费域名 PR。  
**目标**：实现隐藏 8080 端口、端口转发与全站 SSL/HTTPS 安全通信加密。  
**验收标准**：
- [x] 在 AWS EC2 服务器上安装与启动 Nginx 服务
- [ ] 待 `is-a.dev` 免费域名 `listen2code.is-a.dev` PR 审核合并生效
- [ ] 配置 `/etc/nginx/conf.d/portfolio.conf` 将 80 端口隐藏代理至容器 8080 端口
- [ ] 使用 Certbot 命令行自动向 Let's Encrypt 申请并配置 HTTPS 证书及自动续签 Cron

### 6. 架构演进与不足改善 (Architecture Improvements & Limitations)

基于对当前后端生产实现的深入审计，识别出以下 5 项关键优化点：

- [ ] **1. Redis 限流原子性与窗口突刺优化 (Rate Limit Lua Script & Sliding Window)**
  - **现状**：`RateLimitService` 采用 Redis `INCR` 配合 `count == 1` 时调用 `expire`。
  - **隐患**：非原子操作，若在 `INCR` 与 `expire` 之间发生进程崩溃或网络闪断，可能导致该 key 永久不过期；且固定时间窗口在边界时刻存在最高 2x 突发流量击穿隐患。
  - **改进方案**：改用 Redis Lua 脚本实现原子的 `INCR + EXPIRE`，或升级为基于 Redis ZSet 的滑动窗口/令牌桶算法。
- [ ] **2. Refresh Token 批量吊销 `KEYS` 阻塞消除 (`KEYS` vs `SCAN` / User Token Set)**
  - **现状**：`RefreshTokenService.revokeAllRefreshTokens` 采用 `redisTemplate.keys("token:refresh:" + username + ":*")` 模式匹配。
  - **隐患**：Redis 单线程模型下，`KEYS` 命令属于 $O(N)$ 全局阻塞命令，在大规模用户/海量 key 生产环境下会导致 Redis 瞬时不可用。
  - **改进方案**：引入用户维度的活跃 Token 索引集合（如 `token:user:<username>` Set 结构），或将通配删除改为非阻塞的 `SCAN` 游标迭代。
- [ ] **3. ORM 关联子表查询 N+1 性能优化 (Batch Query for Sub-collections)**
  - **现状**：`ProjectService.getProjects()` 在 `selectList` 后通过循环逐项调用 `projectMapper.findTechStackByProjectId(id)`；`AboutMeService` 也对统计标签和技能细项存在循环单查。
  - **隐患**：典型的 N+1 查询隐患，当项目或工作经历增多时，会产生 $O(N)$ 次数据库往返，增加数据库连接池与网络延迟。
  - **改进方案**：改用 `WHERE project_id IN (...)` 单次批量拉取全量子表记录，再利用 Java Stream `Collectors.groupingBy` 在应用内存中装配，降至 $O(1)$ 次往返。
- [ ] **4. Base64 头像存储膨胀与 SVG 安全防腐 (Avatar Storage S3 & SVG Sanitization)**
  - **现状**：头像 Base64 字符串直接持久化于 MySQL `users.avatar_url` (LONGTEXT) 字段中；SVG 仅通过文本头做基础匹配。
  - **隐患**：最大 2MB 的 Base64 图片会导致 InnoDB 行数据过大，引发频繁页分裂和 Buffer Pool 缓存污染；SVG 属于 XML 结构，可包含 `<script>` 或恶意实体，存在潜在 Stored XSS 风险。
  - **改进方案**：将头像上传改造为直传或代理上传至对象存储（如 AWS S3 / Cloudflare R2 / MinIO），数据库仅保存轻量 CDN URL；对 SVG 引入安全解析器或 DOMPurify 过滤脚本代码。
- [ ] **5. OpenAPI / Swagger 规范契约完善**
  - **现状**：部分 Controller 尚未标注全局 `@Parameter(name = "Accept-Language")` 及标准化 4xx/5xx 响应 Schema。
  - **改进方案**：利用 SpringDoc 完善注解标注，提供支持直接切换语言与测试鉴权的交互式 Swagger UI。
- [ ] 在 Flutter `env_config.dart` 中将 prod 环境 BaseUrl 更新为 `https://listen2code.is-a.dev`

### 7. AWS 运维与容器基础设施演进 (AWS DevOps & Infrastructure Improvements)

基于对 AWS EC2 `t2.micro` 生产拓扑与自动化运维链路的深度审计，识别出以下 4 项关键运维增强点：

- [ ] **1. AWS 弹性 IP (EIP) 绑定与安全组端口收敛 (Elastic IP & Security Group Hardening)**
  - **现状**：当前 EC2 使用动态公网 IP（`13.218.192.181`），若实例遭遇重启或底层硬件迁移，公网 IP 存在变动风险。此外，安全组对外暴露了部分内部组件端口。
  - **改进方案**：申请并绑定 AWS 弹性公网 IP (EIP) 固化域名解析；安全组仅对外开放 80 (HTTP)、443 (HTTPS) 与指定白名单 IP 的 22 (SSH)，将 8080 (App)、3307 (MySQL)、6379 (Redis)、9090 (Prometheus)、3000 (Grafana) 彻底限制在 Docker 内部桥接网络或仅允许本地回环 `127.0.0.1` 访问。
- [ ] **2. 1GB 内存约束下的 Linux Swap 交换分区与 OOM 保护 (Swap & OOM Protection)**
  - **现状**：在单核 1GB RAM 上同时承载 Spring Boot (JVM 256MB) + MySQL 8.0 (InnoDB) + Redis + Prometheus + Grafana + Nginx，内存常态使用率高达 85%~90%。
  - **隐患**：在并发高或 Docker build 期间，极易触发 Linux 内核 OOM Killer 随机杀死数据库或应用主进程。
  - **改进方案**：配置 2GB 宿主机 Swap 虚拟内存交换分区 (`/swapfile`) 并调整 `vm.swappiness=10`；为核心容器（`db`, `app`）配置 `oom_score_adj: -500` 防止被内核优先强杀。
- [ ] **3. 蓝绿双容器交替发布与零停机平滑更新 (Zero-Downtime Deployment)**
  - **现状**：当前 CD 部署先执行容器构建与重启，在 Spring Boot 启动初始化的 15~25 秒期间，Nginx 反向代理会向客户端返回 `502 Bad Gateway`。
  - **改进方案**：采用蓝绿交替端口部署模式（如 `app-blue:8080` 与 `app-green:8081`）。新版本容器拉起并通过健康探针后，动态修改 Nginx upstream 并执行 `nginx -s reload`，实现客户端请求零感知无缝割接。
- [ ] **4. MySQL 数据库定时云端异地灾备与 S3 归档 (Automated DB Backup to S3)**
  - **现状**：数据库数据卷仅依赖单一 EBS 块存储卷（`portfolio_db_data`），缺乏定期的异地灾备机制。
  - **改进方案**：编写自动化定时脚本通过 `docker exec portfolio-db-1 mysqldump` 导出压缩 SQL，并通过 AWS CLI 归档至私有 S3 存储桶，配合 Lifecycle 规则保留最近 30 天快照。

### 8. Docker 容器架构与镜像构建深度优化 (Docker Infrastructure & Image Improvements)

基于对当前 Dockerfile、docker-compose.yml 以及容器生命周期管控机制的审计，规划以下 4 项容器化架构改进：

- [ ] **1. Dockerfile 多阶段构建与最小化 JRE 自定义裁剪 (Multi-Stage Build & jlink)**
  - **现状**：当前 `Dockerfile` 依赖外部在宿主机先行打包好 WAR 文件（`COPY target/portfolio-0.0.1-SNAPSHOT.war app.war`），若没有预先执行 `gradlew bootWar` 则构建失败；基础镜像为通用的 `eclipse-temurin:17-jre-alpine` (~140MB)。
  - **改进方案**：引入标准化 Docker 多阶段构建（Multi-Stage），第一阶段使用带有 Gradle/JDK 的镜像编译打包，第二阶段使用 `jlink` 仅提取 Spring Boot 所需的 Java 核心模块（如 `java.base`, `java.sql`, `java.naming`, `java.desktop` 等），制作定制精简运行时，可将基础镜像体积压至 50MB~80MB，并摆脱对宿主机 Gradle 预构建的依赖。
- [ ] **2. 生产环境非 root 用户运行容器加固 (Non-Root User Enforcement)**
  - **现状**：当前 `Dockerfile` 默认以 Alpine root 用户身份执行 `ENTRYPOINT`，存在潜在的容器逃逸与权限扩大安全隐患。
  - **改进方案**：在 Dockerfile 中通过 `adduser -D -s /bin/sh -u 10001 appuser` 创建无特权系统用户，并配置 `USER appuser`，遵循最小特权容器安全规范（CIS Docker Benchmark）。
- [ ] **3. docker-compose.yml 增加资源配额硬限制 (Resource Limits & cgroups)**
  - **现状**：`docker-compose.yml` 中除 JVM `JAVA_OPTS` 参数外，未对容器声明 cgroups 级别的内存与 CPU 配额限制（`deploy.resources.limits`）。
  - **隐患**：若 Spring Boot 发生堆外内存（Metaspace / DirectBuffer / Native Thread）泄漏，可能耗尽整机全部可用内存，拖垮同机的 MySQL 和 Redis。
  - **改进方案**：为 `app` 明确配置 `mem_limit: 450m`、`cpus: '0.8'`，为 `redis` 配置 `mem_limit: 64m`，为 `db` 配置 `mem_limit: 350m`，实现各容器物理隔离，避免单容器资源饥饿引发雪崩。
- [ ] **4. 生产环境镜像推送与私有 Registry 镜像版本追溯 (Container Registry & Semantic Tagging)**
  - **现状**：本地与服务器直接使用本地 build 生成镜像，未推送到统一的容器镜像仓库（如 Amazon ECR 或 Docker Hub），缺乏镜像历史快照与安全漏洞静态扫描。
  - **改进方案**：集成 GitHub Actions 构建并自动推送打标镜像（`portfolio-app:v1.0.X` 与 `commit-sha`）到私有 Registry，拉取部署时支持快速版本回滚与 Trivy 容器漏洞合规扫描。

### 9. Flyway 数据库迁移演进与多环境隔离 (Flyway Migration Architecture Improvements)

基于对当前 FlywayConfig 实现、迁移脚本（V1/V2）及生产部署生命周期的审计，规划以下 4 项演进点：

- [ ] **1. 生产环境种子数据与结构脚本解耦 (Separate DDL from DML / Seed Data)**
  - **现状**：当前 `V2__Add_test_data.sql` 包含了真实的 Listen 履历数据以及测试用户（Listen2）的种子数据，直接作为核心迁移版本纳入统一演进。
  - **隐患**：在不同客户或多租户/私有化交付场景下，测试数据强行随结构一同灌入，缺乏灵活性；且 DML（INSERT）脚本体积达 20KB+，增大了历史回滚难度。
  - **改进方案**：将版本迁移划分为纯 DDL 迁移（`V1__Initial_Schema.sql`, `V3__Add_Column_X.sql`）与按环境加载的 Repeatable 脚本（`R__01_Seed_Data.sql`），或通过 Spring `ApplicationRunner` / Profile（如 `@Profile("demo | dev")`）独立控制种子数据的初始化。
- [ ] **2. 数据库迁移回滚与降级机制规划 (Flyway Undo / Down-Migration Strategy)**
  - **现状**：当前开源版本 Flyway 仅支持单向前进迁移（`migrate`），缺乏逆向回滚脚本机制（`U1__Undo_*.sql` 为商业版特性）。
  - **隐患**：若生产发布后新版本存在隐蔽严重缺陷，缺乏自动化执行逆向 DDL 的降级方案，只能依赖整库冷备还原。
  - **改进方案**：在开发团队规范中建立配对的 `rollback/` 逆向 SQL 归档，并在 CI/CD 发布流程中固化“发布前自动快照 + 失败一键执行 rollback 脚本”的应急保障 SOP。
- [ ] **3. 分布式多节点启动下的 Flyway 迁移互斥锁与超时治理 (Migration Lock & Timeout)**
  - **现状**：在单实例容器场景下 FlywayConfig 运行良好；但若后续扩展为集群部署（多 Pod 或蓝绿多节点并发启动），各节点可能同时争抢创建 `flyway_schema_history` 表并执行迁移。
  - **改进方案**：明确在 Flyway 配置中调优数据库级排他锁超时时间（`lockRetryCount: 50`），并确保仅由主节点（或 CI/CD 独立的一次性 Migration Job 容器）先行执行迁移，应用节点仅负责校验版本就绪。
- [ ] **4. 集成测试环境纯内存 H2 自动化表结构同步校验 (Automated H2 Schema Sync with Flyway DDL)**
  - **现状**：目前 `application-test.properties` 采用 `@Profile("!test")` 完全绕过了 Flyway，依靠手工维护的 `schema-h2.sql` 或 Hibernate 生成建表。
  - **隐患**：当未来新增 `V3__...sql` 修改字段时，若开发者遗漏同步修改 `schema-h2.sql`，集成测试不会报错，但在生产真实 MySQL 环境中会发生线上崩盘。
  - **改进方案**：在 Gradle 构建流水线中加入 Testcontainers MySQL 容器级“全量迁移一致性校验测试”，确保每一次 PR 都会在真实 MySQL 容器中执行全部 `V*__*.sql`，做到真正的生产同构验证。

### 10. JaCoCo 测试覆盖率与质量门禁演进 (JaCoCo Quality Gate & Coverage Improvements)

基于对当前 JaCoCo 配置、覆盖率报表产出及持续集成验证的审计，规划以下 4 项质量门禁增强点：

- [ ] **1. JaCoCo 自动化质量门禁 (jacocoTestCoverageVerification) 强阻断机制**
  - **现状**：当前 `build.gradle` 仅配置了 `jacocoTestReport` 生成报表，未启用 `jacocoTestCoverageVerification` 任务；若测试覆盖率下降或跌破红线，构建依然返回 SUCCESS。
  - **改进方案**：在 `build.gradle` 中增加 `jacocoTestCoverageVerification` 规则，设置硬性红线门禁（例如：全工程指令覆盖率 Instruction >= 70%、核心 `com.listen.portfolio.service.*` 分支覆盖率 Branch >= 60%），当 PR 新代码未补单测导致覆盖率下跌时直接阻断 Gradle 构建。
- [ ] **2. JaCoCo 报表排除生成代码与基础设施类 (Exclusions: DTO, Entity, Config, AOP)**
  - **现状**：覆盖率统计将 `dto/`、`entity/`（主要是 Getter/Setter/Lombok 生成逻辑）以及 `config/`、`aspect/` 计入分母，导致核心业务覆盖率数据被数据传输对象稀释或拉低。
  - **改进方案**：在 `jacocoTestReport` 与 `jacocoTestCoverageVerification` 中通过 `afterEvaluate` 配置 `classDirectories.setFrom(files(classDirectories.files.collect { fileTree(dir: it, exclude: ['**/entity/**', '**/dto/**', '**/config/**', '**/*Application*']) }))`，将统计重心完全聚焦于 Controller、Service 与核心工具类。
- [ ] **3. GitHub Actions CI 流水线 PR 自动化覆盖率评论与徽章展示 (CI PR Coverage Comment & Badge)**
  - **现状**：目前开发者需手动执行 `check-coverage.bat` 并在本地浏览器查看 HTML 报表，GitHub PR 页面缺乏直观的增量代码覆盖率反馈。
  - **改进方案**：集成 GitHub Action（如 `madrapps/jacoco-report` 或 Codecov），在每个 Pull Request 提交时自动解析 `jacocoTestReport.xml`，计算当前 PR 增量覆盖率并在 PR 讨论区自动回复分析表格与覆盖率变化差异。
- [ ] **4. 边缘异常分支与复杂逻辑边界单测补全 (Edge-Case & Boundary Coverage)**
  - **现状**：目前全量覆盖率已达到良好水平，但在部分异常分支（如 JWT 畸变解包、Redis 连接超时降级回退、并发重置密码时效窗口边缘情况）仍存在未覆盖的分支判断。
  - **改进方案**：利用 Mockito 模拟底层极端 I/O 故障，针对性补充边界测试用例，将业务核心服务层分支覆盖率提升至 80% 以上。

### 11. Redis 分布式架构与缓存治理演进 (Redis Architecture & Cache Governance)

基于对当前 RedisConfig、四个核心 Redis 服务模块以及高并发容灾场景的审计，规划以下 4 项架构演进点：

- [ ] **1. 全局吊销扫描 O(N) 阻塞根除与二级索引集合优化 (Eradicate KEYS in revokeAllRefreshTokens)**
  - **现状**：当前 `RefreshTokenService.revokeAllRefreshTokens(username)` 采用 `redisTemplate.keys("token:refresh:" + username + ":*")` 命令遍历匹配 Key。
  - **隐患**：Redis 属于单线程事件驱动模型，`KEYS` 命令在生产环境海量 Key 场景下属于全字典 $O(N)$ 扫描，会阻塞 Redis 主线程数毫秒至数秒，引发雪崩式请求超时。
  - **改进方案**：维护二级索引 Set 数据结构 `user:refresh_tokens:<username>`，将每个新签发的 Token 存入该 Set 并同步设置 TTL；执行全局吊销时直接读取该 Set 并批量删除，将检索复杂度从 $O(N)$ 彻底降至 $O(1)$。
- [ ] **2. 分布式限流从固定窗口升级为 Lua 脚本滑动日志/令牌桶 (Sliding Window Rate Limiter via Lua)**
  - **现状**：`RateLimitService` 目前使用基于固定时间窗口（`INCR + EXPIRE`）的简易限流。
  - **隐患**：在固定时间窗口交界处（如第 59 秒和第 61 秒之间）可能遭遇双倍突发流量冲击（临界双倍阈值攻击）；且 `INCR` 与 `EXPIRE` 为两次独立网络请求，若在两步之间发生服务器意外宕机，会导致 Key 永久丢失 TTL。
  - **改进方案**：引入 Redis 原子 Lua 脚本，实现基于 ZSET 的滑动时间窗口算法或令牌桶（Token Bucket）算法，确保请求计数、时间戳修剪与过期设置在 Redis 服务端单次原子完成。
- [ ] **3. 业务数据只读多级缓存与 Cache-Aside 模式引入 (Spring Cache / Redis Multi-Layer)**
  - **现状**：目前 Redis 仅作为黑名单、Token 凭据与限流计数器使用，核心高频只读 API（如 `/api/v1/projects`、`/api/v1/about-me`）每次均直查 MySQL 数据库。
  - **改进方案**：引入 Spring Cache 注解（`@Cacheable`, `@CacheEvict`）与 Redis 二级缓存，对静态履历与公开作品列表实施多语言键缓存（如 `cache:projects:zh`, `cache:about_me:ja`），配置 TTL 2 小时，大幅减轻 MySQL 负载并提升 API 响应吞吐至 5ms 以内。
- [ ] **4. 生产环境 Redis 连接密码认证与物理持久化策略调优 (Redis Auth & RDB/AOF Optimization)**
  - **现状**：目前 Docker 内部 Redis 未配置 `requirepass` 访问密码；持久化仅采用默认快照，在机器意外断电重启时可能丢失最近数分钟的黑名单与会话数据。
  - **改进方案**：在 `docker-compose.yml` 中配置 `requirepass ${REDIS_PASSWORD}`；开启 AOF 持久化（`appendonly yes`，`appendfsync everysec`），保障黑名单与持久化 Refresh Token 的高可靠不丢失。

### 12. SpotBugs 静态代码安全分析与质量门禁演进 (SpotBugs Static Analysis & Security Auditing)

基于对当前 SpotBugs 配置、`spotbugs-exclude.xml` 过滤规则及 CI 阻断策略的审计，规划以下 4 项质量治理增强点：

- [ ] **1. SpotBugs CI 门禁分级阻断机制 (Tiered Quality Gate: ignoreFailures Strategy)**
  - **现状**：`build.gradle` 中设置了 `spotbugs.ignoreFailures = true`，当检测到高危 Bug 时构建仍显示 SUCCESS，缺乏硬性质量卡点。
  - **改进方案**：引入分级治理机制——在本地开发维持宽松告警，而在 CI/CD 生产构建流水线中对“高危安全漏洞（Security）与致命坏味道（Scariest Bugs，Rank 1~4）”强制开启 `ignoreFailures = false`，一旦引入未通过审查的高危缺陷直接阻断合并。
- [ ] **2. 引入 Find Security Bugs 插件深度扫描 OWASP Top 10 (Find Security Bugs Plugin)**
  - **现状**：当前 SpotBugs 仅加载核心规则库，对 Web 安全专属漏洞（如 SQL 注入深层溯源、XSS 存储型跨站、SSRF、不安全反序列化、密码硬编码）的识别深度有限。
  - **改进方案**：在 Gradle 中集成 `com.h3xstream.findsecbugs:findsecbugs-plugin:1.13.0` 扩展插件，对 REST API 控制器、密码加密处理、JWT 解析与文件/头像上传实施 OWASP Top 10 全面安全专项巡检。
- [ ] **3. spotbugs-exclude.xml 过滤规则精准化与现代化维护 (Precise Exclusion Maintenance)**
  - **现状**：当前排除规则中存在较宽泛的模式匹配（如 `Class name="~.*\$.*"` 屏蔽了所有内部类）。
  - **隐患**：可能会意外漏掉开发人员手写的复杂内部类中的并发安全隐患或空指针缺陷。
  - **改进方案**：利用注解 `@SuppressFBWarnings` 替代宽泛的全局 XML 正则排除，将压制声明精确收敛到具体的类或方法级别，并强制要求注明压制原因。
- [ ] **4. PR 自动化 SpotBugs 增量审查报告机器人集成 (Automated PR Bug Review Bot)**
  - **现状**：开发者需手动运行 `check-spotbugs.bat` 查看 HTML 报表，代码评审时难以直观定位新增代码引入的缺陷。
  - **改进方案**：在 GitHub Actions 流水线中解析 `build/reports/spotbugs/main.xml`，利用 GitHub Check Runs API 在代码行内自动留下行内 Warning 批注（Review Comments），实现研发流无缝质量卡点。

### 13. AWS 云上生产架构与自动化流水线演进 (AWS Production Architecture & GitOps)

基于对当前 EC2 `t2.micro` 单机拓扑、GitHub Actions CI/CD 流水线以及多云托管方案的审计，规划以下 4 项云原生演进增强点：

- [ ] **1. 基于 AWS Systems Manager (SSM) 的零开放端口安全通信 (SSM Session Manager)**
  - **现状**：CI/CD 部署与管理员登录依赖安全组开放 22 端口（`0.0.0.0/0`）并暴露在公网，存在持续性的端口扫描与探测威胁。
  - **改进方案**：在 EC2 上安装并激活 AWS Systems Manager Agent，绑定 `AmazonSSMManagedInstanceCore` IAM 角色；全面关闭安全组入站 22 端口，改由 GitHub Actions 通过 `aws ssm start-session` / AWS CLI 建立安全通道，实现零公网入站端口暴露的极致内网隔离。
- [ ] **2. 静态资源 CDN 加速与 AWS CloudFront / S3 动静分离 (CloudFront Static Acceleration)**
  - **现状**：当前 Flutter Web 编译产物全部托管在 EC2 宿主机的 Nginx 目录（`/var/www/listen_portfolio_web`）下，图片与 CanvasKit Wasm 资源加载直接消耗单核 EC2 的下行带宽。
  - **改进方案**：将 Flutter Web 构建产物部署至私有 AWS S3 存储桶，挂载 AWS CloudFront 全球边缘节点 CDN，实现全球毫秒级静态资源缓存与 HTTPS 加速，仅将 `/api/*` 动态流量反向代理回源至 EC2 / ALB。
- [ ] **3. GitHub Actions 部署后自动化全链路烟雾测试与自愈回滚 (Automated Smoke Test & Rollback)**
  - **现状**：CI 流水线执行完 `docker compose up -d` 后即标记为 Success，若应用在启动期间因环境变量缺失或数据库锁崩溃，流水线无法感知线上坏死。
  - **改进方案**：在 deploy 任务后追加基于 `curl` 的冒烟探测步骤（轮询 `/actuator/health` 与 `/api/v1/projects` 200 OK 校验），若探测超时或返回 5xx，流水线自动触发自愈回滚逻辑（恢复上一版本 WAR 并重启容器）并通过飞书/Slack 发送紧急告警。
- [ ] **4. 生产环境向无服务器容器 AWS ECS Fargate 平滑迁移评估 (ECS Fargate Migration Readiness)**
  - **现状**：单台 EC2 虚机存在硬件宕机单点故障，且需长期承担 Linux 内核补丁、Swap 监控与磁盘清理运维。
  - **改进方案**：完成 Terraform 或 CloudFormation 基础设施即代码（IaC）编排编写，将 Spring Boot App 封装为标准 ECS Task，搭配 AWS RDS Aurora Serverless v2 与 ElastiCache for Redis，实现具备生产级自动水平伸缩（Auto Scaling）与 99.99% 高可用架构。

### 14. 本地研发体验与环境隔离演进 (Developer Experience & Tooling Improvements)

基于对当前本地开发双轨端口（3306 vs 3307）、IDE 调试体验及跨平台脚本的审计，规划以下 4 项研发体验增强点：

- [ ] **1. Spring Boot DevTools 热加载与极速重启支持 (Spring Boot DevTools Integration)**
  - **现状**：在本地修改 Java 代码或 XML Mapper 后，开发者必须手动重新编译并重启整站进程，调试效率受限。
  - **改进方案**：在 `build.gradle` 中以 `developmentOnly 'org.springframework.boot:spring-boot-devtools'` 引入开发工具包，启用类加载器双层隔离，实现秒级代码热替换与资源文件静默重载。
- [ ] **2. Docker Compose 本地按需开发编排拆分 (docker-compose.dev.yml)**
  - **现状**：目前 `docker-compose.yml` 混合了本地 App 容器与基础设施容器（db, redis, prometheus, grafana），当开发者只想本地 IDE 直跑 Java 代码、仅借用 Docker 数据库时，启动命令较为冗长。
  - **改进方案**：引入 `docker-compose.infra.yml`（仅包含 MySQL + Redis），并提供快捷启动命令 `docker compose -f docker-compose.infra.yml up -d`，将宿主机与容器端口保持一致或提供自动端口探测。
- [ ] **3. 跨平台开发统一启动与环境探测脚本 (Make / Just / Bash CLI)**
  - **现状**：目前一键启动脚本主要由 Windows PowerShell（`.ps1`）和批处理（`.bat`）编写，Mac/Linux 开发者需要手动分步输入命令。
  - **改进方案**：编写跨平台的 `Makefile` 或 `justfile`（如 `make dev`, `make test`, `make docker-up`），统一全平台开发运维的指令入口。
- [ ] **4. 统一代码格式化与 Git Pre-commit 静态卡点 (Spotless / Pre-commit Hook)**
  - **现状**：代码风格目前依赖 IDE 自带格式化器，团队协作时不同 IDE（IDEA、VS Code、Android Studio）换行与缩进差异容易在 Git 产生无意义的格式差异 Diff。
  - **改进方案**：集成 Gradle Spotless 插件（Google Java Format），并在 Git 中配置 Pre-commit Hook，在代码 `git commit` 时自动执行格式化与 SpotBugs 增量检查。

### 15. 域名解析、Nginx 网关与 SSL 安全治理演进 (Domain, Nginx Gateway & SSL Governance)

基于对当前 `listen2code.is-a.dev` 免费二级域名、Let's Encrypt Certbot 证书管理以及宿主机 Nginx 网关配置的审计，规划以下 4 项网关与安全演进点：

- [ ] **1. Nginx SSL 密码套件与 HTTP/2 / TLS 1.3 现代安全加固 (TLS 1.3 & HTTP/2 Hardening)**
  - **现状**：目前 Certbot 注入了基础的 SSL 配置，但未显式启用 HTTP/2 协议多路复用，且 SSL 会话复用与安全响应头（HSTS、CSP、X-Frame-Options）尚未完整配置。
  - **改进方案**：在 Nginx `listen 443 ssl` 增加 `http2` 协议支持，大幅降低 Flutter Web 大量小图标与 JS 静态切片并发下载延迟；配置 `ssl_protocols TLSv1.2 TLSv1.3;` 与完善 `Strict-Transport-Security: max-age=31536000; includeSubDomains` 安全头，达成 SSL Labs A+ 级安全评分。
- [ ] **2. 商业顶级独立域名与 AWS Route 53 托管迁移规划 (Custom Apex Domain & Route 53)**
  - **现状**：当前使用的 `is-a.dev` 为由开源社区维护的免费第三方子域名，DNS 解析依赖社区 GitHub 审批流，缺乏企业级 SLA 可用性保障，且无法签发 Wildcard 泛域名证书。
  - **改进方案**：采购商业顶级独立域名（如 `listen2code.dev` 或 `listen.dev`），托管至 AWS Route 53 DNS 解析，配置 Alias 别名记录与故障转移健康检查，提升个人技术品牌专业度与基础设施抗风险能力。
- [ ] **3. Certbot 定时证书自动续签与 Nginx 平滑重载 Systemd Timer 固化 (Certbot Auto-Renew Timer)**
  - **现状**：目前依赖 Certbot 默认生成的任务，缺乏独立的日志持久化与续签失败自动化告警（Let's Encrypt 证书 90 天有效）。
  - **改进方案**：配置专用的 `certbot-renew.service` 与 `certbot-renew.timer`，每周执行 `certbot renew --quiet --deploy-hook "systemctl reload nginx"`，并将续签状态上报至系统监控或邮箱通知，彻底杜绝证书到期红脸事故。
- [ ] **4. Nginx 网关层防 DDoS 与突发流量限速限流 (Nginx Rate Limiting & Burst Control)**
  - **现状**：目前限流主要依赖 Spring Boot 应用层的 AOP 注解（`RateLimitAspect` + Redis）；当遭遇大规模 HTTP Flood 洪水攻击时，流量已打入 Tomcat 内部，仍会消耗大量 JVM 线程池资源。
  - **改进方案**：在宿主机 Nginx 层配置 `limit_req_zone $binary_remote_addr zone=api_limit:10m rate=10r/s;`，并在 `/api/` 路由下设置 `limit_req zone=api_limit burst=20 nodelay;`，将恶意高频扫描直接在宿主机网关层以 503 快速阻断，有效保护后方微服务容器。

### 16. 邮件服务架构演进与生产可靠性治理 (Email Service Architecture & Reliability Governance)

基于对当前基于 Spring Mail、Thymeleaf 与 Redis 的邮件验证码/密码重置链路的审计，规划以下 4 项生产可靠性演进增强点：

- [ ] **1. 邮件异步解耦发送与重试机制 (Asynchronous Dispatch via @Async / Redis Stream)**
  - **现状**：目前 `EmailService.sendPasswordResetEmail` 在 Web 请求主线程中同步阻塞执行。SMTP 协议握手、TLS 协商与网络 I/O 通常需要 1~3 秒，当外部邮件服务（如 Gmail/QQ）网络抖动或超时，会导致用户请求线程被长期挂起，极易耗尽 Servlet 容器线程池。
  - **改进方案**：引入 `@Async` 独立线程池异步执行邮件发送，或将发送任务投递至轻量级消息队列（Redis Stream / RabbitMQ），实现用户请求毫秒级即时响应与后台削峰解耦；同时结合指数退避算法（Exponential Backoff）实现网络异常自动重试机制。
- [ ] **2. 多通道邮件供应商平滑容灾降级 (Multi-Vendor Failover: AWS SES / SendGrid / 备用 SMTP)**
  - **现状**：当前系统只支持单一 SMTP 提供商配置（单点依赖 Gmail 或 QQ）。个人免费 SMTP 服务存在严格的日发送配额限制（如 Gmail 每日 100~500 封），并且容易因异地 IP 登录或风控被临时封禁，导致全站密码重置链路单点瘫痪。
  - **改进方案**：采用策略模式实现 `EmailProviderAdapter` 多通道架构，支持主通道（如 AWS SES / SendGrid API）与备用通道（如 163 / QQ SMTP）。当主通道出现配额用尽、认证失败或超时异常时，自动无缝降级至备用通道重试，并上报 Prometheus 告警指标。
- [ ] **3. 邮件发送审计日志持久化与单邮箱频次防护 (Email Audit Logging & Abuse Defense)**
  - **现状**：目前邮件投递仅输出控制台应用日志，未进行数据库持久化归档；虽然 Controller 层配置了基于 IP 与 EMAIL 的 AOP 短期限流（60 秒 10 次），但缺乏针对单邮箱的自然日总量限制（如同一邮箱 24 小时最多 5 封），存在被恶意当做垃圾邮件轰炸工具利用的合规风险。
  - **改进方案**：新建 `email_send_log` 审计流水表，记录收件人、邮件类型、模板版本、投递耗时、发送状态与关联 RequestId；在业务层引入基于 Redis 的“日发送限额”计数器与风控黑名单校验，全面强化发送行为的审计合规性与反滥用能力。
- [ ] **4. 邮件模板国际化 (i18n) 与移动端 Dark Mode 适配 (Template i18n & Dark Mode Adaptive)**
  - **现状**：当前 `password-reset-in-email.html` 模板文本内容硬编码为中文，不支持境外用户根据偏好语言渲染；且缺乏针对现代移动端邮件客户端（如 iOS Mail、Gmail App）深色模式（Dark Mode）的 CSS 媒体查询适配（`@media (prefers-color-scheme: dark)`），在深色背景下易导致部分区块反色异常或文字可读性差。
  - **改进方案**：集成 Thymeleaf 国际化机制（`#{message.key}` 结合用户偏好或请求头 `Accept-Language`），支持中/英/日多语言动态渲染；重构 HTML 模板 CSS，注入 Dark Mode 响应式样式与纯文本（Plain-text alternative）多部件降级方案，大幅降低被第三方邮箱判定为垃圾邮件（Spam Score）的概率。

### 17. 应用监控与可观测性体系深度治理 (Monitoring, Metrics & Observability Governance)

基于对当前 Spring Boot Actuator、Micrometer、Prometheus 与 Grafana 监控体系的审计，规划以下 4 项生产级可观测性演进增强点：

- [ ] **1. Actuator 监控端点网络安全隔离与鉴权 (Actuator Endpoint Security & Network Hardening)**
  - **现状**：目前 `/actuator/prometheus` 与 `/actuator/health` 在 Spring Security 中配置为公开放行（`permitAll()`）。若宿主机 Nginx 网关或安全组未配置反向代理过滤，外部黑客可直接访问拉取全量 Prometheus 指标，获知系统内部 API 路径结构、JVM 堆内存占用与系统错误率等敏感指纹信息。
  - **改进方案**：在 Nginx 宿主机层限制 `/actuator/**` 仅放行内网网段（如 `172.18.0.0/16`）及 Prometheus 容器访问（`allow 172.18.0.0/16; deny all;`）；或配置 Spring Boot 独立管理端口（`management.server.port=8081`）仅绑定至内网回环地址，实现业务流量与管理流量的物理隔离。
- [ ] **2. Prometheus 告警规则持久化与 Alertmanager 多通道通知闭环 (Alertmanager Notification Pipelines)**
  - **现状**：当前 `monitoring/prometheus.yml` 中的 `rule_files` 与 `alerting.alertmanagers` 配置均处于注释禁用状态，虽然文档列出了告警 PromQL 表达式，但无法在指标异常（如 API 响应时间 > 1s、5xx 错误率飙升、CPU 持续高于 80%、实例宕机等）时自动触发告警下发。
  - **改进方案**：固化创建 `monitoring/rules/portfolio-alerts.yml` 告警规则文件；在 Docker Compose 编排中引入轻量级 `prom/alertmanager` 容器服务，对接企业微信/钉钉/飞书群聊 Webhook 与管理员邮箱，形成“指标采集 -> 规则评估 -> 异常去重 -> 实时推送”的生产闭环。
- [ ] **3. Micrometer 业务级核心指标埋点与 Grafana 专用大盘 (Custom Business Metrics & Dashboard)**
  - **现状**：目前系统收集的指标全部局限于 Spring Boot 默认的框架级性能数据（HTTP 请求延时、JVM GC、系统 CPU），缺少真实业务维度的关键度量指标（如：用户登录成功/失败计数器 `user_login_total{status}`、密码重置邮件投递计数器 `email_sent_total{type,status}`、AOP 限流拦截计数器 `rate_limit_exceeded_total{endpoint}`）。
  - **改进方案**：在核心业务切面和业务服务中引入 `MeterRegistry` 注入自定义 Counter、Timer 与 Gauge 监控项；在 `monitoring/grafana/dashboards/` 中扩充业务关键 KPI 面板，直观展现平台核心业务的实时健康度。
- [ ] **4. 日志、指标与分布式链路追踪黄金三要素统一联动 (Loki & Tempo Full-Stack Observability)**
  - **现状**：当前系统指标存储在 Prometheus，而日志分散在 Docker 容器标准输出（`json-file`），缺乏统一检索入口。当 Grafana 仪表盘监控到某接口延迟抖动时，运维人员无法在图表面板上一键“下钻查看关联的慢请求日志与 Trace 追踪链”，排查成本较高。
  - **改进方案**：在 Docker Compose 栈中接入 Grafana Loki 进行日志集中收集，结合 Spring Boot MDC 注入的 `traceId`，配置 Grafana 数据源联动（Data Links），打通“Prometheus 指标大盘 ➔ 点击跳入 Loki 错误日志 ➔ 查看 Tempo 链路调用全貌”的统一可观测性平台。

### 18. Nginx 边缘网关与高性能动静分离架构演进 (Nginx Edge Gateway & High-Performance Static/API Governance)

基于对当前宿主机 Nginx 80/443 网关配置、Flutter Web 静态托管及 Spring Boot 后端反向代理拓扑的审计，规划以下 4 项生产网关演进增强点：

- [ ] **1. Gzip 与 Brotli 静态预压缩加固 (Brotli & Gzip Pre-compression for Flutter WASM)**
  - **现状**：目前 Nginx 未配置针对 Flutter Web 编译产物的压缩策略。Flutter Web 产物包含 `canvaskit.wasm`、`flutter.js`、`main.dart.js` 等体积较大的核心文件，在移动端或弱网下首次加载会消耗较多下行带宽。
  - **改进方案**：在 Nginx 中启用 `gzip on; gzip_types text/plain application/javascript application/wasm text/css;`，并在 CI/CD 构建阶段通过脚本生成 `.br` (Brotli) 与 `.gz` 预压缩文件，开启 `gzip_static on;`，消除 Nginx 在单核 EC2 上进行实时动态压缩的 CPU 开销，显著压缩网络传输体积（减负 60%~75%）。
- [ ] **2. Upstream 连接池与长连接优化 (Upstream Keepalive & Microservice Decoupling)**
  - **现状**：当前 Nginx 配置直接硬编码 `proxy_pass http://127.0.0.1:8080/;`，默认使用 HTTP/1.0 且未启用长连接池。每个客户端请求都会在 Nginx 与 Spring Boot 之间重新经历 TCP 三次握手与短连接挥手，高并发时易造成本地短暂端口耗尽与 TIME_WAIT 积压。
  - **改进方案**：引入 `upstream portfolio_backend { server 127.0.0.1:8080 max_fails=3 fail_timeout=10s; keepalive 32; }` 块，并在 proxy 内部配置 `proxy_http_version 1.1; proxy_set_header Connection "";`，实现反向代理层到后端容器的长连接池复用。
- [ ] **3. 边缘网关安全响应头与隐藏敏感文件过滤 (Security Headers & Hidden File Shielding)**
  - **现状**：目前 Nginx 响应未配置 OWASP 推荐的基础安全响应头，并且未显式拦截隐藏文件（如 `.git`、`.env`、`.DS_Store`、`.bak`），若未来静态目录误入备份文件存在泄漏隐患；此外未隐藏 Nginx 内部版本号。
  - **改进方案**：在 server 块统一配置 `server_tokens off;`，增加 `X-Content-Type-Options "nosniff"`、`X-Frame-Options "SAMEORIGIN"` 与 `Referrer-Policy "strict-origin-when-cross-origin"` 等标准安全标头，并追加 `location ~ /\. { deny all; access_log off; log_not_found off; }` 彻底杜绝潜在敏感文件泄漏。
- [ ] **4. 访问日志 JSON 结构化与全栈可观测性采集 (Nginx Access Log JSON Structuring)**
  - **现状**：当前 Nginx 采用默认 combined 纯文本日志格式，排查线上性能瓶颈时难以快速提取请求处理耗时（`$request_time`）与后端响应耗时（`$upstream_response_time`），也无法被日志收集器（如 Loki、Fluentd）直接做结构化字段提取。
  - **改进方案**：定义标准 JSON 日志格式 `log_format json_analytics escape=json '{ ... }'`，将客户端真实 IP、TraceId、请求方法、URI、状态码、传输字节数、总耗时及上游耗时以标准 JSON 输出，为 Prometheus/Grafana 统一可观测性体系提供标准入口日志流。

### 19. 研发运维工具链与自查备忘协同演进 (Developer Tooling, CheatSheet & Workflow Automation)

基于对当前 `note.md` 开发者日常自查备忘录、Gradle 构建脚本、本地 Docker 编排及环境诊断工具的审计，规划以下 4 项研发工程化演进增强点：

- [ ] **1. 统一跨平台开发者 CLI 脚本工具箱 (Unified Developer CLI & Justfile/Taskfile)**
  - **现状**：目前日常构建、清理、单测、打包与端口排查命令分散在 `note.md`、PowerShell 脚本（`.ps1`）与 Windows 批处理（`.bat`）中，macOS/Linux 开发者或新成员上手时需手动分步查找并敲击复杂参数命令。
  - **改进方案**：引入现代化的 `justfile` 或 `Makefile`（如 `just dev`、`just test`、`just docker-up`、`just kill-port`），将 `note.md` 中的高频操作命令封装为跨平台统一指令集，提供彩色交互式 Help 帮助菜单。
- [ ] **2. 本地与生产环境变量漂移自动探测与校验 (Config Drift & Environment Validator)**
  - **现状**：目前环境变量（如 `MAIL_*`、`MYSQL_*`、`REDIS_*`、`FRONTEND_URL`）依赖 `.env` 与 `application.properties` 手动维护，当新增配置项或在云端部署时，缺乏自动校验步骤，极易因遗漏关键环境变量而在运行时报空指针或连接失败。
  - **改进方案**：编写启动期轻量校验机制（或集成基于 Bash/Python 的预检脚本 `tools/check-env.sh`），在应用拉起前自动对比 `.env.example` 与实际环境变量，对缺失或未填写的生产关键项执行拦截报错与高亮提示。
- [ ] **3. 数据库一键种子填充与测试数据重置脚本 (Automated DB Seed & Reset Tooling)**
  - **现状**：当前测试数据依赖 Flyway `V2__Add_test_data.sql` 一次性灌入；在本地频繁进行修改与删除接口自测后，若想恢复初始干净状态，需要手动执行容器重置或编写 SQL 清理，操作繁琐。
  - **改进方案**：提供一键重置脚本（`tools/reset_db_seed.py` 或 Gradle 任务 `gradle resetTestDb`），支持清空业务表数据并重新回滚执行 Flyway V2 迁移，快速还原至确定性测试基线。
- [ ] **4. 自动化全端点健康巡检与冒烟测试脚本 (Automated Health & Smoke Probe Script)**
  - **现状**：开发者在本地或云端执行 `./gradlew bootRun` / `docker compose up` 后，需手动在浏览器中逐一打开 Swagger、Actuator、Prometheus、Grafana 与静态图片 URL 进行可用性验证。
  - **改进方案**：编写自动化冒烟测试脚本（`tools/smoke_check.py`），启动后并发探测 `note.md` 中记录的所有 8 大服务与门户端点，输出带 HTTP 状态码与耗时的绿色健康检查矩阵报告。

### 20. 密码重置业务安全与身份认证防御深度演进 (Password Reset Security & Identity Verification Governance)

基于对当前基于 Token、Redis 与邮件的密码重置 API（`/v1/auth/forgot-password` 与 `/v1/auth/reset-password`）的审计，规划以下 4 项认证安全演进增强点：

- [ ] **1. 企业级密码复杂度策略与弱口令字典拦截 (Password Complexity & Dictionary Defense)**
  - **现状**：目前 `ResetPasswordRequest` 仅对新密码做了长度限制（`@Size(min = 6, max = 100)`），未强制校验字母、数字与特殊字符组合，允许用户设置弱口令（如 `123456`、`password` 等），易遭受离线字典破解。
  - **改进方案**：引入密码复杂度策略校验器（结合 zxcvbn 熵评估算法或正则表达式），强制新密码至少包含大小写字母、数字及特殊符号中的三种组合；并内置常见 Top 1000 弱密码黑名单，全面阻断弱口令重置。
- [ ] **2. 密码修改成功异步二次邮件提醒与历史密码防重复机制 (Success Notification & Password History)**
  - **现状**：用户重置密码成功后，系统仅执行数据库密码更新与全端会话吊销，未向原注册邮箱补发确认通知邮件；此外未持久化历史密码记录，允许用户将其改回原密码。
  - **改进方案**：在密码重置成功后，通过 `EmailService.sendSimpleEmail` 触发异步安全提醒邮件，告知用户密码重置的时间、IP 与客户端平台（若非本人操作提示立即冻结）；建立 `user_password_history` 历史表，记录最近 3 次密码 BCrypt 哈希，重置时校验禁止重复使用近期的旧密码。
- [ ] **3. 移动端/小程序原地 6 位短验证码 (OTP) 备用通道支持 (6-Digit Numeric OTP Support)**
  - **现状**：目前仅支持“邮件长链接 Token（256 位 URL-Safe Base64）”模式。在 Flutter 移动端 App 或嵌入式 WebView 中，跳转浏览器点击邮件往往导致 App 退至后台、状态丢失或用户体验割裂。
  - **改进方案**：在重置邮件中同步生成并展示“6 位数字验证码（OTP，15 分钟有效）”；重置接口扩展支持双模入参（既可使用长 Token，亦可使用 `email + otp`），允许用户在移动端 App 原地输入数字验证码完成重置。
- [ ] **4. 混合签名 Token 与客户端防嗅探防重放加固 (Signed HMAC Token & Anti-Replay)**
  - **现状**：目前 Token 为纯随机数且完全依赖 Redis 存取。在高并发场景下若遭遇攻击者使用海量虚假 Token 进行并发探测，每次都要访问 Redis 执行 `hasKey`，存在潜在的缓存穿透压力。
  - **改进方案**：将 Token 设计为自解释签名凭据（如 `Base64(UserId + Timestamp + Nonce + HMAC-SHA256)`），在进入 Redis 查询前可直接在应用层内存进行无状态防篡改初筛与过期判断，大幅减轻缓存基础设施负载。

### 21. 智能分布式限流与高可用防刷架构演进 (Distributed Rate Limiting & High-Availability Anti-Abuse Governance)

基于对当前基于 AOP 切面与 Redis 固定时间窗口限流体系（`@RateLimit`、`RateLimitAspect` 与 `RateLimitService`）的架构复盘与实测分析，规划以下 4 项限流架构演进任务：

- [ ] **1. 基于 Redis ZSet / Lua 脚本的滑动时间窗口算法升级 (Sliding Window Log via Redis ZSet & Lua)**
  - **现状**：当前 `RateLimitService` 采用固定时间窗口计数器算法（`currentWindow = currentTimeMillis / (timeWindowSeconds * 1000)`，生成形如 `rate_limit:{type}:{identifier}:{windowBucket}` 的 Key），在窗口切换边界（如第 59 秒和第 61 秒）可能出现 2 倍阈值的瞬时突发流量（Boundary Burst），无法对时间维度提供平滑的流量约束。此外，`opsForValue().increment(key)` 与 `expire(key, ...)` 分步执行，在极端崩溃场景下存在悬挂键风险。
  - **改进方案**：升级为基于 Redis Sorted Set (ZSet) 或原子 Lua 脚本的滑动时间窗口算法。利用毫秒级时间戳作为 score 和 member，通过原子执行 `ZREMRANGEBYSCORE` 清理过期请求、`ZCARD` 统计当前滑动窗口内的实际请求数、`ZADD` 记录新请求并刷新 TTL，彻底消除时间窗口边界毛刺风险，保证全时间轴绝对平滑限流。
- [ ] **2. 令牌桶算法 (Token Bucket) 引入与突发洪峰平滑整形 (Smooth Burst Shaping & Leaky/Token Bucket)**
  - **现状**：当前限流只提供“允许”或“429 硬拦截”的二值化判定模式，缺乏对合规短时突发流量的平滑缓冲（Traffic Shaping）与削峰填谷能力。对于移动端用户因网络重连连续触发的短暂脉冲请求，容易造成误杀。
  - **改进方案**：引入基于 Redis + Lua 或成熟库（Resilience4j / Bucket4j）的分布式令牌桶算法，支持配置桶容量（Bucket Capacity）与令牌填充速率（Refill Rate）。允许短时间内的合规突发流量（Burst Quota），当超出桶容量时支持自适应退避与排队缓冲，或者在响应体中返回精确的计算重试等待时间。
- [ ] **3. 基于 SpEL 表达式的动态限流键求值与分级白名单放行机制 (Dynamic SpEL Evaluator & Tiered Whitelist)**
  - **现状**：当前 `RateLimitAspect` 中 `CUSTOM` 限流类型的 `extractCustomIdentifier` 仅返回表达式字面量，尚未集成真正的 Spring `SpelExpressionParser` 与 `MethodBasedEvaluationContext`；同时系统缺乏针对内网 IP、管理运维人员或健康检查探针的分级白名单旁路放行机制。
  - **改进方案**：完善 SpEL 表达式求值引擎，支持在注解中编写 `#request.email`、`#header['X-Tenant-Id']` 或 `#user.id` 等复杂动态表达式；在 `@RateLimit` 中新增 `whitelist = "..."` 和 `bypassRoles = {"ADMIN"}` 属性，与配置中心动态绑定，对本地回环（`127.0.0.1`）、VPC 内部健康检查探针及超管角色免除限流校验。
- [ ] **4. 网关与应用双层协同限流与标准 RFC 响应头增强 (Edge Gateway Co-Limiting & RFC RateLimit Headers)**
  - **现状**：当前限流全部集中在 Spring Boot 应用层执行。发生海量恶意攻击时，大量无效流量仍会打穿 Nginx 到达 Tomcat，消耗线程池与反序列化 CPU 资源；并且当前返回 429 时仅输出通用 JSON 响应体，未在响应头中下发 `X-RateLimit-Limit`、`X-RateLimit-Remaining`、`X-RateLimit-Reset` 与 `Retry-After` 等标准 HTTP 标头。
  - **改进方案**：构建“Nginx 边缘网关 `limit_req_zone` 粗粒度防爆破 + Spring Boot 应用层 AOP 多维度（IP/Email/Token/User）细粒度业务防护”的双层防御矩阵；在 `RateLimitAspect` 拦截或放行时，在 `HttpServletResponse` 中规范注入标准 RFC 限流响应头，方便前端 Flutter App 与客户端网络库实现自适应退避重试（Exponential Backoff）。

### 22. 全链路应用安全纵深防御与身份治理演进 (Application Security Architecture & Identity Governance)

基于对当前 Spring Security 过滤器链、无状态 JWT 机制、Redis Token 黑名单、Refresh Token 会话管理与大 Payload 头像防护体系的深度安全审计，规划以下 4 项系统性安全演进任务：

- [ ] **1. 密码复杂度策略强制校验器与高危弱口令字典库拦截 (Password Complexity & Dictionary Auditing)**
  - **现状**：用户注册（`SignUpRequest`）与修改密码（`ChangePasswordRequest`）当前仅依托 `@Size(min = 6, max = 100)` 做简单长度校验，未强制要求大小写字母、数字及特殊字符的多因子组合；系统未内置常见弱口令黑名单，用户仍可设置如 `123456`、`password` 等高危密码，易遭受离线碰撞破解。
  - **改进方案**：实现独立的 `PasswordPolicyValidator` 或接入 zxcvbn 密码熵评估器，强制口令长度 $\ge 8$ 位且必须覆盖大写字母、小写字母、数字与特殊字符中的至少 3 类；内置 Top 1000 常见弱口令字典库与用户名反向相似度检测，在注册与修改时进行严格强校验。
- [ ] **2. 连续登录失败阈值惩罚、账号临时锁定与人机验证码介入 (Account Lockout & Adaptive CAPTCHA)**
  - **现状**：当前 `/v1/auth/login` 接口仅依靠基于 IP 维度的 `@RateLimit`（10次/60秒）进行拦截。当攻击者利用数千个分布式代理 IP 针对特定受害者账号进行漫游撞库（Distributed Credential Stuffing）时，无法有效触发阻断。
  - **改进方案**：引入基于 Redis 的账号级失败重试计数器（如 `login:fail:count:<username>`，记录连续密码错误次数）；若 15 分钟内同一账号连续认证失败达 5 次，自动触发阶梯式安全策略：先要求输入图形/滑块人机验证码（CAPTCHA），失败达 10 次则临时锁定该账号 30 分钟，并向注册邮箱异步投递异地/异常登录风险警报。
- [ ] **3. 基于 Redis Set 集合的 $O(1)$ 复杂度全端会话吊销重构 (O(1) Revocation via Redis User Token Set)**
  - **现状**：当前 `RefreshTokenService.revokeAllRefreshTokens(username)` 采用 Redis 的 `KEYS token:refresh:<username>:*` 模式匹配进行模糊检索删除。在生产高并发、包含数百万级 Key 的 Redis 实例中，`KEYS` 指令属于 $O(N)$ 单线程阻塞操作，极易引发毫秒至秒级的 Redis 请求卡顿（Latency Spike）。
  - **改进方案**：重构 Refresh Token 存储拓扑，采用 Redis Set 维护每个用户的活跃令牌指纹集合（如 Key 为 `token:user_sessions:<username>`，成员为每个设备的 refreshToken 标识）；在执行全端吊销时，先通过 `SMEMBERS` 检索出有限的活跃会话并批量删除对应 Key，再执行 `DEL token:user_sessions:<username>`，将时间复杂度从 $O(N)$ 降至严格的 $O(1)$，消除生产阻塞隐患。
- [ ] **4. 结构化安全审计日志发布与自动化敏感数据注解脱敏 (Security Audit Events & Dynamic Masking)**
  - **现状**：安全相关的事件（密码错误、Token 校验失败、注销、密码重置、头像拦截等）分散记录在各业务日志中，缺乏标准化的安全审计事件模型与独立审计管道；除 `UserController` 简单的局部 Token 遮蔽外，缺乏在 DTO 返回与控制台打印时针对邮箱、手机号、IP 及用户凭证的统一自动化脱敏能力。
  - **改进方案**：建立基于 Spring ApplicationEvent 异步事件驱动的 `SecurityAuditService`，将认证异常、越权尝试、敏感修改统一归集为标准安全事件日志（包含 Client IP、UserAgent、操作类型、威胁等级与毫秒耗时）；实现 `@SensitiveMask(type = MaskType.EMAIL/IP/TOKEN)` 注解与 Jackson 自定义序列化器，在 HTTP 响应序列化与日志切面中实现全自动、零侵入的动态脱敏。

### 23. WAR 传统企业级容器部署与 Jakarta EE 规范适配演进 (Enterprise WAR Deployment & Container Governance)

基于对当前项目 Gradle 8.5 构建配置（`id 'war'`、`providedCompile`、`bootWar`）、`ServletInitializer` 启动适配以及外部独立 Servlet 容器（Apache Tomcat 10.1+ / WildFly / Jetty）运行拓扑的全面审计，规划以下 4 项企业级 WAR 部署演进任务：

- [ ] **1. 外部独立 Servlet 容器 Context Path 与反向代理网关协同重写规范 (Context Path & Reverse Proxy Alignment)**
  - **现状**：将 WAR 包直接扔入独立 Tomcat 的 `webapps/portfolio.war` 时，Tomcat 会默认分配应用上下文路径 `/portfolio`（所有端点变为 `http://host:port/portfolio/v1/...`）。若前端 Flutter、Nginx 反向代理网关或 Swagger UI 仍按根路径 `/` 请求，会导致大面积 404 路由丢失或静态资源相对路径加载失败。
  - **改进方案**：制定标准外部容器部署规范：推荐将 WAR 包重命名为 `ROOT.war` 部署以直接绑定根路径 `/`，或在 Tomcat `conf/server.xml` 的 `<Host>` 中显式配置 `<Context path="" docBase="portfolio" reloadable="false"/>`；在 Nginx 配置中提供与 `/portfolio` 自动剥离/补齐上下文的 `proxy_pass` 模板，并在文档中规范统一。
- [ ] **2. JNDI 外部托管连接池条件装配与应用内 HikariCP 平滑回退机制 (JNDI Resource Injection & HikariCP Fallback)**
  - **现状**：当前系统强依赖应用内置的 HikariCP 连接池与 `application.properties` 直连 MySQL/Redis。在银行、金融机构或传统国企生产环境中，运维团队通常强制要求应用服务器（WebSphere / WebLogic / Tomcat）接管数据源连接池（通过 JNDI 注入 `java:comp/env/jdbc/portfolioDB`），禁止应用直接持有数据库账号密码。
  - **改进方案**：在配置体系中引入条件装配（`@ConditionalOnProperty(name = "spring.datasource.jndi-name")`），在检测到 JNDI 数据源配置时优先使用容器托管连接池，未检测到时自动平滑回退至应用内嵌入式 HikariCP，实现云原生自包含模式与传统企业合规模式的双向兼容。
- [ ] **3. 外部容器生命周期管理与热卸载/停机内存泄漏防范 (External Container Lifecycle & ClassLoader Protection)**
  - **现状**：在可执行 JAR 独立运行时，Spring Boot 通过内置优雅停机机制（`server.shutdown=graceful`）处理 SIGTERM 信号。而在外部 Tomcat 容器中，当执行热重载（Redeploy）、卸载（Undeploy）或 `catalina.sh stop` 时，若后台异步线程池、Redis Lettuce 客户端、数据库驱动或 ThreadLocal 未能彻底注销，容易触发 Tomcat 典型的 `WebappClassLoaderBase.checkThreadLocalMapForLeaks` 报警，引发 Metaspace 内存泄漏。
  - **改进方案**：在 `ServletInitializer` 中实现完整的 `ServletContextListener` 生命周期销毁监听钩子；显式注销 MySQL JDBC 驱动（`DriverManager.deregisterDriver`）、销毁 Redis Lettuce 连接池线程组（`NioEventLoopGroup.shutdownGracefully`）以及清理 Logback 上下文，确保外部容器热替换时的零内存泄露。
- [ ] **4. CI/CD 双轨自动化构建流水线与 Dockerized Tomcat 10.1 冒烟测试探针 (Dual-Track Artifact CI & Tomcat 10.1 Smoke Probe)**
  - **现状**：当前 GitHub Actions 与本地验证脚本主要针对 JAR 模式（`./gradlew test` 与 `bootJar`），缺乏对 `./gradlew bootWar` 的常态化质量门禁，也未在真实的独立外部容器环境中执行自动化启动与接口冒烟验证，存在 WAR 包部署配置意外损坏的隐患。
  - **改进方案**：在 GitHub Actions 流水线中扩展构建矩阵，并行产出标准 JAR 与 WAR 双轨制品；在 CI 阶段拉起 Docker 官方 `tomcat:10.1-jdk17` 容器并挂载生成的 WAR 文件，通过健康探针脚本探测 `/actuator/health` 与核心接口，实现 WAR 交付件全自动冒烟验证闭环。

### 24. 测试覆盖率治理与自动化质量门禁演进 (Test Coverage Governance & Automated Quality Gates)

基于对当前项目 Gradle 8.5 JaCoCo 插件配置、369 个单元与集成测试用例以及最新 92.05% 行覆盖率指标的复盘与代码审查，规划以下 4 项质量保障演进任务：

- [ ] **1. Gradle 任务链自动化测试覆盖率质量门禁与构建阻断 (Enforced JaCoCo Verification Gates in Gradle)**
  - **现状**：当前 `build.gradle` 仅配置了 `jacocoTestReport` 生成 HTML 与 XML 报表，未接入 `jacocoTestCoverageVerification` 门禁校验任务；若团队后续成员引入了无单测覆盖或缺少异常分支覆盖的低质量代码，构建任务依然能够成功退出（BUILD SUCCESSFUL），缺乏编译期强约束。
  - **改进方案**：在 `build.gradle` 中正式引入 `jacocoTestCoverageVerification` 任务并将其绑定到 `./gradlew check` 任务生命周期；配置硬性阈值规则：全工程总体行覆盖率（Line $\ge 80\%$）、分支覆盖率（Branch $\ge 65\%$），核心 Service 模块行覆盖率（$\ge 85\%$），未达到门禁阈值时直接抛出 `GradleException` 阻断流水线。
- [ ] **2. 测试统计排除规则精细化收敛与生成的非业务类过滤 (Exclusion Pattern Refinement for DTO/Config/Main)**
  - **现状**：当前 JaCoCo 分析树中包含了无需单测覆盖的启动引导类（`PortfolioApplication.main`、`ServletInitializer`）、静态资源/OpenAPI 配置类以及纯数据结构 DTO，导致根包 `com.listen.portfolio` 显示 20% 行覆盖率，统计数据存在一定程度的失真与噪声。
  - **改进方案**：在 `jacocoTestReport` 与 `jacocoTestCoverageVerification` 中通过 `classDirectories.setFrom(files(classDirectories.files.collect { fileTree(dir: it, exclude: ['**/PortfolioApplication*', '**/ServletInitializer*', '**/*Config*']) }))` 配置精准白名单/排除模式，将统计范围严格收敛在 Controller、Service、Mapper、Aspect、Util 等核心业务代码域。
- [ ] **3. 基于 Testcontainers 的真实 MySQL/Redis 容器化集成测试 (Testcontainers Containerized Integration Suite)**
  - **现状**：当前集成测试（`BaseIntegrationTest`）依托纯内存级 H2 数据库与嵌入式 Redis（`it.ozimov:embedded-redis`）。虽然执行速度快，但在 SQL 特殊方言（如 JSON 检索、复杂联表锁机制）与 Redis 偶发断连恢复行为上，与生产真实环境存在一定的微观差异；此外嵌入式 Redis 在部分 Linux 发行版与 ARM 架构上存在依赖兼容风险。
  - **改进方案**：利用项目中已引入的 `testcontainers:mysql:1.19.0` 依赖，扩充编写 `@Testcontainers` 端到端集成套件；在夜间每日构建或 Release 发版前拉起真实的 `mysql:8.0` 与 `redis:7.2-alpine` 容器执行全量深度业务联测，彻底消除内存模拟环境与生产真实中间件的运行差异。
- [ ] **4. 变异测试 (Mutation Testing via PITest) 引入与假阳性测试用例根除 (Mutation Testing & Assertion Auditing)**
  - **现状**：传统的行覆盖率与分支覆盖率只能证明测试代码“执行”过了目标行，无法验证测试用例中的断言（Assertions）是否具备真实有效的缺陷拦截能力（例如存在只调用业务方法但断言过于宽泛或缺失的“假阳性”用例）。
  - **改进方案**：规划在测试流水线中引入变异测试工具 PITest（`info.solidsoft.pitest` 插件），自动在字节码中注入突变（如颠倒条件判断、变更返回值、移除方法调用等），计算变异得分（Mutation Score）；若测试用例能够成功失败并“杀死变异体”（Killed Mutants），则证明测试断言真实有效，从而彻底根除无效空跑用例。

## Later

### 1. 生产安全与运维增强

- HTTPS / TLS
- 更完整的生产部署方案
- 更强的监控与安全指标

### 2. 安全能力扩展

- `PasswordPolicyValidator`
- 服务端文案 i18n
- `DataMaskingUtil`
- `SecurityMetrics`

### 3. ORM 架构升级（已完成：将 JPA/Hibernate 改造为 MyBatis-Plus）

**现状**：已完成。全工程全面舍弃 Hibernate/JPA，无缝重构为 **MyBatis-Plus (v3.5.7)**。
- 依赖升级：移除 `spring-boot-starter-data-jpa`，引入 `mybatis-plus-spring-boot3-starter:3.5.7` + `spring-boot-starter-jdbc` + `spring-boot-starter-aop`。
- 实体改造：7 个实体类全部迁移至 `@TableName`、`@TableId(type = IdType.AUTO)`、`@TableLogic`、`@TableField`。
- Mapper 替换：使用 7 个 `BaseMapper<T>` 配合自定义 SQL 注解方法管理子表关联（`user_certifications`, `project_tech_stack`, `skill_items`, `stat_tags`）。
- 业务层重构：Service 层全面使用强类型 `LambdaQueryWrapper`，彻底根除 JPA N+1 与 Open-Session-In-View 性能陷阱。
- 测试保障：369 个单元与集成测试全部 100% 绿色通过，自动生成 `schema-h2.sql` 支持单测纯内存 H2 隔离运行。

### 3. security_features.md 设计落地

以下能力目前仍主要属于设计稿：

- `PasswordPolicyValidator`
- `DataMaskingUtil`
- `SecurityAuditService`
- `SecurityMetrics`
- `AnomalyDetectionService`
- `ThreatDetectionService`
- `AutomatedSecurityResponse`

这些内容只有在与项目定位匹配、且不挤压主线任务时才推进。

## Archive / Backup

### 已确认完成的基础能力

- 分层架构
- JWT 认证
- Token 黑名单
- BCrypt
- Spring Security 无状态配置
- 全局异常处理
- OpenAPI / Swagger UI
- Flyway 启动迁移
- Prometheus + Grafana
- 结构化 JSON 日志
- 请求日志
- JaCoCo / SpotBugs
- Docker Compose 完整栈
- 敏感配置外部化
- 邮件服务与密码重置能力
- AOP 限流
- `ErrorCode + BusinessException`

### 已降级或待重新判断的条目

- 不再保留“9/10”式自评分数表
- `LoginResponse.token` 是否改名为 `accessToken`，暂不作为当前主线任务
- 不再把“测试覆盖率 > 80%”直接写成已证明事实，除非有稳定报告支撑

## 简要检查清单

### 安全

- [x] JWT Token 认证
- [x] BCrypt 密码哈希
- [x] Token 黑名单（Redis）
- [x] 敏感配置环境变量化
- [x] Refresh Token 持久化与主动吊销
- [x] 认证与用户高频接口 AOP 分布式限流
- [x] 头像 Base64 长度校验与魔数白名单深层安全防御
- [ ] 生产环境 HTTPS (等待 `is-a.dev` PR 生效后配置 Certbot)

### Flutter 对接

- [x] Flutter mock 数据补全 `messageId`
- [x] Flutter projects.json 技术栈修正为 Spring Boot
- [x] Flutter aboutMe.json 替换为真实简历数据
- [x] Flutter login / refresh userId 修正为数字类型
- [x] V1 DB 种子数据替换为真实简历内容
- [x] 创建并校准 `docs/api_reference.md`
- [x] Flutter 端适配 `ProjectDto.businessId`
- [x] Flutter 端适配 `StatDto.id` vs `businessId` 映射
- [x] Flutter dev 环境配置指向后端 API URL
- [x] V2 迁移脚本与 6 大技能维度中/英/日多语言动态数据支持
- [x] ListenCore 升级 `0.0.49`，请求头动态拦截与无条件 Accept-Language 注入

### 工程化与质量

- [x] Docker Compose 完整可观测性服务栈 (App + MySQL + Redis + Prometheus + Grafana)
- [x] Prometheus + Grafana 仪表盘与 Actuator 生产指标监控
- [x] 健康检查 / 容器 liveness 与 readiness 探针
- [x] 结构化 JSON 日志与脱敏过滤
- [x] GitHub Actions CI/CD 流水线 (编译、单测、覆盖率、静态扫描、自动部署)
- [x] 全工程全面迁移至标准 Gradle 8.5 构建与 Wrapper
- [x] 全面升级为 MyBatis-Plus 3.5.7 ORM 架构，彻底消除 OSIV 隐患
- [x] 369 个单元与集成测试用例 100% 绿色通过，全局行覆盖率 92.05%
- [x] 建立内嵌 Redis (`EmbeddedRedisTestConfig`) 与内存 H2 测试环境，单测零外部依赖
- [x] 传统 Tomcat 10.1+ / Jakarta EE 10 WAR 包部署双模兼容支持 (`bootWar` / `ServletInitializer`)
- [x] 完善 docs/ 目录下 20 份全量设计架构与运维手册，实现跨文档事实一致性校准
---

📅 **最后更新**: 2026-09-17