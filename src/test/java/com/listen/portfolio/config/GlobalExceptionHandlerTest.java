package com.listen.portfolio.config;

import com.listen.portfolio.common.ApiResponse;
import com.listen.portfolio.common.config.GlobalExceptionHandler;
import com.listen.portfolio.common.error.ErrorCode;
import com.listen.portfolio.common.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * GlobalExceptionHandler 单元测试
 * 
 * 说明：测试全局异常处理器的所有核心功能
 * 目的：确保异常能够被正确捕获并返回统一的错误响应
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GlobalExceptionHandler Unit Tests")
class GlobalExceptionHandlerTest {

    @InjectMocks
    private GlobalExceptionHandler globalExceptionHandler;

    @Mock
    private HttpServletRequest request;

    @Test
    @DisplayName("handleBusinessException - 处理业务异常返回正确响应")
    void testHandleBusinessException_ReturnsCorrectResponse() {
        BusinessException exception = new BusinessException(ErrorCode.BAD_REQUEST);
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/test");

        ResponseEntity<ApiResponse<Object>> response = globalExceptionHandler.handleBusinessException(exception, request);

        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(ErrorCode.BAD_REQUEST.getMessageId(), response.getBody().getCode());
        assertFalse(response.getBody().isSuccess());
    }

    @Test
    @DisplayName("handleBusinessException - 自定义消息的业务异常")
    void testHandleBusinessException_WithCustomMessage_ReturnsCorrectResponse() {
        String customMessage = "自定义错误消息";
        BusinessException exception = new BusinessException(ErrorCode.BAD_REQUEST, customMessage);
        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/api/test");

        ResponseEntity<ApiResponse<Object>> response = globalExceptionHandler.handleBusinessException(exception, request);

        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(customMessage, response.getBody().getMessage());
    }

    @Test
    @DisplayName("handleValidationException - 处理参数验证异常")
    void testHandleValidationException_ReturnsCorrectResponse() {
        MethodArgumentNotValidException exception = mock(MethodArgumentNotValidException.class);
        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/api/test");

        ResponseEntity<ApiResponse<Object>> response = globalExceptionHandler.handleValidationException(exception, request);

        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isSuccess());
        assertEquals("Validation failed", response.getBody().getMessage());
    }


    @Test
    @DisplayName("handleAccessDeniedException - 处理访问拒绝异常")
    void testHandleAccessDeniedException_ReturnsCorrectResponse() {
        AccessDeniedException exception = new AccessDeniedException("没有访问权限");
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/admin");

        ResponseEntity<ApiResponse<Object>> response = globalExceptionHandler.handleAccessDeniedException(exception, request);

        assertNotNull(response);
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isSuccess());
        assertEquals("Forbidden", response.getBody().getMessage());
    }

    @Test
    @DisplayName("handleUnhandledException - 处理未处理异常")
    void testHandleUnhandledException_ReturnsCorrectResponse() {
        Exception exception = new RuntimeException("未知错误");
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/test");

        ResponseEntity<ApiResponse<Object>> response = globalExceptionHandler.handleUnhandledException(exception, request);

        assertNotNull(response);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isSuccess());
        assertEquals("Internal server error", response.getBody().getMessage());
    }

    @Test
    @DisplayName("handleIllegalArgumentException - 处理非法参数异常")
    void testHandleIllegalArgumentException_ReturnsCorrectResponse() {
        IllegalArgumentException exception = new IllegalArgumentException("参数不合法");
        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/api/test");

        ResponseEntity<ApiResponse<Object>> response = globalExceptionHandler.handleIllegalArgumentException(exception, request);

        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isSuccess());
        assertEquals("参数不合法", response.getBody().getMessage());
    }

}
