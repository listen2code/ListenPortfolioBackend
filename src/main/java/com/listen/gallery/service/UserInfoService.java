package com.listen.gallery.service;

import com.listen.gallery.model.UserInfo;
import com.listen.gallery.repository.UserInfoRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserInfoService {

    private final UserInfoRepository repo;

    public UserInfoService(UserInfoRepository repo) {
        this.repo = repo;
    }

    public List<UserInfo> getAllUsers() {
        return repo.findAll();
    }
}