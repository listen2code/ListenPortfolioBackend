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
        boolean result = authService.changePassword(mockChangePasswordRequest);

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
        boolean result = authService.changePassword(mockChangePasswordRequest);

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
        boolean result = authService.changePassword(mockChangePasswordRequest);

        // Then
        assertFalse(result);
        
        verify(userRepository).findById(1L);
        verify(passwordEncoder, never()).matches(anyString(), anyString());
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
    @DisplayName("deleteAccount - 成功删除账户")
    void testDeleteAccount_Success() {
        // Given
        when(userRepository.findById(1L))
                .thenReturn(Optional.of(mockUserEntity));
        doNothing().when(userRepository).delete(mockUserEntity);

        // When
        boolean result = authService.deleteAccount(1L);

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
        boolean result = authService.deleteAccount(1L);

        // Then
        assertFalse(result);
        
        verify(userRepository).findById(1L);
        verify(userRepository, never()).delete(any(UserEntity.class));
    }

    @Test
    @DisplayName("继承测试 - AuthService实现UserDetailsService接口")
    void testAuthServiceImplementsUserDetailsService() {
        // 验证AuthService确实实现了UserDetailsService接口
        assertTrue(authService instanceof org.springframework.security.core.userdetails.UserDetailsService);
    }
}
