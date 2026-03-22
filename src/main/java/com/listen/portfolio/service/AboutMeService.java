package com.listen.portfolio.service;

import com.listen.portfolio.model.response.AboutMeResponse;
import com.listen.portfolio.model.response.UserResponse;
import com.listen.portfolio.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AboutMeService {

    private static final Logger logger = LoggerFactory.getLogger(AboutMeService.class);
    private final UserRepository userInfoRepository;

    public AboutMeService(UserRepository userInfoRepository) {
        this.userInfoRepository = userInfoRepository;
    }

    public Optional<AboutMeResponse> getAboutMe() {
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

