package com.listen.portfolio.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.listen.portfolio.entity.EducationEntity;

public interface EducationRepository extends JpaRepository<EducationEntity, Long> {
}
