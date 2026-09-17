package com.listen.portfolio.common.config;

import com.listen.portfolio.common.jwt.JwtRequestFilter;
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

/**
 * Spring Security 核心安全配置类 (Security Configuration)。
 *
 * <p><b>一、核心安全架构与设计哲学：</b>
 * <ul>
 *   <li><b>无状态认证基石 (Stateless Session Policy)：</b>
 *       配置 {@link SessionCreationPolicy#STATELESS}，服务端彻底禁用标准 Servlet {@code HttpSession}，
 *       不在内存中维系任何基于 Session ID / Cookie 的有状态上下文。
 *       每个请求的身份认证完全依托客户端在 {@code Authorization} 标头中携带的 JWT 访问令牌，
 *       天然消除 Session 固定攻击与跨站请求伪造（CSRF），并无缝支持容器集群在负载均衡器下的弹性横向扩展。</li>
 *   <li><b>禁用 CSRF 防护的安全性论证 (CSRF Exemption Rationale)：</b>
 *       传统 CSRF 攻击的核心机制在于利用浏览器针对 Cookie 的“隐式自动携带”机制（Ambient Credentials）。
 *       本项目采用无状态 JWT 方案，凭据必须由客户端显式置于 HTTP 请求头 {@code Authorization: Bearer <token>} 中，
 *       外部第三方恶意网页无法伪造此标头，因此完全免疫传统 CSRF 攻击，禁用 CSRF 可消除不必要的 Token 同步与性能损耗。</li>
 *   <li><b>基于过滤器链的纵深防御 (Defense-in-Depth Filter Chain)：</b>
 *       在标准 {@link UsernamePasswordAuthenticationFilter} 前插入自定义 {@link JwtRequestFilter}。
 *       所有进入应用层的 HTTP 请求必须优先经过 JWT 解析、签名核验、有效期校验与 Redis 黑名单探测，
 *       认证成功后将授权上下文载入线程绑定的 {@code SecurityContextHolder}。</li>
 *   <li><b>最小权限与公开白名单治理 (Least Privilege & Explicit Whitelisting)：</b>
 *       默认对所有未声明的端点执行 {@code .anyRequest().authenticated()} 严格拦截（Fail-Close 闭网原则）；
 *       仅对身份认证入口（{@code /v1/auth/**}）、作品公开展示（{@code /v1/projects/**}）、静态资产（{@code /images/**}）、
 *       健康探针（{@code /actuator/health}）与 API 文档执行显式放行。</li>
 * </ul>
 *
 * @author Development Team
 * @since 1.0.0
 * @see JwtRequestFilter
 * @see com.listen.portfolio.service.AuthService
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * 用户详情服务接口组件，由 {@link com.listen.portfolio.service.AuthService} 实现，
     * 用于根据用户名加载数据库中的用户安全凭据与授权信息
     */
    @Autowired
    private UserDetailsService userDetailsService;

    /**
     * 自定义 JWT 请求过滤器，拦截每个 HTTP 请求并执行令牌提取与合法性核验
     */
    @Autowired
    private JwtRequestFilter jwtRequestFilter;

    /**
     * 配置基于数据库 DAO 的认证提供器 (DaoAuthenticationProvider)。
     *
     * <p>将 {@link UserDetailsService} 与 {@link PasswordEncoder} 绑定，
     * 在用户执行登录认证时自动完成从数据库拉取哈希密码并使用 BCrypt 算法比对明文密码。
     *
     * @return 配置好的 DaoAuthenticationProvider 实例
     */
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    /**
     * 暴露 Spring Security 全局核心认证管理器 (AuthenticationManager) Bean。
     *
     * <p>供控制器层（如 {@link com.listen.portfolio.api.v1.auth.AuthController#login}）
     * 显式调用 {@code authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(user, pwd))}
     * 触发标准身份凭据核验流程。
     *
     * @param authConfig Spring Security 自动装配认证配置器
     * @return 全局 AuthenticationManager 实例
     * @throws Exception 若获取认证管理器失败抛出异常
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    /**
     * 配置密码编码器为工业级标准的 {@link BCryptPasswordEncoder}。
     *
     * <p><b>BCrypt 安全优势：</b>
     * <ul>
     *   <li><b>内置自动随机加盐 (Salt Generation)：</b>
     *       每次调用 {@code encode()} 时内部生成唯一的 128 位随机 Salt 并编码至 60 字符哈希结果中，
     *       彻底免疫基于预计算彩虹表（Rainbow Table）与哈希字典的碰撞攻击。</li>
     *   <li><b>可调慢速哈希与自适应计算成本 (Work Factor / Cost)：</b>
     *       默认采用 Cost 10（进行 $2^{10} = 1024$ 次密钥扩展迭代），有效防御攻击者利用 GPU / ASIC 实施离线暴力破解。</li>
     * </ul>
     *
     * @return BCryptPasswordEncoder 密码编码器单例
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 配置主安全过滤链 (SecurityFilterChain)，确立全站核心访问控制规则。
     *
     * @param http HttpSecurity 构建器
     * @return 构建就绪的 SecurityFilterChain 实例
     * @throws Exception 配置异常
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 1. 禁用 CSRF：在无状态 JWT 架构中，无需生成或验证 CSRF Token
                .csrf(csrf -> csrf.disable())

                // 2. 会话管理：声明无状态策略，严禁服务器创建 HttpSession
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 3. 声明端点路由授权策略
                .authorizeHttpRequests(auth -> auth
                        // (1) 公开认证入口与作品展示端点放行
                        .requestMatchers("/v1/auth/**", "/v1/projects/**").permitAll()

                        // (2) 静态图片与公共前端资源放行
                        .requestMatchers("/images/**", "/static/**").permitAll()

                        // (3) 邮件重置密码静态落地页放行（便于未登录用户从邮件链接进入操作界面）
                        .requestMatchers("/password-reset-out-email.html").permitAll()

                        // (4) Spring Boot Actuator 监控与健康探针放行策略
                        // 业务考量：
                        //   - /actuator/health 与 /actuator/health/**: 供 Docker 与 K8s 容器探针探测存活与就绪状态；
                        //   - /actuator/prometheus: 供 Prometheus 监控集群以固定周期 Pull 拉取运行时指标。
                        // 纵深防御建议：
                        //   - 在 application.properties 中最小化暴露 include=health,info,prometheus，严禁暴露 env, heapdump 等高危端点；
                        //   - 生产环境建议在反向代理（Nginx）层配置 IP 白名单，仅允许内网或监控探针访问 /actuator/**。
                        .requestMatchers("/actuator/health", "/actuator/health/**", "/actuator/prometheus").permitAll()

                        // (5) OpenAPI 规范元数据与 Swagger UI 交互式调试控制台放行
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()

                        // (6) 默认安全闭环：其他所有 API 端点（如 /v1/user/**）必须携带合法 JWT 认证凭据
                        .anyRequest().authenticated()
                );

        // 4. 装载基于 DAO 的用户密码认证提供器
        http.authenticationProvider(authenticationProvider());

        // 5. 在标准用户名密码认证过滤器之前插入 JWT 过滤器，确保先一步拦截并解析 Header 中的 Bearer Token
        http.addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
