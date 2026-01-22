package com.listen.gallery.controller;

import com.listen.gallery.model.UserInfo;
import com.listen.gallery.service.UserInfoService;
import org.springframework.http.ResponseEntity;
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
    public List<UserInfo> userList() {
        return service.getAllUsers();
    }

    @GetMapping("/userInfo")
    public ResponseEntity<UserInfo> userInfo(@RequestParam Integer id) {
        return service.getUserById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/userUpdate")
    public UserInfo userUpdate(@RequestBody UserInfo userInfo) {
        return service.saveUser(userInfo);
    }

    @PostMapping("/userDelete")
    public ResponseEntity<Void> userDelete(@RequestBody Map<String, Integer> requestBody) {
        Integer id = requestBody.get("id");
        if (id != null) {
            service.deleteUser(id);
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.badRequest().build();
        }
    }
}