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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RefreshTokenService Unit Tests")
class RefreshTokenServiceTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    private static final String TEST_USERNAME = "testuser";
    private static final String TEST_TOKEN = "refresh-token-123";
    private static final String REDIS_KEY = "token:refresh:testuser:refresh-token-123";
    private static final long DURATION_MS = 604800000L; // 7天

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    @DisplayName("saveRefreshToken - 成功保存 refresh token 到 Redis")
    void testSaveRefreshToken_Success() {
        // When
        refreshTokenService.saveRefreshToken(TEST_USERNAME, TEST_TOKEN, DURATION_MS);

        // Then
        verify(valueOperations).set(eq(REDIS_KEY), eq("active"), eq(DURATION_MS), eq(TimeUnit.MILLISECONDS));
    }

    @Test
    @DisplayName("saveRefreshToken - Redis异常时被捕获")
    void testSaveRefreshToken_Exception() {
        // Given
        doThrow(new RuntimeException("Redis connection error"))
                .when(valueOperations).set(anyString(), anyString(), anyLong(), any(TimeUnit.class));

        // When & Then (should not throw exception)
        assertDoesNotThrow(() -> refreshTokenService.saveRefreshToken(TEST_USERNAME, TEST_TOKEN, DURATION_MS));
    }

    @Test
    @DisplayName("isRefreshTokenValid - Token 有效时返回 true")
    void testIsRefreshTokenValid_True() {
        // Given
        when(redisTemplate.hasKey(REDIS_KEY)).thenReturn(true);

        // When
        boolean isValid = refreshTokenService.isRefreshTokenValid(TEST_USERNAME, TEST_TOKEN);

        // Then
        assertTrue(isValid);
        verify(redisTemplate).hasKey(REDIS_KEY);
    }

    @Test
    @DisplayName("isRefreshTokenValid - Token 不存在时返回 false")
    void testIsRefreshTokenValid_False() {
        // Given
        when(redisTemplate.hasKey(REDIS_KEY)).thenReturn(false);

        // When
        boolean isValid = refreshTokenService.isRefreshTokenValid(TEST_USERNAME, TEST_TOKEN);

        // Then
        assertFalse(isValid);
        verify(redisTemplate).hasKey(REDIS_KEY);
    }

    @Test
    @DisplayName("isRefreshTokenValid - Redis异常时返回 false")
    void testIsRefreshTokenValid_Exception() {
        // Given
        when(redisTemplate.hasKey(anyString())).thenThrow(new RuntimeException("Redis error"));

        // When
        boolean isValid = refreshTokenService.isRefreshTokenValid(TEST_USERNAME, TEST_TOKEN);

        // Then
        assertFalse(isValid);
    }

    @Test
    @DisplayName("revokeRefreshToken - 成功删除单个 token")
    void testRevokeRefreshToken_Success() {
        // When
        refreshTokenService.revokeRefreshToken(TEST_USERNAME, TEST_TOKEN);

        // Then
        verify(redisTemplate).delete(REDIS_KEY);
    }

    @Test
    @DisplayName("revokeAllRefreshTokens - 成功批量删除用户的所有 token")
    void testRevokeAllRefreshTokens_Success() {
        // Given
        String pattern = "token:refresh:testuser:*";
        Set<String> keys = new HashSet<>();
        keys.add("token:refresh:testuser:token1");
        keys.add("token:refresh:testuser:token2");

        when(redisTemplate.keys(pattern)).thenReturn(keys);

        // When
        refreshTokenService.revokeAllRefreshTokens(TEST_USERNAME);

        // Then
        verify(redisTemplate).keys(pattern);
        verify(redisTemplate).delete(keys);
    }

    @Test
    @DisplayName("revokeAllRefreshTokens - 无活跃 token 时不进行删除")
    void testRevokeAllRefreshTokens_NoKeys() {
        // Given
        String pattern = "token:refresh:testuser:*";
        when(redisTemplate.keys(pattern)).thenReturn(Collections.emptySet());

        // When
        refreshTokenService.revokeAllRefreshTokens(TEST_USERNAME);

        // Then
        verify(redisTemplate).keys(pattern);
        verify(redisTemplate, never()).delete(any(Set.class));
    }
}
