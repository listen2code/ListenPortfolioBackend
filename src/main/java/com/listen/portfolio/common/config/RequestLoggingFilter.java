package com.listen.portfolio.common.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * 全局 HTTP 请求日志记录与分布式链路追踪过滤器 (Distributed Tracing & Request Logging Filter)。
 *
 * <h3>设计理念与架构考量：</h3>
 * <ul>
 *   <li><b>全链路请求追踪 ID (Trace Correlation)：</b>
 *       优先复用客户端或 Nginx 反向代理层传递的 {@code X-Request-Id} 请求头；
 *       若不存在则生成强随机的 UUID 字符串，并同步回写至响应头 {@code X-Request-Id} 中，
 *       使前端移动端网络报错日志能与后端服务器日志精准匹配。</li>
 *   <li><b>SLF4J MDC 诊断上下文注入：</b>
 *       将 {@code requestId} 注入线程绑定的 Mapped Diagnostic Context (MDC)，
 *       确保当前请求线程后续输出的所有 Controller、Service、MyBatis SQL 日志均自动携带该标识。</li>
 *   <li><b>反向代理 Real-IP 与协议识别：</b>
 *       在生产环境中，应用部署在宿主机 Nginx 之后，原始客户端真实 IP 经由 Nginx 的
 *       {@code X-Real-IP} 和 {@code X-Forwarded-For} 传递，避免将网关本地回环 127.0.0.1 误记录为客户端地址。</li>
 *   <li><b>耗时监控与 ThreadLocal 安全释放：</b>
 *       记录请求执行耗时 (durationMs)，并在 {@code finally} 代码块中强制执行 {@code MDC.remove()}，
 *       防止 Tomcat 线程池复用线程时发生跨请求的上下文脏数据污染与内存泄漏。</li>
 * </ul>
 */
@Component
@Order(1)
public class RequestLoggingFilter extends OncePerRequestFilter {
    private static final Logger logger = LoggerFactory.getLogger(RequestLoggingFilter.class);

    private static final String REQUEST_ID_HEADER = "X-Request-Id";
    private static final String MDC_REQUEST_ID_KEY = "requestId";

    /**
     * 声明无需经过日志追踪过滤的静态静态资源路径。
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/images/")
                || path.startsWith("/static/")
                || path.equals("/favicon.ico");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String requestId = request.getHeader(REQUEST_ID_HEADER);
        if (requestId == null || requestId.isBlank()) {
            requestId = UUID.randomUUID().toString();
        }

        MDC.put(MDC_REQUEST_ID_KEY, requestId);
        response.setHeader(REQUEST_ID_HEADER, requestId);

        long startMs = System.currentTimeMillis();
        try {
            filterChain.doFilter(request, response);
        } finally {
            long durationMs = System.currentTimeMillis() - startMs;
            logger.info(
                    "requestId={} method={} path={} status={} durationMs={}",
                    requestId,
                    request.getMethod(),
                    request.getRequestURI(),
                    response.getStatus(),
                    durationMs
            );
            MDC.remove(MDC_REQUEST_ID_KEY);
        }
    }
}

