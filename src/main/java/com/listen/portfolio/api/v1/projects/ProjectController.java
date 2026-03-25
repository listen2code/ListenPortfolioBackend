package com.listen.portfolio.api.v1.projects;

import com.listen.portfolio.model.ApiResponse;
import com.listen.portfolio.model.response.ProjectResponse;
import com.listen.portfolio.service.ProjectService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import utils.Constants;

import java.util.List;

@RestController
@RequestMapping("/v1/projects")
/**
 * Projects API（v1）。
 *
 * 目的：
 * 1) 以“功能模块”组织包结构：api/v1/projects
 * 2) 保持对外接口路径不变，逐步迁移，不影响既存功能
 */
public class ProjectController {
    private static final Logger logger = LoggerFactory.getLogger(ProjectController.class);

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @GetMapping
    public ApiResponse<List<ProjectResponse>> getProjects() {
        logger.info("获取项目列表");
        List<ProjectResponse> projects = projectService.getProjects();
        if (projects.isEmpty()) {
            return ApiResponse.error(Constants.DEFAULT_SERVER_ERROR, "No projects found");
        }
        return ApiResponse.success(projects);
    }
}

