package com.listen.portfolio.config;

import com.listen.portfolio.jwt.JwtRequestFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
/**
 * Spring Security 配置类
 * 
 * 配置 JWT 认证、密码编码器、认证管理器等安全相关组件。
 * 禁用 CSRF，设置无状态会话，使用 JWT 过滤器。
 */
public class SecurityConfig {

    /** 用户详情服务，用于加载用户信息 */
    @Autowired
    private UserDetailsService userDetailsService;

    /** JWT 请求过滤器，用于验证 JWT 令牌 */
    @Autowired
    private JwtRequestFilter jwtRequestFilter;

    /**
     * 配置 DaoAuthenticationProvider
     * 使用 UserDetailsService 和 BCrypt 密码编码器
     */
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    /**
     * 获取 AuthenticationManager Bean
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    /**
     * 配置密码编码器，使用 BCrypt
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 配置安全过滤链
     * 禁用 CSRF，设置无状态会话，允许 /v1/auth/** 和静态资源路径无认证，其他路径需要认证。
     * 添加 JWT 请求过滤器。
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // 说明：公开认证入口、项目列表与静态资源，便于前端与未登录访问
                        .requestMatchers("/v1/auth/**", "/v1/projects/**", "/images/**", "/static/**").permitAll()
                        // 说明：对健康检查与 Prometheus 指标端点放行，便于探活与监控系统抓取
                        // 原理：这些端点由 Spring Boot Actuator 提供，本身不包含业务敏感数据
                        .requestMatchers("/actuator/health", "/actuator/health/**", "/actuator/prometheus").permitAll()
                        .anyRequest().authenticated()
                );

        http.authenticationProvider(authenticationProvider());
        http.addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
