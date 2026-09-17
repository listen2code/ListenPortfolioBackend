package com.listen.portfolio.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 基于 Redis 的分布式限流核心服务 (Distributed Rate Limiting Service)。
 *
 * <p><b>一、核心设计理念与算法机制：</b>
 * <ul>
 *   <li><b>固定时间窗口计数器算法 (Fixed-Window Counter Algorithm)：</b>
 *       通过公式 {@code windowBucket = System.currentTimeMillis() / (timeWindowSeconds * 1000)} 计算出当前时间戳
 *       所归属的时间桶段号（Window Segment）。同一周期内的所有请求映射到相同的 Redis Key，实现离散时间的精准窗口归集。</li>
 *   <li><b>原子自增与配额计算 (Atomic Increment)：</b>
 *       借助 Redis 单线程原子指令 {@code INCR} 实时递增计数。由于 {@code INCR} 操作具备天然的原子性，
 *       能够确保多节点、高并发并发请求到达时不会产生脏读、丢计等并发竞态漏洞。</li>
 *   <li><b>首次写入触发 TTL 自动过期 (Lazy TTL Initialization)：</b>
 *       当 {@code count == 1} 时（即当前时间段内的首个请求到达），通过 {@code EXPIRE key timeWindowSeconds}
 *       为 Key 设定与时间窗口等长的生命周期，实现过期数据的无感自动回收，避免占用 Redis 宝贵的内存空间。</li>
 *   <li><b>高可用故障开路策略 (Fail-Open Resilience Pattern)：</b>
 *       若 Redis 服务发生宕机、超时或网络分区异常，catch 块会捕获异常并返回 {@code true}（放行请求），
 *       确保限流等辅助性非核心业务基础设施的单点故障绝不阻断正常业务的运转。</li>
 * </ul>
 *
 * <p><b>二、已知不足与演进方向：</b>
 * <ul>
 *   <li><b>窗口临界点突发问题 (Window Boundary Burst)：</b>
 *       固定窗口算法在窗口临界切换瞬间（例如前一个周期的最后一秒与后一个周期的第一秒），
 *       理论上最多可能允许 2 倍的 {@code maxRequests} 流量穿透。未来可平滑演进为基于 Redis ZSet 的滑动时间窗口算法或令牌桶算法。</li>
 *   <li><b>原子性事务封装：</b>
 *       当前实现将 {@code INCR} 与 {@code EXPIRE} 分为两步网络往返。若在 {@code INCR} 成功后应用突发崩溃，
 *       可能导致该 Key 缺少 TTL 成为悬挂键。后续可通过 Redis Lua 脚本实现单次往返的原子化绑定执行。</li>
 * </ul>
 *
 * @author Development Team
 * @since 1.0.0
 * @see com.listen.portfolio.common.aspect.RateLimitAspect
 */
@Service
public class RateLimitService {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitService.class);

    /**
     * Redis 限流键统一命名空间前缀
     */
    private static final String RATE_LIMIT_PREFIX = "rate_limit:";

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    /**
     * 校验指定标识符在指定时间窗口内累计请求数是否仍在配额许可范围内。
     *
     * <p><b>核心处理流程：</b>
     * <ol>
     *   <li>计算当前时间所属的时间桶分段：{@code currentWindow = System.currentTimeMillis() / (timeWindowSeconds * 1000)}。</li>
     *   <li>构造 Redis Key：{@code rate_limit:{identifier}:{currentWindow}}。</li>
     *   <li>执行 Redis 原子自增：{@code redisTemplate.opsForValue().increment(key)}。</li>
     *   <li>若返回计数为 1，说明是该窗口期的第一个请求，为其设置过期时间（{@code EXPIRE}）。</li>
     *   <li>判定 {@code count <= maxRequests}，若超限则记录日志并返回 false。</li>
     *   <li>若遇到 Redis 异常，触发 Fail-Open 开路放行，保证核心系统韧性。</li>
     * </ol>
     *
     * @param identifier 统计目标唯一标识符（例如 "ip:192.168.1.100"、"email:user@example.com"）
     * @param maxRequests 时间窗口内允许通过的最大请求数上限
     * @param timeWindowSeconds 时间窗口跨度大小（单位：秒）
     * @return {@code true} 代表在配额内允许通过；{@code false} 代表已达上限，应当拦截
     */
    public boolean isAllowed(String identifier, int maxRequests, int timeWindowSeconds) {
        try {
            // 1. 计算离散时间窗口段号：同一窗口内的毫秒时间戳整除后数值相同
            long currentWindow = System.currentTimeMillis() / (timeWindowSeconds * 1000L);
            String key = RATE_LIMIT_PREFIX + identifier + ":" + currentWindow;

            // 2. 利用 Redis INCR 原子指令递增计数
            Long count = redisTemplate.opsForValue().increment(key);

            if (count == null) {
                logger.warn("Redis increment returned null for key: {}", key);
                return true; // Fail-Open: 返回异常空值时保守放行
            }

            // 3. 仅当第一次自增（count == 1）时初始化 Key 的生存周期 TTL
            if (count == 1) {
                redisTemplate.expire(key, timeWindowSeconds, TimeUnit.SECONDS);
            }

            // 4. 判定当前计数值是否未超出配置上限
            boolean allowed = count <= maxRequests;

            if (!allowed) {
                logger.warn("Rate limit exceeded for identifier: {}, count: {}, limit: {}",
                        identifier, count, maxRequests);
            }

            return allowed;
        } catch (Exception e) {
            logger.error("Rate limit check failed for identifier: {}, error: {}",
                    identifier, e.getMessage());
            return true; // Fail-Open 韧性保障：Redis 故障时优先放行业务请求
        }
    }

    /**
     * 邮箱操作专用快捷限流校验器（主要用于密码找回、邮件发送等敏感场景）。
     * <p>默认策略：单个邮箱地址在 5 分钟（300 秒）内最多允许请求 3 次。
     *
     * @param email 目标邮箱地址
     * @return {@code true} 允许发送；{@code false} 频率过高予以拦截
     */
    public boolean isEmailAllowed(String email) {
        return isAllowed("email:" + email, 3, 300);
    }

    /**
     * 客户端原始 IP 专用快捷限流校验器（主要用于未登录公开端点）。
     * <p>默认策略：单个 IP 地址在 1 分钟（60 秒）内最多允许请求 10 次。
     *
     * @param ip 客户端 IP 地址
     * @return {@code true} 允许访问；{@code false} 访问过频予以拦截
     */
    public boolean isIpAllowed(String ip) {
        return isAllowed("ip:" + ip, 10, 60);
    }

    /**
     * 查询指定标识符在当前时间窗口期内的剩余可用请求配额。
     * <p>通常用于在 HTTP 响应头中注入标准限流状态信息（如 {@code X-RateLimit-Remaining}）。
     *
     * @param identifier 统计目标标识符
     * @param maxRequests 窗口期最大配额上限
     * @param timeWindowSeconds 时间窗口跨度大小（秒）
     * @return 剩余可用请求数（>= 0），若遇到异常则安全回退返回最大配额数
     */
    public int getRemainingRequests(String identifier, int maxRequests, int timeWindowSeconds) {
        try {
            long currentWindow = System.currentTimeMillis() / (timeWindowSeconds * 1000L);
            String key = RATE_LIMIT_PREFIX + identifier + ":" + currentWindow;

            String countStr = redisTemplate.opsForValue().get(key);
            int count = countStr != null ? Integer.parseInt(countStr) : 0;

            return Math.max(0, maxRequests - count);
        } catch (Exception e) {
            logger.error("Failed to get remaining requests for identifier: {}, error: {}",
                    identifier, e.getMessage());
            return maxRequests; // 异常时回退返回完整可用配额
        }
    }
}
