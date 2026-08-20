# ListenPortfolioBackend

[![Backend CI](https://github.com/listen2code/ListenPortfolioBackend/actions/workflows/ci.yml/badge.svg)](https://github.com/listen2code/ListenPortfolioBackend/actions/workflows/ci.yml)

ListenPortfolioBackend 是 `ListenPortfolioFlutter` 的支撑型后端，基于 Spring Boot 3.2.1 构建。它的首要目标不是做成一个“泛企业平台样板”，而是为 Flutter 作品集 App 提供可信、可维护、可联调的 REST API，同时保留一定工程深度用于展示架构判断。

当前文档遵循两条规则：

- **主文只描述已落地能力，或明确标注的目标态**
- **实现中仍有差距的能力，会明确写出现状与风险**

## 🎯 项目定位

- **主定位**：Flutter Portfolio 的支撑型后端
- **次定位**：展示 Spring Boot、MyBatis-Plus、JWT、Redis、监控、测试等工程实践
- **非主定位**：把所有企业级能力都塞进首页叙事

## ✅ 当前已实现

### 核心业务能力

- **认证流程**：注册、登录、刷新 Token、忘记密码、修改密码、退出登录、账号注销
- **作品集 API**：`/v1/projects`、`/v1/aboutMe`、`/v1/user`
- **动态国际化支持**：根据 `Accept-Language` 请求头动态分发中/英/日多语言数据
- **统一响应结构**：`result`、`messageId`、`message`、`body`
- **密码重置邮件**：SMTP + 邮件模板发送

### 工程能力

- **JWT + Spring Security**：无状态鉴权
- **MyBatis-Plus 3.5.7 ORM**：全量替换 Hibernate/JPA，强类型 `LambdaQueryWrapper`，无 OSIV 与懒加载陷阱
- **Redis Token 黑名单**：退出登录、修改密码后立即失效当前 Token
- **Refresh Token 持久化与吊销**：Redis 存储，支持单个/全部吊销（修改密码、注销时自动清除）
- **AOP 限流**：按 IP / EMAIL / TOKEN / USER / CUSTOM 维度限流
- **Flyway 迁移**：应用启动时执行数据库迁移（V1 建表 + V2 种子数据）
- **Prometheus / Grafana / Actuator**：基础监控与健康检查
- **结构化 JSON 日志**：便于后续接入 ELK / Loki
- **GitHub Actions CI/CD**：Push/PR 自动编译、测试、JaCoCo 报告、SSH 部署到 AWS EC2
- **标准 Gradle 8.5 构建支持**：提供标准 Gradle 脚本与 Wrapper，支持在 Android Studio / IDEA 中一键导入、调试与运行单元测试
- **JaCoCo + SpotBugs**：覆盖率与静态分析工具链已接入
- **Docker 全栈部署**：App (Alpine JRE) + MySQL + Redis + Prometheus + Grafana
- **账号软删除**：`delete-account` 标记删除并释放唯一索引，种子用户受保护

## 📡 当前 API 概览

### 认证接口

| 方法 | 路径 | 说明 |
|------|------|------|
| `POST` | `/v1/auth/signUp` | 注册 |
| `POST` | `/v1/auth/login` | 登录 |
| `POST` | `/v1/auth/refresh` | 刷新 Token |
| `POST` | `/v1/auth/forgot-password` | 发送密码重置邮件 |
| `POST` | `/v1/auth/reset-password` | 重置密码 |
| `POST` | `/v1/auth/logout` | 公开登出（吊销 Refresh Token，无需 Access Token） |

### 用户接口

| 方法 | 路径 | 说明 |
|------|------|------|
| `GET` | `/v1/user?id={id}` | 获取用户摘要 |
| `POST` | `/v1/user/logout` | 退出登录 |
| `POST` | `/v1/user/change-password` | 修改密码 |
| `POST` | `/v1/user/upload-avatar` | 上传头像（Base64） |
| `DELETE` | `/v1/user/delete-account` | 软删除账号 |

### 作品集接口

| 方法 | 路径 | 认证 | 说明 |
|------|------|------|------|
| `GET` | `/v1/projects` | 公开 | 获取项目列表（支持 Accept-Language 国际化） |
| `GET` | `/v1/aboutMe` | 需要 | 获取关于我信息（支持 Accept-Language 国际化） |

### 统一响应格式

```json
{
  "result": "0",
  "messageId": "COMMON_SUCCESS",
  "message": "Success",
  "body": {}
}
```

### 登录响应 `body`

```json
{
  "token": "eyJ...",
  "refreshToken": "eyJ...",
  "userId": 1
}
```

注意：当前字段名是 **`token`**，不是 `accessToken`。

## 🏗️ 代码结构

```text
src/main/java/com/listen/portfolio/
├── api/v1/          # Controller + DTO (projects, about, auth, user)
├── service/         # 业务编排、事务边界、DTO 装配与多语言转换
├── mapper/          # MyBatis-Plus Mapper 接口与 SQL 映射
├── entity/          # MyBatis-Plus Entity 实体类
└── common/          # 配置、JWT、异常、响应模型、切面、工具类等横切能力
```

### 当前分层原则

- **Controller**：参数校验、调用 Service、返回 `ApiResponse`
- **Service**：业务规则、事务边界、Entity -> DTO 装配与国际化
- **Mapper**：MyBatis-Plus 数据访问
- **Entity**：持久化映射模型，不直接作为 API 响应

## 🚀 本地启动

### 环境要求

- Java 17+
- MySQL 8+
- Redis 7+

### 1. 创建数据库

```bash
mysql -u root -p -e "CREATE DATABASE portfolio CHARACTER SET utf8mb4 COLLATE utf8mb4_bin;"
```

### 2. 配置环境变量（可选）

```bash
# PowerShell
$env:DB_USERNAME="your_db_user"
$env:DB_PASSWORD="your_db_password"
$env:JWT_SECRET="your-super-strong-secret-key-at-least-256-bits-long"
$env:MAIL_PASSWORD="your-gmail-app-password"
```

### 3. 启动应用

```bash
# 使用 Gradle Wrapper 启动
./gradlew bootRun
```

说明：

- 默认本地配置由 `application.properties` 提供
- Docker 环境通过 `SPRING_PROFILES_ACTIVE=docker` 使用 `application-docker.properties`
- Flyway 当前主路径是**应用启动时自动迁移**，不是手动执行 Maven Flyway 命令

### 4. 常用地址

| 说明 | 地址 |
|------|------|
| Swagger UI | http://localhost:8080/swagger-ui.html |
| OpenAPI JSON | http://localhost:8080/v3/api-docs |
| Health | http://localhost:8080/actuator/health |
| Prometheus | http://localhost:8080/actuator/prometheus |

## 🔐 当前安全与可靠性机制

- **BCrypt** 密码哈希
- **无状态 JWT 认证**
- **Redis Token 黑名单**
- **AOP 限流**
- **全局异常处理**
- **敏感配置环境变量化**

### JWT 流程摘要

```text
登录 -> 生成 token + refreshToken
请求 -> JwtRequestFilter 校验 Bearer Token
退出登录 / 修改密码 -> 当前 token 进入 Redis 黑名单
```

### 限流说明

当前限流基于 Redis 计数器实现，已经可用，但**不是文档里曾经描述的滑动窗口 ZSet 版本**。如果后续需要更高精度，再升级算法。

## 🧪 测试与质量

### 常用命令

```bash
./gradlew compileJava
./gradlew test
./gradlew jacocoTestReport
./gradlew spotbugsMain
```

### 当前状态判断

- 已有 Controller / Service / Repository / Integration 测试基础
- JaCoCo 和 SpotBugs 工具链已接入
- **GitHub Actions CI/CD 已接入**：Push/PR 自动触发编译、测试与部署
- 一部分测试依赖 Redis，本地未启动 Redis 时会失败

## 🐳 Docker 与部署

### Docker 全栈启动

```bash
cp .env.example .env
./docker_deploy.ps1
```

### 当前包含服务

- Spring Boot App
- MySQL
- Redis
- Prometheus
- Grafana

### 当前部署判断

- **本地开发与 Docker 联调路径清晰**
- **JAR 部署是当前更符合项目定位的主路径**
- **WAR 仍可构建，但不是首页主叙事重点**

## ⚠️ 当前限制与风险

- **部分文档历史上写得比实现更理想化**：目前正在持续收口

## 📚 文档索引

- `docs/todo.md`：当前执行路线图
- `docs/api_reference.md`：接口说明
- `docs/security_features.md`：安全能力设计稿（并非全部已实现）

## 🎯 目标态（明确未全部实现）

以下内容是后续规划，不代表已经全部落地：

- 更完整的 HTTPS / 生产部署方案
- 测试覆盖率持续提升
- 限流算法升级（当前为固定窗口，可升级为滑动窗口）

## 🧾 待删除备份区

以下表述已从主文降级，先保留在此，后续继续清理：

- 不再把 `WAR` 部署能力放在 README 首页高位
- 不再把"企业级平台化"作为本项目的主叙事
- 不再使用 `accessToken` 指代登录返回字段，当前实现字段名是 `token`

---

📅 **最后更新**: 2026-08-05 | 🏷️ **版本**: 0.0.1-SNAPSHOT | 👤 **作者**: Listen — listen2code@gmail.com | [GitHub](https://github.com/listen2code)