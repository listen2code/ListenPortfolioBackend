# 测试覆盖率与质量保障报告 (Test Coverage Report)

**更新时间**: 2026-08-20  
**构建与分析工具**: Gradle 8.5 + JaCoCo 0.8.11 + SpotBugs  
**测试结果**: **347 / 347 单元与集成测试用例 100% 全部通过 (BUILD SUCCESSFUL)**

---

## 📊 后端整体测试覆盖率概览 (JaCoCo Metrics)

| 指标类型 (Metric) | 已覆盖 (Covered) | 总数 (Total) | 覆盖率 (Coverage) | 未覆盖 (Missed) | 状态 |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **指令覆盖率 (INSTRUCTION)** | **4,991** | 5,337 | **93.52%** | 346 | 🟢 极佳 (> 90%) |
| **行覆盖率 (LINE)** | **1,298** | 1,404 | **92.45%** | 106 | 🟢 极佳 (> 90%) |
| **方法覆盖率 (METHOD)** | **371** | 378 | **98.15%** | 7 | 🟢 卓越 (> 95%) |
| **类覆盖率 (CLASS)** | **38** | 40 | **95.00%** | 2 | 🟢 优秀 (> 90%) |
| **圈复杂度覆盖率 (COMPLEXITY)** | **462** | 535 | **86.36%** | 73 | 🟢 优秀 (> 85%) |
| **分支覆盖率 (BRANCH)** | **235** | 310 | **75.81%** | 75 | 🟢 良好 (> 75%) |

---

## 📦 各业务模块与包维度覆盖率详情

| 包名 / 模块 (Package Name) | 行覆盖率 (Lines) | 指令覆盖率 (Instructions) | 分支覆盖率 (Branches) | 核心说明 |
| :--- | :--- | :--- | :--- | :--- |
| **`entity` (持久化模型)** | **100.0%** (272/272) | **100.0%** (640/640) | N/A | 7 个实体类全字段 Getter/Setter/Alias |
| **`common.util` (工具类)** | **100.0%** (8/8) | **100.0%** (36/36) | **100.0%** (14/14) | `I18nUtils` 多语言回退全分支 |
| **`api.v1.projects`** | **100.0%** (9/9) | **100.0%** (27/27) | **100.0%** (2/2) | 项目列表查询与 i18n 响应 |
| **`api.v1.about`** | **100.0%** (12/12) | **100.0%** (38/38) | **100.0%** (2/2) | 个人简历、统计数据与经历装配 |
| **`common` (统一模型)** | **100.0%** (23/23) | **100.0%** (75/75) | N/A | `ApiResponse`, `Constants` 等通用结构 |
| **`common.error`** | **100.0%** (13/13) | **100.0%** (62/62) | N/A | `ErrorCode` 标准错误码体系 |
| **`common.exception`** | **100.0%** (7/7) | **100.0%** (18/18) | N/A | `BusinessException` 业务异常定义 |
| **`api.v1.auth.dto`** | **100.0%** (7/7) | **100.0%** (17/17) | N/A | 认证请求与响应 DTO |
| **`api.v1.user`** | **99.1%** (109/110) | **97.9%** (423/432) | **83.3%** (35/42) | 用户信息、软删除、修改密码、注销 |
| **`common.jwt`** | **96.8%** (120/124) | **97.4%** (446/458) | **90.0%** (18/20) | JWT 签发、校验、Filter 与黑名单拦截 |
| **`api.v1.auth`** | **92.8%** (77/83) | **94.4%** (318/337) | **83.3%** (10/12) | 注册、登录、Token 刷新、密码重置 |
| **`service` (业务层)** | **91.9%** (410/446) | **93.4%** (1741/1864) | **86.5%** (90/104) | MyBatis-Plus Service 核心业务与多语言逻辑 |
| **`common.aspect`** | **84.2%** (64/76) | **91.5%** (303/331) | **64.0%** (32/50) | 限流切面 RateLimitAspect (IP/Email/Token/User/Custom) |
| **`common.config`** | **79.4%** (166/209) | **85.9%** (844/982) | **50.0%** (32/64) | SecurityConfig, RequestLoggingFilter, GlobalException |

---

## 🧪 测试套件结构与执行命令

### 1. 本地生成 JaCoCo 报告
```bash
./gradlew test jacocoTestReport
```
报告生成路径：`build/reports/jacoco/test/html/index.html`

### 2. 核心测试用例分布 (347 Tests)
- **`com.listen.portfolio.entity.*`**：7 大核心实体（User, Project, Stat, Experience, Education, Language, Skill）测试
- **`com.listen.portfolio.aspect.*`**：RateLimitAspect 多维度限流单元与集成测试
- **`com.listen.portfolio.common.config.*`**：RequestLoggingFilter, GlobalExceptionHandler 单元测试
- **`com.listen.portfolio.common.util.*`**：I18nUtils 国际化解析与分支测试
- **`com.listen.portfolio.api.v1.auth.*`**：认证、登录与 Token 刷新单测
- **`com.listen.portfolio.api.v1.user.*`**：用户服务、软删除防护、`UserMapper` 测试
- **`com.listen.portfolio.api.v1.projects.*`**：项目查询与国际化过滤
- **`com.listen.portfolio.service.*`**：Service 层业务逻辑单测与 Mock 测试
- **`com.listen.portfolio.security.*`**：JWT 篡改、XSS 防护、并发令牌验证
- **`com.listen.portfolio.integration.*`**：基于 H2 内存库和嵌入式 Redis 的端到端集成测试
