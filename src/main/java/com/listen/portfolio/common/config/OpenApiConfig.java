package com.listen.portfolio.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
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
                        .description("REST API documentation for Portfolio backend\n\n" +
                                "## 功能特性\n" +
                                "- 🔐 JWT 认证支持\n" +
                                "- 📊 完整的 API 文档\n" +
                                "- 🧪 在线测试功能\n" +
                                "- 📝 详细的请求/响应示例\n" +
                                "- 🌐 多环境支持")
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("Portfolio Team")
                                .email("support@portfolio.com")
                                .url("https://github.com/listen2code/ListenPortfolioBackend"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .externalDocs(new ExternalDocumentation()
                        .description("查看项目源码")
                        .url("https://github.com/listen2code/ListenPortfolioBackend"))
                // 添加 JWT 认证配置
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("请输入登录后返回的 JWT token，系统会自动添加 Bearer 前缀")));
    }
}

