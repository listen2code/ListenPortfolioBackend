package com.listen.portfolio.integration;

import java.io.IOException;
import java.net.ServerSocket;
import java.sql.Connection;
import java.sql.Statement;
import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import redis.embedded.RedisServer;

/**
 * 集成测试基类
 *
 * 说明：使用嵌入式 Redis（无需本机 Docker / redis-server），便于本地与 CI 运行 {@code mvn test}。
 */
@SpringBootTest(properties = {
        // 覆盖主配置中的 spring.flyway.enabled=true，避免在 H2 上执行 MySQL 迁移脚本
        "spring.flyway.enabled=false"
})
@ActiveProfiles("test")
public abstract class BaseIntegrationTest {

    private static final int REDIS_PORT = freePort();

    @Autowired
    private DataSource dataSource;

    static {
        try {
            var builder = RedisServer.builder()
                    .port(REDIS_PORT);
            
            // maxheap is a Windows-specific configuration of MSOpenTech Redis port.
            // Applying it on Linux/macOS causes Redis startup failure.
            if (System.getProperty("os.name").toLowerCase().contains("win")) {
                builder.setting("maxheap 128M");
            }
            
            RedisServer server = builder.build();
            server.start();
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    server.stop();
                } catch (Exception ignored) {
                    // ignore
                }
            }));
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private static int freePort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (IOException e) {
            throw new IllegalStateException("No free TCP port for embedded Redis", e);
        }
    }

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", () -> "127.0.0.1");
        registry.add("spring.data.redis.port", () -> String.valueOf(REDIS_PORT));
        registry.add("spring.data.redis.timeout", () -> "2000ms");
        registry.add("spring.data.redis.database", () -> "1");
    }

    @BeforeEach
    public void initH2Functions() {
        // 在 H2 中注册 MySQL 的 BINARY 函数别名，保证通过用户名匹配进行大小写敏感校验时的 SQL 兼容性
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE ALIAS IF NOT EXISTS \"BINARY\" DETERMINISTIC FOR \"com.listen.portfolio.integration.H2Functions.binary\"");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @BeforeEach
    void setUp() {
        // 可在此按 key 模式清理 Redis；当前依赖短生命周期用例与嵌入式实例隔离
    }
}
