package com.listen.portfolio.api.v1.user;

import com.listen.portfolio.api.v1.auth.dto.ChangePasswordRequest;
import com.listen.portfolio.api.v1.user.dto.UserSummaryDto;
import com.listen.portfolio.common.ApiResponse;
import com.listen.portfolio.infrastructure.persistence.entity.UserEntity;
import com.listen.portfolio.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserController Unit Tests")
class UserControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private Authentication authentication;

    @Mock
    private SecurityContext securityContext;

    @InjectMocks
    private UserController userController;

    private UserEntity mockUserEntity;
    private UserSummaryDto mockUserSummaryDto;
    private ChangePasswordRequest mockChangePasswordRequest;

    @BeforeEach
    void setUp() {
        // 初始化测试数据
        mockUserEntity = new UserEntity();
        mockUserEntity.setId(1L);
        mockUserEntity.setName("testuser");
        mockUserEntity.setEmail("test@example.com");
        mockUserEntity.setPassword("encodedPassword");

        mockUserSummaryDto = new UserSummaryDto();
        mockUserSummaryDto.setId(1L);
        mockUserSummaryDto.setName("testuser");
        mockUserSummaryDto.setEmail("test@example.com");

        mockChangePasswordRequest = new ChangePasswordRequest();
        mockChangePasswordRequest.setUserId("1");
        mockChangePasswordRequest.setOldPassword("oldPassword");
        mockChangePasswordRequest.setNewPassword("newPassword");
    }

    @Test
    @DisplayName("getUserById - 成功获取用户信息")
    void testGetUserById_Success() {
        // Given
        when(userService.getUserSummaryById(1L))
                .thenReturn(Optional.of(mockUserSummaryDto));

        // When
        ApiResponse<UserSummaryDto> response = userController.getUserById(1L);

        // Then
        assertNotNull(response);
        assertEquals("0", response.getResult());
        assertNotNull(response.getBody());
        assertEquals("testuser", response.getBody().getName());
        assertEquals("test@example.com", response.getBody().getEmail());

        verify(userService).getUserSummaryById(1L);
    }

    @Test
    @DisplayName("getUserById - 用户不存在返回错误")
    void testGetUserById_UserNotFound() {
        // Given
        when(userService.getUserSummaryById(999L))
                .thenReturn(Optional.empty());

        // When
        ApiResponse<UserSummaryDto> response = userController.getUserById(999L);

        // Then
        assertNotNull(response);
        assertEquals("1", response.getResult());
        assertEquals("User not found", response.getMessage());
        assertNull(response.getBody());

        verify(userService).getUserSummaryById(999L);
    }

    @Test
    @DisplayName("logout - 成功登出")
    void testLogout_Success() {
        // When
        ResponseEntity<ApiResponse<String>> response = userController.logout();

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("0", response.getBody().getResult());
        assertEquals("Logout successful", response.getBody().getBody());
    }

    @Test
    @DisplayName("changePassword - 成功修改密码")
    void testChangePassword_Success() {
        // Given - 设置 SecurityContext Mock
        try (MockedStatic<SecurityContextHolder> securityContextHolderMock = mockStatic(SecurityContextHolder.class)) {
            securityContextHolderMock.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getName()).thenReturn("testuser");

            when(userService.getUserByName("testuser"))
                    .thenReturn(Optional.of(mockUserEntity));
            when(userService.changePassword(any(ChangePasswordRequest.class)))
                    .thenReturn(true);

            // When
            ResponseEntity<ApiResponse<Void>> response = userController.changePassword(mockChangePasswordRequest);

            // Then
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals("0", response.getBody().getResult());
            assertNull(response.getBody().getBody());

            verify(userService).getUserByName("testuser");
            verify(userService).changePassword(mockChangePasswordRequest);
        }
    }

    @Test
    @DisplayName("changePassword - 用户不存在返回404")
    void testChangePassword_UserNotFound() {
        // Given - 设置 SecurityContext Mock
        try (MockedStatic<SecurityContextHolder> securityContextHolderMock = mockStatic(SecurityContextHolder.class)) {
            securityContextHolderMock.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getName()).thenReturn("testuser");

            when(userService.getUserByName("testuser"))
                    .thenReturn(Optional.empty());

            // When
            ResponseEntity<ApiResponse<Void>> response = userController.changePassword(mockChangePasswordRequest);

            // Then
            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals("1", response.getBody().getResult());
            assertEquals("User not found", response.getBody().getMessage());

            verify(userService).getUserByName("testuser");
            verify(userService, never()).changePassword(any(ChangePasswordRequest.class));
        }
    }

    @Test
    @DisplayName("changePassword - 尝试修改其他用户密码返回403")
    void testChangePassword_Forbidden() {
        // Given - 设置 SecurityContext Mock
        try (MockedStatic<SecurityContextHolder> securityContextHolderMock = mockStatic(SecurityContextHolder.class)) {
            securityContextHolderMock.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getName()).thenReturn("testuser");
            
            // 设置不同的用户ID
            mockUserEntity.setId(2L); // 当前用户ID是2
            mockChangePasswordRequest.setUserId("1"); // 尝试修改用户1的密码

            when(userService.getUserByName("testuser"))
                    .thenReturn(Optional.of(mockUserEntity));

            // When
            ResponseEntity<ApiResponse<Void>> response = userController.changePassword(mockChangePasswordRequest);

            // Then
            assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals("1", response.getBody().getResult());
            assertEquals("Cannot change password for other users", response.getBody().getMessage());

            verify(userService).getUserByName("testuser");
            verify(userService, never()).changePassword(any(ChangePasswordRequest.class));
        }
    }

    @Test
    @DisplayName("changePassword - 密码修改失败返回400")
    void testChangePassword_Failed() {
        // Given - 设置 SecurityContext Mock
        try (MockedStatic<SecurityContextHolder> securityContextHolderMock = mockStatic(SecurityContextHolder.class)) {
            securityContextHolderMock.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getName()).thenReturn("testuser");
            
            when(userService.getUserByName("testuser"))
                    .thenReturn(Optional.of(mockUserEntity));
            when(userService.changePassword(any(ChangePasswordRequest.class)))
                    .thenReturn(false);

            // When
            ResponseEntity<ApiResponse<Void>> response = userController.changePassword(mockChangePasswordRequest);

            // Then
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals("1", response.getBody().getResult());
            assertEquals("Password change failed", response.getBody().getMessage());

            verify(userService).getUserByName("testuser");
            verify(userService).changePassword(mockChangePasswordRequest);
        }
    }

    @Test
    @DisplayName("deleteAccount - 成功删除账户")
    void testDeleteAccount_Success() {
        // Given - 设置 SecurityContext Mock
        try (MockedStatic<SecurityContextHolder> securityContextHolderMock = mockStatic(SecurityContextHolder.class)) {
            securityContextHolderMock.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getName()).thenReturn("testuser");
            
            when(userService.getUserByName("testuser"))
                    .thenReturn(Optional.of(mockUserEntity));
            when(userService.deleteAccount(1L))
                    .thenReturn(true);

            // When
            ResponseEntity<ApiResponse<Void>> response = userController.deleteAccount();

            // Then
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals("0", response.getBody().getResult());
            assertNull(response.getBody().getBody());

            verify(userService).getUserByName("testuser");
            verify(userService).deleteAccount(1L);
        }
    }

    @Test
    @DisplayName("deleteAccount - 用户不存在返回404")
    void testDeleteAccount_UserNotFound() {
        // Given - 设置 SecurityContext Mock
        try (MockedStatic<SecurityContextHolder> securityContextHolderMock = mockStatic(SecurityContextHolder.class)) {
            securityContextHolderMock.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getName()).thenReturn("testuser");
            
            when(userService.getUserByName("testuser"))
                    .thenReturn(Optional.empty());

            // When
            ResponseEntity<ApiResponse<Void>> response = userController.deleteAccount();

            // Then
            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals("1", response.getBody().getResult());
            assertEquals("User not found", response.getBody().getMessage());

            verify(userService).getUserByName("testuser");
            verify(userService, never()).deleteAccount(anyLong());
        }
    }

    @Test
    @DisplayName("deleteAccount - 删除失败返回500")
    void testDeleteAccount_Failed() {
        // Given - 设置 SecurityContext Mock
        try (MockedStatic<SecurityContextHolder> securityContextHolderMock = mockStatic(SecurityContextHolder.class)) {
            securityContextHolderMock.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getName()).thenReturn("testuser");
            
            when(userService.getUserByName("testuser"))
                    .thenReturn(Optional.of(mockUserEntity));
            when(userService.deleteAccount(1L))
                    .thenReturn(false);

            // When
            ResponseEntity<ApiResponse<Void>> response = userController.deleteAccount();

            // Then
            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals("1", response.getBody().getResult());
            assertEquals("Account deletion failed", response.getBody().getMessage());

            verify(userService).getUserByName("testuser");
            verify(userService).deleteAccount(1L);
        }
    }

    @Test
    @DisplayName("构造函数注入验证")
    void testConstructorInjection() {
        // 验证通过 @InjectMocks 创建的实例不为 null
        assertNotNull(userController);
        assertNotNull(userService);
    }

    @Test
    @DisplayName("控制器注解验证")
    void testControllerAnnotations() {
        // 验证控制器相关的注解
        assertTrue(userController.getClass().isAnnotationPresent(org.springframework.web.bind.annotation.RestController.class));
        
        // 验证RequestMapping注解
        org.springframework.web.bind.annotation.RequestMapping requestMapping = 
            userController.getClass().getAnnotation(org.springframework.web.bind.annotation.RequestMapping.class);
        assertNotNull(requestMapping);
        assertArrayEquals(new String[]{"/v1/user"}, requestMapping.value());
    }

    @Test
    @DisplayName("边界测试 - getUserById 参数验证")
    void testGetUserById_ParameterValidation() {
        // 测试边界值
        ApiResponse<UserSummaryDto> response1 = userController.getUserById(1L);
        verify(userService).getUserSummaryById(1L);

        ApiResponse<UserSummaryDto> response2 = userController.getUserById(Long.MAX_VALUE);
        verify(userService).getUserSummaryById(Long.MAX_VALUE);
    }
}
