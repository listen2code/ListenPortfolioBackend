package com.listen.portfolio.api.v1.auth.dto;

import lombok.Data;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * 忘记密码申请请求传输对象 (Forgot Password Request DTO)。
 *
 * <h3>业务与安全规范：</h3>
 * <ul>
 *   <li><b>非空约束：</b>使用 {@link NotBlank} 拦截空值与纯空白字符输入，保护底层业务层免受无效调用；</li>
 *   <li><b>邮箱格式校验：</b>使用 {@link Email} 实施标准 RFC 5322 邮箱格式正则校验，
 *       在 Controller 层快速失败，避免向明显无效的格式发起耗时的 Redis 查询与 SMTP 邮件投递；</li>
 *   <li><b>防枚举契约配合：</b>此 DTO 虽要求合法邮箱格式，但无论数据库是否存在该邮箱，
 *       接口均统一返回成功消息，防御账号探测攻击。</li>
 * </ul>
 */
@Data
public class ForgotPasswordRequest {

    /** 目标注册用户的邮箱地址（必填，且必须符合标准邮箱格式） */
    @NotBlank(message = "email must not be blank")
    @Email(message = "email must be valid")
    private String email;
}
