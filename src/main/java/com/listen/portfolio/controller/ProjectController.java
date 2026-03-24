package com.listen.portfolio.controller;

import com.listen.portfolio.model.ApiResponse;
import com.listen.portfolio.model.response.ProjectResponse;
import com.listen.portfolio.service.ProjectService;

import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import org.slf4j.Logger;
import utils.Constants;

@RestController
@RequestMapping("/v1/projects")
public class ProjectController {
    private static final Logger logger = LoggerFactory.getLogger(ProjectController.class);

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @GetMapping()
    public ApiResponse<List<ProjectResponse>> getProjects() {
        logger.info("获取项目列表");
        List<ProjectResponse> projects = projectService.getProjects();
        if (projects.isEmpty()) {
            return ApiResponse.error(Constants.DEFAULT_SERVER_ERROR, "No projects found");
        }
        return ApiResponse.success(projects);
    }
}
