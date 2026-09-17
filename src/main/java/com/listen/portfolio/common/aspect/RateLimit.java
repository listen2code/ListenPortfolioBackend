package com.listen.portfolio.common.aspect;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 分布式智能限流注解 (Distributed Rate Limit Annotation)。
 *
 * <p><b>一、设计理念与定位：</b>
 * <ul>
 *   <li><b>声明式零侵入设计 (Declarative & Non-Intrusive)：</b>
 *       开发者无需在业务 Controller 或 Service 内部编写繁琐的计数器或 Redis 交互逻辑，
 *       仅需在目标接口方法上标注本注解，由 {@link RateLimitAspect} 切面在请求执行前自动拦截并完成多维度鉴权校验。</li>
 *   <li><b>多维度组合防御 (Multi-Dimensional Defense)：</b>
 *       单接口支持同时配置多个限流维度（例如 {@code types = {IP, EMAIL}}）。
 *       任一维度触发超限阈值均会立即熔断请求并返回 HTTP 429 (Too Many Requests)，
 *       有效阻止单 IP 爆破、分布式爬虫撞库、恶意脚本遍历枚举邮箱或短信/邮件轰炸。</li>
 *   <li><b>高并发分布式协同 (Distributed Synchronization)：</b>
 *       底层计数器依托于统一 Redis 基础设施，各实例无状态部署，
 *       天然支持水平扩展（Scale-Out）与集群负载均衡下的全集群流量配额共享。</li>
 * </ul>
 *
 * <p><b>二、典型使用场景与配置范例：</b>
 * <ul>
 *   <li><b>场景 1：用户登录接口防暴力破解 (IP 维度限流)</b>
 *       <pre>{@code
 *       @PostMapping("/login")
 *       @RateLimit(types = {RateLimit.RateLimitType.IP}, maxRequests = 10, timeWindowSeconds = 60)
 *       public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
 *           // 允许单个 IP 每分钟最多发起 10 次密码尝试
 *       }
 *       }</pre>
 *   </li>
 *   <li><b>场景 2：找回密码/发送验证码接口 (IP + 邮箱双重组合限流)</b>
 *       <pre>{@code
 *       @PostMapping("/forgot-password")
 *       @RateLimit(
 *           types = {RateLimit.RateLimitType.IP, RateLimit.RateLimitType.EMAIL},
 *           maxRequests = 5,
 *           timeWindowSeconds = 300
 *       )
 *       public ResponseEntity<ApiResponse<Void>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
 *           // 5 分钟内同一 IP 限制 5 次，同一目标邮箱亦限制 5 次，双管齐下防止撞库与邮件服务资源耗尽
 *       }
 *       }</pre>
 *   </li>
 *   <li><b>场景 3：已登录敏感资源修改 (用户 ID 维度限流)</b>
 *       <pre>{@code
 *       @PutMapping("/profile")
 *       @RateLimit(types = {RateLimit.RateLimitType.USER}, maxRequests = 100, timeWindowSeconds = 3600)
 *       public ResponseEntity<ApiResponse<UserDto>> updateProfile(@Valid @RequestBody UserUpdateRequest request) {
 *           // 单个登录用户 1 小时内最多允许修改 100 次
 *       }
 *       }</pre>
 *   </li>
 * </ul>
 *
 * @author Development Team
 * @since 1.0.0
 * @see RateLimitAspect
 * @see com.listen.portfolio.service.RateLimitService
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {

    /**
     * 限流维度类型列表。
     * <p>默认为按客户端真实 IP 进行限流。支持指定多个维度，此时系统将对每个维度逐一求值并校验，
     * 只要其中任何一个维度达到阈值，即刻拦截请求并返回 HTTP 429。
     *
     * @return 限流类型枚举数组
     */
    RateLimitType[] types() default {RateLimitType.IP};

    /**
     * 单个时间窗口内允许通过的最大请求数（配额容量）。
     * <p>当指定维度在当前时间窗口内的累计请求数超过该数值时，后续请求将被切面阻断。
     * 默认每个窗口允许 10 次请求。
     *
     * @return 最大请求次数阈值（必须大于 0）
     */
    int maxRequests() default 10;

    /**
     * 限流统计的时间窗口周期（单位：秒）。
     * <p>系统基于固定时间窗口段（Fixed Window Segment）计算当前请求所属的时间桶：
     * {@code windowBucket = System.currentTimeMillis() / (timeWindowSeconds * 1000)}。
     * 默认时间窗口为 60 秒（1 分钟）。
     *
     * @return 时间窗口跨度（秒）
     */
    int timeWindowSeconds() default 60;

    /**
     * 自定义限流标识符提取表达式（用于 {@link RateLimitType#CUSTOM}）。
     * <p>支持指定固定键名或后续扩展 Spring SpEL（Spring Expression Language）动态求值表达式，
     * 例如从请求参数对象中提取租户标识符 {@code "#request.tenantId"} 或自定义 Header。
     *
     * @return 提取表达式字符串，默认为空字符串
     */
    String identifierExpression() default "";

    /**
     * 限流维度类型枚举定义。
     */
    enum RateLimitType {
        /**
         * <b>基于客户端真实 IP 地址限流 (Client IP Address)：</b>
         * <p>适用于未登录公开接口（如登录、注册、验证码发放等）。
         * 切面会智能解析 Nginx/ALB 反向代理传递的 {@code X-Forwarded-For} 与 {@code X-Real-IP} 请求头，
         * 提取真实的客户端源 IP，防止单 IP 高频暴力破解或恶意探测。
         */
        IP,

        /**
         * <b>基于目标邮箱地址限流 (Email Address)：</b>
         * <p>适用于忘记密码、邮箱验证、垃圾邮件防刷等业务。
         * 切面会通过反射自动从入参 DTO 中检索 {@code email} 属性并作为统计键，
         * 杜绝恶意黑客变换大量代理 IP 对同一受害用户邮箱实施泛洪轰炸。
         */
        EMAIL,

        /**
         * <b>基于安全凭证/令牌限流 (Security Token)：</b>
         * <p>适用于重置密码确认链接、单次安全票据校验等接口。
         * 切面通过反射从入参中提取 {@code token} 属性并进行限流，防止针对特定令牌实施自动化暴力猜测。
         */
        TOKEN,

        /**
         * <b>基于已登录用户身份限流 (Authenticated User ID)：</b>
         * <p>适用于需要 JWT 认证的受保护接口（如修改个人资料、发表作品、上传附件等）。
         * 切面从 Spring Security {@code SecurityContextHolder} 中获取当前已认证用户的唯一标识（User Name / User ID），
         * 针对单一登录账号进行频率管控，防止盗号后的自动化灌水或恶意滥用。
         */
        USER,

        /**
         * <b>自定义业务标识符限流 (Custom Identifier / SpEL)：</b>
         * <p>配合 {@link RateLimit#identifierExpression()} 使用，支持按自定义业务标签、
         * 多租户 ID、设备指纹等维度进行高度定制化的流量整形与防护。
         */
        CUSTOM
    }
}
