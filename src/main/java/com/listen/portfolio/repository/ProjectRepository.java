package com.listen.portfolio.repository;

import com.listen.portfolio.model.response.Project;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectRepository extends JpaRepository<Project, Long> {
}
