package com.listen.portfolio.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis 缓存与分布式数据结构核心配置类。
 *
 * <h3>设计理念与架构考量：</h3>
 * <ul>
 *   <li><b>序列化器透明度 (Serialization Transparency)：</b>
 *       Spring 默认的 <code>JdkSerializationRedisSerializer</code> 会在 Key 与 Value 前自动添加不可读的十六进制字节前缀
 *       （如 <code>\xac\xed\x00\x05t\x00...</code>），导致在终端使用 <code>redis-cli</code> 查询或调试时难以肉眼辨识。
 *       本工程统一采用 {@link StringRedisSerializer}，以纯文本 UTF-8 字符串存储所有的 Key、Value、HashKey 与 HashValue，
 *       保证 Redis 存储数据与控制台命令（<code>KEYS</code>, <code>GET</code>, <code>TTL</code>）完全肉眼可读且与其他非 Java 微服务兼容。</li>
 *   <li><b>四大业务场景通用底座：</b>
 *       为项目中的 4 大分布式模块提供统一的高性能连接通道：
 *       <ol>
 *         <li><b>TokenBlacklistService</b>：维护主动登出 JWT 令牌黑名单与基于 TTL 的自动失效。</li>
 *         <li><b>RefreshTokenService</b>：维护多端持久化会话与一键全端吊销（Revoke All）。</li>
 *         <li><b>PasswordResetTokenService</b>：生成并存储单次使用的随机密码重置安全凭据。</li>
 *         <li><b>RateLimitService</b>：基于原子递增 <code>INCR</code> 的滑动时间窗口高并发防刷限流。</li>
 *       </ol>
 *   </li>
 * </ul>
 */
@Configuration
public class RedisConfig {
    
    /**
     * 配置并注册全局通用的 {@code RedisTemplate<String, String>} Bean。
     *
     * @param connectionFactory Spring Boot 自动装配注入的 Redis 连接工厂 (LettuceConnectionFactory)
     * @return 经过纯文本序列化器武装的 RedisTemplate 实例
     */
    @Bean
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        
        // 构建标准 UTF-8 字符串序列化器
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        
        // 1. 设置 Key 序列化器：避免前缀乱码，确保 key 如 "token:blacklist:xxx" 干净存储
        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);
        
        // 2. 设置 Value 序列化器：以纯文本格式存储数值、状态标记或 JSON 字符串
        template.setValueSerializer(stringSerializer);
        template.setHashValueSerializer(stringSerializer);
        
        // 3. 设置默认兜底序列化器
        template.setDefaultSerializer(stringSerializer);
        
        // 初始化并触发属性注入校验
        template.afterPropertiesSet();
        return template;
    }
}
