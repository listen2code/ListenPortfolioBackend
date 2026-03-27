package com.listen.portfolio.service;

import com.listen.portfolio.api.v1.user.dto.UserSummaryDto;
import com.listen.portfolio.api.v1.auth.dto.ChangePasswordRequest;
import com.listen.portfolio.infrastructure.persistence.entity.UserEntity;
import com.listen.portfolio.repository.UserRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * 用户业务服务类
 * 说明：核心业务服务，负责处理用户相关的所有业务逻辑
 * 架构角色：业务逻辑层（Service Layer），隔离表现层与数据访问层
 * 主要职责：
 * - 用户信息查询：根据ID或用户名查询用户基本信息
 * - 用户数据转换：将实体转换为安全的DTO格式
 * - 用户列表管理：提供用户列表查询功能
 * - 用户账户管理：密码修改、账户删除等认证相关操作
 * - 事务管理：确保业务操作的原子性和数据一致性
 * 
 * 设计原则：
 * - 单一职责：负责用户相关的所有业务逻辑
 * - 数据安全：返回DTO而非实体，避免敏感信息泄露
 * - 事务边界：读操作使用只读事务优化性能
 * - 日志记录：关键操作记录日志，便于问题追踪
 * 
 * 依赖注入：
 * - UserRepository：数据访问层，负责数据库操作
 * - PasswordEncoder：Spring Security提供的密码加密组件
 * 
 * 事务策略：
 * - 读操作：使用@Transactional(readOnly = true)优化性能
 * - 写操作：使用@Transactional确保数据一致性
 */
@Service
public class UserService {

    // 日志记录器，用于记录用户相关操作的日志
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    // 用户仓库，负责用户实体的数据库操作
    private final UserRepository repo;

    // 密码编码器，用于安全地加密和验证密码
    private final PasswordEncoder passwordEncoder;

    /**
     * 构造函数 - 依赖注入
     * 说明：通过Spring的依赖注入机制获取所需的组件实例
     * 
     * @param repo 用户仓库接口，提供数据库访问能力
     * @param passwordEncoder Spring Security提供的密码加密器，用于密码的安全加密和验证
     */
    public UserService(UserRepository repo, @Lazy PasswordEncoder passwordEncoder) {
        this.repo = repo;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 根据用户名查询用户信息
     * 说明：提供基础的用户查询功能，返回完整的用户实体对象
     * 使用场景：需要获取用户完整信息的业务逻辑
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
     * 根据用户ID获取用户摘要信息
     * 说明：返回用户的基本信息，避免暴露敏感数据
     * 使用场景：前端显示用户列表、用户卡片等
     * 
     * @param id 用户ID
     * @return 用户摘要DTO的Optional，如果用户不存在则返回空Optional
     */
    @Transactional(readOnly = true)
    public Optional<UserSummaryDto> getUserSummaryById(Long id) {
        logger.info("Fetching user summary by id: {}", id);
        return repo.findById(id)
                .map(this::toUserSummaryDto);
    }

    /**
     * 将用户实体转换为用户摘要DTO
     * 说明：实体到DTO的转换，避免直接暴露数据库实体给前端
     * 原理：只选择需要展示给用户的安全字段，隐藏敏感信息如密码等
     * 扩展性：后续可在此方法中添加字段计算、格式转换等逻辑
     * 
     * @param entity 用户实体对象
     * @return 用户摘要DTO对象，包含安全的用户信息
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