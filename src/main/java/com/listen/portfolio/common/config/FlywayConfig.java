package com.listen.portfolio.common.config;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.exception.FlywayValidateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;

/**
 * Flyway 数据库迁移配置。
 * 通过 ApplicationRunner 在应用启动阶段手动拉起 Flyway 迁移，
 * 不注册 @Bean Flyway，从根本上杜绝 Spring Boot JPA 与 FlywayAutoConfiguration 的循环依赖。
 * 仅在非 test 环境 (@Profile("!test")) 生效，避免在 H2 内存数据库上执行 MySQL 迁移脚本。
 */
@Configuration
@Profile("!test")
@Order(1)
public class FlywayConfig implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(FlywayConfig.class);

    private final DataSource dataSource;

    public FlywayConfig(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(ApplicationArguments args) {
        logger.info("=== Executing Flyway migration via ApplicationRunner ===");
        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .baselineVersion("0")
                .load();

        try {
            flyway.baseline();
            int count = flyway.migrate().migrationsExecuted;
            logger.info("Flyway migration completed. Migrations applied: {}", count);
        } catch (FlywayValidateException e) {
            logger.warn("Flyway validation failed, repairing and retrying", e);
            try {
                flyway.repair();
                int count = flyway.migrate().migrationsExecuted;
                logger.info("Flyway migration completed after repair. Migrations applied: {}", count);
            } catch (Exception repairError) {
                logger.error("Flyway migration failed after repair", repairError);
                throw repairError;
            }
        } catch (Exception e) {
            logger.error("Flyway migration failed", e);
            throw e;
        }
    }
}
