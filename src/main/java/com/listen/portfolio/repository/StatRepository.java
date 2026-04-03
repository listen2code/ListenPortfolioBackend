package com.listen.portfolio.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.listen.portfolio.entity.StatEntity;

public interface StatRepository extends JpaRepository<StatEntity, Long> {
}
