package com.listen.portfolio.repository;

import com.listen.portfolio.model.response.Education;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EducationRepository extends JpaRepository<Education, Long> {
}
