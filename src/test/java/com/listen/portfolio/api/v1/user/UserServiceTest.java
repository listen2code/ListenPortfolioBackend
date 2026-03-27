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

import java.util.Arrays;
import java.util.List;
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
}
