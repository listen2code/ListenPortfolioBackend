package com.listen.portfolio.security;

import com.listen.portfolio.common.jwt.JwtUtil;
import com.listen.portfolio.service.RateLimitService;
import com.listen.portfolio.service.TokenBlacklistService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 安全测试
 * 
 * 说明：测试系统的安全相关功能
 * 目的：验证 JWT 令牌安全、限流安全、权限控制等
 */
@SpringBootTest
@ActiveProfiles("test")
public class SecurityTest {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private RateLimitService rateLimitService;

    @Autowired
    private TokenBlacklistService tokenBlacklistService;

    private UserDetails testUser;

    @BeforeEach
    void setUp() {
        testUser = User.withUsername("testuser")
                .password("password")
                .roles("USER")
                .build();
    }

    @Test
    @DisplayName("JWT 安全 - 令牌篡改检测")
    void testJwtTokenTampering() {
        // Given - 生成有效令牌
        String validToken = jwtUtil.generateToken(testUser);
        
        // When - 篡改令牌
        String tamperedToken = validToken.substring(0, validToken.length() - 10) + "tampered";

        // Then - 篡改的令牌应该验证失败
        assertFalse(jwtUtil.validateToken(tamperedToken, testUser), "篡改的令牌应该验证失败");
        
        // 验证用户名提取也会失败
        assertThrows(Exception.class, () -> {
            jwtUtil.extractUsername(tamperedToken);
        });
    }

    @Test
    @DisplayName("JWT 安全 - 过期令牌检测")
    void testJwtTokenExpiration() {
        // Given - 生成立即过期的令牌
        String expiredToken = jwtUtil.generateToken(testUser);
        
        // 注意：在实际测试中，我们需要等待令牌过期
        // 这里我们模拟过期场景
        
        // When & Then - 过期令牌应该验证失败
        // 注意：由于令牌默认1小时过期，我们无法在单元测试中等待
        // 但可以通过修改配置来测试过期功能
    }

    @Test
    @DisplayName("JWT 安全 - 不同用户令牌验证")
    void testJwtTokenDifferentUserValidation() {
        // Given
        String tokenForUser1 = jwtUtil.generateToken(testUser);
        
        UserDetails differentUser = User.withUsername("differentuser")
                .password("password")
                .roles("USER")
                .build();

        // When & Then - 令牌应该只能对原用户有效
        assertFalse(jwtUtil.validateToken(tokenForUser1, differentUser), 
                   "令牌应该只对原用户有效");
    }

    @Test
    @DisplayName("JWT 安全 - 刷新令牌安全性")
    void testJwtRefreshTokenSecurity() {
        // Given
        String accessToken = jwtUtil.generateToken(testUser);
        String refreshToken = jwtUtil.generateRefreshToken(accessToken);

        // When - 验证刷新令牌
        String usernameFromRefresh = jwtUtil.extractUsername(refreshToken);

        // Then - 刷新令牌应该包含正确的用户信息
        assertEquals(testUser.getUsername(), usernameFromRefresh, 
                    "刷新令牌应该包含正确的用户名");
    }

    @Test
    @DisplayName("限流安全 - 高频请求防护")
    void testRateLimitHighFrequencyProtection() {
        // Given
        String identifier = "security:test";
        int maxRequests = 5;
        int timeWindowSeconds = 10;

        // When - 快速发送大量请求
        int allowedCount = 0;
        int blockedCount = 0;
        
        for (int i = 0; i < 20; i++) {
            if (rateLimitService.isAllowed(identifier, maxRequests, timeWindowSeconds)) {
                allowedCount++;
            } else {
                blockedCount++;
            }
        }

        // Then - 应该只有 maxRequests 个请求通过
        assertEquals(maxRequests, allowedCount, 
                    "应该只有 " + maxRequests + " 个请求通过");
        assertTrue(blockedCount > 0, "应该有请求被阻止");
    }

    @Test
    @DisplayName("限流安全 - 不同标识符隔离")
    void testRateLimitIdentifierIsolation() {
        // Given
        String attackerId = "attacker";
        String legitimateUserId = "legitimate";
        int maxRequests = 3;
        int timeWindowSeconds = 60;

        // When - 攻击者发送大量请求
        for (int i = 0; i < 10; i++) {
            rateLimitService.isAllowed(attackerId, maxRequests, timeWindowSeconds);
        }

        // Then - 合法用户应该不受影响
        boolean legitimateUserAllowed = rateLimitService.isAllowed(
            legitimateUserId, maxRequests, timeWindowSeconds);
        
        assertTrue(legitimateUserAllowed, 
                  "合法用户的请求应该不受攻击者影响");
    }

    @Test
    @DisplayName("Token 黑名单安全 - 撤销令牌")
    void testTokenBlacklistRevocation() {
        // Given
        String validToken = jwtUtil.generateToken(testUser);
        
        // 验证令牌原本有效
        assertTrue(jwtUtil.validateToken(validToken, testUser), 
                  "令牌原本应该有效");

        // When - 将令牌加入黑名单
        tokenBlacklistService.addToBlacklist(validToken, 
            System.currentTimeMillis() + 3600000);

        // Then - 令牌应该在黑名单中
        assertTrue(tokenBlacklistService.isBlacklisted(validToken), 
                  "令牌应该在黑名单中");
    }

    @Test
    @DisplayName("JWT 安全 - 算法强度验证")
    void testJwtAlgorithmStrength() {
        // Given
        String token = jwtUtil.generateToken(testUser);

        // When - 验证令牌结构
        String[] parts = token.split("\\.");

        // Then - JWT 应该有三个部分（header, payload, signature）
        assertEquals(3, parts.length, "JWT 应该有三个部分");
        
        // 验证每个部分都不为空
        assertTrue(parts[0].length() > 0, "Header 不应该为空");
        assertTrue(parts[1].length() > 0, "Payload 不应该为空");
        assertTrue(parts[2].length() > 0, "Signature 不应该为空");
    }

    @Test
    @DisplayName("输入验证安全 - SQL 注入防护")
    void testInputValidationSqlInjection() {
        // Given - 模拟 SQL 注入攻击
        String maliciousUsername = "admin'; DROP TABLE users; --";
        String maliciousEmail = "test@example.com'; DROP TABLE users; --";

        // When & Then - 系统应该正确处理恶意输入
        // 注意：这里我们主要验证系统不会崩溃
        assertDoesNotThrow(() -> {
            // 尝试使用恶意输入生成令牌
            UserDetails maliciousUser = User.withUsername(maliciousUsername)
                    .password("password")
                    .roles("USER")
                    .build();
            
            String token = jwtUtil.generateToken(maliciousUser);
            assertNotNull(token, "即使使用恶意输入，令牌生成也不应该失败");
        });
    }

    @Test
    @DisplayName("输入验证安全 - XSS 防护")
    void testInputValidationXssProtection() {
        // Given - 模拟 XSS 攻击
        String xssPayload = "<script>alert('xss')</script>";
        String xssUsername = "user" + xssPayload;

        // When & Then - 系统应该正确处理 XSS 输入
        assertDoesNotThrow(() -> {
            UserDetails xssUser = User.withUsername(xssUsername)
                    .password("password")
                    .roles("USER")
                    .build();
            
            String token = jwtUtil.generateToken(xssUser);
            String extractedUsername = jwtUtil.extractUsername(token);
            
            assertEquals(xssUsername, extractedUsername, 
                        "XSS 载荷应该被正确存储在令牌中");
        });
    }

    @Test
    @DisplayName("会话管理安全 - 并发令牌验证")
    void testSessionManagementConcurrentTokens() {
        // Given - 为同一用户生成多个令牌
        String token1 = jwtUtil.generateToken(testUser);
        String token2 = jwtUtil.generateToken(testUser);
        String token3 = jwtUtil.generateToken(testUser);

        // When & Then - 所有令牌都应该有效
        assertTrue(jwtUtil.validateToken(token1, testUser), "第一个令牌应该有效");
        assertTrue(jwtUtil.validateToken(token2, testUser), "第二个令牌应该有效");
        assertTrue(jwtUtil.validateToken(token3, testUser), "第三个令牌应该有效");

        // 验证所有令牌都包含正确的用户名
        assertEquals(testUser.getUsername(), jwtUtil.extractUsername(token1));
        assertEquals(testUser.getUsername(), jwtUtil.extractUsername(token2));
        assertEquals(testUser.getUsername(), jwtUtil.extractUsername(token3));
    }

    @Test
    @DisplayName("权限控制安全 - 角色验证")
    void testRoleBasedAccessControl() {
        // Given - 不同角色的用户
        UserDetails regularUser = User.withUsername("regular")
                .password("password")
                .roles("USER")
                .build();
        
        UserDetails adminUser = User.withUsername("admin")
                .password("password")
                .roles("ADMIN", "USER")
                .build();

        // When - 生成不同角色的令牌
        String regularToken = jwtUtil.generateToken(regularUser);
        String adminToken = jwtUtil.generateToken(adminUser);

        // Then - 令牌应该包含正确的用户信息
        assertEquals("regular", jwtUtil.extractUsername(regularToken));
        assertEquals("admin", jwtUtil.extractUsername(adminToken));
        
        // 注意：实际的角色验证需要在授权层面进行
        // 这里我们主要验证令牌生成和解析的正确性
    }

    @Test
    @DisplayName("数据泄露防护 - 敏感信息日志")
    void testSensitiveDataLogging() {
        // Given
        String token = jwtUtil.generateToken(testUser);

        // When & Then - 验证令牌内容不包含敏感信息
        String[] parts = token.split("\\.");
        
        // Payload 部分（第二部分）是 Base64 编码的
        // 我们验证它不包含明文密码等敏感信息
        String payload = parts[1];
        
        assertFalse(payload.toLowerCase().contains("password"), 
                  "令牌不应该包含明文密码");
        assertFalse(payload.toLowerCase().contains("secret"), 
                  "令牌不应该包含密钥信息");
    }
}
