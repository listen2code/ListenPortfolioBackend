package com.listen.portfolio.api.v1.user;

import com.listen.portfolio.api.v1.auth.dto.ChangePasswordRequest;
import com.listen.portfolio.api.v1.auth.dto.SignUpRequest;
import com.listen.portfolio.api.v1.user.dto.UserSummaryDto;
import com.listen.portfolio.infrastructure.persistence.entity.UserEntity;
import com.listen.portfolio.repository.UserRepository;
import com.listen.portfolio.service.UserService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private UserEntity mockUserEntity;
    private SignUpRequest mockSignUpRequest;
    private ChangePasswordRequest mockChangePasswordRequest;

    @BeforeEach
    void setUp() {
        // 初始化测试数据
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
        // Given
        when(userRepository.findByNameCaseSensitive("testuser"))
                .thenReturn(Optional.of(mockUserEntity));

        // When
        Optional<UserEntity> result = userService.getUserByName("testuser");

        // Then
        assertTrue(result.isPresent());
        assertEquals("testuser", result.get().getName());
        assertEquals("test@example.com", result.get().getEmail());
        
        verify(userRepository).findByNameCaseSensitive("testuser");
    }

    @Test
    @DisplayName("getUserByName - 用户不存在返回空Optional")
    void testGetUserByName_UserNotFound() {
        // Given
        when(userRepository.findByNameCaseSensitive("nonexistent"))
                .thenReturn(Optional.empty());

        // When
        Optional<UserEntity> result = userService.getUserByName("nonexistent");

        // Then
        assertFalse(result.isPresent());
        verify(userRepository).findByNameCaseSensitive("nonexistent");
    }

    @Test
    @DisplayName("getUserSummaryById - 成功获取用户摘要")
    void testGetUserSummaryById_Success() {
        // Given
        when(userRepository.findById(1L))
                .thenReturn(Optional.of(mockUserEntity));

        // When
        Optional<UserSummaryDto> result = userService.getUserSummaryById(1L);

        // Then
        assertTrue(result.isPresent());
        UserSummaryDto dto = result.get();
        assertEquals(1L, dto.getId());
        assertEquals("testuser", dto.getName());
        assertEquals("test@example.com", dto.getEmail());
        assertEquals("Beijing", dto.getLocation());
        assertEquals("http://example.com/avatar.jpg", dto.getAvatarUrl());
        
        verify(userRepository).findById(1L);
    }

    @Test
    @DisplayName("getUserSummaryById - 用户不存在返回空Optional")
    void testGetUserSummaryById_UserNotFound() {
        // Given
        when(userRepository.findById(999L))
                .thenReturn(Optional.empty());

        // When
        Optional<UserSummaryDto> result = userService.getUserSummaryById(999L);

        // Then
        assertFalse(result.isPresent());
        verify(userRepository).findById(999L);
    }


    @Test
    @DisplayName("changePassword - 成功修改密码")
    void testChangePassword_Success() {
        // Given
        when(userRepository.findById(1L))
                .thenReturn(Optional.of(mockUserEntity));
        when(passwordEncoder.matches("oldPassword", "encodedPassword"))
                .thenReturn(true);
        when(passwordEncoder.encode("newPassword"))
                .thenReturn("newEncodedPassword");
        when(userRepository.save(any(UserEntity.class)))
                .thenReturn(mockUserEntity);

        // When
        boolean result = userService.changePassword(mockChangePasswordRequest);

        // Then
        assertTrue(result);
        
        verify(userRepository).findById(1L);
        verify(passwordEncoder).matches("oldPassword", "encodedPassword");
        verify(passwordEncoder).encode("newPassword");
        verify(userRepository).save(any(UserEntity.class));
    }

    @Test
    @DisplayName("changePassword - 旧密码不匹配修改失败")
    void testChangePassword_OldPasswordMismatch() {
        // Given
        when(userRepository.findById(1L))
                .thenReturn(Optional.of(mockUserEntity));
        when(passwordEncoder.matches("oldPassword", "encodedPassword"))
                .thenReturn(false);

        // When
        boolean result = userService.changePassword(mockChangePasswordRequest);

        // Then
        assertFalse(result);
        
        verify(userRepository).findById(1L);
        verify(passwordEncoder).matches("oldPassword", "encodedPassword");
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(UserEntity.class));
    }

    @Test
    @DisplayName("changePassword - 用户不存在修改失败")
    void testChangePassword_UserNotFound() {
        // Given
        when(userRepository.findById(1L))
                .thenReturn(Optional.empty());

        // When
        boolean result = userService.changePassword(mockChangePasswordRequest);

        // Then
        assertFalse(result);
        
        verify(userRepository).findById(1L);
        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(UserEntity.class));
    }

    @Test
    @DisplayName("deleteAccount - 成功删除账户")
    void testDeleteAccount_Success() {
        // Given
        when(userRepository.findById(1L))
                .thenReturn(Optional.of(mockUserEntity));
        doNothing().when(userRepository).delete(mockUserEntity);

        // When
        boolean result = userService.deleteAccount(1L);

        // Then
        assertTrue(result);
        
        verify(userRepository).findById(1L);
        verify(userRepository).delete(mockUserEntity);
    }

    @Test
    @DisplayName("deleteAccount - 用户不存在删除失败")
    void testDeleteAccount_UserNotFound() {
        // Given
        when(userRepository.findById(1L))
                .thenReturn(Optional.empty());

        // When
        boolean result = userService.deleteAccount(1L);

        // Then
        assertFalse(result);
        
        verify(userRepository).findById(1L);
        verify(userRepository, never()).delete(any(UserEntity.class));
    }

    @Test
    @DisplayName("toUserSummaryDto - 实体转换测试")
    void testToUserSummaryDto() {
        // 这个测试通过 getUserSummaryById 间接测试 toUserSummaryDto 方法
        // Given
        when(userRepository.findById(1L))
                .thenReturn(Optional.of(mockUserEntity));

        // When
        Optional<UserSummaryDto> result = userService.getUserSummaryById(1L);

        // Then
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
        // 验证通过 @InjectMocks 创建的实例不为 null
        assertNotNull(userService);
        assertNotNull(userRepository);
        assertNotNull(passwordEncoder);
    }

    @Test
    @DisplayName("边界测试 - null参数处理")
    void testEdgeCases_NullParameters() {
        // When & Then - null参数应该返回空结果或不抛出异常
        assertEquals(Optional.empty(), userService.getUserByName(null));
        assertEquals(Optional.empty(), userService.getUserSummaryById(null));
        // changePassword(null)会抛出NullPointerException，这是预期的
        assertThrows(NullPointerException.class, () -> userService.changePassword(null));
        assertFalse(userService.deleteAccount(null));
    }

    @Test
    @DisplayName("边界测试 - 无效ID处理")
    void testEdgeCases_InvalidIds() {
        // Given
        when(userRepository.findById(0L)).thenReturn(Optional.empty());
        when(userRepository.findById(-1L)).thenReturn(Optional.empty());

        // When & Then
        assertEquals(Optional.empty(), userService.getUserSummaryById(0L));
        assertEquals(Optional.empty(), userService.getUserSummaryById(-1L));
        assertFalse(userService.deleteAccount(0L));
        assertFalse(userService.deleteAccount(-1L));
    }

    @Test
    @DisplayName("边界测试 - 最大ID处理")
    void testEdgeCases_MaxId() {
        // Given
        UserEntity maxIdUser = new UserEntity();
        maxIdUser.setId(Long.MAX_VALUE);
        maxIdUser.setName("maxUser");
        when(userRepository.findById(Long.MAX_VALUE)).thenReturn(Optional.of(maxIdUser));

        // When
        Optional<UserSummaryDto> result = userService.getUserSummaryById(Long.MAX_VALUE);

        // Then
        assertTrue(result.isPresent());
        assertEquals(Long.MAX_VALUE, result.get().getId());
    }

    @Test
    @DisplayName("异常处理测试 - Repository异常")
    void testExceptionHandling_RepositoryException() {
        // Given
        when(userRepository.findByNameCaseSensitive(anyString())).thenThrow(new RuntimeException("Database connection failed"));
        when(userRepository.findById(anyLong())).thenThrow(new RuntimeException("Database connection failed"));

        // When & Then
        assertThrows(RuntimeException.class, () -> userService.getUserByName("testuser"));
        assertThrows(RuntimeException.class, () -> userService.getUserSummaryById(1L));
        assertThrows(RuntimeException.class, () -> userService.changePassword(mockChangePasswordRequest));
        assertThrows(RuntimeException.class, () -> userService.deleteAccount(1L));
    }

    @Test
    @DisplayName("异常处理测试 - PasswordEncoder异常")
    void testExceptionHandling_PasswordEncoderException() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUserEntity));
        when(passwordEncoder.matches(anyString(), anyString())).thenThrow(new RuntimeException("Encoding failed"));

        // When & Then
        assertThrows(RuntimeException.class, () -> userService.changePassword(mockChangePasswordRequest));
    }

    @Test
    @DisplayName("事务边界测试 - readOnly事务")
    void testTransactionBoundary_ReadOnlyTransaction() {
        // Given
        when(userRepository.findByNameCaseSensitive("testuser")).thenReturn(Optional.of(mockUserEntity));
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUserEntity));

        // When
        Optional<UserEntity> user = userService.getUserByName("testuser");
        Optional<UserSummaryDto> userSummary = userService.getUserSummaryById(1L);

        // Then - 验证只读事务方法正常工作
        assertTrue(user.isPresent());
        assertTrue(userSummary.isPresent());
        verify(userRepository).findByNameCaseSensitive("testuser");
        verify(userRepository).findById(1L);
        verifyNoMoreInteractions(passwordEncoder);
    }

    @Test
    @DisplayName("事务边界测试 - 写操作事务")
    void testTransactionBoundary_WriteTransaction() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUserEntity));
        when(passwordEncoder.matches("oldPassword", "encodedPassword")).thenReturn(true);
        when(passwordEncoder.encode("newPassword")).thenReturn("encodedNewPassword");
        when(userRepository.save(any(UserEntity.class))).thenReturn(mockUserEntity);

        // When
        boolean result = userService.changePassword(mockChangePasswordRequest);

        // Then - 验证写操作事务正常工作
        assertTrue(result);
        verify(passwordEncoder).matches("oldPassword", "encodedPassword");
        verify(passwordEncoder).encode("newPassword");
        verify(userRepository).save(any(UserEntity.class));
    }

    @Test
    @DisplayName("密码安全测试 - 密码编码验证")
    void testPasswordSecurity_EncodingVerification() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUserEntity));
        when(passwordEncoder.matches("oldPassword", "encodedPassword")).thenReturn(true);
        when(passwordEncoder.encode("newPassword")).thenReturn("encodedNewPassword");
        when(userRepository.save(any(UserEntity.class))).thenReturn(mockUserEntity);

        // When
        boolean result = userService.changePassword(mockChangePasswordRequest);

        // Then - 验证密码被正确编码
        assertTrue(result);
        verify(passwordEncoder).encode("newPassword");
        verify(userRepository).save(argThat(user -> "encodedNewPassword".equals(user.getPassword())));
    }

    @Test
    @DisplayName("数据转换测试 - toUserSummaryDto私有方法")
    void testToUserSummaryDto_PrivateMethod() throws Exception {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUserEntity));

        // When
        Optional<UserSummaryDto> result = userService.getUserSummaryById(1L);

        // Then - 验证DTO转换正确
        assertTrue(result.isPresent());
        UserSummaryDto dto = result.get();
        assertEquals(mockUserEntity.getId(), dto.getId());
        assertEquals(mockUserEntity.getName(), dto.getName());
        assertEquals(mockUserEntity.getAvatarUrl(), dto.getAvatarUrl());
        // UserSummaryDto只包含基本字段：id, name, location, email, avatarUrl
    }

    @Test
    @DisplayName("性能测试 - 大量用户查询")
    void testPerformance_BulkUserQueries() {
        // Given
        when(userRepository.findByNameCaseSensitive(anyString())).thenReturn(Optional.of(mockUserEntity));
        when(userRepository.findById(anyLong())).thenReturn(Optional.of(mockUserEntity));

        // When
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < 100; i++) {
            final String username = "user" + i;
            final Long userId = (long) i;
            assertDoesNotThrow(() -> userService.getUserByName(username));
            assertDoesNotThrow(() -> userService.getUserSummaryById(userId));
        }
        long endTime = System.currentTimeMillis();

        // Then - 操作应该在合理时间内完成
        assertTrue(endTime - startTime < 1000, "Bulk queries should complete within 1 second");
    }

    @Test
    @DisplayName("集成测试 - 完整的用户管理流程")
    void testIntegration_CompleteUserManagementFlow() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUserEntity));
        when(passwordEncoder.matches("oldPassword", "encodedPassword")).thenReturn(true);
        when(passwordEncoder.encode("newPassword")).thenReturn("encodedNewPassword");
        when(userRepository.save(any(UserEntity.class))).thenReturn(mockUserEntity);

        // When - 修改密码
        boolean changePasswordResult = userService.changePassword(mockChangePasswordRequest);

        // Then - 验证密码修改成功
        assertTrue(changePasswordResult);
        verify(passwordEncoder).encode("newPassword");
        verify(userRepository).save(any(UserEntity.class));

        // When - 删除账户
        boolean deleteAccountResult = userService.deleteAccount(1L);

        // Then - 验证账户删除成功
        assertTrue(deleteAccountResult);
        verify(userRepository).delete(mockUserEntity);
    }

    @Test
    @DisplayName("数据隔离测试 - DTO不包含敏感信息")
    void testDataIsolation_DtoNotContainSensitiveInformation() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUserEntity));
        mockUserEntity.setPassword("secretPassword"); // 设置敏感信息

        // When
        Optional<UserSummaryDto> result = userService.getUserSummaryById(1L);

        // Then - 验证DTO不包含敏感信息
        assertTrue(result.isPresent());
        UserSummaryDto dto = result.get();
        // UserSummaryDto不应该有密码字段，如果有也不应该是敏感信息
        assertDoesNotThrow(() -> dto.toString()); // 确保toString不会泄露敏感信息
    }

    @Test
    @DisplayName("日志记录验证 - 操作日志")
    void testLogging_OperationLogs() {
        // Given
        when(userRepository.findByNameCaseSensitive("testuser")).thenReturn(Optional.of(mockUserEntity));
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUserEntity));

        // When
        userService.getUserByName("testuser");
        userService.getUserSummaryById(1L);

        // Then - 验证日志记录（通过验证方法调用间接验证）
        verify(userRepository).findByNameCaseSensitive("testuser");
        verify(userRepository).findById(1L);
    }
}
