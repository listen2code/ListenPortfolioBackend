package com.listen.portfolio.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.listen.portfolio.api.v1.auth.dto.ForgotPasswordRequest;
import com.listen.portfolio.api.v1.auth.dto.SignUpRequest;
import com.listen.portfolio.entity.UserEntity;
import com.listen.portfolio.mapper.UserMapper;
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
import java.util.Optional;

/**
 * 认证服务类（MyBatis-Plus 版本）
 * 说明：专门处理用户认证相关的业务逻辑，包括注册、登录、密码管理等
 */
@Service
public class AuthService implements UserDetailsService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final PasswordResetTokenService passwordResetTokenService;
    private final RefreshTokenService refreshTokenService;

    public AuthService(UserMapper userMapper, @Lazy PasswordEncoder passwordEncoder, 
                      EmailService emailService, PasswordResetTokenService passwordResetTokenService,
                      RefreshTokenService refreshTokenService) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.passwordResetTokenService = passwordResetTokenService;
        this.refreshTokenService = refreshTokenService;
    }

    /**
     * Spring Security认证核心方法
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        logger.info("Loading user by username for security context: {}", username);
        
        UserEntity user = userMapper.findByNameCaseSensitive(username);
        if (user == null) {
            logger.warn("User not found with username: {}", username);
            throw new UsernameNotFoundException("User not found with username: " + username);
        }
        
        logger.info("User found: {}", username);
        return new User(user.getName(), user.getPassword(), new ArrayList<>());
    }

    /**
     * 根据用户名查询用户信息
     */
    @Transactional(readOnly = true)
    public Optional<UserEntity> getUserByName(String username) {
        logger.info("Fetching user by name: {}", username);
        return Optional.ofNullable(userMapper.findByNameCaseSensitive(username));
    }

    public enum SignUpResult {
        SUCCESS,
        USERNAME_EXISTS,
        EMAIL_EXISTS
    }

    /**
     * 详细注册接口，区分用户名与邮箱冲突
     */
    @Transactional
    public SignUpResult signUpResult(SignUpRequest signUpRequest) {
        logger.info("Signing up new user: {}", signUpRequest.getUserName());
        
        // 检查用户名是否已存在（区分大小写）
        if (userMapper.findByNameCaseSensitive(signUpRequest.getUserName()) != null) {
            logger.warn("Username {} already exists", signUpRequest.getUserName());
            return SignUpResult.USERNAME_EXISTS;
        }

        // 检查邮箱是否已存在
        if (signUpRequest.getEmail() != null && !signUpRequest.getEmail().isBlank()) {
            UserEntity existingUserByEmail = userMapper.selectOne(
                    new LambdaQueryWrapper<UserEntity>().eq(UserEntity::getEmail, signUpRequest.getEmail())
            );
            if (existingUserByEmail != null) {
                logger.warn("Email {} already exists", signUpRequest.getEmail());
                return SignUpResult.EMAIL_EXISTS;
            }
        }
        
        // 创建新用户对象
        UserEntity userInfo = new UserEntity();
        userInfo.setName(signUpRequest.getUserName());
        userInfo.setPassword(passwordEncoder.encode(signUpRequest.getPassword()));
        userInfo.setEmail(signUpRequest.getEmail());
        userInfo.setAvatarUrl("https://api.dicebear.com/10.x/bottts/svg?seed=" + signUpRequest.getUserName());
        
        userMapper.insert(userInfo);
        logger.info("User {} signed up successfully", signUpRequest.getUserName());
        return SignUpResult.SUCCESS;
    }

    @Transactional
    public boolean signUp(SignUpRequest signUpRequest) {
        return signUpResult(signUpRequest) == SignUpResult.SUCCESS;
    }

    /**
     * 忘记密码功能
     */
    @Transactional
    public boolean forgotPassword(ForgotPasswordRequest forgotPasswordRequest) {
        String email = forgotPasswordRequest.getEmail();
        logger.info("Password reset requested for email: {}", email);
        
        UserEntity user = userMapper.selectOne(
                new LambdaQueryWrapper<UserEntity>().eq(UserEntity::getEmail, email)
        );
        
        if (user != null) {
            try {
                String resetToken = passwordResetTokenService.generateToken(user.getEmail());
                emailService.sendPasswordResetEmail(
                    user.getEmail(),
                    user.getName(),
                    resetToken
                );
                logger.info("Password reset email sent successfully to: {}", email);
            } catch (Exception e) {
                logger.error("Failed to send password reset email to: {}, error: {}", 
                           email, e.getMessage());
            }
        } else {
            logger.debug("Password reset requested for non-existent email (silent fail for security)");
        }
        
        return true;
    }

    /**
     * 重置密码功能
     */
    @Transactional
    public boolean resetPassword(String token, String newPassword) {
        logger.info("Attempting to reset password with token");
        
        String email = passwordResetTokenService.getEmailByToken(token);
        if (email == null) {
            logger.warn("Invalid or expired password reset token");
            return false;
        }
        
        UserEntity user = userMapper.selectOne(
                new LambdaQueryWrapper<UserEntity>().eq(UserEntity::getEmail, email)
        );
        
        if (user == null) {
            logger.error("User not found for email: {} (token valid but user deleted)", email);
            passwordResetTokenService.deleteToken(token);
            return false;
        }
        
        user.setPassword(passwordEncoder.encode(newPassword));
        userMapper.updateById(user);
        
        passwordResetTokenService.deleteToken(token);
        refreshTokenService.revokeAllRefreshTokens(user.getName());
        
        logger.info("Password reset successfully for user: {}", user.getId());
        return true;
    }
}
