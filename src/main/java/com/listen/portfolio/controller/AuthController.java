package com.listen.portfolio.controller;

import com.listen.portfolio.jwt.JwtUtil;
import com.listen.portfolio.model.*;
import com.listen.portfolio.model.request.AuthRequest;
import com.listen.portfolio.model.request.ChangePasswordRequest;
import com.listen.portfolio.model.request.DeleteAccountRequest;
import com.listen.portfolio.model.request.ForgotPasswordRequest;
import com.listen.portfolio.model.request.SignUpRequest;
import com.listen.portfolio.model.response.AuthResponse;
import com.listen.portfolio.service.UserService;

import utils.Constants;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.*;

/**
 * REST 控制器，处理所有与认证相关的端点。
 * 提供用户注册、登录、登出和账户管理端点。
 * 所有端点以 /v1/auth 前缀开头。
 */
@RestController
@RequestMapping("/v1/auth")
public class AuthController {

    // 日志记录器，用于记录认证操作
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    /**
     * Spring Security 的 AuthenticationManager。
     * 用于认证用户凭据（用户名 + 密码）。
     * 自动根据 UserDetailsService 验证。
     */
    @Autowired
    private AuthenticationManager authenticationManager;

    /**
     * JWT 工具组件。
     * 用于在成功认证后生成和验证 JWT 令牌。
     */
    @Autowired
    private JwtUtil jwtUtil;

    /**
     * Spring Security 的 UserDetailsService。
     * 用于在认证期间按用户名加载用户详情。
     */
    @Autowired
    private UserDetailsService userDetailsService;

    /**
     * 应用程序的自定义 UserService。
     * 处理用户操作的业务逻辑（注册、密码更改等）。
     */
    private final UserService userInfoService;

    /**
     * 构造函数，用于依赖注入 UserService。
     * @param userInfoService 包含业务逻辑的用户服务
     */
    public AuthController(UserService userInfoService) {
        this.userInfoService = userInfoService;
    }

    /**
     * 端点：POST /v1/auth/signUp
     * 注册新用户账户。
     * 
     * @param signUpRequest JSON 请求体，包含用户名、密码和邮箱
     * @return ResponseEntity 成功（201 Created）或错误（400 Bad Request）
     */
    @PostMapping("/signUp")
    public ResponseEntity<ApiResponse<Void>> signUp(@RequestBody SignUpRequest signUpRequest) {
        logger.info("收到注册请求，用户: {}", signUpRequest.getUserName());
        
        // 调用服务注册新用户（检查重复用户名）
        boolean success = userInfoService.signUp(signUpRequest);
        
        if (success) {
            // 用户注册成功，返回 201 Created 状态
            logger.info("用户 {} 注册成功", signUpRequest.getUserName());
            return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(null));
        } else {
            // 用户名已存在，返回 400 Bad Request 和错误消息
            logger.warn("用户名 {} 已存在，注册失败", signUpRequest.getUserName());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(Constants.DEFAULT_SERVER_ERROR, "Username already exists"));
        }
    }

    /**
     * 端点：POST /v1/auth/login
     * 认证用户并生成 JWT 令牌。
     * 步骤：
     * 1. 根据数据库认证用户名/密码
     * 2. 如果认证成功，生成 JWT 令牌
     * 3. 将令牌返回给客户端，用于后续请求
     * 
     * @param authRequest JSON 请求体，包含用户名和密码
     * @return ResponseEntity JWT 令牌（200 OK）或错误（401 Unauthorized）
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest authRequest) {
        logger.info("收到登录请求，用户: {}", authRequest.getUserName());
        
        try {
            // 认证用户凭据
            // 创建包含用户名和密码的令牌，然后验证它
            // 这会调用 UserDetailsService（即 UserService）上的 loadUserByUsername()
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            authRequest.getUserName(), 
                            authRequest.getPassword()
                    )
            );
            logger.info("用户 {} 凭据验证成功", authRequest.getUserName());
        } catch (BadCredentialsException e) {
            // 认证失败（无效用户名或密码）
            logger.error("用户 {} 凭据无效", authRequest.getUserName());
            return ResponseEntity.status(HttpStatus.OK)
                    .body(ApiResponse.error(Constants.DEFAULT_SERVER_ERROR, "Invalid credentials"));
        }

        // 认证成功！现在生成 JWT 令牌
        // 从数据库加载用户详情
        final UserDetails userDetails = userDetailsService.loadUserByUsername(authRequest.getUserName());
        
        // 使用用户名作为主题生成 JWT 令牌
        final String jwt = jwtUtil.generateToken(userDetails);

        // 生成刷新令牌
        final String refreshToken = jwtUtil.generateRefreshToken(jwt);
        
        // 获取用户的数据库 ID 以在响应中返回
        final Long userId = userInfoService.getUserByName(authRequest.getUserName())
                .map(u -> u.getId())  // 如果用户存在，提取 ID
                .orElse(null);  // 如果用户未找到，返回 null（不应该发生）
        
        logger.info("用户 {} 登录成功", authRequest.getUserName());

        // 返回 JWT 令牌和用户 ID（客户端将在后续请求的 Authorization 头中使用令牌）
        return ResponseEntity.ok(ApiResponse.success(new AuthResponse(jwt, refreshToken, userId)));
    }

    /**
     * 端点：POST /v1/auth/logout
     * 登出端点（无状态 JWT 方法）。
     * 在无状态 JWT 系统中，登出由客户端处理，通过删除令牌。
     * 此端点为 API 约定而存在，但不执行任何服务器端操作。
     * 
     * @return ResponseEntity 成功消息
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<String>> logout() {
        logger.info("收到登出请求");
        
        // 对于无状态 JWT，登出由客户端处理，通过删除令牌。
        // 此端点是约定性的，从客户端角度确认登出。
        logger.info("登出成功");
        return ResponseEntity.ok(ApiResponse.success("Logout successful"));
    }

    /**
     * 端点：POST /v1/auth/refresh
     * 刷新 JWT 令牌（无状态方法）。
     * 在无状态 JWT 系统中，刷新由客户端处理，通过使用现有令牌获取新令牌。
     * 此端点为 API 约定而存在，但不执行任何服务器端操作。
     * 
     * @return ResponseEntity 成功消息
     */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<?>> refresh(@RequestParam String refreshToken) {
        logger.info("收到令牌刷新请求 {}", refreshToken);
        String jwt = jwtUtil.refreshToken(refreshToken);
        String newRefreshToken = jwtUtil.generateRefreshToken(jwt);
        String username = jwtUtil.extractUsername(refreshToken);
        Long userId = userInfoService.getUserByName(username)
                .map(u -> u.getId())  // 如果用户存在，提取 ID
                .orElse(null);  // 如果用户未找到，返回 null（不应该发生）
        logger.info("令牌刷新成功，用户: {}", username);
        return ResponseEntity.ok(ApiResponse.success(new AuthResponse(jwt, newRefreshToken, userId)));
    }

    /**
     * 端点：POST /v1/auth/change-password
     * 允许已认证用户更改密码。
     * 出于安全考虑，需要验证旧密码。
     * 
     * @param changePasswordRequest JSON 体，包含 userId、oldPassword、newPassword
     * @return ResponseEntity 成功（200 OK）或错误（400 Bad Request）
     */
    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @RequestBody ChangePasswordRequest changePasswordRequest) {
        logger.info("收到密码更改请求，用户 ID: {}", changePasswordRequest.getUserId());
        
        // 调用服务更改密码（验证旧密码）
        boolean success = userInfoService.changePassword(changePasswordRequest);
        
        if (success) {
            logger.info("用户 {} 密码更改成功", changePasswordRequest.getUserId());
            return ResponseEntity.ok(ApiResponse.success(null));
        } else {
            // 密码更改失败（旧密码不匹配或用户未找到）
            logger.warn("用户 {} 密码更改失败", changePasswordRequest.getUserId());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(Constants.DEFAULT_SERVER_ERROR, "Password change failed"));
        }
    }

    /**
     * 端点：POST /v1/auth/forgot-password
     * 当用户忘记密码时重置密码。
     * 通过邮箱查找用户并重置密码。
     * 将密码设置为默认重置密码。
     * 
     * @param forgotPasswordRequest JSON 体，包含 email
     * @return ResponseEntity 成功（200 OK）或错误（400 Bad Request）
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(
            @RequestBody ForgotPasswordRequest forgotPasswordRequest) {
        logger.info("收到忘记密码请求，邮箱: {}", forgotPasswordRequest.getEmail());
        
        // 调用服务重置密码
        boolean success = userInfoService.forgotPassword(forgotPasswordRequest);
        
        if (success) {
            logger.info("邮箱 {} 密码重置成功", forgotPasswordRequest.getEmail());
            return ResponseEntity.ok(ApiResponse.success(null));
        } else {
            // 密码重置失败（用户未找到）
            logger.warn("邮箱 {} 密码重置失败", forgotPasswordRequest.getEmail());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(Constants.DEFAULT_SERVER_ERROR, "Password change failed"));
        }
    }

    /**
     * 端点：DELETE /v1/auth/delete-account
     * 永久删除用户账户。
     * 用户提供其 ID 作为查询参数。
     * 
     * @param userId 要删除账户的 ID（查询参数）
     * @return ResponseEntity 成功（200 OK）或错误（404 Not Found）
     */
    @DeleteMapping("/delete-account")
    public ResponseEntity<ApiResponse<Void>> deleteAccount(
            @RequestBody DeleteAccountRequest deleteAccountRequest) {
        logger.info("收到删除账户请求，用户 ID: {}", deleteAccountRequest.getUserId());

        // 调用服务删除账户
        boolean success = userInfoService.deleteAccount(Long.parseLong(deleteAccountRequest.getUserId()));
        
        if (success) {
            logger.info("用户 {} 账户删除成功", deleteAccountRequest.getUserId());
            return ResponseEntity.ok(ApiResponse.success(null));
        } else {
            // 用户未找到
            logger.warn("用户 {} 未找到，删除失败", deleteAccountRequest.getUserId());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(Constants.DEFAULT_SERVER_ERROR, "User not found"));
        }
    }
}