package com.listen.portfolio.controller;

import com.listen.portfolio.model.ApiResponse;
import com.listen.portfolio.model.response.UserSimpleResponse;
import com.listen.portfolio.service.UserService;

import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import org.slf4j.Logger;
import utils.Constants;

@RestController
@RequestMapping("/v1/user")
public class UserController {
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    private final UserService service;

    public UserController(UserService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<UserSimpleResponse> getUserById(@RequestParam Long id) {
        logger.info("获取用户信息，用户ID: {}", id);
        return service.getSimpleUserById(id)
                .map(ApiResponse::success)
                .orElse(ApiResponse.error(Constants.DEFAULT_SERVER_ERROR, "User not found"));
    }
}