package com.listen.portfolio.api.v1.user;

import com.listen.portfolio.api.v1.user.dto.UserSummaryDto;
import com.listen.portfolio.common.ApiResponse;
import com.listen.portfolio.common.Constants;
import com.listen.portfolio.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import jakarta.validation.constraints.Min;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

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

    private final UserService service;

    public UserController(UserService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Get user", description = "Get basic user information by userId")
    public ApiResponse<UserSummaryDto> getUserById(@RequestParam @Min(value = 1, message = "id must be >= 1") Long id) {
        logger.info("Get user info, userId: {}", id);
        return service.getUserSummaryById(id)
                .map(ApiResponse::success)
                .orElse(ApiResponse.error(Constants.DEFAULT_SERVER_ERROR, "User not found"));
    }
}
