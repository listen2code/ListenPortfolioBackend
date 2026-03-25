package com.listen.portfolio.api.v1.user;

import com.listen.portfolio.model.ApiResponse;
import com.listen.portfolio.model.response.UserSimpleResponse;
import com.listen.portfolio.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import utils.Constants;

@RestController
@RequestMapping("/v1/user")
/**
 * User API（v1）。
 *
 * 目的：
 * 1) 以“功能模块”组织包结构：api/v1/user
 * 2) 保持对外接口路径不变，逐步迁移，不影响既存功能
 */
public class UserController {
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    private final UserService service;

    public UserController(UserService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<UserSimpleResponse> getUserById(@RequestParam Long id) {
        logger.info("Get user info, userId: {}", id);
        return service.getSimpleUserById(id)
                .map(ApiResponse::success)
                .orElse(ApiResponse.error(Constants.DEFAULT_SERVER_ERROR, "User not found"));
    }
}
