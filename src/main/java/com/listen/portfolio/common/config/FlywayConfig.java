package com.listen.portfolio.common.config;

import org.flywaydb.core.api.exception.FlywayValidateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Flyway 数据库迁移策略配置。
 * 使用 Spring Boot 官方推荐的 FlywayMigrationStrategy，彻底避免与 JPA entityManagerFactory 产生循环依赖。
 */
@Configuration
public class FlywayConfig {

    private static final Logger logger = LoggerFactory.getLogger(FlywayConfig.class);

    @Bean
    public FlywayMigrationStrategy flywayMigrationStrategy() {
        return flyway -> {
            logger.info("=== Starting Flyway migration via FlywayMigrationStrategy ===");
            try {
                flyway.baseline();
                int migrationsApplied = flyway.migrate().migrationsExecuted;
                logger.info("Flyway migration completed. Migrations applied: {}", migrationsApplied);
            } catch (FlywayValidateException e) {
                logger.warn("Flyway validation failed, attempting repair", e);
                try {
                    flyway.repair();
                    int migrationsApplied = flyway.migrate().migrationsExecuted;
                    logger.info("Flyway migration completed after repair. Migrations applied: {}", migrationsApplied);
                } catch (Exception repairException) {
                    logger.error("Flyway migration failed after repair", repairException);
                    throw repairException;
                }
            } catch (Exception e) {
                logger.error("Flyway migration failed", e);
                throw e;
            }
        };
    }
}
