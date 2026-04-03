package com.listen.portfolio.common.aspect;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 限流注解
 * 
 * 说明：用于标记需要限流的接口方法
 * 使用场景：在 Controller 方法上添加此注解，自动应用限流保护
 * 
 * 示例：
 * <pre>
 * @RateLimit(type = RateLimitType.IP, maxRequests = 10, timeWindowSeconds = 60)
 * public ResponseEntity<?> login(@RequestBody LoginRequest request) {
 *     // ...
 * }
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {
    
    /**
     * 限流类型
     */
    RateLimitType[] types() default {RateLimitType.IP};
    
    /**
     * 时间窗口内最大请求数
     */
    int maxRequests() default 10;
    
    /**
     * 时间窗口（秒）
     */
    int timeWindowSeconds() default 60;
    
    /**
     * 限流标识符提取器
     * 用于从请求中提取限流标识（如邮箱、token 等）
     * 支持 SpEL 表达式
     */
    String identifierExpression() default "";
    
    /**
     * 限流类型枚举
     */
    enum RateLimitType {
        /**
         * 基于 IP 地址限流
         */
        IP,
        
        /**
         * 基于邮箱限流
         */
        EMAIL,
        
        /**
         * 基于 Token 限流
         */
        TOKEN,
        
        /**
         * 基于用户 ID 限流（需要登录）
         */
        USER,
        
        /**
         * 自定义限流（使用 identifierExpression）
         */
        CUSTOM
    }
}
