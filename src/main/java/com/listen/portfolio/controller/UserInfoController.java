package com.listen.portfolio.controller;

import com.listen.portfolio.model.ApiResponse;
import com.listen.portfolio.model.response.UserSimpleResponse;
import com.listen.portfolio.service.UserInfoService;


import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/v1/user")
public class UserInfoController {

    private final UserInfoService service;

    public UserInfoController(UserInfoService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<UserSimpleResponse> getUserById(@RequestParam Long id) {
        return service.getSimpleUserById(id)
                .map(ApiResponse::success)
                .orElse(ApiResponse.error("101", "User not found"));
    }
}