package com.listen.portfolio.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

/**
 * 密码重置 Token 服务
 * 
 * 说明：管理密码重置 Token 的生成、存储和验证
 * 原理：使用 Redis 存储 Token，设置过期时间，确保安全性
 */
@Service
public class PasswordResetTokenService {

    private static final Logger logger = LoggerFactory.getLogger(PasswordResetTokenService.class);
    private static final String TOKEN_PREFIX = "password_reset:";
    private static final SecureRandom secureRandom = new SecureRandom();

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Value("${app.password-reset.token-expiration:3600}")
    private long tokenExpiration;

    /**
     * 生成密码重置 Token
     * 
     * 说明：生成安全的随机 Token 并存储到 Redis
     * 
     * @param email 用户邮箱
     * @return 生成的 Token
     */
    public String generateToken(String email) {
        logger.info("为邮箱 {} 生成密码重置 Token", email);

        // 生成 32 字节的随机 Token
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);

        // 存储到 Redis，key = "password_reset:token", value = email
        String key = TOKEN_PREFIX + token;
        redisTemplate.opsForValue().set(key, email, tokenExpiration, TimeUnit.SECONDS);

        logger.info("密码重置 Token 已生成并存储，有效期: {} 秒", tokenExpiration);
        return token;
    }

    /**
     * 验证并获取 Token 对应的邮箱
     * 
     * 说明：验证 Token 是否有效，并返回对应的邮箱
     * 
     * @param token 密码重置 Token
     * @return Token 对应的邮箱，如果 Token 无效则返回 null
     */
    public String getEmailByToken(String token) {
        logger.debug("验证密码重置 Token");

        String key = TOKEN_PREFIX + token;
        String email = redisTemplate.opsForValue().get(key);

        if (email != null) {
            logger.info("Token 验证成功，对应邮箱: {}", email);
        } else {
            logger.warn("Token 验证失败或已过期");
        }

        return email;
    }

    /**
     * 删除 Token
     * 
     * 说明：使用 Token 后立即删除，防止重复使用
     * 
     * @param token 密码重置 Token
     */
    public void deleteToken(String token) {
        logger.info("删除密码重置 Token");

        String key = TOKEN_PREFIX + token;
        redisTemplate.delete(key);

        logger.debug("Token 已删除");
    }

    /**
     * 检查 Token 是否存在
     * 
     * 说明：检查 Token 是否有效
     * 
     * @param token 密码重置 Token
     * @return Token 是否存在
     */
    public boolean isTokenValid(String token) {
        String key = TOKEN_PREFIX + token;
        Boolean exists = redisTemplate.hasKey(key);
        return exists != null && exists;
    }
}
