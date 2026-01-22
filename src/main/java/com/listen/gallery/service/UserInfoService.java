package com.listen.gallery.service;

import com.listen.gallery.model.UserInfo;
import com.listen.gallery.repository.UserInfoRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserInfoService {

    private final UserInfoRepository repo;

    public UserInfoService(UserInfoRepository repo) {
        this.repo = repo;
    }

    public List<UserInfo> getAllUsers() {
        return repo.findAll();
    }

    public Optional<UserInfo> getUserById(Integer id) {
        return repo.findById(id);
    }

    public UserInfo saveUser(UserInfo userInfo) {
        if (userInfo.getUserId() != null && repo.existsById(userInfo.getUserId())) {
            // Update logic: Fetch existing, update fields, save
            // Note: repo.save() works for update too, but if you want to be explicit or partial update:
            return repo.findById(userInfo.getUserId())
                    .map(existingUser -> {
                        existingUser.setUserName(userInfo.getUserName());
                        existingUser.setUserAge(userInfo.getUserAge());
                        return repo.save(existingUser);
                    })
                    .orElseGet(() -> repo.save(userInfo)); // Fallback if ID passed but not found (rare race condition)
        } else {
            // Create logic
            return repo.save(userInfo);
        }
    }

    public void deleteUser(Integer id) {
        repo.deleteById(id);
    }
}