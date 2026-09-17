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

/**
 * OpenAPI 3.0 与 Swagger UI 规范化文档装配配置类 (OpenAPI Configuration)
 *
 * <h3>架构设计与核心职责：</h3>
 * <ul>
 *   <li><b>自动化元数据契约生成：</b>
 *       基于 {@code springdoc-openapi-starter-webmvc-ui} 扫描 Controller 与 DTO 注解，
 *       动态生成符合 OpenAPI 3.0 规范的元数据描述文档（{@code /v3/api-docs}）。</li>
 *   <li><b>可视化交互式调试控制台 (Swagger UI)：</b>
 *       自动提供基于 Web 的前端交互界面（{@code /swagger-ui/index.html} 或 {@code /swagger-ui.html}），
 *       支持免 Postman 直接在浏览器在线测试 API 入参、出参及响应状态码。</li>
 *   <li><b>Bearer JWT 全局安全认证方案声明：</b>
 *       在 OpenAPI 组件中显式声明 {@code bearerAuth} 安全方案（{@link SecurityScheme.Type#HTTP} + {@code scheme="bearer"}），
 *       在 Swagger UI 顶部激活全局 "Authorize" 按钮，输入登录后获取的 JWT 即可自动在所有受保护接口追加
 *       {@code Authorization: Bearer <token>} 请求标头。</li>
 *   <li><b>Spring Security 协同策略：</b>
 *       在 {@link SecurityConfig} 中显式声明对 {@code /v3/api-docs/**} 与 {@code /swagger-ui/**} 放行，
 *       确保联调与自测无需预先认证。</li>
 * </ul>
 */
@Configuration
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

