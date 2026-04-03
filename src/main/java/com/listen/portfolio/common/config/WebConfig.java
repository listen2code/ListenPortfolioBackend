package com.listen.portfolio.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web配置类，用于配置静态资源映射
 * 使用Spring Boot标准的静态资源路径配置
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * 配置静态资源处理器
     * 添加自定义路径映射，同时保留Spring Boot的默认静态资源处理
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 保持默认的静态资源处理（/static/, /public/, /resources/）
        // 同时添加自定义映射
        registry.addResourceHandler("/images/**")
                .addResourceLocations("classpath:/static/images/", "classpath:/public/images/");
    }
}