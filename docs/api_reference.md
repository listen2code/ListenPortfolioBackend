# API Reference（接口文档）

> 基于真实简历数据设计，所有数据字段与数据库种子数据保持一致。
>
> **Base URL**: `http://localhost:8080/v1`  
> **响应格式**: 所有接口统一使用 `ApiResponse<T>` 包装

## 📦 统一响应格式

```json
{
  "result": "0",          // "0" = 成功, 其他 = 错误码
  "messageId": "",        // 消息ID（错误时用于国际化 key）
  "message": "",          // 消息描述
  "body": { ... }         // 业务数据（泛型）
}
```

**错误码（ErrorCode）**：

| messageId | HTTP Status | 说明 |
|-----------|-------------|------|
| `BAD_REQUEST` | 400 | 请求参数错误 |
| `UNAUTHORIZED` | 401 | 未认证 / Token 无效 |
| `FORBIDDEN` | 403 | 无权限 |
| `INTERNAL_ERROR` | 500 | 服务器内部错误 |
| `RATE_LIMIT_EXCEEDED` | 429 | 请求过于频繁 |

---

## 🔐 认证接口 `/v1/auth`

### POST `/v1/auth/signUp` — 用户注册

**Rate Limit**: IP 10次/分钟

**Request Body**:
```json
{
  "userName": "Listen",
  "email": "listen2code@gmail.com",
  "password": "your-password"
}
```

**Response** (200):
```json
{
  "result": "0",
  "messageId": "",
  "message": ""
}
```

---

### POST `/v1/auth/login` — 用户登录

**Rate Limit**: IP 10次/分钟

**Request Body**:
```json
{
  "userName": "Listen",
  "password": "your-password"
}
```

**Response** (200):
```json
{
  "result": "0",
  "messageId": "",
  "message": "",
  "body": {
    "userId": 1,
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiJ9..."
  }
}
```

> **注意**: `userId` 为 `Long` 类型（数字），Flutter 端通过 `@ToStringConverter()` 自动转为 String。

---

### POST `/v1/auth/refresh` — 刷新 Token

**Request Body**:
```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9..."
}
```

**Response** (200): 同 login 响应格式。

---

### POST `/v1/auth/forgot-password` — 忘记密码

**Rate Limit**: Email 3次/5分钟

**Request Body**:
```json
{
  "email": "listen2code@gmail.com"
}
```

**Response** (200):
```json
{
  "result": "0",
  "messageId": "",
  "message": ""
}
```

---

### POST `/v1/auth/reset-password` — 重置密码

**Request Body**:
```json
{
  "token": "reset-token-from-email",
  "newPassword": "new-password"
}
```

---

## 👤 用户接口 `/v1/user`（需认证）

> 所有用户接口需在 Header 中携带 `Authorization: Bearer <token>`

### GET `/v1/user?id={userId}` — 获取用户信息

**Response** (200):
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
    "avatarUrl": "https://api.dicebear.com/7.x/avataaars/png?seed=Listen"
  }
}
```

> **注意**: `UserSummaryDto.id` 通过 `@JsonSerialize(using = ToStringSerializer.class)` 序列化为 String。

---

### POST `/v1/user/logout` — 退出登录

将当前 Token 加入 Redis 黑名单，清除 SecurityContext。

**Response** (200):
```json
{
  "result": "0",
  "messageId": "",
  "message": "Logout successful"
}
```

---

### POST `/v1/user/change-password` — 修改密码

修改成功后所有 Token 失效（当前 Token 加入黑名单 + 清除上下文）。

**Request Body**:
```json
{
  "currentPassword": "old-password",
  "newPassword": "new-password"
}
```

---

### DELETE `/v1/user/delete-account` — 注销账号

软删除（`deleted = true`），同时失效所有 Token。

---

## 📋 项目接口 `/v1/projects`（公开）

### GET `/v1/projects` — 获取项目列表

**Response** (200):
```json
{
  "result": "0",
  "messageId": "",
  "message": "success",
  "body": [
    {
      "id": 1,
      "businessId": "lportfolio-flutter",
      "title": "lPortfolio Flutter",
      "subtitle": "Current Project",
      "desc": "My personal portfolio app (this one!). Demonstrating Clean Architecture, MVI pattern, and advanced Riverpod state management in Flutter.",
      "imageUrl": "localhost/images/project1.jpg",
      "githubUrl": "https://github.com/listen2code/ListenPortfolioFlutter",
      "techStack": ["Flutter", "Riverpod", "Clean Architecture", "MVI"]
    },
    {
      "id": 2,
      "businessId": "listen-core-flutter",
      "title": "Listen Core Flutter",
      "subtitle": "Framework",
      "desc": "A foundational framework for Flutter projects providing base classes for MVI, standardized network wrappers, and lifecycle management.",
      "imageUrl": "localhost/images/project2.jpg",
      "githubUrl": "https://github.com/listen2code/ListenCoreFlutter",
      "techStack": ["Dart", "Riverpod", "Dio", "Architecture"]
    },
    {
      "id": 3,
      "businessId": "listen-ui-kit",
      "title": "Listen UI Kit",
      "subtitle": "Common Library",
      "desc": "A comprehensive UI component library for consistent branding and rapid development across multiple Flutter applications.",
      "imageUrl": "localhost/images/project3.jpg",
      "githubUrl": "https://github.com/listen2code/ListenUikitFlutter",
      "techStack": ["Flutter", "Design System", "CustomPainter"]
    },
    {
      "id": 4,
      "businessId": "portfolio-backend",
      "title": "Portfolio Backend",
      "subtitle": "Cloud Infrastructure",
      "desc": "The server-side implementation for this portfolio, managing user data, projects, and dynamic configurations.",
      "imageUrl": "localhost/images/project4.jpg",
      "githubUrl": "https://github.com/listen2code/ListenPortfolioBackend",
      "techStack": ["Spring Boot", "MySQL", "Redis", "Docker"]
    },
    {
      "id": 5,
      "businessId": "tech-knowledge-base",
      "title": "Tech Knowledge Base",
      "subtitle": "Articles & Docs",
      "desc": "A curated collection of my technical articles, architecture notes, and development experiences over the past 10 years.",
      "imageUrl": "localhost/images/project5.jpg",
      "githubUrl": "https://github.com/listen2code/article",
      "techStack": ["Markdown", "Documentation", "Knowledge Sharing"]
    }
  ]
}
```

> **⚠️ Flutter 前端适配**: `ProjectDto` 包含 `id`(Long) 和 `businessId`(String) 两个字段，Flutter mock 中仅有 `id`(String)。切换真实 API 时需适配 `businessId` 字段。

---

## 🧑‍💼 关于我 `/v1/aboutMe`（需认证）

### GET `/v1/aboutMe` — 获取个人简介

**Response** (200) — 基于真实简历数据：
```json
{
  "result": "0",
  "messageId": "",
  "message": "success",
  "body": {
    "status": "available",
    "jobTitle": "Senior Android / Flutter Engineer",
    "bio": "Senior Android Engineer with 11+ years of mobile development experience and 3+ years in Flutter. Expertise in client architecture (componentization, plugin systems), performance optimization, and APM infrastructure. Key achievements include reducing Feed timeout rates from 1.5% to 0.3%, building full-stack APM monitoring platforms, and leading Flutter app development for securities trading at Rakuten. JLPT N1, BJT J2 certified, currently based in Tokyo, Japan.",
    "graduationYear": "2013",
    "github": "https://github.com/listen2code",
    "major": "softwareEngineering",
    "certifications": ["jlptN1", "bjtJ2"],
    "stats": [
      {
        "id": 1,
        "businessId": "android",
        "year": "11",
        "label": "androidExp",
        "tags": ["archDesign", "perfOptimization", "componentization"]
      },
      {
        "id": 2,
        "businessId": "flutter",
        "year": "3",
        "label": "flutterExp",
        "tags": ["cleanArchitecture", "stateManagement", "riverpod"]
      },
      {
        "id": 3,
        "businessId": "java_web",
        "year": "1",
        "label": "javaWeb",
        "tags": ["springBoot", "restApi"]
      }
    ],
    "experiences": [
      {
        "id": 1,
        "title": "Android / Flutter Engineer",
        "company": "LYC Corp. (Rakuten Securities Project)",
        "period": "2023.02 - Present",
        "description": "Lead developer for new securities Flutter app: architecture design, framework development, FIDO2 authentication integration, and Flutter version upgrades. Maintaining Android stock trading app and conducting code reviews for team members."
      },
      {
        "id": 2,
        "title": "Android Engineer — Mobile Infrastructure",
        "company": "Youzan Technology",
        "period": "2021.10 - 2022.07",
        "description": "Built mobile APM stutter/ANR detection SDK with optimized data reporting and aggregation. Created full-stack monitoring dashboards (React/AntDesign frontend + Spring Boot backend with RESTful APIs). Participated in Commerce SDK Redux-pattern refactoring."
      },
      {
        "id": 3,
        "title": "Android Engineer",
        "company": "Duolu (Yin'ai Network Technology)",
        "period": "2019.11 - 2021.10",
        "description": "Established Feed monitoring system, reducing timeout rate from 1.5% to 0.3% and latency by 40%+. Led componentization (module + module_api + module_run) and plugin architecture (Shadow framework). Built dev-stage performance monitoring tools and automated testing (44 Feed cases via AirTest)."
      },
      {
        "id": 4,
        "title": "Android Engineer",
        "company": "Qibei Technology (Bike-sharing)",
        "period": "2016.09 - 2019.08",
        "description": "Developed bike-sharing apps (Qibei Bike, Dingda Transit) across 4+ major versions. Implemented hot-fix (Tinker), online performance monitoring (Matrix), and reduced build time by 30%+ via Gradle optimization. Set up Jenkins CI pipeline with wireless ADB deployment."
      },
      {
        "id": 5,
        "title": "Android Engineer",
        "company": "Baidu (Waimai Delivery)",
        "period": "2014.12 - 2016.06",
        "description": "Independently maintained delivery rider app (Xiaodu Knight v1.4-2.9). Designed dynamic GPS tracking strategy reducing redundant uploads by 10%+. Developed PassSDK for unified B-side authentication with AES/JNI encryption. Built logistics development framework for multi-app scaffolding."
      },
      {
        "id": 6,
        "title": "Java Developer",
        "company": "NewLand Software Engineering",
        "period": "2013.05 - 2014.09",
        "description": "Developed business management and analytics modules for China Mobile support system using J2EE, S2SH framework, and Oracle database."
      }
    ],
    "education": [
      {
        "id": 1,
        "degree": "Bachelor of Software Engineering",
        "school": "Fujian University of Technology",
        "period": "2011.09 - 2013.06",
        "description": "Outstanding Graduation Thesis: 'Design and Implementation of CRM System Based on Intelligent Evaluation System'"
      },
      {
        "id": 2,
        "degree": "Associate in Computer Applications",
        "school": "Fujian Normal University (IT College)",
        "period": "2008.09 - 2011.06",
        "description": "Provincial Outstanding Student, First-class Scholarship, Outstanding Student Cadre"
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
        "category": "Mobile",
        "items": ["Flutter", "Android Native", "Dart", "Kotlin", "Java"]
      },
      {
        "id": 2,
        "category": "Architecture",
        "items": ["Clean Architecture", "Componentization", "Plugin Architecture", "MVI", "MVVM", "SOLID"]
      },
      {
        "id": 3,
        "category": "Performance & APM",
        "items": ["Profiling", "Memory Optimization", "Feed Monitoring", "Systrace", "LeakCanary"]
      },
      {
        "id": 4,
        "category": "Backend & DevOps",
        "items": ["Spring Boot", "MySQL", "Redis", "Docker", "CI/CD"]
      }
    ]
  }
}
```

---

## 🔄 Backend DTO vs Flutter Mock 字段差异

切换到真实 API 时，Flutter 端需要适配以下差异：

| DTO 字段 | 后端实际类型 | Flutter Mock 现状 | 说明 |
|----------|-------------|-------------------|------|
| `LoginResponse.userId` | `Long` (数字) | String `"1001"` → 已修正为 `1` | `@ToStringConverter` 自动处理 |
| `ProjectDto.id` | `Long` (数字) | String `"1"` | 需要 converter 适配 |
| `ProjectDto.businessId` | `String` | 无此字段 | 需新增字段 |
| `StatDto.id` | `Long` (数字) | String `"android"` (实为 businessId) | ⚠️ Mock 中 id 映射了 businessId |
| `StatDto.businessId` | `String` | 无此字段 | 后端有，Mock 无 |
| `ExperienceDto.id` | `Long` | 无此字段 | 后端有，Mock 无 |
| `EducationDto.id` | `Long` | 无此字段 | 后端有，Mock 无 |
| `LanguageDto.id` | `Long` | 无此字段 | 后端有，Mock 无 |
| `SkillDto.id` | `Long` | 无此字段 | 后端有，Mock 无 |
| 所有 GET 响应 | 包含 `messageId` | 部分缺失 → 已补全 | — |

---

## 📊 数据库种子数据 vs 真实简历映射

| DB 表 | 记录数 | 数据来源 |
|-------|--------|----------|
| `users` | 1 | 简历基本信息 |
| `experiences` | 6 | 6 段真实工作经历（LYC/乐天、有赞、多鹿、骑呗、百度、新大陆） |
| `education` | 2 | 福建工程学院（本科）、福建师范大学（专科） |
| `skills` | 4 | Mobile / Architecture / Performance & APM / Backend & DevOps |
| `skill_items` | 21 | 各分类下具体技能 |
| `languages` | 3 | 日语 JLPT N1 + BJT J2、中文母语、英语 CET-4 |
| `user_certifications` | 2 | jlptN1、bjtJ2 |
| `stats` | 3 | Android 11年、Flutter 3年、Java Web 1年 |
| `stat_tags` | 8 | 各统计维度的关键标签 |
| `projects` | 5 | 个人 GitHub 项目（与工作经历独立） |

---

📅 **最后更新**: 2026-04-05
