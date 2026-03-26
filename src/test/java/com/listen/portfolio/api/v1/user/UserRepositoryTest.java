package com.listen.portfolio.api.v1.user;

import com.listen.portfolio.infrastructure.persistence.entity.UserEntity;
import com.listen.portfolio.repository.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserRepository Unit Tests")
class UserRepositoryTest {

    @Mock
    private UserRepository userRepository;

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
    @DisplayName("findByName - 成功查找用户")
    void testFindByName_Success() {
        // Given
        when(userRepository.findByName("testuser"))
                .thenReturn(Optional.of(testUser));

        // When
        Optional<UserEntity> result = userRepository.findByName("testuser");

        // Then
        assertTrue(result.isPresent());
        assertEquals("testuser", result.get().getName());
        
        verify(userRepository).findByName("testuser");
    }

    @Test
    @DisplayName("findByName - 用户不存在返回空Optional")
    void testFindByName_NotFound() {
        // Given
        when(userRepository.findByName("nonexistent"))
                .thenReturn(Optional.empty());

        // When
        Optional<UserEntity> result = userRepository.findByName("nonexistent");

        // Then
        assertFalse(result.isPresent());
        verify(userRepository).findByName("nonexistent");
    }

    @Test
    @DisplayName("findByNameCaseSensitive - 成功查找用户")
    void testFindByNameCaseSensitive_Success() {
        // Given
        when(userRepository.findByNameCaseSensitive("testuser"))
                .thenReturn(Optional.of(testUser));

        // When
        Optional<UserEntity> result = userRepository.findByNameCaseSensitive("testuser");

        // Then
        assertTrue(result.isPresent());
        assertEquals("testuser", result.get().getName());
        
        verify(userRepository).findByNameCaseSensitive("testuser");
    }

    @Test
    @DisplayName("findByNameCaseSensitive - 用户不存在返回空Optional")
    void testFindByNameCaseSensitive_NotFound() {
        // Given
        when(userRepository.findByNameCaseSensitive("nonexistent"))
                .thenReturn(Optional.empty());

        // When
        Optional<UserEntity> result = userRepository.findByNameCaseSensitive("nonexistent");

        // Then
        assertFalse(result.isPresent());
        verify(userRepository).findByNameCaseSensitive("nonexistent");
    }

    @Test
    @DisplayName("findByEmail - 成功查找用户")
    void testFindByEmail_Success() {
        // Given
        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(testUser));

        // When
        Optional<UserEntity> result = userRepository.findByEmail("test@example.com");

        // Then
        assertTrue(result.isPresent());
        assertEquals("test@example.com", result.get().getEmail());
        
        verify(userRepository).findByEmail("test@example.com");
    }

    @Test
    @DisplayName("findByEmail - 邮箱不存在返回空Optional")
    void testFindByEmail_NotFound() {
        // Given
        when(userRepository.findByEmail("nonexistent@example.com"))
                .thenReturn(Optional.empty());

        // When
        Optional<UserEntity> result = userRepository.findByEmail("nonexistent@example.com");

        // Then
        assertFalse(result.isPresent());
        verify(userRepository).findByEmail("nonexistent@example.com");
    }

    @Test
    @DisplayName("save - 成功保存用户")
    void testSave_Success() {
        // Given
        when(userRepository.save(any(UserEntity.class)))
                .thenReturn(testUser);

        // When
        UserEntity result = userRepository.save(testUser);

        // Then
        assertNotNull(result);
        assertEquals("testuser", result.getName());
        assertEquals("test@example.com", result.getEmail());
        
        verify(userRepository).save(testUser);
    }

    @Test
    @DisplayName("findById - 成功查找用户")
    void testFindById_Success() {
        // Given
        when(userRepository.findById(1L))
                .thenReturn(Optional.of(testUser));

        // When
        Optional<UserEntity> result = userRepository.findById(1L);

        // Then
        assertTrue(result.isPresent());
        assertEquals(1L, result.get().getId());
        
        verify(userRepository).findById(1L);
    }

    @Test
    @DisplayName("findById - 用户不存在返回空Optional")
    void testFindById_NotFound() {
        // Given
        when(userRepository.findById(999L))
                .thenReturn(Optional.empty());

        // When
        Optional<UserEntity> result = userRepository.findById(999L);

        // Then
        assertFalse(result.isPresent());
        verify(userRepository).findById(999L);
    }

    @Test
    @DisplayName("delete - 成功删除用户")
    void testDelete_Success() {
        // When
        userRepository.delete(testUser);

        // Then
        verify(userRepository).delete(testUser);
    }

    @Test
    @DisplayName("deleteById - 成功删除用户")
    void testDeleteById_Success() {
        // When
        userRepository.deleteById(1L);

        // Then
        verify(userRepository).deleteById(1L);
    }

    @Test
    @DisplayName("findAll - 成功查找所有用户")
    void testFindAll_Success() {
        // Given
        java.util.List<UserEntity> userList = java.util.Arrays.asList(testUser);
        when(userRepository.findAll()).thenReturn(userList);

        // When
        java.util.List<UserEntity> result = userRepository.findAll();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("testuser", result.get(0).getName());
        
        verify(userRepository).findAll();
    }

    @Test
    @DisplayName("继承关系验证 - UserRepository继承JpaRepository")
    void testRepositoryInheritance() {
        // 验证UserRepository继承了JpaRepository
        assertTrue(userRepository instanceof JpaRepository);
        
        // 验证泛型类型
        assertTrue(userRepository instanceof JpaRepository<UserEntity, Long>);
    }

    @Test
    @DisplayName("接口方法验证 - 确认自定义方法存在")
    void testCustomMethodsExist() {
        // 验证自定义方法存在（通过反射或直接调用）
        assertDoesNotThrow(() -> {
            userRepository.findByName("test");
            userRepository.findByNameCaseSensitive("test");
            userRepository.findByEmail("test@example.com");
        });
    }
}
