package com.listen.portfolio.service;

import com.listen.portfolio.model.request.AuthRequest;
import com.listen.portfolio.model.request.ChangePasswordRequest;
import com.listen.portfolio.model.request.ForgotPasswordRequest;
import com.listen.portfolio.model.request.SignUpRequest;
import com.listen.portfolio.model.response.AuthResponse;
import com.listen.portfolio.model.response.UserResponse;
import com.listen.portfolio.model.response.UserSimpleResponse;
import com.listen.portfolio.repository.UserRepository;

import utils.Constants;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    private final UserRepository repo;

    public UserService(UserRepository repo) {
        this.repo = repo;
    }

    public List<UserResponse> getAllUsers() {
        logger.info("Fetching all users");
        List<UserResponse> users = repo.findAll();
        logger.info("Found {} users", users.size());
        return users;
    }

    public Optional<UserResponse> getUserById(Long id) {
        logger.info("Fetching user by id: {}", id);
        Optional<UserResponse> user = repo.findById(id);
        if (user.isPresent()) {
            logger.info("Found user: {}", user.get());
        } else {
            logger.warn("User with id: {} not found", id);
        }
        return user;
    }

    public Optional<UserSimpleResponse> getSimpleUserById(Long id) {
        logger.info("Fetching simple user by id: {}", id);
        return repo.findById(id)
                .map(UserSimpleResponse::new);
    }

    public boolean signUp(SignUpRequest signUpRequest) {
        logger.info("Signing up new user: {}", signUpRequest.getUserName());
        
        // Check if username already exists
        if (repo.findByNameCaseSensitive(signUpRequest.getUserName()).isPresent()) {
            logger.warn("Username {} already exists", signUpRequest.getUserName());
            return false;
        }
        
        UserResponse userInfo = new UserResponse();
        userInfo.setName(signUpRequest.getUserName());
        userInfo.setPassword(signUpRequest.getPassword()); // In a real application, hash the password
        userInfo.setEmail(signUpRequest.getEmail());
        repo.save(userInfo);
        logger.info("User {} signed up successfully", signUpRequest.getUserName());
        return true;
    }

    public Optional<AuthResponse> login(AuthRequest authRequest) {
        logger.info("Attempting to log in user: {}", authRequest.getUserName());
        return repo.findByNameCaseSensitive(authRequest.getUserName())
                .filter(userInfo -> {
                    logger.info("User userInfo.getPassword={} authRequest.getPassword()={}", userInfo.getPassword(), authRequest.getPassword());
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

    public Optional<AuthResponse> logout() {
        logger.info("Attempting to log out user");
        // In a real application, you would invalidate the JWT token
        return Optional.of(new AuthResponse("logged-out", "logged-out", null));
    }

    public boolean changePassword(ChangePasswordRequest changePasswordRequest) {
        logger.info("Attempting to change password for user: {}", changePasswordRequest.getUserId());
        return repo.findById(Long.parseLong(changePasswordRequest.getUserId()))
                .map(userInfo -> {
                    // In a real application, you would use passwordEncoder.matches()
                    if (userInfo.getPassword().equals(changePasswordRequest.getOldPassword())) {
                        // In a real application, you would hash the new password
                        userInfo.setPassword(changePasswordRequest.getNewPassword());
                        repo.save(userInfo);
                        logger.info("Password changed successfully for user: {}", changePasswordRequest.getUserId());
                        return true;
                    }
                    logger.warn("Old password does not match for user: {}", changePasswordRequest.getUserId());
                    return false;
                })
                .orElse(false);
    }

    public boolean forgotPassword(ForgotPasswordRequest forgotPasswordRequest) {
        logger.info("Attempting to reset password for user: {}", forgotPasswordRequest.getUserId());
        return repo.findById(Long.parseLong(forgotPasswordRequest.getUserId()))
                .map(userInfo -> {
                    // Verify email matches before resetting password
                    if (!userInfo.getEmail().equals(forgotPasswordRequest.getEmail())) {
                        logger.warn("Email does not match for user: {}", forgotPasswordRequest.getUserId());
                        return false;
                    }
                    // Reset password to default
                    userInfo.setPassword(Constants.DEFAULT_RESET_PASSWORD);
                    repo.save(userInfo);
                    logger.info("Password reset successfully for user: {}", forgotPasswordRequest.getUserId());
                    return true;
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