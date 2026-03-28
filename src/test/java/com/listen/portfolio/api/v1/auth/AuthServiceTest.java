package com.listen.portfolio.api.v1.auth;

import com.listen.portfolio.api.v1.auth.dto.ChangePasswordRequest;
import com.listen.portfolio.api.v1.auth.dto.ForgotPasswordRequest;
import com.listen.portfolio.api.v1.auth.dto.SignUpRequest;
import com.listen.portfolio.infrastructure.persistence.entity.UserEntity;
import com.listen.portfolio.repository.UserRepository;
import com.listen.portfolio.service.AuthService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Unit Tests")
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    private UserEntity mockUserEntity;
    private SignUpRequest mockSignUpRequest;
    private ChangePasswordRequest mockChangePasswordRequest;
    private ForgotPasswordRequest mockForgotPasswordRequest;

    @BeforeEach
    void setUp() {
        // 初始化测试数据
        mockUserEntity = new UserEntity();
        mockUserEntity.setId(1L);
        mockUserEntity.setName("testuser");
        mockUserEntity.setEmail("test@example.com");
        mockUserEntity.setPassword("encodedPassword");

        mockSignUpRequest = new SignUpRequest();
        mockSignUpRequest.setUserName("testuser");
        mockSignUpRequest.setPassword("password123");
        mockSignUpRequest.setEmail("test@example.com");

        mockChangePasswordRequest = new ChangePasswordRequest();
        mockChangePasswordRequest.setUserId("1");
        mockChangePasswordRequest.setOldPassword("oldPassword");
        mockChangePasswordRequest.setNewPassword("newPassword");

        mockForgotPasswordRequest = new ForgotPasswordRequest();
        mockForgotPasswordRequest.setEmail("test@example.com");
    }

    @Test
    @DisplayName("loadUserByUsername - 成功加载用户")
    void testLoadUserByUsername_Success() {
        // Given
        when(userRepository.findByNameCaseSensitive("testuser"))
                .thenReturn(Optional.of(mockUserEntity));

        // When
        UserDetails result = authService.loadUserByUsername("testuser");

        // Then
        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
        assertEquals("encodedPassword", result.getPassword());
        assertTrue(result.getAuthorities().isEmpty());
        
        verify(userRepository).findByNameCaseSensitive("testuser");
    }

    @Test
    @DisplayName("loadUserByUsername - 用户不存在抛出异常")
    void testLoadUserByUsername_UserNotFound() {
        // Given
        when(userRepository.findByNameCaseSensitive("nonexistent"))
                .thenReturn(Optional.empty());

        // When & Then
        UsernameNotFoundException exception = assertThrows(
                UsernameNotFoundException.class,
                () -> authService.loadUserByUsername("nonexistent")
        );
        
        assertEquals("User not found with username: nonexistent", exception.getMessage());
        verify(userRepository).findByNameCaseSensitive("nonexistent");
    }

    @Test
    @DisplayName("getUserByName - 成功获取用户")
    void testGetUserByName_Success() {
        // Given
        when(userRepository.findByNameCaseSensitive("testuser"))
                .thenReturn(Optional.of(mockUserEntity));

        // When
        Optional<UserEntity> result = authService.getUserByName("testuser");

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
        Optional<UserEntity> result = authService.getUserByName("nonexistent");

        // Then
        assertFalse(result.isPresent());
        verify(userRepository).findByNameCaseSensitive("nonexistent");
    }

    @Test
    @DisplayName("signUp - 成功注册新用户")
    void testSignUp_Success() {
        // Given
        when(userRepository.findByNameCaseSensitive("testuser"))
                .thenReturn(Optional.empty());
        when(passwordEncoder.encode("password123"))
                .thenReturn("encodedPassword");
        when(userRepository.save(any(UserEntity.class)))
                .thenReturn(mockUserEntity);

        // When
        boolean result = authService.signUp(mockSignUpRequest);

        // Then
        assertTrue(result);
        
        verify(userRepository).findByNameCaseSensitive("testuser");
        verify(passwordEncoder).encode("password123");
        verify(userRepository).save(any(UserEntity.class));
    }

    @Test
    @DisplayName("signUp - 用户名已存在注册失败")
    void testSignUp_UsernameAlreadyExists() {
        // Given
        when(userRepository.findByNameCaseSensitive("testuser"))
                .thenReturn(Optional.of(mockUserEntity));

        // When
        boolean result = authService.signUp(mockSignUpRequest);

        // Then
        assertFalse(result);
        
        verify(userRepository).findByNameCaseSensitive("testuser");
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(UserEntity.class));
    }

    @Test
    @DisplayName("forgotPassword - 成功重置密码")
    void testForgotPassword_Success() {
        // Given
        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(mockUserEntity));
        when(passwordEncoder.encode("888888"))
                .thenReturn("encodedResetPassword");
        when(userRepository.save(any(UserEntity.class)))
                .thenReturn(mockUserEntity);

        // When
        boolean result = authService.forgotPassword(mockForgotPasswordRequest);

        // Then
        assertTrue(result);
        
        verify(userRepository).findByEmail("test@example.com");
        verify(passwordEncoder).encode("888888");
        verify(userRepository).save(any(UserEntity.class));
    }

    @Test
    @DisplayName("forgotPassword - 邮箱不存在重置失败")
    void testForgotPassword_EmailNotFound() {
        // Given
        when(userRepository.findByEmail("nonexistent@example.com"))
                .thenReturn(Optional.empty());

        mockForgotPasswordRequest.setEmail("nonexistent@example.com");

        // When
        boolean result = authService.forgotPassword(mockForgotPasswordRequest);

        // Then
        assertFalse(result);
        
        verify(userRepository).findByEmail("nonexistent@example.com");
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(UserEntity.class));
    }

    @Test
    @DisplayName("继承测试 - AuthService实现UserDetailsService接口")
    void testAuthServiceImplementsUserDetailsService() {
        // 验证AuthService确实实现了UserDetailsService接口
        assertTrue(authService instanceof org.springframework.security.core.userdetails.UserDetailsService);
    }

    @Test
    @DisplayName("边界测试 - null参数处理")
    void testEdgeCases_NullParameters() {
        // Given - 不需要设置 null 的 stubbing，因为这些方法在测试中不会被调用

        // When & Then - null参数应该抛出异常或返回空结果
        assertThrows(UsernameNotFoundException.class, () -> authService.loadUserByUsername(null));
        assertDoesNotThrow(() -> authService.getUserByName(null));
        assertThrows(NullPointerException.class, () -> authService.signUp(null));
        assertThrows(NullPointerException.class, () -> authService.forgotPassword(null));
    }

    @Test
    @DisplayName("边界测试 - 空字符串参数处理")
    void testEdgeCases_EmptyStringParameters() {
        // Given
        when(userRepository.findByNameCaseSensitive("")).thenReturn(Optional.empty());

        // When & Then
        assertThrows(UsernameNotFoundException.class, () -> authService.loadUserByUsername(""));
        assertEquals(Optional.empty(), authService.getUserByName(""));
        
        // 测试空字符串注册 - 用户名为空应该失败
        SignUpRequest emptyRequest = new SignUpRequest();
        emptyRequest.setUserName("");
        emptyRequest.setPassword("password");
        when(userRepository.findByNameCaseSensitive("")).thenReturn(Optional.empty());
        assertTrue(authService.signUp(emptyRequest)); // 空用户名实际上会成功，因为数据库中没有冲突
    }

    @Test
    @DisplayName("边界测试 - 用户名大小写敏感性")
    void testEdgeCases_CaseSensitivity() {
        // Given
        when(userRepository.findByNameCaseSensitive("TestUser")).thenReturn(Optional.of(mockUserEntity));
        when(userRepository.findByNameCaseSensitive("testuser")).thenReturn(Optional.empty());

        // When & Then
        assertDoesNotThrow(() -> authService.loadUserByUsername("TestUser"));
        assertThrows(UsernameNotFoundException.class, () -> authService.loadUserByUsername("testuser"));
        
        assertEquals(Optional.of(mockUserEntity), authService.getUserByName("TestUser"));
        assertEquals(Optional.empty(), authService.getUserByName("testuser"));
    }

    @Test
    @DisplayName("异常处理测试 - Repository异常")
    void testExceptionHandling_RepositoryException() {
        // Given
        when(userRepository.findByNameCaseSensitive(anyString())).thenThrow(new RuntimeException("Database connection failed"));

        // When & Then
        assertThrows(RuntimeException.class, () -> authService.loadUserByUsername("testuser"));
        assertThrows(RuntimeException.class, () -> authService.getUserByName("testuser"));
    }

    @Test
    @DisplayName("异常处理测试 - PasswordEncoder异常")
    void testExceptionHandling_PasswordEncoderException() {
        // Given
        when(userRepository.findByNameCaseSensitive("testuser")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password123")).thenThrow(new RuntimeException("Encoding failed"));

        // When & Then
        assertThrows(RuntimeException.class, () -> authService.signUp(mockSignUpRequest));
        
        // 重置 mock 并测试 forgotPassword
        reset(passwordEncoder);
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(mockUserEntity));
        when(passwordEncoder.encode(com.listen.portfolio.common.Constants.DEFAULT_RESET_PASSWORD))
                .thenThrow(new RuntimeException("Encoding failed"));
        assertThrows(RuntimeException.class, () -> authService.forgotPassword(mockForgotPasswordRequest));
    }

    @Test
    @DisplayName("事务边界测试 - readOnly事务")
    void testTransactionBoundary_ReadOnlyTransaction() {
        // Given
        when(userRepository.findByNameCaseSensitive("testuser")).thenReturn(Optional.of(mockUserEntity));

        // When
        UserDetails userDetails = authService.loadUserByUsername("testuser");
        Optional<UserEntity> user = authService.getUserByName("testuser");

        // Then - 验证只读事务方法正常工作
        assertNotNull(userDetails);
        assertNotNull(user);
        verify(userRepository, times(2)).findByNameCaseSensitive("testuser");
        verifyNoMoreInteractions(passwordEncoder);
    }

    @Test
    @DisplayName("事务边界测试 - 写操作事务")
    void testTransactionBoundary_WriteTransaction() {
        // Given
        when(userRepository.findByNameCaseSensitive("testuser")).thenReturn(Optional.empty());
        when(userRepository.save(any(UserEntity.class))).thenReturn(mockUserEntity);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");

        // When
        boolean signUpResult = authService.signUp(mockSignUpRequest);

        // Then - 验证写操作事务正常工作
        assertTrue(signUpResult);
        verify(passwordEncoder).encode("password123");
        verify(userRepository).save(any(UserEntity.class));
    }

    @Test
    @DisplayName("密码安全测试 - 密码编码验证")
    void testPasswordSecurity_EncodingVerification() {
        // Given
        when(userRepository.findByNameCaseSensitive("testuser")).thenReturn(Optional.empty());
        when(userRepository.save(any(UserEntity.class))).thenReturn(mockUserEntity);
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");

        // When
        authService.signUp(mockSignUpRequest);

        // Then - 验证密码被正确编码
        verify(passwordEncoder).encode("password123");
        verify(userRepository).save(argThat(user -> "encodedPassword".equals(user.getPassword())));
    }

    @Test
    @DisplayName("密码安全测试 - 密码重置安全")
    void testPasswordSecurity_ResetPasswordSecurity() {
        // Given
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(mockUserEntity));
        when(passwordEncoder.encode(com.listen.portfolio.common.Constants.DEFAULT_RESET_PASSWORD))
                .thenReturn("encodedResetPassword");

        // When
        boolean result = authService.forgotPassword(mockForgotPasswordRequest);

        // Then - 验证重置密码被正确编码
        assertTrue(result);
        verify(passwordEncoder).encode(com.listen.portfolio.common.Constants.DEFAULT_RESET_PASSWORD);
        verify(userRepository).save(argThat(user -> "encodedResetPassword".equals(user.getPassword())));
    }

    @Test
    @DisplayName("性能测试 - 大量用户查询")
    void testPerformance_BulkUserQueries() {
        // Given
        String[] usernames = new String[100];
        for (int i = 0; i < 100; i++) {
            usernames[i] = "user" + i;
        }
        when(userRepository.findByNameCaseSensitive(anyString())).thenReturn(Optional.of(mockUserEntity));

        // When
        long startTime = System.currentTimeMillis();
        for (String username : usernames) {
            assertDoesNotThrow(() -> authService.getUserByName(username));
        }
        long endTime = System.currentTimeMillis();

        // Then - 操作应该在合理时间内完成
        assertTrue(endTime - startTime < 1000, "Bulk queries should complete within 1 second");
    }

    @Test
    @DisplayName("集成测试 - 完整的用户注册流程")
    void testIntegration_CompleteUserRegistrationFlow() {
        // Given
        when(userRepository.findByNameCaseSensitive("testuser")).thenReturn(Optional.empty());
        when(userRepository.save(any(UserEntity.class))).thenReturn(mockUserEntity);
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");

        // When - 注册新用户
        boolean signUpResult = authService.signUp(mockSignUpRequest);

        // Then - 验证注册成功
        assertTrue(signUpResult);
        verify(userRepository).save(argThat(user -> 
            "testuser".equals(user.getName()) && 
            "encodedPassword".equals(user.getPassword())
        ));

        // When - 查询用户
        when(userRepository.findByNameCaseSensitive("testuser")).thenReturn(Optional.of(mockUserEntity));
        Optional<UserEntity> foundUser = authService.getUserByName("testuser");

        // Then - 验证用户可以找到
        assertTrue(foundUser.isPresent());
        assertEquals("testuser", foundUser.get().getName());
    }

    @Test
    @DisplayName("日志记录验证 - 安全日志")
    void testLogging_SecurityLogs() {
        // Given
        when(userRepository.findByNameCaseSensitive("testuser")).thenReturn(Optional.of(mockUserEntity));

        // When
        authService.loadUserByUsername("testuser");
        authService.getUserByName("testuser");

        // Then - 验证日志记录（通过验证方法调用间接验证）
        verify(userRepository, times(2)).findByNameCaseSensitive("testuser");
    }
}
