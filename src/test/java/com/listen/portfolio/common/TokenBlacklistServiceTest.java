package com.listen.portfolio.common;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import com.listen.portfolio.service.TokenBlacklistService;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TokenBlacklistService Unit Tests")
class TokenBlacklistServiceTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private TokenBlacklistService tokenBlacklistService;

    private static final String TEST_TOKEN = "test-jwt-token";
    private static final String BLACKLIST_KEY = "token:blacklist:" + TEST_TOKEN;

    @BeforeEach
    void setUp() {
        // 只在需要时设置 mock
    }

    @Test
    @DisplayName("addToBlacklist - 成功添加token到黑名单")
    void testAddToBlacklist_Success() {
        // Given
        long expiration = System.currentTimeMillis() + 3600000; // 1小时后过期
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        // When
        tokenBlacklistService.addToBlacklist(TEST_TOKEN, expiration);

        // Then
        verify(valueOperations).set(eq(BLACKLIST_KEY), eq("blacklisted"), any(Long.class), eq(TimeUnit.MILLISECONDS));
    }

    @Test
    @DisplayName("addToBlacklist - token已过期时设置默认TTL")
    void testAddToBlacklist_ExpiredToken() {
        // Given - token已过期
        long expiration = System.currentTimeMillis() - 1000; // 1秒前过期
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        // When
        tokenBlacklistService.addToBlacklist(TEST_TOKEN, expiration);

        // Then - 应该使用默认TTL（24小时）
        verify(valueOperations).set(eq(BLACKLIST_KEY), eq("blacklisted"), eq(Duration.ofHours(24).toMillis()), eq(TimeUnit.MILLISECONDS));
    }

    @Test
    @DisplayName("addToBlacklist - Redis异常处理")
    void testAddToBlacklist_RedisException() {
        // Given
        long expiration = System.currentTimeMillis() + 3600000;
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        doThrow(new RuntimeException("Redis connection failed")).when(valueOperations)
                .set(anyString(), anyString(), any(Long.class), eq(TimeUnit.MILLISECONDS));

        // When & Then - 不应该抛出异常
        assertDoesNotThrow(() -> tokenBlacklistService.addToBlacklist(TEST_TOKEN, expiration));
    }

    @Test
    @DisplayName("isBlacklisted - token在黑名单中返回true")
    void testIsBlacklisted_TokenInBlacklist() {
        // Given
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(BLACKLIST_KEY)).thenReturn("blacklisted");

        // When
        boolean result = tokenBlacklistService.isBlacklisted(TEST_TOKEN);

        // Then
        assertTrue(result);
        verify(valueOperations).get(BLACKLIST_KEY);
    }

    @Test
    @DisplayName("isBlacklisted - token不在黑名单中返回false")
    void testIsBlacklisted_TokenNotInBlacklist() {
        // Given
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(BLACKLIST_KEY)).thenReturn(null);

        // When
        boolean result = tokenBlacklistService.isBlacklisted(TEST_TOKEN);

        // Then
        assertFalse(result);
        verify(valueOperations).get(BLACKLIST_KEY);
    }

    @Test
    @DisplayName("isBlacklisted - Redis异常处理")
    void testIsBlacklisted_RedisException() {
        // Given
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenThrow(new RuntimeException("Redis connection failed"));

        // When
        boolean result = tokenBlacklistService.isBlacklisted(TEST_TOKEN);

        // Then - 异常情况应该返回false（不在黑名单中）
        assertFalse(result);
    }

    @Test
    @DisplayName("removeFromBlacklist - 成功移除token")
    void testRemoveFromBlacklist_Success() {
        // When
        tokenBlacklistService.removeFromBlacklist(TEST_TOKEN);

        // Then
        verify(redisTemplate).delete(BLACKLIST_KEY);
    }

    @Test
    @DisplayName("removeFromBlacklist - Redis异常处理")
    void testRemoveFromBlacklist_RedisException() {
        // Given
        doThrow(new RuntimeException("Redis connection failed")).when(redisTemplate).delete(anyString());

        // When & Then - 不应该抛出异常
        assertDoesNotThrow(() -> tokenBlacklistService.removeFromBlacklist(TEST_TOKEN));
    }

    @Test
    @DisplayName("calculateTTL - 计算正确的TTL")
    void testCalculateTTL_ValidExpiration() throws Exception {
        // Given
        long expiration = System.currentTimeMillis() + 3600000; // 1小时后过期

        // When - 使用反射调用私有方法
        java.lang.reflect.Method method = TokenBlacklistService.class.getDeclaredMethod("calculateTTL", long.class);
        method.setAccessible(true);
        long actualTTL = (Long) method.invoke(tokenBlacklistService, expiration);

        // Then
        assertTrue(actualTTL > 0);
        assertTrue(actualTTL <= 3600000 + 1000); // 允许1秒误差
    }

    @Test
    @DisplayName("calculateTTL - token已过期返回默认TTL")
    void testCalculateTTL_ExpiredToken() throws Exception {
        // Given
        long expiration = System.currentTimeMillis() - 1000; // 1秒前过期

        // When
        java.lang.reflect.Method method = TokenBlacklistService.class.getDeclaredMethod("calculateTTL", long.class);
        method.setAccessible(true);
        long actualTTL = (Long) method.invoke(tokenBlacklistService, expiration);

        // Then
        assertEquals(Duration.ofHours(24).toMillis(), actualTTL);
    }

    @Test
    @DisplayName("边界测试 - 空token处理")
    void testEdgeCases_NullToken() {
        // When & Then - 空token不应该导致异常
        assertDoesNotThrow(() -> tokenBlacklistService.addToBlacklist(null, System.currentTimeMillis() + 3600000));
        assertDoesNotThrow(() -> tokenBlacklistService.isBlacklisted(null));
        assertDoesNotThrow(() -> tokenBlacklistService.removeFromBlacklist(null));
    }

    @Test
    @DisplayName("边界测试 - 空字符串token处理")
    void testEdgeCases_EmptyToken() {
        // When & Then - 空字符串token不应该导致异常
        assertDoesNotThrow(() -> tokenBlacklistService.addToBlacklist("", System.currentTimeMillis() + 3600000));
        assertDoesNotThrow(() -> tokenBlacklistService.isBlacklisted(""));
        assertDoesNotThrow(() -> tokenBlacklistService.removeFromBlacklist(""));
    }

    @Test
    @DisplayName("集成测试 - 完整的黑名单流程")
    void testIntegration_CompleteBlacklistFlow() {
        // Given
        long expiration = System.currentTimeMillis() + 3600000;
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(BLACKLIST_KEY)).thenReturn("blacklisted", null);

        // When - 添加到黑名单
        tokenBlacklistService.addToBlacklist(TEST_TOKEN, expiration);

        // Then - 应该在黑名单中
        assertTrue(tokenBlacklistService.isBlacklisted(TEST_TOKEN));

        // When - 从黑名单移除
        tokenBlacklistService.removeFromBlacklist(TEST_TOKEN);

        // Then - 应该不在黑名单中
        assertFalse(tokenBlacklistService.isBlacklisted(TEST_TOKEN));
    }

    @Test
    @DisplayName("性能测试 - 大量token处理")
    void testPerformance_BulkTokenProcessing() {
        // Given
        String[] tokens = new String[100];
        for (int i = 0; i < 100; i++) {
            tokens[i] = "token-" + i;
        }

        // When & Then - 大量操作不应该导致性能问题或异常
        long startTime = System.currentTimeMillis();
        for (String token : tokens) {
            assertDoesNotThrow(() -> tokenBlacklistService.addToBlacklist(token, System.currentTimeMillis() + 3600000));
            assertDoesNotThrow(() -> tokenBlacklistService.isBlacklisted(token));
        }
        long endTime = System.currentTimeMillis();

        // Then - 操作应该在合理时间内完成（小于1秒）
        assertTrue(endTime - startTime < 1000, "Bulk operations should complete within 1 second");
    }
}
