package com.listen.portfolio;

import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

/**
 * 传统外部 Servlet 容器部署初始化器 (WAR Deployment Servlet Initializer)。
 *
 * <p><b>一、核心架构背景与设计哲学：</b>
 * <ul>
 *   <li><b>双模部署支持架构 (Dual-Mode Execution Architecture)：</b>
 *       本项目不仅支持以独立可执行 JAR（微服务/容器化模式通过内置嵌入式 Tomcat 运行，调用 {@link PortfolioApplication#main}），
 *       同时支持打包为标准 WAR 文件（{@code portfolio-0.0.1-SNAPSHOT.war}）部署至企业传统的独立 Servlet 容器中
 *       （如 Apache Tomcat 10.1+、Eclipse Jetty 12+、JBoss/WildFly 等）。</li>
 *   <li><b>Servlet 3.0+ / 6.0 规范启动 SPI 机制 (Container Bootstrapping Mechanism)：</b>
 *       根据 Jakarta EE / Servlet 规范，当 WAR 包部署至外部独立应用服务器时，容器启动阶段不会调用类的 {@code main()} 方法。
 *       外部 Servlet 容器会通过 Java SPI 机制扫描类路径下的 {@code META-INF/services/jakarta.servlet.ServletContainerInitializer}，
 *       触发 Spring 提供的 {@code SpringServletContainerInitializer}。该组件检索到实现了
 *       {@link org.springframework.web.WebApplicationInitializer} 的本类，
 *       并回调 {@link #onStartup(jakarta.servlet.ServletContext)} 方法。</li>
 *   <li><b>Spring IoC 容器与宿主环境绑定：</b>
 *       {@link #configure(SpringApplicationBuilder)} 将 {@link PortfolioApplication} 主类注册为配置源（Source），
 *       从而在宿主容器的 {@code ServletContext} 内部拉起 Spring Boot 应用上下文，
 *       实现与内置内嵌服务器运行时完全等价的自动装配、过滤器链、AOP 切面与数据源初始化。</li>
 *   <li><b>双轨兼备特性 (Executable WAR Pattern)：</b>
 *       依托 Gradle 8.5 构建配置中的 {@code providedCompile 'org.springframework.boot:spring-boot-starter-tomcat'}，
 *       内嵌 Tomcat 依赖被放置在 WAR 包内的 {@code WEB-INF/lib-provided/} 目录。
 *       这使得生成的 WAR 制品具备“双轨运行”能力：既可以直接扔进外部 Tomcat 部署，
 *       又可以直接在终端执行 {@code java -jar portfolio-0.0.1-SNAPSHOT.war} 启动，极大方便了本地调试与容器镜像打包。</li>
 * </ul>
 *
 * <p><b>二、部署关键兼容性注意：</b>
 * <ul>
 *   <li>本项目采用 <b>Spring Boot 3.4.2</b> 与 <b>Java 17</b>，全面迁移至 <b>Jakarta EE 10</b> 规范（使用 {@code jakarta.servlet.*} 命名空间）。
 *       外部应用服务器必须选用 <b>Apache Tomcat 10.1+</b> 或兼容 Servlet 6.0 规范的容器，<b>严禁</b>使用 Tomcat 9 或更早版本（旧版本使用 {@code javax.servlet.*} 会引发 ClassNotFoundException）。</li>
 * </ul>
 *
 * @author Development Team
 * @since 1.0.0
 * @see PortfolioApplication
 * @see SpringBootServletInitializer
 */
public class ServletInitializer extends SpringBootServletInitializer {

    /**
     * 配置并绑定 Spring Boot 应用程序的配置源主类。
     *
     * <p>在外部 Servlet 容器（如 Tomcat 10.1+）拉起应用上下文时被框架回调执行。
     *
     * @param application Spring 应用程序构建器
     * @return 配置好主类源的 SpringApplicationBuilder 实例
     */
    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        // 绑定应用核心引导主类，将 Bean 定义、组件扫描与安全过滤器链无缝装载至外部容器上下文中
        return application.sources(PortfolioApplication.class);
    }
}
