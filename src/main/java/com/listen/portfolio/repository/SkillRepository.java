package com.listen.portfolio.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.listen.portfolio.entity.SkillEntity;

public interface SkillRepository extends JpaRepository<SkillEntity, Long> {
}
