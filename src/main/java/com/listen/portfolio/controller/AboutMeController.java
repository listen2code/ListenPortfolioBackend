package com.listen.portfolio.controller;

import com.listen.portfolio.model.ApiResponse;
import com.listen.portfolio.model.response.AboutMeResponse;
import com.listen.portfolio.service.AboutMeService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/aboutMe")
public class AboutMeController {

    private final AboutMeService aboutMeService;

    public AboutMeController(AboutMeService aboutMeService) {
        this.aboutMeService = aboutMeService;
    }

    @GetMapping()
    public ApiResponse<AboutMeResponse> getAboutMe() {
        return aboutMeService.getAboutMe()
                .map(ApiResponse::success)
                .orElse(ApiResponse.error("103", "About me not found"));
    }
}
