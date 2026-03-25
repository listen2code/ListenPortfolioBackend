package com.listen.portfolio.repository;

import com.listen.portfolio.infrastructure.persistence.entity.LanguageEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LanguageRepository extends JpaRepository<LanguageEntity, Long> {
}
