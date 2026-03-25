package com.listen.portfolio.api.v1.about.dto;

import lombok.Data;

import java.util.List;

@Data
/**
 * Stat DTO（API 返回对象）。
 *
 * 说明（中文）：
 * - 对应 stats 表的“只读展示数据”
 * - 不包含 user 反向引用字段，避免 JSON 循环引用与实体耦合
 */
public class StatDto {
    private Long id;
    private String businessId;
    private String year;
    private String label;
    private List<String> tags;
}
