package com.listen.portfolio.integration;

import java.io.IOException;
import java.net.ServerSocket;
import java.sql.Connection;
import java.sql.Statement;
import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import redis.embedded.RedisServer;

/**
 * 全局集成测试统一基类 (Base Integration Test)。
 *
 * <p><b>一、核心设计理念与架构考量：</b>
 * <ul>
 *   <li><b>零外部依赖的确定性隔离运行 (Self-Contained Deterministic Testing)：</b>
 *       为了保证开发者在本地（Mac/Windows/Linux）或 GitHub Actions CI 流水线中无需预先安装配置
 *       外部 MySQL 与 Redis 实例，本基类集成了纯内存 <b>H2 内存数据库</b> 与 <b>嵌入式 Redis (Embedded Redis)</b>，
 *       实现开箱即用、完全自包含的端到端集成测试执行环境。</li>
 *   <li><b>动态随机端口与并发防冲突机制 (Dynamic Ephemeral Port Allocation)：</b>
 *       通过 {@link #freePort()} 动态申请操作系统当前空闲的临时端口绑定嵌入式 Redis。
 *       避免硬编码默认端口（6379）导致与本地已运行的生产/开发容器发生端口冲突，支持多测试类并发安全执行。</li>
 *   <li><b>跨平台操作系统适配 (Cross-Platform OS Adaptation)：</b>
 *       针对 Windows 操作系统检测（{@code os.name.contains("win")}），自动注入 MSOpenTech Redis 端口所需的
 *       {@code maxheap 128M} 内存配额约束，彻底解决 Windows 环境下嵌入式 Redis 启动报错 {@code OOM command not allowed} 的兼容性痛点。</li>
 *   <li><b>JVM 停机钩子安全回收 (Graceful Resource Teardown)：</b>
 *       静态注册 {@link Runtime#addShutdownHook}，确保在测试 JVM 进程退出时安全关闭嵌入式 Redis 守护进程，
 *       绝不遗留任何孤儿后台进程占用系统资源。</li>
 *   <li><b>H2 与 MySQL 专有 SQL 方言兼容桥接 (Dialect Bridging via SQL Alias)：</b>
 *       在 {@link #initH2Functions()} 中，向 H2 内存库动态注册 MySQL 专有函数 {@code BINARY} 的 Java 静态映射别名
 *       （指向 {@link H2Functions#binary}），确保业务代码中的大小写敏感校验 SQL 在纯内存 H2 环境中完美兼容执行。</li>
 * </ul>
 *
 * @author Development Team
 * @since 1.0.0
 * @see H2Functions
 */
@SpringBootTest(properties = {
        // 关键配置：覆盖主配置中的 spring.flyway.enabled=true，
        // 避免 Flyway 在 H2 内存库上尝试执行包含 MySQL 专有特性的物理迁移脚本（H2 采用 schema-h2.sql 自动建表）
        "spring.flyway.enabled=false"
})
@ActiveProfiles("test")
public abstract class BaseIntegrationTest {

    private static final Logger log = LoggerFactory.getLogger(BaseIntegrationTest.class);

    /**
     * 当前运行周期分配的 Redis 监听端口，默认回退为 6379
     */
    private static int redisPort = 6379;

    /**
     * 嵌入式 Redis 服务端单例实例
     */
    private static RedisServer embeddedRedisServer;

    @Autowired
    private DataSource dataSource;

    // 静态代码块：在任何集成测试类加载时初始化嵌入式 Redis 运行时
    static {
        try {
            int port = freePort();
            var builder = RedisServer.builder().port(port);

            // 1. Windows 特殊参数处理：maxheap 限制
            if (System.getProperty("os.name").toLowerCase().contains("win")) {
                builder.setting("maxheap 128M");
            }

            // 2. 构建并启动嵌入式 Redis 实例
            RedisServer server = builder.build();
            server.start();
            embeddedRedisServer = server;
            redisPort = port;
            log.info(">>> [BaseIntegrationTest] 嵌入式 Redis 服务启动成功，监听随机端口: {}", port);

            // 3. 注册 JVM 关闭钩子，确保进程退出时彻底回收资源
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    if (embeddedRedisServer != null && embeddedRedisServer.isActive()) {
                        embeddedRedisServer.stop();
                        log.info(">>> [BaseIntegrationTest] 嵌入式 Redis 服务已安全停止退出");
                    }
                } catch (Exception ignored) {
                }
            }));
        } catch (Throwable t) {
            // 容灾回退：若嵌入式 Redis 在受限容器环境中启动受阻，安全降级回退至默认端口 6379
            log.warn(">>> [BaseIntegrationTest] 嵌入式 Redis 启动失败 ({})，回退连接默认端口 6379", t.getMessage());
            redisPort = 6379;
        }
    }

    /**
     * 申请获取本地操作系统当前未被占用的可用空闲端口。
     *
     * <p>利用 {@code new ServerSocket(0)} 由操作系统内核自动分配临时端口号（Ephemeral Port），
     * 获取端口后立即关闭 Socket 并返回该端口。
     *
     * @return 可用的端口号（若发生异常则保守回退为 6379）
     */
    private static int freePort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (IOException e) {
            return 6379;
        }
    }

    /**
     * 动态属性注入：在 Spring 容器启动前，动态重写 Spring Data Redis 的连接参数。
     *
     * <p>利用 Spring 5.2.5+ / Spring Boot 2.2.6+ 提供的 {@link DynamicPropertySource}，
     * 将随机分配的 {@code redisPort} 动态赋给 {@code spring.data.redis.port}，
     * 确保所有的 {@link org.springframework.data.redis.core.RedisTemplate} 均精准连接至本地嵌入式实例。
     *
     * @param registry Spring 动态属性注册器
     */
    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", () -> "127.0.0.1");
        registry.add("spring.data.redis.port", () -> String.valueOf(redisPort));
        registry.add("spring.data.redis.timeout", () -> "2000ms");
        registry.add("spring.data.redis.database", () -> "1");
    }

    /**
     * 在每个测试方法执行前，向 H2 内存数据库动态注册 MySQL 专有函数别名。
     *
     * <p><b>方言兼容背景：</b>
     * MySQL 原生支持 {@code BINARY name = #{name}} 实现严格区分大小写的字符串比对；
     * 而 H2 原生语法不支持该前缀操作符。通过在 H2 中执行别名注册：
     * {@code CREATE ALIAS IF NOT EXISTS "BINARY" DETERMINISTIC FOR "com.listen.portfolio.integration.H2Functions.binary"}，
     * 使得在 H2 数据库上执行相同 SQL 时调用 Java 静态方法返回字符串，实现单测与生产 SQL 语法的高度统一。
     */
    @BeforeEach
    public void initH2Functions() {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE ALIAS IF NOT EXISTS \"BINARY\" DETERMINISTIC FOR \"com.listen.portfolio.integration.H2Functions.binary\"");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 测试前置环境准备与隔离。
     */
    @BeforeEach
    void setUp() {
        // 当前依赖短生命周期单测用例与嵌入式数据库隔离，可按需扩展 Redis 键清理逻辑
    }
}
