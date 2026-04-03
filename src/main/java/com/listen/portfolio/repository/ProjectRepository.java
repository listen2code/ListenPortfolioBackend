package com.listen.portfolio.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.listen.portfolio.entity.ProjectEntity;

public interface ProjectRepository extends JpaRepository<ProjectEntity, Long> {
}
