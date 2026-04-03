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
 * JWT 请求过滤器
 * 
 * 此过滤器拦截每个 HTTP 请求以验证 JWT 令牌。
 * 每个请求只运行一次（因此是 OncePerRequestFilter）。
 * 
 * 处理流程：
 * 1. 从 Authorization 头中提取 JWT 令牌
 * 2. 验证令牌（检查签名和过期时间）
 * 3. 如果有效，提取用户名并加载用户详情
 * 4. 在 Spring Security 上下文中设置用户
 * 5. 允许请求继续到控制器
 * 
 * 如果令牌无效或缺失（对于受保护的端点），请求将被拒绝。
 */
@Component
public class JwtRequestFilter extends OncePerRequestFilter {

    // 日志记录器，用于记录过滤器的操作
    private static final Logger logger = LoggerFactory.getLogger(JwtRequestFilter.class);

    /**
     * 用户详情服务，用于从数据库加载用户信息
     */
    @Autowired
    private UserDetailsService userDetailsService;

    /**
     * JWT 工具类，用于令牌验证和用户名提取
     */
    @Autowired
    private JwtUtil jwtUtil;
    
    /**
     * Token 黑名单服务，用于检查 token 是否已被登出
     */
    @Autowired
    private TokenBlacklistService tokenBlacklistService;

    /**
     * 此方法每个 HTTP 请求调用一次。
     * 拦截请求，验证 JWT 令牌，并设置认证用户。
     * 
     * @param request HTTP 请求
     * @param response HTTP 响应
     * @param chain 过滤器链，用于传递请求
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                    HttpServletResponse response, 
                                    FilterChain chain) 
            throws ServletException, IOException {
        
        // 记录开始处理请求
        logger.debug("Start JWT filter for URI: {}", request.getRequestURI());
        
        // 从请求头中获取 Authorization 头的值
        final String authorizationHeader = request.getHeader("Authorization");

        // 初始化用户名和 JWT 令牌变量
        String username = null;
        String jwt = null;

        // 检查 Authorization 头是否存在且以 "Bearer " 开头
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            // 提取 JWT 令牌（去掉 "Bearer " 前缀）
            jwt = authorizationHeader.substring(7);
            logger.debug("Extracted JWT from Authorization header");
            
            try {
                // 使用 JWT 工具从令牌中提取用户名
                username = jwtUtil.extractUsername(jwt);
                logger.debug("Extracted username from JWT: {}", username);
            } catch (Exception e) {
                // 如果令牌无效，返回标准 ApiResponse
                logger.warn("Invalid JWT, cannot extract username: {}", e.getMessage());
                writeErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "401", "Invalid or expired token");
                return;
            }
        } else {
            // 如果没有有效的 Authorization 头，记录警告
            logger.debug("No valid Authorization header present");
        }

        // 如果用户名已提取且当前上下文没有认证信息
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            logger.debug("Username extracted and context unauthenticated, validating token");
            
            try {
                // 从数据库加载用户详情
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);
                logger.debug("Loaded user details for: {}", username);

                // 检查 token 是否在黑名单中
                if (tokenBlacklistService.isBlacklisted(jwt)) {
                    logger.warn("Token is blacklisted for user: {}", username);
                    writeErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "401", "Token has been invalidated");
                    return;
                }
                
                // 验证 JWT 令牌是否有效
                if (jwtUtil.validateToken(jwt, userDetails)) {
                    logger.debug("JWT validated successfully, user {} authenticated", username);
                    
                    // 创建认证令牌
                    UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = new UsernamePasswordAuthenticationToken(
                            userDetails, // 用户详情作为主体
                            null, // 凭据（密码）为 null，因为使用 JWT
                            userDetails.getAuthorities() // 用户权限
                    );
                    
                    // 设置认证令牌的详细信息（包括请求信息）
                    usernamePasswordAuthenticationToken
                            .setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    
                    // 将认证信息设置到 Spring Security 上下文中
                    SecurityContextHolder.getContext().setAuthentication(usernamePasswordAuthenticationToken);
                    logger.debug("Authentication set in security context for user {}", username);
                } else {
                    // 令牌验证失败，返回标准 ApiResponse
                    logger.warn("JWT validation failed for user: {}", username);
                    writeErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "401", "Invalid or expired token");
                    return;
                }
            } catch (UsernameNotFoundException e) {
                // 用户不存在，返回标准 ApiResponse
                logger.warn("User not found: {}", username);
                writeErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "401", "User not found");
                return;
            }
        } else {
            // 如果没有用户名或已认证，记录调试信息
            logger.debug("Skip JWT validation: username is null or context already authenticated");
        }
        
        // 继续过滤器链，将请求传递给下一个过滤器或控制器
        logger.debug("JWT filter completed, proceed with filter chain");
        chain.doFilter(request, response);
    }

    private void writeErrorResponse(HttpServletResponse response, int status, String messageId, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        ApiResponse<String> apiResponse = ApiResponse.error(messageId, message);
        String json = new ObjectMapper().writeValueAsString(apiResponse);
        response.getWriter().write(json);
    }
}
