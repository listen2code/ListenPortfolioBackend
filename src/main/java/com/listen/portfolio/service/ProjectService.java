package com.listen.portfolio.service;

import com.listen.portfolio.api.v1.projects.dto.ProjectDto;
import com.listen.portfolio.common.util.I18nUtils;
import com.listen.portfolio.entity.ProjectEntity;
import com.listen.portfolio.mapper.ProjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * 项目业务服务类（MyBatis-Plus 版本）
 */
@Service
public class ProjectService {

    private static final Logger logger = LoggerFactory.getLogger(ProjectService.class);
    private final ProjectMapper projectMapper;

    public ProjectService(ProjectMapper projectMapper) {
        this.projectMapper = projectMapper;
    }

    /**
     * 事务与国际化说明：
     * - 使用 @Transactional(readOnly = true) 开启只读事务
     * - 根据 LocaleContextHolder 获取当前客户端 Accept-Language 对应的 Locale
     * - 映射 title, subtitle, desc 的多语言版本与 techStack 技术栈列表
     */
    @Transactional(readOnly = true)
    public List<ProjectDto> getProjects() {
        logger.info("Fetching all projects from the database with i18n support.");
        Locale locale = LocaleContextHolder.getLocale();
        List<ProjectEntity> list = projectMapper.selectList(null);
        return list.stream()
                .map(entity -> {
                    entity.setTechStack(projectMapper.findTechStackByProjectId(entity.getId()));
                    return toDto(entity, locale);
                })
                .collect(Collectors.toList());
    }

    private ProjectDto toDto(ProjectEntity entity, Locale locale) {
        ProjectDto dto = new ProjectDto();
        dto.setId(entity.getId());
        dto.setBusinessId(entity.getBusinessId());
        dto.setTitle(I18nUtils.getLocalizedText(entity.getTitle(), entity.getTitleZh(), entity.getTitleJa(), locale));
        dto.setSubtitle(I18nUtils.getLocalizedText(entity.getSubtitle(), entity.getSubtitleZh(), entity.getSubtitleJa(), locale));
        dto.setDesc(I18nUtils.getLocalizedText(entity.getDesc(), entity.getDescZh(), entity.getDescJa(), locale));
        dto.setImageUrl(entity.getImageUrl());
        dto.setGithubUrl(entity.getGithubUrl());
        dto.setTechStack(entity.getTechStack() != null ? new java.util.ArrayList<>(entity.getTechStack()) : null);
        return dto;
    }
}
