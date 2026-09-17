# API Reference（接口参考与架构设计指南）

> 基于真实简历与生产实践设计，数据结构与 Flyway `V1`（DDL 建表）及 `V2`（DML 业务数据）迁移脚本完全对齐。
>
> - **Local Base URL**: `http://localhost:8080/v1`  
> - **Production Base URL**: `http://13.218.192.181/api/v1` (AWS EC2 + Nginx 反向代理)  
> - **统一协议**: HTTP/1.1 RESTful JSON，全接口封装为统一响应模型 `ApiResponse<T>`  
> - **当前版本**: `v2.1`（整合 Project 6 原生记账应用、6 大技能雷达维度、动态多语言降级引擎）

---

## 📑 目录

1. [🏛️ 核心设计思路与架构哲学](#1-核心设计思路与架构哲学)
2. [📦 统一响应契约规范 (Contract & Error Codes)](#2-统一响应契约规范-contract--error-codes)
3. [🌐 动态多语言回退机制 (i18n Engine)](#3-动态多语言回退机制-i18n-engine)
4. [🔐 认证与用户接口规范 (Auth & User APIs)](#4-认证与用户接口规范-auth--user-apis)
5. [📋 项目与个人履历接口规范 (Projects & AboutMe APIs)](#5-项目与个人履历接口规范-projects--aboutme-apis)
6. [🔬 重点与难点代码实现深度剖析 (Deep-Dive)](#6-重点与难点代码实现深度剖析-deep-dive)
7. [⚠️ 现有实现不足与演进路线 (Limitations & Roadmap)](#7-现有实现不足与演进路线-limitations--roadmap)
8. [🔄 Backend DTO vs Flutter Model 契约适配表](#8-backend-dto-vs-flutter-model-契约适配表)
9. [📊 数据库种子表结构映射 (Database Schema Mapping)](#9-数据库种子表结构映射-database-schema-mapping)

---

## 1. 🏛️ 核心设计思路与架构哲学

本服务作为移动端与 Web 端的统一作品集及鉴权中台，架构设计围绕**高内聚低耦合**、**无状态伸缩**、**纵深安全防御**与**优雅跨端适配**展开：

```mermaid
graph TD
    Client[客户端: Flutter App / Web / Admin] -->|HTTP/REST + Accept-Language + X-Request-Id| Nginx[Nginx Reverse Proxy :80]
    Nginx -->|Proxy Pass /api/ -> :8080/| LoggingFilter[RequestLoggingFilter (MDC requestId & Timer)]
    LoggingFilter --> RateLimitAspect[RateLimitAspect (AOP IP/User/Email 限流)]
    RateLimitAspect --> JwtFilter[JwtRequestFilter (Bearer Token 鉴权 + Redis 黑名单)]
    JwtFilter --> Controller[Controller 层 (API 路由 / DTO 校验)]
    Controller --> Service[Service 业务聚合层 (只读事务 / 领域逻辑)]
    Service -->|多语言解析| I18n[I18nUtils (Locale 优先与默认降级)]
    Service -->|数据访问| Mapper[MyBatis-Plus Mapper 层]
    Mapper --> MySQL[(MySQL 8.0 数据持久化)]
    Service -->|缓存/黑名单/限流/Refresh| Redis[(Redis 7.2 缓存与令牌会话)]
```

### 1.1 分层架构与领域隔离 (Layered Architecture)
- **Controller 层**：负责参数校验（Jakarta Validation）、Swagger 文档声明与 HTTP 契约转换，严禁直接访问数据库 Mapper。
- **Service 层**：聚合业务逻辑、开启 `@Transactional` 事务管理、组装关联子集合并调用 `I18nUtils` 完成多语言动态映射。
- **Entity 与 DTO 严格解耦**：数据库实体 `Entity` 绝不对外暴露，统一转换为强类型 `DTO`；数值 ID 在跨语言交互时通过 `@JsonSerialize(using = ToStringSerializer.class)` 序列化为 String，根除 JavaScript/Dart 的 64 位大整数精度截断 Bug。

### 1.2 无状态鉴权与双 Token 生命周期 (Dual-Token Pattern)
- **Access Token（5 分钟短周期）**：无状态自包含 JWT，携带用户 ID、角色与签发时间。网关与过滤器仅校验签名与本地有效期，极大减轻数据库与 Redis 读压力。
- **Refresh Token（7 天长周期）**：有状态持久化至 Redis（Key 格式 `token:refresh:<username>:<token>`）。客户端在 Access Token 过期时无感调用 `/v1/auth/refresh` 轮转续期。
- **即时吊销与黑名单机制**：用户登出、修改密码或软删除账号时，未过期的 Access Token 加入 Redis 黑名单直至 TTL 耗尽；同时主动清除 Redis 中对应用户的所有 Refresh Token，实现全端会话瞬间失效。

### 1.3 纵深防御安全策略 (Defense-in-Depth)
1. **密码安全**：强哈希算法 BCrypt（工作因子 10），加盐不可逆存储，有效防御彩虹表与碰撞攻击。
2. **声明式防刷限流**：基于自定义 `@RateLimit` 注解结合 Redis 计数器，支持按 `IP`、`USER`、`EMAIL`、`TOKEN` 多维控制单位时间请求频次。
3. **敏感操作枚举防护**：找回密码接口对已存在和不存在的邮箱统一返回成功提示文案，杜绝恶意遍历攻击。
4. **头像多级安全防腐**：限制 URL 长度防 CRLF 注入；限制 Base64 字符串长度（<= 3MB）；校验图片文件头魔数（Magic Bytes）白名单（PNG/JPEG/GIF/WebP/SVG），阻断恶意 Webshell 上传。
5. **核心数据保护**：种子管理员账户（`userId = 1`）受业务层硬编码逻辑保护，禁止软删除，修改权限受限。

---

## 2. 📦 统一响应契约规范 (Contract & Error Codes)

全系统遵循统一的响应包装对象 `ApiResponse<T>`，客户端解析无歧义。

### 2.1 基础 JSON 结构
```json
{
  "result": "0",          // 字符串状态码: "0" 表示业务成功; 其他非零字符串表示业务错误
  "messageId": "",        // 机器可读的错误标识/国际化 Key (例如: "INVALID_TOKEN", "BIZ_0507")
  "message": "success",   // 面向开发者的提示信息
  "body": { ... },        // 泛型业务数据载荷 (支持 Object, List, 或 null)
  "success": true,        // 布尔值快捷判断辅助字段
  "code": ""             // 兼容旧版本错误码字段
}
```

### 2.2 标准错误码体系 (ErrorCode)

| messageId | HTTP Status | 业务含义 | 典型触发场景 |
|-----------|-------------|---------|-------------|
| `BAD_REQUEST` | 400 | 请求语法或参数校验失败 | DTO 字段 `@NotBlank` 或 `@Size` 不符、JSON 语法错误 |
| `UNAUTHORIZED` | 401 | 未认证或令牌已失效 | 未传 Authorization 头、JWT 格式错误、Token 已过期或在黑名单中 |
| `FORBIDDEN` | 403 | 无权访问该资源 | 普通用户越权操作种子管理员资源 |
| `NOT_FOUND` | 404 | 目标资源不存在 | 用户 ID 或项目 ID 在数据库中不存在 |
| `INTERNAL_ERROR` | 500 | 服务器未捕获异常 | 数据库连接瞬断、空指针等系统故障 |
| `RATE_LIMIT_EXCEEDED` | 429 | 请求频率超限 | 客户端在指定时间窗口内请求次数超过 `@RateLimit` 阈值 |
| `INVALID_TOKEN` | 400 | 令牌失效或不合法 | 密码重置 Token 不匹配或超时、Refresh Token 已被吊销 |
| `USER_NOT_FOUND` | 404 | 目标用户未找到 | 根据 ID 或用户名未查到有效记录 |
| `USER_ALREADY_EXISTS`| 409 | 用户名或邮箱冲突 | 注册时已有同名或相同邮箱用户 |
| `PASSWORD_MISMATCH` | 400 | 旧密码验证不通过 | 修改密码时输入的原密码与数据库不一致 |
| `BIZ_0507` | 400 | 头像格式或大小非法 | 头像 Base64 超过 3MB、二进制超 2MB、魔数非合法图片格式 |

---

## 3. 🌐 动态多语言回退机制 (i18n Engine)

系统在数据访问层采用**“单表扩展字段 + 上下文回退解析”**的设计，避免多表 JOIN 造成的查询性能开销。

### 3.1 字段设计规范
数据库实体中为多语言文本维护统一的字段族：
- 默认字段（默认英文）：如 `title`, `project_desc`, `company`, `bio`
- 中文字段：后缀 `_zh`，如 `title_zh`, `project_desc_zh`, `company_zh`, `bio_zh`
- 日文字段：后缀 `_ja`，如 `title_ja`, `project_desc_ja`, `company_ja`, `bio_ja`

### 3.2 动态解析流转流程
```text
Client Request (Header: Accept-Language: zh-CN)
   ↓
Spring Framework LocaleResolver (解析为 Locale.SIMPLIFIED_CHINESE)
   ↓
LocaleContextHolder.getLocale() (绑定在当前请求线程 ThreadLocal)
   ↓
Service 遍历 Entity 调用 I18nUtils.getLocalizedText(defaultVal, zhVal, jaVal, locale)
   ↓
优先级判断:
   1. 当前语言为 "zh" 且 zhVal 非空 -> 返回 zhVal
   2. 当前语言为 "ja" 且 jaVal 非空 -> 返回 jaVal
   3. 目标语言为空或客户端为其他语言 -> 优雅回退并返回 defaultVal (英文)
```

---

## 4. 🔐 认证与用户接口规范 (Auth & User APIs)

### 4.1 POST `/v1/auth/signUp` — 用户注册
创建普通用户账号并初始化安全散列密码。
- **Rate Limit**: IP 限流 10次/分钟
- **认证要求**: 公开接口

**Request Body**:
```json
{
  "userName": "testUser",
  "email": "test@example.com",
  "password": "SecurePassword123"
}
```

**Response** (201 Created):
```json
{
  "result": "0",
  "messageId": "",
  "message": "User registered successfully",
  "body": null,
  "success": true
}
```

---

### 4.2 POST `/v1/auth/login` — 用户登录认证
校验用户名与密码，生成双 Token。
- **Rate Limit**: IP 限流 10次/分钟
- **认证要求**: 公开接口

**Request Body**:
```json
{
  "userName": "Listen",
  "password": "your-password"
}
```

**Response** (200 OK):
```json
{
  "result": "0",
  "messageId": "",
  "message": "",
  "body": {
    "userId": 1,
    "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJMaXN0ZW4iLCJpYXQiOjE3OD...",
    "refreshToken": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJMaXN0ZW4iLCJpYXQiOjE3OD..."
  },
  "success": true
}
```
> **字段说明**：
> - `token`: 5 分钟短周期 Access Token，供后续受保护接口放置于 `Authorization: Bearer <token>` 请求头。
> - `refreshToken`: 7 天长周期刷新令牌，保存在客户端安全存储（如 Flutter Secure Storage）。
> - `userId`: `Long` 类型数字，客户端使用 `@ToStringConverter()` 适配。

---

### 4.3 POST `/v1/auth/refresh` — 轮转刷新令牌
使用长周期 Refresh Token 换发全新 Access Token。
- **Rate Limit**: IP 限流 20次/分钟
- **认证要求**: 公开接口

**Query Param**:
- `refreshToken`: 客户端持有的刷新令牌

**Request**:
```http
POST /v1/auth/refresh?refreshToken=eyJhbGciOiJIUzI1NiJ9... HTTP/1.1
```

**Response** (200 OK):
```json
{
  "result": "0",
  "messageId": "",
  "message": "",
  "body": {
    "userId": 1,
    "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJMaXN0ZW4iLCJleHAiOjE3OD...",
    "refreshToken": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJMaXN0ZW4iLCJleHAiOjE3OD..."
  },
  "success": true
}
```

---

### 4.4 POST `/v1/auth/forgot-password` — 申请密码重置邮件
触发向用户邮箱发送重置密码临时 Token。
- **Rate Limit**: IP 限流 10次/分钟, 目标 Email 限流 10次/分钟
- **防枚举策略**: 无论邮箱是否存在，接口统一响应成功提示。

**Request Body**:
```json
{
  "email": "listen2code@gmail.com"
}
```

**Response** (200 OK):
```json
{
  "result": "0",
  "messageId": "",
  "message": "If the email exists, a password reset link has been sent",
  "body": null,
  "success": true
}
```

---

### 4.5 POST `/v1/auth/reset-password` — 重置密码
根据邮件中的有效 Token 设置新密码。
- **Rate Limit**: IP 限流 10次/分钟, Token 限流 10次/分钟

**Request Body**:
```json
{
  "token": "d748f219-c09a-4712-bb20-1a868a8bc8f1",
  "newPassword": "NewStrongPassword456"
}
```

**Response** (200 OK):
```json
{
  "result": "0",
  "messageId": "",
  "message": "Password reset successfully",
  "body": null,
  "success": true
}
```

---

### 4.6 GET `/v1/user?id={userId}` — 获取用户摘要
- **认证要求**: 必需 (`Authorization: Bearer <token>`)

**Response** (200 OK):
```json
{
  "result": "0",
  "messageId": "",
  "message": "success",
  "body": {
    "id": "1",
    "name": "Listen",
    "location": "Japan / Tokyo",
    "email": "listen2code@gmail.com",
    "avatarUrl": "https://api.dicebear.com/10.x/bottts/svg?seed=Listen"
  },
  "success": true
}
```

---

### 4.7 POST `/v1/user/upload-avatar` — 上传并更新用户头像
- **认证要求**: 必需 (`Authorization: Bearer <token>`)
- **权限限制**: 仅种子管理员用户（`userId = 1`）允许调用。
- **校验规则**:
  - 支持合法的 HTTP/HTTPS 图片 URL（<= 2048 字符，防 CRLF 注入）
  - 支持 Data URI Base64 图片（最大字符 3MB，折合二进制不超过 2MB）
  - 必须通过底层文件二进制魔数签名白名单（PNG, JPEG, GIF, WebP, SVG）

**Request Body**:
```json
{
  "avatar": "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNkYAAAAAYAAjCB0C8AAAAASUVORK5CYII="
}
```

**Response** (200 OK):
```json
{
  "result": "0",
  "messageId": "",
  "message": "success",
  "body": {
    "id": "1",
    "name": "Listen",
    "location": "Japan / Tokyo",
    "email": "listen2code@gmail.com",
    "avatarUrl": "data:image/png;base64,iVBORw0KGgoAAA..."
  },
  "success": true
}
```

**Response** (400 Bad Request — 图片魔数或体积不合法):
```json
{
  "result": "1",
  "messageId": "BIZ_0507",
  "message": "Invalid avatar image format or size",
  "body": null,
  "success": false
}
```

---

### 4.8 POST `/v1/user/logout` — 退出登录
使当前 Access Token 立即失效（加入 Redis 黑名单），并吊销该用户的所有 Refresh Token。
- **认证要求**: 必需 (`Authorization: Bearer <token>`)

**Response** (200 OK):
```json
{
  "result": "0",
  "messageId": "",
  "message": "Logout successful",
  "body": null,
  "success": true
}
```

---

### 4.9 POST `/v1/user/change-password` — 修改密码
校验旧密码并更新新密码，更新完成后清空所有会话与令牌。
- **认证要求**: 必需 (`Authorization: Bearer <token>`)

**Request Body**:
```json
{
  "userId": "1",
  "oldPassword": "CurrentPassword123",
  "newPassword": "NewStrongPassword456"
}
```

---

### 4.10 DELETE `/v1/user/delete-account` — 注销/软删除账号
软删除指定用户（`deleted = 1` 并重命名邮箱与用户名避免占用唯一索引），同时吊销全部会话。
- **认证要求**: 必需 (`Authorization: Bearer <token>`)
- **特别保护**: 种子用户（`userId = 1`）受硬保护，禁止删除。

---

## 5. 📋 项目与个人履历接口规范 (Projects & AboutMe APIs)

### 5.1 GET `/v1/projects` — 获取全量项目列表
作品集核心接口，公开开放，支持无缝多语言切换。
- **认证要求**: 公开接口
- **多语言支持**: 依赖请求头 `Accept-Language: zh-CN` 或 `ja-JP`（默认回退为英文）

**Request**:
```http
GET /v1/projects HTTP/1.1
Host: 13.218.192.181
Accept-Language: zh-CN
```

**Response** (200 OK) — 包含全部 6 个经典项目（含最新原生 Android 记账应用）：
```json
{
  "result": "0",
  "messageId": "",
  "message": "",
  "body": [
    {
      "id": 1,
      "businessId": "lportfolio-flutter",
      "title": "lPortfolio Flutter 客户端",
      "subtitle": "当前项目",
      "desc": "个人作品集客户端应用（即本 App）。全面展示 Flutter 下的 Clean 架构、MVI 模式及高级 Riverpod 状态管理。",
      "imageUrl": "localhost/images/project1.jpg",
      "githubUrl": "https://github.com/listen2code/ListenPortfolioFlutter",
      "techStack": ["Clean Architecture", "Flutter", "MVI", "Riverpod"]
    },
    {
      "id": 2,
      "businessId": "listen-core-flutter",
      "title": "Listen Core 核心框架",
      "subtitle": "核心基础库",
      "desc": "为 Flutter 项目打造的底座框架，提供 MVI 基类、标准化网络请求封装及生命周期管理机制。",
      "imageUrl": "localhost/images/project2.jpg",
      "githubUrl": "https://github.com/listen2code/ListenCoreFlutter",
      "techStack": ["Architecture", "Dart", "Dio", "Riverpod"]
    },
    {
      "id": 3,
      "businessId": "listen-ui-kit",
      "title": "Listen UI 组件库",
      "subtitle": "公共 UI 库",
      "desc": "高复用的通用 UI 组件库，用于保持多款 Flutter 应用间视觉规范一致与快速迭代开发。",
      "imageUrl": "localhost/images/project3.jpg",
      "githubUrl": "https://github.com/listen2code/ListenUikitFlutter",
      "techStack": ["CustomPainter", "Design System", "Flutter"]
    },
    {
      "id": 4,
      "businessId": "portfolio-backend",
      "title": "服务端后台架构",
      "subtitle": "云端微服务",
      "desc": "本作品集的服务端实现，采用 Spring Boot + MySQL + Redis 架构，支持多语言数据动态分发与安全防护。",
      "imageUrl": "localhost/images/project4.jpg",
      "githubUrl": "https://github.com/listen2code/ListenPortfolioBackend",
      "techStack": ["Docker", "MySQL", "Redis", "Spring Boot"]
    },
    {
      "id": 5,
      "businessId": "tech-knowledge-base",
      "title": "技术知识库文章",
      "subtitle": "技术文章与文档",
      "desc": "近 10 年移动端与全栈开发的精选技术文章、架构设计笔记及实践经验总结。",
      "imageUrl": "localhost/images/project5.jpg",
      "githubUrl": "https://github.com/listen2code/article",
      "techStack": ["Documentation", "Knowledge Sharing", "Markdown"]
    },
    {
      "id": 6,
      "businessId": "listen-expense-tracker",
      "title": "Listen Expense Tracker 原生记账",
      "subtitle": "原生移动应用",
      "desc": "基于 Kotlin 2.x + Jetpack Compose + MVI + Room 离线优先架构打造的原生 Android 极简记账应用。支持 Google Credential Manager 原生账户认证、Google Drive 云端备份恢复、多维 Canvas 统计图表及桌面小组件。",
      "imageUrl": "localhost/images/project6.jpg",
      "githubUrl": "https://github.com/listen2code/ListenExpenseTracker",
      "techStack": ["Google Drive API", "Jetpack Compose", "Kotlin", "MVI", "Room"]
    }
  ],
  "success": true
}
```

---

### 5.2 GET `/v1/aboutMe` — 获取个人真实简历与综合档案
作品集个人背景核心接口，包含基于真实从业经历提炼的 6 段工作履历、教育经历、双外语能力认证与 6 大雷达技能体系。
- **认证要求**: 必需 (`Authorization: Bearer <token>`)
- **多语言支持**: 依赖请求头 `Accept-Language`，动态国际化履历描述与技能分类。

**Response** (200 OK) — 基于真实简历数据（英文默认模式）：
```json
{
  "result": "0",
  "messageId": "",
  "message": "success",
  "body": {
    "name": "Listen",
    "location": "Japan / Tokyo",
    "avatarUrl": "https://api.dicebear.com/10.x/bottts/svg?seed=Listen",
    "status": "available",
    "jobTitle": "Senior Android / Flutter Engineer",
    "bio": "Senior Android Engineer with 11+ years of mobile development experience and 3+ years in Flutter. Expertise in client architecture (componentization, plugin systems), performance optimization, and APM infrastructure. Key achievements include reducing Feed timeout rates from 1.5% to 0.3%, building full-stack APM monitoring platforms, and leading Flutter app development for securities trading at Rakuten. JLPT N1, BJT J2 certified, currently based in Tokyo, Japan.",
    "graduationYear": "2013",
    "github": "https://github.com/listen2code",
    "major": "softwareEngineering",
    "certifications": ["bjtJ2", "jlptN1"],
    "stats": [
      {
        "id": 1,
        "businessId": "android",
        "year": "11",
        "label": "androidExp",
        "tags": ["archDesign", "componentization", "perfOptimization"]
      },
      {
        "id": 2,
        "businessId": "flutter",
        "year": "3",
        "label": "flutterExp",
        "tags": ["cleanArchitecture", "riverpod", "stateManagement"]
      },
      {
        "id": 3,
        "businessId": "java_web",
        "year": "1",
        "label": "javaWeb",
        "tags": ["restApi", "springBoot"]
      }
    ],
    "experiences": [
      {
        "id": 1,
        "title": "Android / Flutter Engineer",
        "company": "LYC Corp. (Rakuten Securities Project)",
        "period": "2023.02 - Present",
        "description": "Lead developer for new securities Flutter app (60-person project site): architecture design, framework development, FIDO2 authentication integration, and Flutter version upgrades. Maintained Android stock trading app and conducted code reviews for team members."
      },
      {
        "id": 2,
        "title": "Android Engineer — Mobile Infrastructure",
        "company": "Hangzhou Youzan Technology Co., Ltd.",
        "period": "2021.10 - 2022.07",
        "description": "Built mobile APM stutter/ANR detection SDK with optimized data reporting and aggregation. Created full-stack monitoring dashboards (React/AntDesign frontend + Spring Boot backend with RESTful APIs). Participated in Commerce SDK Redux-pattern refactoring and WeChat Mall App iterations."
      },
      {
        "id": 3,
        "title": "Android Engineer",
        "company": "Hangzhou Yin'ai Network Technology Co., Ltd. (Duolu)",
        "period": "2019.11 - 2021.10",
        "description": "Established Feed monitoring system, reducing timeout rate from 1.5% to 0.3% and latency by 40%+. Led componentization (1+2 module mode + shell scaffolding) and plugin architecture (Shadow framework with auto-fallback). Built dev-stage performance tools and automated testing (44 Feed cases via AirTest)."
      },
      {
        "id": 4,
        "title": "Android Engineer",
        "company": "Hangzhou Qibei Technology Co., Ltd. (Qibei Bike / Dingda Transit)",
        "period": "2016.09 - 2019.08",
        "description": "Developed bike-sharing apps (Qibei Bike, Dingda Transit, Luban Operations) across 4+ major versions. Implemented hot-fix (Tinker), online performance monitoring (Matrix), MVP scaffolding, and reduced build time by 30%+ via Gradle optimization. Set up Jenkins CI pipeline with wireless ADB deployment."
      },
      {
        "id": 5,
        "title": "Android Engineer",
        "company": "Beijing Baidu Times Network Technology Co., Ltd. (Baidu Waimai)",
        "period": "2014.12 - 2016.06",
        "description": "Independently maintained delivery rider app (Xiaodu Knight v1.4-2.9). Designed dynamic GPS tracking strategy with location offset guard, reducing redundant uploads by 10%+. Developed PassSDK for unified B-side authentication with AES/JNI encryption. Built logistics development framework for multi-app scaffolding."
      },
      {
        "id": 6,
        "title": "Java Developer",
        "company": "Fuzhou NewLand Software Engineering Co., Ltd.",
        "period": "2013.05 - 2014.09",
        "description": "Developed business management and analytics modules for China Mobile support system (BOSS) using J2EE, S2SH framework, and Oracle database."
      }
    ],
    "education": [
      {
        "id": 1,
        "degree": "Bachelor of Software Engineering",
        "school": "Fujian University of Technology",
        "period": "2011.09 - 2013.06",
        "description": "Outstanding Graduation Thesis: Design and Implementation of CRM System Based on Intelligent Evaluation System"
      },
      {
        "id": 2,
        "degree": "Associate in Computer Applications",
        "school": "Fujian Normal University (IT College)",
        "period": "2008.09 - 2011.06",
        "description": "Fujian Provincial Outstanding Student, University Outstanding Graduate, First & Second Class Scholarships, Outstanding Student Cadre"
      }
    ],
    "languages": [
      { "id": 1, "name": "Japanese", "level": "JLPT N1 (131), BJT J2 (512)" },
      { "id": 2, "name": "Chinese", "level": "Native" },
      { "id": 3, "name": "English", "level": "CET-4" }
    ],
    "skills": [
      {
        "id": 1,
        "category": "Android Native",
        "score": 97,
        "items": [
          "Kotlin & Java Advanced",
          "Android SDK & Framework",
          "JNI / C++ & NDK",
          "Componentization Architecture",
          "Plugin (Shadow) & Hotfix"
        ]
      },
      {
        "id": 2,
        "category": "Flutter",
        "score": 93,
        "items": [
          "Dart Core & Async Mechanism",
          "Clean Architecture + MVI",
          "Riverpod State Management",
          "Canvas & CustomPainter Engine",
          "Platform Channel & FIDO2"
        ]
      },
      {
        "id": 3,
        "category": "Performance & APM",
        "score": 96,
        "items": [
          "Vsync Frame & Jank Monitor",
          "ANR & Stutter Detection SDK",
          "Feed Lag 40% Reduction",
          "Memory & GC Profiling",
          "Systrace & Perfetto Tracing"
        ]
      },
      {
        "id": 4,
        "category": "Architecture",
        "score": 94,
        "items": [
          "Clean Architecture",
          "MVI Unidirectional Flow",
          "Zone Distributed Tracing",
          "401 Concurrent Retry Queue",
          "Crash Safe Mode Circuit Breaker"
        ]
      },
      {
        "id": 5,
        "category": "Java & Backend",
        "score": 84,
        "items": [
          "1-Yr Java Server Experience",
          "Spring Boot Microservices",
          "RESTful API & Contract",
          "MySQL & Index Tuning",
          "Redis Cache & Docker"
        ]
      },
      {
        "id": 6,
        "category": "DevOps & CI/CD",
        "score": 89,
        "items": [
          "CI/CD (Jenkins / GitHub Actions)",
          "Gradle 30%+ Build Tuning",
          "Custom Lint Rule Sets",
          "Automated Testing (540+ Suites)",
          "Shorebird OTA Code Push"
        ]
      }
    ]
  },
  "success": true
}
```

---

## 6. 🔬 重点与难点代码实现深度剖析 (Deep-Dive)

### 6.1 AOP 声明式限流切面 (`RateLimitAspect` + `RateLimitService`)

#### 设计挑战
各接口对防刷的要求不尽相同（例如：登录接口防撞库需按 IP 限流，找回密码需同时按 IP 与邮箱限流，上传头像需按登录 UserID 限流）。若在每个 Controller 中编写重复的计数代码，会导致业务逻辑污染和难以维护。

#### 核心实现剖析
系统通过自定义注解 `@RateLimit`、Spring AOP 环绕通知与 Redis 计数器实现非侵入式限流：

```java
// 1. 在 Controller 方法上声明多维度限流策略
@PostMapping("/forgot-password")
@RateLimit(types = {RateLimitType.IP, RateLimitType.EMAIL}, maxRequests = 10, timeWindowSeconds = 60)
public ApiResponse<Void> forgotPassword(@RequestBody @Valid ForgotPasswordRequest request) { ... }

// 2. 切面中动态提取标识符 (RateLimitAspect)
private String extractIdentifier(RateLimit.RateLimitType type, HttpServletRequest request, ProceedingJoinPoint joinPoint, RateLimit rateLimit) {
    switch (type) {
        case IP: return getClientIp(request); // 提取 X-Forwarded-For 与代理穿透 IP
        case USER: return SecurityContextHolder.getContext().getAuthentication().getName(); // 提取登录用户
        case EMAIL: return extractEmailFromArgs(joinPoint.getArgs()); // 反射从 DTO 提取 email 属性
        case TOKEN: return request.getHeader("Authorization"); // 提取令牌特征
    }
}

// 3. Redis 计数与窗口校验 (RateLimitService)
long currentWindow = System.currentTimeMillis() / (timeWindowSeconds * 1000);
String key = "rate_limit:" + identifier + ":" + currentWindow;
Long count = redisTemplate.opsForValue().increment(key);
if (count != null && count == 1) {
    redisTemplate.expire(key, timeWindowSeconds, TimeUnit.SECONDS);
}
return count != null && count <= maxRequests;
```

#### 关键技术点
- **Fail-Open 弹性容错**：当 Redis 发生瞬态网络抖动或不可用时，限流层在捕获异常后记录告警日志并默认返回 `true`（放行），避免限流组件崩溃拖垮整站核心业务。
- **SpEL 与参数反射支持**：对于 `EMAIL` 等业务限流维度，切面通过检查方法参数对象反射获取 `getEmail()`，实现动态业务键绑定。

---

### 6.2 严格二进制魔数检测与 Base64 内存防腐 (`UserService`)

#### 设计挑战
移动端上传头像往往存在两个安全与性能痛点：
1. 客户端可能上传伪造了 `.png` 后缀的恶意可执行脚本或带有 `<script>` 的 SVG 恶意文件。
2. 超大畸形 Base64 数据如果直接交给图片解码器，容易引发服务器内存溢出（OOM）。

#### 核心实现剖析
`UserService.isValidAvatarData` 建立了严格的 5 级流水线防御：

```java
public boolean isValidAvatarData(String avatarData) {
    // 1. 规范 URL 快速放行 (限长 2048 字符且禁止换行注入)
    if (avatarData.startsWith("http://") || avatarData.startsWith("https://")) {
        return avatarData.length() <= 2048 && !avatarData.contains("
") && !avatarData.contains("");
    }
    // 2. 字符串长度预检: Base64 文本不得超过 3MB
    if (avatarData.length() > 3 * 1024 * 1024) return false;
    
    // 3. MIME 前缀强校验: 必须以 data:image/ 开头且包含 ;base64,
    int commaIndex = avatarData.indexOf(",");
    if (commaIndex == -1) return false;
    String metadata = avatarData.substring(0, commaIndex).toLowerCase();
    if (!metadata.startsWith("data:image/") || !metadata.contains(";base64")) return false;
    
    // 4. Base64 安全解码与二进制体积检验 (<= 2MB)
    byte[] decodedBytes;
    try {
        decodedBytes = Base64.getDecoder().decode(avatarData.substring(commaIndex + 1).trim());
    } catch (IllegalArgumentException e) {
        return false; // 非法 Base64 字符
    }
    if (decodedBytes.length == 0 || decodedBytes.length > 2 * 1024 * 1024) return false;
    
    // 5. 文件头底层魔数 (Magic Bytes) 严格匹配
    return isValidImageBytes(decodedBytes, metadata);
}
```

#### 支持的魔数白名单表

| 文件格式 | 魔数签名（十六进制） | 检验算法逻辑 |
|---------|-------------------|-------------|
| **PNG** | `89 50 4E 47 0D 0A 1A 0A` | 严格比对前 8 个字节 |
| **JPEG**| `FF D8 FF` | 比对前 3 个字节 |
| **GIF** | `47 49 46 38` (`GIF87a` / `GIF89a`) | 比对前 4 字节 ASCII 为 `GIF8` 且后续为 `7a` 或 `9a` |
| **WebP**| `52 49 46 46 ... 57 45 42 50` | 前 4 字节为 `RIFF`，第 8~11 字节为 `WEBP` |
| **SVG** | `<svg` 或 `<?xml` | 解码为 UTF-8 文本去除 BOM 后检查标签首部 |

---

### 6.3 全链路分布式请求追踪 (`RequestLoggingFilter` + MDC)

#### 设计挑战
生产环境下出现接口报错（如 502 / 500 / 400）时，微服务容器接收来自全球的高并发并发请求。如果日志中没有唯一关联标识，排查单个用户的请求调用链极其困难。

#### 核心实现剖析
通过实现 `OncePerRequestFilter` 并设置最高优先级 `@Order(1)`：

```java
@Component
@Order(1)
public class RequestLoggingFilter extends OncePerRequestFilter {
    private static final String REQUEST_ID_HEADER = "X-Request-Id";
    private static final String MDC_REQUEST_ID_KEY = "requestId";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) 
            throws ServletException, IOException {
        // 1. 复用客户端传入的 requestId，若无则由后端自动生成 UUID
        String requestId = request.getHeader(REQUEST_ID_HEADER);
        if (requestId == null || requestId.isBlank()) {
            requestId = UUID.randomUUID().toString();
        }

        // 2. 注入 SLF4J MDC 上下文，使该请求在所有线程内打印的日志均自动携带该 requestId
        MDC.put(MDC_REQUEST_ID_KEY, requestId);
        // 3. 将 requestId 反写回 HTTP 响应头，方便前端/移动端崩溃上报关联
        response.setHeader(REQUEST_ID_HEADER, requestId);

        long startMs = System.currentTimeMillis();
        try {
            filterChain.doFilter(request, response);
        } finally {
            long durationMs = System.currentTimeMillis() - startMs;
            // 4. 统计并输出访问日志
            logger.info("requestId={} method={} path={} status={} durationMs={}",
                    requestId, request.getMethod(), request.getRequestURI(), response.getStatus(), durationMs);
            // 5. 清理线程局部变量，杜绝容器线程池复用导致的内存泄漏或脏上下文
            MDC.remove(MDC_REQUEST_ID_KEY);
        }
    }
}
```

---

### 6.4 双 Token 轮转与主动吊销 (`JwtRequestFilter` + `RefreshTokenService`)

#### 设计挑战
纯无状态 JWT 的经典软肋是**无法在服务端主动失效**。如果用户的 Token 被劫持，即使修改密码，攻击者在 Token 过期前依然可以访问接口。

#### 核心实现剖析
结合 Redis 实现了高效的主动吊销与续期闭环：
1. **Access Token 黑名单**：
   - 用户登出或改密时，计算该 Token 剩余有效时长：`long remainingTtl = expiration - now`；
   - 调用 `TokenBlacklistService.blacklistToken(token, remainingTtl)` 将其加入 Redis（格式 `token:blacklist:<token>`）；
   - `JwtRequestFilter` 在校验签名通过后，先查 Redis 黑名单，若命中直接返回 401 并中断请求。
2. **Refresh Token 持久化与全端下线**：
   - 登录时以 `token:refresh:<username>:<token>` 格式存入 Redis，有效期 7 天；
   - 刷新时调用 `isRefreshTokenValid`，确认存在才颁发新令牌；
   - 用户改密或销户时，调用 `revokeAllRefreshTokens(username)` 清空该用户全部会话，任何设备上的旧会话瞬间失效。

---

## 7. ⚠️ 现有实现不足与演进路线 (Limitations & Roadmap)

通过对当前后端架构的生产级审计，识别出以下 5 项技术瓶颈，已同步收录至 [todo.md](file:///c:/Users/liste/Downloads/github/ListenPortfolioBackend/docs/todo.md) 中：

### 7.1 Redis 限流非原子性与窗口边界突刺 (Rate Limit Lua Script)
- **现状分析**：当前 `RateLimitService` 使用 `opsForValue().increment(key)`，判断 `count == 1` 再执行 `expire(key, timeWindow)`。
- **潜在风险**：
  1. **非原子操作**：若在高并发场景下，程序在 `increment` 与 `expire` 之间遭遇网络断开或 Pod 重启，该 key 将成为**永久无过期时间**的孤儿 key，导致该用户被永久误限流。
  2. **固定窗口突刺**：在窗口重置交界处（如第 59 秒进入 10 次请求，第 61 秒又进入 10 次请求），2 秒内实际上穿透了 20 次请求。
- **改进方案**：
  - 改用 Redis Lua 脚本原子执行 `INCR + EXPIRE`；
  - 或者迁移至基于 Redis ZSet 的**滑动时间戳窗口算法**（Sliding Window Log）或**令牌桶算法**（Token Bucket）。

### 7.2 Refresh Token 批量吊销采用 `KEYS` 命令的阻塞风险
- **现状分析**：`RefreshTokenService.revokeAllRefreshTokens` 内部调用了 `redisTemplate.keys("token:refresh:" + username + ":*")`。
- **潜在风险**：Redis 为单线程事件循环架构。`KEYS` 命令会对全库进行 $O(N)$ 暴力扫描。当生产环境 key 数量达到数十万级时，执行 `KEYS` 会导致 Redis 线程毫秒至秒级挂起，造成全站接口请求超时雪崩。
- **改进方案**：
  - 维护基于 Redis Set 的用户活跃 Token 索引：`user:tokens:<username>`，将多台设备的 token 放入 Set 中，注销时直接获取 Set 成员做定向删除，时间复杂度降为 $O(1)$；
  - 或降级使用非阻塞的游标扫描命令 `SCAN`。

### 7.3 ORM 关联子集合查询存在潜在 N+1 隐患
- **现状分析**：
  - `ProjectService.getProjects()` 先通过 `selectList(null)` 查出 N 个项目，随后在 Stream 循环中逐个调用 `projectMapper.findTechStackByProjectId(id)`。
  - `AboutMeService` 针对 `stat_tags`、`skill_items` 同样存在类似循环单查。
- **潜在风险**：当项目数或履历条目增多时，产生 $1 + N$ 次数据库网络往返（Round-trip），增加了连接池开销。
- **改进方案**：
  - 改为单次批量拉取：`SELECT * FROM project_tech_stack WHERE project_id IN (...)`；
  - 在内存中利用 Java 8 Stream `Collectors.groupingBy(ProjectTechStack::getProjectId)` 进行高效映射装配，将数据库往返压缩为固定 $O(1)$ 次。

### 7.4 Base64 头像存储数据库膨胀与 SVG XSS 隐患
- **现状分析**：头像采用 Base64 字符串直接存储在 MySQL `users.avatar_url` (LONGTEXT) 字段中；SVG 格式仅做了简单的文本头部匹配。
- **潜在风险**：
  1. **数据库膨胀**：单张 2MB 二进制图片转换为 Base64 占用近 2.7MB 存储，高并发读写 `users` 表极易导致 InnoDB Buffer Pool 缓存污染和行溢出页分裂。
  2. **SVG Stored XSS**：SVG 属于 XML 规范，支持嵌入 `<script>` 标签或带有恶意 JavaScript 的 `onload` 属性。若前端在浏览器直接内联渲染该 SVG，会导致跨站脚本攻击。
- **改进方案**：
  - 接入对象存储服务（如 AWS S3 / Cloudflare R2 / MinIO），服务端仅接收流并直传云端，数据库仅保留轻量 CDN URL；
  - 针对必须保留的 SVG 文件，引入安全消毒库（如 `DOMPurify` 或基于 Java SAX 的白名单过滤器）剥离危险脚本。

### 7.5 OpenAPI / Swagger 接口交互与多语言参数完善
- **现状分析**：当前部分接口在 Swagger 页面上未将 `Accept-Language` 暴露为显式测试参数，且 4xx/5xx 错误响应缺少统一 Schema 引用。
- **改进方案**：配置全局 `OpenAPI` 契约，自动将 `Accept-Language` 头挂载至所有展示端点，提供交互式多语言测试切换体验。

---

## 8. 🔄 Backend DTO vs Flutter Model 契约适配表

| DTO 字段名 | 后端数据类型 | Flutter Model 类型 | 转换与适配机制 | 说明 |
|-----------|-------------|-------------------|---------------|------|
| `LoginResponse.userId` | `Long` (数字) | `String` / `int` | `@ToStringConverter()` | 数字类型安全转换为字符串 |
| `ProjectDto.id` | `Long` (数字) | `String` / `int` | `@ToStringConverter()` | 数据库主键自增 ID |
| `ProjectDto.businessId` | `String` | `String` | 直接映射 | 业务唯一标识符（如 `listen-expense-tracker`） |
| `StatDto.id` | `Long` (数字) | `String` | `@ToStringConverter()` | 统计项自增 ID |
| `StatDto.businessId` | `String` | `String` | 直接映射 | 统计业务键（如 `android`, `flutter`） |
| `ExperienceDto.id` | `Long` (数字) | `String` | `@ToStringConverter()` | 工作经历自增 ID |
| `EducationDto.id` | `Long` (数字) | `String` | `@ToStringConverter()` | 教育经历自增 ID |
| `LanguageDto.id` | `Long` (数字) | `String` | `@ToStringConverter()` | 语言认证自增 ID |
| `SkillDto.id` | `Long` (数字) | `String` | `@ToStringConverter()` | 技能大类自增 ID |
| `SkillDto.score` | `Integer` (数字) | `int` | 直接映射 | 技能雷达得分（如 97, 93, 96, 94, 84, 89） |
| 全接口响应头 | `X-Request-Id` | `String` | 响应拦截器提取 | 分布式全链路追踪排错标识 |

---

## 9. 📊 数据库种子表结构映射 (Database Schema Mapping)

| 表名 (Table Name) | 实体类 (Entity) | 当前种子行数 | 说明与数据构成 |
|-------------------|----------------|-------------|---------------|
| `users` | `UserEntity` | 1 | 种子管理员核心档案（姓名、邮箱、头像、职业、自我评价、多语言） |
| `user_certifications` | 关联集合 | 2 | 外语资质认证标识：`jlptN1` (日语一级 131分), `bjtJ2` (商务日语 512分) |
| `projects` | `ProjectEntity` | 6 | 全量作品集项目（Flutter 客户端、Core 核心库、UI 组件库、Spring Boot 后台、技术知识库、Expense Tracker 原生 Android 记账应用） |
| `project_tech_stack` | 关联集合 | 21 | 各项目技术栈标签集合（包含 Jetpack Compose, Kotlin, MVI, Room 等） |
| `experiences` | `ExperienceEntity` | 6 | 6 段真实工作履历（乐天证券/LYC、有赞、多鹿、骑呗单车、百度外卖、新大陆） |
| `education` | `EducationEntity` | 2 | 本科（福建工程学院 软件工程）与专科（福建师范大学 计算机应用）教育背景 |
| `languages` | `LanguageEntity` | 3 | 语言能力评估（日语 JLPT N1/BJT J2、中文母语、英语 CET-4） |
| `stats` | `StatEntity` | 3 | 3 大技术年限统计（Android 11年、Flutter 3年、Java Web 1年） |
| `stat_tags` | 关联集合 | 8 | 统计维度关联标签（如 `archDesign`, `perfOptimization`, `cleanArchitecture` 等） |
| `skills` | `SkillEntity` | 6 | 6 大核心技能维度（Android Native: 97, Flutter: 93, Performance & APM: 96, Architecture: 94, Java & Backend: 84, DevOps & CI/CD: 89） |
| `skill_items` | `SkillItemEntity` | 25 | 6 大技能维度下的专业细分技能项（均支持中/英/日多语言动态分发） |

---

📅 **文档最后修订时间**: 2026-09-17  
👨‍💻 **维护团队**: Listen Portfolio Engineering Team
