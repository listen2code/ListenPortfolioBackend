# 🛡️ API 限流保护完整指南

## 📋 目录

- [概述](#概述)
- [技术架构](#技术架构)
- [已实现功能](#已实现功能)
- [使用指南](#使用指南)
- [配置说明](#配置说明)
- [测试覆盖](#测试覆盖)
- [监控与运维](#监控与运维)
- [重构历史](#重构历史)
- [最佳实践](#最佳实践)

---

## 概述

本项目实现了基于 **AOP 切面 + 注解** 的统一限流保护方案，使用 Redis 实现分布式限流，有效防止 API 被暴力破解和滥用。

### 核心特性

- ✅ **统一管理**：通过 `@RateLimit` 注解统一管理所有限流规则
- ✅ **代码简洁**：一个注解即可添加限流，无需手动编写限流代码
- ✅ **分布式支持**：基于 Redis 实现，支持多实例部署
- ✅ **多种限流类型**：支持 IP、EMAIL、TOKEN、USER、CUSTOM 五种限流类型
- ✅ **灵活配置**：可自定义限流次数和时间窗口
- ✅ **故障容错**：Redis 故障时允许请求通过，不影响服务可用性

### 为什么需要限流？

#### 未登录接口（认证接口）
- **防止暴力破解**：限制登录、注册等接口的访问频率
- **防止邮箱枚举**：限制忘记密码接口的邮箱查询
- **防止 DoS 攻击**：限制单个 IP 的请求频率

#### 已登录接口（用户接口）
- **防止账号被盗用**：被盗账号可能被用于恶意操作
- **保护系统资源**：防止单个用户占用过多资源
- **确保公平性**：所有用户公平使用系统资源
- **防止滥用**：即使是合法用户也可能恶意调用 API

---

## 技术架构

### 核心组件

```
┌─────────────────────────────────────────────────────────┐
│                    用户请求                              │
└─────────────────┬───────────────────────────────────────┘
                  │
                  ▼
┌─────────────────────────────────────────────────────────┐
│          Spring MVC DispatcherServlet                   │
└─────────────────┬───────────────────────────────────────┘
                  │
                  ▼
┌─────────────────────────────────────────────────────────┐
│         RateLimitAspect (AOP 拦截)                      │
│  - 检查 @RateLimit 注解                                  │
│  - 提取限流标识符 (IP/EMAIL/TOKEN/USER)                 │
│  - 调用 RateLimitService 检查限流                       │
└─────────────────┬───────────────────────────────────────┘
                  │
                  ▼
          ┌───────┴───────┐
          │  超限？        │
          └───────┬───────┘
                  │
        ┌─────────┴─────────┐
        │                   │
        ▼                   ▼
    是：返回 429        否：执行业务逻辑
```

### 1. `@RateLimit` 注解

**位置**: `src/main/java/com/listen/portfolio/common/RateLimit.java`

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {
    // 限流类型（可组合使用）
    RateLimitType[] types() default {RateLimitType.IP};
    
    // 时间窗口内最大请求数
    int maxRequests() default 10;
    
    // 时间窗口（秒）
    int timeWindowSeconds() default 60;
    
    // 限流类型枚举
    enum RateLimitType {
        IP,      // 基于 IP 地址
        EMAIL,   // 基于邮箱
        TOKEN,   // 基于 Token
        USER,    // 基于用户 ID（需要登录）
        CUSTOM   // 自定义
    }
}
```

### 2. `RateLimitAspect` 切面

**位置**: `src/main/java/com/listen/portfolio/aspect/RateLimitAspect.java`

**核心功能**：
- 自动拦截所有带 `@RateLimit` 注解的方法
- 从 HTTP 请求中提取限流标识符
- 调用 `RateLimitService` 执行限流检查
- 超限时返回 HTTP 429 响应

**标识符提取逻辑**：
- **IP**: 从 `X-Forwarded-For`、`X-Real-IP` 或 `RemoteAddr` 提取
- **EMAIL**: 从请求参数的 `email` 字段提取
- **TOKEN**: 从请求参数的 `token` 字段提取
- **USER**: 从 Spring Security 上下文提取当前用户名

### 3. `RateLimitService` 服务

**位置**: `src/main/java/com/listen/portfolio/service/RateLimitService.java`

**核心功能**：
- 基于 Redis 的分布式限流
- 滑动窗口算法
- 故障容错（Redis 故障时允许访问）

**主要方法**：
```java
// 通用限流检查
public boolean isAllowed(String identifier, int maxRequests, int timeWindowSeconds)

// IP 限流检查：1 分钟内最多 10 次
public boolean isIpAllowed(String ip)

// 邮箱限流检查：5 分钟内最多 3 次
public boolean isEmailAllowed(String email)

// 获取剩余请求次数
public int getRemainingRequests(String identifier, int maxRequests, int timeWindowSeconds)
```

### 限流算法

使用 **滑动窗口算法**，基于 Redis 的 `INCR` 命令实现：

1. 生成限流 key：`rate_limit:{type}:{identifier}:{timeWindow}`
2. 使用 `INCR` 命令增加计数
3. 如果是第一次访问，设置过期时间
4. 检查计数是否超过限制

**Redis Key 示例**：
```
rate_limit:ip:127.0.0.1:1234567890           # IP 限流
rate_limit:email:user@example.com:1234567890 # 邮箱限流
rate_limit:user:testuser:1234567890          # 用户限流
```

### 故障容错

当 Redis 连接失败或发生异常时：
- 记录错误日志
- **允许请求通过**（Fail Open 策略）
- 避免影响正常用户访问

---

## 已实现功能

### 认证接口（未登录）

| 接口 | 限流类型 | 限制 | 说明 |
|------|---------|------|------|
| `POST /v1/auth/login` | IP | 10次/分钟 | 防止暴力破解 |
| `POST /v1/auth/signUp` | IP | 10次/分钟 | 防止批量注册 |
| `POST /v1/auth/refresh` | IP | 20次/分钟 | 稍宽松，正常用户可能频繁刷新 |
| `POST /v1/auth/forgot-password` | IP + EMAIL | 10次/分钟 + 3次/5分钟 | 双重保护，防止邮箱枚举 |
| `POST /v1/auth/reset-password` | IP + TOKEN | 10次/分钟 + 3次/分钟 | 防止暴力破解 token |

### 用户接口（已登录）

| 接口 | 限流类型 | 限制 | 说明 |
|------|---------|------|------|
| `GET /v1/user` | USER | 100次/分钟 | 查询操作，宽松限制 |
| `POST /v1/user/logout` | USER | 20次/分钟 | 中等限制 |
| `POST /v1/user/change-password` | USER | 20次/分钟 | 敏感操作 |
| `DELETE /v1/user/delete-account` | USER | 5次/分钟 | 危险操作，严格限制 |

### 超限响应

所有接口超限时返回统一格式：

**HTTP 状态码**: `429 Too Many Requests`

**响应体**：
```json
{
  "result": "1",
  "messageId": "RATE_LIMIT_EXCEEDED",
  "message": "Requests are too frequent, please try again later"
}
```

---

## 使用指南

### 快速开始

#### 1. 为新接口添加限流

**示例 1：IP 限流（未登录接口）**
```java
@PostMapping("/api/public")
@RateLimit(
    types = {RateLimitType.IP},
    maxRequests = 10,
    timeWindowSeconds = 60
)
public ResponseEntity<?> publicApi() {
    // 业务逻辑
    return ResponseEntity.ok("Success");
}
```

**示例 2：用户限流（已登录接口）**
```java
@GetMapping("/api/user/data")
@RateLimit(
    types = {RateLimitType.USER},
    maxRequests = 100,
    timeWindowSeconds = 60
)
public ResponseEntity<?> getUserData() {
    // 业务逻辑
    return ResponseEntity.ok(data);
}
```

**示例 3：多重限流（IP + EMAIL）**
```java
@PostMapping("/api/send-verification")
@RateLimit(
    types = {RateLimitType.IP, RateLimitType.EMAIL},
    maxRequests = 5,
    timeWindowSeconds = 300
)
public ResponseEntity<?> sendVerification(@RequestBody VerificationRequest request) {
    // 业务逻辑
    return ResponseEntity.ok("Email sent");
}
```

**示例 4：自定义限流参数**
```java
@PostMapping("/api/sensitive")
@RateLimit(
    types = {RateLimitType.IP, RateLimitType.USER},
    maxRequests = 3,        // 3 次
    timeWindowSeconds = 600  // 10 分钟
)
public ResponseEntity<?> sensitiveOperation() {
    // 敏感操作
    return ResponseEntity.ok("Done");
}
```

#### 2. 调整限流参数

只需修改注解参数即可：

```java
// 从 10次/分钟 改为 20次/分钟
@RateLimit(types = {RateLimitType.IP}, maxRequests = 20, timeWindowSeconds = 60)

// 从 1分钟 改为 5分钟
@RateLimit(types = {RateLimitType.IP}, maxRequests = 10, timeWindowSeconds = 300)
```

### 重构前后对比

#### 重构前（手动限流）

```java
@PostMapping("/login")
public ResponseEntity<?> login(
        @RequestBody LoginRequest request,
        HttpServletRequest httpRequest) {
    
    // 手动获取 IP
    String clientIp = getClientIp(httpRequest);
    logger.info("Received login request, IP: {}", clientIp);
    
    // 手动限流检查
    if (!rateLimitService.isIpAllowed(clientIp)) {
        logger.warn("Rate limit exceeded for IP: {}", clientIp);
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(ApiResponse.error("RATE_LIMIT_EXCEEDED", 
                      "Requests are too frequent, please try again later"));
    }
    
    // 业务逻辑
    // ...
}

// 需要手动实现 getClientIp 方法
private String getClientIp(HttpServletRequest request) {
    String ip = request.getHeader("X-Forwarded-For");
    if (ip == null || ip.isEmpty()) {
        ip = request.getHeader("X-Real-IP");
    }
    if (ip == null || ip.isEmpty()) {
        ip = request.getRemoteAddr();
    }
    return ip;
}
```

**问题**：
- ❌ 每个接口都要重复编写限流代码（10-15 行）
- ❌ 需要手动注入 `HttpServletRequest` 参数
- ❌ 需要手动实现 IP 提取逻辑
- ❌ 代码重复，难以维护

#### 重构后（注解限流）

```java
@PostMapping("/login")
@RateLimit(types = {RateLimitType.IP}, maxRequests = 10, timeWindowSeconds = 60)
public ResponseEntity<?> login(@RequestBody LoginRequest request) {
    // 业务逻辑
    // ...
}
```

**优势**：
- ✅ 一个注解搞定，代码简洁
- ✅ 无需手动注入 `HttpServletRequest`
- ✅ 无需手动编写限流检查代码
- ✅ 统一管理，易于维护

**代码减少**：每个接口减少约 10-15 行代码

---

## 配置说明

### 限流规则推荐

#### 认证接口（未登录）

```java
// 登录 - 严格限制
@RateLimit(types = {RateLimitType.IP}, maxRequests = 10, timeWindowSeconds = 60)

// 注册 - 严格限制
@RateLimit(types = {RateLimitType.IP}, maxRequests = 10, timeWindowSeconds = 60)

// 刷新令牌 - 稍宽松
@RateLimit(types = {RateLimitType.IP}, maxRequests = 20, timeWindowSeconds = 60)

// 忘记密码 - 双重限流
@RateLimit(
    types = {RateLimitType.IP, RateLimitType.EMAIL},
    maxRequests = 10,
    timeWindowSeconds = 60
)

// 重置密码 - IP + Token 双重限流
@RateLimit(
    types = {RateLimitType.IP, RateLimitType.TOKEN},
    maxRequests = 10,
    timeWindowSeconds = 60
)
```

#### 用户接口（已登录）

```java
// 查询操作 - 宽松限制
@RateLimit(types = {RateLimitType.USER}, maxRequests = 100, timeWindowSeconds = 60)

// 普通操作 - 中等限制
@RateLimit(types = {RateLimitType.USER}, maxRequests = 50, timeWindowSeconds = 60)

// 敏感操作 - 严格限制
@RateLimit(types = {RateLimitType.USER}, maxRequests = 20, timeWindowSeconds = 60)

// 危险操作 - 非常严格
@RateLimit(types = {RateLimitType.USER}, maxRequests = 5, timeWindowSeconds = 60)
```

### 自定义 RateLimitService 配置

如需修改默认限流规则，可以编辑 `RateLimitService.java`：

```java
public boolean isIpAllowed(String ip) {
    // 修改为：1 分钟内最多 5 次
    return isAllowed("ip:" + ip, 5, 60);
}

public boolean isEmailAllowed(String email) {
    // 修改为：10 分钟内最多 5 次
    return isAllowed("email:" + email, 5, 600);
}
```

---

## 测试覆盖

### 单元测试

#### AuthControllerTest

**位置**: `src/test/java/com/listen/portfolio/api/v1/auth/AuthControllerTest.java`

**测试用例**：
- ✅ `testSignUp_Success` - 注册成功
- ✅ `testSignUp_UsernameAlreadyExists` - 用户名已存在
- ✅ `testLogin_Success` - 登录成功
- ✅ `testLogin_InvalidCredentials` - 凭据无效
- ✅ `testRefreshToken_Success` - 刷新令牌成功
- ✅ `testForgotPassword_Success` - 忘记密码成功
- ✅ `testResetPassword_Success` - 重置密码成功
- ✅ `testResetPassword_InvalidToken` - Token 无效
- ✅ `testResetPassword_EmptyRequest` - 空请求参数

**测试结果**: 12/12 通过 ✅

**注意**: 限流功能的测试现在在 `RateLimitAspectTest` 中进行，不再需要在每个 Controller 测试中重复测试限流逻辑。

#### RateLimitAspectTest

**位置**: `src/test/java/com/listen/portfolio/aspect/RateLimitAspectTest.java`

**测试用例**：
- ✅ IP 限流 - 允许通过
- ✅ IP 限流 - 超限拒绝
- ✅ 提取客户端 IP - 从 X-Forwarded-For
- ✅ 提取客户端 IP - 从 X-Real-IP
- ✅ 提取客户端 IP - 从 RemoteAddr
- ✅ 多种限流类型 - IP + EMAIL
- ✅ 用户级限流 - 需要认证
- ✅ 限流标识符掩码 - 保护隐私
- ✅ 无 HttpServletRequest - 跳过限流检查

### 集成测试示例

```bash
# 测试正常请求
curl -X POST http://localhost:8080/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"userName":"testuser","password":"password123"}'

# 响应: HTTP 200 OK
{
  "result": "0",
  "message": "Success",
  "body": {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
    "userId": 1
  }
}

# 测试限流（在 1 分钟内发送超过 10 次请求）
for i in {1..11}; do
  curl -X POST http://localhost:8080/v1/auth/login \
    -H "Content-Type: application/json" \
    -d '{"userName":"testuser","password":"password123"}'
done

# 第 11 次请求响应: HTTP 429 Too Many Requests
{
  "result": "1",
  "messageId": "RATE_LIMIT_EXCEEDED",
  "message": "Requests are too frequent, please try again later"
}
```

---

## 监控与运维

### 日志监控

限流触发时会记录警告日志：

```
WARN  c.l.p.aspect.RateLimitAspect - Rate limit exceeded for type: IP, identifier: 192.168.1.100
WARN  c.l.p.aspect.RateLimitAspect - Rate limit exceeded for type: EMAIL, identifier: user@example.com
WARN  c.l.p.aspect.RateLimitAspect - Rate limit exceeded for type: USER, identifier: testuser
```

### Prometheus 指标建议

可以添加以下自定义指标：

```java
// 限流触发次数
rate_limit_exceeded_total{endpoint="/v1/auth/login", type="IP"}

// 总请求次数
rate_limit_requests_total{endpoint="/v1/auth/login"}

// 允许通过的请求次数
rate_limit_allowed_requests_total{endpoint="/v1/auth/login"}
```

### Redis 监控

监控 Redis 中的限流 key：

```bash
# 查看所有限流 key
redis-cli KEYS "rate_limit:*"

# 查看特定 IP 的限流计数
redis-cli GET "rate_limit:ip:192.168.1.100:1234567890"

# 查看 key 的过期时间
redis-cli TTL "rate_limit:ip:192.168.1.100:1234567890"
```

---

## 重构历史

### 2026-03-31：统一限流方案重构

#### 重构前的问题
1. **代码重复**：每个接口都要手动编写限流检查代码
2. **维护困难**：修改限流规则需要改动多个地方
3. **易出错**：容易遗漏某些接口的限流保护
4. **测试冗余**：每个 Controller 测试都要重复测试限流逻辑

#### 重构方案
采用 **AOP 切面 + 注解** 的统一限流方案：
- 创建 `@RateLimit` 注解
- 创建 `RateLimitAspect` 切面
- 重构所有接口使用注解

#### 重构成果

**代码质量提升**：
- ✅ 减少约 140 行重复代码
- ✅ AuthController: 减少约 60 行
- ✅ AuthControllerTest: 减少约 80 行

**功能增强**：
- ✅ 重构 5 个认证接口
- ✅ 新增 4 个用户接口限流保护
- ✅ 支持 5 种限流类型
- ✅ 支持多种限流类型组合

**测试覆盖**：
- ✅ AuthControllerTest: 12/12 通过
- ✅ RateLimitAspectTest: 9/9 通过

**可维护性**：
- ✅ 限流逻辑集中管理
- ✅ 修改限流规则只需改注解参数
- ✅ 新增限流接口只需添加一个注解

---

## 最佳实践

### 1. 限流类型选择

| 场景 | 推荐限流类型 | 说明 |
|------|-------------|------|
| 未登录接口 | IP | 防止单个 IP 暴力攻击 |
| 邮箱相关 | IP + EMAIL | 双重保护，防止邮箱枚举 |
| Token 相关 | IP + TOKEN | 防止暴力破解 token |
| 已登录接口 | USER | 基于用户限流，更精准 |
| 敏感操作 | IP + USER | 双重保护 |

### 2. 限流参数设置

**时间窗口**：
- 短时间窗口（1 分钟）：适用于高频接口
- 中等时间窗口（5 分钟）：适用于邮箱等敏感操作
- 长时间窗口（10 分钟）：适用于非常敏感的操作

**最大请求数**：
- 严格限制（5-10 次）：登录、注册、敏感操作
- 中等限制（20-50 次）：普通操作
- 宽松限制（100+ 次）：查询操作

### 3. 安全建议

**已实现**：
- ✅ IP 级别限流
- ✅ 邮箱级别限流
- ✅ Token 级别限流
- ✅ 用户级别限流
- ✅ 分布式限流（基于 Redis）
- ✅ 故障容错
- ✅ 详细日志记录

**建议增强**：
- [ ] 实现动态限流规则（根据时段调整）
- [ ] 添加黑名单机制（自动封禁恶意 IP）
- [ ] 实现验证码机制（多次失败后）
- [ ] 集成 WAF（Web Application Firewall）
- [ ] 添加 Prometheus 监控指标
- [ ] 实现限流白名单（VIP 用户）

### 4. 故障处理

**Redis 故障**：
- 系统采用 Fail Open 策略
- Redis 故障时允许请求通过
- 记录错误日志便于排查

**建议**：
- 监控 Redis 健康状态
- 设置 Redis 高可用（主从、哨兵、集群）
- 定期检查限流日志

### 5. 性能优化

**Redis 优化**：
- 使用 Redis Pipeline 批量操作
- 设置合理的 key 过期时间
- 定期清理过期 key

**代码优化**：
- 限流检查尽早执行（在业务逻辑之前）
- 避免在限流逻辑中执行耗时操作
- 使用异步日志记录

---

## 验收标准

- ✅ 超出频率限制后返回 HTTP 429
- ✅ 正常请求不受影响
- ✅ 分布式环境下限流生效
- ✅ Redis 故障时不影响服务可用性
- ✅ 完整的单元测试覆盖
- ✅ 详细的日志记录
- ✅ 代码简洁易维护
- ✅ 支持多种限流类型
- ✅ 支持灵活配置

---

## 相关文档

- [TODO 清单](./todo.md)
- [密码重置 API 文档](./PASSWORD_RESET_API.md)
- [README](../README.md)

---

## 更新历史

- **2026-03-31**: 完成限流功能统一重构
  - 创建 `@RateLimit` 注解和 `RateLimitAspect` 切面
  - 重构 AuthController 的 5 个接口
  - 为 UserController 的 4 个接口添加限流
  - 更新测试和文档
  - 合并 3 个限流文档为统一指南

- **2026-03-31**: 完成认证接口限流保护功能
  - 实现 `RateLimitService` 服务
  - 为 login、signUp、forgot-password、refresh、reset-password 接口添加限流
  - 添加完整的单元测试

---

📌 **文档版本**: v2.0  
📅 **最后更新**: 2026-03-31  
👨‍💻 **维护者**: Portfolio Backend Team
