package com.listen.portfolio.performance;

import com.listen.portfolio.common.jwt.JwtUtil;
import com.listen.portfolio.integration.BaseIntegrationTest;
import com.listen.portfolio.service.RateLimitService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 性能测试
 * 
 * 说明：测试系统的性能相关功能
 * 目的：验证 JWT 处理性能、限流性能、并发处理能力等
 *
 * <p>阈值在 CI / Docker / 共享 CPU 下需留足余量；此处仅防止数量级退化，不作为严格 SLA。</p>
 */
public class PerformanceTest extends BaseIntegrationTest {

    private static final long MAX_MS_FOR_1000_OPS = 120_000L;
    private static final double MAX_AVG_MS_PER_MICRO_BENCH = 25.0;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private RateLimitService rateLimitService;

    private UserDetails testUser;

    @BeforeEach
    void setUp() {
        testUser = User.withUsername("testuser")
                .password("password")
                .roles("USER")
                .build();
    }

    @Test
    @DisplayName("JWT 性能 - 令牌生成性能")
    void testJwtTokenGenerationPerformance() {
        // Given
        int tokenCount = 1000;

        // When
        long startTime = System.currentTimeMillis();
        List<String> tokens = new ArrayList<>();
        
        for (int i = 0; i < tokenCount; i++) {
            String token = jwtUtil.generateToken(testUser);
            tokens.add(token);
        }
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        // Then
        assertEquals(tokenCount, tokens.size(), "应该生成指定数量的令牌");
        
        // 性能断言：生成1000个令牌应该在合理时间内完成（比如5秒）
        assertTrue(duration < 5000, 
                  String.format("生成 %d 个令牌耗时 %d ms，应该少于 5000 ms", tokenCount, duration));
        
        // 计算平均每个令牌的生成时间
        double avgTimePerToken = (double) duration / tokenCount;
        assertTrue(avgTimePerToken < 5.0, 
                  String.format("平均每个令牌生成时间 %.2f ms，应该少于 5 ms", avgTimePerToken));
    }

    @Test
    @DisplayName("JWT 性能 - 令牌验证性能")
    void testJwtTokenValidationPerformance() {
        // Given
        int tokenCount = 1000;
        List<String> tokens = new ArrayList<>();
        
        for (int i = 0; i < tokenCount; i++) {
            tokens.add(jwtUtil.generateToken(testUser));
        }

        // When
        long startTime = System.currentTimeMillis();
        int validCount = 0;
        
        for (String token : tokens) {
            if (jwtUtil.validateToken(token, testUser)) {
                validCount++;
            }
        }
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        // Then
        assertEquals(tokenCount, validCount, "所有令牌都应该验证通过");
        
        assertTrue(duration < MAX_MS_FOR_1000_OPS,
                  String.format("验证 %d 个令牌耗时 %d ms，应少于 %d ms", tokenCount, duration, MAX_MS_FOR_1000_OPS));
        
        double avgTimePerToken = (double) duration / tokenCount;
        assertTrue(avgTimePerToken < 120.0,
                  String.format("平均每个令牌验证时间 %.2f ms，应少于 120 ms", avgTimePerToken));
    }

    @Test
    @DisplayName("JWT 性能 - 用户名提取性能")
    void testJwtUsernameExtractionPerformance() {
        // Given
        int tokenCount = 1000;
        List<String> tokens = new ArrayList<>();
        
        for (int i = 0; i < tokenCount; i++) {
            tokens.add(jwtUtil.generateToken(testUser));
        }

        // When
        long startTime = System.currentTimeMillis();
        int extractedCount = 0;
        
        for (String token : tokens) {
            String username = jwtUtil.extractUsername(token);
            if (testUser.getUsername().equals(username)) {
                extractedCount++;
            }
        }
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        // Then
        assertEquals(tokenCount, extractedCount, "应该成功提取所有用户名");
        
        assertTrue(duration < MAX_MS_FOR_1000_OPS,
                  String.format("提取 %d 个用户名耗时 %d ms，应少于 %d ms", tokenCount, duration, MAX_MS_FOR_1000_OPS));
    }

    @Test
    @DisplayName("限流性能 - 单线程限流性能")
    void testRateLimitSingleThreadPerformance() {
        // Given
        String identifier = "performance:test";
        int requestCount = 1000;
        int maxRequests = 100;
        int timeWindowSeconds = 60;

        // When
        long startTime = System.currentTimeMillis();
        int allowedCount = 0;
        
        for (int i = 0; i < requestCount; i++) {
            if (rateLimitService.isAllowed(identifier, maxRequests, timeWindowSeconds)) {
                allowedCount++;
            }
        }
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        // Then
        assertEquals(maxRequests, allowedCount, "应该只有最大请求数量的请求通过");
        
        // 性能断言：1000次限流检查应该在合理时间内完成（比如2秒）
        assertTrue(duration < 2000, 
                  String.format("%d 次限流检查耗时 %d ms，应该少于 2000 ms", requestCount, duration));
        
        // 计算平均每次限流检查的时间
        double avgTimePerCheck = (double) duration / requestCount;
        assertTrue(avgTimePerCheck < 2.0, 
                  String.format("平均每次限流检查时间 %.2f ms，应该少于 2 ms", avgTimePerCheck));
    }

    @Test
    @DisplayName("限流性能 - 多线程并发限流性能")
    void testRateLimitConcurrentPerformance() throws InterruptedException {
        // Given
        String identifier = "concurrent:test";
        int threadCount = 10;
        int requestsPerThread = 100;
        int maxRequests = 50;
        int timeWindowSeconds = 60;
        
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        List<CompletableFuture<Integer>> futures = new ArrayList<>();

        // When
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < threadCount; i++) {
            CompletableFuture<Integer> future = CompletableFuture.supplyAsync(() -> {
                int allowedCount = 0;
                for (int j = 0; j < requestsPerThread; j++) {
                    if (rateLimitService.isAllowed(identifier, maxRequests, timeWindowSeconds)) {
                        allowedCount++;
                    }
                }
                return allowedCount;
            });
            futures.add(future);
        }
        
        // 等待所有任务完成
        int totalAllowed = 0;
        for (CompletableFuture<Integer> future : futures) {
            try {
                totalAllowed += future.get(5, TimeUnit.SECONDS);
            } catch (Exception e) {
                // 忽略异常，继续处理其他任务
            }
        }
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        // Then
        assertEquals(maxRequests, totalAllowed, "应该只有最大请求数量的请求通过");
        
        // 性能断言：并发限流检查应该在合理时间内完成（比如3秒）
        assertTrue(duration < 3000, 
                  String.format("%d 线程并发限流检查耗时 %d ms，应该少于 3000 ms", threadCount, duration));
    }

    @Test
    @DisplayName("JWT 性能 - 并发令牌生成")
    void testJwtConcurrentTokenGeneration() throws InterruptedException {
        // Given
        int threadCount = 10;
        int tokensPerThread = 100;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        List<CompletableFuture<List<String>>> futures = new ArrayList<>();

        // When
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < threadCount; i++) {
            CompletableFuture<List<String>> future = CompletableFuture.supplyAsync(() -> {
                List<String> tokens = new ArrayList<>();
                for (int j = 0; j < tokensPerThread; j++) {
                    tokens.add(jwtUtil.generateToken(testUser));
                }
                return tokens;
            });
            futures.add(future);
        }
        
        // 等待所有任务完成并收集结果
        int totalTokens = 0;
        for (CompletableFuture<List<String>> future : futures) {
            try {
                totalTokens += future.get(5, TimeUnit.SECONDS).size();
            } catch (Exception e) {
                // 忽略异常，继续处理其他任务
            }
        }
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        // Then
        assertEquals(threadCount * tokensPerThread, totalTokens, "应该生成指定数量的令牌");
        
        // 性能断言：并发令牌生成应该在合理时间内完成（比如5秒）
        assertTrue(duration < 5000, 
                  String.format("%d 线程并发生成 %d 个令牌耗时 %d ms，应该少于 5000 ms", 
                               threadCount, totalTokens, duration));
    }

    @Test
    @DisplayName("JWT 性能 - 并发令牌验证")
    void testJwtConcurrentTokenValidation() throws InterruptedException {
        // Given
        int tokenCount = 1000;
        List<String> tokens = new ArrayList<>();
        
        for (int i = 0; i < tokenCount; i++) {
            tokens.add(jwtUtil.generateToken(testUser));
        }
        
        int threadCount = 10;
        int tokensPerThread = tokenCount / threadCount;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        List<CompletableFuture<Integer>> futures = new ArrayList<>();

        // When
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < threadCount; i++) {
            int startIndex = i * tokensPerThread;
            int endIndex = (i == threadCount - 1) ? tokenCount : startIndex + tokensPerThread;
            List<String> threadTokens = tokens.subList(startIndex, endIndex);
            
            CompletableFuture<Integer> future = CompletableFuture.supplyAsync(() -> {
                int validCount = 0;
                for (String token : threadTokens) {
                    if (jwtUtil.validateToken(token, testUser)) {
                        validCount++;
                    }
                }
                return validCount;
            });
            futures.add(future);
        }
        
        // 等待所有任务完成
        int totalValid = 0;
        for (CompletableFuture<Integer> future : futures) {
            try {
                totalValid += future.get(5, TimeUnit.SECONDS);
            } catch (Exception e) {
                // 忽略异常，继续处理其他任务
            }
        }
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        // Then
        assertEquals(tokenCount, totalValid, "所有令牌都应该验证通过");
        
        assertTrue(duration < MAX_MS_FOR_1000_OPS,
                  String.format("%d 线程并发验证 %d 个令牌耗时 %d ms，应少于 %d ms",
                               threadCount, tokenCount, duration, MAX_MS_FOR_1000_OPS));
    }

    @Test
    @DisplayName("内存使用性能 - 大量令牌内存占用")
    void testMemoryUsageWithManyTokens() {
        // Given
        int tokenCount = 10000;
        
        // When - 生成大量令牌
        Runtime runtime = Runtime.getRuntime();
        long memoryBefore = runtime.totalMemory() - runtime.freeMemory();
        
        List<String> tokens = new ArrayList<>();
        for (int i = 0; i < tokenCount; i++) {
            tokens.add(jwtUtil.generateToken(testUser));
        }
        
        long memoryAfter = runtime.totalMemory() - runtime.freeMemory();
        long memoryUsed = memoryAfter - memoryBefore;
        
        // Then
        assertEquals(tokenCount, tokens.size(), "应该生成指定数量的令牌");
        
        // 内存使用断言：10000个令牌的内存占用应该在合理范围内（比如50MB）
        long maxMemoryBytes = 50 * 1024 * 1024; // 50MB
        assertTrue(memoryUsed < maxMemoryBytes, 
                  String.format("%d 个令牌占用内存 %d MB，应该少于 %d MB", 
                               tokenCount, memoryUsed / (1024 * 1024), maxMemoryBytes / (1024 * 1024)));
        
        // 计算平均每个令牌的内存占用
        double avgMemoryPerToken = (double) memoryUsed / tokenCount;
        assertTrue(avgMemoryPerToken < 5120, // 5KB per token
                  String.format("平均每个令牌占用内存 %.2f bytes，应该少于 5120 bytes", avgMemoryPerToken));
    }

    @Test
    @DisplayName("响应时间性能 - 系统响应时间基准")
    void testSystemResponseTimeBenchmark() {
        // Given
        int operationCount = 100;

        // When - 测试各种操作的响应时间
        long[] tokenGenerationTimes = new long[operationCount];
        long[] tokenValidationTimes = new long[operationCount];
        long[] usernameExtractionTimes = new long[operationCount];

        for (int i = 0; i < operationCount; i++) {
            // 令牌生成时间
            long startGen = System.nanoTime();
            String token = jwtUtil.generateToken(testUser);
            tokenGenerationTimes[i] = System.nanoTime() - startGen;

            // 令牌验证时间
            long startVal = System.nanoTime();
            jwtUtil.validateToken(token, testUser);
            tokenValidationTimes[i] = System.nanoTime() - startVal;

            // 用户名提取时间
            long startExt = System.nanoTime();
            jwtUtil.extractUsername(token);
            usernameExtractionTimes[i] = System.nanoTime() - startExt;
        }

        // Then - 计算平均响应时间
        double avgGenTime = average(tokenGenerationTimes) / 1_000_000.0; // 转换为毫秒
        double avgValTime = average(tokenValidationTimes) / 1_000_000.0;
        double avgExtTime = average(usernameExtractionTimes) / 1_000_000.0;

        assertTrue(avgGenTime < MAX_AVG_MS_PER_MICRO_BENCH,
                  String.format("平均令牌生成时间 %.3f ms，应少于 %.0f ms", avgGenTime, MAX_AVG_MS_PER_MICRO_BENCH));
        assertTrue(avgValTime < MAX_AVG_MS_PER_MICRO_BENCH,
                  String.format("平均令牌验证时间 %.3f ms，应少于 %.0f ms", avgValTime, MAX_AVG_MS_PER_MICRO_BENCH));
        assertTrue(avgExtTime < MAX_AVG_MS_PER_MICRO_BENCH,
                  String.format("平均用户名提取时间 %.3f ms，应少于 %.0f ms", avgExtTime, MAX_AVG_MS_PER_MICRO_BENCH));
    }

    /**
     * 计算数组的平均值
     */
    private double average(long[] values) {
        long sum = 0;
        for (long value : values) {
            sum += value;
        }
        return (double) sum / values.length;
    }
}
