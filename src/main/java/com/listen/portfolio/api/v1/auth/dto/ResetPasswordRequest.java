package com.listen.portfolio.api.v1.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 密码最终重置请求传输对象 (Reset Password Request DTO)。
 *
 * <h3>业务与安全设计：</h3>
 * <ul>
 *   <li><b>重置令牌验证：</b>字段 {@code token} 必须非空，用于去 Redis 校验
 *       {@code password_reset:<token>} 键的存在性与有效时效（TTL）；</li>
 *   <li><b>密码长度约束：</b>强制 {@link Size} 在 6 到 100 字符之间，
 *       拦截过短的弱口令，同时防范过长文本引起的哈希 DoS 攻击；</li>
 *   <li><b>传输安全性：</b>新密码通过 HTTPS 传输，并在业务层立即被 BCrypt 加密为摘要落盘。</li>
 * </ul>
 */
public class ResetPasswordRequest {

    /** 邮件内分发的高熵安全重置令牌字符串（必填） */
    @NotBlank(message = "Token must not be blank")
    private String token;

    /** 用户设定的新明文密码（长度必须在 6-100 字符之间） */
    @NotBlank(message = "New password must not be blank")
    @Size(min = 6, max = 100, message = "Password must be between 6 and 100 characters")
    private String newPassword;

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }
}
