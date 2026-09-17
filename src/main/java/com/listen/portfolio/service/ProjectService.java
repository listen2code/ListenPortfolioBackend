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
 * Project Business Service (MyBatis-Plus Implementation).
 *
 * <p>Architectural Responsibilities:
 * <ul>
 *   <li><b>Read-Only Transactions</b>: Annotates query methods with {@code @Transactional(readOnly = true)}
 *       to optimize database connection lifecycle and prevent accidental DML mutations.</li>
 *   <li><b>Dynamic Localization</b>: Inspects the thread-bound {@link Locale} via {@link LocaleContextHolder}
 *       to map entity titles, subtitles, and descriptions to the client's language preference.</li>
 *   <li><b>DTO Decoupling</b>: Transforms persistent {@link ProjectEntity} models into client-facing
 *       {@link ProjectDto} objects, strictly separating database schemas from API contracts.</li>
 * </ul>
 */
@Service
public class ProjectService {

    private static final Logger logger = LoggerFactory.getLogger(ProjectService.class);
    private final ProjectMapper projectMapper;

    public ProjectService(ProjectMapper projectMapper) {
        this.projectMapper = projectMapper;
    }

    /**
     * Retrieves all projects from the database, populated with their associated tech stacks
     * and dynamic localization applied.
     *
     * @return List of fully populated {@link ProjectDto} instances.
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
