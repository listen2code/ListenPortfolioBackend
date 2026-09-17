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
     * 忘记密码处理核心业务逻辑 (Forgot Password Flow)。
     *
     * <h3>安全防御与架构设计考量：</h3>
     * <ul>
     *   <li><b>防止账号/邮箱枚举嗅探 (Anti-User-Enumeration Pattern)：</b>
     *       无论目标邮箱在数据库中是否存在，本方法均始终静默返回 {@code true}，对外统一输出成功文案。
     *       防止恶意黑客通过接口返回的状态码、耗时或错误消息逆向探测全站注册用户的邮箱列表。</li>
     *   <li><b>一次性安全凭证签发 (CSPRNG Token)：</b>
     *       若用户存在，调用 {@link PasswordResetTokenService#generateToken(String)} 生成 256 位安全令牌，
     *       并存储至 Redis（带 TTL 强时效保障）。</li>
     *   <li><b>邮件投递异常静默隔离：</b>
     *       邮件网络传输异常（如 SMTP 服务抖动、超时）仅做后台错误日志记录，不向上层暴露内部堆栈，
     *       避免给前端泄露后端网络与中间件配置细节。</li>
     * </ul>
     *
     * @param forgotPasswordRequest 包含目标用户邮箱的请求 DTO
     * @return 恒为 {@code true}，遵循防枚举统一响应契约
     */
    @Transactional
    public boolean forgotPassword(ForgotPasswordRequest forgotPasswordRequest) {
        String email = forgotPasswordRequest.getEmail();
        logger.info(">>> [AuthService] 收到忘记密码请求, 目标邮箱: {}", email);
        
        // 1. 基于邮箱精确检索用户实体
        UserEntity user = userMapper.selectOne(
                new LambdaQueryWrapper<UserEntity>().eq(UserEntity::getEmail, email)
        );
        
        if (user != null) {
            try {
                // 2. 签发高熵重置令牌并写入 Redis (默认 TTL 3600 秒)
                String resetToken = passwordResetTokenService.generateToken(user.getEmail());
                
                // 3. 异步/同步调用邮件服务渲染 HTML 模板并发送
                emailService.sendPasswordResetEmail(
                    user.getEmail(),
                    user.getName(),
                    resetToken
                );
                logger.info(">>> [AuthService] 密码重置邮件投递成功, 邮箱: {}", email);
            } catch (Exception e) {
                // 仅记录审计日志，严禁向外抛出导致接口 500 泄露内部信息
                logger.error(">>> [AuthService] 密码重置邮件投递失败, 邮箱: {}, 错误信息: {}", 
                           email, e.getMessage());
            }
        } else {
            // 防枚举安全设计：当邮箱不存在时不触发任何动作，但保持静默一致性
            logger.debug(">>> [AuthService] 目标邮箱不存在于用户库, 静默忽略以防止账号枚举嗅探: {}", email);
        }
        
        return true;
    }

    /**
     * 基于安全令牌重置用户登录密码 (Reset Password Flow)。
     *
     * <h3>严密的安全重置流转细节：</h3>
     * <ol>
     *   <li><b>Token 凭据有效性与 TTL 核验：</b>
     *       通过 {@link PasswordResetTokenService#getEmailByToken(String)} 校验 Token。
     *       若 Redis 中不存在或已过期，立即以 {@code false} 快速失败，防御凭据伪造与过期利用。</li>
     *   <li><b>所有者一致性校验：</b>
     *       以 Token 关联的 Email 检索数据库中最新用户记录。若用户不存在（如被物理删除），
     *       则立即物理注销该 Token，杜绝幽灵凭据残留。</li>
     *   <li><b>BCrypt 密码加盐哈希：</b>
     *       使用 {@link PasswordEncoder#encode(CharSequence)} 重新计算新密码的哈希摘要，
     *       生成包含随机 Salt 与 Cost Factor 的高强度密文落盘更新。</li>
     *   <li><b>单次使用即作废语义 (One-Time Semantic)：</b>
     *       更新密码成功后，立即调用 {@link PasswordResetTokenService#deleteToken(String)}
     *       从 Redis 彻底擦除该 Token，严防任何形式的重放修改攻击。</li>
     *   <li><b>全端会话协同吊销 (Global Session Invalidation)：</b>
     *       调用 {@link RefreshTokenService#revokeAllRefreshTokens(String)}，
     *       彻底注销该用户当前在所有设备（Web、App 等）上持有的长效凭证，强制全终端重新登录认证。</li>
     * </ol>
     *
     * @param token       客户端提交的一次性重置令牌
     * @param newPassword 用户设定的新明文密码
     * @return {@code true} 表示密码更新成功且全端凭据已刷新；{@code false} 表示 Token 非法、已过期或用户不存在
     */
    @Transactional
    public boolean resetPassword(String token, String newPassword) {
        logger.info(">>> [AuthService] 尝试执行密码重置, Token 验证中...");
        
        // 1. 从 Redis 检索并核验 Token 对应的目标用户邮箱
        String email = passwordResetTokenService.getEmailByToken(token);
        if (email == null) {
            logger.warn(">>> [AuthService] 密码重置失败: Token 无效或已过期失效");
            return false;
        }
        
        // 2. 检索绑定的用户数据库实体
        UserEntity user = userMapper.selectOne(
                new LambdaQueryWrapper<UserEntity>().eq(UserEntity::getEmail, email)
        );
        
        if (user == null) {
            logger.error(">>> [AuthService] 密码重置失败: 数据库不存在邮箱为 [{}] 的用户, 正在清理残留 Token", email);
            passwordResetTokenService.deleteToken(token);
            return false;
        }
        
        // 3. 执行 BCrypt 加盐哈希并更新持久化实体
        user.setPassword(passwordEncoder.encode(newPassword));
        userMapper.updateById(user);
        
        // 4. 物理删除 Token，确保一次性使用不可复用
        passwordResetTokenService.deleteToken(token);

        // 5. 关键安全闭环：吊销该用户所有活跃的 Refresh Token，强制全端重新登录
        refreshTokenService.revokeAllRefreshTokens(user.getName());
        
        logger.info(">>> [AuthService] 用户 [{}] 密码已成功重置, 历史 Refresh Token 已全量吊销", user.getId());
        return true;
    }
}
