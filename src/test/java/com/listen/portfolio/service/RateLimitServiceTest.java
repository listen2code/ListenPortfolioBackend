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

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * RateLimitService 单元测试
 * 
 * 说明：测试限流服务的所有核心功能
 * 目的：确保限流检查、Redis 操作、故障容错功能正常工作
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RateLimitService Unit Tests")
class RateLimitServiceTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private RateLimitService rateLimitService;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    @DisplayName("限流检查 - 允许通过（首次访问）")
    void testIsAllowed_FirstAccess_ReturnsTrue() {
        // Given
        String identifier = "ip:192.168.1.100";
        int maxRequests = 10;
        int timeWindowSeconds = 60;
        
        when(valueOperations.increment(anyString())).thenReturn(1L);
        when(redisTemplate.expire(anyString(), eq(60L), eq(TimeUnit.SECONDS))).thenReturn(true);

        // When
        boolean result = rateLimitService.isAllowed(identifier, maxRequests, timeWindowSeconds);

        // Then
        assertTrue(result, "首次访问应该被允许");
        verify(valueOperations).increment(contains("rate_limit:" + identifier));
        verify(redisTemplate).expire(contains("rate_limit:" + identifier), eq((long) 60), eq(TimeUnit.SECONDS));
    }

    @Test
    @DisplayName("限流检查 - 允许通过（未超限）")
    void testIsAllowed_UnderLimit_ReturnsTrue() {
        // Given
        String identifier = "ip:192.168.1.100";
        int maxRequests = 10;
        int timeWindowSeconds = 60;
        
        when(valueOperations.increment(anyString())).thenReturn(5L);

        // When
        boolean result = rateLimitService.isAllowed(identifier, maxRequests, timeWindowSeconds);

        // Then
        assertTrue(result, "未超限的访问应该被允许");
        verify(valueOperations).increment(contains("rate_limit:" + identifier));
        verify(redisTemplate, never()).expire(anyString(), anyLong(), any(TimeUnit.class));
    }

    @Test
    @DisplayName("限流检查 - 超限拒绝")
    void testIsAllowed_ExceedsLimit_ReturnsFalse() {
        // Given
        String identifier = "ip:192.168.1.100";
        int maxRequests = 10;
        int timeWindowSeconds = 60;
        
        when(valueOperations.increment(anyString())).thenReturn(11L);

        // When
        boolean result = rateLimitService.isAllowed(identifier, maxRequests, timeWindowSeconds);

        // Then
        assertFalse(result, "超限的访问应该被拒绝");
        verify(valueOperations).increment(contains("rate_limit:" + identifier));
        verify(redisTemplate, never()).expire(anyString(), anyLong(), any(TimeUnit.class));
    }

    @Test
    @DisplayName("IP 限流检查 - 允许通过")
    void testIsIpAllowed_ReturnsTrue() {
        // Given
        String ip = "192.168.1.100";
        
        when(valueOperations.increment(anyString())).thenReturn(1L);
        when(redisTemplate.expire(anyString(), eq(60L), eq(TimeUnit.SECONDS))).thenReturn(true);

        // When
        boolean result = rateLimitService.isIpAllowed(ip);

        // Then
        assertTrue(result, "IP 限流首次访问应该被允许");
        verify(valueOperations).increment(contains("rate_limit:ip:" + ip));
        verify(redisTemplate).expire(contains("rate_limit:ip:" + ip), eq(60L), eq(TimeUnit.SECONDS));
    }

    @Test
    @DisplayName("邮箱限流检查 - 允许通过")
    void testIsEmailAllowed_ReturnsTrue() {
        // Given
        String email = "test@example.com";
        
        when(valueOperations.increment(anyString())).thenReturn(1L);
        when(redisTemplate.expire(anyString(), eq(300L), eq(TimeUnit.SECONDS))).thenReturn(true);

        // When
        boolean result = rateLimitService.isEmailAllowed(email);

        // Then
        assertTrue(result, "邮箱限流首次访问应该被允许");
        verify(valueOperations).increment(contains("rate_limit:email:" + email));
        verify(redisTemplate).expire(contains("rate_limit:email:" + email), eq(300L), eq(TimeUnit.SECONDS));
    }

    @Test
    @DisplayName("获取剩余请求次数 - 正常情况")
    void testGetRemainingRequests_NormalCase() {
        // Given
        String identifier = "ip:192.168.1.100";
        int maxRequests = 10;
        int timeWindowSeconds = 60;
        
        when(valueOperations.get(anyString())).thenReturn("3");

        // When
        int remaining = rateLimitService.getRemainingRequests(identifier, maxRequests, timeWindowSeconds);

        // Then
        assertEquals(7, remaining, "剩余请求次数应该是 7");
        verify(valueOperations).get(contains("rate_limit:" + identifier));
    }

    @Test
    @DisplayName("获取剩余请求次数 - 无记录")
    void testGetRemainingRequests_NoRecord() {
        // Given
        String identifier = "ip:192.168.1.100";
        int maxRequests = 10;
        int timeWindowSeconds = 60;
        
        when(valueOperations.get(anyString())).thenReturn(null);

        // When
        int remaining = rateLimitService.getRemainingRequests(identifier, maxRequests, timeWindowSeconds);

        // Then
        assertEquals(10, remaining, "无记录时应该返回最大请求数");
        verify(valueOperations).get(contains("rate_limit:" + identifier));
    }

    @Test
    @DisplayName("Redis 异常 - 容错处理")
    void testIsAllowed_RedisException_ReturnsTrue() {
        // Given
        String identifier = "ip:192.168.1.100";
        int maxRequests = 10;
        int timeWindowSeconds = 60;
        
        when(valueOperations.increment(anyString()))
                .thenThrow(new RuntimeException("Redis 连接失败"));

        // When
        boolean result = rateLimitService.isAllowed(identifier, maxRequests, timeWindowSeconds);

        // Then
        assertTrue(result, "Redis 异常时应该允许访问（容错）");
        verify(valueOperations).increment(contains("rate_limit:" + identifier));
    }

    @Test
    @DisplayName("并发测试 - 多线程访问")
    void testIsAllowed_ConcurrentAccess() throws InterruptedException {
        // Given
        String identifier = "ip:192.168.1.100";
        int maxRequests = 5;
        int timeWindowSeconds = 60;
        int threadCount = 10;
        
        when(valueOperations.increment(anyString()))
                .thenReturn(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L);

        // When
        Thread[] threads = new Thread[threadCount];
        boolean[] results = new boolean[threadCount];
        
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            threads[i] = new Thread(() -> {
                results[index] = rateLimitService.isAllowed(identifier, maxRequests, timeWindowSeconds);
            });
            threads[i].start();
        }
        
        for (Thread thread : threads) {
            thread.join();
        }

        // Then
        int allowedCount = 0;
        for (boolean result : results) {
            if (result) allowedCount++;
        }
        
        assertEquals(5, allowedCount, "应该有 5 个请求被允许");
        verify(valueOperations, times(threadCount)).increment(contains("rate_limit:" + identifier));
    }

    @Test
    @DisplayName("时间窗口边界测试")
    void testIsAllowed_TimeWindowBoundary() {
        // Given
        String identifier = "ip:192.168.1.100";
        int maxRequests = 10;
        int timeWindowSeconds = 60;
        
        when(valueOperations.increment(anyString())).thenReturn(10L);

        // When
        boolean result = rateLimitService.isAllowed(identifier, maxRequests, timeWindowSeconds);

        // Then
        assertTrue(result, "达到限制但未超时应该被允许");
        verify(valueOperations).increment(contains("rate_limit:" + identifier));
    }

    @Test
    @DisplayName("Redis increment 返回 null - 容错处理")
    void testIsAllowed_NullCount_ReturnsTrue() {
        // Given
        String identifier = "ip:192.168.1.100";
        int maxRequests = 10;
        int timeWindowSeconds = 60;
        
        when(valueOperations.increment(anyString())).thenReturn(null);

        // When
        boolean result = rateLimitService.isAllowed(identifier, maxRequests, timeWindowSeconds);

        // Then
        assertTrue(result, "Redis increment 返回 null 时应该允许访问（容错）");
        verify(valueOperations).increment(contains("rate_limit:" + identifier));
    }

    @Test
    @DisplayName("批量操作 - 多个标识符限流检查")
    void testMultipleIdentifiers() {
        // Given
        String[] identifiers = {"ip:192.168.1.100", "email:test@example.com", "user:testuser"};
        int maxRequests = 10;
        int timeWindowSeconds = 60;
        
        when(valueOperations.increment(anyString())).thenReturn(1L);
        when(redisTemplate.expire(anyString(), eq((long) timeWindowSeconds), eq(TimeUnit.SECONDS))).thenReturn(true);

        // When
        boolean[] results = new boolean[identifiers.length];
        for (int i = 0; i < identifiers.length; i++) {
            results[i] = rateLimitService.isAllowed(identifiers[i], maxRequests, timeWindowSeconds);
        }

        // Then
        for (boolean result : results) {
            assertTrue(result, "所有标识符的首次访问都应该被允许");
        }
        
        verify(valueOperations, times(identifiers.length)).increment(contains("rate_limit:"));
        verify(redisTemplate, times(identifiers.length)).expire(contains("rate_limit:"), eq((long) timeWindowSeconds), eq(TimeUnit.SECONDS));
    }

    @Test
    @DisplayName("边界测试 - 极大时间窗口")
    void testIsAllowed_LargeTimeWindow() {
        // Given
        String identifier = "ip:192.168.1.100";
        int maxRequests = 10;
        int timeWindowSeconds = 86400; // 24 小时
        
        when(valueOperations.increment(anyString())).thenReturn(1L);
        when(redisTemplate.expire(anyString(), eq((long) timeWindowSeconds), eq(TimeUnit.SECONDS))).thenReturn(true);

        // When
        boolean result = rateLimitService.isAllowed(identifier, maxRequests, timeWindowSeconds);

        // Then
        assertTrue(result, "大时间窗口的首次访问应该被允许");
        verify(valueOperations).increment(contains("rate_limit:" + identifier));
        verify(redisTemplate).expire(contains("rate_limit:" + identifier), eq((long) timeWindowSeconds), eq(TimeUnit.SECONDS));
    }

    @Test
    @DisplayName("边界测试 - 极小时间窗口")
    void testIsAllowed_SmallTimeWindow() {
        // Given
        String identifier = "ip:192.168.1.100";
        int maxRequests = 10;
        int timeWindowSeconds = 1; // 1 秒
        
        when(valueOperations.increment(anyString())).thenReturn(1L);
        when(redisTemplate.expire(anyString(), eq((long) timeWindowSeconds), eq(TimeUnit.SECONDS))).thenReturn(true);

        // When
        boolean result = rateLimitService.isAllowed(identifier, maxRequests, timeWindowSeconds);

        // Then
        assertTrue(result, "小时间窗口的首次访问应该被允许");
        verify(valueOperations).increment(contains("rate_limit:" + identifier));
        verify(redisTemplate).expire(contains("rate_limit:" + identifier), eq((long) timeWindowSeconds), eq(TimeUnit.SECONDS));
    }
}
