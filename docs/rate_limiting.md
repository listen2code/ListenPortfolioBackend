# 🛡️ 智能限流系统完整指南

## 概述

本项目实现了基于 AOP（面向切面编程）的智能限流系统，有效防止各种恶意攻击和滥用行为。系统支持多维度限流，使用 Redis 作为分布式存储，完全适配集群部署环境。

## 🎯 设计目标

- **多维度防护**：支持 IP、邮箱、Token、用户等多种限流维度
- **分布式支持**：基于 Redis 存储，天然支持集群部署
- **灵活配置**：通过注解轻松配置限流规则
- **高性能**：AOP 切面编程，对业务代码无侵入
- **自动清理**：利用 Redis TTL 机制自动过期清理

## 🏗️ 架构设计

### 核心组件

```
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│   @RateLimit     │    │ RateLimitAspect  │    │RateLimitService │
│   注解配置       │───▶│   AOP 切面       │───▶│   限流逻辑       │
└─────────────────┘    └──────────────────┘    └─────────────────┘
                                                       │
                                                       ▼
                                                 ┌─────────────┐
                                                 │    Redis    │
                                                 │  分布式存储  │
                                                 └─────────────┘
```

### 工作流程

1. **请求到达**：客户端请求带有 @RateLimit 注解的接口
2. **切面拦截**：RateLimitAspect 拦截方法调用
3. **标识符提取**：根据限流类型提取唯一标识（IP、邮箱等）
4. **限流检查**：调用 RateLimitService 检查是否超过限制
5. **响应处理**：通过则执行业务逻辑，拒绝则返回 429 错误

## 🚀 功能特性

### 支持的限流类型

| 类型 | 标识符 | 使用场景 | 防护目标 |
|------|--------|----------|----------|
| **IP** | 客户端 IP 地址 | 登录、注册等公开接口 | 防止 IP 暴力攻击 |
| **EMAIL** | 邮箱地址 | 注册、忘记密码 | 防止邮箱枚举、垃圾邮件 |
| **TOKEN** | JWT Token | 需要认证的接口 | 防止 Token 滥用 |
| **USER** | 用户名 | 用户操作接口 | 防止单用户恶意操作 |
| **CUSTOM** | 自定义表达式 | 特定业务需求 | 业务场景定制 |

### 限流配置示例

#### 基础配置
```java
@RateLimit(
    types = {RateLimitType.IP},
    maxRequests = 10,
    timeWindowSeconds = 60
)
public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
    // 登录逻辑
}
```

#### 多维度组合
```java
@RateLimit(
    types = {RateLimitType.IP, RateLimitType.EMAIL},
    maxRequests = 5,
    timeWindowSeconds = 300
)
public ResponseEntity<?> signUp(@Valid @RequestBody SignUpRequest request) {
    // 注册逻辑 - 同时限制 IP 和邮箱
}
```

#### 用户级别限流
```java
@RateLimit(
    types = {RateLimitType.USER},
    maxRequests = 100,
    timeWindowSeconds = 3600
)
public ResponseEntity<?> updateProfile(@Valid @RequestBody ProfileRequest request) {
    // 用户资料更新 - 限制单用户操作频率
}
```

## 📊 Redis 存储结构

### Key 设计
```
限流类型:标识符 → rate_limit:ip:192.168.1.100
                → rate_limit:email:user@example.com
                → rate_limit:token:abc123
                → rate_limit:user:username
```

### 存储格式
- **Key**: `rate_limit:{type}:{identifier}`
- **Value**: 请求计数（数字）
- **TTL**: 时间窗口（自动过期）

### 示例
```redis
# IP 限流
SET rate_limit:ip:192.168.1.100 1 EX 60

# 邮箱限流
SET rate_limit:email:user@example.com 3 EX 300

# 用户限流
SET rate_limit:user:john_doe 50 EX 3600
```

## 🛠️ 使用指南

### 1. 添加依赖

限流系统已集成在项目中，无需额外依赖。

### 2. 启用 Redis

确保 Redis 服务正常运行：
```bash
# Docker 方式
docker run -d -p 6379:6379 redis:7.2-alpine

# 或使用项目的 docker-compose
docker-compose up -d redis
```

### 3. 配置注解

在需要限流的 Controller 方法上添加 `@RateLimit` 注解：

```java
@RestController
public class AuthController {
    
    @PostMapping("/login")
    @RateLimit(
        types = {RateLimitType.IP},
        maxRequests = 10,
        timeWindowSeconds = 60
    )
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        // 业务逻辑
    }
}
```

### 4. 自定义限流类型

如需自定义限流逻辑，可以扩展 `RateLimitType` 枚举：

```java
public enum RateLimitType {
    IP, EMAIL, TOKEN, USER, 
    CUSTOM,  // 自定义类型
    API_KEY  // API 密钥限流
}
```

## 🔧 高级配置

### 环境变量配置

```properties
# Redis 配置（已在 application.properties 中配置）
spring.data.redis.host=localhost
spring.data.redis.port=6379
spring.data.redis.timeout=2000ms
```

### 自定义限流服务

可以扩展 `RateLimitService` 实现更复杂的限流逻辑：

```java
@Service
public class CustomRateLimitService extends RateLimitService {
    
    @Override
    public boolean isAllowed(String key, int maxRequests, int timeWindowSeconds) {
        // 实现自定义限流算法（如滑动窗口、令牌桶等）
        return super.isAllowed(key, maxRequests, timeWindowSeconds);
    }
}
```

## 🔍 监控与日志

### 日志记录

限流系统会记录相关日志：

```log
2024-03-31 20:15:30.123 INFO  --- RateLimitAspect : Rate limit exceeded for type: IP, identifier: 192.168.1.100...
2024-03-31 20:15:30.124 WARN  --- AuthController : Too many requests from IP: 192.168.1.100
```

### Prometheus 指标

可以添加限流相关的监控指标：

```java
// 在 RateLimitService 中添加
private final Counter rateLimitCounter = Counter.builder("rate_limit_total")
    .description("Total number of rate limit checks")
    .tag("type", "ip")
    .register(Metrics.globalRegistry);
```

## 🚨 错误处理

### 限流触发响应

当触发限流时，系统返回标准 HTTP 429 响应：

```json
{
  "result": "RATE_LIMIT_EXCEEDED",
  "messageId": "RATE_LIMIT_EXCEEDED",
  "message": "Requests are too frequent, please try again later",
  "body": null
}
```

### 异常处理

限流异常由 `GlobalExceptionHandler` 统一处理，确保响应格式一致。

## 🔒 安全考虑

### 1. 标识符掩码

日志中的敏感标识符会被掩码处理：
```java
private String maskIdentifier(String identifier) {
    if (identifier.length() <= 4) return identifier;
    return identifier.substring(0, Math.min(10, identifier.length())) + "...";
}
```

### 2. 防护绕过

- **IP 限流**：支持 X-Forwarded-For 和 X-Real-For 头
- **邮箱限流**：不区分大小写，防止大小写绕过
- **Token 限流**：验证 Token 格式和有效性

### 3. 分布式一致性

- 使用 Redis 原子操作确保计数准确
- 支持 Redis 集群和哨兵模式
- 网络异常时采用保守策略（允许请求）

## 🎯 最佳实践

### 1. 限流策略建议

| 接口类型 | 推荐限流 | 时间窗口 | 理由 |
|----------|----------|----------|------|
| 登录 | IP: 10次/分钟 | 60s | 防止暴力破解 |
| 注册 | IP+邮箱: 5次/5分钟 | 300s | 防止垃圾注册 |
| 忘记密码 | 邮箱: 3次/小时 | 3600s | 防止邮件轰炸 |
| 数据修改 | 用户: 100次/小时 | 3600s | 防止恶意操作 |
| 文件上传 | IP: 20次/分钟 | 60s | 防止资源滥用 |

### 2. 性能优化

- **Redis 连接池**：配置合适的连接池大小
- **批量操作**：对于高频接口考虑批量限流检查
- **本地缓存**：对于静态限流规则可考虑本地缓存

### 3. 运维监控

- **监控限流触发频率**：设置告警阈值
- **Redis 性能监控**：关注内存使用和响应时间
- **业务指标关联**：分析限流对业务的影响

## 🔮 扩展计划

### 短期计划
- [ ] 滑动窗口算法支持
- [ ] 令牌桶算法实现
- [ ] 限流规则动态配置

### 长期计划
- [ ] 机器学习驱动的智能限流
- [ ] 分布式限流协调
- [ ] 限流效果分析仪表板

---

## 📚 相关文档

- [Spring AOP 文档](https://docs.spring.io/spring-framework/docs/current/reference/html/core.html#aop)
- [Redis 文档](https://redis.io/documentation)
- [Rate Limiting 最佳实践](https://kubernetes.io/docs/concepts/services-networking/ingress/#rate-limiting)

---

**最后更新**: 2026-03-31  
**维护者**: Development Team
