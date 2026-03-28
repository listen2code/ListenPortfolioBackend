package com.listen.portfolio.common;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Redis 连接测试
 * 
 * 说明：测试 Redis 连接和基本操作
 * 目的：验证 Spring Boot Redis 配置是否正确
 */
@SpringBootTest
@ActiveProfiles("test")
public class RedisConnectionTest {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Test
    void testRedisConnection() {
        // 测试基本的 Redis 操作
        String key = "test:redis:connection";
        String value = "Redis is working!";
        
        // 设置值
        redisTemplate.opsForValue().set(key, value);
        
        // 获取值
        String retrievedValue = redisTemplate.opsForValue().get(key);
        
        // 验证
        assertEquals(value, retrievedValue, "Redis 连接测试失败");
        
        // 清理测试数据
        redisTemplate.delete(key);
        
        System.out.println("✅ Redis 连接测试通过！");
    }
    
    @Test
    void testTokenBlacklistSimulation() {
        // 模拟 Token 黑名单操作
        String token = "test-token-123";
        String key = "token:blacklist:" + token;
        
        // 将 token 加入黑名单
        redisTemplate.opsForValue().set(key, "blacklisted");
        
        // 检查 token 是否在黑名单中
        String result = redisTemplate.opsForValue().get(key);
        
        // 验证
        assertEquals("blacklisted", result, "Token 黑名单测试失败");
        
        // 清理测试数据
        redisTemplate.delete(key);
        
        System.out.println("✅ Token 黑名单模拟测试通过！");
    }
}
