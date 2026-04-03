package com.listen.portfolio.api.v1.user;

import com.listen.portfolio.api.v1.user.dto.ChangePasswordRequest;
import com.listen.portfolio.api.v1.user.dto.UserSummaryDto;
import com.listen.portfolio.common.ApiResponse;
import com.listen.portfolio.common.jwt.JwtUtil;
import com.listen.portfolio.entity.UserEntity;
import com.listen.portfolio.service.UserService;
import com.listen.portfolio.service.TokenBlacklistService;

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
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Optional;
import java.util.Date;
import java.lang.reflect.Method;

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

    @Mock
    private HttpServletRequest request;

    @Mock
    private ServletRequestAttributes requestAttributes;

    @Mock
    private TokenBlacklistService tokenBlacklistService;

    @Mock
    private JwtUtil jwtUtil;

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
        // Given - 设置 SecurityContext Mock 和 RequestContextHolder Mock
        try (MockedStatic<SecurityContextHolder> securityContextHolderMock = mockStatic(SecurityContextHolder.class);
             MockedStatic<RequestContextHolder> requestContextHolderMock = mockStatic(RequestContextHolder.class)) {
            
            // 设置 SecurityContext
            securityContextHolderMock.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getName()).thenReturn("testuser");
            
            // 设置 RequestContextHolder 和 HttpServletRequest
            requestContextHolderMock.when(RequestContextHolder::getRequestAttributes).thenReturn(requestAttributes);
            when(requestAttributes.getRequest()).thenReturn(request);
            when(request.getHeader("Authorization")).thenReturn("Bearer valid-jwt-token");
            
            // 模拟 JwtUtil 和 TokenBlacklistService
            when(jwtUtil.extractExpiration(anyString())).thenReturn(new java.util.Date());
            doNothing().when(tokenBlacklistService).addToBlacklist(anyString(), anyLong());

            // When
            ResponseEntity<ApiResponse<String>> response = userController.logout();

            // Then
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals("0", response.getBody().getResult());
            assertEquals("Logout successful", response.getBody().getMessage());
        }
    }

    @Test
    @DisplayName("changePassword - 成功修改密码")
    void testChangePassword_Success() {
        // Given - 设置 SecurityContext Mock
        try (MockedStatic<SecurityContextHolder> securityContextHolderMock = mockStatic(SecurityContextHolder.class)) {
            securityContextHolderMock.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getName()).thenReturn("testuser");
            when(authentication.getCredentials()).thenReturn("mock-jwt-token");

            when(userService.getUserByName("testuser"))
                    .thenReturn(Optional.of(mockUserEntity));
            when(userService.changePassword(any(ChangePasswordRequest.class)))
                    .thenReturn(true);
            
            // Mock token extraction and blacklist operations
            when(jwtUtil.extractExpiration(anyString()))
                    .thenReturn(new Date(System.currentTimeMillis() + 3600000)); // 1 hour later
            doNothing().when(tokenBlacklistService).addToBlacklist(anyString(), anyLong());

            // When
            ResponseEntity<ApiResponse<Object>> response = userController.changePassword(mockChangePasswordRequest);

            // Then
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals("0", response.getBody().getResult());
            assertEquals("Password changed successfully. Please login again.", response.getBody().getBody());

            verify(userService).getUserByName("testuser");
            verify(userService).changePassword(mockChangePasswordRequest);
            verify(tokenBlacklistService).addToBlacklist(anyString(), anyLong());
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
            ResponseEntity<ApiResponse<Object>> response = userController.changePassword(mockChangePasswordRequest);

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
            ResponseEntity<ApiResponse<Object>> response = userController.changePassword(mockChangePasswordRequest);

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
            ResponseEntity<ApiResponse<Object>> response = userController.changePassword(mockChangePasswordRequest);

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
            ResponseEntity<ApiResponse<Object>> response = userController.deleteAccount();

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
            ResponseEntity<ApiResponse<Object>> response = userController.deleteAccount();

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
            ResponseEntity<ApiResponse<Object>> response = userController.deleteAccount();

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

    @Test
    @DisplayName("边界测试 - getUserById 正常值验证")
    void testGetUserById_ParameterValidation_Additional() {
        // 测试正常值 - 验证方法能正常调用
        assertDoesNotThrow(() -> userController.getUserById(1L));
        assertDoesNotThrow(() -> userController.getUserById(Long.MAX_VALUE));
        assertDoesNotThrow(() -> userController.getUserById(Long.MIN_VALUE + 1));
    }

    @Test
    @DisplayName("logout - 无认证用户返回成功")
    void testLogout_NoAuthentication() {
        // Given - 无认证用户
        try (MockedStatic<SecurityContextHolder> securityContextHolderMock = mockStatic(SecurityContextHolder.class)) {
            securityContextHolderMock.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            when(securityContext.getAuthentication()).thenReturn(null);

            // When
            ResponseEntity<ApiResponse<String>> response = userController.logout();

            // Then
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals("0", response.getBody().getResult());
            assertEquals("Logout successful", response.getBody().getMessage());
        }
    }

    @Test
    @DisplayName("logout - 无token返回成功")
    void testLogout_NoToken() {
        // Given - 有认证但无token
        try (MockedStatic<SecurityContextHolder> securityContextHolderMock = mockStatic(SecurityContextHolder.class);
             MockedStatic<RequestContextHolder> requestContextHolderMock = mockStatic(RequestContextHolder.class)) {
            
            securityContextHolderMock.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getName()).thenReturn("testuser");
            
            // 模拟无Authorization头
            requestContextHolderMock.when(RequestContextHolder::getRequestAttributes).thenReturn(requestAttributes);
            when(requestAttributes.getRequest()).thenReturn(request);
            when(request.getHeader("Authorization")).thenReturn(null);

            // When
            ResponseEntity<ApiResponse<String>> response = userController.logout();

            // Then
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals("0", response.getBody().getResult());
            assertEquals("Logout successful", response.getBody().getMessage());
        }
    }

    @Test
    @DisplayName("logout - 空token返回成功")
    void testLogout_EmptyToken() {
        // Given - 空token
        try (MockedStatic<SecurityContextHolder> securityContextHolderMock = mockStatic(SecurityContextHolder.class);
             MockedStatic<RequestContextHolder> requestContextHolderMock = mockStatic(RequestContextHolder.class)) {
            
            securityContextHolderMock.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getName()).thenReturn("testuser");
            
            requestContextHolderMock.when(RequestContextHolder::getRequestAttributes).thenReturn(requestAttributes);
            when(requestAttributes.getRequest()).thenReturn(request);
            when(request.getHeader("Authorization")).thenReturn("");

            // When
            ResponseEntity<ApiResponse<String>> response = userController.logout();

            // Then
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals("0", response.getBody().getResult());
            assertEquals("Logout successful", response.getBody().getMessage());
        }
    }

    @Test
    @DisplayName("logout - 无效token格式返回成功")
    void testLogout_InvalidTokenFormat() {
        // Given - 无效token格式（无Bearer前缀）
        try (MockedStatic<SecurityContextHolder> securityContextHolderMock = mockStatic(SecurityContextHolder.class);
             MockedStatic<RequestContextHolder> requestContextHolderMock = mockStatic(RequestContextHolder.class)) {
            
            securityContextHolderMock.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getName()).thenReturn("testuser");
            
            requestContextHolderMock.when(RequestContextHolder::getRequestAttributes).thenReturn(requestAttributes);
            when(requestAttributes.getRequest()).thenReturn(request);
            when(request.getHeader("Authorization")).thenReturn("invalid-token");

            // When
            ResponseEntity<ApiResponse<String>> response = userController.logout();

            // Then
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals("0", response.getBody().getResult());
            assertEquals("Logout successful", response.getBody().getMessage());
        }
    }

    @Test
    @DisplayName("logout - JwtUtil异常处理")
    void testLogout_JwtUtilException() {
        // Given - JwtUtil抛出异常
        try (MockedStatic<SecurityContextHolder> securityContextHolderMock = mockStatic(SecurityContextHolder.class);
             MockedStatic<RequestContextHolder> requestContextHolderMock = mockStatic(RequestContextHolder.class)) {
            
            securityContextHolderMock.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getName()).thenReturn("testuser");
            
            requestContextHolderMock.when(RequestContextHolder::getRequestAttributes).thenReturn(requestAttributes);
            when(requestAttributes.getRequest()).thenReturn(request);
            when(request.getHeader("Authorization")).thenReturn("Bearer valid-jwt-token");
            
            // 模拟JwtUtil抛出异常
            when(jwtUtil.extractExpiration(anyString())).thenThrow(new RuntimeException("JWT processing error"));

            // When
            ResponseEntity<ApiResponse<String>> response = userController.logout();

            // Then
            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals("1", response.getBody().getResult());
            assertEquals("Logout failed", response.getBody().getMessage());
        }
    }

    @Test
    @DisplayName("logout - TokenBlacklistService异常处理")
    void testLogout_TokenBlacklistServiceException() {
        // Given - TokenBlacklistService抛出异常
        try (MockedStatic<SecurityContextHolder> securityContextHolderMock = mockStatic(SecurityContextHolder.class);
             MockedStatic<RequestContextHolder> requestContextHolderMock = mockStatic(RequestContextHolder.class)) {
            
            securityContextHolderMock.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getName()).thenReturn("testuser");
            
            requestContextHolderMock.when(RequestContextHolder::getRequestAttributes).thenReturn(requestAttributes);
            when(requestAttributes.getRequest()).thenReturn(request);
            when(request.getHeader("Authorization")).thenReturn("Bearer valid-jwt-token");
            
            when(jwtUtil.extractExpiration(anyString())).thenReturn(new java.util.Date());
            // 模拟TokenBlacklistService抛出异常
            doThrow(new RuntimeException("Redis error")).when(tokenBlacklistService).addToBlacklist(anyString(), anyLong());

            // When
            ResponseEntity<ApiResponse<String>> response = userController.logout();

            // Then
            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals("1", response.getBody().getResult());
            assertEquals("Logout failed", response.getBody().getMessage());
        }
    }

    @Test
    @DisplayName("extractTokenFromRequest - 从SecurityContext获取token成功")
    void testExtractTokenFromRequest_FromSecurityContext_Success() throws Exception {
        // Given - 模拟SecurityContext中有有效token
        try (MockedStatic<SecurityContextHolder> securityContextHolderMock = mockStatic(SecurityContextHolder.class)) {
            securityContextHolderMock.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getCredentials()).thenReturn("valid-token-from-context");

            // When - 使用反射调用私有方法
            Method method = UserController.class.getDeclaredMethod("extractTokenFromRequest");
            method.setAccessible(true);
            String token = (String) method.invoke(userController);

            // Then
            assertEquals("valid-token-from-context", token);
        }
    }

    @Test
    @DisplayName("extractTokenFromRequest - 从Authorization头获取token成功")
    void testExtractTokenFromRequest_FromAuthorizationHeader_Success() throws Exception {
        // Given - SecurityContext无token，但Authorization头有
        try (MockedStatic<SecurityContextHolder> securityContextHolderMock = mockStatic(SecurityContextHolder.class);
             MockedStatic<RequestContextHolder> requestContextHolderMock = mockStatic(RequestContextHolder.class)) {
            
            securityContextHolderMock.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getCredentials()).thenReturn(null);
            
            requestContextHolderMock.when(RequestContextHolder::getRequestAttributes).thenReturn(requestAttributes);
            when(requestAttributes.getRequest()).thenReturn(request);
            when(request.getHeader("Authorization")).thenReturn("Bearer token-from-header");

            // When - 使用反射调用私有方法
            Method method = UserController.class.getDeclaredMethod("extractTokenFromRequest");
            method.setAccessible(true);
            String token = (String) method.invoke(userController);

            // Then
            assertEquals("token-from-header", token);
        }
    }

    @Test
    @DisplayName("extractTokenFromRequest - 无token返回null")
    void testExtractTokenFromRequest_NoToken_ReturnsNull() throws Exception {
        // Given - SecurityContext和Authorization头都无token
        try (MockedStatic<SecurityContextHolder> securityContextHolderMock = mockStatic(SecurityContextHolder.class);
             MockedStatic<RequestContextHolder> requestContextHolderMock = mockStatic(RequestContextHolder.class)) {
            
            securityContextHolderMock.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getCredentials()).thenReturn(null);
            
            requestContextHolderMock.when(RequestContextHolder::getRequestAttributes).thenReturn(requestAttributes);
            when(requestAttributes.getRequest()).thenReturn(request);
            when(request.getHeader("Authorization")).thenReturn(null);

            // When - 使用反射调用私有方法
            Method method = UserController.class.getDeclaredMethod("extractTokenFromRequest");
            method.setAccessible(true);
            String token = (String) method.invoke(userController);

            // Then
            assertNull(token);
        }
    }

    @Test
    @DisplayName("extractTokenFromRequest - RequestContextHolder异常处理")
    void testExtractTokenFromRequest_RequestContextHolderException() throws Exception {
        // Given - RequestContextHolder抛出异常
        try (MockedStatic<SecurityContextHolder> securityContextHolderMock = mockStatic(SecurityContextHolder.class);
             MockedStatic<RequestContextHolder> requestContextHolderMock = mockStatic(RequestContextHolder.class)) {
            
            securityContextHolderMock.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getCredentials()).thenReturn(null);
            
            // 模拟RequestContextHolder抛出异常
            requestContextHolderMock.when(RequestContextHolder::getRequestAttributes).thenThrow(new RuntimeException("Request context error"));

            // When - 使用反射调用私有方法
            Method method = UserController.class.getDeclaredMethod("extractTokenFromRequest");
            method.setAccessible(true);
            String token = (String) method.invoke(userController);

            // Then
            assertNull(token);
        }
    }

    @Test
    @DisplayName("maskToken - 正常token遮盖")
    void testMaskToken_NormalToken() throws Exception {
        // Given
        String token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";

        // When - 使用反射调用私有方法
        Method method = UserController.class.getDeclaredMethod("maskToken", String.class);
        method.setAccessible(true);
        String masked = (String) method.invoke(userController, token);

        // Then
        assertTrue(masked.startsWith("eyJhbG"));
        assertTrue(masked.endsWith("w5c"));
        assertTrue(masked.contains("***"));
        assertTrue(masked.length() < token.length());
    }

    @Test
    @DisplayName("maskToken - 短token处理")
    void testMaskToken_ShortToken() throws Exception {
        // Given - 短token
        String token = "short";

        // When - 使用反射调用私有方法
        Method method = UserController.class.getDeclaredMethod("maskToken", String.class);
        method.setAccessible(true);
        String masked = (String) method.invoke(userController, token);

        // Then
        assertEquals("***", masked);
    }

    @Test
    @DisplayName("maskToken - null token处理")
    void testMaskToken_NullToken() throws Exception {
        // Given - null token
        String token = null;

        // When - 使用反射调用私有方法
        Method method = UserController.class.getDeclaredMethod("maskToken", String.class);
        method.setAccessible(true);
        String masked = (String) method.invoke(userController, token);

        // Then
        assertEquals("***", masked);
    }

    @Test
    @DisplayName("maskToken - 空token处理")
    void testMaskToken_EmptyToken() throws Exception {
        // Given - 空token
        String token = "";

        // When - 使用反射调用私有方法
        Method method = UserController.class.getDeclaredMethod("maskToken", String.class);
        method.setAccessible(true);
        String masked = (String) method.invoke(userController, token);

        // Then
        assertEquals("***", masked);
    }

    @Test
    @DisplayName("maskToken - 10字符token边界测试")
    void testMaskToken_TenCharToken() throws Exception {
        // Given - 正好10字符的token
        String token = "1234567890";

        // When - 使用反射调用私有方法
        Method method = UserController.class.getDeclaredMethod("maskToken", String.class);
        method.setAccessible(true);
        String masked = (String) method.invoke(userController, token);

        // Then - 10字符应该被遮盖: 前6位 + *** + 后4位
        assertEquals("123456***7890", masked);
        assertTrue(masked.contains("***"));
    }

    @Test
    @DisplayName("maskToken - 11字符token边界测试")
    void testMaskToken_ElevenCharToken() throws Exception {
        // Given - 11字符的token
        String token = "12345678901";

        // When - 使用反射调用私有方法
        Method method = UserController.class.getDeclaredMethod("maskToken", String.class);
        method.setAccessible(true);
        String masked = (String) method.invoke(userController, token);

        // Then - 11字符应该被遮盖: 前6位 + *** + 后4位
        assertEquals("123456***8901", masked);
        assertTrue(masked.contains("***"));
        // 注意：遮盖后的长度可能比原长度长，这是正常的
    }

    @Test
    @DisplayName("getUserById - ID为0的边界测试")
    void testGetUserById_ZeroIdBoundary() {
        // When - 调用getUserById(0)
        ApiResponse<UserSummaryDto> response = userController.getUserById(0L);
        
        // Then - 应该正常处理（@Min注解在单元测试中可能不生效）
        assertNotNull(response);
    }

    @Test
    @DisplayName("getUserById - 负数ID边界测试")
    void testGetUserById_NegativeIdBoundary() {
        // When - 调用getUserById(-1)
        ApiResponse<UserSummaryDto> response = userController.getUserById(-1L);
        
        // Then - 应该正常处理（@Min注解在单元测试中可能不生效）
        assertNotNull(response);
    }
}
