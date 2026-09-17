package com.listen.portfolio.common.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.listen.portfolio.common.ApiResponse;
import com.listen.portfolio.service.TokenBlacklistService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * JWT 访问令牌拦截验证过滤器 (JWT Request Filter)。
 *
 * <p><b>一、组件职责与运行生命周期：</b>
 * <ul>
 *   <li><b>单次请求仅执行一次 (OncePerRequestFilter)：</b>
 *       继承 Spring 提供的 {@link OncePerRequestFilter}，确保在任何 Servlet 容器转发（Forward / Include）
 *       或过滤器链嵌套调用中，当前过滤器针对每个独立的 HTTP 请求线程有且仅执行一次内部逻辑。</li>
 *   <li><b>请求头解析与凭据提取 (Bearer Token Extraction)：</b>
 *       从 HTTP {@code Authorization} 头中检索是否以标准前缀 {@code "Bearer "} 开头，提取实际的紧凑 JWT 字符串。</li>
 *   <li><b>多重安全防线校验链路 (Multi-Tier Security Validation)：</b>
 *       <ol>
 *         <li><b>第一道防线：JWT 结构与加密签名核验</b>（利用 {@link JwtUtil} 校验 HMAC-SHA256 签名与有效时间戳）。</li>
 *         <li><b>第二道防线：Redis 实时黑名单探测</b>（检查 Token 是否因用户登出、修改密码或软删除而被主动注销）。</li>
 *         <li><b>第三道防线：用户数据库存在性与状态校验</b>（调用 {@link UserDetailsService} 确认用户未被停用或物理删除）。</li>
 *       </ol>
 *   </li>
 *   <li><b>安全上下文上下文注入 (SecurityContext Injection)：</b>
 *       校验通过后，构造标准的 {@link UsernamePasswordAuthenticationToken} 并绑定至当前请求线程的
 *       {@link SecurityContextHolder}，供后续业务切面、权限注解（{@code @PreAuthorize}）与 Controller 使用。</li>
 *   <li><b>统一非阻断式 JSON 401 响应 (Standardized 401 JSON Writer)：</b>
 *       若携带了非法、篡改、过期或黑名单中的 Token，立即就地中断过滤器链，
 *       向响应流直接输出规范化统一响应体 {@link ApiResponse}，避免出现 Spring Security 默认的 403 页面或空白响应。</li>
 * </ul>
 *
 * @author Development Team
 * @since 1.0.0
 * @see JwtUtil
 * @see TokenBlacklistService
 * @see com.listen.portfolio.common.config.SecurityConfig
 */
@Component
public class JwtRequestFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtRequestFilter.class);

    /**
     * 用户详情加载服务组件，用于从数据存储拉取用户安全凭据
     */
    @Autowired
    private UserDetailsService userDetailsService;

    /**
     * JWT 核心工具组件，负责令牌解码、签名校验与声明提取
     */
    @Autowired
    private JwtUtil jwtUtil;

    /**
     * Redis 分布式黑名单服务，用于主动吊销处于有效期内的 Access Token
     */
    @Autowired
    private TokenBlacklistService tokenBlacklistService;

    /**
     * 核心过滤处理逻辑：对每个到达的 HTTP 请求执行 JWT 令牌检验与身份上下文绑定。
     *
     * @param request 当前 HTTP 请求对象
     * @param response 当前 HTTP 响应对象
     * @param chain 过滤器调用链
     * @throws ServletException Servlet 异常
     * @throws IOException I/O 读写异常
     */
    @Override
    public void doFilterInternal(HttpServletRequest request,
                                 HttpServletResponse response,
                                 FilterChain chain)
            throws ServletException, IOException {

        logger.debug("Start JWT filter for URI: {}", request.getRequestURI());

        // 1. 尝试从请求头中提取标准 Authorization 字段
        final String authorizationHeader = request.getHeader("Authorization");

        String username = null;
        String jwt = null;

        // 2. 严格核查 "Bearer " 规范前缀（区分大小写，后接 1 个空格）
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            jwt = authorizationHeader.substring(7); // 剥离 "Bearer " 前缀获取纯密文
            logger.debug("Extracted JWT from Authorization header");

            try {
                // 3. 解析 Token 声明并提取 Subject 用户名（若签名错误、格式损毁或已过期会在此抛出 JwtException）
                username = jwtUtil.extractUsername(jwt);
                logger.debug("Extracted username from JWT: {}", username);
            } catch (Exception e) {
                // 异常防御：当 Token 解析失败（如篡改或过期）时，返回明确规范的 401 统一错误响应
                logger.warn("Invalid JWT, cannot extract username: {}", e.getMessage());
                writeErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "401", "Invalid or expired token");
                return;
            }
        } else {
            // 未携带 Authorization 标头或格式非 Bearer：放行给下游，由 SecurityConfig 决定是否拦截未认证请求
            logger.debug("No valid Authorization header present");
        }

        // 4. 若成功解析出用户名，且当前线程的 SecurityContext 尚未被认证（避免同一次请求内重复查库校验）
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            logger.debug("Username extracted and context unauthenticated, validating token");

            try {
                // 5. 从数据库加载最新的用户实体信息
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);
                logger.debug("Loaded user details for: {}", username);

                // 6. 核心防线：检查该 Token 是否在 Redis 吊销黑名单中（例如用户刚刚点击了登出或修改了密码）
                if (tokenBlacklistService.isBlacklisted(jwt)) {
                    logger.warn("Token is blacklisted for user: {}", username);
                    writeErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "401", "Token has been invalidated");
                    return;
                }

                // 7. 严格核验 Token 的用户名匹配性与过期时间戳
                if (jwtUtil.validateToken(jwt, userDetails)) {
                    logger.debug("JWT validated successfully, user {} authenticated", username);

                    // 8. 组装 Spring Security 内部认证凭据对象
                    UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,                  // Principal: 当前用户安全上下文对象
                                    null,                         // Credentials: 无状态 JWT 模式下置空，避免内存残留敏感密码
                                    userDetails.getAuthorities()  // Granted Authorities: 权限集合
                            );

                    // 9. 附加当前 Web 请求的远程 IP、会话 ID 等元数据
                    usernamePasswordAuthenticationToken
                            .setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    // 10. 将认证通过的 Authentication 绑定到当前线程的 ThreadLocal 上下文中
                    SecurityContextHolder.getContext().setAuthentication(usernamePasswordAuthenticationToken);
                    logger.debug("Authentication set in security context for user {}", username);
                } else {
                    logger.warn("JWT validation failed for user: {}", username);
                    writeErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "401", "Invalid or expired token");
                    return;
                }
            } catch (UsernameNotFoundException e) {
                // 用户已被数据库删除或账号不存在
                logger.warn("User not found: {}", username);
                writeErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "401", "User not found");
                return;
            }
        } else {
            logger.debug("Skip JWT validation: username is null or context already authenticated");
        }

        // 11. 校验顺利通过，将请求交由安全过滤器链中的下一个过滤器（如 AuthorizationFilter）处理
        logger.debug("JWT filter completed, proceed with filter chain");
        chain.doFilter(request, response);
    }

    /**
     * 向 HTTP 响应流中直接写入标准统一格式的 JSON 错误响应体。
     *
     * <p>确保客户端收到的认证错误信息具有一致的 {@link ApiResponse} 数据结构，
     * 避免因 Spring Security 默认行为产生歧义的 HTML 报错页面或空体。
     *
     * @param response HTTP 响应对象
     * @param status HTTP 状态码（通常为 401 Unauthorized）
     * @param messageId 业务统一错误标识符（如 "401"）
     * @param message 面向调用方的错误提示文案
     * @throws IOException 响应输出流写入异常
     */
    private void writeErrorResponse(HttpServletResponse response, int status, String messageId, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        ApiResponse<String> apiResponse = ApiResponse.error(messageId, message);
        String json = new ObjectMapper().writeValueAsString(apiResponse);
        response.getWriter().write(json);
    }
}
