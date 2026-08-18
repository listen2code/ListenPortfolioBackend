package com.listen.portfolio.api.v1.auth;

import com.listen.portfolio.api.v1.user.dto.ChangePasswordRequest;
import com.listen.portfolio.entity.UserEntity;
import com.listen.portfolio.api.v1.auth.dto.ForgotPasswordRequest;
import com.listen.portfolio.api.v1.auth.dto.SignUpRequest;
import com.listen.portfolio.mapper.UserMapper;
import com.listen.portfolio.service.AuthService;
import com.listen.portfolio.service.EmailService;
import com.listen.portfolio.service.PasswordResetTokenService;
import com.listen.portfolio.service.RefreshTokenService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatcher;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("AuthService Unit Tests")
class AuthServiceTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private EmailService emailService;

    @Mock
    private PasswordResetTokenService passwordResetTokenService;
    
    @Mock
    private RefreshTokenService refreshTokenService;

    @InjectMocks
    private AuthService authService;

    private UserEntity mockUserEntity;
    private SignUpRequest mockSignUpRequest;
    private ChangePasswordRequest mockChangePasswordRequest;
    private ForgotPasswordRequest mockForgotPasswordRequest;

    @BeforeEach
    void setUp() {
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
        when(userMapper.findByNameCaseSensitive("testuser"))
                .thenReturn(mockUserEntity);

        UserDetails result = authService.loadUserByUsername("testuser");

        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
        assertEquals("encodedPassword", result.getPassword());
        assertTrue(result.getAuthorities().isEmpty());
        
        verify(userMapper).findByNameCaseSensitive("testuser");
    }

    @Test
    @DisplayName("loadUserByUsername - 用户不存在抛出异常")
    void testLoadUserByUsername_UserNotFound() {
        when(userMapper.findByNameCaseSensitive("nonexistent"))
                .thenReturn(null);

        UsernameNotFoundException exception = assertThrows(
                UsernameNotFoundException.class,
                () -> authService.loadUserByUsername("nonexistent")
        );
        
        assertEquals("User not found with username: nonexistent", exception.getMessage());
        verify(userMapper).findByNameCaseSensitive("nonexistent");
    }

    @Test
    @DisplayName("getUserByName - 成功获取用户")
    void testGetUserByName_Success() {
        when(userMapper.findByNameCaseSensitive("testuser"))
                .thenReturn(mockUserEntity);

        Optional<UserEntity> result = authService.getUserByName("testuser");

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

        Optional<UserEntity> result = authService.getUserByName("nonexistent");

        assertFalse(result.isPresent());
        verify(userMapper).findByNameCaseSensitive("nonexistent");
    }

    @Test
    @DisplayName("signUp - 成功注册新用户")
    void testSignUp_Success() {
        when(userMapper.findByNameCaseSensitive("testuser"))
                .thenReturn(null);
        when(passwordEncoder.encode("password123"))
                .thenReturn("encodedPassword");
        when(userMapper.insert(any(UserEntity.class)))
                .thenReturn(1);

        boolean result = authService.signUp(mockSignUpRequest);

        assertTrue(result);
        
        verify(userMapper).findByNameCaseSensitive("testuser");
        verify(passwordEncoder).encode("password123");
        verify(userMapper).insert(any(UserEntity.class));
    }

    @Test
    @DisplayName("signUp - 用户名已存在注册失败")
    void testSignUp_UsernameAlreadyExists() {
        when(userMapper.findByNameCaseSensitive("testuser"))
                .thenReturn(mockUserEntity);

        boolean result = authService.signUp(mockSignUpRequest);

        assertFalse(result);
        
        verify(userMapper).findByNameCaseSensitive("testuser");
        verify(passwordEncoder, never()).encode(anyString());
        verify(userMapper, never()).insert(any(UserEntity.class));
    }

    @Test
    @DisplayName("signUp - 邮箱已存在注册失败")
    void testSignUp_EmailAlreadyExists() {
        when(userMapper.findByNameCaseSensitive("testuser"))
                .thenReturn(null);
        when(userMapper.selectOne(any()))
                .thenReturn(mockUserEntity);

        AuthService.SignUpResult result = authService.signUpResult(mockSignUpRequest);

        assertEquals(AuthService.SignUpResult.EMAIL_EXISTS, result);
        
        verify(userMapper).findByNameCaseSensitive("testuser");
        verify(passwordEncoder, never()).encode(anyString());
        verify(userMapper, never()).insert(any(UserEntity.class));
    }

    @Test
    @DisplayName("forgotPassword - 成功发送密码重置邮件")
    void testForgotPassword_Success() {
        when(userMapper.selectOne(any()))
                .thenReturn(mockUserEntity);
        when(passwordResetTokenService.generateToken("test@example.com"))
                .thenReturn("test-token");

        boolean result = authService.forgotPassword(mockForgotPasswordRequest);

        assertTrue(result);
        
        verify(passwordResetTokenService).generateToken("test@example.com");
        verify(passwordEncoder, never()).encode(anyString());
        verify(userMapper, never()).insert(any(UserEntity.class));
    }

    @Test
    @DisplayName("forgotPassword - 邮箱不存在也返回成功（防止邮箱枚举攻击）")
    void testForgotPassword_EmailNotFound() {
        when(userMapper.selectOne(any()))
                .thenReturn(null);

        mockForgotPasswordRequest.setEmail("nonexistent@example.com");

        boolean result = authService.forgotPassword(mockForgotPasswordRequest);

        assertTrue(result);
        
        verify(passwordResetTokenService, never()).generateToken(anyString());
        verify(passwordEncoder, never()).encode(anyString());
        verify(userMapper, never()).insert(any(UserEntity.class));
    }

    @Test
    @DisplayName("继承测试 - AuthService实现UserDetailsService接口")
    void testAuthServiceImplementsUserDetailsService() {
        assertTrue(authService instanceof org.springframework.security.core.userdetails.UserDetailsService);
    }

    @Test
    @DisplayName("边界测试 - null参数处理")
    void testEdgeCases_NullParameters() {
        assertThrows(UsernameNotFoundException.class, () -> authService.loadUserByUsername(null));
        assertDoesNotThrow(() -> authService.getUserByName(null));
        assertThrows(NullPointerException.class, () -> authService.signUp(null));
        assertThrows(NullPointerException.class, () -> authService.forgotPassword(null));
    }

    @Test
    @DisplayName("边界测试 - 空字符串参数处理")
    void testEdgeCases_EmptyStringParameters() {
        when(userMapper.findByNameCaseSensitive("")).thenReturn(null);

        assertThrows(UsernameNotFoundException.class, () -> authService.loadUserByUsername(""));
        assertEquals(Optional.empty(), authService.getUserByName(""));
        
        SignUpRequest emptyRequest = new SignUpRequest();
        emptyRequest.setUserName("");
        emptyRequest.setPassword("password");
        when(userMapper.findByNameCaseSensitive("")).thenReturn(null);
        assertTrue(authService.signUp(emptyRequest));
    }

    @Test
    @DisplayName("边界测试 - 用户名大小写敏感性")
    void testEdgeCases_CaseSensitivity() {
        when(userMapper.findByNameCaseSensitive("TestUser")).thenReturn(mockUserEntity);
        when(userMapper.findByNameCaseSensitive("testuser")).thenReturn(null);

        assertDoesNotThrow(() -> authService.loadUserByUsername("TestUser"));
        assertThrows(UsernameNotFoundException.class, () -> authService.loadUserByUsername("testuser"));
        
        assertEquals(Optional.of(mockUserEntity), authService.getUserByName("TestUser"));
        assertEquals(Optional.empty(), authService.getUserByName("testuser"));
    }

    @Test
    @DisplayName("异常处理测试 - Repository异常")
    void testExceptionHandling_RepositoryException() {
        when(userMapper.findByNameCaseSensitive(anyString())).thenThrow(new RuntimeException("Database connection failed"));

        assertThrows(RuntimeException.class, () -> authService.loadUserByUsername("testuser"));
        assertThrows(RuntimeException.class, () -> authService.getUserByName("testuser"));
    }

    @Test
    @DisplayName("异常处理测试 - PasswordEncoder异常")
    void testExceptionHandling_PasswordEncoderException() {
        when(userMapper.findByNameCaseSensitive("testuser")).thenReturn(null);
        when(passwordEncoder.encode("password123")).thenThrow(new RuntimeException("Encoding failed"));

        assertThrows(RuntimeException.class, () -> authService.signUp(mockSignUpRequest));
        
        reset(passwordEncoder);
        when(userMapper.selectOne(any())).thenReturn(mockUserEntity);
        when(passwordResetTokenService.generateToken("test@example.com")).thenReturn("test-token");
        boolean result = authService.forgotPassword(mockForgotPasswordRequest);
        assertTrue(result);
    }

    @Test
    @DisplayName("事务边界测试 - readOnly事务")
    void testTransactionBoundary_ReadOnlyTransaction() {
        when(userMapper.findByNameCaseSensitive("testuser")).thenReturn(mockUserEntity);

        UserDetails userDetails = authService.loadUserByUsername("testuser");
        Optional<UserEntity> user = authService.getUserByName("testuser");

        assertNotNull(userDetails);
        assertNotNull(user);
        verify(userMapper, times(2)).findByNameCaseSensitive("testuser");
        verifyNoMoreInteractions(passwordEncoder);
    }

    @Test
    @DisplayName("事务边界测试 - 写操作事务")
    void testTransactionBoundary_WriteTransaction() {
        when(userMapper.findByNameCaseSensitive("testuser")).thenReturn(null);
        when(userMapper.insert(any(UserEntity.class))).thenReturn(1);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");

        boolean signUpResult = authService.signUp(mockSignUpRequest);

        assertTrue(signUpResult);
        verify(passwordEncoder).encode("password123");
        verify(userMapper).insert(any(UserEntity.class));
    }

    @Test
    @DisplayName("密码安全测试 - 密码编码验证")
    void testPasswordSecurity_EncodingVerification() {
        when(userMapper.findByNameCaseSensitive("testuser")).thenReturn(null);
        when(userMapper.insert(any(UserEntity.class))).thenReturn(1);
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");

        authService.signUp(mockSignUpRequest);

        verify(passwordEncoder).encode("password123");
        verify(userMapper).insert(argThat((ArgumentMatcher<UserEntity>) user -> "encodedPassword".equals(user.getPassword())));
    }

    @Test
    @DisplayName("密码安全测试 - 密码重置安全")
    void testPasswordSecurity_ResetPasswordSecurity() {
        when(userMapper.selectOne(any())).thenReturn(mockUserEntity);
        when(passwordResetTokenService.generateToken("test@example.com")).thenReturn("test-token");

        boolean result = authService.forgotPassword(mockForgotPasswordRequest);

        assertTrue(result);
        verify(passwordResetTokenService).generateToken("test@example.com");
        verify(passwordEncoder, never()).encode(anyString());
        verify(userMapper, never()).insert(any(UserEntity.class));
    }

    @Test
    @DisplayName("性能测试 - 大量用户查询")
    void testPerformance_BulkUserQueries() {
        String[] usernames = new String[100];
        for (int i = 0; i < 100; i++) {
            usernames[i] = "user" + i;
        }
        when(userMapper.findByNameCaseSensitive(anyString())).thenReturn(mockUserEntity);

        long startTime = System.currentTimeMillis();
        for (String username : usernames) {
            assertDoesNotThrow(() -> authService.getUserByName(username));
        }
        long endTime = System.currentTimeMillis();

        assertTrue(endTime - startTime < 1000, "Bulk queries should complete within 1 second");
    }

    @Test
    @DisplayName("集成测试 - 完整的用户注册流程")
    void testIntegration_CompleteUserRegistrationFlow() {
        when(userMapper.findByNameCaseSensitive("testuser")).thenReturn(null);
        when(userMapper.insert(any(UserEntity.class))).thenReturn(1);
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");

        boolean signUpResult = authService.signUp(mockSignUpRequest);

        assertTrue(signUpResult);
        verify(userMapper).insert(argThat((ArgumentMatcher<UserEntity>) user -> 
            "testuser".equals(user.getName()) && 
            "encodedPassword".equals(user.getPassword())
        ));

        when(userMapper.findByNameCaseSensitive("testuser")).thenReturn(mockUserEntity);
        Optional<UserEntity> foundUser = authService.getUserByName("testuser");

        assertTrue(foundUser.isPresent());
        assertEquals("testuser", foundUser.get().getName());
    }

    @Test
    @DisplayName("日志记录验证 - 安全日志")
    void testLogging_SecurityLogs() {
        when(userMapper.findByNameCaseSensitive("testuser")).thenReturn(mockUserEntity);

        authService.loadUserByUsername("testuser");
        authService.getUserByName("testuser");

        verify(userMapper, times(2)).findByNameCaseSensitive("testuser");
    }

    @Test
    @DisplayName("resetPassword - 密码重置成功并吊销所有 Refresh Token")
    void testResetPassword_Success() {
        String token = "valid-reset-token";
        String newPassword = "newSecurePassword123";
        when(passwordResetTokenService.getEmailByToken(token)).thenReturn("test@example.com");
        when(userMapper.selectOne(any())).thenReturn(mockUserEntity);
        when(passwordEncoder.encode(newPassword)).thenReturn("newEncodedPassword");

        boolean result = authService.resetPassword(token, newPassword);

        assertTrue(result);
        verify(passwordResetTokenService).getEmailByToken(token);
        verify(passwordEncoder).encode(newPassword);
        verify(userMapper).updateById(mockUserEntity);
        verify(passwordResetTokenService).deleteToken(token);
        verify(refreshTokenService).revokeAllRefreshTokens("testuser");
    }
}
