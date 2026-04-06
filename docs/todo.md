# 📋 TODO 清单

## 📊 项目现状评估

### ✅ 已完成

- **分层架构**：Controller / Service / Repository / Entity 职责清晰
- **JWT 认证**：登录、注册、Token 刷新、退出登录、修改密码（含 Token 失效）、忘记密码、账号注销
- **Token 黑名单**：基于 Redis，退出登录 / 修改密码时立即失效
- **BCrypt 密码哈希**
- **Spring Security 无状态配置**（CSRF 禁用、Stateless Session）
- **全局异常处理**：`GlobalExceptionHandler` 统一错误响应
- **OpenAPI / Swagger UI**：自动生成 API 文档
- **HikariCP 连接池**：完整的连接池参数配置
- **Flyway 数据库迁移**：启动时自动执行
- **Prometheus + Grafana 监控**：JVM / HTTP / 连接池指标
- **结构化 JSON 日志**：logstash-logback-encoder，兼容 ELK / Loki
- **请求日志**：`RequestLoggingFilter` 记录所有入站请求
- **JaCoCo 覆盖率报告**：`check-coverage.bat` 一键生成
- **SpotBugs + Find Security Bugs 静态分析**：`check-spotbugs.bat` 一键运行
- **Docker Compose 完整栈**：App + MySQL 8 + Redis 7 + Prometheus + Grafana
- **多环境配置**：`.env` 环境变量注入，支持 local / staging / prod profiles
- **敏感配置外部化**：DB / JWT 均通过环境变量注入
- **Kubernetes 探针**：liveness / readiness 健康检查端点
- **邮件服务**：`EmailService` + SMTP 配置，支持密码重置邮件发送
- **密码重置 Token**：`PasswordResetTokenService` 支持邮件链接重置密码
- **AOP 智能限流**：`@RateLimit` 注解 + `RateLimitAspect` + Redis，支持 IP/EMAIL/TOKEN/USER/CUSTOM 多维度
- **ErrorCode + BusinessException**：基础错误码体系和业务异常类

### 📈 当前评分

| 维度 | 分数 | 说明 |
|------|------|------|
| 架构设计 | 9/10 | 分层清晰，职责分明，AOP 限流设计优秀 |
| 安全性 | 9/10 | JWT + 黑名单 + BCrypt + 智能限流，防护全面 |
| 测试覆盖 | 8/10 | Controller/Service/Repository 均有，集成测试完善 |
| 可观测性 | 9/10 | Prometheus + Grafana + 结构化日志，监控完整 |
| 文档完善 | 7/10 | README + Swagger UI，部分功能文档待完善 |
| 部署就绪 | 8/10 | Docker 完整，支持 JAR/WAR 双模式，缺 HTTPS |

---

## 🔴 高优先级

### 1. ⭐ Flutter 前后端 API 对齐

**现状**：大部分 Mock 数据已修正，剩余 DTO 字段差异需 Flutter 端模型适配。  
**具体差异**：

| 项目 | Flutter Mock | 后端实际 | 处理 |
|------|-------------|----------|------|
| login.json `body.token` | `token` | `LoginResponse.token` | ✅ 已匹配 |
| login.json `body.userId` | ~~String `"1001"`~~ → `1` | Long `1` | ✅ 已修正（ToStringConverter 兼容） |
| user.json / projects.json / aboutMe.json | ~~缺少 `messageId`~~ | 始终返回 `messageId` | ✅ 已补全 |
| projects.json `techStack` | ~~`Node.js, Express`~~ | 实际是 Spring Boot | ✅ 已修正 |
| aboutMe.json | ~~虚假简历数据~~ | 真实简历内容 | ✅ 已替换为真实简历 |
| `ProjectDto` | 无 `businessId` | 有 `businessId` 字段 | ⚠️ 前端需适配 |
| `StatDto.id` | String `"android"` (实为 businessId) | Long `1` + `businessId: "android"` | ⚠️ 前端需映射 businessId→id |
| aboutMe 路径 | `GET /aboutMe` | `GET /v1/aboutMe` | ✅ 已匹配 |

**剩余工作**：Flutter 模型增加 `businessId` 字段适配，或后端 DTO 调整序列化策略。  
**验收标准**：Flutter dev 环境调用后端 API，所有接口响应格式一致、无解析异常。

---

### 2. ⭐ GitHub Actions CI

**现状**：后端项目无 CI 工作流，`.github/workflows/` 为空。  
**目标**：添加 GitHub Actions CI，自动化测试 + JaCoCo 报告 + SpotBugs。

**实施步骤**：
```
1. 创建 .github/workflows/ci.yml
2. 配置 MySQL + Redis 服务容器
3. 添加 ./mvnw test + jacoco:report + spotbugs:check 步骤
4. 添加 CI badge 到 README.md
```

**验收标准**：Push / PR 自动触发测试，README 显示绿色 badge。

---

### 3. 启用 OSIV 关闭配置

**现状**：`application.properties` 中 `spring.jpa.open-in-view=false` 已注释。  
**目标**：正式启用，强制 Service 层完成所有懒加载，避免序列化阶段 N+1 查询。

**实施步骤**：
```
1. 取消注释 spring.jpa.open-in-view=false
2. 运行全量测试，确认无懒加载异常（LazyInitializationException）
3. 对存在懒加载的 Service 方法补充 @Transactional 或 fetch join
```

**验收标准**：应用正常启动，全量测试通过，无 OSIV 相关告警。

---

### 3. ⭐ deleteAccount 软删除修复

**现状**：`UserService.deleteAccount()` 执行硬删除，但 `UserEntity` 有 `deleted` 字段暗示软删除设计意图。  
**问题**：userId=1 是种子用户，硬删除后所有 portfolio 数据丢失。

**实施步骤**：
```
1. 修改 UserService.deleteAccount() 改为软删除：user.setDeleted(true)
2. 修改 aboutMe/projects 查询，排除 deleted=true 的用户
3. 添加单元测试验证软删除逻辑
```

**验收标准**：userId=1 无法被删除，查询接口自动排除已删除用户。

---

### 4. ⭐ V2 迁移脚本（已部署环境数据更新）

**现状**：V1 迁移使用 INSERT IGNORE，已部署环境需要数据更新。  
**目标**：创建 V2 迁移脚本，使用 UPDATE 语句更新现有数据。

**实施步骤**：
```
1. 创建 src/main/resources/db/migration/V2__Update_seed_data.sql
2. 更新 users 表中的 jobTitle, location 等字段为真实简历数据
3. 更新 stats 表中的 year 字段（Android 11年，Flutter 3年）
```

**验收标准**：已部署环境执行 V2 迁移后，数据与真实简历一致。

---

### 5. ⭐ GitHub Actions CI

**现状**：后端项目无 CI 工作流，`.github/workflows/` 为空。  
**目标**：添加 GitHub Actions CI，自动化测试 + JaCoCo 报告 + SpotBugs。

**实施步骤**：
```
1. 创建 .github/workflows/ci.yml
2. 配置 MySQL + Redis 服务容器
3. 添加 ./mvnw test + jacoco:report + spotbugs:check 步骤
4. 添加 CI badge 到 README.md
```

**验收标准**：Push / PR 自动触发测试，README 显示绿色 badge。

---

### 6. 启用 OSIV 关闭配置

**现状**：`application.properties` 中 `spring.jpa.open-in-view=false` 已注释。  
**目标**：正式启用，强制 Service 层完成所有懒加载，避免序列化阶段 N+1 查询。

**实施步骤**：
```
1. 取消注释 spring.jpa.open-in-view=false
2. 运行全量测试，确认无懒加载异常（LazyInitializationException）
3. 对存在懒加载的 Service 方法补充 @Transactional 或 fetch join
```

**验收标准**：应用正常启动，全量测试通过，无 OSIV 相关告警。

---

## 🟡 中优先级

### 7. ⭐ Refresh Token 持久化与吊销

**现状**：刷新 Token 生成后不存储，无法主动吊销（例如修改密码后旧 Refresh Token 仍有效）。  
**目标**：将 Refresh Token 存入 Redis，支持主动吊销。

**实施步骤**：
```
1. 登录时将 refreshToken 存储到 Redis，key = "refresh:<username>"，TTL = refresh 过期时间
2. /v1/auth/refresh 接口验证 Redis 中是否存在该 Token
3. 修改密码 / 注销账号时删除 Redis 中对应的 refreshToken
4. 更新相关单元测试
```

**验收标准**：修改密码后，使用旧 Refresh Token 刷新返回 401。

---

### 8. ⭐ 限流算法升级

**现状**：`RateLimitService` 使用 INCR 固定窗口计数器，存在窗口边界突发问题。  
**目标**：升级为滑动窗口（Redis ZSet）或令牌桶算法，提高限流精度。

---

### 9. 补全测试覆盖

**现状**：Controller / Service / Repository 均有测试，但部分层覆盖不完整。  
**缺少的测试**：

```
src/test/java/com/listen/portfolio/
├── api/v1/auth/
│   └── AuthControllerRefreshTest.java     # refresh / forgot-password 分支
├── service/
│   ├── AuthServiceTest.java               # 注册 / 忘记密码业务逻辑
│   └── ProjectServiceTest.java            # 项目服务
└── repository/
    └── ProjectRepositoryTest.java          # 项目数据访问
```

**验收标准**：整体行覆盖率 > 80%（JaCoCo 报告）。

---

## 🟢 低优先级

### 10. 生产环境 HTTPS / TLS

**现状**：应用仅支持 HTTP，生产环境流量未加密。  
**目标**：通过反向代理（Nginx / AWS ALB）终止 TLS，或直接在应用层配置证书。

---

### 11. 配置文件拆分

**现状**：`application.properties` 约 243 行，所有环境配置混合在一起。  
**目标**：按环境拆分，减少误配置风险。

```
src/main/resources/
├── application.yml           # 公共配置
├── config/
│   ├── application-dev.yml
│   ├── application-prod.yml
│   └── application-test.yml
```

---

### 12. PasswordPolicyValidator 密码复杂度校验

**现状**：密码校验仅依赖前端基础验证，后端无复杂度策略。  
**目标**：实现 `PasswordPolicyValidator` 类，支持长度、字符类型、特殊字符等规则。

---

### 13. 服务端文案 i18n

**现状**：所有错误消息和响应文案均为硬编码中文。  
**目标**：支持多语言消息，根据 Accept-Language 头返回对应语言文案。

---

### 14. security_features.md 设计落地

**现状**：`security_features.md` 中记录的以下类均为设计规划，代码未实现：  
- `PasswordPolicyValidator`：密码复杂度校验
- `DataMaskingUtil`：邮箱/IP 脱敏工具
- `SecurityAuditService`：安全审计日志
- `SecurityMetrics`：Prometheus 安全指标
- `AnomalyDetectionService`：异常登录检测
- `ThreatDetectionService`：实时威胁检测
- `AutomatedSecurityResponse`：自动化安全响应

**建议**：根据面试价值和实际需要逐步实现，优先级从高到低：PasswordPolicyValidator > DataMaskingUtil > SecurityMetrics。

---

### 15. ~~Pom.xml Flyway 插件密码硬编码~~ ✅ 已解决

**现状**：`pom.xml` 的 Flyway Maven 插件配置中密码硬编码（`Ls-88888888`）。  
**目标**：改为读取 Maven 属性或环境变量。

**解决方案**：该问题已解决，Flyway Maven 插件已被注释掉，应用启动时通过 `FlywayConfig.java` 自动执行迁移，支持环境变量配置。

```xml
<!-- 插件已禁用，使用 FlywayConfig.java 自动执行迁移 -->
```

---

## 📋 检查清单

### 安全
- [x] JWT Token 认证
- [x] BCrypt 密码哈希
- [x] Token 黑名单（Redis）
- [x] 敏感配置环境变量化
- [ ] Refresh Token 持久化与吊销
- [x] 认证接口限流（login/signUp/forgot-password）
- [ ] 生产环境 HTTPS

### 功能完整性
- [x] 注册 / 登录 / 刷新 Token
- [x] 修改密码（含 Token 失效）
- [x] 退出登录（Token 黑名单）
- [x] 账号注销
- [x] 忘记密码发送真实邮件
- [ ] Userid=1的账户无法delete-account

### 运维就绪
- [x] Docker Compose 完整栈
- [x] Prometheus + Grafana 监控
- [x] 健康检查 / 探针
- [x] 结构化 JSON 日志
- [ ] Docker 自动执行 Flyway 迁移
- [ ] 生产环境 HTTPS

### 代码质量
- [x] JaCoCo 覆盖率报告
- [x] SpotBugs + Find Security Bugs
- [x] 全局异常处理
- [x] 测试覆盖率 > 80%（实际覆盖情况良好）
- [x] ~~Pom.xml 密码硬编码修复~~ ✅ 已解决

### 10. 🔮 security_features.md 设计规划落地

**现状**：`security_features.md` 中记录的以下类均为设计规划，代码未实现：  
- `PasswordPolicyValidator`：密码复杂度校验
- `DataMaskingUtil`：邮箱/IP 脎敏工具
- `SecurityAuditService`：安全审计日志
- `SecurityMetrics`：Prometheus 安全指标
- `AnomalyDetectionService`：异常登录检测
- `ThreatDetectionService`：实时威胁检测
- `AutomatedSecurityResponse`：自动化安全响应
- HTTPS 强制、CORS 精细化配置

**建议**：根据面试价值和实际需要逐步实现，优先级从高到低：PasswordPolicyValidator > DataMaskingUtil > SecurityMetrics。

---

### 11. ⭐ LoginResponse 字段名与 README 一致性

**现状**：`LoginResponse.java` 中字段名为 `token`，但 Flutter 端 mock 数据和行业惯例通常使用 `accessToken` 以区分于 `refreshToken`。  
**建议**：评估是否将 `token` 重命名为 `accessToken`，同步更新 Flutter 端模型。

---

## 📊 检查清单

### Flutter 对接
- [x] Flutter mock 数据补全 `messageId` 字段 ✅
- [x] Flutter projects.json 后端技术栈修正为 Spring Boot ✅
- [x] Flutter aboutMe.json 替换为真实简历数据 ✅
- [x] Flutter login/refresh.json userId 修正为数字类型 ✅
- [x] V1 DB 种子数据替换为真实简历内容 ✅
- [x] 创建 `docs/api_reference.md` 完整接口文档 ✅
- [ ] Flutter 端适配 `ProjectDto.businessId` 字段
- [ ] Flutter 端适配 `StatDto.id` vs `businessId` 映射
- [ ] Flutter dev 环境配置指向后端 API URL
- [ ] V2 迁移脚本（已部署环境的数据更新）

---

📅 **最后更新**: 2026-04-05