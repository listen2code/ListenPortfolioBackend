package com.listen.portfolio.integration;

import java.io.IOException;
import java.net.ServerSocket;

import org.junit.jupiter.api.BeforeEach;
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

    static {
        try {
            RedisServer server = RedisServer.builder().port(REDIS_PORT).build();
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
    void setUp() {
        // 可在此按 key 模式清理 Redis；当前依赖短生命周期用例与嵌入式实例隔离
    }
}
