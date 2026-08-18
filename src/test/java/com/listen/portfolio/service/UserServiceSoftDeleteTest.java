package com.listen.portfolio.service;

import com.listen.portfolio.entity.UserEntity;
import com.listen.portfolio.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Soft Delete Unit Tests")
class UserServiceSoftDeleteTest {

    @Mock
    private UserMapper userMapper;

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
        when(userMapper.selectById(2L)).thenReturn(testUser);
        when(userMapper.updateById(any(UserEntity.class))).thenReturn(1);

        boolean result = userService.deleteAccount(2L);

        assertTrue(result);
        
        ArgumentCaptor<UserEntity> captor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userMapper).updateById(captor.capture());
        
        UserEntity savedUser = captor.getValue();
        assertTrue(savedUser.isDeleted());
        assertTrue(savedUser.getName().startsWith("deleted_"));
        assertTrue(savedUser.getName().contains("testuser"));
        assertTrue(savedUser.getEmail().startsWith("deleted_"));
        assertTrue(savedUser.getEmail().contains("testuser@example.com"));
    }

    @Test
    @DisplayName("deleteAccount - 拦截对种子用户 (userId = 1) 的删除请求")
    void testDeleteAccount_SeedUserBlocked() {
        boolean result = userService.deleteAccount(1L);

        assertFalse(result);
        verifyNoInteractions(userMapper);
    }

    @Test
    @DisplayName("deleteAccount - 用户不存在时返回 false")
    void testDeleteAccount_UserNotFound() {
        when(userMapper.selectById(999L)).thenReturn(null);

        boolean result = userService.deleteAccount(999L);

        assertFalse(result);
        verify(userMapper, never()).updateById(any(UserEntity.class));
        verify(userMapper, never()).deleteById(any(Long.class));
    }
}
