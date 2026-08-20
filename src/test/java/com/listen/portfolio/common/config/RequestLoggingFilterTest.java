package com.listen.portfolio.common.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("RequestLoggingFilter 单元测试")
class RequestLoggingFilterTest {

    private final RequestLoggingFilter filter = new RequestLoggingFilter();

    @Test
    @DisplayName("普通 API 请求：自动生成 requestId 并放入响应头与 MDC")
    void testDoFilterInternalWithoutRequestId() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/v1/projects");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
        String resHeader = response.getHeader("X-Request-Id");
        assertNotNull(resHeader);
        assertFalse(resHeader.isBlank());
    }

    @Test
    @DisplayName("传入已有 X-Request-Id：复用现有 requestId")
    void testDoFilterInternalWithExistingRequestId() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/v1/auth/login");
        request.addHeader("X-Request-Id", "custom-trace-uuid-12345");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
        assertEquals("custom-trace-uuid-12345", response.getHeader("X-Request-Id"));
    }

    @Test
    @DisplayName("静态资源与图标：shouldNotFilter 返回 true")
    void testShouldNotFilter() {
        MockHttpServletRequest req1 = new MockHttpServletRequest("GET", "/images/logo.png");
        assertTrue(filter.shouldNotFilter(req1));

        MockHttpServletRequest req2 = new MockHttpServletRequest("GET", "/static/css/app.css");
        assertTrue(filter.shouldNotFilter(req2));

        MockHttpServletRequest req3 = new MockHttpServletRequest("GET", "/favicon.ico");
        assertTrue(filter.shouldNotFilter(req3));

        MockHttpServletRequest req4 = new MockHttpServletRequest("GET", "/v1/aboutMe");
        assertFalse(filter.shouldNotFilter(req4));
    }
}
