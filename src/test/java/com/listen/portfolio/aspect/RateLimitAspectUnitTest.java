package com.listen.portfolio.aspect;

import com.listen.portfolio.common.ApiResponse;
import com.listen.portfolio.common.aspect.RateLimit;
import com.listen.portfolio.common.aspect.RateLimitAspect;
import com.listen.portfolio.service.RateLimitService;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RateLimitAspect 单元测试")
class RateLimitAspectUnitTest {

    @Mock
    private RateLimitService rateLimitService;

    @Mock
    private ProceedingJoinPoint joinPoint;

    @Mock
    private MethodSignature methodSignature;

    private RateLimitAspect rateLimitAspect;

    @BeforeEach
    void setUp() {
        rateLimitAspect = new RateLimitAspect(rateLimitService);
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
        SecurityContextHolder.clearContext();
    }

    // Dummy test methods with annotations
    @RateLimit(types = {RateLimit.RateLimitType.IP}, maxRequests = 5, timeWindowSeconds = 60)
    public void methodWithIpRateLimit() {}

    @RateLimit(types = {RateLimit.RateLimitType.EMAIL}, maxRequests = 3, timeWindowSeconds = 60)
    public void methodWithEmailRateLimit() {}

    @RateLimit(types = {RateLimit.RateLimitType.TOKEN}, maxRequests = 2, timeWindowSeconds = 60)
    public void methodWithTokenRateLimit() {}

    @RateLimit(types = {RateLimit.RateLimitType.USER}, maxRequests = 5, timeWindowSeconds = 60)
    public void methodWithUserRateLimit() {}

    @RateLimit(types = {RateLimit.RateLimitType.CUSTOM}, identifierExpression = "custom_expr", maxRequests = 5, timeWindowSeconds = 60)
    public void methodWithCustomRateLimit() {}

    static class SampleEmailRequest {
        private String email = "user@test.com";
    }

    static class SampleTokenRequest {
        private String token = "secret_jwt_token_1234567890";
    }

    @Test
    @DisplayName("没有 RequestContext 时跳过限流直接执行")
    void testCheckRateLimitWithoutRequest() throws Throwable {
        Method method = getClass().getMethod("methodWithIpRateLimit");
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getMethod()).thenReturn(method);
        when(joinPoint.proceed()).thenReturn("success");

        Object result = rateLimitAspect.checkRateLimit(joinPoint);
        assertEquals("success", result);
    }

    @Test
    @DisplayName("IP 限流 - 允许请求")
    void testIpRateLimitAllowed() throws Throwable {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "192.168.1.100");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        Method method = getClass().getMethod("methodWithIpRateLimit");
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getMethod()).thenReturn(method);
        when(rateLimitService.isAllowed("ip:192.168.1.100", 5, 60)).thenReturn(true);
        when(joinPoint.proceed()).thenReturn("success");

        Object result = rateLimitAspect.checkRateLimit(joinPoint);
        assertEquals("success", result);
    }

    @Test
    @DisplayName("IP 限流 - X-Real-IP 回退与超限拦截")
    void testIpRateLimitExceededWithXRealIp() throws Throwable {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Real-IP", "10.0.0.1");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        Method method = getClass().getMethod("methodWithIpRateLimit");
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getMethod()).thenReturn(method);
        when(rateLimitService.isAllowed("ip:10.0.0.1", 5, 60)).thenReturn(false);

        Object result = rateLimitAspect.checkRateLimit(joinPoint);
        assertTrue(result instanceof ResponseEntity);
        ResponseEntity<?> response = (ResponseEntity<?>) result;
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, response.getStatusCode());
        ApiResponse<?> body = (ApiResponse<?>) response.getBody();
        assertNotNull(body);
        assertEquals("RATE_LIMIT_EXCEEDED", body.getMessageId());
    }

    @Test
    @DisplayName("Email 限流 - 从参数对象反射提取邮箱")
    void testEmailRateLimit() throws Throwable {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        Method method = getClass().getMethod("methodWithEmailRateLimit");
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getMethod()).thenReturn(method);
        when(joinPoint.getArgs()).thenReturn(new Object[]{new SampleEmailRequest()});
        when(rateLimitService.isAllowed("email:user@test.com", 3, 60)).thenReturn(true);
        when(joinPoint.proceed()).thenReturn("success");

        Object result = rateLimitAspect.checkRateLimit(joinPoint);
        assertEquals("success", result);
    }

    @Test
    @DisplayName("Token 限流 - 从参数对象反射提取 token")
    void testTokenRateLimit() throws Throwable {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        Method method = getClass().getMethod("methodWithTokenRateLimit");
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getMethod()).thenReturn(method);
        when(joinPoint.getArgs()).thenReturn(new Object[]{new SampleTokenRequest()});
        when(rateLimitService.isAllowed(contains("token:secret_jwt"), eq(2), eq(60))).thenReturn(true);
        when(joinPoint.proceed()).thenReturn("success");

        Object result = rateLimitAspect.checkRateLimit(joinPoint);
        assertEquals("success", result);
    }

    @Test
    @DisplayName("User 限流 - 从 SecurityContext 提取当前用户名")
    void testUserRateLimit() throws Throwable {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("listen_user", "pwd", Collections.emptyList())
        );

        Method method = getClass().getMethod("methodWithUserRateLimit");
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getMethod()).thenReturn(method);
        when(rateLimitService.isAllowed("user:listen_user", 5, 60)).thenReturn(true);
        when(joinPoint.proceed()).thenReturn("success");

        Object result = rateLimitAspect.checkRateLimit(joinPoint);
        assertEquals("success", result);
    }

    @Test
    @DisplayName("Custom 限流 - 表达式标识符提取")
    void testCustomRateLimit() throws Throwable {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        Method method = getClass().getMethod("methodWithCustomRateLimit");
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getMethod()).thenReturn(method);
        when(rateLimitService.isAllowed("custom:custom_expr", 5, 60)).thenReturn(true);
        when(joinPoint.proceed()).thenReturn("success");

        Object result = rateLimitAspect.checkRateLimit(joinPoint);
        assertEquals("success", result);
    }
}
