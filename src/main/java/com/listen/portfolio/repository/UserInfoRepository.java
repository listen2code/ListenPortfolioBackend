package com.listen.portfolio.repository;



import com.listen.portfolio.model.UserInfo;

import org.springframework.data.jpa.repository.JpaRepository;



import java.util.Optional;



public interface UserInfoRepository extends JpaRepository<UserInfo, Long> {

    Optional<UserInfo> findByName(String userName);

}
