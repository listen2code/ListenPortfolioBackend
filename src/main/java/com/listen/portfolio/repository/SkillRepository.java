package com.listen.portfolio.repository;

import com.listen.portfolio.infrastructure.persistence.entity.SkillEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SkillRepository extends JpaRepository<SkillEntity, Long> {
}
