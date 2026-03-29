package com.listen.portfolio.config;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

/**
 * Flyway 数据库迁移配置类
 * 
 * 背景问题：
 * Spring Boot 4.x 的 FlywayAutoConfiguration 在某些情况下不会自动触发，导致迁移脚本无法执行。
 * 主要原因包括：
 * 1. 自动配置条件不满足（如缺少特定的配置属性）
 * 2. Bean 加载顺序问题导致 Flyway 未能正确初始化
 * 3. 与其他配置类存在循环依赖
 * 
 * 解决方案：
 * 手动创建 Flyway Bean 并通过 ApplicationRunner 在应用启动时执行迁移。
 * 采用分离设计：
 * - flyway() 方法：负责创建和配置 Flyway Bean
 * - flywayMigrationRunner() 方法：负责在应用启动时执行迁移
 * 这种分离避免了循环依赖，并确保迁移在数据源初始化后、应用启动前执行。
 * 
 * 执行时机：
 * 通过 @Order(1) 确保 Flyway 迁移在所有其他 ApplicationRunner 之前执行，
 * 保证数据库结构在业务逻辑启动前已经就绪。
 */
@Configuration
public class FlywayConfig {

    private static final Logger logger = LoggerFactory.getLogger(FlywayConfig.class);

    /**
     * 手动创建 Flyway Bean
     * 
     * 配置说明：
     * - dataSource(dataSource)：使用 Spring 管理的数据源，支持环境变量配置
     * - locations("classpath:db/migration")：指定迁移脚本位置，默认扫描 src/main/resources/db/migration 目录
     * - baselineOnMigrate(true)：允许对已有数据库执行 baseline 操作
     *   当数据库已存在表结构时，Flyway 会创建 baseline 记录而不是报错
     * - baselineVersion("0")：设置 baseline 版本号为 0，后续迁移脚本从 V1 开始
     *   这样可以区分 baseline 之前的表结构和 Flyway 管理的迁移
     * 
     * 为什么不使用 application.properties 配置：
     * Spring Boot 4.x 的自动配置在某些情况下不可靠，手动创建 Bean 可以确保：
     * 1. Flyway 一定会被初始化
     * 2. 配置项完全可控
     * 3. 避免与其他自动配置的冲突
     * 
     * @param dataSource Spring 管理的数据源 Bean
     * @return 配置好的 Flyway 实例
     */
    @Bean
    public Flyway flyway(DataSource dataSource) {
        logger.info("Creating Flyway bean manually");
        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .baselineVersion("0")
                .load();
        logger.info("Flyway bean created successfully");
        return flyway;
    }

    /**
     * Flyway 迁移执行器
     * 
     * 执行流程：
     * 1. baseline()：为已有数据库创建基线记录
     *    - 如果 flyway_schema_history 表不存在，创建并插入 baseline 记录
     *    - 如果已存在 baseline 记录，跳过此步骤
     *    - baseline 版本号为 0，标记为 Flyway 管理的起点
     * 
     * 2. migrate()：执行未应用的迁移脚本
     *    - 扫描 db/migration 目录下的所有 V*.sql 文件
     *    - 按版本号顺序执行未应用的迁移
     *    - 每个迁移成功后在 flyway_schema_history 表中记录
     *    - 返回本次执行的迁移数量
     * 
     * 异常处理：
     * - FlywayValidateException：迁移验证失败（如存在失败的迁移记录）
     *   自动执行 repair() 修复失败记录，然后重试迁移
     * - 其他异常：记录错误日志并抛出，阻止应用启动
     *   这确保了数据库问题能够及时发现，避免在错误的数据库结构上运行应用
     * 
     * 为什么使用 ApplicationRunner：
     * - 在 Spring 容器完全初始化后执行
     * - 在应用接收请求前执行
     * - 通过 @Order(1) 确保优先级最高
     * 
     * @param flyway Flyway Bean 实例
     * @return ApplicationRunner 实现，在应用启动时执行迁移
     */
    @Bean
    @Order(1)
    public ApplicationRunner flywayMigrationRunner(Flyway flyway) {
        return new ApplicationRunner() {
            @Override
            public void run(ApplicationArguments args) {
                logger.info("=== Starting Flyway migration ===");
                try {
                    // 先执行 baseline（如果需要）
                    flyway.baseline();
                    logger.info("Flyway baseline completed");
                    
                    // 再执行迁移
                    int migrationsApplied = flyway.migrate().migrationsExecuted;
                    logger.info("Flyway migration completed. Migrations applied: {}", migrationsApplied);
                } catch (org.flywaydb.core.api.exception.FlywayValidateException e) {
                    logger.warn("Flyway validation failed, attempting to repair and retry", e);
                    try {
                        flyway.repair();
                        logger.info("Flyway repair completed, retrying migration");
                        int migrationsApplied = flyway.migrate().migrationsExecuted;
                        logger.info("Flyway migration completed after repair. Migrations applied: {}", migrationsApplied);
                    } catch (Exception repairException) {
                        logger.error("Flyway migration failed even after repair", repairException);
                        throw repairException;
                    }
                } catch (Exception e) {
                    logger.error("Flyway migration failed", e);
                    throw e;
                }
            }
        };
    }
}
