package com.listen.portfolio.controller;

import com.listen.portfolio.model.ApiResponse;
import com.listen.portfolio.model.UserInfo;
import com.listen.portfolio.service.UserInfoService;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/v1/user")
public class UserInfoController {

    private final UserInfoService service;

    public UserInfoController(UserInfoService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<UserInfo> getUserById(@RequestParam Long id) {
        return service.getUserById(id)
                .map(ApiResponse::success)
                .orElse(ApiResponse.error("101", "User not found"));
    }

    @PutMapping("/{id}")
    public ApiResponse<UserInfo> updateUser(@PathVariable Long id, @RequestBody UserInfo userInfo) {
        // Ensure the ID from the path is set on the object to be saved
        userInfo.setId(id);
        // The signUp method in the service will update if the user exists
        UserInfo updatedUser = service.signUp(userInfo);
        return ApiResponse.success(updatedUser);
    }
}