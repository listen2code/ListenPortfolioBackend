package com.listen.portfolio.service;

import com.listen.portfolio.api.v1.about.dto.AboutMeDto;
import com.listen.portfolio.api.v1.about.dto.EducationDto;
import com.listen.portfolio.api.v1.about.dto.ExperienceDto;
import com.listen.portfolio.api.v1.about.dto.LanguageDto;
import com.listen.portfolio.api.v1.about.dto.SkillDto;
import com.listen.portfolio.api.v1.about.dto.StatDto;
import com.listen.portfolio.model.response.AboutMeResponse;
import com.listen.portfolio.model.response.EducationResponse;
import com.listen.portfolio.model.response.ExperienceResponse;
import com.listen.portfolio.model.response.LanguageResponse;
import com.listen.portfolio.model.response.SkillResponse;
import com.listen.portfolio.model.response.StatResponse;
import com.listen.portfolio.model.response.UserResponse;
import com.listen.portfolio.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class AboutMeService {

    private static final Logger logger = LoggerFactory.getLogger(AboutMeService.class);
    private final UserRepository userInfoRepository;

    public AboutMeService(UserRepository userInfoRepository) {
        this.userInfoRepository = userInfoRepository;
    }

    /**
     * 事务说明（中文）：
     * - 使用 @Transactional(readOnly = true) 开启只读事务
     * - 目的：控制查询在同一持久化上下文中完成，降低事务开销，避免无意义的 flush/脏检查
     * - 注意：仅进行读操作；从实体装配到响应对象在事务内完成，避免后续序列化阶段二次查询
     */
    @Transactional(readOnly = true)
    public Optional<AboutMeResponse> getAboutMe() {
        // 说明（中文）：关于我页面查询属于只读操作，标记为只读事务以减少持久化上下文开销
        // Assuming we are fetching a specific user's "About Me" page, e.g., user with ID 1.
        long userId = 1L;
        Optional<UserResponse> userInfoOptional = userInfoRepository.findById(userId);

        if (userInfoOptional.isEmpty()) {
            logger.warn("User with id {} not found for AboutMe page.", userId);
            return Optional.empty();
        }

        UserResponse userInfo = userInfoOptional.get();
        AboutMeResponse aboutMe = new AboutMeResponse();

        aboutMe.setStatus(userInfo.getStatus());
        aboutMe.setJobTitle(userInfo.getJobTitle());
        aboutMe.setBio(userInfo.getBio());
        aboutMe.setGraduationYear(userInfo.getGraduationYear());
        aboutMe.setGithub(userInfo.getGithubUrl());
        aboutMe.setMajor(userInfo.getMajor());
        aboutMe.setCertifications(userInfo.getCertifications());
        aboutMe.setStats(userInfo.getStats());
        aboutMe.setExperiences(userInfo.getExperiences());
        aboutMe.setEducation(userInfo.getEducation());
        aboutMe.setLanguages(userInfo.getLanguages());
        aboutMe.setSkills(userInfo.getSkills());

        return Optional.of(aboutMe);
    }

    /**
     * 说明（中文）：
     * - 新版接口建议返回 DTO（AboutMeDto），避免 JPA Entity 直接暴露到 API 层导致耦合与懒加载风险
     * - 原理：在 Service 的只读事务内完成实体到 DTO 的转换，Controller 只返回 DTO
     * - 兼容策略：保留 getAboutMe()（旧返回结构），逐步迁移后再移除旧方法
     */
    @Transactional(readOnly = true)
    public Optional<AboutMeDto> getAboutMeDto() {
        long userId = 1L;
        Optional<UserResponse> userInfoOptional = userInfoRepository.findById(userId);
        if (userInfoOptional.isEmpty()) {
            logger.warn("User with id {} not found for AboutMe page.", userId);
            return Optional.empty();
        }

        UserResponse userInfo = userInfoOptional.get();
        AboutMeDto dto = new AboutMeDto();
        dto.setStatus(userInfo.getStatus());
        dto.setJobTitle(userInfo.getJobTitle());
        dto.setBio(userInfo.getBio());
        dto.setGraduationYear(userInfo.getGraduationYear());
        dto.setGithub(userInfo.getGithubUrl());
        dto.setMajor(userInfo.getMajor());
        dto.setCertifications(nullToEmpty(userInfo.getCertifications()));
        dto.setStats(toStatDtos(userInfo.getStats()));
        dto.setExperiences(toExperienceDtos(userInfo.getExperiences()));
        dto.setEducation(toEducationDtos(userInfo.getEducation()));
        dto.setLanguages(toLanguageDtos(userInfo.getLanguages()));
        dto.setSkills(toSkillDtos(userInfo.getSkills()));
        return Optional.of(dto);
    }

    private List<String> nullToEmpty(List<String> value) {
        return value == null ? Collections.emptyList() : value;
    }

    private List<StatDto> toStatDtos(List<StatResponse> stats) {
        if (stats == null) {
            return Collections.emptyList();
        }
        return stats.stream()
                .map(this::toStatDto)
                .collect(Collectors.toList());
    }

    private StatDto toStatDto(StatResponse entity) {
        StatDto dto = new StatDto();
        dto.setId(entity.getId());
        dto.setBusinessId(entity.getBusinessId());
        dto.setYear(entity.getYear());
        dto.setLabel(entity.getLabel());
        dto.setTags(nullToEmpty(entity.getTags()));
        return dto;
    }

    private List<ExperienceDto> toExperienceDtos(List<ExperienceResponse> experiences) {
        if (experiences == null) {
            return Collections.emptyList();
        }
        return experiences.stream()
                .map(this::toExperienceDto)
                .collect(Collectors.toList());
    }

    private ExperienceDto toExperienceDto(ExperienceResponse entity) {
        ExperienceDto dto = new ExperienceDto();
        dto.setId(entity.getId());
        dto.setTitle(entity.getTitle());
        dto.setCompany(entity.getCompany());
        dto.setPeriod(entity.getPeriod());
        dto.setDescription(entity.getDescription());
        return dto;
    }

    private List<EducationDto> toEducationDtos(List<EducationResponse> education) {
        if (education == null) {
            return Collections.emptyList();
        }
        return education.stream()
                .map(this::toEducationDto)
                .collect(Collectors.toList());
    }

    private EducationDto toEducationDto(EducationResponse entity) {
        EducationDto dto = new EducationDto();
        dto.setId(entity.getId());
        dto.setDegree(entity.getDegree());
        dto.setSchool(entity.getSchool());
        dto.setPeriod(entity.getPeriod());
        dto.setDescription(entity.getDescription());
        return dto;
    }

    private List<LanguageDto> toLanguageDtos(List<LanguageResponse> languages) {
        if (languages == null) {
            return Collections.emptyList();
        }
        return languages.stream()
                .map(this::toLanguageDto)
                .collect(Collectors.toList());
    }

    private LanguageDto toLanguageDto(LanguageResponse entity) {
        LanguageDto dto = new LanguageDto();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setLevel(entity.getLevel());
        return dto;
    }

    private List<SkillDto> toSkillDtos(List<SkillResponse> skills) {
        if (skills == null) {
            return Collections.emptyList();
        }
        return skills.stream()
                .map(this::toSkillDto)
                .collect(Collectors.toList());
    }

    private SkillDto toSkillDto(SkillResponse entity) {
        SkillDto dto = new SkillDto();
        dto.setId(entity.getId());
        dto.setCategory(entity.getCategory());
        dto.setItems(nullToEmpty(entity.getItems()));
        return dto;
    }
}
