package com.listen.gallery.controller;

import com.listen.gallery.model.UserInfo;
import com.listen.gallery.service.UserInfoService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class UserInfoController {

    private final UserInfoService service;

    public UserInfoController(UserInfoService service) {
        this.service = service;
    }

    @GetMapping("/api/users")
    public List<UserInfo> getUsers() {
        return service.getAllUsers();
    }
}