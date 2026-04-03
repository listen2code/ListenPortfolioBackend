package com.listen.portfolio.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.listen.portfolio.entity.LanguageEntity;

public interface LanguageRepository extends JpaRepository<LanguageEntity, Long> {
}
