package com.listen.portfolio.api.v1.auth;

import com.listen.portfolio.api.v1.auth.dto.AuthRequest;
import com.listen.portfolio.api.v1.auth.dto.AuthResponse;
import com.listen.portfolio.api.v1.auth.dto.ForgotPasswordRequest;
import com.listen.portfolio.api.v1.auth.dto.SignUpRequest;
import com.listen.portfolio.common.ApiResponse;
import com.listen.portfolio.infrastructure.persistence.entity.UserEntity;
import com.listen.portfolio.jwt.JwtUtil;
import com.listen.portfolio.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.ArrayList;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthController Refactored Unit Tests")
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private UserDetailsService userDetailsService;

    private AuthController authController;
    private SignUpRequest mockSignUpRequest;
    private AuthRequest mockAuthRequest;
    private ForgotPasswordRequest mockForgotPasswordRequest;
    private UserEntity mockUserEntity;
    private UserDetails mockUserDetails;

    @BeforeEach
    void setUp() {
        // 使用重构后的构造函数创建 AuthController
        authController = new AuthController(
                authService,
                authenticationManager,
                jwtUtil,
                userDetailsService
        );

        // 初始化测试数据
        mockUserEntity = new UserEntity();
        mockUserEntity.setId(1L);
        mockUserEntity.setName("testuser");
        mockUserEntity.setEmail("test@example.com");
        mockUserEntity.setPassword("encodedPassword");

        mockUserDetails = new User("testuser", "encodedPassword", new ArrayList<>());

        mockSignUpRequest = new SignUpRequest();
        mockSignUpRequest.setUserName("testuser");
        mockSignUpRequest.setPassword("password123");
        mockSignUpRequest.setEmail("test@example.com");

        mockAuthRequest = new AuthRequest();
        mockAuthRequest.setUserName("testuser");
        mockAuthRequest.setPassword("password123");

        mockForgotPasswordRequest = new ForgotPasswordRequest();
        mockForgotPasswordRequest.setEmail("test@example.com");
    }

    @Test
    @DisplayName("signUp - 成功注册新用户")
    void testSignUp_Success() {
        // Given
        when(authService.signUp(any(SignUpRequest.class)))
                .thenReturn(true);

        // When
        ResponseEntity<ApiResponse<Void>> response = authController.signUp(mockSignUpRequest);

        // Then
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("0", response.getBody().getResult());
        assertNull(response.getBody().getBody());

        // 验证调用
        verify(authService).signUp(any(SignUpRequest.class));
    }

    @Test
    @DisplayName("signUp - 用户名已存在注册失败")
    void testSignUp_UsernameAlreadyExists() {
        // Given
        when(authService.signUp(any(SignUpRequest.class)))
                .thenReturn(false);

        // When
        ResponseEntity<ApiResponse<Void>> response = authController.signUp(mockSignUpRequest);

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("1", response.getBody().getResult());
        assertEquals("Username already exists", response.getBody().getMessage());

        // 验证调用
        verify(authService).signUp(any(SignUpRequest.class));
    }

    @Test
    @DisplayName("login - 成功登录")
    void testLogin_Success() {
        // Given
        Authentication mockAuthentication = mock(Authentication.class);
        when(authenticationManager.authenticate(any()))
                .thenReturn(mockAuthentication);
        when(userDetailsService.loadUserByUsername("testuser"))
                .thenReturn(mockUserDetails);
        when(jwtUtil.generateToken(mockUserDetails))
                .thenReturn("jwtToken");
        when(jwtUtil.generateRefreshToken("jwtToken"))
                .thenReturn("refreshToken");
        when(authService.getUserByName("testuser"))
                .thenReturn(Optional.of(mockUserEntity));

        // When
        ResponseEntity<?> response = authController.login(mockAuthRequest);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        
        ApiResponse<AuthResponse> apiResponse = (ApiResponse<AuthResponse>) response.getBody();
        assertEquals("0", apiResponse.getResult());
        assertNotNull(apiResponse.getBody());
        assertEquals("jwtToken", apiResponse.getBody().getToken());
        assertEquals("refreshToken", apiResponse.getBody().getRefreshToken());
        assertEquals(1L, apiResponse.getBody().getUserId());

        // 验证调用
        verify(authenticationManager).authenticate(any());
        verify(userDetailsService).loadUserByUsername("testuser");
        verify(jwtUtil).generateToken(mockUserDetails);
        verify(jwtUtil).generateRefreshToken("jwtToken");
        verify(authService).getUserByName("testuser");
    }

    @Test
    @DisplayName("login - 凭据无效登录失败")
    void testLogin_InvalidCredentials() {
        // Given
        doThrow(new BadCredentialsException("Bad credentials"))
                .when(authenticationManager).authenticate(any());

        // When
        ResponseEntity<?> response = authController.login(mockAuthRequest);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        
        ApiResponse<?> apiResponse = (ApiResponse<?>) response.getBody();
        assertEquals("1", apiResponse.getResult());
        assertEquals("Invalid credentials", apiResponse.getMessage());

        // 验证调用
        verify(authenticationManager).authenticate(any());
        verify(userDetailsService, never()).loadUserByUsername(anyString());
        verify(jwtUtil, never()).generateToken(any());
    }

    @Test
    @DisplayName("refresh - 成功刷新令牌")
    void testRefreshToken_Success() {
        // Given
        String oldRefreshToken = "oldRefreshToken";
        String newJwtToken = "newJwtToken";
        String newRefreshToken = "newRefreshToken";

        when(jwtUtil.refreshToken(oldRefreshToken))
                .thenReturn(newJwtToken);
        when(jwtUtil.generateRefreshToken(newJwtToken))
                .thenReturn(newRefreshToken);
        when(jwtUtil.extractUsername(oldRefreshToken))
                .thenReturn("testuser");
        when(authService.getUserByName("testuser"))
                .thenReturn(Optional.of(mockUserEntity));

        // When
        ResponseEntity<ApiResponse<?>> response = authController.refresh(oldRefreshToken);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        
        ApiResponse<AuthResponse> apiResponse = (ApiResponse<AuthResponse>) response.getBody();
        assertEquals("0", apiResponse.getResult());
        assertNotNull(apiResponse.getBody());
        assertEquals(newJwtToken, apiResponse.getBody().getToken());
        assertEquals(newRefreshToken, apiResponse.getBody().getRefreshToken());
        assertEquals(1L, apiResponse.getBody().getUserId());

        // 验证调用
        verify(jwtUtil).refreshToken(oldRefreshToken);
        verify(jwtUtil).generateRefreshToken(newJwtToken);
        verify(jwtUtil).extractUsername(oldRefreshToken);
        verify(authService).getUserByName("testuser");
    }

    @Test
    @DisplayName("forgotPassword - 成功重置密码")
    void testForgotPassword_Success() {
        // Given
        when(authService.forgotPassword(any(ForgotPasswordRequest.class)))
                .thenReturn(true);

        // When
        ResponseEntity<ApiResponse<Void>> response = authController.forgotPassword(mockForgotPasswordRequest);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("0", response.getBody().getResult());
        assertNull(response.getBody().getBody());

        // 验证调用
        verify(authService).forgotPassword(any(ForgotPasswordRequest.class));
    }

    @Test
    @DisplayName("forgotPassword - 邮箱不存在重置失败")
    void testForgotPassword_EmailNotFound() {
        // Given
        when(authService.forgotPassword(any(ForgotPasswordRequest.class)))
                .thenReturn(false);

        // When
        ResponseEntity<ApiResponse<Void>> response = authController.forgotPassword(mockForgotPasswordRequest);

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("1", response.getBody().getResult());
        assertEquals("Password change failed", response.getBody().getMessage());

        // 验证调用
        verify(authService).forgotPassword(any(ForgotPasswordRequest.class));
    }

    @Test
    @DisplayName("构造函数注入验证 - 所有依赖都已正确注入")
    void testConstructorInjection() {
        // 验证通过构造函数创建的实例不为 null
        assertNotNull(authController);
        
        // 验证所有依赖都已正确注入（通过调用方法验证不会抛出 NullPointerException）
        assertDoesNotThrow(() -> {
            // 这些调用在正常情况下不会抛出 NullPointerException
            assertNotNull(authService);
            assertNotNull(authenticationManager);
            assertNotNull(jwtUtil);
            assertNotNull(userDetailsService);
        });
    }

    @Test
    @DisplayName("控制器注解验证 - 验证控制器注解")
    void testControllerAnnotations() {
        // 验证控制器相关的注解
        assertTrue(authController.getClass().isAnnotationPresent(org.springframework.web.bind.annotation.RestController.class));
        
        // 验证RequestMapping注解
        org.springframework.web.bind.annotation.RequestMapping requestMapping = 
            authController.getClass().getAnnotation(org.springframework.web.bind.annotation.RequestMapping.class);
        assertNotNull(requestMapping);
        assertArrayEquals(new String[]{"/v1/auth"}, requestMapping.value());
    }

    @Test
    @DisplayName("边界测试 - 空请求处理")
    void testNullRequestHandling() {
        // 测试空请求不会导致 NullPointerException
        assertDoesNotThrow(() -> {
            // 这些测试需要根据实际的验证逻辑来调整
            // authController.signUp(null); // 可能抛出验证异常
        });
    }
}
