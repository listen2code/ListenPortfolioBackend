package com.listen.portfolio.controller;

import com.listen.portfolio.model.ApiResponse;
import com.listen.portfolio.model.response.AboutMeResponse;
import com.listen.portfolio.model.response.Project;
import com.listen.portfolio.service.AboutMeService;
import com.listen.portfolio.service.ProjectService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/v1")
public class PortfolioController {

    private final ProjectService projectService;
    private final AboutMeService aboutMeService;

    public PortfolioController(ProjectService projectService, AboutMeService aboutMeService) {
        this.projectService = projectService;
        this.aboutMeService = aboutMeService;
    }

    @GetMapping("/projects")
    public ApiResponse<List<Project>> getProjects() {
        List<Project> projects = projectService.getProjects();
        if (projects.isEmpty()) {
            return ApiResponse.error("102", "No projects found");
        }
        return ApiResponse.success(projects);
    }

    @GetMapping("/aboutme")
    public ApiResponse<AboutMeResponse> getAboutMe() {
        return aboutMeService.getAboutMe()
                .map(ApiResponse::success)
                .orElse(ApiResponse.error("103", "About me not found"));
    }
}
