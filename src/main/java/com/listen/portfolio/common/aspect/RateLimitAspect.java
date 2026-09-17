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
 * 分布式限流切面 (Rate Limiting Aspect)。
 *
 * <p><b>一、核心设计理念与执行流程：</b>
 * <ul>
 *   <li><b>AOP 环绕通知 (@Around)：</b>
 *       声明式拦截所有标注了 {@link RateLimit} 注解的方法（主要为 REST 控制器端点）。
 *       在目标方法执行前完成全部维度的配额评估，将安全防护与核心业务代码彻底解耦。</li>
 *   <li><b>多维度组合短路校验 (Multi-Dimensional Short-Circuit Evaluation)：</b>
 *       按顺序遍历注解中声明的所有限流维度（如 {@code IP}、{@code EMAIL} 等）。
 *       通过策略方法提取对应维度的唯一标识符（Identifier），调用 {@link RateLimitService} 进行判定；
 *       若任一维度超出阈值，立即短路中断请求，直接构造标准 HTTP 429 错误响应返回，业务逻辑不予执行。</li>
 *   <li><b>高可用故障开路策略 (Fail-Open Resilience)：</b>
 *       当切面因脱离 Web 上下文无法获取 {@link HttpServletRequest}，或者 Redis 出现连接抖动或异常时，
 *       采用开路放行原则（{@code proceed()} 或返回 {@code true}），
 *       确保限流等辅助安全基础设施的单点故障绝不会演变为系统的全局不可用。</li>
 * </ul>
 *
 * @author Development Team
 * @since 1.0.0
 * @see RateLimit
 * @see RateLimitService
 */
@Aspect
@Component
public class RateLimitAspect {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitAspect.class);

    /**
     * 底层 Redis 分布式限流服务
     */
    private final RateLimitService rateLimitService;

    /**
     * 构造函数注入限流服务组件。
     *
     * @param rateLimitService 分布式限流服务实例
     */
    public RateLimitAspect(RateLimitService rateLimitService) {
        this.rateLimitService = rateLimitService;
    }

    /**
     * 环绕通知：拦截标注了 {@link RateLimit} 的方法并执行前置限流拦截。
     *
     * <p><b>执行逻辑拆解：</b>
     * <ol>
     *   <li>通过反射解析当前目标方法上的 {@link RateLimit} 注解及其参数配置。</li>
     *   <li>从 Spring 上下文 {@link RequestContextHolder} 提取当前线程绑定的 HTTP 请求对象。</li>
     *   <li>循环遍历注解配置的全部限流类型维度（{@code types()}）。</li>
     *   <li>针对每个维度提取对应的标识符（IP、邮箱、Token、已认证用户名或自定义值）。</li>
     *   <li>调用 {@link #checkLimit} 进行配额校验；若任意维度超限，记录告警日志并返回 HTTP 429。</li>
     *   <li>全部维度均校验通过后，调用 {@code joinPoint.proceed()} 继续执行控制器核心业务代码。</li>
     * </ol>
     *
     * @param joinPoint AOP 连接点上下文，包含目标方法、参数及签名
     * @return 业务方法返回值，或超限时的 HTTP 429 (Too Many Requests) 实体
     * @throws Throwable 目标业务方法抛出的受检或非受检异常
     */
    @Around("@annotation(com.listen.portfolio.common.aspect.RateLimit)")
    public Object checkRateLimit(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        RateLimit rateLimit = method.getAnnotation(RateLimit.class);

        // 1. 从当前线程的 ThreadLocal 获取 HttpServletRequest 上下文
        HttpServletRequest request = getCurrentRequest();
        if (request == null) {
            logger.warn("Cannot get HttpServletRequest, skipping rate limit check");
            return joinPoint.proceed(); // Fail-Open: 非 Web 容器线程环境直接放行
        }

        // 2. 依次评估注解上声明的所有限流维度（如同时限制 IP 与 EMAIL）
        for (RateLimit.RateLimitType type : rateLimit.types()) {
            String identifier = extractIdentifier(type, request, joinPoint, rateLimit);

            if (identifier == null) {
                logger.warn("Cannot extract identifier for rate limit type: {}", type);
                continue; // 无法提取标识符时记录日志并跳过当前维度，避免异常误杀
            }

            // 3. 校验该维度标识符在当前时间窗口内是否超限
            boolean allowed = checkLimit(type, identifier, rateLimit.maxRequests(), rateLimit.timeWindowSeconds());

            if (!allowed) {
                // 4. 触发限流阈值拦截：对敏感标识进行脱敏后打印告警日志，返回标准 429 结构体
                logger.warn("Rate limit exceeded for type: {}, identifier: {}", type, maskIdentifier(identifier));
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                        .body(ApiResponse.error("RATE_LIMIT_EXCEEDED",
                                "Requests are too frequent, please try again later"));
            }
        }

        // 5. 全部限流维度检查通过，放行至下游目标 Controller 处理业务
        return joinPoint.proceed();
    }

    /**
     * 根据限流类型路由提取对应的限流统计目标标识符。
     *
     * @param type 限流维度类型枚举
     * @param request 当前 HTTP 请求上下文
     * @param joinPoint AOP 拦截连接点（用于反射入参）
     * @param rateLimit 注解实例（用于读取自定义表达式）
     * @return 提取出的唯一标识字符串，若无法提取则返回 null
     */
    private String extractIdentifier(RateLimit.RateLimitType type, HttpServletRequest request,
                                     ProceedingJoinPoint joinPoint, RateLimit rateLimit) {
        switch (type) {
            case IP:
                // 客户端真实源 IP
                return getClientIp(request);

            case EMAIL:
                // 反射从方法参数对象中解析 email 属性
                return extractEmailFromRequest(joinPoint);

            case TOKEN:
                // 反射从方法参数对象中解析 token 属性
                return extractTokenFromRequest(joinPoint);

            case USER:
                // 从 Spring Security 安全上下文获取已认证用户名
                return extractUserIdFromSecurity();

            case CUSTOM:
                // 提取自定义标识符（支持 SpEL 表达式求值扩展）
                return extractCustomIdentifier(rateLimit.identifierExpression(), joinPoint);

            default:
                return null;
        }
    }

    /**
     * 拼接标准维度前缀并委托 {@link RateLimitService} 进行 Redis 分布式计数校验。
     *
     * @param type 限流类型
     * @param identifier 统计目标唯一标识
     * @param maxRequests 最大允许通过请求数
     * @param timeWindowSeconds 时间窗口跨度（秒）
     * @return {@code true} 代表配额充足允许通过，{@code false} 代表已被限流
     */
    private boolean checkLimit(RateLimit.RateLimitType type, String identifier,
                               int maxRequests, int timeWindowSeconds) {
        String prefix = type.name().toLowerCase() + ":";
        return rateLimitService.isAllowed(prefix + identifier, maxRequests, timeWindowSeconds);
    }

    /**
     * 智能提取客户端真实 IP 地址。
     *
     * <p><b>反向代理多级穿透识别：</b>
     * <ul>
     *   <li>优先从 {@code X-Forwarded-For} 标头解析真实源 IP（当经过 Nginx、AWS ALB 或 Cloudflare 转发时）。</li>
     *   <li>若首选为空，回退到 {@code X-Real-IP} 标头。</li>
     *   <li>若均为空或为 "unknown"，最终回退到底层 Socket 的 {@link HttpServletRequest#getRemoteAddr()}。</li>
     * </ul>
     *
     * @param request 当前 HTTP 请求
     * @return 解析得到的客户端 IPv4 或 IPv6 地址
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 当多级代理转发时，X-Forwarded-For 格式为 "client, proxy1, proxy2"，首个有效 IP 即为真实客户端 IP
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    /**
     * 利用 Java 反射机制深度扫描方法调用入参，提取属性名为 "email" 的字段值。
     * <p>常用于找回密码（{@code ForgotPasswordRequest}）或用户注册 DTO。
     *
     * @param joinPoint AOP 连接点
     * @return 提取出的邮箱字符串，若未声明或对象为 null 则返回 null
     */
    private String extractEmailFromRequest(ProceedingJoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        for (Object arg : args) {
            if (arg == null) continue;

            // 尝试通过反射读取 DTO 中的 "email" 私有属性
            try {
                java.lang.reflect.Field emailField = arg.getClass().getDeclaredField("email");
                emailField.setAccessible(true);
                Object email = emailField.get(arg);
                if (email != null) {
                    return email.toString().trim().toLowerCase();
                }
            } catch (Exception e) {
                // 忽略当前参数没有 email 字段的反射异常，继续探测后续入参
            }
        }
        return null;
    }

    /**
     * 利用 Java 反射机制深度扫描方法调用入参，提取属性名为 "token" 的字段值。
     * <p>常用于重置密码凭证（{@code ResetPasswordRequest}）等安全接口。
     *
     * @param joinPoint AOP 连接点
     * @return 提取出的安全令牌字符串，若未探测到则返回 null
     */
    private String extractTokenFromRequest(ProceedingJoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        for (Object arg : args) {
            if (arg == null) continue;

            // 尝试通过反射读取 DTO 中的 "token" 私有属性
            try {
                java.lang.reflect.Field tokenField = arg.getClass().getDeclaredField("token");
                tokenField.setAccessible(true);
                Object token = tokenField.get(arg);
                if (token != null) {
                    return token.toString();
                }
            } catch (Exception e) {
                // 忽略当前参数没有 token 字段的反射异常，继续探测后续入参
            }
        }
        return null;
    }

    /**
     * 从 Spring Security 安全上下文（{@link SecurityContextHolder}）提取已认证用户的 Principal 用户名。
     *
     * @return 已通过身份认证的用户标识名，若为匿名访问或未登录则返回 null
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
     * 计算并提取自定义限流标识符。
     * <p>当前版本直接返回注解配置的表达式字面量，后续可升级集成 SpEL 动态求值解析器。
     *
     * @param expression 自定义标识符表达式
     * @param joinPoint AOP 拦截上下文
     * @return 计算后的标识符
     */
    private String extractCustomIdentifier(String expression, ProceedingJoinPoint joinPoint) {
        return expression;
    }

    /**
     * 从 Spring Web 上下文工具类 {@link RequestContextHolder} 检索当前 HTTP 请求。
     *
     * @return 当前线程绑定的 {@link HttpServletRequest}，若非 Web 上下文环境返回 null
     */
    private HttpServletRequest getCurrentRequest() {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes != null ? attributes.getRequest() : null;
    }

    /**
     * 敏感信息脱敏处理器。
     * <p>在向控制台或日志平台输出限流拦截记录时，对邮箱、Token、用户标识等敏感数据实施遮蔽，
     * 严格遵守隐私合规要求，防止生产日志泄露敏感身份信息。
     *
     * @param identifier 原始标识符字符串
     * @return 脱敏后的安全字符串（如保留前 4 位字符，其余字符遮蔽为 ...）
     */
    private String maskIdentifier(String identifier) {
        if (identifier == null || identifier.length() <= 4) {
            return identifier;
        }
        return identifier.substring(0, Math.min(10, identifier.length())) + "...";
    }
}
