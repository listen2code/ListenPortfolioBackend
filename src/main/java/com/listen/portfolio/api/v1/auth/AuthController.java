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
import org.springframework.validation.annotation.Validated;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
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
@Validated
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
    public ResponseEntity<ApiResponse<Void>> signUp(@Valid @RequestBody SignUpRequest signUpRequest) {
        logger.info("Received sign-up request, user: {}", signUpRequest.getUserName());

        boolean success = userInfoService.signUp(signUpRequest);

        if (success) {
            logger.info("User {} signed up successfully", signUpRequest.getUserName());
            return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(null));
        }

        logger.warn("Username {} already exists, sign-up failed", signUpRequest.getUserName());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(Constants.DEFAULT_SERVER_ERROR, "Username already exists"));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody AuthRequest authRequest) {
        logger.info("Received login request, user: {}", authRequest.getUserName());

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            authRequest.getUserName(),
                            authRequest.getPassword()
                    )
            );
            logger.info("Credentials verified for user {}", authRequest.getUserName());
        } catch (BadCredentialsException e) {
            // 说明（中文）：登录失败统一按“凭据无效”处理，避免区分“用户不存在/密码错误”导致用户枚举风险
            // 日志（英文）：记录 user 与 reason，便于排查（不会打印密码/token 等敏感信息）
            logger.warn("Invalid credentials for user {}, reason: {}", authRequest.getUserName(), e.getMessage());
            return ResponseEntity.status(HttpStatus.OK)
                    .body(ApiResponse.error(Constants.DEFAULT_SERVER_ERROR, "Invalid credentials"));
        }

        final UserDetails userDetails = userDetailsService.loadUserByUsername(authRequest.getUserName());
        final String jwt = jwtUtil.generateToken(userDetails);
        final String refreshToken = jwtUtil.generateRefreshToken(jwt);

        final Long userId = userInfoService.getUserByName(authRequest.getUserName())
                .map(u -> u.getId())
                .orElse(null);

        logger.info("User {} logged in successfully", authRequest.getUserName());
        return ResponseEntity.ok(ApiResponse.success(new AuthResponse(jwt, refreshToken, userId)));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<String>> logout() {
        logger.info("Received logout request");
        logger.info("Logout successful");
        return ResponseEntity.ok(ApiResponse.success("Logout successful"));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<?>> refresh(@RequestParam @NotBlank(message = "refreshToken must not be blank") String refreshToken) {
        logger.info("Received token refresh request");

        String jwt = jwtUtil.refreshToken(refreshToken);
        String newRefreshToken = jwtUtil.generateRefreshToken(jwt);

        String username = jwtUtil.extractUsername(refreshToken);
        Long userId = userInfoService.getUserByName(username)
                .map(u -> u.getId())
                .orElse(null);

        logger.info("Token refreshed successfully, user: {}", username);
        return ResponseEntity.ok(ApiResponse.success(new AuthResponse(jwt, newRefreshToken, userId)));
    }

    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(@Valid @RequestBody ChangePasswordRequest changePasswordRequest) {
        logger.info("Received change-password request, userId: {}", changePasswordRequest.getUserId());

        boolean success = userInfoService.changePassword(changePasswordRequest);

        if (success) {
            logger.info("Password changed successfully for user {}", changePasswordRequest.getUserId());
            return ResponseEntity.ok(ApiResponse.success(null));
        }

        logger.warn("Password change failed for user {}", changePasswordRequest.getUserId());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(Constants.DEFAULT_SERVER_ERROR, "Password change failed"));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest forgotPasswordRequest) {
        logger.info("Received forgot-password request, email: {}", forgotPasswordRequest.getEmail());

        boolean success = userInfoService.forgotPassword(forgotPasswordRequest);

        if (success) {
            logger.info("Password reset to default for email {}", forgotPasswordRequest.getEmail());
            return ResponseEntity.ok(ApiResponse.success(null));
        }

        logger.warn("Password reset failed for email {}", forgotPasswordRequest.getEmail());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(Constants.DEFAULT_SERVER_ERROR, "Password change failed"));
    }

    @DeleteMapping("/delete-account")
    public ResponseEntity<ApiResponse<Void>> deleteAccount(@Valid @RequestBody DeleteAccountRequest deleteAccountRequest) {
        logger.info("Received delete-account request, userId: {}", deleteAccountRequest.getUserId());

        boolean success = userInfoService.deleteAccount(Long.parseLong(deleteAccountRequest.getUserId()));

        if (success) {
            logger.info("Account deleted successfully for user {}", deleteAccountRequest.getUserId());
            return ResponseEntity.ok(ApiResponse.success(null));
        }

        logger.warn("User {} not found, delete failed", deleteAccountRequest.getUserId());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(Constants.DEFAULT_SERVER_ERROR, "User not found"));
    }
}

