package com.listen.portfolio.api.v1.user;

import com.listen.portfolio.api.v1.user.dto.UserSummaryDto;
import com.listen.portfolio.api.v1.user.dto.ChangePasswordRequest;
import com.listen.portfolio.common.ApiResponse;
import com.listen.portfolio.common.Constants;
import com.listen.portfolio.service.UserService;
import com.listen.portfolio.service.TokenBlacklistService;
import com.listen.portfolio.infrastructure.persistence.entity.UserEntity;
import com.listen.portfolio.jwt.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Date;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import java.util.Optional;

@RestController
@RequestMapping("/v1/user")
/**
 * User API（v1）。
 *
 * 目的：
 * 1) 以“功能模块”组织包结构：api/v1/user
 * 2) 保持对外接口路径不变，逐步迁移，不影响既存功能
 */
@Validated
@Tag(name = "User", description = "User APIs")
public class UserController {
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    private final UserService userService;
    private final TokenBlacklistService tokenBlacklistService;
    private final JwtUtil jwtUtil;

    public UserController(UserService userService, 
                      TokenBlacklistService tokenBlacklistService,
                      JwtUtil jwtUtil) {
        this.userService = userService;
        this.tokenBlacklistService = tokenBlacklistService;
        this.jwtUtil = jwtUtil;
    }

    @GetMapping
    @Operation(summary = "Get user", description = "Get basic user information by userId", 
              security = @SecurityRequirement(name = "bearerAuth"))
    @com.listen.portfolio.common.RateLimit(
        types = {com.listen.portfolio.common.RateLimit.RateLimitType.USER},
        maxRequests = 100,
        timeWindowSeconds = 60
    )
    public ApiResponse<UserSummaryDto> getUserById(@RequestParam @Min(value = 1, message = "id must be >= 1") Long id) {
        logger.info("Get user info, userId: {}", id);
        return userService.getUserSummaryById(id)
                .map(ApiResponse::success)
                .orElse(ApiResponse.error(Constants.DEFAULT_SERVER_ERROR, "User not found"));
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout", description = "Logout current user and invalidate token",
              security = @SecurityRequirement(name = "bearerAuth"))
    @com.listen.portfolio.common.RateLimit(
        types = {com.listen.portfolio.common.RateLimit.RateLimitType.USER},
        maxRequests = 20,
        timeWindowSeconds = 60
    )
    public ResponseEntity<ApiResponse<String>> logout() {
        logger.info("Received logout request");
        
        try {
            // 获取当前认证用户的用户名
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = (authentication != null) ? authentication.getName() : "unknown";
            
            logger.info("Received logout request for user: {}", username);
            
            // 从请求头中获取当前 token
            String token = extractTokenFromRequest();
            
            if (token != null) {
                // 获取 token 的过期时间
                Date expiration = jwtUtil.extractExpiration(token);
                
                // 将 token 加入黑名单
                tokenBlacklistService.addToBlacklist(token, expiration.getTime());
                
                logger.info("User {} logged out successfully, token added to blacklist", username);
                
                // 清除当前用户的认证上下文
                SecurityContextHolder.clearContext();
                
                return ResponseEntity.ok(new ApiResponse<>("0", "", "Logout successful", null));
            } else {
                logger.warn("No token found in request during logout for user: {}", username);
                return ResponseEntity.ok(new ApiResponse<>("0", "", "Logout successful", null));
            }
            
        } catch (Exception e) {
            logger.error("Error during logout: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(Constants.DEFAULT_SERVER_ERROR, "Logout failed"));
        }
    }
    
    /**
     * 从当前请求中提取 JWT token
     * 
     * @return JWT token 字符串，如果没有找到则返回 null
     */
    private String extractTokenFromRequest() {
        try {
            // 方法1：从 SecurityContext 中获取
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getCredentials() != null) {
                String credentials = authentication.getCredentials().toString();
                if (credentials != null && !credentials.isEmpty() && !credentials.equals("N/A")) {
                    logger.debug("Token extracted from SecurityContext credentials: {}", maskToken(credentials));
                    return credentials;
                }
            }
            
            // 方法2：从 HTTP 请求头中直接获取
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String bearerToken = request.getHeader("Authorization");
                
                if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
                    String token = bearerToken.substring(7); // 移除 "Bearer " 前缀
                    logger.debug("Token extracted from Authorization header: {}", maskToken(token));
                    return token;
                }
            }
            
            logger.debug("No token found in SecurityContext or Authorization header");
            return null;
            
        } catch (Exception e) {
            logger.warn("Error extracting token from request: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * 遮盖 token 用于日志输出
     * 
     * @param token 原始 token
     * @return 遮盖后的 token
     */
    private String maskToken(String token) {
        if (token == null || token.length() < 10) {
            return "***";
        }
        return token.substring(0, 6) + "***" + token.substring(token.length() - 4);
    }

    @PostMapping("/change-password")
    @Operation(summary = "Change password", description = "Change password for current user",
              security = @SecurityRequirement(name = "bearerAuth"))
    @com.listen.portfolio.common.RateLimit(
        types = {com.listen.portfolio.common.RateLimit.RateLimitType.USER},
        maxRequests = 20,
        timeWindowSeconds = 60
    )
    public ResponseEntity<ApiResponse<Object>> changePassword(@Valid @RequestBody ChangePasswordRequest changePasswordRequest) {
        // 获取当前认证用户的用户名
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        
        logger.info("Received change-password request for current user: {}", username);

        // 根据用户名查找用户
        Optional<UserEntity> userOpt = userService.getUserByName(username);
        if (userOpt.isEmpty()) {
            logger.warn("User not found for change-password request: {}", username);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(Constants.DEFAULT_SERVER_ERROR, "User not found"));
        }

        var user = userOpt.get();
        
        // 验证请求中的userId是否与当前用户ID匹配
        if (!user.getId().toString().equals(changePasswordRequest.getUserId())) {
            logger.warn("User {} attempted to change password for different user ID: {}", 
                       username, changePasswordRequest.getUserId());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error(Constants.DEFAULT_SERVER_ERROR, "Cannot change password for other users"));
        }
        
        boolean success = userService.changePassword(changePasswordRequest);
        if (success) {
            logger.info("Password changed successfully for user: {}", username);
            
            // 密码修改成功后，清空当前用户的 token
            String currentToken = extractTokenFromRequest();
            if (currentToken != null) {
                // 将当前 token 加入黑名单，强制用户重新登录
                Date expiration = jwtUtil.extractExpiration(currentToken);
                tokenBlacklistService.addToBlacklist(currentToken, expiration.getTime());
                logger.info("Current token added to blacklist after password change for user: {}", username);
                
                // 清除当前用户的认证上下文
                SecurityContextHolder.clearContext();
                logger.info("Security context cleared after password change for user: {}", username);
            }
            
            return ResponseEntity.ok(ApiResponse.success("Password changed successfully. Please login again."));
        } else {
            logger.warn("Password change failed for user: {}", username);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(Constants.DEFAULT_SERVER_ERROR, "Password change failed"));
        }
    }

    @DeleteMapping("/delete-account")
    @Operation(summary = "Delete account", description = "Permanently delete current user's account",
              security = @SecurityRequirement(name = "bearerAuth"))
    @com.listen.portfolio.common.RateLimit(
        types = {com.listen.portfolio.common.RateLimit.RateLimitType.USER},
        maxRequests = 5,
        timeWindowSeconds = 60
    )
    public ResponseEntity<ApiResponse<Object>> deleteAccount() {
        // 获取当前认证用户的用户名
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        
        logger.info("Received delete-account request for current user: {}", username);

        // 根据用户名查找用户
        Optional<UserEntity> userOpt = userService.getUserByName(username);
        if (userOpt.isEmpty()) {
            logger.warn("User not found for delete-account request: {}", username);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(Constants.DEFAULT_SERVER_ERROR, "User not found"));
        }

        var user = userOpt.get();
        boolean success = userService.deleteAccount(user.getId());
        
        if (success) {
            logger.info("Account deleted successfully for user: {}", username);
            return ResponseEntity.ok(ApiResponse.success(null));
        } else {
            logger.warn("Account deletion failed for user: {}", username);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(Constants.DEFAULT_SERVER_ERROR, "Account deletion failed"));
        }
    }
}
