package com.listen.portfolio.service;

import com.listen.portfolio.model.response.AboutMeResponse;
import com.listen.portfolio.model.response.UserResponse;
import com.listen.portfolio.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class AboutMeService {

    private static final Logger logger = LoggerFactory.getLogger(AboutMeService.class);
    private final UserRepository userInfoRepository;

    public AboutMeService(UserRepository userInfoRepository) {
        this.userInfoRepository = userInfoRepository;
    }

    /**
     * 事务说明（中文）：
     * - 使用 @Transactional(readOnly = true) 开启只读事务
     * - 目的：控制查询在同一持久化上下文中完成，降低事务开销，避免无意义的 flush/脏检查
     * - 注意：仅进行读操作；从实体装配到响应对象在事务内完成，避免后续序列化阶段二次查询
     */
    @Transactional(readOnly = true)
    public Optional<AboutMeResponse> getAboutMe() {
        // 说明（中文）：关于我页面查询属于只读操作，标记为只读事务以减少持久化上下文开销
        // Assuming we are fetching a specific user's "About Me" page, e.g., user with ID 1.
        long userId = 1L;
        Optional<UserResponse> userInfoOptional = userInfoRepository.findById(userId);

        if (userInfoOptional.isEmpty()) {
            logger.warn("User with id {} not found for AboutMe page.", userId);
            return Optional.empty();
        }

        UserResponse userInfo = userInfoOptional.get();
        AboutMeResponse aboutMe = new AboutMeResponse();

        aboutMe.setStatus(userInfo.getStatus());
        aboutMe.setJobTitle(userInfo.getJobTitle());
        aboutMe.setBio(userInfo.getBio());
        aboutMe.setGraduationYear(userInfo.getGraduationYear());
        aboutMe.setGithub(userInfo.getGithubUrl());
        aboutMe.setMajor(userInfo.getMajor());
        aboutMe.setCertifications(userInfo.getCertifications());
        aboutMe.setStats(userInfo.getStats());
        aboutMe.setExperiences(userInfo.getExperiences());
        aboutMe.setEducation(userInfo.getEducation());
        aboutMe.setLanguages(userInfo.getLanguages());
        aboutMe.setSkills(userInfo.getSkills());

        return Optional.of(aboutMe);
    }
}
