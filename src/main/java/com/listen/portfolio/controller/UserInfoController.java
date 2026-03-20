package com.listen.portfolio.controller;

import com.listen.portfolio.model.ApiResponse;
import com.listen.portfolio.model.response.UserInfoResponse;
import com.listen.portfolio.service.UserInfoService;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/v1/user")
public class UserInfoController {

    private final UserInfoService service;

    public UserInfoController(UserInfoService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<UserInfoResponse> getUserById(@RequestParam Long id) {
        return service.getUserById(id)
                .map(ApiResponse::success)
                .orElse(ApiResponse.error("101", "User not found"));
    }
}