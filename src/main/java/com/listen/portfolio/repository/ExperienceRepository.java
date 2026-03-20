package com.listen.portfolio.repository;

import com.listen.portfolio.model.response.Experience;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExperienceRepository extends JpaRepository<Experience, Long> {
}
