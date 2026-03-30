package com.listen.portfolio.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 限流服务
 * 
 * 说明：基于 Redis 实现的分布式限流服务
 * 原理：使用 Redis 的 INCR 命令和过期时间实现滑动窗口限流
 * 使用场景：防止 API 暴力访问、保护敏感接口
 */
@Service
public class RateLimitService {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitService.class);
    private static final String RATE_LIMIT_PREFIX = "rate_limit:";

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    /**
     * 检查是否超过限流阈值
     * 
     * 说明：使用 Redis 计数器实现限流检查
     * 原理：
     * 1. 生成限流 key：rate_limit:{identifier}:{timeWindow}
     * 2. 使用 INCR 命令增加计数
     * 3. 如果是第一次访问，设置过期时间
     * 4. 检查计数是否超过限制
     * 
     * @param identifier 限流标识（如 IP 地址、用户 ID、邮箱等）
     * @param maxRequests 时间窗口内最大请求数
     * @param timeWindowSeconds 时间窗口（秒）
     * @return true 表示允许访问，false 表示超过限制
     */
    public boolean isAllowed(String identifier, int maxRequests, int timeWindowSeconds) {
        try {
            // 生成限流 key，包含时间窗口信息
            long currentWindow = System.currentTimeMillis() / (timeWindowSeconds * 1000);
            String key = RATE_LIMIT_PREFIX + identifier + ":" + currentWindow;

            // 增加计数
            Long count = redisTemplate.opsForValue().increment(key);
            
            if (count == null) {
                logger.warn("Redis increment returned null for key: {}", key);
                return true; // Redis 故障时允许访问，避免影响正常用户
            }

            // 如果是第一次访问，设置过期时间
            if (count == 1) {
                redisTemplate.expire(key, timeWindowSeconds, TimeUnit.SECONDS);
            }

            boolean allowed = count <= maxRequests;
            
            if (!allowed) {
                logger.warn("Rate limit exceeded for identifier: {}, count: {}, limit: {}", 
                          identifier, count, maxRequests);
            }

            return allowed;
        } catch (Exception e) {
            logger.error("Rate limit check failed for identifier: {}, error: {}", 
                       identifier, e.getMessage());
            return true; // 发生异常时允许访问，避免影响正常用户
        }
    }

    /**
     * 检查邮箱是否超过限流阈值
     * 
     * 说明：专门用于邮箱相关操作的限流
     * 默认：5 分钟内最多 3 次
     * 
     * @param email 邮箱地址
     * @return true 表示允许访问，false 表示超过限制
     */
    public boolean isEmailAllowed(String email) {
        return isAllowed("email:" + email, 3, 300); // 5 分钟内最多 3 次
    }

    /**
     * 检查 IP 是否超过限流阈值
     * 
     * 说明：专门用于 IP 地址的限流
     * 默认：1 分钟内最多 10 次
     * 
     * @param ip IP 地址
     * @return true 表示允许访问，false 表示超过限制
     */
    public boolean isIpAllowed(String ip) {
        return isAllowed("ip:" + ip, 10, 60); // 1 分钟内最多 10 次
    }

    /**
     * 获取剩余请求次数
     * 
     * @param identifier 限流标识
     * @param maxRequests 最大请求数
     * @param timeWindowSeconds 时间窗口（秒）
     * @return 剩余请求次数
     */
    public int getRemainingRequests(String identifier, int maxRequests, int timeWindowSeconds) {
        try {
            long currentWindow = System.currentTimeMillis() / (timeWindowSeconds * 1000);
            String key = RATE_LIMIT_PREFIX + identifier + ":" + currentWindow;

            String countStr = redisTemplate.opsForValue().get(key);
            int count = countStr != null ? Integer.parseInt(countStr) : 0;

            return Math.max(0, maxRequests - count);
        } catch (Exception e) {
            logger.error("Failed to get remaining requests for identifier: {}, error: {}", 
                       identifier, e.getMessage());
            return maxRequests; // 发生异常时返回最大值
        }
    }
}
