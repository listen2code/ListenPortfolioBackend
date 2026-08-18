package com.listen.portfolio.api.v1.user;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.listen.portfolio.entity.UserEntity;
import com.listen.portfolio.mapper.UserMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserMapper Unit Tests")
class UserMapperTest {

    @Mock
    private UserMapper userMapper;

    private UserEntity testUser;

    @BeforeEach
    void setUp() {
        testUser = new UserEntity();
        testUser.setId(1L);
        testUser.setName("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword("encodedPassword");
    }

    @Test
    @DisplayName("findByNameCaseSensitive - 成功查找用户")
    void testFindByNameCaseSensitive_Success() {
        when(userMapper.findByNameCaseSensitive("testuser"))
                .thenReturn(testUser);

        UserEntity result = userMapper.findByNameCaseSensitive("testuser");

        assertNotNull(result);
        assertEquals("testuser", result.getName());
        verify(userMapper).findByNameCaseSensitive("testuser");
    }

    @Test
    @DisplayName("findByNameCaseSensitive - 用户不存在返回null")
    void testFindByNameCaseSensitive_NotFound() {
        when(userMapper.findByNameCaseSensitive("nonexistent"))
                .thenReturn(null);

        UserEntity result = userMapper.findByNameCaseSensitive("nonexistent");

        assertNull(result);
        verify(userMapper).findByNameCaseSensitive("nonexistent");
    }

    @Test
    @DisplayName("insert - 成功保存用户")
    void testInsert_Success() {
        when(userMapper.insert(any(UserEntity.class)))
                .thenReturn(1);

        int result = userMapper.insert(testUser);

        assertEquals(1, result);
        verify(userMapper).insert(testUser);
    }

    @Test
    @DisplayName("selectById - 成功查找用户")
    void testSelectById_Success() {
        when(userMapper.selectById(1L))
                .thenReturn(testUser);

        UserEntity result = userMapper.selectById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        verify(userMapper).selectById(1L);
    }

    @Test
    @DisplayName("selectById - 用户不存在返回null")
    void testSelectById_NotFound() {
        when(userMapper.selectById(999L))
                .thenReturn(null);

        UserEntity result = userMapper.selectById(999L);

        assertNull(result);
        verify(userMapper).selectById(999L);
    }

    @Test
    @DisplayName("deleteById - 成功删除用户")
    void testDeleteById_Success() {
        when(userMapper.deleteById(1L)).thenReturn(1);

        int result = userMapper.deleteById(1L);

        assertEquals(1, result);
        verify(userMapper).deleteById(1L);
    }

    @Test
    @DisplayName("selectList - 成功查找所有用户")
    void testSelectList_Success() {
        List<UserEntity> userList = Arrays.asList(testUser);
        when(userMapper.selectList(null)).thenReturn(userList);

        List<UserEntity> result = userMapper.selectList(null);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("testuser", result.get(0).getName());
        verify(userMapper).selectList(null);
    }

    @Test
    @DisplayName("继承关系验证 - UserMapper继承BaseMapper")
    void testMapperInheritance() {
        assertTrue(userMapper instanceof BaseMapper);
    }

    @Test
    @DisplayName("接口方法验证 - 确认自定义方法存在")
    void testCustomMethodsExist() {
        assertDoesNotThrow(() -> {
            userMapper.findByNameCaseSensitive("test");
            userMapper.findCertificationsByUserId(1L);
            userMapper.insertCertification(1L, "AWS Certified");
            userMapper.deleteCertificationsByUserId(1L);
        });
    }
}
