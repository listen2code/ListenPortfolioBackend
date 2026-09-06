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
     * 更新用户头像 (Base64 或 URL)
     */
    @Transactional
    public Optional<UserSummaryDto> updateAvatar(String username, String base64Data) {
        logger.info("Attempting to update avatar for user: {}", username);
        if (!isValidAvatarData(base64Data)) {
            logger.warn("Invalid avatar data provided for user: {}", username);
            throw new IllegalArgumentException("Invalid avatar image format or size");
        }
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

    /**
     * 验证头像数据是否合法（HTTP/HTTPS URL 或 Base64 图片数据）
     */
    public boolean isValidAvatarData(String avatarData) {
        if (avatarData == null || avatarData.isBlank()) {
            return false;
        }

        // 允许 HTTP/HTTPS 图片 URL (最大 2048 字符，不能包含换行符)
        if (avatarData.startsWith("http://") || avatarData.startsWith("https://")) {
            return avatarData.length() <= 2048 && !avatarData.contains("\n") && !avatarData.contains("\r");
        }

        // Base64 字符串长度限制：最大 3MB (~2MB 二进制图片)
        if (avatarData.length() > 3 * 1024 * 1024) {
            logger.warn("Avatar base64 data exceeds max length of 3MB: {}", avatarData.length());
            return false;
        }

        // 必须为合法的 data:image/ 前缀
        String lowerCaseData = avatarData.toLowerCase();
        if (!lowerCaseData.startsWith("data:image/")) {
            return false;
        }

        int commaIndex = avatarData.indexOf(",");
        if (commaIndex == -1) {
            return false;
        }

        String metadata = lowerCaseData.substring(0, commaIndex);
        if (!metadata.contains(";base64")) {
            return false;
        }

        String base64Payload = avatarData.substring(commaIndex + 1).trim();
        if (base64Payload.isEmpty()) {
            return false;
        }

        byte[] decodedBytes;
        try {
            decodedBytes = java.util.Base64.getDecoder().decode(base64Payload);
        } catch (IllegalArgumentException e) {
            logger.warn("Avatar base64 decoding failed: {}", e.getMessage());
            return false;
        }

        if (decodedBytes.length == 0 || decodedBytes.length > 2 * 1024 * 1024) {
            logger.warn("Avatar binary size invalid: {} bytes", decodedBytes.length);
            return false;
        }

        // 校验图片魔数 (Magic Bytes) 或 SVG 格式
        return isValidImageBytes(decodedBytes, metadata);
    }

    private boolean isValidImageBytes(byte[] bytes, String metadata) {
        // PNG: 89 50 4E 47 0D 0A 1A 0A
        if (bytes.length >= 8 &&
                (bytes[0] & 0xFF) == 0x89 &&
                bytes[1] == 0x50 &&
                bytes[2] == 0x4E &&
                bytes[3] == 0x47 &&
                bytes[4] == 0x0D &&
                bytes[5] == 0x0A &&
                bytes[6] == 0x1A &&
                bytes[7] == 0x0A) {
            return true;
        }

        // JPEG: FF D8 FF
        if (bytes.length >= 3 &&
                (bytes[0] & 0xFF) == 0xFF &&
                (bytes[1] & 0xFF) == 0xD8 &&
                (bytes[2] & 0xFF) == 0xFF) {
            return true;
        }

        // GIF: GIF87a 或 GIF89a
        if (bytes.length >= 6 &&
                bytes[0] == 'G' &&
                bytes[1] == 'I' &&
                bytes[2] == 'F' &&
                bytes[3] == '8' &&
                (bytes[4] == '7' || bytes[4] == '9') &&
                bytes[5] == 'a') {
            return true;
        }

        // WebP: RIFF....WEBP
        if (bytes.length >= 12 &&
                bytes[0] == 'R' &&
                bytes[1] == 'I' &&
                bytes[2] == 'F' &&
                bytes[3] == 'F' &&
                bytes[8] == 'W' &&
                bytes[9] == 'E' &&
                bytes[10] == 'B' &&
                bytes[11] == 'P') {
            return true;
        }

        // SVG (允许 data:image/svg+xml;base64)
        if (metadata.contains("svg")) {
            String content = new String(bytes, java.nio.charset.StandardCharsets.UTF_8).trim();
            return content.startsWith("<svg") || content.startsWith("<?xml") || content.contains("<svg");
        }

        return false;
    }
}