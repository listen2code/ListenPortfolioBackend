package com.listen.portfolio.service;

import com.listen.portfolio.entity.UserEntity;
import com.listen.portfolio.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Soft Delete Unit Tests")
class UserServiceSoftDeleteTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private UserEntity testUser;

    @BeforeEach
    void setUp() {
        testUser = new UserEntity();
        testUser.setId(2L);
        testUser.setName("testuser");
        testUser.setEmail("testuser@example.com");
        testUser.setDeleted(false);
    }

    @Test
    @DisplayName("deleteAccount - 成功软删除普通用户并修改用户名和邮箱以释放索引")
    void testDeleteAccount_SoftDelete_Success() {
        // Given
        when(userRepository.findById(2L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        boolean result = userService.deleteAccount(2L);

        // Then
        assertTrue(result);
        
        ArgumentCaptor<UserEntity> captor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).save(captor.capture());
        
        UserEntity savedUser = captor.getValue();
        assertTrue(savedUser.isDeleted());
        assertTrue(savedUser.getName().startsWith("deleted_"));
        assertTrue(savedUser.getName().contains("testuser"));
        assertTrue(savedUser.getEmail().startsWith("deleted_"));
        assertTrue(savedUser.getEmail().contains("testuser@example.com"));
        
        verify(userRepository, never()).delete(any());
    }

    @Test
    @DisplayName("deleteAccount - 拦截对种子用户 (userId = 1) 的删除请求")
    void testDeleteAccount_SeedUserBlocked() {
        // When
        boolean result = userService.deleteAccount(1L);

        // Then
        assertFalse(result);
        verifyNoInteractions(userRepository);
    }

    @Test
    @DisplayName("deleteAccount - 用户不存在时返回 false")
    void testDeleteAccount_UserNotFound() {
        // Given
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // When
        boolean result = userService.deleteAccount(999L);

        // Then
        assertFalse(result);
        verify(userRepository, never()).save(any());
        verify(userRepository, never()).delete(any());
    }
}
