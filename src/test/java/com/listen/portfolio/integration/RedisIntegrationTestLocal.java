package com.listen.portfolio.integration;

import com.listen.portfolio.service.RateLimitService;
import com.listen.portfolio.service.TokenBlacklistService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Redis 集成测试 - 本地 Redis 版本
 * 
 * 说明：测试 Redis 相关服务的真实操作（使用 BaseIntegrationTest 提供的嵌入式 Redis）
 * 目的：验证限流服务和 Token 黑名单在真实 Redis 协议下的行为
 */
public class RedisIntegrationTestLocal extends BaseIntegrationTest {

    @Autowired
    private RateLimitService rateLimitService;

    @Autowired
    private TokenBlacklistService tokenBlacklistService;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @BeforeEach
    void cleanRedis() {
        try {
            // 清理 Redis 中的测试数据
            redisTemplate.getConnectionFactory().getConnection().serverCommands().flushDb();
        } catch (Exception e) {
            // 如果 Redis 连接失败，跳过测试
            org.junit.jupiter.api.Assumptions.assumeTrue(false, "Redis 服务不可用，跳过集成测试: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("限流服务 - 真实 Redis 操作测试")
    void testRateLimitService_RealRedis() throws InterruptedException {
        // Given - 使用动态生成的标识符
        String identifier = "test:rate-limit:" + System.currentTimeMillis();
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
    @DisplayName("Token 黑名单服务 - 真实 Redis 操作测试")
    void testTokenBlacklistService_RealRedis() throws InterruptedException {
        // Given
        String token = "test.token." + System.currentTimeMillis();
        // 传入过期时间戳（当前时间 + 5秒）
        long expirationTimestamp = System.currentTimeMillis() + 5000;

        // When - 添加 token 到黑名单
        tokenBlacklistService.addToBlacklist(token, expirationTimestamp);

        // Then - token 应该在黑名单中
        assertTrue(tokenBlacklistService.isBlacklisted(token), "Token 应该在黑名单中");

        // When - 等待过期
        Thread.sleep(6000); // 等待6秒，超过5秒的过期时间

        // Then - token 应该已经从黑名单中移除
        assertFalse(tokenBlacklistService.isBlacklisted(token), "Token 应该已经从黑名单中移除");
    }

    @Test
    @DisplayName("Redis 连接测试")
    void testRedisConnection() {
        // Given
        String testKey = "test:connection:" + System.currentTimeMillis();
        String testValue = "test-value";

        // When
        redisTemplate.opsForValue().set(testKey, testValue, 10, TimeUnit.SECONDS);

        // Then
        String retrievedValue = redisTemplate.opsForValue().get(testKey);
        assertEquals(testValue, retrievedValue, "Redis 连接和基本操作应该正常");

        // Cleanup
        redisTemplate.delete(testKey);
    }
}
