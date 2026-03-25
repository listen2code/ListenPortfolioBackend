package com.listen.portfolio.api.v1.about;

import com.listen.portfolio.model.ApiResponse;
import com.listen.portfolio.model.response.AboutMeResponse;
import com.listen.portfolio.service.AboutMeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import utils.Constants;

@RestController
@RequestMapping("/v1/aboutMe")
/**
 * AboutMe API（v1）。
 *
 * 目的：
 * 1) 以“功能模块”组织包结构：api/v1/about
 * 2) 保持对外接口路径不变，逐步迁移，不影响既存功能
 */
public class AboutMeController {
    private static final Logger logger = LoggerFactory.getLogger(AboutMeController.class);

    private final AboutMeService aboutMeService;

    public AboutMeController(AboutMeService aboutMeService) {
        this.aboutMeService = aboutMeService;
    }

    @GetMapping
    public ApiResponse<AboutMeResponse> getAboutMe() {
        logger.info("Get about-me information");
        return aboutMeService.getAboutMe()
                .map(ApiResponse::success)
                .orElse(ApiResponse.error(Constants.DEFAULT_SERVER_ERROR, "About me not found"));
    }
}
