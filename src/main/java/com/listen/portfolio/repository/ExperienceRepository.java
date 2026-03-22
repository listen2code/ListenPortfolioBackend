package com.listen.portfolio.repository;

import com.listen.portfolio.model.response.ExperienceResponse;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExperienceRepository extends JpaRepository<ExperienceResponse, Long> {
}
