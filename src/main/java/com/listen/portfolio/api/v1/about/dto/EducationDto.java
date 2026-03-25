package com.listen.portfolio.api.v1.about.dto;

import lombok.Data;

@Data
/**
 * Education DTO（API 返回对象）。
 *
 * 说明（中文）：
 * - 对应 education 表展示字段
 * - 不包含 user 字段，避免实体透传与循环引用
 */
public class EducationDto {
    private Long id;
    private String degree;
    private String school;
    private String period;
    private String description;
}
