package com.listen.portfolio.api.v1.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.listen.portfolio.api.v1.auth.dto.LoginRequest;
import com.listen.portfolio.api.v1.user.dto.ChangePasswordRequest;
import com.listen.portfolio.entity.UserEntity;
import com.listen.portfolio.integration.BaseIntegrationTest;
import com.listen.portfolio.repository.UserRepository;
import com.listen.portfolio.service.RefreshTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import jakarta.servlet.Filter;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("AuthController Refresh Token Integration Tests")
public class AuthControllerRefreshTest extends BaseIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private Filter springSecurityFilterChain;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private RefreshTokenService refreshTokenService;

    // 手动实例化 ObjectMapper，避免 Spring Boot 4 版本迁移中可能发生的 Jackson 依赖注入解析差异
    private final ObjectMapper objectMapper = new ObjectMapper();

    private UserEntity testUser;

    @BeforeEach
    void setUp() {
        // 手动配置 MockMvc 并载入 Spring Security 过滤链，保证接口权限认证生效
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .addFilters(springSecurityFilterChain)
                .build();

        // 清理数据库中所有用户
        userRepository.deleteAll();
    }

    private String[] loginAndGetTokens(String username) throws Exception {
        // 注册一个测试用的非种子用户 (ID 将自动生成，通常 > 1)
        UserEntity user = new UserEntity();
        user.setName(username);
        user.setEmail(username + "@example.com");
        user.setPassword(passwordEncoder.encode("password123"));
        user.setDeleted(false);
        testUser = userRepository.save(user);

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUserName(username);
        loginRequest.setPassword("password123");

        MvcResult result = mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String responseContent = result.getResponse().getContentAsString();
        JsonNode jsonNode = objectMapper.readTree(responseContent);
        // LoginResponse 中的字段名为 token，并非 accessToken
        String accessToken = jsonNode.path("body").path("token").asText();
        String refreshToken = jsonNode.path("body").path("refreshToken").asText();

        assertNotNull(accessToken);
        assertNotNull(refreshToken);
        assertFalse(accessToken.isEmpty());
        assertFalse(refreshToken.isEmpty());

        return new String[]{accessToken, refreshToken};
    }

    @Test
    @DisplayName("1. 登录成功后，Refresh Token 应成功存入 Redis")
    void testLogin_ShouldSaveRefreshTokenInRedis() throws Exception {
        // When - 登录并提取 Token
        String[] tokens = loginAndGetTokens("loginuser");
        String refreshToken = tokens[1];

        // Then - 校验 Redis 中是否存在该 Refresh Token
        assertTrue(refreshTokenService.isRefreshTokenValid("loginuser", refreshToken));
    }

    @Test
    @DisplayName("2. 刷新 Token 成功时应执行旋转 (Rotation) 逻辑")
    void testRefresh_ShouldRotateTokensAndInvalidateOldOne() throws Exception {
        // Given - 登录获取 Token
        String[] tokens = loginAndGetTokens("rotateuser");
        String originalRefreshToken = tokens[1];

        // 睡眠 1.1 秒跨越 JWT 的秒级时间戳精度（iat/exp），确保旋转后生成内容/签名不同的新 Token
        Thread.sleep(1100);

        // When - 调用刷新接口
        MvcResult refreshResult = mockMvc.perform(post("/v1/auth/refresh")
                        .param("refreshToken", originalRefreshToken))
                .andExpect(status().isOk())
                .andReturn();

        String refreshResponse = refreshResult.getResponse().getContentAsString();
        JsonNode jsonNode = objectMapper.readTree(refreshResponse);
        String newAccessToken = jsonNode.path("body").path("token").asText();
        String newRefreshToken = jsonNode.path("body").path("refreshToken").asText();

        // Then - 校验生成了新 Token 且值不同
        assertNotNull(newAccessToken);
        assertNotNull(newRefreshToken);
        assertNotEquals(originalRefreshToken, newRefreshToken);

        // Then - 校验旧 Refresh Token 在 Redis 中已被吊销/删除
        assertFalse(refreshTokenService.isRefreshTokenValid("rotateuser", originalRefreshToken));

        // Then - 校验新 Refresh Token 在 Redis 中为有效状态
        assertTrue(refreshTokenService.isRefreshTokenValid("rotateuser", newRefreshToken));
    }

    @Test
    @DisplayName("3. 旋转发生后，尝试重放/重用旧的 Refresh Token 应被拒绝并返回 401")
    void testRefresh_ReplayAttackShouldBeBlocked() throws Exception {
        // Given - 登录并进行第一次刷新旋转
        String[] tokens = loginAndGetTokens("replayuser");
        String originalRefreshToken = tokens[1];

        // 睡眠 1.1 秒跨越时间戳精度限制
        Thread.sleep(1100);

        mockMvc.perform(post("/v1/auth/refresh")
                        .param("refreshToken", originalRefreshToken))
                .andExpect(status().isOk());

        // When - 尝试重用已被删除 of originalRefreshToken 进行第二次刷新
        mockMvc.perform(post("/v1/auth/refresh")
                        .param("refreshToken", originalRefreshToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.result").value("1"))
                .andExpect(jsonPath("$.message").value("Refresh token has been revoked or is invalid"));
    }

    @Test
    @DisplayName("4. 用户退出登录时，名下的所有 Refresh Token 均应被吊销")
    void testLogout_ShouldRevokeAllRefreshTokens() throws Exception {
        // Given - 登录获取 Token
        String[] tokens = loginAndGetTokens("logoutuser");
        String accessToken = tokens[0];
        String refreshToken = tokens[1];

        // When - 发起退出登录请求 (需要带上 Access Token)
        mockMvc.perform(post("/v1/user/logout")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());

        // Then - 验证 Refresh Token 在 Redis 中已失效
        assertFalse(refreshTokenService.isRefreshTokenValid("logoutuser", refreshToken));

        // Then - 尝试刷新应该被拒绝
        mockMvc.perform(post("/v1/auth/refresh")
                        .param("refreshToken", refreshToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("5. 修改密码成功后，原所有的 Refresh Token 均应被吊销")
    void testChangePassword_ShouldRevokeAllRefreshTokens() throws Exception {
        // Given - 登录并拿到 Token
        String[] tokens = loginAndGetTokens("passuser");
        String accessToken = tokens[0];
        String refreshToken = tokens[1];

        // 构造修改密码请求
        ChangePasswordRequest changePasswordRequest = new ChangePasswordRequest();
        changePasswordRequest.setUserId(String.valueOf(testUser.getId()));
        changePasswordRequest.setOldPassword("password123");
        changePasswordRequest.setNewPassword("newPassword123");

        // When - 发起修改密码请求 (需要 Bearer Token，路径为 kebab-case /change-password)
        mockMvc.perform(post("/v1/user/change-password")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(changePasswordRequest)))
                .andExpect(status().isOk());

        // Then - 验证旧的 Refresh Token 在 Redis 中已被吊销
        assertFalse(refreshTokenService.isRefreshTokenValid("passuser", refreshToken));

        // Then - 尝试刷新应该被拒绝
        mockMvc.perform(post("/v1/auth/refresh")
                        .param("refreshToken", refreshToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("6. 账户注销(注销)成功后，名下所有的 Refresh Token 均应被吊销")
    void testDeleteAccount_ShouldRevokeAllRefreshTokens() throws Exception {
        // Given - 登录获取 Token
        String[] tokens = loginAndGetTokens("deleteuser");
        String accessToken = tokens[0];
        String refreshToken = tokens[1];

        // When - 发起注销账户请求 (DELETE /v1/user/delete-account)
        mockMvc.perform(delete("/v1/user/delete-account")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());

        // Then - 验证 Refresh Token 在 Redis 中已失效
        assertFalse(refreshTokenService.isRefreshTokenValid("deleteuser", refreshToken));

        // Then - 尝试刷新应该被拒绝
        mockMvc.perform(post("/v1/auth/refresh")
                        .param("refreshToken", refreshToken))
                .andExpect(status().isUnauthorized());
    }
}
