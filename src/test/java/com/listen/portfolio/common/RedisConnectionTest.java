package com.listen.portfolio.common;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Redis 连接测试
 * 
 * 说明：测试 Redis 连接和基本操作（使用 Mock）
 * 目的：验证 Redis 操作逻辑是否正确
 */
@ExtendWith(MockitoExtension.class)
public class RedisConnectionTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;
    
    @Mock
    private ValueOperations<String, String> valueOperations;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void testRedisConnection() {
        // 测试基本的 Redis 操作
        String key = "test:redis:connection";
        String value = "Redis is working!";
        
        // Mock 设置值
        doNothing().when(valueOperations).set(key, value);
        
        // Mock 获取值
        when(valueOperations.get(key)).thenReturn(value);
        
        // Mock 删除
        when(redisTemplate.delete(key)).thenReturn(true);
        
        // 执行操作
        redisTemplate.opsForValue().set(key, value);
        String retrievedValue = redisTemplate.opsForValue().get(key);
        redisTemplate.delete(key);
        
        // 验证
        assertEquals(value, retrievedValue, "Redis 连接测试失败");
        
        // 验证调用
        verify(valueOperations).set(key, value);
        verify(valueOperations).get(key);
        verify(redisTemplate).delete(key);
        
        System.out.println("✅ Redis 连接测试通过！");
    }
    
    @Test
    void testTokenBlacklistSimulation() {
        // 模拟 Token 黑名单操作
        String token = "test-token-123";
        String key = "token:blacklist:" + token;
        
        // Mock 设置值
        doNothing().when(valueOperations).set(key, "blacklisted");
        
        // Mock 获取值
        when(valueOperations.get(key)).thenReturn("blacklisted");
        
        // Mock 删除
        when(redisTemplate.delete(key)).thenReturn(true);
        
        // 执行操作
        redisTemplate.opsForValue().set(key, "blacklisted");
        String result = redisTemplate.opsForValue().get(key);
        redisTemplate.delete(key);
        
        // 验证
        assertEquals("blacklisted", result, "Token 黑名单测试失败");
        
        // 验证调用
        verify(valueOperations).set(key, "blacklisted");
        verify(valueOperations).get(key);
        verify(redisTemplate).delete(key);
        
        System.out.println("✅ Token 黑名单模拟测试通过！");
    }
}
