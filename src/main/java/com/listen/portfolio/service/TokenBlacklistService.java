package com.listen.portfolio.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Token 黑名单服务
 * 
 * 说明：
 * - 维护已登出的 token 黑名单，防止被继续使用
 * - 使用 Redis 存储黑名单，支持分布式部署
 * - 设置合理的过期时间，避免内存泄漏
 * 
 * 设计原则：
 * - 高性能：使用 Redis 的快速查询能力
 * - 自动清理：利用 Redis 的 TTL 机制
 * - 线程安全：Redis 操作天然线程安全
 */
@Service
public class TokenBlacklistService {
    
    private static final Logger logger = LoggerFactory.getLogger(TokenBlacklistService.class);
    
    private final RedisTemplate<String, String> redisTemplate;
    
    // 黑名单 key 前缀
    private static final String BLACKLIST_PREFIX = "token:blacklist:";
    
    // 默认黑名单过期时间（24小时）
    private static final Duration DEFAULT_BLACKLIST_TTL = Duration.ofHours(24);
    
    public TokenBlacklistService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }
    
    /**
     * 将 token 加入黑名单
     * 
     * @param token 要加入黑名单的 token
     * @param expiration token 的原始过期时间
     */
    public void addToBlacklist(String token, long expiration) {
        try {
            String key = BLACKLIST_PREFIX + token;
            long ttl = calculateTTL(expiration);
            
            redisTemplate.opsForValue().set(key, "blacklisted", ttl, TimeUnit.MILLISECONDS);
            logger.info("Token added to blacklist, TTL: {}ms", ttl);
        } catch (Exception e) {
            logger.error("Failed to add token to blacklist: {}", e.getMessage());
        }
    }
    
    /**
     * 检查 token 是否在黑名单中
     * 
     * @param token 要检查的 token
     * @return 如果在黑名单中返回 true，否则返回 false
     */
    public boolean isBlacklisted(String token) {
        try {
            String key = BLACKLIST_PREFIX + token;
            String value = redisTemplate.opsForValue().get(key);
            return value != null;
        } catch (Exception e) {
            logger.error("Failed to check token blacklist: {}", e.getMessage());
            // 出错时保守处理，认为不在黑名单中
            return false;
        }
    }
    
    /**
     * 计算 token 在黑名单中的存活时间
     * 
     * @param tokenExpiration token 的原始过期时间（时间戳）
     * @return 黑名单中的存活时间（毫秒）
     */
    private long calculateTTL(long tokenExpiration) {
        long currentTime = System.currentTimeMillis();
        long remainingTime = tokenExpiration - currentTime;
        
        // 如果 token 还有剩余时间，使用剩余时间
        if (remainingTime > 0) {
            return Math.min(remainingTime, DEFAULT_BLACKLIST_TTL.toMillis());
        }
        
        // 如果 token 已经过期，使用默认黑名单时间
        return DEFAULT_BLACKLIST_TTL.toMillis();
    }
    
    /**
     * 从黑名单中移除 token（可选功能）
     * 
     * @param token 要移除的 token
     */
    public void removeFromBlacklist(String token) {
        try {
            String key = BLACKLIST_PREFIX + token;
            redisTemplate.delete(key);
            logger.info("Token removed from blacklist");
        } catch (Exception e) {
            logger.error("Failed to remove token from blacklist: {}", e.getMessage());
        }
    }
}
