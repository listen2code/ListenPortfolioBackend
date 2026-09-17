package com.listen.portfolio.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * 基于 Redis 的 JWT 访问令牌黑名单服务 (Token Blacklist Service)。
 *
 * <p><b>一、架构背景与设计思路：</b>
 * <ul>
 *   <li><b>有状态控制补全无状态缺陷 (Stateless vs Stateful Balance)：</b>
 *       标准 JWT 是纯无状态的，一旦签发并在有效期内，服务端无法单方面强制使其失效（例如用户主动点击登出、
 *       修改密码、注销账号或管理员封禁账号）。
 *       通过在 Redis 中建立一个轻量级黑名单字典（Key 结构：{@code token:blacklist:<jwt>}，TTL 为该 Token 距离过期的剩余秒数），
 *       在 {@link com.listen.portfolio.common.jwt.JwtRequestFilter} 拦截器中增加 $O(1)$ 复杂度的极速缓存探测，
 *       以极小内存开销完美解决 JWT 登出不可控的安全痛点。</li>
 *   <li><b>利用 Redis TTL 机制实现自动自洁（Zero Maintenance）：</b>
 *       黑名单记录的存活时间严格对齐该 Token 的剩余有效时间（过期后 JWT 自身解析就会校验失败并拒绝）。
 *       由 Redis 内部定时与惰性删除策略自动淘汰，无需定时任务轮询清表，永不引发内存泄漏。</li>
 *   <li><b>高可用故障开路策略 (Fail-Safe Strategy)：</b>
 *       当 Redis 短暂断连或宕机发生异常时，捕获异常并返回 false（保守放行并记录错误告警），
 *       防止缓存故障直接演变成整站所有用户请求均不可用的级联崩溃。</li>
 * </ul>
 *
 * @author Development Team
 * @since 1.0.0
 * @see com.listen.portfolio.common.jwt.JwtRequestFilter
 * @see com.listen.portfolio.service.RefreshTokenService
 */
@Service
public class TokenBlacklistService {

    private static final Logger logger = LoggerFactory.getLogger(TokenBlacklistService.class);

    private final RedisTemplate<String, String> redisTemplate;

    /**
     * Redis 黑名单 Key 统一命名空间前缀：token:blacklist:<rawToken>
     */
    private static final String BLACKLIST_PREFIX = "token:blacklist:";

    /**
     * 默认兜底最大黑名单过期时间（24小时）
     */
    private static final Duration DEFAULT_BLACKLIST_TTL = Duration.ofHours(24);

    /**
     * 构造函数注入 RedisTemplate
     *
     * @param redisTemplate Redis 字符串操作模板
     */
    public TokenBlacklistService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 将指定的 JWT Token 登记至 Redis 黑名单。
     *
     * <p>通常在用户登出、修改密码或账号软删除注销时调用。
     * 根据 Token 的原始过期时间戳动态计算剩余生存时间，设置精确的毫秒级 TTL。
     *
     * @param token 待封禁的原始 JWT 字符串
     * @param expiration Token 的原始到期绝对时间戳 (毫秒)
     */
    public void addToBlacklist(String token, long expiration) {
        try {
            String key = BLACKLIST_PREFIX + token;
            long ttl = calculateTTL(expiration);

            // 写入 Redis 并严格设置毫秒级 TTL 自动淘汰
            redisTemplate.opsForValue().set(key, "blacklisted", ttl, TimeUnit.MILLISECONDS);
            logger.info(">>> [TokenBlacklistService] Token 已成功加入黑名单，剩余 TTL: {}ms", ttl);
        } catch (Exception e) {
            logger.error(">>> [TokenBlacklistService] 写入 Token 黑名单失败: {}", e.getMessage());
        }
    }

    /**
     * 检查指定的 JWT Token 是否存在于黑名单中。
     *
     * @param token 待校验的 JWT 令牌字符串
     * @return {@code true} 代表已被封禁加入黑名单；{@code false} 代表不在黑名单中或 Redis 发生异常
     */
    public boolean isBlacklisted(String token) {
        try {
            String key = BLACKLIST_PREFIX + token;
            String value = redisTemplate.opsForValue().get(key);
            return value != null;
        } catch (Exception e) {
            logger.error("Failed to check token blacklist: {}", e.getMessage());
            // 容灾开路：发生异常时保守处理，避免缓存故障导致系统瘫痪
            return false;
        }
    }

    /**
     * 动态计算 Token 在黑名单中的存活时长（TTL）。
     *
     * @param tokenExpiration Token 原始过期时间戳（毫秒）
     * @return 应当在黑名单中保留的毫秒数
     */
    private long calculateTTL(long tokenExpiration) {
        long currentTime = System.currentTimeMillis();
        long remainingTime = tokenExpiration - currentTime;

        // 若 Token 尚未过期，TTL 设为剩余有效时长（最大不超过兜底 24 小时）
        if (remainingTime > 0) {
            return Math.min(remainingTime, DEFAULT_BLACKLIST_TTL.toMillis());
        }

        // 若 Token 已由客户端过期但仍需登记，使用兜底 24 小时
        return DEFAULT_BLACKLIST_TTL.toMillis();
    }

    /**
     * 从黑名单中移除指定的 Token（支持管理后台解封等运维场景）。
     *
     * @param token 要移除的 JWT 令牌
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
