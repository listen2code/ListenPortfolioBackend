package com.listen.portfolio.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis 配置类
 * 
 * 说明：
 * - 配置 Redis 连接和序列化器
 * - 为 TokenBlacklistService 提供 RedisTemplate
 * - 支持字符串和 JSON 对象的存储
 * 
 * 设计原则：
 * - 类型安全：使用泛型确保类型正确
 * - 性能优化：使用合适的序列化器
 * - 可扩展性：支持多种数据类型
 */
@Configuration
public class RedisConfig {
    
    /**
     * 配置 RedisTemplate
     * 
     * @param connectionFactory Redis 连接工厂
     * @return 配置好的 RedisTemplate
     */
    @Bean
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        
        // 设置 key 序列化器
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        
        // 设置 value 序列化器
        template.setValueSerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new StringRedisSerializer());
        
        // 设置默认序列化器
        template.setDefaultSerializer(new StringRedisSerializer());
        
        template.afterPropertiesSet();
        return template;
    }
}
