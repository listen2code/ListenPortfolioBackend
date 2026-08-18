package com.listen.portfolio.service;

import com.listen.portfolio.api.v1.user.dto.ChangePasswordRequest;
import com.listen.portfolio.api.v1.user.dto.UserSummaryDto;
import com.listen.portfolio.entity.UserEntity;
import com.listen.portfolio.mapper.UserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * 用户业务服务类（MyBatis-Plus 版本）
 * 说明：核心业务服务，负责处理用户相关的所有业务逻辑
 */
@Service
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserMapper userMapper, @Lazy PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 根据用户名查询用户信息（区分大小写）
     */
    @Transactional(readOnly = true)
    public Optional<UserEntity> getUserByName(String username) {
        logger.info("Fetching user by name: {}", username);
        return Optional.ofNullable(userMapper.findByNameCaseSensitive(username));
    }

    /**
     * 根据用户ID获取用户摘要信息
     */
    @Transactional(readOnly = true)
    public Optional<UserSummaryDto> getUserSummaryById(Long id) {
        logger.info("Fetching user summary by id: {}", id);
        return Optional.ofNullable(userMapper.selectById(id))
                .map(this::toUserSummaryDto);
    }

    /**
     * 将用户实体转换为用户摘要DTO
     */
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
     * 修改用户密码
     */
    @Transactional
    public boolean changePassword(ChangePasswordRequest changePasswordRequest) {
        logger.info("Attempting to change password for user: {}", changePasswordRequest.getUserId());
        
        UserEntity userInfo = userMapper.selectById(Long.parseLong(changePasswordRequest.getUserId()));
        if (userInfo != null) {
            if (passwordEncoder.matches(changePasswordRequest.getOldPassword(), userInfo.getPassword())) {
                userInfo.setPassword(passwordEncoder.encode(changePasswordRequest.getNewPassword()));
                userMapper.updateById(userInfo);
                logger.info("Password changed successfully for user: {}", changePasswordRequest.getUserId());
                return true;
            }
            logger.warn("Old password does not match for user: {}", changePasswordRequest.getUserId());
            return false;
        }
        return false;
    }

    /**
     * 删除用户账户（软删除）
     */
    @Transactional
    public boolean deleteAccount(Long userId) {
        if (userId != null && userId.equals(1L)) {
            logger.warn("Deletion of seed/admin user (userId = 1) is blocked.");
            return false;
        }
        logger.info("Attempting to delete account for user: {}", userId);
        
        UserEntity userInfo = userMapper.selectById(userId);
        if (userInfo != null) {
            userInfo.setDeleted(true);
            userInfo.setEmail("deleted_" + System.currentTimeMillis() + "_" + userInfo.getEmail());
            userInfo.setName("deleted_" + System.currentTimeMillis() + "_" + userInfo.getName());
            userMapper.updateById(userInfo);
            logger.info("Account soft-deleted successfully for user: {}", userId);
            return true;
        }
        return false;
    }

    /**
     * 更新用户头像 (Base64)
     */
    @Transactional
    public Optional<UserSummaryDto> updateAvatar(String username, String base64Data) {
        logger.info("Attempting to update avatar for user: {}", username);
        UserEntity user = userMapper.findByNameCaseSensitive(username);
        if (user == null) {
            return Optional.empty();
        }
        if (user.getId() != null && user.getId() != 1L) {
            logger.warn("User {} (id={}) is not permitted to update avatar", username, user.getId());
            throw new IllegalArgumentException("Only user with id 1 is permitted to change avatar");
        }
        user.setAvatarUrl(base64Data);
        userMapper.updateById(user);
        logger.info("Avatar updated successfully for user: {}", username);
        return Optional.of(toUserSummaryDto(user));
    }
}