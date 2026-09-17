# 🧪 测试覆盖率与质量工程架构完整指南 (Test Coverage & Quality Engineering Guide)

## 1. 测试战略与质量保障全景 (Testing Strategy & Pyramid)

在企业级微服务体系中，高可信度的自动化测试是系统快速迭代、架构重构（如本项目从 JPA 迁移至 MyBatis-Plus 3.5.7）与生产安全上线的基石。
本项目基于 **JUnit 5 (Jupiter)**、**Mockito**、**Spring Boot Test**、**H2 内存数据库**、**Embedded Redis** 以及 **JaCoCo 0.8.11** 构建了分层清晰、开箱即用、零外部环境依赖的质量保障体系。

```
                               【质量工程自动化测试金字塔】

                                  /                                 /                                  / 4. \  安全与边界测试 (Security & Boundary Tests)
                               / 渗透 \  - JWT 篡改与伪造拦截、黑名单实时阻断
                              / 验证   \ - @RateLimit 多维度限流与并发熔断
                             /──────────\ - 7 层头像 Payload 防爆与魔数签名检测
                            /                                       /   3. 端到端  \  集成测试 (Integration Tests - BaseIntegrationTest)
                          /   集成测试     \  - SpringBootTest + H2 内存库 (schema-h2.sql)
                         /                  \ - 动态端口嵌入式 Redis (Embedded Redis)
                        /                    \ - MySQL BINARY 方言兼容函数别名桥接
                       /──────────────────────                      /                                             /       2. 业务切面与服务   \  服务层单测 (Service & Aspect Unit Tests)
                    /       单元测试 (Service)    \  - Mockito 隔离测试 AuthService, UserService
                   /                              \ - MyBatis-Plus LambdaQueryWrapper 逻辑断言
                  /                                \ - RateLimitAspect 多维度反射参数提取测试
                 /──────────────────────────────────                /                                                   /           1. 基础模型与工具测试       \  底层模型单测 (Entity, Util, ErrorCode)
              /           (Entity / Util / ErrorCode) \  - 7 大实体全字段 Getter/Setter/Alias 测试
             /                                         \ - I18nUtils 多语言自适应回退全分支覆盖
            /───────────────────────────────────────────```

### 核心质量指标达成概览 (Latest Quality Baseline)
- **测试套件总数**：**33 个测试套件 (Test Suites)**
- **用例通过率**：**369 / 369 单元与集成测试用例 100% 全部通过 (BUILD SUCCESSFUL)**
- **测试执行耗时**：全套用例本地执行耗时约 **31.7 秒**
- **失败与跳过**：**0 Failures, 0 Errors, 0 Skipped**

---

## 2. JaCoCo 全维度覆盖率全景大盘 (Coverage Metrics Matrix)

根据 JaCoCo 字节码动态插桩分析生成的最新权威数据（解析自 `build/reports/jacoco/test/jacocoTestReport.xml`），后端系统的覆盖率指标如下：

### 2.1 全局六大度量指标汇总

| 指标类型 (Metric) | 已覆盖 (Covered) | 总数 (Total) | 覆盖率 (Coverage) | 未覆盖 (Missed) | 质量评级 |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **指令覆盖率 (INSTRUCTION)** | **5,347** | 5,793 | **92.30%** | 446 | 🟢 极佳 (> 90%) |
| **行覆盖率 (LINE)** | **1,367** | 1,485 | **92.05%** | 118 | 🟢 极佳 (> 90%) |
| **方法覆盖率 (METHOD)** | **386** | 399 | **96.74%** | 13 | 🟢 卓越 (> 95%) |
| **类覆盖率 (CLASS)** | **39** | 41 | **95.12%** | 2 | 🟢 卓越 (> 95%) |
| **圈复杂度覆盖率 (COMPLEXITY)** | **491** | 610 | **80.49%** | 119 | 🟢 优秀 (> 80%) |
| **分支覆盖率 (BRANCH)** | **289** | 418 | **69.14%** | 129 | 🟢 良好 (> 65%) |

---

### 2.2 各业务模块与包维度覆盖率排行榜

| 排序 | 包名 / 模块路径 (Package) | 行覆盖率 (Lines) | 指令覆盖率 (Instructions) | 覆盖状态 | 核心职责说明 |
| :---: | :--- | :--- | :--- | :---: | :--- |
| 1 | **`common.error`** | **100.0%** (13/13) | **100.0%** (62/62) | 🟢 | `ErrorCode` 全局标准错误码枚举与映射 |
| 2 | **`common.exception`** | **100.0%** (7/7) | **100.0%** (18/18) | 🟢 | `BusinessException` 统一业务受检异常 |
| 3 | **`common.util`** | **100.0%** (8/8) | **100.0%** (36/36) | 🟢 | `I18nUtils` 多语言解析与全分支回退 |
| 4 | **`common` (统一模型)** | **100.0%** (23/23) | **100.0%** (75/75) | 🟢 | `ApiResponse`, `Constants` 统一数据契约 |
| 5 | **`api.v1.auth.dto`** | **100.0%** (7/7) | **100.0%** (17/17) | 🟢 | 登录、注册、找回密码与重置请求 DTO |
| 6 | **`api.v1.projects`** | **100.0%** (9/9) | **100.0%** (27/27) | 🟢 | 项目作品公开查询端点与 i18n 过滤 |
| 7 | **`api.v1.about`** | **100.0%** (12/12) | **100.0%** (38/38) | 🟢 | 个人资料、经历与多维技能信息装配 |
| 8 | **`api.v1.user`** | **99.1%** (113/114) | **97.9%** (423/432) | 🟢 | 用户资料、软删除、改密、注销与头像上传 |
| 9 | **`common.jwt`** | **96.8%** (120/124) | **97.4%** (446/458) | 🟢 | JWT 签发、HMAC 校验、Filter 拦截与黑名单探针 |
| 10 | **`entity` (持久化模型)** | **96.0%** (285/297) | **97.2%** (680/700) | 🟢 | 7 大核心实体类全字段 Getter/Setter/Alias |
| 11 | **`api.v1.auth`** | **92.8%** (77/83) | **94.4%** (318/337) | 🟢 | 登录认证、注册查重、Token 刷新与密码重置 |
| 12 | **`service` (核心业务)** | **92.7%** (458/494) | **93.5%** (1741/1864)| 🟢 | MyBatis-Plus Service 业务流转与多语言逻辑 |
| 13 | **`common.aspect`** | **84.6%** (66/78) | **91.5%** (303/331) | 🟢 | RateLimitAspect 限流切面 (IP/Email/User/Token) |
| 14 | **`common.config`** | **79.6%** (168/211) | **85.9%** (844/982) | 🟢 | SecurityConfig, RequestLoggingFilter, 异常处理 |
| 15 | **`com.listen.portfolio`** | **20.0%** (1/5) | **15.4%** (4/26) | ⚪ | 根包启动类 (`main` 与 `ServletInitializer`) |

> 📌 **统计说明**：根包 `com.listen.portfolio` 的行覆盖率为 20.0%，原因是容器启动入口 `PortfolioApplication.main()` 与 `ServletInitializer.configure()` 属于系统引导代码，在测试生命周期中由测试框架托管启动，无需也不建议编写空跑用例刷覆盖率。在 `docs/todo.md` Section 24 中已规划了精细化排除规则将其过滤。

---

## 3. 测试架构设计与关键难点深度剖析

### 3.1 难点 1：跨平台嵌入式 Redis 自动化生命周期与端口防冲突 (`BaseIntegrationTest.java`)

在 `src/test/java/com/listen/portfolio/integration/BaseIntegrationTest.java` 中：

#### 技术挑战：
1. **端口并发冲突**：本地开发者机器或 CI 宿主机往往已运行 Docker 容器并占用了 `6379` 端口，若单测硬编码连接 `6379`，会导致环境脏数据污染或端口占用启动失败；
2. **Windows 平台兼容性深坑**：嵌入式 Redis 在 Windows 上移植于 MSOpenTech，若不显式指定堆内存大小，会直接抛出 `OOM command not allowed when used memory > 'maxmemory'` 错误；
3. **资源泄漏隐患**：若测试异常退出，嵌入式 Redis 作为独立 OS 进程可能残留后台，导致后续构建一直报错。

#### 解决方案与实现代码剖析：
```java
static {
    try {
        // 1. 动态申请操作系统的临时空闲端口 (Ephemeral Port)
        int port = freePort();
        var builder = RedisServer.builder().port(port);

        // 2. 针对 Windows 平台动态注入内存约束
        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            builder.setting("maxheap 128M");
        }

        // 3. 启动嵌入式 Redis 并记录端口
        RedisServer server = builder.build();
        server.start();
        embeddedRedisServer = server;
        redisPort = port;

        // 4. 注册 JVM 关闭钩子，确保进程退出时彻底回收资源
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                if (embeddedRedisServer != null && embeddedRedisServer.isActive()) {
                    embeddedRedisServer.stop();
                }
            } catch (Exception ignored) {}
        }));
    } catch (Throwable t) {
        // 5. 容灾回退策略
        redisPort = 6379;
    }
}

// 6. 利用 Spring Boot @DynamicPropertySource 动态改写 Redis 连接端口
@DynamicPropertySource
static void redisProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.data.redis.host", () -> "127.0.0.1");
    registry.add("spring.data.redis.port", () -> String.valueOf(redisPort));
    registry.add("spring.data.redis.timeout", () -> "2000ms");
    registry.add("spring.data.redis.database", () -> "1");
}
```

---

### 3.2 难点 2：H2 内存库与 MySQL 专有函数的方言桥接 (`H2Functions.java`)

#### 技术挑战：
在用户登录与鉴权逻辑中，为了严格区分大小写敏感用户名（例如保证 `Listen` 与 `listen` 为两个独立账号），`UserMapper.findByNameCaseSensitive` 编写了 MySQL 专有的原生 SQL 语句：
```sql
SELECT * FROM users WHERE BINARY name = #{username} AND deleted = 0
```
然而，**H2 内存数据库并不支持 MySQL 的 `BINARY` 关键字语法**！如果在 H2 内存库上直接运行该 SQL，会直接报语法错误 `Function "BINARY" not found`。

#### 解决方案：
在 `src/test/java/com/listen/portfolio/integration/H2Functions.java` 与 `BaseIntegrationTest` 中实现了方言桥接机制：
1. 编写 Java 静态直通函数：
```java
package com.listen.portfolio.integration;

public class H2Functions {
    public static String binary(String value) {
        return value; // 原样返回字符串，配合 H2 默认严格大小写比对
    }
}
```
2. 在 `BaseIntegrationTest.@BeforeEach` 中向 H2 动态注册别名：
```java
@BeforeEach
public void initH2Functions() {
    try (Connection conn = dataSource.getConnection();
         Statement stmt = conn.createStatement()) {
        stmt.execute("CREATE ALIAS IF NOT EXISTS "BINARY" DETERMINISTIC FOR "com.listen.portfolio.integration.H2Functions.binary"");
    } catch (Exception e) {
        e.printStackTrace();
    }
}
```
通过该桥接，生产代码无需对 H2 妥协或书写任何条件分支，一套 SQL 既能在生产 MySQL 上利用二进制索引加速，又能在 H2 内存库中绿色跑通单测。

---

### 3.3 难点 3：AOP 切面与 HTTP 上下文脱敏 Mock (`RateLimitAspectUnitTest.java`)

切面拦截器 `RateLimitAspect` 强依赖 `RequestContextHolder`、`HttpServletRequest` 以及 `SecurityContextHolder`。
在 `RateLimitAspectUnitTest.java` 中，通过纯内存 Mock 技术实现了全场景覆盖：
- 模拟反向代理多级穿透：注入 `X-Forwarded-For: 192.168.1.100` 或 `X-Real-IP: 10.0.0.1`，验证源 IP 正确提取；
- 模拟 DTO 反射：构造携带 `email` 与 `token` 属性的实体入参，验证无需依赖真实 HTTP 容器即可反射提取目标字段；
- 模拟安全上下文：注入 `UsernamePasswordAuthenticationToken` 验证已认证用户名提取。

---

### 3.4 难点 4：实体模型多语言别名无死角覆盖 (`EntityCoverageTest.java`)

在 `EntityCoverageTest.java` 中：
针对 `ProjectEntity`、`UserEntity`、`ExperienceEntity` 等核心模型，系统不仅验证了 Lombok 生成的数十个属性 Getter/Setter，还重点测试了多语言别名（Alias）机制：
```java
// 验证业务别名与数据库映射字段的互通性
project.setDesc("New Desc EN");
assertEquals("New Desc EN", project.getProjectDesc());
```
确保前端调用的属性名与持久层底层映射完全一致，消除重构风险。

---

## 4. 自动化测试套件分布与执行分析

全工程目前共有 **33 个测试套件，共计 369 个测试用例**：

| 测试套件分类 | 核心套件名称 | 用例数量 | 测试类型 | 覆盖重点与保障目标 |
| :--- | :--- | :---: | :---: | :--- |
| **控制器与用户域** | `UserControllerTest` | 36 | 单元测试 | 用户资料、改密权限校验、软删除保护 |
| **用户核心业务** | `UserServiceTest` | 34 | 单元测试 | 用户业务流转、更新头像魔数校验 |
| **关于与经历展示** | `AboutMeServiceTest` | 24 | 单元测试 | 多语言关于我、经历、教育、技能数据装配 |
| **身份认证服务** | `AuthServiceTest` | 23 | 单元测试 | 用户名/邮箱查重、密码加密比对、防枚举 |
| **安全重置凭证** | `PasswordResetTokenServiceTest` | 19 | 单元测试 | 256 位 CSPRNG Token 生成与 TTL 淘汰 |
| **邮件通知服务** | `EmailServiceTest` | 17 | 单元测试 | HTML 模板渲染、SMTP 投递与错误静默 |
| **作品项目服务** | `ProjectServiceTest` | 15 | 单元测试 | 项目详情、列表查询与多语言字段过滤 |
| **黑名单服务** | `TokenBlacklistServiceTest` | 14 | 单元测试 | Redis 黑名单写入、毫秒 TTL 与开路容灾 |
| **限流核心服务** | `RateLimitServiceTest` | 14 | 单元测试 | 固定时间窗口计数、原子 INCR 与超限拦截 |
| **认证控制器单测** | `AuthControllerTest` | 13 | 单元测试 | 登录鉴权、Token 签发、双令牌返回契约 |
| **令牌旋转单测** | `AuthControllerRefreshTest` | 8 | 单元测试 | Refresh Token 旋转 (RTR) 与旧凭证销毁 |
| **限流切面单测** | `RateLimitAspectUnitTest` | 8 | 单元测试 | IP/Email/Token/User 多维度标识提取 |
| **数据访问层** | `UserMapperTest` | 6 | 集成测试 | MyBatis-Plus 真实 SQL、BINARY 大小写敏感 |
| **安全集成测试** | `SecurityTest` | 5 | 集成测试 | 未授权拦截、CSRF 豁免、开放端点探测 |
| **实体全属性** | `EntityCoverageTest` | 6 | 单元测试 | 7 大实体全字段与别名测试 |
| **其他组件与集成**| 包含 `LogoutIntegrationTest` 等 18 个套件 | 127 | 单元/集成 | 错误码、异常处理器、日志过滤器等 |

---

## 5. 质量门禁工作流与日常操作指南

### 5.1 本地测试与报告生成

```bash
# 1. 运行全量单元测试与集成测试，并自动拉起 JaCoCo 报表任务
./gradlew test jacocoTestReport

# 2. 排除高耗时性能测试的快速验证命令 (日常开发推荐)
./gradlew test -Dtest=!PerformanceTest

# 3. Windows 平台一键自动化脚本 (自动在默认浏览器中调起 HTML 大盘)
.\check-coverage.bat
```

### 5.2 报告生成目录结构与阅读技巧

JaCoCo 报告输出在 `build/reports/jacoco/test/html/` 目录下：

```
build/reports/jacoco/test/html/
├── index.html              # 全局覆盖率总览首页 (打开此文件即可查看全站仪表盘)
├── com.listen.portfolio/   # 按包名组织的子页面
│   ├── index.html
│   ├── AuthService.html    # 单个类的详细代码覆盖行视图
│   └── ...
└── jacoco-resources/       # 样式表与图标静态资源
```

**代码级染色阅读规范**：
- 🟩 **绿色背景行**：该行字节码已被单测用例完整执行覆盖；
- 🟨 **黄色菱形 / 背景**：部分分支覆盖（如 `if (a || b)` 中仅触发了其中一个条件）；
- 🟥 **红色背景行**：未被任何测试用例执行到的盲区代码，需重点补充用例。

---

## 6. 当前不足与未来演进路线 (已收录至 todo.md Section 24)

结合当前项目覆盖率大盘与行业前沿质量保障标准，系统在 [`docs/todo.md`](file:///c:/Users/liste/Downloads/github/ListenPortfolioBackend/docs/todo.md) 第 24 节明确规划了 4 项演进任务：

| 规划条目 | 核心任务 | 现状痛点 | 演进与解决方案 |
| :--- | :--- | :--- | :--- |
| **24.1** | **Gradle 任务链自动化质量门禁与构建阻断** | 未接入 `jacocoTestCoverageVerification`，单测质量下滑时构建依然成功 | 引入 `jacocoTestCoverageVerification` 绑定 `./gradlew check`，设定 Line $\ge 80\%$、Branch $\ge 65\%$ 硬门禁阈值 |
| **24.2** | **测试统计排除规则精细化收敛** | 统计包含了 `main` 启动类与 OpenAPI 配置，导致根包显示 20% 行覆盖率偏差 | 在 JaCoCo 配置中精确配置白名单与排除模式，剔除启动类与生成类，提升统计纯净度 |
| **24.3** | **基于 Testcontainers 的真实容器化集成测试** | 依赖内存 H2 与嵌入式 Redis，在复杂 SQL 方言与网络抖动上与生产存在微观差异 | 引入 Testcontainers 在 CI 中拉起真实的 MySQL 8.0 与 Redis 7.2 镜像执行全量深度联调 |
| **24.4** | **变异测试 (Mutation Testing via PITest) 引入** | 传统行覆盖率无法度量断言有效性，存在只执行不校验的“假阳性”用例 | 引入 PITest 自动注入字节码突变，以“变异体击杀率”量化单测用例的真实缺陷检出能力 |

---

## 7. 关联参考

- [JaCoCo 官方文档与度量指标定义](https://www.jacoco.org/jacoco/trunk/doc/counters.html)
- [JUnit 5 用户参考指南](https://junit.org/junit5/docs/current/user-guide/)
- [Spring Boot Testing 参考手册](https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/#features.testing)
- [项目规划待办清单 (docs/todo.md)](file:///c:/Users/liste/Downloads/github/ListenPortfolioBackend/docs/todo.md)
