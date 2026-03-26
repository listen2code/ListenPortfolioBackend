package com.listen.portfolio.api.v1.user;

import com.listen.portfolio.api.v1.user.dto.UserSummaryDto;
import com.listen.portfolio.api.v1.auth.dto.ChangePasswordRequest;
import com.listen.portfolio.api.v1.auth.dto.DeleteAccountRequest;
import com.listen.portfolio.common.ApiResponse;
import com.listen.portfolio.common.Constants;
import com.listen.portfolio.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    @Operation(summary = "Change password", description = "Change password for an existing user",
              security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<Void>> changePassword(@Valid @RequestBody ChangePasswordRequest changePasswordRequest) {
        logger.info("Received change-password request, userId: {}", changePasswordRequest.getUserId());

        boolean success = userService.changePassword(changePasswordRequest);

        if (success) {
            logger.info("Password changed successfully for user {}", changePasswordRequest.getUserId());
            return ResponseEntity.ok(ApiResponse.success(null));
        }

        logger.warn("Password change failed for user {}", changePasswordRequest.getUserId());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(Constants.DEFAULT_SERVER_ERROR, "Password change failed"));
    }

    @DeleteMapping("/delete-account")
    @Operation(summary = "Delete account", description = "Permanently delete user account by userId",
              security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<Void>> deleteAccount(@Valid @RequestBody DeleteAccountRequest deleteAccountRequest) {
        logger.info("Received delete-account request, userId: {}", deleteAccountRequest.getUserId());

        boolean success = userService.deleteAccount(Long.parseLong(deleteAccountRequest.getUserId()));

        if (success) {
            logger.info("Account deleted successfully for user {}", deleteAccountRequest.getUserId());
            return ResponseEntity.ok(ApiResponse.success(null));
        }

        logger.warn("User {} not found, delete failed", deleteAccountRequest.getUserId());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(Constants.DEFAULT_SERVER_ERROR, "User not found"));
    }
}
