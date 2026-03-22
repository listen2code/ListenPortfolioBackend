package com.listen.portfolio.repository;



import org.springframework.data.jpa.repository.JpaRepository;

import com.listen.portfolio.model.response.UserResponse;

import java.util.Optional;



public interface UserRepository extends JpaRepository<UserResponse, Long> {

    Optional<UserResponse> findByName(String userName);

}
