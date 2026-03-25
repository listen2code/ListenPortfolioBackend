package com.listen.portfolio.api.v1.projects.dto;

import lombok.Data;

import java.util.List;

@Data
public class ProjectDto {
    private Long id;
    private String businessId;
    private String title;
    private String subtitle;
    private String desc;
    private String imageUrl;
    private String githubUrl;
    private List<String> techStack;
}

