package com.listen.portfolio.common;

import com.listen.portfolio.common.error.ErrorCode;
import com.listen.portfolio.common.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * BusinessException 单元测试
 * 
 * 说明：测试业务异常类的所有核心功能
 * 目的：确保业务异常能够正确创建和传递错误信息
 */
@DisplayName("BusinessException Unit Tests")
class BusinessExceptionTest {

    @Test
    @DisplayName("constructor - 使用 ErrorCode 创建异常")
    void testConstructor_WithErrorCode_CreatesException() {
        BusinessException exception = new BusinessException(ErrorCode.BAD_REQUEST);
        
        assertNotNull(exception);
        assertEquals(ErrorCode.BAD_REQUEST, exception.getErrorCode());
        assertEquals(ErrorCode.BAD_REQUEST.getDefaultMessage(), exception.getMessage());
    }

    @Test
    @DisplayName("constructor - 使用 ErrorCode 和自定义消息创建异常")
    void testConstructor_WithErrorCodeAndMessage_CreatesException() {
        String customMessage = "自定义错误消息";
        BusinessException exception = new BusinessException(ErrorCode.BAD_REQUEST, customMessage);
        
        assertNotNull(exception);
        assertEquals(ErrorCode.BAD_REQUEST, exception.getErrorCode());
        assertEquals(customMessage, exception.getMessage());
    }

    @Test
    @DisplayName("getErrorCode - 获取错误码")
    void testGetErrorCode_ReturnsCorrectErrorCode() {
        BusinessException exception = new BusinessException(ErrorCode.UNAUTHORIZED);
        
        assertEquals(ErrorCode.UNAUTHORIZED, exception.getErrorCode());
    }

    @Test
    @DisplayName("getMessage - 使用默认消息")
    void testGetMessage_WithDefaultMessage_ReturnsDefaultMessage() {
        BusinessException exception = new BusinessException(ErrorCode.FORBIDDEN);
        
        assertEquals(ErrorCode.FORBIDDEN.getDefaultMessage(), exception.getMessage());
    }

    @Test
    @DisplayName("getMessage - 使用自定义消息")
    void testGetMessage_WithCustomMessage_ReturnsCustomMessage() {
        String customMessage = "您没有权限访问此资源";
        BusinessException exception = new BusinessException(ErrorCode.FORBIDDEN, customMessage);
        
        assertEquals(customMessage, exception.getMessage());
    }

    @Test
    @DisplayName("throw and catch - 异常可以被正常抛出和捕获")
    void testThrowAndCatch_WorksCorrectly() {
        assertThrows(BusinessException.class, () -> {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR);
        });
    }

    @Test
    @DisplayName("throw and catch - 捕获后可以获取错误信息")
    void testThrowAndCatch_CanGetErrorInfo() {
        try {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "参数验证失败");
        } catch (BusinessException e) {
            assertEquals(ErrorCode.BAD_REQUEST, e.getErrorCode());
            assertEquals("参数验证失败", e.getMessage());
        }
    }

    @Test
    @DisplayName("instanceof RuntimeException - BusinessException 是 RuntimeException")
    void testInstanceOf_IsRuntimeException() {
        BusinessException exception = new BusinessException(ErrorCode.INTERNAL_ERROR);
        
        assertTrue(exception instanceof RuntimeException);
    }

    @Test
    @DisplayName("different error codes - 不同错误码创建不同异常")
    void testDifferentErrorCodes_CreateDifferentExceptions() {
        BusinessException badRequest = new BusinessException(ErrorCode.BAD_REQUEST);
        BusinessException unauthorized = new BusinessException(ErrorCode.UNAUTHORIZED);
        BusinessException forbidden = new BusinessException(ErrorCode.FORBIDDEN);
        BusinessException internalError = new BusinessException(ErrorCode.INTERNAL_ERROR);
        
        assertEquals(ErrorCode.BAD_REQUEST, badRequest.getErrorCode());
        assertEquals(ErrorCode.UNAUTHORIZED, unauthorized.getErrorCode());
        assertEquals(ErrorCode.FORBIDDEN, forbidden.getErrorCode());
        assertEquals(ErrorCode.INTERNAL_ERROR, internalError.getErrorCode());
    }
}
