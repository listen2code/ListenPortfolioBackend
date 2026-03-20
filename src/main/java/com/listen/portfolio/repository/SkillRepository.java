package com.listen.portfolio.repository;

import com.listen.portfolio.model.response.Skill;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SkillRepository extends JpaRepository<Skill, Long> {
}
