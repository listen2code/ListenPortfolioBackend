package com.listen.portfolio.service;

import com.listen.portfolio.api.v1.projects.dto.ProjectDto;
import com.listen.portfolio.infrastructure.persistence.entity.ProjectEntity;
import com.listen.portfolio.repository.ProjectRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProjectService {

    private static final Logger logger = LoggerFactory.getLogger(ProjectService.class);
    private final ProjectRepository projectRepository;

    public ProjectService(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    /**
     * 事务说明：
     * - 使用 @Transactional(readOnly = true) 开启只读事务
     * - 目的：降低事务开销、避免不必要的脏检查；在同一持久化上下文中完成查询与 DTO 装配
     * - 注意：只读事务中不执行写操作；DTO 转换在事务内完成，避免序列化阶段触发懒加载
     */
    @Transactional(readOnly = true)
    public List<ProjectDto> getProjects() {
        // 只读查询使用 readOnly=true，避免无意义的脏检查，提高性能并减少锁竞争
        logger.info("Fetching all projects from the database.");
        // 不要直接把 JPA Entity 透传给 Controller/序列化层，避免 Lazy 字段在事务外触发导致报错
        // 原理：在事务内完成实体到 DTO 的转换，序列化只依赖 DTO 的普通字段，不依赖 Hibernate Session
        return projectRepository.findAll()
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    private ProjectDto toDto(ProjectEntity entity) {
        ProjectDto dto = new ProjectDto();
        dto.setId(entity.getId());
        dto.setBusinessId(entity.getBusinessId());
        dto.setTitle(entity.getTitle());
        dto.setSubtitle(entity.getSubtitle());
        dto.setDesc(entity.getDesc());
        dto.setImageUrl(entity.getImageUrl());
        dto.setGithubUrl(entity.getGithubUrl());
        dto.setTechStack(entity.getTechStack());
        return dto;
    }
}

