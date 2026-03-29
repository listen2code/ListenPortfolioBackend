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

### 📈 当前评分

| 维度 | 分数 | 说明 |
|------|------|------|
| 架构设计 | 8/10 | 分层清晰，职责分明 |
| 安全性 | 8/10 | JWT + 黑名单 + BCrypt，缺限流 |
| 测试覆盖 | 7/10 | Controller/Service/Repository 均有，集成测试较少 |
| 可观测性 | 8/10 | Prometheus + Grafana + 结构化日志 |
| 文档完善 | 8/10 | README + Swagger UI |
| 部署就绪 | 7/10 | Docker 完整，缺自动迁移和 HTTPS |

---

## 🔴 高优先级

### 1. forgotPassword 发送真实邮件

**现状**：`POST /v1/auth/forgot-password` 当前行为是将密码重置为默认值，并不发送邮件通知用户。  
**目标**：向用户注册邮箱发送包含重置链接或临时密码的邮件。

**实施步骤**：
```
1. 引入 spring-boot-starter-mail 依赖
2. 配置 SMTP（环境变量注入：MAIL_HOST / MAIL_USERNAME / MAIL_PASSWORD）
3. 创建 EmailService，使用 JavaMailSender 发送 HTML 邮件
4. 修改 AuthService.forgotPassword()，调用 EmailService
5. 可选：生成带过期时间的重置 Token，存储到 Redis
```

**验收标准**：调用接口后，注册邮箱收到邮件；无效邮箱返回合理错误。

---

### 2. Docker 容器自动执行 Flyway 迁移

**现状**：`spring.flyway.enabled=false`，需要手动执行 `./mvnw flyway:migrate`。  
**目标**：容器启动时自动执行迁移，无需手动操作。

**实施步骤**：
```
1. 将 application.properties 中 spring.flyway.enabled 改为 true（或通过环境变量控制）
2. 在 docker-compose.yml 的 app 服务中添加 SPRING_FLYWAY_ENABLED=true
3. 确认迁移脚本幂等（已有 V1__ 脚本的情况下，测试 baseline 策略）
4. 测试容器冷启动和重启场景
```

**验收标准**：`docker compose up` 后数据库自动建表，无需手动干预。

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

## 🟡 中优先级

### 4. Refresh Token 持久化与吊销

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

### 5. 认证接口限流保护

**现状**：`/v1/auth/login`、`/v1/auth/signUp` 等接口无限流，存在暴力破解风险。  
**目标**：对认证接口按 IP 限制请求频率。

**实施步骤**：
```
1. 引入 Bucket4j 或 Spring Retry 限流方案
2. 对 /v1/auth/login 配置：同一 IP 1 分钟内最多 10 次
3. 超限返回 HTTP 429 Too Many Requests
4. 可结合 Redis 实现分布式限流
```

**验收标准**：超出频率限制后返回 429，正常请求不受影响。

---

### 6. 补全测试覆盖

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

### 7. 生产环境 HTTPS / TLS

**现状**：应用仅支持 HTTP，生产环境流量未加密。  
**目标**：通过反向代理（Nginx / AWS ALB）终止 TLS，或直接在应用层配置证书。

---

### 8. 配置文件拆分

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

### 9. Pom.xml Flyway 插件密码硬编码

**现状**：`pom.xml` 的 Flyway Maven 插件配置中密码硬编码（`Ls-88888888`）。  
**目标**：改为读取 Maven 属性或环境变量。

```xml
<password>${env.DB_PASSWORD}</password>
```

---

## 📋 检查清单

### 安全
- [x] JWT Token 认证
- [x] BCrypt 密码哈希
- [x] Token 黑名单（Redis）
- [x] 敏感配置环境变量化
- [ ] Refresh Token 持久化与吊销
- [ ] 认证接口限流
- [ ] 生产环境 HTTPS

### 功能完整性
- [x] 注册 / 登录 / 刷新 Token
- [x] 修改密码（含 Token 失效）
- [x] 退出登录（Token 黑名单）
- [x] 账号注销
- [ ] 忘记密码发送真实邮件

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
- [ ] 测试覆盖率 > 80%
- [ ] Pom.xml 密码硬编码修复

---

📅 **最后更新**: 2026-03-29