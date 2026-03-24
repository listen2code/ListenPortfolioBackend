package com.listen.portfolio.repository;



import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.listen.portfolio.model.response.UserResponse;

import java.util.Optional;



public interface UserRepository extends JpaRepository<UserResponse, Long> {

    Optional<UserResponse> findByName(String userName);

    @Query("SELECT u FROM UserResponse u WHERE BINARY(u.name) = BINARY(:name)")
    Optional<UserResponse> findByNameCaseSensitive(@Param("name") String name);

    Optional<UserResponse> findByEmail(String email);
}