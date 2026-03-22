package com.listen.portfolio.repository;

import com.listen.portfolio.model.response.EducationResponse;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EducationRepository extends JpaRepository<EducationResponse, Long> {
}
