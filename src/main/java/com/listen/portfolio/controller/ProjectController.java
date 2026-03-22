package com.listen.portfolio.controller;

import com.listen.portfolio.model.ApiResponse;
import com.listen.portfolio.model.response.Project;
import com.listen.portfolio.service.ProjectService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/v1/projects")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @GetMapping()
    public ApiResponse<List<Project>> getProjects() {
        List<Project> projects = projectService.getProjects();
        if (projects.isEmpty()) {
            return ApiResponse.error("102", "No projects found");
        }
        return ApiResponse.success(projects);
    }
}
