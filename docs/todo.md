# 📋 TODO 清单

## 项目现状判断

### 已确认的当前状态

- 分层结构、JWT、Redis 黑名单、邮件服务、监控、日志、Flyway、Docker 基础能力都已具备
- 这个项目已经是一个**可运行、可联调、可继续维护**的支撑型后端
- 当前最需要补的不是“再加很多功能”，而是**契约收口、可靠性、文档可信度与自动化**

### 当前主要风险

- Flutter / Backend 契约还有最后一段收尾工作
- OSIV 关闭还没正式启用，README 不能把它写成已完成
- Refresh Token 还不能主动吊销
- CI 仍为空白
- 部分安全 / 设计文档仍是 spec，不是已实现能力

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
- 测试保障：338 个单元与集成测试全部 100% 绿色通过，自动生成 `schema-h2.sql` 支持纯内存 H2 隔离运行。

### 5. GitHub Actions CI

**现状**：已成功搭建，包含编译校验、单元测试、JaCoCo 报告以及基于 SSH / SCP 的 AWS EC2 自动构建部署流。已加入 `ConnectTimeout 30` 与 `ConnectionAttempts 5` 超时重试防抖机制。  
**目标**：建立最小可信 CI：测试 + JaCoCo + SpotBugs + 自动 CD 部署。  
**为什么现在做**：保证 Push / PR 自动完成防抖校验与部署上线。  
**验收标准**：
- [x] Push / PR 自动触发校验；README 挂载状态。
- [x] 增加 SSH `ConnectTimeout` 与 `ConnectionAttempts` 避免 EC2 瞬态握手超时。

## Next

### 1. 测试补强

**目标**：补上目前最有代表性的缺口测试。  
**建议补充**：

```text
src/test/java/com/listen/portfolio/
├── api/v1/auth/
│   └── AuthControllerRefreshTest.java
├── service/
│   ├── AuthServiceTest.java
│   └── ProjectServiceTest.java
└── repository/
    └── ProjectRepositoryTest.java
```

**验收标准**：关键链路覆盖更完整，JaCoCo 报告不再只靠现有样本测试支撑。

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
- [ ] 在 Flutter `env_config.dart` 中将 prod 环境 BaseUrl 更新为 `https://listen2code.is-a.dev`

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
- 测试保障：338 个单元与集成测试全部 100% 绿色通过，自动生成 `schema-h2.sql` 支持单测纯内存 H2 隔离运行。

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
- [x] Refresh Token 持久化与吊销
- [x] 认证接口限流
- [ ] 生产环境 HTTPS

### Flutter 对接

- [x] Flutter mock 数据补全 `messageId`
- [x] Flutter projects.json 技术栈修正为 Spring Boot
- [x] Flutter aboutMe.json 替换为真实简历数据
- [x] Flutter login / refresh userId 修正为数字类型
- [x] V1 DB 种子数据替换为真实简历内容
- [x] 创建 `docs/api_reference.md`
- [x] Flutter 端适配 `ProjectDto.businessId`
- [x] Flutter 端适配 `StatDto.id` vs `businessId` 映射
- [x] Flutter dev 环境配置指向后端 API URL
- [x] V2 迁移脚本与 6 大技能维度中/英/日多语言数据

### 工程化

- [x] Docker Compose 完整栈
- [x] Prometheus + Grafana
- [x] 健康检查 / 探针
- [x] 结构化 JSON 日志
- [x] GitHub Actions CI/CD 防抖与自动部署
- [x] 全工程全面迁移至标准 Gradle 8.5 构建与 Wrapper
- [x] 全面升级为 MyBatis-Plus 3.5.7 ORM 架构
---

📅 **最后更新**: 2026-08-21