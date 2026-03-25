package com.listen.portfolio.service;

import com.listen.portfolio.api.v1.auth.dto.ChangePasswordRequest;
import com.listen.portfolio.api.v1.auth.dto.ForgotPasswordRequest;
import com.listen.portfolio.api.v1.auth.dto.SignUpRequest;
import com.listen.portfolio.api.v1.user.dto.UserSummaryDto;
import com.listen.portfolio.infrastructure.persistence.entity.UserEntity;
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
import org.springframework.transaction.annotation.Transactional;

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
    /**
     * 事务说明（中文）：
     * - 使用 @Transactional(readOnly = true) 开启只读事务
     * - 目的：避免不必要的脏检查与 flush，降低开销；保证读取在同一持久化上下文中完成
     * - 注意：只读事务不等于强制不可写，但约定不在该方法内进行写操作
     */
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        logger.info("Loading user by username for security context: {}", username);
        
        // Search for user in database by username (case-sensitive search)
        UserEntity user = repo.findByNameCaseSensitive(username)
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
     * Get user by username (returns JPA Entity, not Spring Security's UserDetails).
     * 
     * @param username The username to search for (case-sensitive)
     * @return Optional containing the user if found
     */
    @Transactional(readOnly = true)
    public Optional<UserEntity> getUserByName(String username) {
        logger.info("Fetching user by name: {}", username);
        return repo.findByNameCaseSensitive(username);
    }

    /**
     * Retrieve all users from the database.
     * 
     * @return List of all user objects
     */
    @Transactional(readOnly = true)
    public List<UserEntity> getAllUsers() {
        logger.info("Fetching all users");
        List<UserEntity> users = repo.findAll();
        logger.info("Found {} users", users.size());
        return users;
    }

    /**
     * Get a specific user by their ID.
     * 
     * @param id The user's ID (primary key)
     * @return Optional containing the user if found
     */
    @Transactional(readOnly = true)
    public Optional<UserEntity> getUserById(Long id) {
        logger.info("Fetching user by id: {}", id);
        Optional<UserEntity> user = repo.findById(id);
        if (user.isPresent()) {
            logger.info("Found user: {}", user.get());
        } else {
            logger.warn("User with id: {} not found", id);
        }
        return user;
    }

    /**
     * 说明（中文）：
     * - API 层推荐使用 DTO 返回（UserSummaryDto），避免直接暴露 JPA Entity 或历史遗留的 model/response“伪 DTO”
     * - 原理：在 Service 的只读事务内完成实体到 DTO 的转换，Controller 只负责返回 DTO
     */
    @Transactional(readOnly = true)
    public Optional<UserSummaryDto> getUserSummaryById(Long id) {
        logger.info("Fetching user summary by id: {}", id);
        return repo.findById(id)
                .map(this::toUserSummaryDto);
    }

    private UserSummaryDto toUserSummaryDto(UserEntity entity) {
        UserSummaryDto dto = new UserSummaryDto();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setLocation(entity.getLocation());
        dto.setEmail(entity.getEmail());
        dto.setAvatarUrl(entity.getAvatarUrl());
        return dto;
    }

    /**
     * Register a new user (sign up).
     * Validates that username doesn't already exist and encodes the password.
     * 
     * @param signUpRequest Request object containing username, password and email
     * @return true if signup successful, false if username already exists
     */
    /**
     * 事务说明（中文）：
     * - 使用 @Transactional 开启读写事务
     * - 目的：将“查重 + 新增用户”置于同一原子操作中，发生运行时异常时整体回滚，保证一致性
     * - 原理：Spring 默认对 RuntimeException 回滚；同一事务共享持久化上下文，避免中途可见性问题
     */
    @Transactional
    public boolean signUp(SignUpRequest signUpRequest) {
        // 说明（中文）：注册流程包含“检查用户名是否存在 + 写入用户记录”两个步骤，需要原子性，避免并发下产生脏数据
        // 原理：@Transactional 会将方法内数据库操作放在同一事务中，发生异常会回滚
        // 注意：日志为英文，且不打印密码等敏感信息
        logger.info("Signing up new user: {}", signUpRequest.getUserName());
        
        // Check if username already exists (prevents duplicate usernames)
        if (repo.findByNameCaseSensitive(signUpRequest.getUserName()).isPresent()) {
            logger.warn("Username {} already exists", signUpRequest.getUserName());
            return false;
        }
        
        // Create new user object
        UserEntity userInfo = new UserEntity();
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
    /**
     * 事务说明（中文）：
     * - 使用 @Transactional 开启读写事务，确保“校验旧密码 + 写入新密码”要么全部成功、要么全部回滚
     * - 默认在 RuntimeException 时回滚，保证密码更新的原子性与一致性
     */
    @Transactional
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
    /**
     * 事务说明（中文）：
     * - 使用 @Transactional 开启读写事务，保证“根据邮箱查询 + 重置密码写回”在同一事务中完成
     * - 异常时整体回滚，避免部分状态更新导致的不一致
     */
    @Transactional
    public boolean forgotPassword(ForgotPasswordRequest forgotPasswordRequest) {
        logger.info("Attempting to reset password for email: {}", forgotPasswordRequest.getEmail());
        
        return repo.findByEmail(forgotPasswordRequest.getEmail())
                .map(userInfo -> {
                    // Reset password to the default reset password (e.g., "888888" from Constants)
                    userInfo.setPassword(passwordEncoder.encode(Constants.DEFAULT_RESET_PASSWORD));
                    // Save the reset password to database
                    repo.save(userInfo);
                    logger.info("Password reset successfully for user: {}", userInfo.getId());
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
    /**
     * 事务说明（中文）：
     * - 使用 @Transactional 开启读写事务，确保删除操作在事务内执行，异常自动回滚
     * - 若未来切换为软删除，也可在同一事务内完成多表一致性更新
     */
    @Transactional
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
