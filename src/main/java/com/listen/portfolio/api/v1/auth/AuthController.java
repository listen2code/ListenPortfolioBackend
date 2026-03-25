package com.listen.portfolio.api.v1.auth;

import com.listen.portfolio.jwt.JwtUtil;
import com.listen.portfolio.api.v1.auth.dto.AuthRequest;
import com.listen.portfolio.api.v1.auth.dto.AuthResponse;
import com.listen.portfolio.api.v1.auth.dto.ChangePasswordRequest;
import com.listen.portfolio.api.v1.auth.dto.DeleteAccountRequest;
import com.listen.portfolio.api.v1.auth.dto.ForgotPasswordRequest;
import com.listen.portfolio.api.v1.auth.dto.SignUpRequest;
import com.listen.portfolio.model.ApiResponse;
import com.listen.portfolio.service.UserService;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import utils.Constants;

@RestController
@RequestMapping("/v1/auth")
/**
 * Auth API（v1）。
 *
 * 目的：
 * 1) 以“功能模块”组织包结构：api/v1/auth
 * 2) 保持对外接口路径不变，逐步迁移，不影响既存功能
 */
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserDetailsService userDetailsService;

    private final UserService userInfoService;

    public AuthController(UserService userInfoService) {
        this.userInfoService = userInfoService;
    }

    @PostMapping("/signUp")
    public ResponseEntity<ApiResponse<Void>> signUp(@RequestBody SignUpRequest signUpRequest) {
        logger.info("收到注册请求，用户: {}", signUpRequest.getUserName());

        boolean success = userInfoService.signUp(signUpRequest);

        if (success) {
            logger.info("用户 {} 注册成功", signUpRequest.getUserName());
            return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(null));
        }

        logger.warn("用户名 {} 已存在，注册失败", signUpRequest.getUserName());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(Constants.DEFAULT_SERVER_ERROR, "Username already exists"));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest authRequest) {
        logger.info("收到登录请求，用户: {}", authRequest.getUserName());

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            authRequest.getUserName(),
                            authRequest.getPassword()
                    )
            );
            logger.info("用户 {} 凭据验证成功", authRequest.getUserName());
        } catch (BadCredentialsException e) {
            logger.warn("用户 {} 凭据无效", authRequest.getUserName());
            return ResponseEntity.status(HttpStatus.OK)
                    .body(ApiResponse.error(Constants.DEFAULT_SERVER_ERROR, "Invalid credentials"));
        }

        final UserDetails userDetails = userDetailsService.loadUserByUsername(authRequest.getUserName());
        final String jwt = jwtUtil.generateToken(userDetails);
        final String refreshToken = jwtUtil.generateRefreshToken(jwt);

        final Long userId = userInfoService.getUserByName(authRequest.getUserName())
                .map(u -> u.getId())
                .orElse(null);

        logger.info("用户 {} 登录成功", authRequest.getUserName());
        return ResponseEntity.ok(ApiResponse.success(new AuthResponse(jwt, refreshToken, userId)));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<String>> logout() {
        logger.info("收到登出请求");
        logger.info("登出成功");
        return ResponseEntity.ok(ApiResponse.success("Logout successful"));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<?>> refresh(@RequestParam String refreshToken) {
        logger.info("收到令牌刷新请求");

        String jwt = jwtUtil.refreshToken(refreshToken);
        String newRefreshToken = jwtUtil.generateRefreshToken(jwt);

        String username = jwtUtil.extractUsername(refreshToken);
        Long userId = userInfoService.getUserByName(username)
                .map(u -> u.getId())
                .orElse(null);

        logger.info("令牌刷新成功，用户: {}", username);
        return ResponseEntity.ok(ApiResponse.success(new AuthResponse(jwt, newRefreshToken, userId)));
    }

    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(@RequestBody ChangePasswordRequest changePasswordRequest) {
        logger.info("收到密码更改请求，用户 ID: {}", changePasswordRequest.getUserId());

        boolean success = userInfoService.changePassword(changePasswordRequest);

        if (success) {
            logger.info("用户 {} 密码更改成功", changePasswordRequest.getUserId());
            return ResponseEntity.ok(ApiResponse.success(null));
        }

        logger.warn("用户 {} 密码更改失败", changePasswordRequest.getUserId());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(Constants.DEFAULT_SERVER_ERROR, "Password change failed"));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@RequestBody ForgotPasswordRequest forgotPasswordRequest) {
        logger.info("收到忘记密码请求，邮箱: {}", forgotPasswordRequest.getEmail());

        boolean success = userInfoService.forgotPassword(forgotPasswordRequest);

        if (success) {
            logger.info("邮箱 {} 密码重置成功", forgotPasswordRequest.getEmail());
            return ResponseEntity.ok(ApiResponse.success(null));
        }

        logger.warn("邮箱 {} 密码重置失败", forgotPasswordRequest.getEmail());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(Constants.DEFAULT_SERVER_ERROR, "Password change failed"));
    }

    @DeleteMapping("/delete-account")
    public ResponseEntity<ApiResponse<Void>> deleteAccount(@RequestBody DeleteAccountRequest deleteAccountRequest) {
        logger.info("收到删除账户请求，用户 ID: {}", deleteAccountRequest.getUserId());

        boolean success = userInfoService.deleteAccount(Long.parseLong(deleteAccountRequest.getUserId()));

        if (success) {
            logger.info("用户 {} 账户删除成功", deleteAccountRequest.getUserId());
            return ResponseEntity.ok(ApiResponse.success(null));
        }

        logger.warn("用户 {} 未找到，删除失败", deleteAccountRequest.getUserId());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(Constants.DEFAULT_SERVER_ERROR, "User not found"));
    }
}

