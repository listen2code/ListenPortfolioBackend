package com.listen.portfolio.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ApiResponse 单元测试
 * 
 * 说明：测试统一响应封装类的所有核心功能
 * 目的：确保 API 响应格式正确且一致
 */
@DisplayName("ApiResponse Unit Tests")
class ApiResponseTest {

    @Test
    @DisplayName("success - 创建成功响应（带数据）")
    void testSuccess_WithData_ReturnsSuccessResponse() {
        String testData = "test data";
        
        ApiResponse<String> response = ApiResponse.success(testData);
        
        assertNotNull(response);
        assertEquals("0", response.getResult());
        assertEquals("", response.getMessageId());
        assertEquals("", response.getMessage());
        assertEquals(testData, response.getBody());
    }

    @Test
    @DisplayName("success - 创建成功响应（null 数据）")
    void testSuccess_WithNullData_ReturnsSuccessResponse() {
        ApiResponse<Object> response = ApiResponse.success(null);
        
        assertNotNull(response);
        assertEquals("0", response.getResult());
        assertNull(response.getBody());
    }

    @Test
    @DisplayName("success - 创建成功响应（复杂对象）")
    void testSuccess_WithComplexObject_ReturnsSuccessResponse() {
        TestDto testDto = new TestDto("test", 123);
        
        ApiResponse<TestDto> response = ApiResponse.success(testDto);
        
        assertNotNull(response);
        assertEquals("0", response.getResult());
        assertEquals(testDto, response.getBody());
        assertEquals("test", response.getBody().getName());
        assertEquals(123, response.getBody().getValue());
    }

    @Test
    @DisplayName("error - 创建错误响应")
    void testError_ReturnsErrorResponse() {
        String messageId = "ERR001";
        String message = "错误消息";
        
        ApiResponse<Void> response = ApiResponse.error(messageId, message);
        
        assertNotNull(response);
        assertEquals("1", response.getResult());
        assertEquals(messageId, response.getMessageId());
        assertEquals(message, response.getMessage());
        assertNull(response.getBody());
    }

    @Test
    @DisplayName("constructor - 使用构造函数创建响应")
    void testConstructor_CreatesResponseCorrectly() {
        String result = "0";
        String messageId = "MSG001";
        String message = "成功";
        String body = "data";
        
        ApiResponse<String> response = new ApiResponse<>(result, messageId, message, body);
        
        assertNotNull(response);
        assertEquals(result, response.getResult());
        assertEquals(messageId, response.getMessageId());
        assertEquals(message, response.getMessage());
        assertEquals(body, response.getBody());
    }

    @Test
    @DisplayName("setters - 测试所有 setter 方法")
    void testSetters_ModifyFieldsCorrectly() {
        ApiResponse<String> response = new ApiResponse<>("0", "", "", null);
        
        response.setResult("1");
        response.setMessageId("ERR001");
        response.setMessage("错误");
        response.setBody("new data");
        
        assertEquals("1", response.getResult());
        assertEquals("ERR001", response.getMessageId());
        assertEquals("错误", response.getMessage());
        assertEquals("new data", response.getBody());
    }

    @Test
    @DisplayName("isSuccess - 判断响应是否成功")
    void testIsSuccess_ReturnsCorrectValue() {
        ApiResponse<String> successResponse = ApiResponse.success("data");
        ApiResponse<Void> errorResponse = ApiResponse.error("ERR001", "error");
        
        assertTrue(successResponse.isSuccess());
        assertFalse(errorResponse.isSuccess());
    }

    @Test
    @DisplayName("getCode - 获取响应码")
    void testGetCode_ReturnsCorrectCode() {
        ApiResponse<String> successResponse = ApiResponse.success("data");
        ApiResponse<Void> errorResponse = ApiResponse.error("ERR001", "error");
        
        assertEquals("", successResponse.getCode());
        assertEquals("ERR001", errorResponse.getCode());
    }

    private static class TestDto {
        private String name;
        private int value;

        public TestDto(String name, int value) {
            this.name = name;
            this.value = value;
        }

        public String getName() {
            return name;
        }

        public int getValue() {
            return value;
        }
    }
}
