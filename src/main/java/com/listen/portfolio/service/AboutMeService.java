package com.listen.portfolio.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.listen.portfolio.api.v1.about.dto.AboutMeDto;
import com.listen.portfolio.api.v1.about.dto.EducationDto;
import com.listen.portfolio.api.v1.about.dto.ExperienceDto;
import com.listen.portfolio.api.v1.about.dto.LanguageDto;
import com.listen.portfolio.api.v1.about.dto.SkillDto;
import com.listen.portfolio.api.v1.about.dto.StatDto;
import com.listen.portfolio.common.util.I18nUtils;
import com.listen.portfolio.entity.EducationEntity;
import com.listen.portfolio.entity.ExperienceEntity;
import com.listen.portfolio.entity.LanguageEntity;
import com.listen.portfolio.entity.SkillEntity;
import com.listen.portfolio.entity.StatEntity;
import com.listen.portfolio.entity.UserEntity;
import com.listen.portfolio.mapper.EducationMapper;
import com.listen.portfolio.mapper.ExperienceMapper;
import com.listen.portfolio.mapper.LanguageMapper;
import com.listen.portfolio.mapper.SkillMapper;
import com.listen.portfolio.mapper.StatMapper;
import com.listen.portfolio.mapper.UserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * AboutMe 业务服务类（MyBatis-Plus 版本）
 */
@Service
public class AboutMeService {

    private static final Logger logger = LoggerFactory.getLogger(AboutMeService.class);

    private final UserMapper userMapper;
    private final StatMapper statMapper;
    private final ExperienceMapper experienceMapper;
    private final EducationMapper educationMapper;
    private final LanguageMapper languageMapper;
    private final SkillMapper skillMapper;

    public AboutMeService(UserMapper userMapper, StatMapper statMapper,
                          ExperienceMapper experienceMapper, EducationMapper educationMapper,
                          LanguageMapper languageMapper, SkillMapper skillMapper) {
        this.userMapper = userMapper;
        this.statMapper = statMapper;
        this.experienceMapper = experienceMapper;
        this.educationMapper = educationMapper;
        this.languageMapper = languageMapper;
        this.skillMapper = skillMapper;
    }

    /**
     * 说明：
     * - 在 Service 的只读事务内完成实体与关联集合的装配并转换为 DTO
     * - 根据 LocaleContextHolder 获取客户端 Accept-Language 并做多语言字段动态映射
     */
    @Transactional(readOnly = true)
    public Optional<AboutMeDto> getAboutMeDto(Long userId) {
        if (userId == null) {
            logger.warn("UserId is null for getAboutMeDto");
            return Optional.empty();
        }

        UserEntity userInfo = userMapper.selectById(userId);
        if (userInfo == null) {
            logger.warn("User with userId {} not found for AboutMe page.", userId);
            return Optional.empty();
        }

        // 装配多语言与子集合
        Locale locale = LocaleContextHolder.getLocale();

        List<String> certifications = userMapper.findCertificationsByUserId(userId);
        userInfo.setCertifications(certifications);

        List<StatEntity> stats = statMapper.selectList(
                new LambdaQueryWrapper<StatEntity>().eq(StatEntity::getUserId, userId)
        );
        if (stats != null) {
            for (StatEntity s : stats) {
                s.setTags(statMapper.findTagsByStatId(s.getId()));
            }
        }
        userInfo.setStats(stats);

        List<ExperienceEntity> experiences = experienceMapper.selectList(
                new LambdaQueryWrapper<ExperienceEntity>().eq(ExperienceEntity::getUserId, userId)
        );
        userInfo.setExperiences(experiences);

        List<EducationEntity> education = educationMapper.selectList(
                new LambdaQueryWrapper<EducationEntity>().eq(EducationEntity::getUserId, userId)
        );
        userInfo.setEducation(education);

        List<LanguageEntity> languages = languageMapper.selectList(
                new LambdaQueryWrapper<LanguageEntity>().eq(LanguageEntity::getUserId, userId)
        );
        userInfo.setLanguages(languages);

        List<SkillEntity> skills = skillMapper.selectList(
                new LambdaQueryWrapper<SkillEntity>().eq(SkillEntity::getUserId, userId)
        );
        if (skills != null) {
            for (SkillEntity sk : skills) {
                sk.setItems(skillMapper.findSkillItemsBySkillId(sk.getId()));
            }
        }
        userInfo.setSkills(skills);

        AboutMeDto dto = new AboutMeDto();
        dto.setName(userInfo.getName());
        dto.setLocation(I18nUtils.getLocalizedText(userInfo.getLocation(), userInfo.getLocationZh(), userInfo.getLocationJa(), locale));
        dto.setAvatarUrl(userInfo.getAvatarUrl());
        dto.setStatus(userInfo.getStatus());
        dto.setJobTitle(I18nUtils.getLocalizedText(userInfo.getJobTitle(), userInfo.getJobTitleZh(), userInfo.getJobTitleJa(), locale));
        dto.setBio(I18nUtils.getLocalizedText(userInfo.getBio(), userInfo.getBioZh(), userInfo.getBioJa(), locale));
        dto.setGraduationYear(userInfo.getGraduationYear());
        dto.setGithub(userInfo.getGithubUrl());
        dto.setMajor(I18nUtils.getLocalizedText(userInfo.getMajor(), userInfo.getMajorZh(), userInfo.getMajorJa(), locale));
        dto.setCertifications(nullToEmpty(userInfo.getCertifications()));
        dto.setStats(toStatDtos(userInfo.getStats()));
        dto.setExperiences(toExperienceDtos(userInfo.getExperiences(), locale));
        dto.setEducation(toEducationDtos(userInfo.getEducation(), locale));
        dto.setLanguages(toLanguageDtos(userInfo.getLanguages(), locale));
        dto.setSkills(toSkillDtos(userInfo.getSkills()));
        return Optional.of(dto);
    }

    private List<String> nullToEmpty(List<String> value) {
        return value == null ? Collections.emptyList() : new java.util.ArrayList<>(value);
    }

    private List<StatDto> toStatDtos(List<StatEntity> stats) {
        if (stats == null) {
            return Collections.emptyList();
        }
        return stats.stream()
                .map(this::toStatDto)
                .collect(Collectors.toList());
    }

    private StatDto toStatDto(StatEntity entity) {
        StatDto dto = new StatDto();
        dto.setId(entity.getId());
        dto.setBusinessId(entity.getBusinessId());
        dto.setYear(entity.getYear());
        dto.setLabel(entity.getLabel());
        dto.setTags(nullToEmpty(entity.getTags()));
        return dto;
    }

    private List<ExperienceDto> toExperienceDtos(List<ExperienceEntity> experiences, Locale locale) {
        if (experiences == null) {
            return Collections.emptyList();
        }
        return experiences.stream()
                .map(entity -> toExperienceDto(entity, locale))
                .collect(Collectors.toList());
    }

    private ExperienceDto toExperienceDto(ExperienceEntity entity, Locale locale) {
        ExperienceDto dto = new ExperienceDto();
        dto.setId(entity.getId());
        dto.setTitle(I18nUtils.getLocalizedText(entity.getTitle(), entity.getTitleZh(), entity.getTitleJa(), locale));
        dto.setCompany(I18nUtils.getLocalizedText(entity.getCompany(), entity.getCompanyZh(), entity.getCompanyJa(), locale));
        dto.setPeriod(entity.getPeriod());
        dto.setDescription(I18nUtils.getLocalizedText(entity.getDescription(), entity.getDescriptionZh(), entity.getDescriptionJa(), locale));
        return dto;
    }

    private List<EducationDto> toEducationDtos(List<EducationEntity> education, Locale locale) {
        if (education == null) {
            return Collections.emptyList();
        }
        return education.stream()
                .map(entity -> toEducationDto(entity, locale))
                .collect(Collectors.toList());
    }

    private EducationDto toEducationDto(EducationEntity entity, Locale locale) {
        EducationDto dto = new EducationDto();
        dto.setId(entity.getId());
        dto.setDegree(I18nUtils.getLocalizedText(entity.getDegree(), entity.getDegreeZh(), entity.getDegreeJa(), locale));
        dto.setSchool(I18nUtils.getLocalizedText(entity.getSchool(), entity.getSchoolZh(), entity.getSchoolJa(), locale));
        dto.setPeriod(entity.getPeriod());
        dto.setDescription(I18nUtils.getLocalizedText(entity.getDescription(), entity.getDescriptionZh(), entity.getDescriptionJa(), locale));
        return dto;
    }

    private List<LanguageDto> toLanguageDtos(List<LanguageEntity> languages, Locale locale) {
        if (languages == null) {
            return Collections.emptyList();
        }
        return languages.stream()
                .map(entity -> toLanguageDto(entity, locale))
                .collect(Collectors.toList());
    }

    private LanguageDto toLanguageDto(LanguageEntity entity, Locale locale) {
        LanguageDto dto = new LanguageDto();
        dto.setId(entity.getId());
        dto.setName(I18nUtils.getLocalizedText(entity.getName(), entity.getNameZh(), entity.getNameJa(), locale));
        dto.setLevel(I18nUtils.getLocalizedText(entity.getLevel(), entity.getLevelZh(), entity.getLevelJa(), locale));
        return dto;
    }

    private List<SkillDto> toSkillDtos(List<SkillEntity> skills) {
        if (skills == null) {
            return Collections.emptyList();
        }
        return skills.stream()
                .map(this::toSkillDto)
                .collect(Collectors.toList());
    }

    private SkillDto toSkillDto(SkillEntity entity) {
        SkillDto dto = new SkillDto();
        dto.setId(entity.getId());
        dto.setCategory(entity.getCategory());
        dto.setItems(nullToEmpty(entity.getItems()));
        return dto;
    }
}
