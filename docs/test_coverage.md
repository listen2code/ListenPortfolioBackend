# 测试覆盖率与质量保障

**Status**: `Implemented Locally, Metrics Must Be Regenerated`

> 本文档中的测试文件统计以当前 `src/test/java` 为准。
> 覆盖率数值请以执行 `./mvnw clean test jacoco:report` 或 `check-coverage.bat` 后生成的 JaCoCo 报告为准。
> 文中的代码片段主要用于说明测试模式，未必与仓库中的实际测试类一一对应。

## 概述

本项目包含单元测试、集成测试、安全测试和性能测试。仓库当前通过 JaCoCo Maven 插件配置了覆盖率检查，其中可直接从 `pom.xml` 验证的质量门禁是 **指令覆盖率最低 80%**；实际覆盖率结果需要在本地重新生成报告确认。

## 测试现状

### 测试统计

| 测试类型 | 文件数量 | 说明 |
|----------|----------|------|
| **API 层测试** | 10 个 | Controller + Service（auth/user/projects/about） |
| **通用组件测试** | 6 个 | ApiResponse / ErrorCode / Redis / TokenBlacklist 等 |
| **JWT 测试** | 2 个 | JwtUtil + JwtRequestFilter |
| **服务层测试** | 4 个 | AboutMeService / EmailService / PasswordResetToken / RateLimit |
| **集成测试** | 2 个 | BaseIntegrationTest / RedisIntegrationTestLocal |
| **安全 / 性能测试** | 2 个 | SecurityTest / PerformanceTest |
| **配置测试** | 1 个 | GlobalExceptionHandler |
| **总计** | **27 个** | — |

> 注意：覆盖率数据请以 `./mvnw clean test jacoco:report` 生成的 JaCoCo 报告为准。

### 测试分布

```
src/test/java/com/listen/portfolio/
├── api/v1/                           # API 层测试
│   ├── auth/
│   │   ├── AuthControllerTest.java
│   │   └── AuthServiceTest.java
│   ├── projects/
│   │   ├── ProjectControllerTest.java
│   │   └── ProjectServiceTest.java
│   ├── user/
│   │   ├── UserControllerTest.java
│   │   ├── UserRepositoryTest.java
│   │   ├── UserServiceTest.java
│   │   ├── LogoutIntegrationTest.java
│   │   └── LogoutTokenExtractionTest.java
│   └── about/
│       └── AboutMeControllerTest.java
├── aspect/                           # 切面测试
│   └── RateLimitAspectTest.java
├── common/                           # 通用组件测试
│   ├── ApiResponseTest.java
│   ├── BusinessExceptionTest.java
│   ├── ErrorCodeTest.java
│   ├── PasswordGenerator.java
│   ├── RedisConnectionTest.java
│   └── TokenBlacklistServiceTest.java
├── config/                           # 配置测试
│   └── GlobalExceptionHandlerTest.java
├── integration/                      # 集成测试
│   ├── BaseIntegrationTest.java
│   └── RedisIntegrationTestLocal.java
├── jwt/                              # JWT 测试
│   ├── JwtRequestFilterTest.java
│   └── JwtUtilTest.java
├── performance/                      # 性能测试
│   └── PerformanceTest.java
├── security/                         # 安全测试
│   └── SecurityTest.java
└── service/                          # 服务层测试
    ├── AboutMeServiceTest.java
    ├── EmailServiceTest.java
    ├── PasswordResetTokenServiceTest.java
    └── RateLimitServiceTest.java
```

## 测试框架与工具

### 核心测试框架

```xml
<!-- JUnit 5 - 测试框架 -->
<dependency>
    <groupId>org.junit.jupiter</groupId>
    <artifactId>junit-jupiter</artifactId>
    <scope>test</scope>
</dependency>

<!-- Mockito - Mock 框架 -->
<dependency>
    <groupId>org.mockito</groupId>
    <artifactId>mockito-core</artifactId>
    <scope>test</scope>
</dependency>

<!-- Spring Test - 集成测试支持 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>

<!-- Testcontainers - 容器化测试 -->
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>junit-jupiter</artifactId>
    <scope>test</scope>
</dependency>
```

### 测试工具链

- **JaCoCo**：代码覆盖率分析
- **SpotBugs**：静态代码分析
- **TestContainers**：容器化集成测试

## 测试策略

### 1. 单元测试

#### 测试范围
- **Service 层**：业务逻辑测试
- **Controller 层**：API 接口测试
- **Utility 类**：工具方法测试
- **JWT 组件**：令牌生成和验证测试

#### 测试示例

```java
@ExtendWith(MockitoExtension.class)
class JwtUtilTest {
    
    @Mock
    private Clock clock;
    
    @InjectMocks
    private JwtUtil jwtUtil;
    
    @Test
    @DisplayName("应该成功生成 JWT 令牌")
    void shouldGenerateTokenSuccessfully() {
        // Given
        UserDetails userDetails = User.builder()
            .username("testuser")
            .password("password")
            .authorities(List.of())
            .build();
        
        // When
        String token = jwtUtil.generateToken(userDetails);
        
        // Then
        assertThat(token).isNotNull();
        assertThat(token.split("\\.").length).isEqualTo(3);
    }
    
    @Test
    @DisplayName("应该成功验证有效的 JWT 令牌")
    void shouldValidateValidToken() {
        // Given
        UserDetails userDetails = User.builder()
            .username("testuser")
            .password("password")
            .authorities(List.of())
            .build();
        String token = jwtUtil.generateToken(userDetails);
        
        // When
        boolean isValid = jwtUtil.validateToken(token, userDetails);
        
        // Then
        assertThat(isValid).isTrue();
    }
}
```

### 2. 集成测试

#### 测试范围
- **数据库集成**：JPA 实体和 Repository 测试
- **Redis 集成**：缓存和黑名单功能测试
- **测试环境基座**：基础 Spring Boot 集成环境验证
  
> 当前仓库可直接看到的集成测试文件主要是 `BaseIntegrationTest` 与 `RedisIntegrationTestLocal`；下方代码块更适合作为集成测试编写模式示例阅读。

#### 测试示例

```java
@SpringBootTest
@Testcontainers
@Transactional
class AuthServiceIntegrationTest {
    
    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
        .withDatabaseName("test_portfolio")
        .withUsername("test")
        .withPassword("test");
    
    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7.2-alpine")
        .withExposedPorts(6379);
    
    @Autowired
    private AuthService authService;
    
    @Test
    @DisplayName("应该完成完整的用户注册流程")
    void shouldCompleteUserRegistrationFlow() {
        // Given
        SignUpRequest request = SignUpRequest.builder()
            .userName("testuser")
            .password("SecurePassword123!")
            .email("test@example.com")
            .build();
        
        // When
        boolean result = authService.signUp(request);
        
        // Then
        assertThat(result).isTrue();
        
        // 验证用户已保存到数据库
        Optional<UserEntity> user = authService.getUserByName("testuser");
        assertThat(user).isPresent();
        assertThat(user.get().getEmail()).isEqualTo("test@example.com");
    }
}
```

### 3. 安全测试

#### 测试范围
- **认证测试**：登录、注册、Token 验证
- **授权测试**：权限控制和访问限制
- **限流测试**：各种限流场景验证
- **输入验证**：参数校验和 SQL 注入防护

#### 测试示例

```java
@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class SecurityIntegrationTest {
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Test
    @DisplayName("应该正确实施 IP 限流")
    void shouldImplementRateLimitingCorrectly() {
        // Given
        String url = "http://localhost:8080/v1/auth/login";
        LoginRequest request = LoginRequest.builder()
            .userName("testuser")
            .password("wrongpassword")
            .build();
        
        // When - 快速发送多个请求
        List<ResponseEntity<String>> responses = IntStream.range(0, 15)
            .mapToObj(i -> restTemplate.postForEntity(url, request, String.class))
            .collect(Collectors.toList());
        
        // Then - 前10个请求应该正常，后续应该被限流
        long successCount = responses.stream()
            .filter(r -> r.getStatusCode() == HttpStatus.OK)
            .count();
        long rateLimitCount = responses.stream()
            .filter(r -> r.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS)
            .count();
        
        assertThat(successCount).isEqualTo(10);
        assertThat(rateLimitCount).isEqualTo(5);
    }
}
```

### 4. API 测试

#### 测试范围
- **REST API**：所有 API 端点功能测试
- **错误处理**：异常情况和错误响应
- **数据验证**：输入参数校验测试
- **性能测试**：API 响应时间测试

#### 测试示例

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthControllerApiTest {
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Test
    @DisplayName("POST /v1/auth/login - 成功登录应该返回令牌")
    void shouldReturnTokensOnSuccessfulLogin() {
        // Given
        String url = "http://localhost:8080/v1/auth/login";
        LoginRequest request = LoginRequest.builder()
            .userName("testuser")
            .password("correctpassword")
            .build();
        
        // When
        ResponseEntity<ApiResponse> response = restTemplate.postForEntity(url, request, ApiResponse.class);
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getResult()).isEqualTo("0");
        
        // 验证响应结构
        Map<String, Object> body = (Map<String, Object>) response.getBody().getBody();
        assertThat(body).containsKeys("accessToken", "refreshToken", "userId");
    }
}
```

## 覆盖率分析

### JaCoCo 配置

```xml
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.8</version>
    <executions>
        <execution>
            <goals>
                <goal>prepare-agent</goal>
            </goals>
        </execution>
        <execution>
            <id>report</id>
            <phase>test</phase>
            <goals>
                <goal>report</goal>
            </goals>
        </execution>
        <execution>
            <id>check</id>
            <goals>
                <goal>check</goal>
            </goals>
            <configuration>
                <rules>
                    <rule>
                        <element>BUNDLE</element>
                        <limits>
                            <limit>
                                <counter>INSTRUCTION</counter>
                                <value>COVEREDRATIO</value>
                                <minimum>0.80</minimum>
                            </limit>
                        </limits>
                    </rule>
                </rules>
            </configuration>
        </execution>
    </executions>
</plugin>
```

### 覆盖率报告

运行测试并生成覆盖率报告：

```bash
# Windows
check-coverage.bat

# Linux/Mac
./mvnw clean test jacoco:report

# 查看报告
open target/site/jacoco/index.html
```

### 覆盖率目标

| 指标类型 | 仓库可验证目标 | 当前值 | 状态 |
|----------|----------------|--------|------|
| **指令覆盖率** | ≥ 80% | 以最新 JaCoCo 报告为准 | 运行后确认 |

## 测试配置

### 测试配置文件

**application-test.properties**：
```properties
# 测试数据库配置
spring.datasource.url=jdbc:h2:mem:testdb
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=

# JPA 测试配置
spring.jpa.hibernate.ddl-auto=create-drop
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true

# Redis 测试配置（使用嵌入式 Redis）
spring.data.redis.host=localhost
spring.data.redis.port=6370
spring.data.redis.timeout=2000ms

# 邮件测试配置（不实际发送邮件）
spring.mail.host=localhost
spring.mail.port=25
spring.mail.test-connection=false

# JWT 测试配置
jwt.secret=test-secret-key-for-testing-only
jwt.expiration=60000
jwt.refresh-expiration=300000

# 日志测试配置
logging.level.com.listen.portfolio=DEBUG
logging.level.org.springframework.security=DEBUG
```

### 测试工具配置

**TestConfig.java**：
```java
@TestConfiguration
public class TestConfig {
    
    @Bean
    @Primary
    public EmailService mockEmailService() {
        return Mockito.mock(EmailService.class);
    }
    
    @Bean
    @Primary
    public PasswordEncoder testPasswordEncoder() {
        return new PasswordEncoder() {
            @Override
            public String encode(CharSequence rawPassword) {
                return "encoded_" + rawPassword;
            }
            
            @Override
            public boolean matches(CharSequence rawPassword, String encodedPassword) {
                return encode(rawPassword).equals(encodedPassword);
            }
        };
    }
}
```

## 运行测试

### 本地测试

```bash
# 运行所有测试
./mvnw test

# 运行特定测试类
./mvnw test -Dtest=JwtUtilTest

# 运行特定测试方法
./mvnw test -Dtest=JwtUtilTest#shouldGenerateTokenSuccessfully

# 跳过测试快速构建
./mvnw clean package -DskipTests
```

### CI/CD 测试

当前仓库中 **没有可直接引用的 Backend GitHub Actions 工作流文件**。
因此，测试执行与覆盖率生成应以本地命令和 Maven 插件配置为准；若后续新增 CI，请再基于真实工作流补充本节。

## 测试最佳实践

### 1. 测试命名规范

```java
// 推荐的测试命名模式
@Test
@DisplayName("应该 [期望行为] 当 [条件] 时")
void shouldExpectedBehaviorWhenCondition() {
    // 测试实现
}

// 示例
@Test
@DisplayName("应该拒绝无效的 JWT 令牌")
void shouldRejectInvalidJwtToken() {
    // 测试实现
}
```

### 2. 测试结构（AAA 模式）

```java
@Test
@DisplayName("应该成功创建新用户")
void shouldCreateNewUserSuccessfully() {
    // Arrange - 准备测试数据
    SignUpRequest request = SignUpRequest.builder()
        .userName("newuser")
        .password("SecurePassword123!")
        .email("newuser@example.com")
        .build();
    
    // Act - 执行被测试的操作
    boolean result = authService.signUp(request);
    
    // Assert - 验证结果
    assertThat(result).isTrue();
    Optional<UserEntity> user = authService.getUserByName("newuser");
    assertThat(user).isPresent();
    assertThat(user.get().getEmail()).isEqualTo("newuser@example.com");
}
```

### 3. Mock 使用原则

```java
@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    
    @Mock
    private UserRepository userRepository;
    
    @InjectMocks
    private UserService userService;
    
    @Test
    @DisplayName("应该返回存在的用户")
    void shouldReturnExistingUser() {
        // Given
        UserEntity expectedUser = new UserEntity();
        expectedUser.setName("testuser");
        when(userRepository.findByNameCaseSensitive("testuser"))
            .thenReturn(Optional.of(expectedUser));
        
        // When
        Optional<UserEntity> result = userService.getUserByName("testuser");
        
        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("testuser");
        
        // 验证 Mock 调用
        verify(userRepository).findByNameCaseSensitive("testuser");
    }
}
```

### 4. 测试数据管理

```java
@TestMethodOrder(OrderAnnotation.class)
class UserDataTest {
    
    @Test
    @Order(1)
    @DisplayName("应该创建测试用户")
    void shouldCreateTestUser() {
        // 创建测试数据
    }
    
    @Test
    @Order(2)
    @DisplayName("应该找到创建的测试用户")
    void shouldFindCreatedTestUser() {
        // 使用之前创建的测试数据
    }
    
    @AfterEach
    void cleanup() {
        // 清理测试数据
        userRepository.deleteAll();
    }
}
```

## 质量门禁

### 测试质量指标

| 指标 | 阈值 | 当前值 | 状态 |
|------|------|--------|------|
| **指令覆盖率** | ≥ 80% | 以最新 JaCoCo 报告为准 | 运行后确认 |

### 质量门禁配置

```xml
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <configuration>
        <rules>
            <rule>
                <element>BUNDLE</element>
                <limits>
                    <limit>
                        <counter>INSTRUCTION</counter>
                        <value>COVEREDRATIO</value>
                        <minimum>0.80</minimum>
                    </limit>
                </limits>
            </rule>
        </rules>
        <excludes>
            <exclude>**/Application.class</exclude>
            <exclude>**/dto/**</exclude>
            <exclude>**/config/**</exclude>
        </excludes>
    </configuration>
</plugin>
```

## 🎯 测试改进计划

### 短期计划（1-2 周）
- [ ] 添加性能测试用例
- [ ] 增加边界条件测试
- [ ] 完善错误场景测试
- [ ] 优化测试执行速度

### 中期计划（1-2 月）
- [ ] 引入契约测试
- [ ] 添加混沌工程测试
- [ ] 实施测试数据工厂
- [ ] 建立测试数据管理平台

### 长期计划（3-6 月）
- [ ] 自动化测试生成
- [ ] AI 辅助测试用例推荐
- [ ] 测试覆盖率智能分析
- [ ] 测试质量持续改进

---

## 📚 相关文档

- [JUnit 5 用户指南](https://junit.org/junit5/docs/current/user-guide/)
- [Mockito 文档](https://site.mockito.org/)
- [Testcontainers 指南](https://www.testcontainers.org/)
- [JaCoCo 用户指南](https://www.jacoco.org/jacoco/trunk/doc/)

---

**最后更新**: 2026-03-31  
**维护者**: Development Team
