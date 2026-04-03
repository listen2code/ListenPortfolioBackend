package com.listen.portfolio.common.aspect;

import com.listen.portfolio.common.ApiResponse;
import com.listen.portfolio.service.RateLimitService;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;

/**
 * 限流切面
 * 
 * 说明：统一处理 @RateLimit 注解标记的方法的限流逻辑
 * 原理：使用 AOP 环绕通知，在方法执行前进行限流检查
 * 优势：
 * - 代码复用：避免在每个接口重复编写限流代码
 * - 统一管理：所有限流逻辑集中在一处
 * - 灵活配置：通过注解参数灵活配置限流规则
 */
@Aspect
@Component
public class RateLimitAspect {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitAspect.class);
    
    private final RateLimitService rateLimitService;

    public RateLimitAspect(RateLimitService rateLimitService) {
        this.rateLimitService = rateLimitService;
    }

    /**
     * 环绕通知：拦截所有带 @RateLimit 注解的方法
     */
    @Around("@annotation(com.listen.portfolio.common.RateLimit)")
    public Object checkRateLimit(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        RateLimit rateLimit = method.getAnnotation(RateLimit.class);

        // 获取 HttpServletRequest
        HttpServletRequest request = getCurrentRequest();
        if (request == null) {
            logger.warn("Cannot get HttpServletRequest, skipping rate limit check");
            return joinPoint.proceed();
        }

        // 检查每种限流类型
        for (RateLimit.RateLimitType type : rateLimit.types()) {
            String identifier = extractIdentifier(type, request, joinPoint, rateLimit);
            
            if (identifier == null) {
                logger.warn("Cannot extract identifier for rate limit type: {}", type);
                continue;
            }

            boolean allowed = checkLimit(type, identifier, rateLimit.maxRequests(), rateLimit.timeWindowSeconds());
            
            if (!allowed) {
                logger.warn("Rate limit exceeded for type: {}, identifier: {}", type, maskIdentifier(identifier));
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                        .body(ApiResponse.error("RATE_LIMIT_EXCEEDED", 
                              "Requests are too frequent, please try again later"));
            }
        }

        // 限流检查通过，执行原方法
        return joinPoint.proceed();
    }

    /**
     * 提取限流标识符
     */
    private String extractIdentifier(RateLimit.RateLimitType type, HttpServletRequest request, 
                                     ProceedingJoinPoint joinPoint, RateLimit rateLimit) {
        switch (type) {
            case IP:
                return getClientIp(request);
                
            case EMAIL:
                return extractEmailFromRequest(joinPoint);
                
            case TOKEN:
                return extractTokenFromRequest(joinPoint);
                
            case USER:
                return extractUserIdFromSecurity();
                
            case CUSTOM:
                // 自定义标识符提取（可以使用 SpEL 表达式）
                return extractCustomIdentifier(rateLimit.identifierExpression(), joinPoint);
                
            default:
                return null;
        }
    }

    /**
     * 执行限流检查
     */
    private boolean checkLimit(RateLimit.RateLimitType type, String identifier, 
                               int maxRequests, int timeWindowSeconds) {
        String prefix = type.name().toLowerCase() + ":";
        return rateLimitService.isAllowed(prefix + identifier, maxRequests, timeWindowSeconds);
    }

    /**
     * 获取客户端 IP
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }

    /**
     * 从请求参数中提取邮箱
     */
    private String extractEmailFromRequest(ProceedingJoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        for (Object arg : args) {
            if (arg == null) continue;
            
            // 尝试通过反射获取 email 字段
            try {
                java.lang.reflect.Field emailField = arg.getClass().getDeclaredField("email");
                emailField.setAccessible(true);
                Object email = emailField.get(arg);
                if (email != null) {
                    return email.toString();
                }
            } catch (Exception e) {
                // 忽略，继续尝试下一个参数
            }
        }
        return null;
    }

    /**
     * 从请求参数中提取 token
     */
    private String extractTokenFromRequest(ProceedingJoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        for (Object arg : args) {
            if (arg == null) continue;
            
            // 尝试通过反射获取 token 字段
            try {
                java.lang.reflect.Field tokenField = arg.getClass().getDeclaredField("token");
                tokenField.setAccessible(true);
                Object token = tokenField.get(arg);
                if (token != null) {
                    return token.toString();
                }
            } catch (Exception e) {
                // 忽略，继续尝试下一个参数
            }
        }
        return null;
    }

    /**
     * 从 Spring Security 上下文中提取用户 ID
     */
    private String extractUserIdFromSecurity() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() 
            && !"anonymousUser".equals(authentication.getPrincipal())) {
            return authentication.getName();
        }
        return null;
    }

    /**
     * 提取自定义标识符（使用 SpEL 表达式）
     */
    private String extractCustomIdentifier(String expression, ProceedingJoinPoint joinPoint) {
        // 简化实现：直接返回表达式作为标识符
        // 完整实现可以使用 Spring Expression Language (SpEL)
        return expression;
    }

    /**
     * 获取当前请求
     */
    private HttpServletRequest getCurrentRequest() {
        ServletRequestAttributes attributes = 
            (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes != null ? attributes.getRequest() : null;
    }

    /**
     * 掩码标识符（用于日志，保护隐私）
     */
    private String maskIdentifier(String identifier) {
        if (identifier == null || identifier.length() <= 4) {
            return identifier;
        }
        return identifier.substring(0, Math.min(10, identifier.length())) + "...";
    }
}
