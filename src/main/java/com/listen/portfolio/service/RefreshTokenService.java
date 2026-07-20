package com.listen.portfolio.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Refresh Token 持久化与吊销服务
 * 
 * 说明：
 * - 维护已发放的 refresh token 记录，实现有状态的 JWT 刷新控制
 * - 使用 Redis 存储，支持多实例分布式部署
 * - 通过前缀匹配实现用户所有设备会话的一键吊销（如修改密码、退出登录、软删除账户）
 */
@Service
public class RefreshTokenService {
    
    private static final Logger logger = LoggerFactory.getLogger(RefreshTokenService.class);
    
    private final RedisTemplate<String, String> redisTemplate;
    
    // Redis 键名前缀：token:refresh:<username>:<refreshToken>
    private static final String REFRESH_TOKEN_PREFIX = "token:refresh:";
    
    public RefreshTokenService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }
    
    /**
     * 将生成的 Refresh Token 保存到 Redis
     * 
     * @param username 用户名
     * @param refreshToken 刷新令牌
     * @param durationMs 过期时长（毫秒）
     */
    public void saveRefreshToken(String username, String refreshToken, long durationMs) {
        try {
            String key = REFRESH_TOKEN_PREFIX + username + ":" + refreshToken;
            redisTemplate.opsForValue().set(key, "active", durationMs, TimeUnit.MILLISECONDS);
            logger.info("Successfully saved refresh token in Redis for user: {}, TTL: {}ms", username, durationMs);
        } catch (Exception e) {
            logger.error("Failed to save refresh token for user: {}, error: {}", username, e.getMessage());
        }
    }
    
    /**
     * 校验 Refresh Token 是否有效（存在于 Redis 中且未过期）
     * 
     * @param username 用户名
     * @param refreshToken 刷新令牌
     * @return 如果有效返回 true，否则返回 false
     */
    public boolean isRefreshTokenValid(String username, String refreshToken) {
        try {
            String key = REFRESH_TOKEN_PREFIX + username + ":" + refreshToken;
            Boolean exists = redisTemplate.hasKey(key);
            return exists != null && exists;
        } catch (Exception e) {
            logger.error("Failed to check refresh token validity for user: {}, error: {}", username, e.getMessage());
            return false;
        }
    }
    
    /**
     * 吊销/删除特定的 Refresh Token
     * 
     * @param username 用户名
     * @param refreshToken 刷新令牌
     */
    public void revokeRefreshToken(String username, String refreshToken) {
        try {
            String key = REFRESH_TOKEN_PREFIX + username + ":" + refreshToken;
            redisTemplate.delete(key);
            logger.info("Revoked specific refresh token for user: {}", username);
        } catch (Exception e) {
            logger.error("Failed to revoke specific refresh token for user: {}, error: {}", username, e.getMessage());
        }
    }
    
    /**
     * 吊销/清空当前用户的所有 Refresh Token
     * 
     * @param username 用户名
     */
    public void revokeAllRefreshTokens(String username) {
        try {
            String pattern = REFRESH_TOKEN_PREFIX + username + ":*";
            Set<String> keys = redisTemplate.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                logger.info("Successfully revoked all refresh tokens ({}) for user: {}", keys.size(), username);
            } else {
                logger.info("No active refresh tokens found to revoke for user: {}", username);
            }
        } catch (Exception e) {
            logger.error("Failed to revoke all refresh tokens for user: {}, error: {}", username, e.getMessage());
        }
    }
}
