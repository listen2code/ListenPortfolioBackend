package com.listen.portfolio.service;

import com.listen.portfolio.model.request.ChangePasswordRequest;
import com.listen.portfolio.model.request.ForgotPasswordRequest;
import com.listen.portfolio.model.request.SignUpRequest;
import com.listen.portfolio.model.response.UserResponse;
import com.listen.portfolio.model.response.UserSimpleResponse;
import com.listen.portfolio.repository.UserRepository;

import utils.Constants;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class UserService implements UserDetailsService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    private final UserRepository repo;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository repo, @Lazy PasswordEncoder passwordEncoder) {
        this.repo = repo;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        logger.info("Loading user by username for security context: {}", username);
        UserResponse user = repo.findByNameCaseSensitive(username)
                .orElseThrow(() -> {
                    logger.warn("User not found with username: {}", username);
                    return new UsernameNotFoundException("User not found with username: " + username);
                });
        logger.info("User found: {}", username);
        return new User(user.getName(), user.getPassword(), new ArrayList<>());
    }

    public Optional<UserResponse> getUserByName(String username) {
        logger.info("Fetching user by name: {}", username);
        return repo.findByNameCaseSensitive(username);
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
        userInfo.setPassword(passwordEncoder.encode(signUpRequest.getPassword()));
        userInfo.setEmail(signUpRequest.getEmail());
        repo.save(userInfo);
        logger.info("User {} signed up successfully", signUpRequest.getUserName());
        return true;
    }

    public boolean changePassword(ChangePasswordRequest changePasswordRequest) {
        logger.info("Attempting to change password for user: {}", changePasswordRequest.getUserId());
        return repo.findById(Long.parseLong(changePasswordRequest.getUserId()))
                .map(userInfo -> {
                    if (passwordEncoder.matches(changePasswordRequest.getOldPassword(), userInfo.getPassword())) {
                        userInfo.setPassword(passwordEncoder.encode(changePasswordRequest.getNewPassword()));
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
                    userInfo.setPassword(passwordEncoder.encode(Constants.DEFAULT_RESET_PASSWORD));
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