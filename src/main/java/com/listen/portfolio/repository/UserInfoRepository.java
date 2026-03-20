package com.listen.portfolio.repository;



import org.springframework.data.jpa.repository.JpaRepository;

import com.listen.portfolio.model.response.UserInfoResponse;

import java.util.Optional;



public interface UserInfoRepository extends JpaRepository<UserInfoResponse, Long> {

    Optional<UserInfoResponse> findByName(String userName);

}
