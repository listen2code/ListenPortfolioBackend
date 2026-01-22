package com.listen.gallery.controller;

import com.listen.gallery.model.ApiResponse;
import com.listen.gallery.model.UserInfo;
import com.listen.gallery.service.UserInfoService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class UserInfoController {

    private final UserInfoService service;

    public UserInfoController(UserInfoService service) {
        this.service = service;
    }

    @GetMapping("/userList")
    public ApiResponse<List<UserInfo>> userList() {
        return ApiResponse.success(service.getAllUsers());
    }

    @GetMapping("/userInfo")
    public ApiResponse<UserInfo> userInfo(@RequestParam Integer id) {
        return service.getUserById(id)
                .map(ApiResponse::success)
                .orElse(ApiResponse.error("101", "User not found"));
    }

    @PostMapping("/userUpdate")
    public ApiResponse<Void> userUpdate(@RequestBody UserInfo userInfo) {
        service.saveUser(userInfo);
        return ApiResponse.success(null);
    }

    @PostMapping("/userDelete")
    public ApiResponse<Void> userDelete(@RequestBody Map<String, Integer> requestBody) {
        Integer id = requestBody.get("id");
        if (id != null) {
            service.deleteUser(id);
            return ApiResponse.success(null);
        } else {
            return ApiResponse.error("102","ID is required");
        }
    }
}