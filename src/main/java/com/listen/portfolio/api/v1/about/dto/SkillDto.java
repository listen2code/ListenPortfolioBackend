package com.listen.portfolio.api.v1.about.dto;

import lombok.Data;

import java.util.List;

@Data
/**
 * Skill DTO（API 返回对象）。
 *
 * 说明（中文）：
 * - 对应 skills 表展示字段
 * - 不包含 user 字段，避免实体透传与循环引用
 */
public class SkillDto {
    private Long id;
    private String category;
    private List<String> items;
}
