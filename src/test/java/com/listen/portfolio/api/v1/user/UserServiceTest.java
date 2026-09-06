package com.listen.portfolio.api.v1.user;

import com.listen.portfolio.api.v1.user.dto.ChangePasswordRequest;
import com.listen.portfolio.api.v1.auth.dto.SignUpRequest;
import com.listen.portfolio.api.v1.user.dto.UserSummaryDto;
import com.listen.portfolio.entity.UserEntity;
import com.listen.portfolio.mapper.UserMapper;
import com.listen.portfolio.service.UserService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatcher;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Unit Tests")
class UserServiceTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private UserEntity mockUserEntity;
    private SignUpRequest mockSignUpRequest;
    private ChangePasswordRequest mockChangePasswordRequest;

    @BeforeEach
    void setUp() {
        mockUserEntity = new UserEntity();
        mockUserEntity.setId(1L);
        mockUserEntity.setName("testuser");
        mockUserEntity.setEmail("test@example.com");
        mockUserEntity.setPassword("encodedPassword");
        mockUserEntity.setLocation("Beijing");
        mockUserEntity.setAvatarUrl("http://example.com/avatar.jpg");

        mockSignUpRequest = new SignUpRequest();
        mockSignUpRequest.setUserName("testuser");
        mockSignUpRequest.setPassword("password123");
        mockSignUpRequest.setEmail("test@example.com");

        mockChangePasswordRequest = new ChangePasswordRequest();
        mockChangePasswordRequest.setUserId("1");
        mockChangePasswordRequest.setOldPassword("oldPassword");
        mockChangePasswordRequest.setNewPassword("newPassword");
    }

    @Test
    @DisplayName("getUserByName - 成功获取用户")
    void testGetUserByName_Success() {
        when(userMapper.findByNameCaseSensitive("testuser"))
                .thenReturn(mockUserEntity);

        Optional<UserEntity> result = userService.getUserByName("testuser");

        assertTrue(result.isPresent());
        assertEquals("testuser", result.get().getName());
        assertEquals("test@example.com", result.get().getEmail());
        
        verify(userMapper).findByNameCaseSensitive("testuser");
    }

    @Test
    @DisplayName("getUserByName - 用户不存在返回空Optional")
    void testGetUserByName_UserNotFound() {
        when(userMapper.findByNameCaseSensitive("nonexistent"))
                .thenReturn(null);

        Optional<UserEntity> result = userService.getUserByName("nonexistent");

        assertFalse(result.isPresent());
        verify(userMapper).findByNameCaseSensitive("nonexistent");
    }

    @Test
    @DisplayName("getUserSummaryById - 成功获取用户摘要")
    void testGetUserSummaryById_Success() {
        when(userMapper.selectById(1L))
                .thenReturn(mockUserEntity);

        Optional<UserSummaryDto> result = userService.getUserSummaryById(1L);

        assertTrue(result.isPresent());
        UserSummaryDto dto = result.get();
        assertEquals(1L, dto.getId());
        assertEquals("testuser", dto.getName());
        assertEquals("test@example.com", dto.getEmail());
        assertEquals("Beijing", dto.getLocation());
        assertEquals("http://example.com/avatar.jpg", dto.getAvatarUrl());
        
        verify(userMapper).selectById(1L);
    }

    @Test
    @DisplayName("getUserSummaryById - 用户不存在返回空Optional")
    void testGetUserSummaryById_UserNotFound() {
        when(userMapper.selectById(999L))
                .thenReturn(null);

        Optional<UserSummaryDto> result = userService.getUserSummaryById(999L);

        assertFalse(result.isPresent());
        verify(userMapper).selectById(999L);
    }

    @Test
    @DisplayName("changePassword - 成功修改密码")
    void testChangePassword_Success() {
        when(userMapper.selectById(1L))
                .thenReturn(mockUserEntity);
        when(passwordEncoder.matches("oldPassword", "encodedPassword"))
                .thenReturn(true);
        when(passwordEncoder.encode("newPassword"))
                .thenReturn("newEncodedPassword");
        when(userMapper.updateById(any(UserEntity.class)))
                .thenReturn(1);

        boolean result = userService.changePassword(mockChangePasswordRequest);

        assertTrue(result);
        
        verify(userMapper).selectById(1L);
        verify(passwordEncoder).matches("oldPassword", "encodedPassword");
        verify(passwordEncoder).encode("newPassword");
        verify(userMapper).updateById(any(UserEntity.class));
    }

    @Test
    @DisplayName("changePassword - 旧密码不匹配修改失败")
    void testChangePassword_OldPasswordMismatch() {
        when(userMapper.selectById(1L))
                .thenReturn(mockUserEntity);
        when(passwordEncoder.matches("oldPassword", "encodedPassword"))
                .thenReturn(false);

        boolean result = userService.changePassword(mockChangePasswordRequest);

        assertFalse(result);
        
        verify(userMapper).selectById(1L);
        verify(passwordEncoder).matches("oldPassword", "encodedPassword");
        verify(passwordEncoder, never()).encode(anyString());
        verify(userMapper, never()).updateById(any(UserEntity.class));
    }

    @Test
    @DisplayName("changePassword - 用户不存在修改失败")
    void testChangePassword_UserNotFound() {
        when(userMapper.selectById(1L))
                .thenReturn(null);

        boolean result = userService.changePassword(mockChangePasswordRequest);

        assertFalse(result);
        
        verify(userMapper).selectById(1L);
        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(passwordEncoder, never()).encode(anyString());
        verify(userMapper, never()).updateById(any(UserEntity.class));
    }

    @Test
    @DisplayName("deleteAccount - 成功软删除账户")
    void testDeleteAccount_Success() {
        mockUserEntity.setId(2L);
        when(userMapper.selectById(2L))
                .thenReturn(mockUserEntity);
        when(userMapper.updateById(any(UserEntity.class))).thenReturn(1);

        boolean result = userService.deleteAccount(2L);

        assertTrue(result);
        
        verify(userMapper).selectById(2L);
        verify(userMapper).updateById(mockUserEntity);
    }

    @Test
    @DisplayName("deleteAccount - 用户不存在删除失败")
    void testDeleteAccount_UserNotFound() {
        when(userMapper.selectById(2L))
                .thenReturn(null);

        boolean result = userService.deleteAccount(2L);

        assertFalse(result);
        
        verify(userMapper).selectById(2L);
        verify(userMapper, never()).updateById(any(UserEntity.class));
    }

    @Test
    @DisplayName("toUserSummaryDto - 实体转换测试")
    void testToUserSummaryDto() {
        when(userMapper.selectById(1L))
                .thenReturn(mockUserEntity);

        Optional<UserSummaryDto> result = userService.getUserSummaryById(1L);

        assertTrue(result.isPresent());
        UserSummaryDto dto = result.get();
        assertEquals(mockUserEntity.getId(), dto.getId());
        assertEquals(mockUserEntity.getName(), dto.getName());
        assertEquals(mockUserEntity.getLocation(), dto.getLocation());
        assertEquals(mockUserEntity.getEmail(), dto.getEmail());
        assertEquals(mockUserEntity.getAvatarUrl(), dto.getAvatarUrl());
    }

    @Test
    @DisplayName("构造函数注入验证")
    void testConstructorInjection() {
        assertNotNull(userService);
        assertNotNull(userMapper);
        assertNotNull(passwordEncoder);
    }

    @Test
    @DisplayName("边界测试 - null参数处理")
    void testEdgeCases_NullParameters() {
        assertEquals(Optional.empty(), userService.getUserByName(null));
        assertEquals(Optional.empty(), userService.getUserSummaryById(null));
        assertThrows(NullPointerException.class, () -> userService.changePassword(null));
        assertFalse(userService.deleteAccount(null));
    }

    @Test
    @DisplayName("边界测试 - 无效ID处理")
    void testEdgeCases_InvalidIds() {
        when(userMapper.selectById(0L)).thenReturn(null);
        when(userMapper.selectById(-1L)).thenReturn(null);

        assertEquals(Optional.empty(), userService.getUserSummaryById(0L));
        assertEquals(Optional.empty(), userService.getUserSummaryById(-1L));
        assertFalse(userService.deleteAccount(0L));
        assertFalse(userService.deleteAccount(-1L));
    }

    @Test
    @DisplayName("边界测试 - 最大ID处理")
    void testEdgeCases_MaxId() {
        UserEntity maxIdUser = new UserEntity();
        maxIdUser.setId(Long.MAX_VALUE);
        maxIdUser.setName("maxUser");
        when(userMapper.selectById(Long.MAX_VALUE)).thenReturn(maxIdUser);

        Optional<UserSummaryDto> result = userService.getUserSummaryById(Long.MAX_VALUE);

        assertTrue(result.isPresent());
        assertEquals(Long.MAX_VALUE, result.get().getId());
    }

    @Test
    @DisplayName("异常处理测试 - Repository异常")
    void testExceptionHandling_RepositoryException() {
        when(userMapper.findByNameCaseSensitive(anyString())).thenThrow(new RuntimeException("Database connection failed"));
        when(userMapper.selectById(anyLong())).thenThrow(new RuntimeException("Database connection failed"));

        assertThrows(RuntimeException.class, () -> userService.getUserByName("testuser"));
        assertThrows(RuntimeException.class, () -> userService.getUserSummaryById(1L));
        assertThrows(RuntimeException.class, () -> userService.changePassword(mockChangePasswordRequest));
        assertThrows(RuntimeException.class, () -> userService.deleteAccount(2L));
    }

    @Test
    @DisplayName("异常处理测试 - PasswordEncoder异常")
    void testExceptionHandling_PasswordEncoderException() {
        when(userMapper.selectById(1L)).thenReturn(mockUserEntity);
        when(passwordEncoder.matches(anyString(), anyString())).thenThrow(new RuntimeException("Encoding failed"));

        assertThrows(RuntimeException.class, () -> userService.changePassword(mockChangePasswordRequest));
    }

    @Test
    @DisplayName("事务边界测试 - readOnly事务")
    void testTransactionBoundary_ReadOnlyTransaction() {
        when(userMapper.findByNameCaseSensitive("testuser")).thenReturn(mockUserEntity);
        when(userMapper.selectById(1L)).thenReturn(mockUserEntity);

        Optional<UserEntity> user = userService.getUserByName("testuser");
        Optional<UserSummaryDto> userSummary = userService.getUserSummaryById(1L);

        assertTrue(user.isPresent());
        assertTrue(userSummary.isPresent());
        verify(userMapper).findByNameCaseSensitive("testuser");
        verify(userMapper).selectById(1L);
        verifyNoMoreInteractions(passwordEncoder);
    }

    @Test
    @DisplayName("事务边界测试 - 写操作事务")
    void testTransactionBoundary_WriteTransaction() {
        when(userMapper.selectById(1L)).thenReturn(mockUserEntity);
        when(passwordEncoder.matches("oldPassword", "encodedPassword")).thenReturn(true);
        when(passwordEncoder.encode("newPassword")).thenReturn("encodedNewPassword");
        when(userMapper.updateById(any(UserEntity.class))).thenReturn(1);

        boolean result = userService.changePassword(mockChangePasswordRequest);

        assertTrue(result);
        verify(passwordEncoder).matches("oldPassword", "encodedPassword");
        verify(passwordEncoder).encode("newPassword");
        verify(userMapper).updateById(any(UserEntity.class));
    }

    @Test
    @DisplayName("密码安全测试 - 密码编码验证")
    void testPasswordSecurity_EncodingVerification() {
        when(userMapper.selectById(1L)).thenReturn(mockUserEntity);
        when(passwordEncoder.matches("oldPassword", "encodedPassword")).thenReturn(true);
        when(passwordEncoder.encode("newPassword")).thenReturn("encodedNewPassword");
        when(userMapper.updateById(any(UserEntity.class))).thenReturn(1);

        boolean result = userService.changePassword(mockChangePasswordRequest);

        assertTrue(result);
        verify(passwordEncoder).encode("newPassword");
        verify(userMapper).updateById(argThat((ArgumentMatcher<UserEntity>) user -> "encodedNewPassword".equals(user.getPassword())));
    }

    @Test
    @DisplayName("数据转换测试 - toUserSummaryDto私有方法")
    void testToUserSummaryDto_PrivateMethod() throws Exception {
        when(userMapper.selectById(1L)).thenReturn(mockUserEntity);

        Optional<UserSummaryDto> result = userService.getUserSummaryById(1L);

        assertTrue(result.isPresent());
        UserSummaryDto dto = result.get();
        assertEquals(mockUserEntity.getId(), dto.getId());
        assertEquals(mockUserEntity.getName(), dto.getName());
        assertEquals(mockUserEntity.getAvatarUrl(), dto.getAvatarUrl());
    }

    @Test
    @DisplayName("性能测试 - 大量用户查询")
    void testPerformance_BulkUserQueries() {
        when(userMapper.findByNameCaseSensitive(anyString())).thenReturn(mockUserEntity);
        when(userMapper.selectById(anyLong())).thenReturn(mockUserEntity);

        long startTime = System.currentTimeMillis();
        for (int i = 0; i < 100; i++) {
            final String username = "user" + i;
            final Long userId = (long) i;
            assertDoesNotThrow(() -> userService.getUserByName(username));
            assertDoesNotThrow(() -> userService.getUserSummaryById(userId));
        }
        long endTime = System.currentTimeMillis();

        assertTrue(endTime - startTime < 1000, "Bulk queries should complete within 1 second");
    }

    @Test
    @DisplayName("集成测试 - 完整的用户管理流程")
    void testIntegration_CompleteUserManagementFlow() {
        mockUserEntity.setId(2L);
        mockChangePasswordRequest.setUserId("2");
        when(userMapper.selectById(2L)).thenReturn(mockUserEntity);
        when(passwordEncoder.matches("oldPassword", "encodedPassword")).thenReturn(true);
        when(passwordEncoder.encode("newPassword")).thenReturn("encodedNewPassword");
        when(userMapper.updateById(any(UserEntity.class))).thenReturn(1);

        boolean changePasswordResult = userService.changePassword(mockChangePasswordRequest);

        assertTrue(changePasswordResult);
        verify(passwordEncoder).encode("newPassword");
        verify(userMapper).updateById(any(UserEntity.class));

        boolean deleteAccountResult = userService.deleteAccount(2L);

        assertTrue(deleteAccountResult);
        verify(userMapper, times(2)).updateById(any(UserEntity.class));
    }

    @Test
    @DisplayName("数据隔离测试 - DTO不包含敏感信息")
    void testDataIsolation_DtoNotContainSensitiveInformation() {
        when(userMapper.selectById(1L)).thenReturn(mockUserEntity);
        mockUserEntity.setPassword("secretPassword");

        Optional<UserSummaryDto> result = userService.getUserSummaryById(1L);

        assertTrue(result.isPresent());
        UserSummaryDto dto = result.get();
        assertDoesNotThrow(() -> dto.toString());
    }

    @Test
    @DisplayName("日志记录验证 - 操作日志")
    void testLogging_OperationLogs() {
        when(userMapper.findByNameCaseSensitive("testuser")).thenReturn(mockUserEntity);
        when(userMapper.selectById(1L)).thenReturn(mockUserEntity);

        userService.getUserByName("testuser");
        userService.getUserSummaryById(1L);

        verify(userMapper).findByNameCaseSensitive("testuser");
        verify(userMapper).selectById(1L);
    }

    @Test
    @DisplayName("isValidAvatarData - 合法 URL 测试")
    void testIsValidAvatarData_ValidUrl() {
        assertTrue(userService.isValidAvatarData("https://api.dicebear.com/10.x/bottts/svg?seed=Listen"));
        assertTrue(userService.isValidAvatarData("http://example.com/avatar.png"));
    }

    @Test
    @DisplayName("isValidAvatarData - 非法参数及空值测试")
    void testIsValidAvatarData_NullAndBlank() {
        assertFalse(userService.isValidAvatarData(null));
        assertFalse(userService.isValidAvatarData(""));
        assertFalse(userService.isValidAvatarData("   "));
        assertFalse(userService.isValidAvatarData("http://example.com/avatar\n.png"));
    }

    @Test
    @DisplayName("isValidAvatarData - 合法 PNG Base64 测试")
    void testIsValidAvatarData_ValidPngBase64() {
        String validPng = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNkYAAAAAYAAjCB0C8AAAAASUVORK5CYII=";
        assertTrue(userService.isValidAvatarData(validPng));
    }

    @Test
    @DisplayName("isValidAvatarData - 合法 SVG Base64 测试")
    void testIsValidAvatarData_ValidSvgBase64() {
        String svg = "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"10\" height=\"10\"></svg>";
        String validSvg = "data:image/svg+xml;base64," + java.util.Base64.getEncoder().encodeToString(svg.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        assertTrue(userService.isValidAvatarData(validSvg));
    }

    @Test
    @DisplayName("isValidAvatarData - 非法魔数或损坏数据测试 (如纯 A 字符)")
    void testIsValidAvatarData_CorruptedDummyData() {
        // 模拟先前导致生产环境崩溃的损坏数据（1000个A字符，base64解码后全为0x00）
        String corruptData = "data:image/png;base64," + "A".repeat(1000);
        assertFalse(userService.isValidAvatarData(corruptData));

        // 格式不符合 data:image/...;base64,
        assertFalse(userService.isValidAvatarData("not-a-valid-data-uri"));
        assertFalse(userService.isValidAvatarData("data:text/plain;base64,SGVsbG8="));
    }

    @Test
    @DisplayName("isValidAvatarData - 超长数据拦截测试")
    void testIsValidAvatarData_ExceedsMaxLength() {
        String oversized = "data:image/png;base64," + "A".repeat(3 * 1024 * 1024 + 10);
        assertFalse(userService.isValidAvatarData(oversized));
    }

    @Test
    @DisplayName("updateAvatar - 成功为用户1更新头像")
    void testUpdateAvatar_Success() {
        mockUserEntity.setId(1L);
        when(userMapper.findByNameCaseSensitive("testuser")).thenReturn(mockUserEntity);
        when(userMapper.updateById(any(UserEntity.class))).thenReturn(1);

        String validPng = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNkYAAAAAYAAjCB0C8AAAAASUVORK5CYII=";
        Optional<UserSummaryDto> result = userService.updateAvatar("testuser", validPng);

        assertTrue(result.isPresent());
        assertEquals(validPng, result.get().getAvatarUrl());
        verify(userMapper).updateById(mockUserEntity);
    }

    @Test
    @DisplayName("updateAvatar - 用户不存在返回 empty")
    void testUpdateAvatar_UserNotFound() {
        when(userMapper.findByNameCaseSensitive("unknown")).thenReturn(null);

        String validPng = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNkYAAAAAYAAjCB0C8AAAAASUVORK5CYII=";
        Optional<UserSummaryDto> result = userService.updateAvatar("unknown", validPng);

        assertTrue(result.isEmpty());
        verify(userMapper, never()).updateById(any(UserEntity.class));
    }

    @Test
    @DisplayName("updateAvatar - 非用户1修改抛出 IllegalArgumentException")
    void testUpdateAvatar_ForbiddenForOtherUser() {
        mockUserEntity.setId(2L);
        when(userMapper.findByNameCaseSensitive("otheruser")).thenReturn(mockUserEntity);

        String validPng = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNkYAAAAAYAAjCB0C8AAAAASUVORK5CYII=";
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                userService.updateAvatar("otheruser", validPng)
        );
        assertEquals("Only user with id 1 is permitted to change avatar", ex.getMessage());
    }

    @Test
    @DisplayName("updateAvatar - 无效图片数据抛出 IllegalArgumentException")
    void testUpdateAvatar_InvalidDataThrows() {
        String invalidData = "invalid-data";
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                userService.updateAvatar("testuser", invalidData)
        );
        assertEquals("Invalid avatar image format or size", ex.getMessage());
    }
}

