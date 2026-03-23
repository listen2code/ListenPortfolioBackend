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

/**
 * Service class that handles user-related business logic.
 * Implements UserDetailsService interface for Spring Security authentication.
 * This service is responsible for user CRUD operations and password management.
 */
@Service
public class UserService implements UserDetailsService {

    // Logger instance for logging user operations
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    // Repository for database operations on User entities
    private final UserRepository repo;
    
    // Password encoder for encrypting and verifying passwords securely
    private final PasswordEncoder passwordEncoder;

    /**
     * Constructor with dependency injection.
     * @param repo UserRepository for database access
     * @param passwordEncoder PasswordEncoder for password encryption
     * @Lazy annotation ensures PasswordEncoder is not created until first use
     */
    public UserService(UserRepository repo, @Lazy PasswordEncoder passwordEncoder) {
        this.repo = repo;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Override method from UserDetailsService interface.
     * Called by Spring Security during authentication to load user information by username.
     * 
     * @param username The username to search for (case-sensitive)
     * @return UserDetails object containing username, password and authorities
     * @throws UsernameNotFoundException if user is not found
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        logger.info("Loading user by username for security context: {}", username);
        
        // Search for user in database by username (case-sensitive search)
        UserResponse user = repo.findByNameCaseSensitive(username)
                // If user not found, throw exception
                .orElseThrow(() -> {
                    logger.warn("User not found with username: {}", username);
                    return new UsernameNotFoundException("User not found with username: " + username);
                });
        
        logger.info("User found: {}", username);
        
        // Return Spring Security's User object with username, encrypted password, and empty authorities list
        // Authorities (roles/permissions) are empty here but can be extended
        return new User(user.getName(), user.getPassword(), new ArrayList<>());
    }

    /**
     * Get user by username (returns custom UserResponse object, not Spring Security's UserDetails).
     * 
     * @param username The username to search for (case-sensitive)
     * @return Optional containing the user if found
     */
    public Optional<UserResponse> getUserByName(String username) {
        logger.info("Fetching user by name: {}", username);
        return repo.findByNameCaseSensitive(username);
    }

    /**
     * Retrieve all users from the database.
     * 
     * @return List of all user objects
     */
    public List<UserResponse> getAllUsers() {
        logger.info("Fetching all users");
        List<UserResponse> users = repo.findAll();
        logger.info("Found {} users", users.size());
        return users;
    }

    /**
     * Get a specific user by their ID.
     * 
     * @param id The user's ID (primary key)
     * @return Optional containing the user if found
     */
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

    /**
     * Get a simplified version of user information (fewer fields).
     * Useful for endpoints that don't need all user details.
     * 
     * @param id The user's ID
     * @return Optional containing the simplified user response
     */
    public Optional<UserSimpleResponse> getSimpleUserById(Long id) {
        logger.info("Fetching simple user by id: {}", id);
        return repo.findById(id)
                // Transform UserResponse to UserSimpleResponse using constructor
                .map(UserSimpleResponse::new);
    }

    /**
     * Register a new user (sign up).
     * Validates that username doesn't already exist and encodes the password.
     * 
     * @param signUpRequest Request object containing username, password and email
     * @return true if signup successful, false if username already exists
     */
    public boolean signUp(SignUpRequest signUpRequest) {
        logger.info("Signing up new user: {}", signUpRequest.getUserName());
        
        // Check if username already exists (prevents duplicate usernames)
        if (repo.findByNameCaseSensitive(signUpRequest.getUserName()).isPresent()) {
            logger.warn("Username {} already exists", signUpRequest.getUserName());
            return false;
        }
        
        // Create new user object
        UserResponse userInfo = new UserResponse();
        userInfo.setName(signUpRequest.getUserName());
        
        // Encrypt the password using BCrypt algorithm before storing
        // Never store plain text passwords! This ensures security
        userInfo.setPassword(passwordEncoder.encode(signUpRequest.getPassword()));
        userInfo.setEmail(signUpRequest.getEmail());
        
        // Save the new user to database
        repo.save(userInfo);
        logger.info("User {} signed up successfully", signUpRequest.getUserName());
        return true;
    }

    /**
     * Change an existing user's password.
     * Verifies the old password before allowing password change.
     * 
     * @param changePasswordRequest Request containing userId, oldPassword, and newPassword
     * @return true if password changed successfully, false if old password doesn't match
     */
    public boolean changePassword(ChangePasswordRequest changePasswordRequest) {
        logger.info("Attempting to change password for user: {}", changePasswordRequest.getUserId());
        
        return repo.findById(Long.parseLong(changePasswordRequest.getUserId()))
                .map(userInfo -> {
                    // Verify that the provided old password matches the encrypted password in database
                    // passwordEncoder.matches() compares plain text with hashed password
                    if (passwordEncoder.matches(changePasswordRequest.getOldPassword(), userInfo.getPassword())) {
                        // Old password is correct, so set new encrypted password
                        userInfo.setPassword(passwordEncoder.encode(changePasswordRequest.getNewPassword()));
                        // Persist the changes to database
                        repo.save(userInfo);
                        logger.info("Password changed successfully for user: {}", changePasswordRequest.getUserId());
                        return true;
                    }
                    logger.warn("Old password does not match for user: {}", changePasswordRequest.getUserId());
                    return false;
                })
                // If user not found, return false
                .orElse(false);
    }

    /**
     * Reset user's password to default value.
     * This is typically used when user forgets their password and resets it via email.
     * Verifies email before resetting for security.
     * 
     * @param forgotPasswordRequest Request containing userId and email
     * @return true if password reset successfully, false if email doesn't match or user not found
     */
    public boolean forgotPassword(ForgotPasswordRequest forgotPasswordRequest) {
        logger.info("Attempting to reset password for user: {}", forgotPasswordRequest.getUserId());
        
        return repo.findById(Long.parseLong(forgotPasswordRequest.getUserId()))
                .map(userInfo -> {
                    // Verify that the email matches the user's registered email (security check)
                    if (!userInfo.getEmail().equals(forgotPasswordRequest.getEmail())) {
                        logger.warn("Email does not match for user: {}", forgotPasswordRequest.getUserId());
                        return false;
                    }
                    
                    // Reset password to the default reset password (e.g., "888888" from Constants)
                    userInfo.setPassword(passwordEncoder.encode(Constants.DEFAULT_RESET_PASSWORD));
                    // Save the reset password to database
                    repo.save(userInfo);
                    logger.info("Password reset successfully for user: {}", forgotPasswordRequest.getUserId());
                    return true;
                })
                // If user not found, return false
                .orElse(false);        
    }

    /**
     * Delete a user account permanently.
     * 
     * @param userId The ID of the user to delete
     * @return true if account deleted successfully, false if user not found
     */
    public boolean deleteAccount(Long userId) {
        logger.info("Attempting to delete account for user: {}", userId);
        
        return repo.findById(userId)
                .map(userInfo -> {
                    // Delete the user from database
                    repo.delete(userInfo);
                    logger.info("Account deleted successfully for user: {}", userId);
                    return true;
                })
                // If user not found, return false
                .orElse(false);
    }
}