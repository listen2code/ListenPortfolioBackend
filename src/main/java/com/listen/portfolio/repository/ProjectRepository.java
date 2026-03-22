package com.listen.portfolio.repository;

import com.listen.portfolio.model.response.ProjectResponse;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectRepository extends JpaRepository<ProjectResponse, Long> {
}
