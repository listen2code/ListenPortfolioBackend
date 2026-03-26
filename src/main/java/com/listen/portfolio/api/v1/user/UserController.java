package com.listen.portfolio.api.v1.user;

import com.listen.portfolio.api.v1.user.dto.UserSummaryDto;
import com.listen.portfolio.api.v1.auth.dto.ChangePasswordRequest;
import com.listen.portfolio.common.ApiResponse;
import com.listen.portfolio.common.Constants;
import com.listen.portfolio.service.UserService;
import com.listen.portfolio.infrastructure.persistence.entity.UserEntity;
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

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    @Operation(summary = "Get user", description = "Get basic user information by userId", 
              security = @SecurityRequirement(name = "bearerAuth"))
    public ApiResponse<UserSummaryDto> getUserById(@RequestParam @Min(value = 1, message = "id must be >= 1") Long id) {
        logger.info("Get user info, userId: {}", id);
        return userService.getUserSummaryById(id)
                .map(ApiResponse::success)
                .orElse(ApiResponse.error(Constants.DEFAULT_SERVER_ERROR, "User not found"));
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout", description = "Stateless logout; client should discard tokens",
              security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<String>> logout() {
        // todo Logout
        logger.info("Received logout request");
        logger.info("Logout successful");
        return ResponseEntity.ok(ApiResponse.success("Logout successful"));
    }

    @PostMapping("/change-password")
    @Operation(summary = "Change password", description = "Change password for current user",
              security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<Void>> changePassword(@Valid @RequestBody ChangePasswordRequest changePasswordRequest) {
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
            return ResponseEntity.ok(ApiResponse.success(null));
        } else {
            logger.warn("Password change failed for user: {}", username);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(Constants.DEFAULT_SERVER_ERROR, "Password change failed"));
        }
    }

    @DeleteMapping("/delete-account")
    @Operation(summary = "Delete account", description = "Permanently delete current user's account",
              security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<Void>> deleteAccount() {
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
