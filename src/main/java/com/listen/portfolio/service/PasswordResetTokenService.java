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
 * 基于 Redis 的密码重置凭据 (Password Reset Token) 生命周期管理服务。
 *
 * <h3>安全设计考量：</h3>
 * <ul>
 *   <li><b>高强度密码学随机熵源 (CSPRNG)：</b>
 *       采用底层调用操作系统安全熵池的 {@link SecureRandom} 生成 32 字节（256 位）高防碰撞随机字节数组，
 *       并经过 URL 安全的 Base64 编码，彻底免疫伪随机数预测攻击与时间戳碰撞。</li>
 *   <li><b>Redis TTL 强时效控制：</b>
 *       将生成的重置 Token 作为 Key，用户 Email 作为 Value，持久化存储于 Redis 并配置短时效 TTL（默认 3600 秒 / 1 小时）。
 *       超时后 Redis 自动销毁凭据，杜绝链接被窃听或长时间泄露后的重放修改风险。</li>
 *   <li><b>单次使用即作废 (One-Time Semantic)：</b>
 *       密码一旦成功更新，立即显式调用 {@link #deleteToken(String)} 物理删除 Key，保证链接不可二次复用。</li>
 * </ul>
 */
@Service
public class PasswordResetTokenService {

    private static final Logger logger = LoggerFactory.getLogger(PasswordResetTokenService.class);
    
    // Redis 存储前缀：password_reset:<token>
    private static final String TOKEN_PREFIX = "password_reset:";
    private static final SecureRandom secureRandom = new SecureRandom();

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    // 凭据有效时长 (秒)，默认 1 小时
    @Value("${app.password-reset.token-expiration:3600}")
    private long tokenExpiration;

    /**
     * 为指定目标邮箱生成并签发唯一的密码重置安全 Token。
     *
     * @param email 用户注册邮箱地址
     * @return 经过 URL-Safe Base64 编码的 256 位安全 Token 字符串
     */
    public String generateToken(String email) {
        logger.info(">>> [PasswordResetTokenService] 正在为邮箱 {} 签发密码重置安全凭据...", email);

        // 1. 生成 32 字节高熵随机数
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);

        // 2. 存入 Redis：key = "password_reset:<token>", value = email，设置 3600 秒时效
        String key = TOKEN_PREFIX + token;
        redisTemplate.opsForValue().set(key, email, tokenExpiration, TimeUnit.SECONDS);

        logger.info(">>> [PasswordResetTokenService] 密码重置 Token 签发完毕，Redis TTL: {} 秒", tokenExpiration);
        return token;
    }

    /**
     * 校验密码重置安全 Token 并检索绑定的目标邮箱。
     *
     * <h3>设计与安全性细节：</h3>
     * <ul>
     *   <li><b>O(1) Redis 检索：</b>从 Redis 查询键名 {@code password_reset:<token>}，
     *       若键已自然过期（TTL 到期被 Redis 驱逐）或从未生成过，返回 {@code null}。</li>
     *   <li><b>邮箱提取与上下文传递：</b>返回的 Email 将作为重置用户密码的主键匹配条件，
     *       保证密码修改仅针对凭据签发时的合法所有者。</li>
     * </ul>
     *
     * @param token 客户端提交的 256 位重置令牌字符串
     * @return 关联的用户邮箱地址；若令牌不存在、被篡改或已过期，则返回 {@code null}
     */
    public String getEmailByToken(String token) {
        logger.debug(">>> [PasswordResetTokenService] 正在验证密码重置 Token: {}", token);

        String key = TOKEN_PREFIX + token;
        String email = redisTemplate.opsForValue().get(key);

        if (email != null) {
            logger.info(">>> [PasswordResetTokenService] Token 校验合法, 关联邮箱: {}", email);
        } else {
            logger.warn(">>> [PasswordResetTokenService] Token 校验失败或已过期失效: {}", token);
        }

        return email;
    }

    /**
     * 物理注销并删除密码重置 Token（单次使用即作废语义）。
     *
     * <h3>安全防重放机制：</h3>
     * <p>一旦用户成功通过凭证重置密码，或者在重置流程中发现异常，必须立即显式执行该方法
     * 彻底从 Redis 物理删除该 Key。即使 Token 尚未达到 1 小时过期上限，也禁止被二次复用，
     * 彻底阻断中间人攻击者拦截旧链接实施二次篡改的风险。
     *
     * @param token 待销毁作废的密码重置令牌
     */
    public void deleteToken(String token) {
        logger.info(">>> [PasswordResetTokenService] 正在物理删除密码重置 Token: {}", token);

        String key = TOKEN_PREFIX + token;
        redisTemplate.delete(key);

        logger.debug(">>> [PasswordResetTokenService] Token 物理删除完成: {}", key);
    }

    /**
     * 快速探测密码重置 Token 当前是否仍然合法有效（未过期且存在）。
     *
     * <p>利用 Redis {@code EXISTS} 命令进行 O(1) 高性能无锁探测，不反序列化 Value 内容。
     * 常用于前端页面初始化时的第一步凭据有效性预检。
     *
     * @param token 待检测的重置令牌
     * @return {@code true} 表示 Token 存在且在 TTL 有效期内；{@code false} 表示不存在或已过期
     */
    public boolean isTokenValid(String token) {
        String key = TOKEN_PREFIX + token;
        Boolean exists = redisTemplate.hasKey(key);
        return exists != null && exists;
    }
}
