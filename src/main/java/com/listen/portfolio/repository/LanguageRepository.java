package com.listen.portfolio.repository;

import com.listen.portfolio.model.response.LanguageResponse;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LanguageRepository extends JpaRepository<LanguageResponse, Long> {
}
