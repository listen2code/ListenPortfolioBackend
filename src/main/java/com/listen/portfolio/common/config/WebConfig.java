package com.listen.portfolio.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

import java.util.Locale;

/**
 * Web MVC 核心配置类 (Web Configuration)
 *
 * <h3>架构设计与核心职责：</h3>
 * <ul>
 *   <li><b>多语言国际化解析 (i18n Locale Resolution)：</b>
 *       配置基于 HTTP 请求头 {@code Accept-Language} 的 {@link AcceptHeaderLocaleResolver}，
 *       自动提取前端传递的用户语言偏好（如 zh-CN、en-US、ja-JP），并作为全局上下文注入业务层与响应封装。</li>
 *   <li><b>后端静态图片资产映射 (Static Asset Mapping)：</b>
 *       配置 {@link ResourceHandlerRegistry}，将业务图片请求路径 {@code /images/**}
 *       映射到底层类路径静态资源目录（{@code classpath:/static/images/} 与 {@code classpath:/public/images/}）。</li>
 *   <li><b>与 Nginx 边缘网关的协同反向代理机制：</b>
 *       在生产拓扑中，前端或移动端统一请求 {@code /api/images/{filename}}。
 *       Nginx 通过配置 {@code location ^~ /api/ { proxy_pass http://127.0.0.1:8080/; }} 自动剥离
 *       {@code /api} 路由前缀，精准转发为 {@code /images/{filename}} 打入当前静态资源处理器，
 *       实现统一入口、动静分离与同源免跨域调用。</li>
 * </ul>
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * 注册国际化区域解析器 Bean。
     *
     * <p>采用基于 HTTP 请求头 {@code Accept-Language} 的标准解析器，
     * 当客户端未显式传递该标头或传递的语系不支持时，默认回退至英语 ({@link Locale#ENGLISH})。
     *
     * @return 配置好默认语言环境的 LocaleResolver 实例
     */
    @Bean
    public LocaleResolver localeResolver() {
        AcceptHeaderLocaleResolver resolver = new AcceptHeaderLocaleResolver();
        resolver.setDefaultLocale(Locale.ENGLISH);
        return resolver;
    }

    /**
     * 配置静态资源映射规则与查找路径。
     *
     * <h3>设计细节：</h3>
     * <ul>
     *   <li><b>URL 路由规则：</b>匹配所有以 {@code /images/} 开头的资源请求（如 {@code /images/avatar.png}）；</li>
     *   <li><b>双重查找源：</b>优先检索 {@code classpath:/static/images/}，未命中时回退检索 {@code classpath:/public/images/}；</li>
     *   <li><b>安全权限配合：</b>在 {@link SecurityConfig} 中已配置 {@code .requestMatchers("/images/**").permitAll()}，
     *       允许未登录游客或外链无凭据加载公共图片素材。</li>
     * </ul>
     *
     * @param registry 资源处理器注册中心
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/images/**")
                .addResourceLocations("classpath:/static/images/", "classpath:/public/images/");
    }
}