package com.listen.portfolio.common;

import com.listen.portfolio.common.error.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ErrorCode 单元测试
 * 
 * 说明：测试错误码枚举的所有核心功能
 * 目的：确保错误码定义正确且完整
 */
@DisplayName("ErrorCode Unit Tests")
class ErrorCodeTest {

    @Test
    @DisplayName("BAD_REQUEST - 验证错误码属性")
    void testBadRequest_HasCorrectProperties() {
        ErrorCode errorCode = ErrorCode.BAD_REQUEST;
        
        assertEquals("400", errorCode.getMessageId());
        assertEquals("Bad request", errorCode.getDefaultMessage());
        assertEquals(HttpStatus.BAD_REQUEST, errorCode.getHttpStatus());
    }

    @Test
    @DisplayName("UNAUTHORIZED - 验证错误码属性")
    void testUnauthorized_HasCorrectProperties() {
        ErrorCode errorCode = ErrorCode.UNAUTHORIZED;
        
        assertEquals("401", errorCode.getMessageId());
        assertEquals("Unauthorized", errorCode.getDefaultMessage());
        assertEquals(HttpStatus.UNAUTHORIZED, errorCode.getHttpStatus());
    }

    @Test
    @DisplayName("FORBIDDEN - 验证错误码属性")
    void testForbidden_HasCorrectProperties() {
        ErrorCode errorCode = ErrorCode.FORBIDDEN;
        
        assertEquals("403", errorCode.getMessageId());
        assertEquals("Forbidden", errorCode.getDefaultMessage());
        assertEquals(HttpStatus.FORBIDDEN, errorCode.getHttpStatus());
    }

    @Test
    @DisplayName("INTERNAL_ERROR - 验证错误码属性")
    void testInternalError_HasCorrectProperties() {
        ErrorCode errorCode = ErrorCode.INTERNAL_ERROR;
        
        assertEquals("1", errorCode.getMessageId());
        assertEquals("Internal server error", errorCode.getDefaultMessage());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, errorCode.getHttpStatus());
    }

    @Test
    @DisplayName("values - 验证所有错误码存在")
    void testValues_ContainsAllErrorCodes() {
        ErrorCode[] errorCodes = ErrorCode.values();
        
        assertNotNull(errorCodes);
        assertTrue(errorCodes.length >= 4);
        
        boolean hasBadRequest = false;
        boolean hasUnauthorized = false;
        boolean hasForbidden = false;
        boolean hasInternalError = false;
        
        for (ErrorCode code : errorCodes) {
            if (code == ErrorCode.BAD_REQUEST) hasBadRequest = true;
            if (code == ErrorCode.UNAUTHORIZED) hasUnauthorized = true;
            if (code == ErrorCode.FORBIDDEN) hasForbidden = true;
            if (code == ErrorCode.INTERNAL_ERROR) hasInternalError = true;
        }
        
        assertTrue(hasBadRequest, "应该包含 BAD_REQUEST");
        assertTrue(hasUnauthorized, "应该包含 UNAUTHORIZED");
        assertTrue(hasForbidden, "应该包含 FORBIDDEN");
        assertTrue(hasInternalError, "应该包含 INTERNAL_ERROR");
    }

    @Test
    @DisplayName("valueOf - 通过名称获取错误码")
    void testValueOf_ReturnsCorrectErrorCode() {
        ErrorCode errorCode = ErrorCode.valueOf("BAD_REQUEST");
        
        assertEquals(ErrorCode.BAD_REQUEST, errorCode);
    }

    @Test
    @DisplayName("getMessageId - 所有错误码都有消息ID")
    void testGetMessageId_AllErrorCodesHaveMessageId() {
        for (ErrorCode code : ErrorCode.values()) {
            assertNotNull(code.getMessageId(), 
                    code.name() + " 应该有消息ID");
            assertFalse(code.getMessageId().isEmpty(), 
                    code.name() + " 的消息ID不应为空");
        }
    }

    @Test
    @DisplayName("getDefaultMessage - 所有错误码都有默认消息")
    void testGetDefaultMessage_AllErrorCodesHaveDefaultMessage() {
        for (ErrorCode code : ErrorCode.values()) {
            assertNotNull(code.getDefaultMessage(), 
                    code.name() + " 应该有默认消息");
            assertFalse(code.getDefaultMessage().isEmpty(), 
                    code.name() + " 的默认消息不应为空");
        }
    }

    @Test
    @DisplayName("getHttpStatus - 所有错误码都有 HTTP 状态码")
    void testGetHttpStatus_AllErrorCodesHaveHttpStatus() {
        for (ErrorCode code : ErrorCode.values()) {
            assertNotNull(code.getHttpStatus(), 
                    code.name() + " 应该有 HTTP 状态码");
        }
    }

    @Test
    @DisplayName("HTTP status mapping - 验证 HTTP 状态码映射正确")
    void testHttpStatusMapping_IsCorrect() {
        assertEquals(HttpStatus.BAD_REQUEST, ErrorCode.BAD_REQUEST.getHttpStatus());
        assertEquals(HttpStatus.UNAUTHORIZED, ErrorCode.UNAUTHORIZED.getHttpStatus());
        assertEquals(HttpStatus.FORBIDDEN, ErrorCode.FORBIDDEN.getHttpStatus());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.INTERNAL_ERROR.getHttpStatus());
    }
}
