# 安全功能完整指南

**Status**: `Partially Implemented`

> ⚠️ **文档说明**：本文档同时包含 **当前已实现的安全能力** 与 **规划中的扩展方案**。
> - ✅ 标记的部分可在当前仓库代码中找到对应实现
> - 🔮 标记的部分为设计草案或示例代码，当前仓库未落地
> - 如与代码冲突，以 `src/main` 中实际实现为准
>
> 本文档的目标是帮助学习与 review，不应将所有章节默认理解为“已经上线的完整安全体系”。

## 概述

当前已落地的核心能力主要包括：JWT 认证与刷新、基于 Redis 的 Token 黑名单、`BCryptPasswordEncoder` 密码加密、基于注解 + AOP 的限流、基础输入校验，以及通过环境变量注入敏感配置。审计日志、异常检测、自动化威胁响应、数据脱敏等章节仍属于设计规划。

## 🛡️ 安全架构总览

### 防护层次

```
┌─────────────────────────────────────────────────────────────┐
│              网络层安全（主要依赖部署基础设施）               │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐        │
│  │  HTTPS/TLS* │  │  防火墙规则* │  │  DDoS 防护* │        │
│  └─────────────┘  └─────────────┘  └─────────────┘        │
└─────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────┐
│                    应用层安全（当前核心实现）                │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐        │
│  │  注解限流    │  │  JWT 认证   │  │  权限控制    │        │
│  └─────────────┘  └─────────────┘  └─────────────┘        │
└─────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────┐
│           数据与配置安全（部分实现，部分规划）               │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐        │
│  │  密码加密    │  │  数据脱敏*   │  │  审计日志*   │        │
│  └─────────────┘  └─────────────┘  └─────────────┘        │
└─────────────────────────────────────────────────────────────┘
```

* 带 `*` 的部分依赖部署环境或仍属于规划，不代表当前应用内已完整实现。

## 🔐 认证与授权 ✅

### JWT 认证系统 ✅

#### JWT Token 结构
```json
{
  "header": {
    "alg": "HS256",
    "typ": "JWT"
  },
  "payload": {
    "sub": "username",
    "iat": 1640995200,
    "exp": 1640998800
  },
  "signature": "HMACSHA256(base64UrlEncode(header) + "." + base64UrlEncode(payload), secret)"
}
```

当前 `JwtUtil` 主要写入标准 claims `sub`、`iat`、`exp`；`iss` / `aud` 并未在当前实现中显式设置。

#### Token 生命周期管理

```
用户登录
    │
    ├─ 生成 Access Token（5分钟）
    ├─ 生成 Refresh Token（24小时）
    │
    ├─ Access Token 使用
    │   ├─ 每次请求验证签名与过期时间
    │   ├─ 检查 Redis 黑名单
    │   └─ 需要时由客户端调用 /refresh 获取新 Access Token
    │
    ├─ Token 失效场景
    │   ├─ 用户退出登录 → 当前 Token 加入黑名单
    │   ├─ 修改密码 / 注销账号 → 相关失效处理以当前接口实现为准
    │   └─ Token 过期 → 自然失效
    │
    └─ Token 刷新
        ├─ 验证 Refresh Token
        ├─ 生成新的 Access Token
        └─ 是否轮换 Refresh Token 以当前代码返回结果为准
```

#### Token 黑名单机制

**Redis 黑名单存储结构**：
```
Key: token:blacklist:<jwt_token>
Value: "blacklisted"
TTL: Token 剩余有效期（上限 24 小时；若 token 已过期则回退为默认 24 小时）
```

**黑名单操作流程**：
```java
@Service
public class TokenBlacklistService {
    
    // 将 Token 加入黑名单
    public void addToBlacklist(String token, long expiration) {
        String key = BLACKLIST_PREFIX + token;
        long ttl = calculateTTL(expiration);
        redisTemplate.opsForValue().set(key, "blacklisted", ttl, TimeUnit.MILLISECONDS);
    }
    
    // 检查 Token 是否在黑名单
    public boolean isBlacklisted(String token) {
        String key = BLACKLIST_PREFIX + token;
        return redisTemplate.hasKey(key);
    }
}
```

### 密码安全 ✅

#### BCrypt 加密 ✅

实际实现通过 `SecurityConfig.java` 中的 `BCryptPasswordEncoder` Bean（使用 Spring 默认 cost）：

```java
// SecurityConfig.java
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
}
```

**BCrypt 特性**：
- **盐值自动生成**：每次加密使用不同的盐值
- **计算成本可调**：通过 cost 参数控制计算复杂度
- **抗彩虹表**：盐值机制防止预计算攻击

#### 🔮 密码策略验证器（设计规划，当前使用 `@Valid` 注解做基本校验）

> 以下 `PasswordPolicyValidator` 类尚未实现，当前仅依赖 `@Valid` 注解做基本校验。

```java
@Component
public class PasswordPolicyValidator {
    
    public boolean validate(String password) {
        // 长度要求：至少 8 位
        if (password.length() < 8) return false;
        
        // 复杂度要求：包含大小写字母、数字、特殊字符
        boolean hasUpper = password.chars().anyMatch(Character::isUpperCase);
        boolean hasLower = password.chars().anyMatch(Character::isLowerCase);
        boolean hasDigit = password.chars().anyMatch(Character::isDigit);
        boolean hasSpecial = password.chars().anyMatch(c -> !Character.isLetterOrDigit(c));
        
        return hasUpper && hasLower && hasDigit && hasSpecial;
    }
}
```

## 🚦 限流系统 ✅

### 多维度限流

#### 限流类型详解

| 限流类型 | 标识符来源 | 防护目标 | 典型配置 |
|----------|------------|----------|----------|
| **IP 限流** | X-Forwarded-For, X-Real-IP, RemoteAddr | 防止 IP 暴力攻击 | 10次/分钟 |
| **邮箱限流** | 请求参数中的 email 字段 | 防止邮箱枚举、垃圾邮件 | 10次/分钟（如 forgot-password） |
| **Token 限流** | 请求参数中的 reset token 字段 | 防止重置链接滥用 | 10次/分钟（如 reset-password） |
| **用户限流** | SecurityContext 中的用户名 | 防止单用户恶意操作 | 5-100次/分钟（视接口而定） |
| **自定义限流** | SpEL 表达式 | 业务特定需求 | 当前仓库未见实际使用 |

#### 限流实现架构

```java
@Aspect
@Component
public class RateLimitAspect {
    
    @Around("@annotation(rateLimit)")
    public Object checkRateLimit(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        HttpServletRequest request = getCurrentRequest();
        
        // 检查每种限流类型
        for (RateLimitType type : rateLimit.types()) {
            String identifier = extractIdentifier(type, request, joinPoint);
            boolean allowed = rateLimitService.isAllowed(
                type.name().toLowerCase() + ":" + identifier,
                rateLimit.maxRequests(),
                rateLimit.timeWindowSeconds()
            );
            
            if (!allowed) {
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(ApiResponse.error(
                        "RATE_LIMIT_EXCEEDED",
                        "Requests are too frequent, please try again later"
                    ));
            }
        }
        
        return joinPoint.proceed();
    }
}
```

#### Redis 限流算法

当前实现使用 **固定窗口计数器**，而不是 ZSet 滑动窗口。

**当前实现示意**：
```java
@Service
public class RateLimitService {
    
    public boolean isAllowed(String identifier, int maxRequests, int timeWindowSeconds) {
        long currentWindow = System.currentTimeMillis() / (timeWindowSeconds * 1000L);
        String key = "rate_limit:" + identifier + ":" + currentWindow;
 
        Long count = redisTemplate.opsForValue().increment(key);
        if (count == 1) {
            redisTemplate.expire(key, timeWindowSeconds, TimeUnit.SECONDS);
        }
 
        return count <= maxRequests;
    }
}
```

如需滑动窗口、漏桶或令牌桶算法，应视为后续增强项，而非当前实现事实。

## 🔒 数据安全

### 敏感数据处理

#### 配置外部化 ✅

当前仓库使用 `application.properties` 作为主配置文件，并通过环境变量覆盖敏感值；Docker 场景仅用 `application-docker.properties` 覆盖与本地不同的主机地址。

**环境变量配置示例**：
```bash
# 数据库配置
export DB_URL=jdbc:mysql://localhost:3306/portfolio
export DB_USERNAME=portfolio_user
export DB_PASSWORD=secure_password

# JWT 配置
export JWT_SECRET=your-super-strong-secret-key-at-least-256-bits-long
export JWT_EXPIRATION=300000
export JWT_REFRESH_EXPIRATION=86400000

# 邮件配置
export MAIL_HOST=smtp.gmail.com
export MAIL_USERNAME=your-email@gmail.com
export MAIL_PASSWORD=your-app-password

# Redis 配置
export REDIS_HOST=localhost
export REDIS_PORT=6379
```

#### 🔮 数据脱敏（设计规划，代码未实现）

> 以下 `DataMaskingUtil` 类尚未实现，当前仅在 `UserController.maskToken()` 中有简单的 Token 遮盖。

```java
@Component
public class DataMaskingUtil {
    
    // 邮箱脱敏
    public String maskEmail(String email) {
        if (email == null || email.length() < 5) return email;
        
        int atIndex = email.indexOf('@');
        if (atIndex <= 0) return email;
        
        String username = email.substring(0, atIndex);
        String domain = email.substring(atIndex);
        
        if (username.length() <= 2) {
            return username.charAt(0) + "***" + domain;
        }
        
        return username.charAt(0) + "***" + username.charAt(username.length() - 1) + domain;
    }
    
    // IP 地址脱敏
    public String maskIp(String ip) {
        if (ip == null) return ip;
        
        String[] parts = ip.split("\\.");
        if (parts.length != 4) return ip;
        
        return parts[0] + "." + parts[1] + ".***.***";
    }
}

## 📊 🔮 审计与扩展监控（设计规划，代码未实现）

> 以下安全审计、专用安全指标、异常检测、威胁响应等功能均为设计规划，尚未在当前仓库中实现。
> 本章中的 `SecurityAuditService`、`SecurityMetrics` 等类为说明性示例，不应被视为现有代码。
> 当前与安全排查相关的已实现能力主要是：Actuator / Prometheus 指标、Grafana 监控栈、`RequestLoggingFilter` 请求日志，以及部分控制器中的敏感 token 遮盖日志。

## 🚨 威胁防护

### 当前已具备的基础防护

#### 1. 暴力破解与滥用请求

- 通过 `@RateLimit` + `RateLimitAspect` 对部分接口执行 IP / Email / User / Token 维度限流
- 当前没有账户锁定、验证码或自动封禁 IP 的完整机制

#### 2. 会话与令牌滥用

- 使用短期 Access Token + Refresh Token 模式
- 支持将已退出登录的 Token 写入 Redis 黑名单
- 设备指纹绑定、异常设备识别仍属于规划

#### 3. CSRF 与接口访问模型

- 当前 API 采用无状态 JWT 认证模型
- 当前仓库未实现单独的 CSRF Token 交互流程
- CORS 策略以当前 `WebConfig` 与部署环境配置为准

#### 4. 注入与输入安全

- 主要依赖 Bean Validation、Spring MVC 参数绑定、JPA 参数化查询
- 未实现独立的 WAF / IDS / SQL 审计层

#### 5. 传输链路安全

- 应用代码当前未强制开启 `requiresSecure()` 或内置 HSTS 配置
- 生产环境 HTTPS / TLS 通常应由反向代理、网关或云负载均衡负责终止
- 证书固定、自动中间人攻击检测仍属于规划

### 🔮 异常检测与自动化威胁响应（设计规划）

异常登录识别、失败事件聚合、自动封禁、专用安全告警等能力尚未在当前代码中实现；如需补齐，应新增专用领域服务与审计事件模型，而不是默认认为现有请求日志已经覆盖这些能力。

## 🔧 安全配置

### 当前配置基线

当前仓库采用 `application.properties` 作为主配置文件，通过环境变量覆盖敏感值；Docker 场景仅使用 `application-docker.properties` 覆盖数据库与 Redis hostname。当前并不存在一套长期维护的 `dev / test / prod` 安全 profile 矩阵。

#### 当前已存在的关键配置

```properties
jwt.secret=${JWT_SECRET:...}
jwt.expiration=${JWT_EXPIRATION:300000}
jwt.refresh-expiration=${JWT_REFRESH_EXPIRATION:86400000}
spring.datasource.username=${DB_USERNAME:root}
spring.datasource.password=${DB_PASSWORD:...}
spring.mail.password=${MAIL_PASSWORD:...}
management.endpoints.web.exposure.include=health,info,prometheus
```

#### 生产部署建议（运维侧）

- 使用反向代理或云负载均衡终止 HTTPS / TLS
- 通过环境变量或密钥管理系统覆盖默认密钥与密码
- 收紧 Actuator 暴露范围与 Swagger/OpenAPI 访问策略
- 根据生产数据库策略调整 `useSSL`、连接池与日志级别配置

以上建议属于部署基线，不代表当前应用内已经自动启用。

## 📋 安全运维

### 定期安全检查

#### 1. 依赖漏洞扫描

```bash
# 使用 Maven 检查依赖漏洞
./mvnw org.owasp:dependency-check-maven:check

# 使用 SpotBugs 进行安全检查
./mvnw spotbugs:check
```

#### 2. 配置审计

```bash
# 检查敏感配置泄露
grep -r "password\|secret\|key" src/ --include="*.properties" --include="*.yml"

# 检查硬编码敏感信息
grep -r "localhost\|127\.0\.0\.1\|test" src/main/java/
```

#### 3. 日志审计

```bash
# 当前默认日志主要输出到控制台；如已重定向到文件，请将 <your-log-file> 替换为实际日志路径

# 查找限流或认证失败相关日志
grep -E "Rate limit exceeded|Invalid credentials|Password reset failed" <your-log-file>

# 查找 token 黑名单与登出相关日志
grep -E "token added to blacklist|logged out successfully|Current token added to blacklist" <your-log-file>
```

### 应急响应

#### 安全事件响应流程

```
安全事件发现
    │
    ├─ 立即评估威胁等级
    ├─ 启动应急响应预案
    │
    ├─ 威胁遏制
    │   ├─ 隔离受影响系统
    │   ├─ 阻断攻击源 IP
    │   └─ 撤销可疑 Token
    │
    ├─ 事件调查
    │   ├─ 收集日志证据
    │   ├─ 分析攻击路径
    │   └─ 评估影响范围
    │
    ├─ 恢复操作
    │   ├─ 修复安全漏洞
    │   ├─ 恢复系统服务
    │   └─ 加强安全防护
    │
    └─ 事后总结
        ├─ 编写事件报告
        ├─ 更新安全策略
        └─ 完善监控告警
```

#### 🔮 自动化响应（设计规划）

> 以下 `AutomatedSecurityResponse` 为说明性示例，当前仓库未实现对应事件模型、自动封禁或自动撤销 Token 流程。

```java
@Component
public class AutomatedSecurityResponse {
    
    @EventListener
    public void handleSecurityThreat(SecurityThreatEvent event) {
        switch (event.getThreatLevel()) {
            case CRITICAL:
                // 自动阻断 IP
                blockIpAddress(event.getSourceIp(), Duration.ofHours(24));
                // 撤销所有用户 Token
                revokeAllUserTokens(event.getUsername());
                // 发送紧急告警
                sendUrgentAlert(event);
                break;
                
            case HIGH:
                // 增加 IP 限流
                increaseRateLimit(event.getSourceIp(), 1);
                // 发送安全告警
                sendSecurityAlert(event);
                break;
                
            case MEDIUM:
                // 记录安全事件
                logSecurityEvent(event);
                break;
        }
    }
}
```

## 🎯 安全最佳实践

### 1. 开发安全

- **安全编码规范**：遵循 OWASP 安全编码指南
- **代码审查**：所有代码变更必须经过安全审查
- **安全测试**：集成安全测试到 CI/CD 流程
- **依赖管理**：定期更新依赖，及时修复漏洞

### 2. 部署安全

- **最小权限原则**：应用和服务使用最小必要权限
- **网络隔离**：使用防火墙和网络分段
- **加密传输**：所有网络通信使用加密
- **定期备份**：重要数据定期备份和恢复测试

### 3. 运维安全

- **访问控制**：严格的访问控制和权限管理
- **审计日志**：完整的操作审计日志
- **监控告警**：实时安全监控和告警
- **应急演练**：定期安全应急演练

---

## 📚 相关文档

- [OWASP Top 10](https://owasp.org/www-project-top-ten/)
- [Spring Security 参考文档](https://docs.spring.io/spring-security/reference/)
- [JWT 安全最佳实践](https://auth0.com/blog/json-web-token-best-practices/)
- [Redis 安全配置](https://redis.io/topics/security)

---

**最后更新**: 2026-03-31  
**维护者**: Development Team
