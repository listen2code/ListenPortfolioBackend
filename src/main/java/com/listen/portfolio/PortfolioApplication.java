package com.listen.portfolio;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;

/**
 * Portfolio 后端微服务启动主类 (Spring Boot Application Entrypoint)。
 *
 * <h3>核心架构与装配设计考量：</h3>
 * <ul>
 *   <li><b>排除 FlywayAutoConfiguration 自动装配：</b>
 *       通过 {@code @SpringBootApplication(exclude = {FlywayAutoConfiguration.class})} 显式关闭框架默认的 Flyway 自动装配。
 *       如在开发手册与 Flyway 章节所述，此举旨在彻底根除 Spring 容器初始化早期，
 *       数据源 (DataSource)、安全用户加载 (CustomUserDetailsService) 与 Flyway 迁移之间的底层循环依赖死锁。
 *       实际的迁移交由 {@link com.listen.portfolio.common.config.FlywayConfig} 在应用就绪时受控拉起。</li>
 *   <li><b>MyBatis-Plus Mapper 扫描范围收敛：</b>
 *       声明 {@code @MapperScan("com.listen.portfolio.mapper")}，自动为数据访问层接口生成动态代理实现类，
 *       并注入 Spring IoC 容器，提供无侵入的强类型 CRUD 能力。</li>
 *   <li><b>跨环境运行支持：</b>
 *       同时支持本地直接执行 {@code main()} 方法启动、Docker 容器化注入参数运行、以及打成 WAR 包部署至外部 Servlet 容器。</li>
 * </ul>
 *
 * @see ServletInitializer
 */
@SpringBootApplication(exclude = {FlywayAutoConfiguration.class})
@MapperScan("com.listen.portfolio.mapper")
public class PortfolioApplication {

    /**
     * 应用程序标准启动入口。
     *
     * @param args 命令行启动参数 (例如 --spring.profiles.active=docker)
     */
    public static void main(String[] args) {
        SpringApplication.run(PortfolioApplication.class, args);
    }
}
