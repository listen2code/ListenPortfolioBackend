package com.listen.portfolio.api.v1.user.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

@Data
/**
 * UserSummary DTO（API 返回对象）。
 *
 * 说明（中文）：
 * - 用于替代直接返回 UserResponse(JPA Entity) 或放在 model/response 下的“伪 DTO”
 * - 目的：让 API 层只暴露必要字段，降低与持久化层的耦合，避免事务外懒加载导致的不确定问题
 * - 兼容性：字段名与当前接口返回保持一致（id/name/location/email/avatarUrl）
 */
public class UserSummaryDto {
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    private String name;
    private String location;
    private String email;
    private String avatarUrl;
}

