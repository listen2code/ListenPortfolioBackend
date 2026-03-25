package com.listen.portfolio.repository;

import com.listen.portfolio.infrastructure.persistence.entity.ExperienceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExperienceRepository extends JpaRepository<ExperienceEntity, Long> {
}
