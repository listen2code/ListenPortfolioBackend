package com.listen.portfolio.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.listen.portfolio.entity.ExperienceEntity;

public interface ExperienceRepository extends JpaRepository<ExperienceEntity, Long> {
}
