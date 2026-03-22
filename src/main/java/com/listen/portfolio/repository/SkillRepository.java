package com.listen.portfolio.repository;

import com.listen.portfolio.model.response.SkillResponse;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SkillRepository extends JpaRepository<SkillResponse, Long> {
}
