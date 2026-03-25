package com.listen.portfolio.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.listen.portfolio.infrastructure.persistence.entity.UserEntity;

import java.util.Optional;

public interface UserRepository extends JpaRepository<UserEntity, Long> {

    Optional<UserEntity> findByName(String userName);

    @Query("SELECT u FROM UserEntity u WHERE BINARY(u.name) = BINARY(:name)")
    Optional<UserEntity> findByNameCaseSensitive(@Param("name") String name);

    Optional<UserEntity> findByEmail(String email);
}
