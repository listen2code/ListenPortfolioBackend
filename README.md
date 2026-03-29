# ListenPortfolioBackend

ListenPortfolioBackend 是 [ListenPortfolioFlutter](README-app.md) 的后端 REST API，基于 Spring Boot 4.0.1 构建，提供安全的 JWT 认证、作品集数据管理和完整的可观测性支持。

**生产环境 API**: `https://api.lPortfolio.com`

**主要能力：**
- **完整认证流程**：注册、登录、Token 刷新、忘记密码、修改密码（自动 Token 失效）、账号注销、退出登录
- **作品集 API**：关于我（需登录）、项目列表（公开）、用户信息
- **Token 黑名单**：基于 Redis 的 JWT 黑名单，支持退出登录和密码修改时立即失效
- **可观测性**：Prometheus 指标、Grafana 仪表板、结构化 JSON 日志
- **代码质量**：JaCoCo 覆盖率报告、SpotBugs + Find Security Bugs 静态分析
- **Docker 就绪**：MySQL 8 + Redis 7 + Prometheus + Grafana 完整栈

## 🛠️ 技术栈

| 类别 | 技术 | 版本 |
|------|------|------|
| 框架 | Spring Boot | 4.0.1 |
| 语言 | Java | 17 |
| 安全 | Spring Security + JWT (jjwt) | 6.x / 0.11.5 |
| 数据库 | MySQL + Spring Data JPA + Hibernate | 8.0 / 6.x |
| 连接池 | HikariCP | (内置) |
| DB 迁移 | Flyway | 11.14.1 |
| 缓存 / 黑名单 | Redis | 7.2 |
| API 文档 | SpringDoc OpenAPI / Swagger UI | 2.7.0 |
| 监控 | Actuator + Micrometer + Prometheus | — |
| 日志 | Logstash Logback Encoder（JSON 结构化）| 8.1 |
| 测试 | JUnit 5 + Mockito + JaCoCo | 0.8.8 |
| 静态分析 | SpotBugs + Find Security Bugs | 4.7.3.6 / 1.12.0 |
| 构建 | Maven Wrapper | — |

## 🚀 快速开始

### 环境要求
- Java 17+
- MySQL 8.0+
- Redis 7.x（Token 黑名单）

### 1. 克隆与配置

```bash
git clone <repository-url>
cd ListenPortfolioBackend
```

配置环境变量（`.env` 文件或系统环境变量）：

```bash
DB_URL=jdbc:mysql://localhost:3306/portfolio?useSSL=false&serverTimezone=Asia/Tokyo&collation=utf8mb4_bin&allowPublicKeyRetrieval=true
DB_USERNAME=root
DB_PASSWORD=your_password
JWT_SECRET=your-super-strong-secret-key-at-least-256-bits-long
JWT_EXPIRATION=300000          # 5 分钟（毫秒）
JWT_REFRESH_EXPIRATION=86400000   # 24 小时（毫秒）
```

### 2. 初始化数据库

```bash
# 创建空数据库
mysql -u root -p -e "CREATE DATABASE portfolio CHARACTER SET utf8mb4 COLLATE utf8mb4_bin;"

# 执行 Flyway 迁移（自动创建表结构 + 初始数据）
./mvnw flyway:migrate
```

### 3. 启动应用

```bash
./mvnw spring-boot:run -DskipTests
```

### 4. 访问地址

| 说明 | 地址 |
|------|------|
| Swagger UI API 文档 | http://localhost:8080/swagger-ui.html |
| OpenAPI JSON | http://localhost:8080/v3/api-docs |
| 健康检查 | http://localhost:8080/actuator/health |
| Prometheus 指标 | http://localhost:8080/actuator/prometheus |

## 📡 API 接口

### 认证 — `/v1/auth/**`（公开，无需 Token）

| 方法 | 路径 | 说明 |
|------|------|------|
| `POST` | `/v1/auth/signUp` | 注册新用户 |
| `POST` | `/v1/auth/login` | 登录 → 返回 `accessToken`、`refreshToken`、`userId` |
| `POST` | `/v1/auth/refresh?refreshToken=` | 刷新 accessToken |
| `POST` | `/v1/auth/forgot-password` | 通过邮箱重置密码 |

### 用户 — `/v1/user/**`（需要 Bearer Token）

| 方法 | 路径 | 说明 |
|------|------|------|
| `GET` | `/v1/user?id={id}` | 根据 ID 获取用户摘要 |
| `POST` | `/v1/user/logout` | 退出登录 — 将 Token 加入 Redis 黑名单 |
| `POST` | `/v1/user/change-password` | 修改密码 — 立即失效当前 Token，需重新登录 |
| `DELETE` | `/v1/user/delete-account` | 永久删除账号 |

### 作品集

| 方法 | 路径 | 认证 | 说明 |
|------|------|------|------|
| `GET` | `/v1/projects` | 公开 | 获取所有项目 |
| `GET` | `/v1/about` | 需要 | 获取关于我信息（统计、经历、教育、技能、语言） |

### 统一响应格式

```json
{
  "result": "0",
  "message": "Success",
  "body": { ... }
}
```

登录响应 body：

```json
{
  "accessToken": "eyJ...",
  "refreshToken": "eyJ...",
  "userId": 1
}
```

## 🏗️ 架构设计

### 实际目录结构

```
src/main/java/com/listen/portfolio/
├── api/v1/                          # 表现层：Controller + DTO
│   ├── auth/                        # AuthController + dto/
│   ├── user/                        # UserController + dto/
│   ├── projects/                    # ProjectController + dto/
│   └── about/                       # AboutMeController + dto/
├── service/                         # 业务逻辑：用例编排 + DTO 装配 + 事务边界
│   ├── AuthService.java
│   ├── UserService.java
│   ├── AboutMeService.java
│   ├── ProjectService.java
│   └── TokenBlacklistService.java
├── repository/                      # 数据访问：Spring Data JPA 接口
├── infrastructure/
│   └── persistence/entity/          # JPA 实体（仅属于持久化层）
│       ├── UserEntity.java          # + Stat / Experience / Education / Language / Skill
│       ├── ProjectEntity.java
│       └── ...
├── config/                          # 横切配置
│   ├── SecurityConfig.java
│   ├── GlobalExceptionHandler.java
│   ├── OpenApiConfig.java
│   ├── RedisConfig.java
│   ├── RequestLoggingFilter.java
│   └── WebConfig.java
├── jwt/                             # JWT 工具
│   ├── JwtUtil.java                 # Token 生成 / 验证 / 刷新
│   └── JwtRequestFilter.java        # 每次请求的 JWT 校验过滤器
└── common/                          # 共享模型
    ├── ApiResponse.java
    └── Constants.java
```

### 分层原则

- **Controller**：参数校验 → 调用 Service → 返回 `ApiResponse`。不写业务规则，不直接访问 Repository。
- **Service**：`@Transactional` 事务边界、业务规则、Entity→DTO 装配。OSIV 已关闭（`open-in-view=false`）— 懒加载必须在此层完成。
- **Repository**：仅做数据访问，返回 Entity / 投影。
- **Entity**：仅用于持久化映射，不对外暴露为 API 响应。

### JWT Token 流程

```
登录
  └─ AuthController.login()
       ├─ AuthenticationManager.authenticate()
       ├─ JwtUtil.generateToken()         → accessToken  (默认 5 分钟)
       └─ JwtUtil.generateRefreshToken()  → refreshToken (默认 24 小时)

每次请求认证
  └─ JwtRequestFilter
       ├─ 提取 "Authorization: Bearer <token>"
       ├─ TokenBlacklistService.isBlacklisted() → 在黑名单则拒绝
       └─ JwtUtil.validateToken() → 写入 SecurityContext

退出登录 / 修改密码
  └─ TokenBlacklistService.addToBlacklist(token, expiry)
       └─ Redis: SET token:blacklist:<token> 1 EX <ttl>
```

## 🔐 安全机制

- **BCrypt** 密码哈希
- **无状态 Session**（JWT，服务端无会话状态）
- **Token 黑名单**：Redis 存储，退出登录和修改密码时立即失效
- **公开路径**：`/v1/auth/**`、`/v1/projects/**`、Actuator 健康/Prometheus、Swagger UI
- **CSRF 禁用**（无状态 JWT API）
- 所有敏感配置通过环境变量注入（`DB_PASSWORD`、`JWT_SECRET` 等）

## 🧪 测试与代码质量

### 运行测试

```bash
./mvnw test
```

### 覆盖率报告（JaCoCo）

```bash
check-coverage.bat        # Windows：运行测试 + 打开 HTML 报告
# 或
./mvnw clean test jacoco:report
# 报告位置：target/site/jacoco/index.html
```

### SpotBugs 静态分析

```bash
check-spotbugs.bat        # Windows：运行分析 + 打开 HTML 报告
# 或
./mvnw spotbugs:check
# 报告位置：target/site/spotbugs/spotbugs.html
```

### 测试覆盖范围

| 测试文件 | 覆盖范围 |
|---------|---------|
| `UserControllerTest` | UserController 全接口 + 分支 |
| `UserServiceTest` | UserService 业务逻辑 |
| `UserRepositoryTest` | 用户数据访问 |
| `AboutMeControllerTest` | AboutMeController |
| `AboutMeServiceTest` | AboutMeService + DTO 映射 |
| `AuthControllerTest` | 认证接口 |
| `ProjectControllerTest` | 项目接口 |
| `TokenBlacklistServiceTest` | Redis 黑名单操作 |
| `LogoutIntegrationTest` | 退出登录流程 |
| `LogoutTokenExtractionTest` | Token 提取分支 |

## 🐳 Docker 部署

### 完整栈（应用 + MySQL + Redis + Prometheus + Grafana）

```bash
cp .env.example .env    # 配置 DB / JWT 等参数（参考 .env 文件）

./docker_deploy.ps1     # PowerShell 一键部署
./docker_stop.ps1       # 停止所有服务
```

### 服务端口

| 服务 | 端口 | 说明 |
|------|------|------|
| App | 8080 | Spring Boot API |
| MySQL | 3307 | 数据库 |
| Redis | 6379 | Token 黑名单 |
| Prometheus | 9090 | 指标采集 |
| Grafana | 3000 | 仪表板（admin/admin123） |

### Docker Compose Profiles

| Profile | 包含服务 |
|---------|---------|
| `local` | 仅监控（Prometheus + Grafana） |
| `full` | 完整栈 |
| `staging` | 预发布环境 |
| `prod` | 生产环境 |

## 📊 监控与日志

- **健康检查**：`GET /actuator/health` — 存活 / 就绪探针（支持 Kubernetes）
- **Prometheus**：`GET /actuator/prometheus` — JVM、HTTP、连接池指标
- **Grafana**：端口 3000，预配置仪表板
- **结构化日志**：JSON 格式，兼容 ELK / Loki 日志平台
- **请求日志**：`RequestLoggingFilter` 记录所有入站请求

## 🔮 未来规划

- **邮件发送**：`forgot-password` 发送真实邮件（当前为重置为默认密码）
- **Docker 自动迁移**：容器启动时自动执行 Flyway 迁移
- **Refresh Token 持久化**：将刷新 Token 存储到 DB/Redis，支持主动吊销
- **限流保护**：对认证接口添加速率限制，防止暴力破解
- **HTTPS/TLS**：生产环境强制 HTTPS

---

📅 **最后更新**: 2026-03-29 | 🏷️ **版本**: 0.0.1-SNAPSHOT | 👤 **作者**: Listen — listen2code@gmail.com | [GitHub](https://github.com/listen2code)