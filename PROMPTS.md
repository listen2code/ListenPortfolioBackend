# 🤖 ListenPortfolioBackend - AI 开发助手系统提示

## 📋 项目概述

ListenPortfolioBackend 是 [ListenPortfolioFlutter](README-app.md) 的后端 REST API，基于 Spring Boot 4.0.1 构建，提供安全的 JWT 认证、作品集数据管理和完整的可观测性支持。

**生产环境 API**: `https://api.lPortfolio.com`

**主要能力：**
- **完整认证流程**：注册、登录、Token 刷新、忘记密码、修改密码（自动 Token 失效）、账号注销、退出登录
- **作品集 API**：关于我（需登录）、项目列表（公开）、用户信息
- **Token 黑名单**：基于 Redis 的 JWT 黑名单，支持退出登录和密码修改时立即失效
- **智能限流**：基于 AOP 的多维度限流系统（IP、邮箱、Token、用户），防止暴力攻击
- **邮件服务**：完整的 SMTP 邮件发送，支持密码重置、HTML 模板、多邮件提供商
- **可观测性**：Prometheus 指标、Grafana 仪表板、结构化 JSON 日志
- **代码质量**：JaCoCo 覆盖率报告、SpotBugs + Find Security Bugs 静态分析
- **Docker 就绪**：MySQL 8 + Redis 7 + Prometheus + Grafana 完整栈
- **灵活部署**：支持 JAR 和 WAR 两种打包方式，适应不同部署环境

## 🎯 AI 开发助手核心规则

### 📝 编码规范

#### 语言使用原则
- **代码注释**：必须使用中文，清晰说明业务逻辑和设计思路
- **文档内容**：必须使用中文，包括 README、API 文档、技术文档
- **日志输出**：必须使用英文，便于国际化团队协作和日志分析
- **变量命名**：使用英文，遵循 Java 命名规范
- **API 响应消息**：使用中文，提升用户体验

#### 代码质量标准
```java
/**
 * 用户认证服务
 * 
 * 功能说明：
 * - 处理用户注册、登录、密码管理等核心认证功能
 * - 集成 JWT Token 生成和验证
 * - 支持密码重置邮件发送
 * - 实现登录失败限流保护
 * 
 * 设计原则：
 * - 单一职责：专注于认证相关业务逻辑
 * - 事务边界：确保数据一致性
 * - 安全优先：所有密码操作使用 BCrypt 加密
 * - 性能优化：合理使用缓存减少数据库访问
 */
@Service
@Transactional
public class AuthService {
    
    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);
    
    /**
     * 用户注册
     * 
     * @param signUpRequest 注册请求信息
     * @return 注册成功返回 true，用户名已存在返回 false
     * @throws BusinessException 当注册过程中发生业务异常时抛出
     */
    public boolean signUp(SignUpRequest signUpRequest) {
        logger.info("Processing user registration for username: {}", signUpRequest.getUserName());
        
        try {
            // 检查用户名是否已存在
            if (userRepository.findByNameCaseSensitive(signUpRequest.getUserName()).isPresent()) {
                logger.warn("Username already exists: {}", signUpRequest.getUserName());
                return false;
            }
            
            // 创建新用户
            UserEntity user = createUserFromRequest(signUpRequest);
            userRepository.save(user);
            
            logger.info("User registered successfully: {}", signUpRequest.getUserName());
            return true;
            
        } catch (Exception e) {
            logger.error("Failed to register user: {}, error: {}", signUpRequest.getUserName(), e.getMessage());
            throw new BusinessException("User registration failed", e);
        }
    }
}
```

### 🏗️ 架构设计原则

#### 分层架构规范
```
┌─────────────────────────────────────────────────────────┐
│                    Controller 层                          │
│  - 参数校验和格式转换                                      │
│  - 调用 Service 层业务逻辑                                 │
│  - 统一响应格式封装                                       │
│  - 不包含业务规则实现                                      │
└─────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────┐
│                    Service 层                            │
│  - 业务规则实现和用例编排                                  │
│  - 事务边界控制 (@Transactional)                         │
│  - Entity 与 DTO 转换                                   │
│  - 外部服务集成                                           │
└─────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────┐
│                  Repository 层                           │
│  - 数据访问接口定义                                       │
│  - 查询方法封装                                           │
│  - 返回 Entity 或投影对象                                 │
└─────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────┐
│                    Entity 层                             │
│  - JPA 实体映射                                           │
│  - 数据库表结构对应                                       │
│  - 不对外暴露，仅用于持久化                               │
└─────────────────────────────────────────────────────────┘
```

#### 核心设计原则
1. **单一职责原则**：每个类只负责一个明确的职责
2. **开闭原则**：对扩展开放，对修改封闭
3. **依赖倒置**：依赖抽象而非具体实现
4. **接口隔离**：使用小而专一的接口
5. **迪米特法则**：最小化类之间的依赖关系

### 🔒 安全开发规范

#### 认证与授权
```java
/**
 * JWT 认证过滤器
 * 
 * 安全特性：
 * - 无状态认证，支持分布式部署
 * - Token 黑名单机制，支持主动失效
 * - 自动 Token 刷新，提升用户体验
 * - 详细的认证日志，便于安全审计
 */
@Component
public class JwtRequestFilter extends OncePerRequestFilter {
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                  HttpServletResponse response, 
                                  FilterChain chain) throws ServletException, IOException {
        
        final String requestTokenHeader = request.getHeader("Authorization");
        
        if (requestTokenHeader != null && requestTokenHeader.startsWith("Bearer ")) {
            String jwtToken = requestTokenHeader.substring(7);
            
            try {
                // 检查 Token 是否在黑名单中
                if (tokenBlacklistService.isBlacklisted(jwtToken)) {
                    logger.warn("Token is blacklisted: {}", maskToken(jwtToken));
                    throw new JwtTokenBlacklistedException("Token has been revoked");
                }
                
                // 验证 Token 并设置用户认证信息
                if (jwtUtil.validateToken(jwtToken)) {
                    String username = jwtUtil.getUsernameFromToken(jwtToken);
                    UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);
                    
                    UsernamePasswordAuthenticationToken authentication = 
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    logger.debug("User authenticated successfully: {}", username);
                }
                
            } catch (JwtException e) {
                logger.error("JWT token validation failed: {}", e.getMessage());
                throw new JwtAuthenticationException("Invalid JWT token", e);
            }
        }
        
        chain.doFilter(request, response);
    }
}
```

#### 限流保护实现
```java
/**
 * 智能限流切面
 * 
 * 限流策略：
 * - IP 限流：防止暴力攻击
 * - 邮箱限流：防止邮箱枚举
 * - Token 限流：防止 Token 滥用
 * - 用户限流：防止恶意操作
 * 
 * 技术实现：
 * - 基于 AOP 切面编程，对业务无侵入
 * - Redis 分布式存储，支持集群部署
 * - 滑动窗口算法，精确控制频率
 * - 故障容错机制，确保服务可用性
 */
@Aspect
@Component
public class RateLimitAspect {
    
    @Around("@annotation(rateLimit)")
    public Object checkRateLimit(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        
        HttpServletRequest request = getCurrentRequest();
        
        // 检查每种限流类型
        for (RateLimitType type : rateLimit.types()) {
            String identifier = extractIdentifier(type, request, joinPoint);
            
            if (!rateLimitService.isAllowed(
                type.name().toLowerCase() + ":" + identifier,
                rateLimit.maxRequests(),
                rateLimit.timeWindowSeconds()
            )) {
                String maskedIdentifier = maskIdentifier(identifier);
                logger.warn("Rate limit exceeded for type: {}, identifier: {}", type, maskedIdentifier);
                
                throw new RateLimitExceededException(
                    "Requests are too frequent, please try again later"
                );
            }
        }
        
        return joinPoint.proceed();
    }
}
```

### 📊 监控与日志规范

#### 结构化日志标准
```java
/**
 * 统一日志记录规范
 * 
 * 日志级别使用原则：
 * - ERROR：系统错误、异常情况（需要立即关注）
 * - WARN ：警告信息、业务异常（需要关注但不影响系统运行）
 * - INFO : 重要业务操作、系统状态（常规业务跟踪）
 * - DEBUG：调试信息、详细执行过程（开发调试使用）
 * - TRACE: 最详细的跟踪信息（性能分析使用）
 */

// 业务操作日志示例
logger.info("User login successful, username: {}, ip: {}", username, clientIp);

// 安全事件日志示例
logger.warn("Failed login attempt, username: {}, ip: {}, reason: {}", username, clientIp, reason);

// 系统错误日志示例
logger.error("Database connection failed, error: {}", e.getMessage(), e);

// 性能监控日志示例
logger.debug("API request completed, endpoint: {}, duration: {}ms", endpoint, duration);
```

#### 监控指标实现
```java
/**
 * 自定义监控指标
 * 
 * 监控维度：
 * - 业务指标：用户注册数、登录成功率、API 调用量
 * - 技术指标：响应时间、错误率、资源使用率
 * - 安全指标：限流触发次数、异常登录尝试
 */
@Component
public class BusinessMetrics {
    
    private final Counter userRegistrationCounter = Counter.builder("user_registrations_total")
        .description("Total number of user registrations")
        .register(Metrics.globalRegistry);
    
    private final Counter loginAttemptsCounter = Counter.builder("login_attempts_total")
        .description("Total number of login attempts")
        .tag("result", "unknown")
        .register(Metrics.globalRegistry);
    
    private final Timer apiResponseTimer = Timer.builder("api_response_time")
        .description("API response time in seconds")
        .tag("endpoint", "unknown")
        .register(Metrics.globalRegistry);
    
    /**
     * 记录用户注册指标
     */
    public void recordUserRegistration() {
        userRegistrationCounter.increment();
        logger.info("User registration metric recorded");
    }
    
    /**
     * 记录登录尝试指标
     */
    public void recordLoginAttempt(String result) {
        loginAttemptsCounter.tags("result", result).increment();
        logger.debug("Login attempt metric recorded, result: {}", result);
    }
    
    /**
     * 记录 API 响应时间
     */
    public void recordApiResponseTime(String endpoint, Duration duration) {
        apiResponseTimer.tags("endpoint", endpoint)
            .record(duration);
        logger.debug("API response time metric recorded, endpoint: {}, duration: {}ms", endpoint, duration.toMillis());
    }
}
```

### 🧪 测试开发规范

#### 测试策略
```java
/**
 * 单元测试规范
 * 
 * 测试原则：
 * - 快速执行：单元测试应该在几秒内完成
 * - 独立运行：不依赖外部系统和服务
 * - 可重复性：多次运行结果一致
 * - 可读性：测试用例名称清晰，断言明确
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private PasswordEncoder passwordEncoder;
    
    @InjectMocks
    private AuthService authService;
    
    @Test
    @DisplayName("应该成功注册新用户")
    void shouldRegisterNewUserSuccessfully() {
        // Given - 准备测试数据
        SignUpRequest request = SignUpRequest.builder()
            .userName("testuser")
            .password("SecurePassword123!")
            .email("test@example.com")
            .build();
        
        when(userRepository.findByNameCaseSensitive("testuser"))
            .thenReturn(Optional.empty());
        when(passwordEncoder.encode("SecurePassword123!"))
            .thenReturn("encoded_password");
        
        // When - 执行被测试的操作
        boolean result = authService.signUp(request);
        
        // Then - 验证结果
        assertThat(result).isTrue();
        verify(userRepository).save(any(UserEntity.class));
        verify(passwordEncoder).encode("SecurePassword123!");
    }
    
    @Test
    @DisplayName("当用户名已存在时应该注册失败")
    void shouldFailRegistrationWhenUsernameExists() {
        // Given
        SignUpRequest request = SignUpRequest.builder()
            .userName("existinguser")
            .password("SecurePassword123!")
            .build();
        
        UserEntity existingUser = new UserEntity();
        when(userRepository.findByNameCaseSensitive("existinguser"))
            .thenReturn(Optional.of(existingUser));
        
        // When
        boolean result = authService.signUp(request);
        
        // Then
        assertThat(result).isFalse();
        verify(userRepository, never()).save(any());
    }
}
```

### 📚 文档编写规范

#### API 文档标准
```java
/**
 * 认证控制器
 * 
 * 提供用户认证相关的 API 接口，包括注册、登录、Token 刷新等功能。
 * 所有接口都支持限流保护，确保系统安全性。
 */
@RestController
@RequestMapping("/v1/auth")
@Tag(name = "认证管理", description = "用户认证相关接口")
public class AuthController {
    
    /**
     * 用户注册
     * 
     * 创建新的用户账户。用户名必须唯一，密码需要满足安全要求。
     * 注册成功后，用户可以使用用户名和密码进行登录。
     * 
     * @param signUpRequest 注册请求信息，包含用户名、密码、邮箱等
     * @return 注册成功返回 HTTP 201，用户名已存在返回 HTTP 400
     * @throws BusinessException 当注册过程中发生业务异常时抛出
     */
    @PostMapping("/signUp")
    @Operation(
        summary = "用户注册", 
        description = "创建新的用户账户。支持用户名唯一性检查、密码加密存储。",
        responses = {
            @ApiResponse(responseCode = "201", description = "注册成功"),
            @ApiResponse(responseCode = "400", description = "用户名已存在"),
            @ApiResponse(responseCode = "429", description = "请求过于频繁，触发限流")
        }
    )
    @RateLimit(
        types = {RateLimitType.IP},
        maxRequests = 10,
        timeWindowSeconds = 60
    )
    public ResponseEntity<ApiResponse<Object>> signUp(@Valid @RequestBody SignUpRequest signUpRequest) {
        logger.info("Received sign-up request, user: {}", signUpRequest.getUserName());
        
        boolean success = authService.signUp(signUpRequest);
        
        if (success) {
            logger.info("User {} signed up successfully", signUpRequest.getUserName());
            return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(null));
        }
        
        logger.warn("Username {} already exists, sign-up failed", signUpRequest.getUserName());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(Constants.DEFAULT_SERVER_ERROR, "用户名已存在"));
    }
}
```

### 🚀 部署与运维规范

#### Docker 配置标准
```yaml
# Docker Compose 配置示例
version: '3.8'

services:
  app:
    image: portfolio-backend:latest
    container_name: portfolio-app
    restart: unless-stopped
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - DB_HOST=mysql
      - REDIS_HOST=redis
    ports:
      - "8080:8080"
    depends_on:
      - mysql
      - redis
    volumes:
      - ./logs:/app/logs
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 40s
```

#### 生产环境配置
```properties
# 生产环境配置示例
spring.profiles.active=prod

# 数据库配置
spring.datasource.url=jdbc:mysql://db-host:3306/portfolio?useSSL=true&serverTimezone=Asia/Tokyo
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.minimum-idle=5

# Redis 配置
spring.data.redis.host=redis-host
spring.data.redis.port=6379
spring.data.redis.timeout=2000ms
spring.data.redis.lettuce.pool.max-active=8
spring.data.redis.lettuce.pool.max-idle=8

# 日志配置
logging.level.com.listen.portfolio=INFO
logging.level.org.springframework.security=WARN
logging.file.path=/var/log/portfolio/portfolio.log
logging.logback.rollingpolicy.max-file-size=100MB
logging.logback.rollingpolicy.max-history=30

# 监控配置
management.endpoints.web.exposure.include=health,info,prometheus
management.endpoint.health.show-details=when-authorized
management.metrics.export.prometheus.enabled=true
```

## 🔄 开发流程规范

### 功能开发流程
1. **需求分析**：理解业务需求，明确功能边界
2. **设计阶段**：设计 API 接口、数据模型、业务流程
3. **编码实现**：按照编码规范实现功能
4. **单元测试**：编写全面的单元测试
5. **集成测试**：验证与其他模块的集成
6. **文档更新**：更新相关文档和 API 说明
7. **代码审查**：提交代码前进行自我审查

### 代码审查清单
- [ ] 代码注释使用中文，说明清晰
- [ ] 日志输出使用英文，级别正确
- [ ] 异常处理完善，不泄露敏感信息
- [ ] 安全检查：SQL 注入、XSS、CSRF 防护
- [ ] 性能考虑：避免 N+1 查询，合理使用缓存
- [ ] 测试覆盖：核心逻辑有对应测试
- [ ] 文档更新：API 文档、技术文档同步更新

## 📋 技术债务管理

### 代码质量标准
- **测试覆盖率**：不低于 80%
- **代码重复率**：不超过 5%
- **圈复杂度**：单个方法不超过 10
- **技术债务**：及时重构，避免积累

### 重构原则
1. **小步重构**：每次只改变一个小功能
2. **测试保护**：重构前确保有足够测试
3. **向后兼容**：公共接口保持兼容性
4. **文档同步**：重构后及时更新文档

## 🎯 AI 助手使用指南

### 何时调用 AI 助手
- **设计阶段**：架构设计、技术选型咨询
- **编码阶段**：代码实现、问题调试
- **测试阶段**：测试用例设计、覆盖率分析
- **文档阶段**：文档编写、内容优化
- **部署阶段**：配置检查、问题排查

### AI 助手能力范围
- **代码生成**：根据需求生成符合规范的代码
- **问题诊断**：分析日志、定位问题根因
- **性能优化**：提供性能改进建议
- **安全审查**：识别潜在安全风险
- **文档编写**：生成技术文档和使用说明

### 与 AI 协作的最佳实践
1. **明确需求**：提供清晰的功能需求和约束条件
2. **上下文完整**：提供足够的代码上下文和项目信息
3. **迭代优化**：通过多轮对话逐步完善方案
4. **验证结果**：对 AI 生成的内容进行验证和测试
5. **学习改进**：从协作过程中学习最佳实践

---

## 📚 相关文档索引

- [README.md](README.md) - 项目总体介绍
- [docs/development_setup.md](docs/development_setup.md) - 开发环境设置
- [docs/security_features.md](docs/security_features.md) - 安全功能详解
- [docs/rate_limiting.md](docs/rate_limiting.md) - 限流系统指南
- [docs/emaill_setup.md](docs/emaill_setup.md) - 邮件服务配置
- [docs/war_deployment_guide.md](docs/war_deployment_guide.md) - WAR 部署指南
- [docs/test_coverage.md](docs/test_coverage.md) - 测试覆盖率分析

---

**最后更新**: 2026-03-31  
**维护者**: Development Team  
**版本**: 1.0.0  
**适用范围**: ListenPortfolioBackend 项目 AI 辅助开发

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
| 打包方式 | JAR / WAR | 支持 Spring Boot 内嵌服务器和传统服务器部署 |

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

### 5. 打包与部署

本项目支持两种打包方式，适应不同的部署需求：

#### JAR 包（推荐用于开发和小型部署）
```bash
# 构建 JAR 包
./mvnw clean package -DskipTests

# 运行 JAR 包
java -jar target/portfolio-0.0.1-SNAPSHOT.jar
```

#### WAR 包（适用于传统服务器部署）
```bash
# 构建 WAR 包
./mvnw clean package -DskipTests

# 部署到外部 Tomcat
# 将 target/portfolio-0.0.1-SNAPSHOT.war 复制到 Tomcat webapps 目录
```

**WAR 包优势：**
- 兼容传统 Java EE 应用服务器
- 支持多应用共享服务器资源
- 便于企业级环境统一管理
- 支持 JNDI 数据源配置

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
- **智能限流**：基于 AOP 的多维度限流系统
  - 支持限流类型：IP、邮箱、Token、用户、自定义
  - Redis 分布式存储，支持集群部署
  - 防止暴力破解、邮箱枚举、Token 滥用
- **公开路径**：`/v1/auth/**`、`/v1/projects/**`、Actuator 健康/Prometheus、Swagger UI
- **CSRF 禁用**（无状态 JWT API）
- 所有敏感配置通过环境变量注入（`DB_PASSWORD`、`JWT_SECRET` 等）

### 限流系统详解

本项目实现了基于 AOP 的智能限流系统，有效防止各种攻击：

#### 限流维度
- **IP 限流**：防止同一 IP 的暴力攻击
- **邮箱限流**：防止邮箱枚举和垃圾邮件
- **Token 限流**：防止 Token 滥用
- **用户限流**：针对已认证用户的频率控制
- **自定义限流**：支持业务特定的限流规则

#### 限流配置示例
```java
@RateLimit(
    types = {RateLimitType.IP, RateLimitType.EMAIL},
    maxRequests = 10,
    timeWindowSeconds = 60
)
public ResponseEntity<?> signUp(@Valid @RequestBody SignUpRequest request) {
    // 注册逻辑
}
```

#### 限流存储
- 使用 Redis 存储限流计数器
- 支持分布式部署
- 自动过期清理

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

📅 **最后更新**: 2026-03-31 | 🏷️ **版本**: 1.0.0 | 👤 **作者**: Listen — listen2code@gmail.com | [GitHub](https://github.com/listen2code)