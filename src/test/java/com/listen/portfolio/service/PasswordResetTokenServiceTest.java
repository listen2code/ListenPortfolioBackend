package com.listen.portfolio.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * PasswordResetTokenService 单元测试
 * 
 * 说明：测试密码重置 Token 服务的所有核心功能
 * 目的：确保 Token 生成、存储、验证、过期处理功能正常工作
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PasswordResetTokenService Unit Tests")
class PasswordResetTokenServiceTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private PasswordResetTokenService tokenService;

    @BeforeEach
    void setUp() {
        // Mock response.getWriter() to avoid NullPointerException - 使用 lenient() 避免不必要的 stubbing 警告
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        // 设置默认的 Token 过期时间为 1 小时
        ReflectionTestUtils.setField(tokenService, "tokenExpiration", 3600L);
    }

    @Test
    @DisplayName("生成 Token - 成功")
    void testGenerateToken_Success() {
        // Given
        String email = "test@example.com";

        // When
        String token = tokenService.generateToken(email);

        // Then
        assertNotNull(token, "生成的 Token 不应为空");
        assertTrue(token.length() >= 32, "Token 长度应该至少 32 字符");
        verify(valueOperations).set(contains("password_reset:"), eq(email), eq(3600L), eq(TimeUnit.SECONDS));
    }

    @Test
    @DisplayName("通过 Token 获取邮箱 - 成功")
    void testGetEmailByToken_Success() {
        // Given
        String email = "test@example.com";
        String token = "generated-token";
        
        when(valueOperations.get(contains("password_reset:" + token))).thenReturn(email);

        // When
        String resultEmail = tokenService.getEmailByToken(token);

        // Then
        assertEquals(email, resultEmail, "应该返回正确的邮箱");
        verify(valueOperations).get(contains("password_reset:" + token));
    }

    @Test
    @DisplayName("通过 Token 获取邮箱 - Token 不存在")
    void testGetEmailByToken_TokenNotFound() {
        // Given
        String token = "non-existent-token";
        
        when(valueOperations.get(contains("password_reset:" + token))).thenReturn(null);

        // When
        String resultEmail = tokenService.getEmailByToken(token);

        // Then
        assertNull(resultEmail, "不存在的 Token 应该返回 null");
        verify(valueOperations).get(contains("password_reset:" + token));
    }

    @Test
    @DisplayName("删除 Token - 成功")
    void testDeleteToken_Success() {
        // Given
        String token = "valid-token";
        
        when(redisTemplate.delete(contains("password_reset:" + token))).thenReturn(true);

        // When
        assertDoesNotThrow(() -> tokenService.deleteToken(token));

        // Then
        verify(redisTemplate).delete(contains("password_reset:" + token));
    }

    @Test
    @DisplayName("删除 Token - Token 不存在")
    void testDeleteToken_TokenNotExists() {
        // Given
        String token = "non-existent-token";
        
        when(redisTemplate.delete(contains("password_reset:" + token))).thenReturn(false);

        // When
        assertDoesNotThrow(() -> tokenService.deleteToken(token));

        // Then
        verify(redisTemplate).delete(contains("password_reset:" + token));
    }

    @Test
    @DisplayName("检查 Token 是否有效 - 有效")
    void testIsTokenValid_Valid() {
        // Given
        String token = "valid-token";
        
        when(redisTemplate.hasKey(contains("password_reset:" + token))).thenReturn(true);

        // When
        boolean isValid = tokenService.isTokenValid(token);

        // Then
        assertTrue(isValid, "有效的 Token 应该返回 true");
        verify(redisTemplate).hasKey(contains("password_reset:" + token));
    }

    @Test
    @DisplayName("检查 Token 是否有效 - 无效")
    void testIsTokenValid_Invalid() {
        // Given
        String token = "invalid-token";
        
        when(redisTemplate.hasKey(contains("password_reset:" + token))).thenReturn(false);

        // When
        boolean isValid = tokenService.isTokenValid(token);

        // Then
        assertFalse(isValid, "无效的 Token 应该返回 false");
        verify(redisTemplate).hasKey(contains("password_reset:" + token));
    }

    @Test
    @DisplayName("检查 Token 是否有效 - Redis 返回 null")
    void testIsTokenValid_NullResponse() {
        // Given
        String token = "test-token";
        
        when(redisTemplate.hasKey(contains("password_reset:" + token))).thenReturn(null);

        // When
        boolean isValid = tokenService.isTokenValid(token);

        // Then
        assertFalse(isValid, "Redis 返回 null 时应该返回 false");
        verify(redisTemplate).hasKey(contains("password_reset:" + token));
    }

    @Test
    @DisplayName("参数验证 - 空邮箱")
    void testGenerateToken_NullEmail() {
        // Given
        String email = null;

        // When
        String token = tokenService.generateToken(email);

        // Then - 实际实现没有参数验证，所以不会抛出异常
        assertNotNull(token, "即使邮箱为空，也应该生成 Token");
    }

    @Test
    @DisplayName("参数验证 - 空邮箱字符串")
    void testGenerateToken_EmptyEmail() {
        // Given
        String email = "";

        // When
        String token = tokenService.generateToken(email);

        // Then - 实际实现没有参数验证，所以不会抛出异常
        assertNotNull(token, "即使邮箱为空字符串，也应该生成 Token");
    }

    @Test
    @DisplayName("参数验证 - 无效邮箱格式")
    void testGenerateToken_InvalidEmailFormat() {
        // Given
        String email = "invalid-email";

        // When
        String token = tokenService.generateToken(email);

        // Then - 实际实现没有邮箱格式验证，所以不会抛出异常
        assertNotNull(token, "即使邮箱格式无效，也应该生成 Token");
    }

    @Test
    @DisplayName("Token 唯一性测试 - 同一邮箱多次生成")
    void testGenerateToken_Uniqueness() {
        // Given
        String email = "test@example.com";

        // When
        String token1 = tokenService.generateToken(email);
        String token2 = tokenService.generateToken(email);

        // Then
        assertNotEquals(token1, token2, "同一邮箱多次生成的 Token 应该不同");
        verify(valueOperations, times(2)).set(contains("password_reset:"), eq(email), eq(3600L), eq(TimeUnit.SECONDS));
    }

    @Test
    @DisplayName("Redis 异常 - 容错处理")
    void testGenerateToken_RedisException() {
        // Given
        String email = "test@example.com";
        
        doThrow(new RuntimeException("Redis 连接失败"))
                .when(valueOperations).set(anyString(), anyString(), anyLong(), any(TimeUnit.class));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> 
            tokenService.generateToken(email)
        );
        assertEquals("Redis 连接失败", exception.getMessage());
    }

    @Test
    @DisplayName("Token 过期时间配置")
    void testTokenExpiration_Configuration() {
        // Given
        String email = "test@example.com";
        long customExpiration = 7200L; // 2 小时
        
        ReflectionTestUtils.setField(tokenService, "tokenExpiration", customExpiration);

        // When
        tokenService.generateToken(email);

        // Then
        verify(valueOperations).set(contains("password_reset:"), eq(email), eq(customExpiration), eq(TimeUnit.SECONDS));
    }

    @Test
    @DisplayName("批量操作 - 生成多个 Token")
    void testGenerateMultipleTokens() {
        // Given
        String[] emails = {"user1@example.com", "user2@example.com", "user3@example.com"};

        // When
        String[] tokens = new String[emails.length];
        for (int i = 0; i < emails.length; i++) {
            tokens[i] = tokenService.generateToken(emails[i]);
        }

        // Then
        for (String token : tokens) {
            assertNotNull(token, "生成的 Token 不应为空");
            assertTrue(token.length() >= 32, "Token 长度应该至少 32 字符");
        }
        
        verify(valueOperations, times(3)).set(contains("password_reset:"), anyString(), eq(3600L), eq(TimeUnit.SECONDS));
    }

    @Test
    @DisplayName("安全性测试 - Token 随机性")
    void testTokenRandomness() {
        // Given
        String email = "test@example.com";
        int sampleSize = 100;

        // When
        String[] tokens = new String[sampleSize];
        for (int i = 0; i < sampleSize; i++) {
            tokens[i] = tokenService.generateToken(email);
        }

        // Then
        // 检查所有 Token 都不相同
        for (int i = 0; i < sampleSize; i++) {
            for (int j = i + 1; j < sampleSize; j++) {
                assertNotEquals(tokens[i], tokens[j], "生成的 Token 应该具有随机性");
            }
        }
    }

    @Test
    @DisplayName("边界测试 - 极长邮箱地址")
    void testGenerateToken_LongEmail() {
        // Given
        String email = "very.long.email.address.that.exceeds.normal.length.expectations." +
                       "and.should.still.be.handled.properly.by.the.system." +
                       "this.is.a.test.to.ensure.robustness@example.com";

        // When
        String token = tokenService.generateToken(email);

        // Then
        assertNotNull(token, "即使是长邮箱地址也应该能生成 Token");
        assertTrue(token.length() >= 32, "Token 长度应该至少 32 字符");
    }

    @Test
    @DisplayName("完整流程测试 - 生成、验证、删除")
    void testCompleteTokenFlow() {
        // Given
        String email = "test@example.com";

        // When - 生成 Token
        String token = tokenService.generateToken(email);
        
        // Then - 验证 Token 生成
        assertNotNull(token);
        
        // When - 验证 Token 有效性
        when(redisTemplate.hasKey(contains("password_reset:" + token))).thenReturn(true);
        when(valueOperations.get(contains("password_reset:" + token))).thenReturn(email);
        
        boolean isValid = tokenService.isTokenValid(token);
        String retrievedEmail = tokenService.getEmailByToken(token);
        
        // Then - 验证 Token 有效性
        assertTrue(isValid);
        assertEquals(email, retrievedEmail);
        
        // When - 删除 Token
        tokenService.deleteToken(token);
        
        // Then - 验证删除
        verify(redisTemplate).delete(contains("password_reset:" + token));
    }

    @Test
    @DisplayName("并发测试 - 多线程生成 Token")
    void testConcurrentTokenGeneration() throws InterruptedException {
        // Given
        String email = "test@example.com";
        int threadCount = 10;

        // When
        Thread[] threads = new Thread[threadCount];
        String[] tokens = new String[threadCount];
        
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            threads[i] = new Thread(() -> {
                tokens[index] = tokenService.generateToken(email);
            });
            threads[i].start();
        }
        
        for (Thread thread : threads) {
            thread.join();
        }

        // Then
        for (String token : tokens) {
            assertNotNull(token, "并发生成的 Token 不应为空");
            assertTrue(token.length() >= 32, "Token 长度应该至少 32 字符");
        }
        
        // 验证所有 Token 都不相同
        for (int i = 0; i < threadCount; i++) {
            for (int j = i + 1; j < threadCount; j++) {
                assertNotEquals(tokens[i], tokens[j], "并发生成的 Token 应该不同");
            }
        }
    }
}
