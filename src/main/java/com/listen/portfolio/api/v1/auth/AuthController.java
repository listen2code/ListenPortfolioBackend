package com.listen.portfolio.api.v1.auth;

import com.listen.portfolio.jwt.JwtUtil;
import com.listen.portfolio.api.v1.auth.dto.LoginRequest;
import com.listen.portfolio.api.v1.auth.dto.LoginResponse;
import com.listen.portfolio.api.v1.auth.dto.ForgotPasswordRequest;
import com.listen.portfolio.api.v1.auth.dto.SignUpRequest;
import com.listen.portfolio.common.ApiResponse;
import com.listen.portfolio.common.Constants;
import com.listen.portfolio.service.AuthService;
import com.listen.portfolio.service.RateLimitService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/v1/auth")
/**
 * Auth API（v1）。
 *
 * 目的：
 * 1) 以"功能模块"组织包结构：api/v1/auth
 * 2) 保持对外接口路径不变，逐步迁移，不影响既存功能
 */
@Validated
@Tag(name = "Auth", description = "Authentication and account management APIs")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;
    private final RateLimitService rateLimitService;

    /**
     * 构造函数 - 全依赖注入
     * 说明：通过构造函数注入所有依赖，便于单元测试和依赖管理
     * 设计优势：
     * - 依赖明确：所有依赖都在构造函数中声明，便于理解和维护
     * - 测试友好：构造函数注入便于在单元测试中进行 Mock
     * - 不可变性：使用 final 修饰符确保依赖不会被意外修改
     * - 线程安全：final 字段保证了线程安全性
     * 
     * @param authService 认证服务，处理用户注册、密码管理等业务逻辑
     * @param authenticationManager Spring Security 认证管理器，处理用户认证
     * @param jwtUtil JWT 工具类，处理令牌生成和验证
     * @param userDetailsService Spring Security 用户详情服务，加载用户信息
     * @param rateLimitService 限流服务，防止 API 暴力访问
     */
    public AuthController(
            AuthService authService,
            AuthenticationManager authenticationManager,
            JwtUtil jwtUtil,
            UserDetailsService userDetailsService,
            RateLimitService rateLimitService) {
        this.authService = authService;
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
        this.rateLimitService = rateLimitService;
    }

    
    @PostMapping("/signUp")
    @Operation(summary = "Sign up", description = "Register a new user account")
    public ResponseEntity<ApiResponse<Object>> signUp(@Valid @RequestBody SignUpRequest signUpRequest) {
        logger.info("Received sign-up request, user: {}", signUpRequest.getUserName());

        boolean success = authService.signUp(signUpRequest);

        if (success) {
            logger.info("User {} signed up successfully", signUpRequest.getUserName());
            return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(null));
        }

        logger.warn("Username {} already exists, sign-up failed", signUpRequest.getUserName());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(Constants.DEFAULT_SERVER_ERROR, "Username already exists"));
    }

    @PostMapping("/login")
    @Operation(summary = "Login", description = "Authenticate user and issue JWT access token and refresh token")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest LoginRequest) {
        logger.info("Received login request, user: {}", LoginRequest.getUserName());

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            LoginRequest.getUserName(),
                            LoginRequest.getPassword()
                    )
            );
            logger.info("Credentials verified for user {}", LoginRequest.getUserName());
        } catch (BadCredentialsException e) {
            // 登录失败统一按"凭据无效"处理，避免区分"用户不存在/密码错误"导致用户枚举风险
            // 记录 user 与 reason，便于排查（不会打印密码/token 等敏感信息）
            logger.warn("Invalid credentials for user {}, reason: {}", LoginRequest.getUserName(), e.getMessage());
            return ResponseEntity.status(HttpStatus.OK)
                    .body(ApiResponse.error(Constants.DEFAULT_SERVER_ERROR, "Invalid credentials"));
        }

        final UserDetails userDetails = userDetailsService.loadUserByUsername(LoginRequest.getUserName());
        final String jwt = jwtUtil.generateToken(userDetails);
        final String refreshToken = jwtUtil.generateRefreshToken(jwt);

        final Long userId = authService.getUserByName(LoginRequest.getUserName())
                .map(u -> u.getId())
                .orElse(null);

        logger.info("User {} logged in successfully", LoginRequest.getUserName());
        return ResponseEntity.ok(ApiResponse.success(new LoginResponse(jwt, refreshToken, userId)));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh token", description = "Refresh JWT access token using refresh token")
    public ResponseEntity<ApiResponse<?>> refresh(@RequestParam @NotBlank(message = "refreshToken must not be blank") String refreshToken) {
        logger.info("Received token refresh request");

        String jwt = jwtUtil.refreshToken(refreshToken);
        String newRefreshToken = jwtUtil.generateRefreshToken(jwt);

        String username = jwtUtil.extractUsername(refreshToken);
        Long userId = authService.getUserByName(username)
                .map(u -> u.getId())
                .orElse(null);

        logger.info("Token refreshed successfully, user: {}", username);
        return ResponseEntity.ok(ApiResponse.success(new LoginResponse(jwt, newRefreshToken, userId)));
    }
    
    @PostMapping("/forgot-password")
    @Operation(summary = "Forgot password", description = "Send password reset email to user")
    public ResponseEntity<ApiResponse<Object>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest forgotPasswordRequest,
            HttpServletRequest request) {
        
        String email = forgotPasswordRequest.getEmail();
        String clientIp = getClientIp(request);
        
        logger.info("Received forgot-password request, email: {}, IP: {}", email, clientIp);

        // 限流检查：同一邮箱 5 分钟内最多 3 次
        if (!rateLimitService.isEmailAllowed(email)) {
            logger.warn("Rate limit exceeded for email: {}", email);
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(ApiResponse.error("RATE_LIMIT_EXCEEDED", 
                          "Requests are too frequent, please try again later"));
        }

        // 限流检查：同一 IP 1 分钟内最多 10 次
        if (!rateLimitService.isIpAllowed(clientIp)) {
            logger.warn("Rate limit exceeded for IP: {}", clientIp);
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(ApiResponse.error("RATE_LIMIT_EXCEEDED", 
                          "Requests are too frequent, please try again later"));
        }

        // 发送密码重置邮件
        authService.forgotPassword(forgotPasswordRequest);

        // 始终返回成功，防止邮箱枚举攻击
        logger.info("Password reset request processed for email: {}", email);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset password", description = "Reset password using token from email")
    public ResponseEntity<ApiResponse<Object>> resetPassword(@Valid @RequestBody com.listen.portfolio.api.v1.auth.dto.ResetPasswordRequest resetPasswordRequest) {
        logger.info("Received reset-password request");

        boolean success = authService.resetPassword(
            resetPasswordRequest.getToken(),
            resetPasswordRequest.getNewPassword()
        );

        if (success) {
            logger.info("Password reset successfully");
            return ResponseEntity.ok(ApiResponse.success(null));
        }

        logger.warn("Password reset failed - invalid or expired token");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("INVALID_TOKEN", "The reset link is invalid or has expired"));
    }

    /**
     * 获取客户端真实 IP 地址
     * 
     * 说明：考虑代理和负载均衡的情况
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 如果有多个 IP，取第一个
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}

