package com.listen.portfolio.jwt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
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
        logger.debug("开始处理 JWT 过滤器请求: {}", request.getRequestURI());
        
        // 从请求头中获取 Authorization 头的值
        final String authorizationHeader = request.getHeader("Authorization");

        // 初始化用户名和 JWT 令牌变量
        String username = null;
        String jwt = null;

        // 检查 Authorization 头是否存在且以 "Bearer " 开头
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            // 提取 JWT 令牌（去掉 "Bearer " 前缀）
            jwt = authorizationHeader.substring(7);
            logger.debug("从请求头提取到 JWT 令牌");
            
            try {
                // 使用 JWT 工具从令牌中提取用户名
                username = jwtUtil.extractUsername(jwt);
                logger.debug("从 JWT 令牌中提取用户名: {}", username);
            } catch (Exception e) {
                // 如果令牌无效，记录错误并继续（不抛出异常）
                logger.warn("JWT 令牌无效，无法提取用户名: {}", e.getMessage());
            }
        } else {
            // 如果没有有效的 Authorization 头，记录警告
            logger.debug("请求中没有有效的 Authorization 头");
        }

        // 如果用户名已提取且当前上下文没有认证信息
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            logger.debug("用户名已提取且上下文未认证，开始验证令牌");
            
            // 从数据库加载用户详情
            UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);
            logger.debug("从数据库加载用户详情: {}", username);

            // 验证 JWT 令牌是否有效
            if (jwtUtil.validateToken(jwt, userDetails)) {
                logger.info("JWT 令牌验证成功，用户 {} 已认证", username);
                
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
                logger.debug("用户 {} 的认证信息已设置到安全上下文中", username);
            } else {
                // 令牌验证失败，记录警告
                logger.warn("JWT 令牌验证失败，用户: {}", username);
            }
        } else {
            // 如果没有用户名或已认证，记录调试信息
            logger.debug("跳过 JWT 验证：用户名为空或上下文已认证");
        }
        
        // 继续过滤器链，将请求传递给下一个过滤器或控制器
        logger.debug("JWT 过滤器处理完成，继续过滤器链");
        chain.doFilter(request, response);
    }
}
