package com.listen.portfolio.integration;

import com.listen.portfolio.service.RateLimitService;
import com.listen.portfolio.service.TokenBlacklistService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Redis 集成测试
 * 
 * 说明：测试 Redis 相关服务的真实操作
 * 目的：验证限流服务和 Token 黑名单在真实 Redis 环境中的行为
 */
public class RedisIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private RateLimitService rateLimitService;

    @Autowired
    private TokenBlacklistService tokenBlacklistService;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @BeforeEach
    void cleanRedis() {
        // 清理 Redis 中的测试数据
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushDb();
    }

    @Test
    @DisplayName("限流服务 - 真实 Redis 操作测试")
    void testRateLimitService_RealRedis() throws InterruptedException {
        // Given
        String identifier = "ip:192.168.1.100";
        int maxRequests = 3;
        int timeWindowSeconds = 5;

        // When - 连续发送请求
        boolean result1 = rateLimitService.isAllowed(identifier, maxRequests, timeWindowSeconds);
        boolean result2 = rateLimitService.isAllowed(identifier, maxRequests, timeWindowSeconds);
        boolean result3 = rateLimitService.isAllowed(identifier, maxRequests, timeWindowSeconds);
        boolean result4 = rateLimitService.isAllowed(identifier, maxRequests, timeWindowSeconds);

        // Then - 前3个应该通过，第4个应该被拒绝
        assertTrue(result1, "第1个请求应该通过");
        assertTrue(result2, "第2个请求应该通过");
        assertTrue(result3, "第3个请求应该通过");
        assertFalse(result4, "第4个请求应该被拒绝");

        // When - 等待时间窗口过期
        Thread.sleep(6000); // 等待6秒，超过5秒的时间窗口

        // Then - 时间窗口过期后应该重新允许
        boolean resultAfterExpire = rateLimitService.isAllowed(identifier, maxRequests, timeWindowSeconds);
        assertTrue(resultAfterExpire, "时间窗口过期后应该重新允许请求");
    }

    @Test
    @DisplayName("限流服务 - IP 限流测试")
    void testRateLimitService_IpLimiting() {
        // Given
        String ip = "192.168.1.100";

        // When - 使用 IP 限流方法
        boolean result1 = rateLimitService.isIpAllowed(ip);
        boolean result2 = rateLimitService.isIpAllowed(ip);
        boolean result3 = rateLimitService.isIpAllowed(ip);
        boolean result4 = rateLimitService.isIpAllowed(ip);
        boolean result5 = rateLimitService.isIpAllowed(ip);
        boolean result6 = rateLimitService.isIpAllowed(ip);
        boolean result7 = rateLimitService.isIpAllowed(ip);
        boolean result8 = rateLimitService.isIpAllowed(ip);
        boolean result9 = rateLimitService.isIpAllowed(ip);
        boolean result10 = rateLimitService.isIpAllowed(ip);
        boolean result11 = rateLimitService.isIpAllowed(ip);

        // Then - 前10个应该通过（1分钟内最多10次），第11个应该被拒绝
        assertTrue(result1, "IP限流第1个请求应该通过");
        assertTrue(result2, "IP限流第2个请求应该通过");
        assertTrue(result3, "IP限流第3个请求应该通过");
        assertTrue(result4, "IP限流第4个请求应该通过");
        assertTrue(result5, "IP限流第5个请求应该通过");
        assertTrue(result6, "IP限流第6个请求应该通过");
        assertTrue(result7, "IP限流第7个请求应该通过");
        assertTrue(result8, "IP限流第8个请求应该通过");
        assertTrue(result9, "IP限流第9个请求应该通过");
        assertTrue(result10, "IP限流第10个请求应该通过");
        assertFalse(result11, "IP限流第11个请求应该被拒绝");
    }

    @Test
    @DisplayName("限流服务 - 邮箱限流测试")
    void testRateLimitService_EmailLimiting() {
        // Given
        String email = "test@example.com";

        // When - 使用邮箱限流方法
        boolean result1 = rateLimitService.isEmailAllowed(email);
        boolean result2 = rateLimitService.isEmailAllowed(email);
        boolean result3 = rateLimitService.isEmailAllowed(email);
        boolean result4 = rateLimitService.isEmailAllowed(email);

        // Then - 前3个应该通过（5分钟内最多3次），第4个应该被拒绝
        assertTrue(result1, "邮箱限流第1个请求应该通过");
        assertTrue(result2, "邮箱限流第2个请求应该通过");
        assertTrue(result3, "邮箱限流第3个请求应该通过");
        assertFalse(result4, "邮箱限流第4个请求应该被拒绝");
    }

    @Test
    @DisplayName("Token 黑名单服务 - 添加和检查")
    void testTokenBlacklistService_AddAndCheck() {
        // Given
        String token = "test.jwt.token";

        // When - 添加 Token 到黑名单
        tokenBlacklistService.addToBlacklist(token, System.currentTimeMillis() + 3600000); // 1小时后过期

        // Then - Token 应该在黑名单中
        assertTrue(tokenBlacklistService.isBlacklisted(token), "Token 应该在黑名单中");

        // When - 检查不存在的 Token
        boolean nonExistentToken = tokenBlacklistService.isBlacklisted("non.existent.token");

        // Then - 不存在的 Token 不应该在黑名单中
        assertFalse(nonExistentToken, "不存在的 Token 不应该在黑名单中");
    }

    @Test
    @DisplayName("Token 黑名单服务 - 过期处理")
    void testTokenBlacklistService_Expiration() throws InterruptedException {
        // Given
        String token = "test.jwt.token";

        // When - 添加 Token 到黑名单（短过期时间用于测试）
        tokenBlacklistService.addToBlacklist(token, System.currentTimeMillis() + 2000); // 2秒后过期

        // Then - Token 应该立即在黑名单中
        assertTrue(tokenBlacklistService.isBlacklisted(token), "Token 应该立即在黑名单中");

        // When - 等待过期
        Thread.sleep(3000); // 等待3秒，确保过期

        // Then - 过期后不应该在黑名单中
        boolean expiredToken = tokenBlacklistService.isBlacklisted(token);
        assertFalse(expiredToken, "过期后不应该在黑名单中");
    }

    @Test
    @DisplayName("Redis 连接 - 基本操作测试")
    void testRedisConnection_BasicOperations() {
        // Given
        String key = "test:key";
        String value = "test:value";

        // When - 设置值
        ValueOperations<String, String> operations = redisTemplate.opsForValue();
        operations.set(key, value);

        // Then - 应该能够获取到值
        String retrievedValue = operations.get(key);
        assertEquals(value, retrievedValue, "应该能够获取到设置的值");

        // When - 删除键
        redisTemplate.delete(key);

        // Then - 键应该不存在
        String deletedValue = operations.get(key);
        assertNull(deletedValue, "删除后应该获取不到值");
    }

    @Test
    @DisplayName("Redis 连接 - 过期时间测试")
    void testRedisConnection_Expiration() throws InterruptedException {
        // Given
        String key = "test:expire";
        String value = "test:value";

        // When - 设置带过期时间的值
        ValueOperations<String, String> operations = redisTemplate.opsForValue();
        operations.set(key, value, 2, TimeUnit.SECONDS);

        // Then - 应该立即能够获取到值
        String retrievedValue = operations.get(key);
        assertEquals(value, retrievedValue, "应该立即能够获取到值");

        // When - 等待过期
        Thread.sleep(3000); // 等待3秒，确保过期

        // Then - 过期后应该获取不到值
        String expiredValue = operations.get(key);
        assertNull(expiredValue, "过期后应该获取不到值");
    }

    @Test
    @DisplayName("限流服务 - 并发测试")
    void testRateLimitService_ConcurrentAccess() throws InterruptedException {
        // Given
        String identifier = "concurrent:test";
        int maxRequests = 5;
        int timeWindowSeconds = 10;
        int threadCount = 10;

        // When - 多线程并发访问
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

        // Then - 应该只有 maxRequests 个请求通过
        int allowedCount = 0;
        for (boolean result : results) {
            if (result) allowedCount++;
        }
        
        assertEquals(maxRequests, allowedCount, "应该只有 " + maxRequests + " 个请求通过");
    }

    @Test
    @DisplayName("限流服务 - 不同标识符隔离测试")
    void testRateLimitService_IdentifierIsolation() {
        // Given
        String identifier1 = "user1";
        String identifier2 = "user2";
        int maxRequests = 2;
        int timeWindowSeconds = 60;

        // When - 对不同标识符进行限流
        boolean user1Result1 = rateLimitService.isAllowed(identifier1, maxRequests, timeWindowSeconds);
        boolean user1Result2 = rateLimitService.isAllowed(identifier1, maxRequests, timeWindowSeconds);
        boolean user1Result3 = rateLimitService.isAllowed(identifier1, maxRequests, timeWindowSeconds);
        
        boolean user2Result1 = rateLimitService.isAllowed(identifier2, maxRequests, timeWindowSeconds);
        boolean user2Result2 = rateLimitService.isAllowed(identifier2, maxRequests, timeWindowSeconds);

        // Then - 不同标识符应该独立计算
        assertTrue(user1Result1, "用户1第1个请求应该通过");
        assertTrue(user1Result2, "用户1第2个请求应该通过");
        assertFalse(user1Result3, "用户1第3个请求应该被拒绝");
        
        assertTrue(user2Result1, "用户2第1个请求应该通过");
        assertTrue(user2Result2, "用户2第2个请求应该通过");
    }
}
