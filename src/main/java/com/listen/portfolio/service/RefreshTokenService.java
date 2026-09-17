package com.listen.portfolio.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Refresh Token 持久化与跨端多会话吊销服务 (Refresh Token Persistence & Revocation Service)。
 *
 * <p><b>一、核心设计理念与业务场景：</b>
 * <ul>
 *   <li><b>双令牌模型协同 (Dual-Token Architecture)：</b>
 *       客户端同时持有短期有效（如 5 分钟）的 Access Token 与长效持久化（如 24 小时 / 7 天）的 Refresh Token。
 *       Access Token 用于日常高频请求鉴权，Refresh Token 仅用于在 Access Token 过期时请求换发新凭证。</li>
 *   <li><b>多端持久化会话管控 (Multi-Device Session Persistence)：</b>
 *       以 {@code token:refresh:<username>:<refreshToken>} 的命名空间存储在 Redis 中，
 *       天然支持同一用户在 Web、iOS、Android 多端同时保持独立会话。</li>
 *   <li><b>全端协同即时吊销 (Instant Global Revocation)：</b>
 *       在用户执行“修改密码”、“重置密码”或“注销账号”等重大安全操作时，
 *       调用 {@link #revokeAllRefreshTokens(String)} 一键清除该用户所有终端的活跃会话，强制全端重新认证。</li>
 *   <li><b>令牌旋转机制 (Refresh Token Rotation - RTR)：</b>
 *       在调用 {@code /v1/auth/refresh} 时，服务端不仅发放新的 Access Token，
 *       同时销毁旧的 Refresh Token 并签发新的 Refresh Token，彻底阻断长效凭据被截获后的无限重放。</li>
 * </ul>
 *
 * @author Development Team
 * @since 1.0.0
 * @see TokenBlacklistService
 * @see com.listen.portfolio.api.v1.auth.AuthController
 */
@Service
public class RefreshTokenService {

    private static final Logger logger = LoggerFactory.getLogger(RefreshTokenService.class);

    private final RedisTemplate<String, String> redisTemplate;

    /**
     * Redis Key 命名空间前缀：token:refresh:<username>:<refreshToken>
     */
    private static final String REFRESH_TOKEN_PREFIX = "token:refresh:";

    /**
     * 构造函数注入 RedisTemplate
     *
     * @param redisTemplate Redis 字符串操作模板
     */
    public RefreshTokenService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 将活跃的 Refresh Token 存储至 Redis 并设定对应生存期（TTL）。
     *
     * @param username 令牌归属用户名
     * @param refreshToken 原始 Refresh Token 字符串
     * @param durationMs 有效期（毫秒）
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
     * 校验指定的 Refresh Token 在 Redis 中是否存在且处于活跃状态（未被吊销或过期）。
     *
     * @param username 令牌归属用户名
     * @param refreshToken 待核验的 Refresh Token
     * @return {@code true} 代表合法且活跃；{@code false} 代表已被吊销或过期
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
     * 单端会话注销：精确吊销并移除指定的单条 Refresh Token（如用户在当前设备点击退出登录）。
     *
     * @param username 令牌归属用户名
     * @param refreshToken 待销毁的 Refresh Token
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
     * 全端会话协同注销：吊销指定用户在所有设备上的全部活跃 Refresh Token。
     * <p>常用于修改密码、找回密码重置成功或账号注销软删除等高危场景。
     *
     * @param username 令牌归属用户名
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
