package com.listen.portfolio.api.v1.about.dto;

import lombok.Data;

import java.util.List;

@Data
/**
 * AboutMe DTO（API 返回对象）。
 *
 * 说明：
 * - 用于替代直接返回 JPA Entity，避免实体结构泄露到 API 层以及事务外懒加载风险
 * - 字段命名与前端期望保持一致（例如 github 对应 users.githubUrl）
 */
public class AboutMeDto {
    private String status;
    private String jobTitle;
    private String bio;
    private String graduationYear;
    private String github;
    private String major;
    private List<String> certifications;
    private List<StatDto> stats;
    private List<ExperienceDto> experiences;
    private List<EducationDto> education;
    private List<LanguageDto> languages;
    private List<SkillDto> skills;
}
