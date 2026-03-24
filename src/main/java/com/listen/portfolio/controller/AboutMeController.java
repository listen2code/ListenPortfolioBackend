package com.listen.portfolio.controller;

import com.listen.portfolio.model.ApiResponse;
import com.listen.portfolio.model.response.AboutMeResponse;
import com.listen.portfolio.service.AboutMeService;

import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.slf4j.Logger;
import utils.Constants;

@RestController
@RequestMapping("/v1/aboutMe")
public class AboutMeController {
    private static final Logger logger = LoggerFactory.getLogger(AboutMeController.class);

    private final AboutMeService aboutMeService;

    public AboutMeController(AboutMeService aboutMeService) {
        this.aboutMeService = aboutMeService;
    }
    
    @GetMapping()
    public ApiResponse<AboutMeResponse> getAboutMe() {
        logger.info("获取关于我的信息");
        return aboutMeService.getAboutMe()
                .map(ApiResponse::success)
                .orElse(ApiResponse.error(Constants.DEFAULT_SERVER_ERROR, "About me not found"));
    }
}
