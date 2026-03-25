package com.listen.portfolio.repository;

import com.listen.portfolio.infrastructure.persistence.entity.EducationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EducationRepository extends JpaRepository<EducationEntity, Long> {
}
