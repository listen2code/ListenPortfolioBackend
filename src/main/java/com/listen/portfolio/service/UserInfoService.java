package com.listen.portfolio.service;

import com.listen.portfolio.model.AuthRequest;
import com.listen.portfolio.model.AuthResponse;
import com.listen.portfolio.model.ChangePasswordRequest;
import com.listen.portfolio.model.UserInfo;
import com.listen.portfolio.repository.UserInfoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserInfoService {

    private static final Logger logger = LoggerFactory.getLogger(UserInfoService.class);

    private final UserInfoRepository repo;

    public UserInfoService(UserInfoRepository repo) {
        this.repo = repo;
    }

    public List<UserInfo> getAllUsers() {
        logger.info("Fetching all users");
        List<UserInfo> users = repo.findAll();
        logger.info("Found {} users", users.size());
        return users;
    }

    public Optional<UserInfo> getUserById(Long id) {
        logger.info("Fetching user by id: {}", id);
        Optional<UserInfo> user = repo.findById(id);
        if (user.isPresent()) {
            logger.info("Found user: {}", user.get());
        } else {
            logger.warn("User with id: {} not found", id);
        }
        return user;
    }

    public UserInfo signUp(UserInfo userInfo) {
        logger.info("Signing up user: {}", userInfo.getEmail());
        // In a real application, you would hash the password here
        // userInfo.setPassword(passwordEncoder.encode(userInfo.getPassword()));
        return repo.save(userInfo);
    }

    public Optional<AuthResponse> login(AuthRequest authRequest) {
        logger.info("Attempting to log in user: {}", authRequest.getUserName());
        return repo.findByName(authRequest.getUserName())
                .filter(userInfo -> {
                    // In a real application, you would use passwordEncoder.matches()
                    return userInfo.getPassword().equals(authRequest.getPassword());
                })
                .map(userInfo -> {
                    // In a real application, you would generate a real JWT token
                    String token = "dummy-jwt-token-for-" + userInfo.getEmail();
                    String refreshToken = "dummy-refresh-token";
                    logger.info("User {} logged in successfully", userInfo.getEmail());
                    return new AuthResponse(token, refreshToken, userInfo.getId());
                });
    }

    public boolean changePassword(ChangePasswordRequest changePasswordRequest) {
        logger.info("Attempting to change password for user: {}", changePasswordRequest.getEmail());
        return repo.findByName(changePasswordRequest.getEmail())
                .map(userInfo -> {
                    // In a real application, you would use passwordEncoder.matches()
                    if (userInfo.getPassword().equals(changePasswordRequest.getOldPassword())) {
                        // In a real application, you would hash the new password
                        userInfo.setPassword(changePasswordRequest.getNewPassword());
                        repo.save(userInfo);
                        logger.info("Password changed successfully for user: {}", changePasswordRequest.getEmail());
                        return true;
                    }
                    logger.warn("Old password does not match for user: {}", changePasswordRequest.getEmail());
                    return false;
                })
                .orElse(false);
    }

    public boolean deleteAccount(Long userId) {
        logger.info("Attempting to delete account for user: {}", userId);
        return repo.findById(userId)
                .map(userInfo -> {
                    repo.delete(userInfo);
                    logger.info("Account deleted successfully for user: {}", userId);
                    return true;
                }).orElse(false);
    }
}