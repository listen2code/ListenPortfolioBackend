package com.listen.portfolio.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JwtUtil 单元测试
 * 
 * 说明：测试 JWT 工具类的所有核心功能
 * 目的：确保 JWT 生成、验证、解析功能正常工作
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("JwtUtil Unit Tests")
class JwtUtilTest {

    @Autowired
    private JwtUtil jwtUtil;

    private UserDetails testUser;

    @BeforeEach
    void setUp() {
        testUser = User.withUsername("testuser")
                .password("password")
                .roles("USER")
                .build();
    }

    @Test
    @DisplayName("generateToken - 成功生成访问令牌")
    void testGenerateToken_Success() {
        String token = jwtUtil.generateToken(testUser);
        
        assertNotNull(token, "生成的令牌不应为空");
        assertTrue(token.length() > 0, "令牌应该有内容");
        assertTrue(token.split("\\.").length == 3, "JWT 应该包含3个部分（header.payload.signature）");
    }

    @Test
    @DisplayName("generateRefreshToken - 成功生成刷新令牌")
    void testGenerateRefreshToken_Success() {
        String accessToken = jwtUtil.generateToken(testUser);
        String refreshToken = jwtUtil.generateRefreshToken(accessToken);
        
        assertNotNull(refreshToken, "生成的刷新令牌不应为空");
        assertTrue(refreshToken.length() > 0, "刷新令牌应该有内容");
    }

    @Test
    @DisplayName("extractUsername - 成功从令牌提取用户名")
    void testExtractUsername_Success() {
        String token = jwtUtil.generateToken(testUser);
        
        String username = jwtUtil.extractUsername(token);
        
        assertEquals("testuser", username, "提取的用户名应该与原始用户名一致");
    }

    @Test
    @DisplayName("extractExpiration - 成功从令牌提取过期时间")
    void testExtractExpiration_Success() {
        String token = jwtUtil.generateToken(testUser);
        
        Date expiration = jwtUtil.extractExpiration(token);
        
        assertNotNull(expiration, "过期时间不应为空");
        assertTrue(expiration.after(new Date()), "过期时间应该在未来");
    }

    @Test
    @DisplayName("validateToken - 有效令牌验证成功")
    void testValidateToken_ValidToken_Success() {
        String token = jwtUtil.generateToken(testUser);
        
        boolean isValid = jwtUtil.validateToken(token, testUser);
        
        assertTrue(isValid, "有效的令牌应该验证通过");
    }

    @Test
    @DisplayName("validateToken - 用户名不匹配验证失败")
    void testValidateToken_UsernameMismatch_Failure() {
        String token = jwtUtil.generateToken(testUser);
        
        UserDetails differentUser = User.withUsername("differentuser")
                .password("password")
                .roles("USER")
                .build();
        
        boolean isValid = jwtUtil.validateToken(token, differentUser);
        
        assertFalse(isValid, "用户名不匹配的令牌应该验证失败");
    }

    @Test
    @DisplayName("validateToken - 过期令牌验证失败")
    void testValidateToken_ExpiredToken_Failure() {
        // 设置一个很短的过期时间（1毫秒）
        ReflectionTestUtils.setField(jwtUtil, "expiration", 1L);
        
        String token = jwtUtil.generateToken(testUser);
        
        // 等待令牌过期
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // 恢复正常的过期时间
        ReflectionTestUtils.setField(jwtUtil, "expiration", 3600000L);
        
        boolean isValid = jwtUtil.validateToken(token, testUser);
        
        assertFalse(isValid, "过期的令牌应该验证失败");
    }

    @Test
    @DisplayName("validateToken - 无效令牌格式验证失败")
    void testValidateToken_InvalidFormat_Failure() {
        String invalidToken = "invalid.token.format";
        
        boolean isValid = jwtUtil.validateToken(invalidToken, testUser);
        
        assertFalse(isValid, "无效格式的令牌应该验证失败");
    }

    @Test
    @DisplayName("extractUsername - 无效令牌抛出异常")
    void testExtractUsername_InvalidToken_ThrowsException() {
        String invalidToken = "invalid.token.format";
        
        assertThrows(MalformedJwtException.class, () -> {
            jwtUtil.extractUsername(invalidToken);
        }, "无效令牌应该抛出 MalformedJwtException");
    }

    @Test
    @DisplayName("isTokenExpired - 未过期令牌返回 false")
    void testIsTokenExpired_NotExpired_ReturnsFalse() {
        String token = jwtUtil.generateToken(testUser);
        
        boolean isExpired = jwtUtil.isTokenExpired(token);
        
        assertFalse(isExpired, "未过期的令牌应该返回 false");
    }

    @Test
    @DisplayName("isTokenExpired - 过期令牌返回 true")
    void testIsTokenExpired_Expired_ReturnsTrue() {
        // 设置一个很短的过期时间
        ReflectionTestUtils.setField(jwtUtil, "expiration", 1L);
        
        String token = jwtUtil.generateToken(testUser);
        
        // 等待令牌过期
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // 恢复正常的过期时间
        ReflectionTestUtils.setField(jwtUtil, "expiration", 3600000L);
        
        boolean isExpired = jwtUtil.isTokenExpired(token);
        
        assertTrue(isExpired, "过期的令牌应该返回 true");
    }




    @Test
    @DisplayName("refreshToken - 成功刷新令牌")
    void testRefreshToken_Success() {
        String originalToken = jwtUtil.generateToken(testUser);
        
        // 等待一小段时间确保新令牌的签发时间不同
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        String refreshedToken = jwtUtil.refreshToken(originalToken);
        
        assertNotNull(refreshedToken, "刷新后的令牌不应为空");
        assertNotEquals(originalToken, refreshedToken, "刷新后的令牌应该与原令牌不同");
        
        // 验证刷新后的令牌仍然有效
        assertTrue(jwtUtil.validateToken(refreshedToken, testUser), "刷新后的令牌应该有效");
    }

    @Test
    @DisplayName("generateToken 和 generateRefreshToken - 刷新令牌有效期更长")
    void testRefreshTokenExpirationLongerThanAccessToken() {
        String accessToken = jwtUtil.generateToken(testUser);
        String refreshToken = jwtUtil.generateRefreshToken(accessToken);
        
        Date accessExpiration = jwtUtil.extractExpiration(accessToken);
        Date refreshExpiration = jwtUtil.extractExpiration(refreshToken);
        
        assertTrue(refreshExpiration.after(accessExpiration), 
                "刷新令牌的过期时间应该晚于访问令牌");
    }
}
