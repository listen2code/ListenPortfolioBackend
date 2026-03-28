package com.listen.portfolio;

import com.listen.portfolio.api.v1.about.dto.AboutMeDto;
import com.listen.portfolio.infrastructure.persistence.entity.UserEntity;
import com.listen.portfolio.repository.UserRepository;
import com.listen.portfolio.service.AboutMeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AboutMeServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private Authentication authentication;

    @Mock
    private SecurityContext securityContext;

    @InjectMocks
    private AboutMeService aboutMeService;

    private UserEntity testUser;

    @BeforeEach
    void setUp() {
        testUser = new UserEntity();
        testUser.setId(1L);
        testUser.setName("testuser");
        testUser.setStatus("Active");
        testUser.setJobTitle("Software Engineer");
        testUser.setBio("Test bio");
        testUser.setGraduationYear("2020");
        testUser.setGithubUrl("https://github.com/testuser");
        testUser.setMajor("Computer Science");
    }

    @Test
    void testGetAboutMeDto_WithAuthenticatedUser_ShouldReturnUserDto() {
        // 设置认证上下文
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("testuser");
        SecurityContextHolder.setContext(securityContext);

        // 模拟数据库查询
        when(userRepository.findByName("testuser")).thenReturn(Optional.of(testUser));

        // 执行测试
        Optional<AboutMeDto> result = aboutMeService.getAboutMeDto("testuser");

        // 验证结果
        assertTrue(result.isPresent());
        AboutMeDto dto = result.get();
        assertEquals("Active", dto.getStatus());
        assertEquals("Software Engineer", dto.getJobTitle());
        assertEquals("Test bio", dto.getBio());
        assertEquals("2020", dto.getGraduationYear());
        assertEquals("https://github.com/testuser", dto.getGithub());
        assertEquals("Computer Science", dto.getMajor());

        // 验证方法调用
        verify(userRepository, times(1)).findByName("testuser");
        verify(authentication, times(1)).getName();
    }

    @Test
    void testGetAboutMeDto_WithNoAuthentication_ShouldReturnEmpty() {
        // 设置空的认证上下文
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(null);

        // 执行测试
        Optional<AboutMeDto> result = aboutMeService.getAboutMeDto("testuser");

        // 验证结果
        assertFalse(result.isPresent());

        // 验证没有调用数据库查询
        verify(userRepository, never()).findByName(anyString());
    }

    @Test
    void testGetAboutMeDto_WithUserNotFound_ShouldReturnEmpty() {
        // 设置认证上下文
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("nonexistentuser");
        SecurityContextHolder.setContext(securityContext);

        // 模拟用户不存在
        when(userRepository.findByName("nonexistentuser")).thenReturn(Optional.empty());

        // 执行测试
        Optional<AboutMeDto> result = aboutMeService.getAboutMeDto("testuser");

        // 验证结果
        assertFalse(result.isPresent());

        // 验证方法调用
        verify(userRepository, times(1)).findByName("nonexistentuser");
    }

    @Test
    void testGetAboutMeDto_WithNullAuthenticationName_ShouldReturnEmpty() {
        // 设置认证上下文但用户名为null
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn(null);
        SecurityContextHolder.setContext(securityContext);

        // 执行测试
        Optional<AboutMeDto> result = aboutMeService.getAboutMeDto("testuser");

        // 验证结果
        assertFalse(result.isPresent());

        // 验证方法调用
        verify(userRepository, times(1)).findByName(null);
    }
}
