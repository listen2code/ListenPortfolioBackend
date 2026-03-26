package com.listen.portfolio.service;

import com.listen.portfolio.api.v1.auth.dto.ChangePasswordRequest;
import com.listen.portfolio.api.v1.auth.dto.ForgotPasswordRequest;
import com.listen.portfolio.api.v1.auth.dto.SignUpRequest;
import com.listen.portfolio.common.Constants;
import com.listen.portfolio.infrastructure.persistence.entity.UserEntity;
import com.listen.portfolio.repository.UserRepository;

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
 * 认证服务类
 * 说明：专门处理用户认证相关的业务逻辑，包括注册、登录、密码管理等
 * 架构角色：认证业务逻辑层，专注于用户认证和授权相关功能
 * 主要职责：
 * - 用户注册：处理新用户注册流程，包含唯一性检查和密码加密
 * - 密码管理：密码修改、重置等安全相关操作
 * - Spring Security集成：实现UserDetailsService接口支持认证授权
 * - 用户验证：提供用户名查询和身份验证功能
 * 
 * 设计原则：
 * - 单一职责：只负责认证相关业务，不涉及其他业务领域
 * - 安全优先：所有密码操作都经过加密处理，不存储明文密码
 * - 事务管理：确保认证操作的原子性和数据一致性
 * - 日志审计：关键操作都记录日志，便于安全审计和问题追踪
 * 
 * 依赖注入：
 * - UserRepository：数据访问层，负责用户数据操作
 * - PasswordEncoder：Spring Security提供的密码加密组件
 * 
 * 事务策略：
 * - 读操作：使用@Transactional(readOnly = true)优化性能
 * - 写操作：使用@Transactional确保数据一致性
 * 
 * 安全考虑：
 * - 密码必须使用BCrypt算法加密存储，严禁明文保存
 * - 用户名区分大小写，避免视觉混淆攻击
 * - 所有密码操作都记录安全日志，不包含敏感信息
 */
@Service
public class AuthService implements UserDetailsService {

    // 日志记录器，用于记录认证相关操作的日志
    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    // 用户仓库，负责用户实体的数据库操作
    private final UserRepository repo;
    
    // 密码编码器，用于安全地加密和验证密码
    private final PasswordEncoder passwordEncoder;

    /**
     * 构造函数 - 依赖注入
     * 说明：通过Spring的依赖注入机制获取所需的组件实例
     * 设计考虑：
     * - 使用final修饰符确保依赖项不可变，保证线程安全性
     * - @Lazy注解延迟加载PasswordEncoder，避免循环依赖问题
     * - 构造函数注入是推荐的依赖注入方式，便于单元测试
     * 
     * @param repo 用户仓库接口，提供数据库访问能力
     * @param passwordEncoder Spring Security提供的密码加密器，用于密码的安全加密和验证
     */
    public AuthService(UserRepository repo, @Lazy PasswordEncoder passwordEncoder) {
        this.repo = repo;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Spring Security认证核心方法
     * 说明：实现UserDetailsService接口的关键方法，Spring Security在进行用户认证时自动调用
     * 调用时机：用户每次登录时，Spring Security都会调用此方法加载用户信息
     * 安全重要性：这是整个认证系统的入口点，必须确保数据的准确性和安全性
     * 
     * 工作流程：
     * 1. 根据用户名查询用户实体（区分大小写查询）
     * 2. 用户不存在时抛出UsernameNotFoundException异常
     * 3. 用户存在时构建Spring Security的UserDetails对象
     * 4. 返回包含用户名、加密密码和权限信息的UserDetails
     * 
     * @param username 用户名（区分大小写），来自登录表单或JWT令牌
     * @return Spring Security的UserDetails对象，包含用户名、加密密码和权限信息
     * @throws UsernameNotFoundException 当用户不存在时抛出，Spring Security会转换为认证失败响应
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        logger.info("Loading user by username for security context: {}", username);
        
        // 根据用户名在数据库中搜索用户（区分大小写搜索）
        UserEntity user = repo.findByNameCaseSensitive(username)
                // 如果用户不存在，抛出异常
                .orElseThrow(() -> {
                    logger.warn("User not found with username: {}", username);
                    return new UsernameNotFoundException("User not found with username: " + username);
                });
        
        logger.info("User found: {}", username);
        
        // 返回Spring Security的User对象，包含用户名、加密密码和空权限列表
        // 权限（角色/权限）目前为空，但后续可以扩展为从数据库加载用户角色和权限
        return new User(user.getName(), user.getPassword(), new ArrayList<>());
    }

    /**
     * 根据用户名查询用户信息
     * 说明：提供基础的用户查询功能，返回完整的用户实体对象
     * 使用场景：需要获取用户完整信息的认证相关业务逻辑
     * 注意：该方法使用只读事务，不会对数据进行修改
     * 
     * @param username 用户名（区分大小写）
     * @return 包含用户实体的Optional对象，如果用户不存在则返回空Optional
     */
    @Transactional(readOnly = true)
    public Optional<UserEntity> getUserByName(String username) {
        logger.info("Fetching user by name: {}", username);
        return repo.findByNameCaseSensitive(username);
    }

    /**
     * 用户注册服务
     * 说明：处理新用户注册流程，包含用户名唯一性检查和密码加密存储
     * 事务要求：整个注册流程必须原子性执行，避免并发注册产生重复用户
     * 安全考虑：
     * - 密码必须使用BCrypt算法加密存储，严禁明文保存
     * - 用户名区分大小写，避免视觉混淆攻击
     * - 记录操作日志但不包含敏感信息
     * 
     * @param signUpRequest 注册请求对象，包含用户名、密码、邮箱等信息
     * @return 注册成功返回true，用户名已存在返回false
     */
    @Transactional
    public boolean signUp(SignUpRequest signUpRequest) {
        logger.info("Signing up new user: {}", signUpRequest.getUserName());
        
        // 检查用户名是否已存在（防止重复用户名）
        if (repo.findByNameCaseSensitive(signUpRequest.getUserName()).isPresent()) {
            logger.warn("Username {} already exists", signUpRequest.getUserName());
            return false;
        }
        
        // 创建新用户对象
        UserEntity userInfo = new UserEntity();
        userInfo.setName(signUpRequest.getUserName());
        
        // 使用BCrypt算法在存储前对密码进行加密
        // 严禁存储明文密码！这确保了安全性
        userInfo.setPassword(passwordEncoder.encode(signUpRequest.getPassword()));
        userInfo.setEmail(signUpRequest.getEmail());
        
        // 将新用户保存到数据库
        repo.save(userInfo);
        logger.info("User {} signed up successfully", signUpRequest.getUserName());
        return true;
    }

    /**
     * 修改用户密码
     * 说明：验证旧密码正确性后更新为新密码，确保用户身份验证的安全性
     * 事务要求：整个密码修改过程必须原子性执行，避免部分更新导致数据不一致
     * 安全考虑：
     * - 必须验证旧密码的正确性，防止未授权访问
     * - 新密码同样使用BCrypt算法加密存储
     * - 记录操作日志用于安全审计
     * 使用场景：用户在个人中心修改密码，或管理员重置用户密码
     * 
     * @param changePasswordRequest 密码修改请求，包含用户ID、旧密码和新密码
     * @return 密码修改成功返回true，旧密码不匹配或用户不存在返回false
     */
    @Transactional
    public boolean changePassword(ChangePasswordRequest changePasswordRequest) {
        logger.info("Attempting to change password for user: {}", changePasswordRequest.getUserId());
        
        return repo.findById(Long.parseLong(changePasswordRequest.getUserId()))
                .map(userInfo -> {
                    // 验证提供的旧密码是否与数据库中的加密密码匹配
                    // passwordEncoder.matches()方法比较明文密码与数据库中的哈希密码
                    if (passwordEncoder.matches(changePasswordRequest.getOldPassword(), userInfo.getPassword())) {
                        // 旧密码正确，设置新的加密密码
                        userInfo.setPassword(passwordEncoder.encode(changePasswordRequest.getNewPassword()));
                        // 将更改持久化到数据库
                        repo.save(userInfo);
                        logger.info("Password changed successfully for user: {}", changePasswordRequest.getUserId());
                        return true;
                    }
                    logger.warn("Old password does not match for user: {}", changePasswordRequest.getUserId());
                    return false;
                })
            // 如果用户不存在，返回false
                .orElse(false);
    }

    /**
     * 忘记密码重置服务
     * 说明：用户通过邮箱验证后重置密码为默认密码，用于密码找回功能
     * 安全考虑：
     * - 必须通过邮箱验证用户身份，确保是本人操作
     * - 重置为默认密码后，用户首次登录必须强制修改密码
     * - 记录操作日志用于安全审计和异常追踪
     * 使用场景：用户在登录页面点击"忘记密码"，通过邮箱验证后重置密码
     * 注意：实际应用中建议结合邮箱验证码或短信验证码增强安全性
     * 
     * @param forgotPasswordRequest 忘记密码请求，包含用户邮箱信息
     * @return 密码重置成功返回true，邮箱对应的用户不存在返回false
     */
    @Transactional
    public boolean forgotPassword(ForgotPasswordRequest forgotPasswordRequest) {
        logger.info("Attempting to reset password for email: {}", forgotPasswordRequest.getEmail());
        
        return repo.findByEmail(forgotPasswordRequest.getEmail())
                .map(userInfo -> {
                    // 将用户密码重置为默认密码（Constants.DEFAULT_RESET_PASSWORD中定义的默认密码）
                    userInfo.setPassword(passwordEncoder.encode(Constants.DEFAULT_RESET_PASSWORD));
                    // 将重置后的密码保存到数据库
                    repo.save(userInfo);
                    logger.info("Password reset successfully for user: {}", userInfo.getId());
                    return true;
                })
            // 如果用户不存在，返回false
                .orElse(false);        
    }

    /**
     * 删除用户账户
     * 说明：永久删除用户账户及其相关数据，操作不可恢复
     * 安全考虑：
     * - 执行前应该验证操作者权限，确保只有管理员或本人可以删除
     * - 删除前建议进行数据备份，防止误操作导致数据丢失
     * - 记录删除日志用于安全审计和合规要求
     * 扩展考虑：
     * - 可扩展为软删除，添加删除标记而非物理删除
     * - 删除用户时需要考虑级联删除相关数据（如用户发布的文章、评论等）
     * 使用场景：用户主动注销账户或管理员清理无效账户
     * 
     * @param userId 要删除的用户ID
     * @return 删除成功返回true，用户不存在返回false
     */
    @Transactional
    public boolean deleteAccount(Long userId) {
        logger.info("Attempting to delete account for user: {}", userId);
        
        return repo.findById(userId)
                .map(userInfo -> {
                    // 从数据库中删除用户
                    repo.delete(userInfo);
                    logger.info("Account deleted successfully for user: {}", userId);
                    return true;
                })
            // 如果用户不存在，返回false
                .orElse(false);
    }
}
