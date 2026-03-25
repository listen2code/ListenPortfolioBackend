package com.listen.portfolio.api.v1.about.dto;

import lombok.Data;

@Data
/**
 * Language DTO（API 返回对象）。
 *
 * 说明：
 * - 对应 languages 表展示字段
 * - 不包含 user 字段，避免实体透传与循环引用
 */
public class LanguageDto {
    private Long id;
    private String name;
    private String level;
}
