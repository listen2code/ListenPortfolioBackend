# 安全功能完整指南

> ⚠️ **文档说明**：本文档同时包含 **已实现功能** 和 **设计规划**。
> - ✅ 标记的部分已在代码中实现
> - 🔮 标记的部分为设计规划，代码尚未实现
>
> 具体实现状态请参照各章节标注。

## 概述

本项目已实现多层安全防护体系的核心部分，包括 JWT 认证授权、AOP 智能限流、Token 黑名单、BCrypt 密码加密。部分高级功能（审计日志、异常检测、威胁响应等）为设计规划，尚未落地到代码中。

## 🛡️ 安全架构总览

### 防护层次

```
┌─────────────────────────────────────────────────────────────┐
│                    网络层安全                                │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐        │
│  │  HTTPS/TLS  │  │  防火墙规则  │  │  DDoS 防护  │        │
│  └─────────────┘  └─────────────┘  └─────────────┘        │
└─────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────┐
│                    应用层安全                                │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐        │
│  │  智能限流    │  │  JWT 认证   │  │  权限控制    │        │
│  └─────────────┘  └─────────────┘  └─────────────┘        │
└─────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────┐
│                    数据层安全                                │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐        │
│  │  密码加密    │  │  数据脱敏    │  │  审计日志    │        │
│  └─────────────┘  └─────────────┘  └─────────────┘        │
└─────────────────────────────────────────────────────────────┘
```

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
    "exp": 1640998800,
    "iss": "portfolio-api",
    "aud": "portfolio-client"
  },
  "signature": "HMACSHA256(base64UrlEncode(header) + "." + base64UrlEncode(payload), secret)"
}
```

#### Token 生命周期管理

```
用户登录
    │
    ├─ 生成 Access Token（5分钟）
    ├─ 生成 Refresh Token（24小时）
    │
    ├─ Access Token 使用
    │   ├─ 每次请求验证
    │   ├─ 检查黑名单
    │   └─ 自动刷新机制
    │
    ├─ Token 失效场景
    │   ├─ 用户退出登录 → 加入黑名单
    │   ├─ 修改密码 → 加入黑名单
    │   ├─ 注销账号 → 加入黑名单
    │   └─ Token 过期 → 自然失效
    │
    └─ Token 刷新
        ├─ 验证 Refresh Token
        ├─ 生成新的 Access Token
        └─ 可选：生成新的 Refresh Token
```

#### Token 黑名单机制

**Redis 黑名单存储结构**：
```
Key: token:blacklist:<jwt_token_hash>
Value: "blacklisted"
TTL: Token 剩余有效期
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

#### 🔮 密码策略验证器（设计规划，代码未实现）

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

## 🚦 智能限流系统

### 多维度限流

#### 限流类型详解

| 限流类型 | 标识符来源 | 防护目标 | 典型配置 |
|----------|------------|----------|----------|
| **IP 限流** | X-Forwarded-For, X-Real-IP, RemoteAddr | 防止 IP 暴力攻击 | 10次/分钟 |
| **邮箱限流** | 请求参数中的 email 字段 | 防止邮箱枚举、垃圾邮件 | 5次/5分钟 |
| **Token 限流** | JWT Token 中的用户名 | 防止 Token 滥用 | 100次/小时 |
| **用户限流** | SecurityContext 中的用户名 | 防止单用户恶意操作 | 50次/小时 |
| **自定义限流** | SpEL 表达式 | 业务特定需求 | 根据业务定 |

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
                throw new RateLimitExceededException("Rate limit exceeded for " + type);
            }
        }
        
        return joinPoint.proceed();
    }
}
```

#### Redis 限流算法

**滑动窗口算法实现**：
```java
@Service
public class RateLimitService {
    
    public boolean isAllowed(String key, int maxRequests, int timeWindowSeconds) {
        long currentTime = System.currentTimeMillis();
        long windowStart = currentTime - timeWindowSeconds * 1000L;
        
        // 使用 ZSet 实现滑动窗口
        redisTemplate.opsForZSet().removeRangeByScore(key, 0, windowStart);
        
        // 检查当前窗口内的请求数
        Long currentCount = redisTemplate.opsForZSet().count(key, windowStart, currentTime);
        
        if (currentCount < maxRequests) {
            // 添加当前请求
            redisTemplate.opsForZSet().add(key, UUID.randomUUID().toString(), currentTime);
            redisTemplate.expire(key, timeWindowSeconds);
            return true;
        }
        
        return false;
    }
}
```

## 🔒 数据安全

### 敏感数据处理

#### 配置外部化 ✅

**环境变量配置**：
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
```

### 🔮 数据传输安全（设计规划，代码未实现）

#### HTTPS 强制

> 当前代码未启用 HTTPS 强制，生产环境应通过反向代理（Nginx/ALB）终止 TLS。

```java
@Configuration
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // 强制 HTTPS
            .requiresChannel(channel -> channel
                .anyRequest().requiresSecure()
            )
            // 其他安全配置...
            ;
        return http.build();
    }
}
```

#### 🔮 CORS 配置（设计规划，当前使用 WebConfig 简单配置）

```java
@Configuration
public class CorsConfig {
    
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("https://app.example.com"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type"));
        configuration.setAllowCredentials(true);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
```

## 📊 🔮 审计与监控（设计规划，代码未实现）

> 以下安全审计、监控指标、异常检测、威胁响应等功能均为设计规划，尚未在代码中实现。
> 当前已实现的监控能力：Prometheus 指标（Actuator）、Grafana 仪表板、`RequestLoggingFilter` 请求日志。

### 安全审计日志

#### 结构化安全日志

```json
{
  "timestamp": "2024-03-31T20:15:30.123Z",
  "level": "INFO",
  "logger": "com.listen.portfolio.audit.SecurityAuditService",
  "message": "User login successful",
  "audit": {
    "event": "USER_LOGIN",
    "userId": "12345",
    "username": "john_doe",
    "ipAddress": "192.168.1.100",
    "userAgent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64)",
    "sessionId": "session-abc-123",
    "result": "SUCCESS",
    "timestamp": "2024-03-31T20:15:30.123Z",
    "duration": 150
  }
}
```

#### 审计服务实现

```java
@Service
public class SecurityAuditService {
    
    private static final Logger auditLogger = LoggerFactory.getLogger("SECURITY_AUDIT");
    
    public void auditLogin(String username, String ipAddress, String userAgent, boolean success) {
        AuditEvent event = AuditEvent.builder()
            .event(success ? "USER_LOGIN" : "USER_LOGIN_FAILED")
            .username(username)
            .ipAddress(maskIp(ipAddress))
            .userAgent(userAgent)
            .result(success ? "SUCCESS" : "FAILED")
            .timestamp(Instant.now())
            .build();
            
        auditLogger.info("Security event: {}", event.toJson());
    }
    
    public void auditTokenRevocation(String username, String reason) {
        AuditEvent event = AuditEvent.builder()
            .event("TOKEN_REVOKED")
            .username(username)
            .reason(reason)
            .timestamp(Instant.now())
            .build();
            
        auditLogger.warn("Token revoked: {}", event.toJson());
    }
}
```

### 安全监控指标

#### Prometheus 安全指标

```java
@Component
public class SecurityMetrics {
    
    // 登录尝试计数
    private final Counter loginAttempts = Counter.builder("security_login_attempts_total")
        .description("Total number of login attempts")
        .tag("result", "unknown")
        .register(Metrics.globalRegistry);
    
    // 限流触发计数
    private final Counter rateLimitHits = Counter.builder("security_rate_limit_hits_total")
        .description("Total number of rate limit hits")
        .tag("type", "unknown")
        .register(Metrics.globalRegistry);
    
    // Token 黑名单大小
    private final Gauge tokenBlacklistSize = Gauge.builder("security_token_blacklist_size")
        .description("Current size of token blacklist")
        .register(Metrics.globalRegistry, this, SecurityMetrics::getBlacklistSize);
    
    public void recordLoginAttempt(String result) {
        loginAttempts.tags("result", result).increment();
    }
    
    public void recordRateLimitHit(String type) {
        rateLimitHits.tags("type", type).increment();
    }
    
    private double getBlacklistSize() {
        // 从 Redis 获取黑名单大小
        return tokenBlacklistService.getBlacklistSize();
    }
}
```

#### Grafana 安全仪表板

**关键监控面板**：
1. **登录失败率**：失败登录 / 总登录尝试
2. **限流触发频率**：各类型限流触发次数
3. **Token 黑名单趋势**：黑名单大小变化
4. **异常登录检测**：异常 IP、异常时间登录
5. **安全事件时间线**：安全事件的时间分布

## 🚨 威胁防护

### 常见攻击防护（部分已实现，部分为规划）

#### 1. 暴力破解攻击

**防护措施**：
- IP 限流：同一 IP 限制登录尝试频率
- 账户锁定：连续失败后临时锁定账户
- 验证码：高风险场景增加验证码验证

```java
@RateLimit(
    types = {RateLimitType.IP, RateLimitType.EMAIL},
    maxRequests = 5,
    timeWindowSeconds = 300
)
public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
    // 登录逻辑
}
```

#### 2. 会话劫持

**防护措施**：
- JWT 短期有效：Access Token 5分钟过期
- Token 黑名单：退出登录立即失效
- 绑定设备：可选的设备指纹验证

#### 3. CSRF 攻击

**防护措施**：
- 无状态设计：JWT 天然免疫 CSRF
- 同源策略：配置 CORS 白名单
- 双重提交：如需要可启用 CSRF Token

#### 4. 注入攻击

**防护措施**：
- 参数化查询：JPA 原生 SQL 使用参数绑定
- 输入验证：所有用户输入严格验证
- 输出编码：模板引擎自动转义

#### 5. 中间人攻击

**防护措施**：
- HTTPS 强制：所有 API 强制使用 HTTPS
- HSTS 头：设置 HTTP Strict Transport Security
- 证书固定：可选的客户端证书固定

### 异常检测

#### 异常登录检测

```java
@Service
public class AnomalyDetectionService {
    
    public boolean isSuspiciousLogin(String username, String ipAddress, String userAgent) {
        // 检查异常 IP
        if (isNewIpAddress(username, ipAddress)) {
            return true;
        }
        
        // 检查异常时间
        if (isUnusualTime(username)) {
            return true;
        }
        
        // 检查异常设备
        if (isNewDevice(username, userAgent)) {
            return true;
        }
        
        return false;
    }
    
    private boolean isNewIpAddress(String username, String ipAddress) {
        // 检查用户历史 IP 列表
        Set<String> historicalIps = getUserHistoricalIps(username);
        return !historicalIps.contains(ipAddress);
    }
}
```

#### 实时威胁检测

```java
@Component
public class ThreatDetectionService {
    
    @EventListener
    public void handleLoginFailed(LoginFailedEvent event) {
        // 检查是否为暴力攻击
        if (isBruteForceAttack(event.getIpAddress())) {
            // 自动拉黑 IP
            blockIpAddress(event.getIpAddress(), Duration.ofHours(1));
            
            // 发送告警
            sendSecurityAlert("Brute force attack detected", event);
        }
    }
    
    private boolean isBruteForceAttack(String ipAddress) {
        // 检查短时间内的失败次数
        String key = "login_failed:" + ipAddress;
        Long failedCount = redisTemplate.opsForValue().get(key);
        return failedCount != null && failedCount > 10;
    }
}
```

## 🔧 安全配置

### 生产环境安全清单

#### 1. 基础安全配置

```properties
# 安全相关配置
server.ssl.enabled=true
server.ssl.key-store=classpath:keystore.p12
server.ssl.key-store-password=${SSL_KEYSTORE_PASSWORD}
server.ssl.key-store-type=PKCS12

# Session 配置
server.servlet.session.timeout=1800
server.servlet.session.cookie.http-only=true
server.servlet.session.cookie.secure=true

# 安全头配置
security.headers.frame-options=DENY
security.headers.content-type-options=nosniff
security.headers.xss-protection=1; mode=block
```

#### 2. JWT 安全配置

```properties
# JWT 强安全配置
jwt.secret=${JWT_SECRET}
jwt.expiration=300000
jwt.refresh-expiration=86400000

# JWT 算法配置（仅使用强算法）
jwt.algorithm=HS256
jwt.issuer=portfolio-api
jwt.audience=portfolio-client
```

#### 3. 数据库安全配置

```properties
# 数据库连接安全
spring.datasource.url=jdbc:mysql://localhost:3306/portfolio?useSSL=true&verifyServerCertificate=true
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}

# 连接池安全配置
spring.datasource.hikari.leak-detection-threshold=60000
spring.datasource.hikari.connection-test-query=SELECT 1
spring.datasource.hikari.connection-timeout=30000
```

### 环境隔离

#### 开发环境

```properties
# 开发环境 - 相对宽松的安全配置
spring.profiles.active=dev
logging.level.com.listen.portfolio=DEBUG
security.debug=true
```

#### 测试环境

```properties
# 测试环境 - 接近生产的安全配置
spring.profiles.active=test
logging.level.com.listen.portfolio=INFO
security.debug=false
```

#### 生产环境

```properties
# 生产环境 - 最严格的安全配置
spring.profiles.active=prod
logging.level.com.listen.portfolio=WARN
security.debug=false
management.endpoints.web.exposure.include=health,info
```

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
# 分析安全日志
grep "SECURITY_AUDIT" logs/application.log | grep "FAILED"

# 检查异常登录模式
grep "USER_LOGIN_FAILED" logs/application.log | awk '{print $1}' | sort | uniq -c | sort -nr
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

#### 自动化响应

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
