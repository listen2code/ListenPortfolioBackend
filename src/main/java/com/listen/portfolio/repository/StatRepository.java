package com.listen.portfolio.repository;

import com.listen.portfolio.infrastructure.persistence.entity.StatEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StatRepository extends JpaRepository<StatEntity, Long> {
}
