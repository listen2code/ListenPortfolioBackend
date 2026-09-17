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
 * Flyway 数据库版本化迁移与平滑升级自动配置类。
 *
 * <h3>设计思路与核心架构考量：</h3>
 * <ul>
 *   <li><b>规避框架级循环依赖 (Cyclic Dependency Elimination)：</b>
 *       在 Spring Boot 传统开发中，若声明 <code>@Bean public Flyway flyway()</code>，由于 FlywayAutoConfiguration 会在
 *       EntityManagerFactory / MyBatis SqlSessionFactory 之前尝试构建 Flyway Bean，而部分安全框架或审计拦截器又依赖 DataSource，
 *       极易在框架启动早期形成底层循环依赖。
 *       本实现通过实现 {@link ApplicationRunner} 接口，主动关闭 <code>spring.flyway.enabled=false</code>，
 *       使 Spring 容器与 ORM Mapper 优先完成装配，并在应用上下文就绪的第一时间（<code>@Order(1)</code>）手动拉起 Flyway。</li>
 *   <li><b>多环境适配与隔离 (@Profile("!test"))：</b>
 *       单元测试与 CI/CD 环境（test profile）采用内存 H2 数据库模拟运行。由于 V1/V2 脚本包含大量 MySQL 专属语法
 *       （如 <code>ENGINE=InnoDB</code>, <code>COLLATE=utf8mb4_bin</code>, <code>INSERT IGNORE</code>, <code>ON UPDATE CURRENT_TIMESTAMP</code>），
 *       直接在 H2 执行会报错崩溃。因此通过 <code>@Profile("!test")</code> 将 Flyway 严格限制在 dev, docker, prod 等实体环境。</li>
 *   <li><b>自愈式基线校验与校验失败自动修复 (Self-Healing Baseline & Repair)：</b>
 *       当开发团队或 CI/CD 流水线修改了已有历史迁移脚本的注释、换行或微调 SQL 时，Flyway 会抛出 {@link FlywayValidateException}
 *       校验和（Checksum）不匹配异常导致容器崩溃挂掉。
 *       本实现捕获该异常后，自动触发 <code>flyway.repair()</code> 刷新 <code>flyway_schema_history</code> 中的校验和与失败状态，
 *       随后二次重试 <code>migrate()</code>，实现零人工介入的生产环境平滑启动。</li>
 * </ul>
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

    /**
     * Spring Boot 应用启动后回调执行的迁移入口。
     *
     * @param args 命令行参数
     */
    @Override
    public void run(ApplicationArguments args) {
        logger.info("=== [FlywayConfig] 开始执行 Flyway 数据库版本迁移 (via ApplicationRunner) ===");
        
        // 编程式构建 Flyway 实例并装配配置
        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)  // 若历史数据库未初始化 flyway 表，自动打上基线版本，避免抛出异常
                .baselineVersion("0")     // 基线版本设为 0，使 V1 及后续所有版本脚本均能正常执行
                .load();

        try {
            // 对未受 Flyway 托管的既有库执行基线标记（幂等操作，若已存在则无缝跳过）
            flyway.baseline();
            // 执行迁移脚本（仅执行本地高于 flyway_schema_history 最新已成功版本的脚本）
            int count = flyway.migrate().migrationsExecuted;
            logger.info(">>> [FlywayConfig] 数据库迁移成功！本次新应用的迁移脚本数量: {}", count);
        } catch (FlywayValidateException e) {
            // 捕获脚本被改动、换行符差异或校验和不匹配导致的校验失败，开启自动修复自愈流程
            logger.warn(">>> [FlywayConfig] 捕获到 Flyway 校验和校验异常 (FlywayValidateException)，正在启动自动修复 (repair)...", e);
            try {
                // repair() 会清除失败的迁移记录，并将历史脚本的新 Checksum 同步到元数据表
                flyway.repair();
                int count = flyway.migrate().migrationsExecuted;
                logger.info(">>> [FlywayConfig] 自动修复完成并成功执行迁移！本次应用的迁移脚本数量: {}", count);
            } catch (Exception repairError) {
                logger.error(">>> [FlywayConfig] 自动修复后二次迁移仍然失败，请排查 SQL 语法或连接权限", repairError);
                throw repairError;
            }
        } catch (Exception e) {
            logger.error(">>> [FlywayConfig] 数据库迁移执行遇到严重未知错误", e);
            throw e;
        }
    }
}
