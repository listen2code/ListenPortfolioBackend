package com.listen.portfolio.config;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
/**
 * OpenAPI 配置（Swagger UI）。
 * 说明：
 * - 自动生成 /v3/api-docs JSON 与 /swagger-ui 可视化文档
 * - 便于前后端联调与回归验证
 * 原理：
 * - 依赖 springdoc-openapi 对 Spring Web 注解进行扫描与建模
 */
public class OpenApiConfig {

    @Bean
    public OpenAPI portfolioOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Listen Portfolio Backend API")
                        .description("REST API documentation for Portfolio backend")
                        .version("v1"))
                .externalDocs(new ExternalDocumentation()
                        .description("Springdoc OpenAPI")
                        .url("https://springdoc.org/"));
    }
}

