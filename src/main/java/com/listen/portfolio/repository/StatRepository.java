package com.listen.portfolio.repository;

import com.listen.portfolio.model.response.StatResponse;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StatRepository extends JpaRepository<StatResponse, Long> {
}
