package com.listen.portfolio.api.v1.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.listen.portfolio.api.v1.auth.dto.LoginRequest;
import com.listen.portfolio.api.v1.user.dto.ChangePasswordRequest;
import com.listen.portfolio.entity.UserEntity;
import com.listen.portfolio.integration.BaseIntegrationTest;
import com.listen.portfolio.mapper.UserMapper;
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
    private UserMapper userMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private RefreshTokenService refreshTokenService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private UserEntity testUser;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .addFilters(springSecurityFilterChain)
                .build();

        userMapper.delete(null);
    }

    private String[] loginAndGetTokens(String username) throws Exception {
        UserEntity user = new UserEntity();
        user.setName(username);
        user.setEmail(username + "@example.com");
        user.setPassword(passwordEncoder.encode("password123"));
        user.setDeleted(false);
        userMapper.insert(user);
        testUser = user;

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
        String[] tokens = loginAndGetTokens("loginuser");
        String refreshToken = tokens[1];

        assertTrue(refreshTokenService.isRefreshTokenValid("loginuser", refreshToken));
    }

    @Test
    @DisplayName("2. 刷新 Token 成功时应执行旋转 (Rotation) 逻辑")
    void testRefresh_ShouldRotateTokensAndInvalidateOldOne() throws Exception {
        String[] tokens = loginAndGetTokens("rotateuser");
        String originalRefreshToken = tokens[1];

        Thread.sleep(1100);

        MvcResult refreshResult = mockMvc.perform(post("/v1/auth/refresh")
                        .param("refreshToken", originalRefreshToken))
                .andExpect(status().isOk())
                .andReturn();

        String refreshResponse = refreshResult.getResponse().getContentAsString();
        JsonNode jsonNode = objectMapper.readTree(refreshResponse);
        String newAccessToken = jsonNode.path("body").path("token").asText();
        String newRefreshToken = jsonNode.path("body").path("refreshToken").asText();

        assertNotNull(newAccessToken);
        assertNotNull(newRefreshToken);
        assertNotEquals(originalRefreshToken, newRefreshToken);
        assertFalse(refreshTokenService.isRefreshTokenValid("rotateuser", originalRefreshToken));
        assertTrue(refreshTokenService.isRefreshTokenValid("rotateuser", newRefreshToken));
    }

    @Test
    @DisplayName("3. 旋转发生后，尝试重放/重用旧的 Refresh Token 应被拒绝并返回 401")
    void testRefresh_ReplayAttackShouldBeBlocked() throws Exception {
        String[] tokens = loginAndGetTokens("replayuser");
        String originalRefreshToken = tokens[1];

        Thread.sleep(1100);

        mockMvc.perform(post("/v1/auth/refresh")
                        .param("refreshToken", originalRefreshToken))
                .andExpect(status().isOk());

        mockMvc.perform(post("/v1/auth/refresh")
                        .param("refreshToken", originalRefreshToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.result").value("1"))
                .andExpect(jsonPath("$.message").value("Refresh token has been revoked or is invalid"));
    }

    @Test
    @DisplayName("4. 用户退出登录时，名下的所有 Refresh Token 均应被吊销")
    void testLogout_ShouldRevokeAllRefreshTokens() throws Exception {
        String[] tokens = loginAndGetTokens("logoutuser");
        String accessToken = tokens[0];
        String refreshToken = tokens[1];

        mockMvc.perform(post("/v1/user/logout")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());

        assertFalse(refreshTokenService.isRefreshTokenValid("logoutuser", refreshToken));

        mockMvc.perform(post("/v1/auth/refresh")
                        .param("refreshToken", refreshToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("5. 修改密码成功后，原所有的 Refresh Token 均应被吊销")
    void testChangePassword_ShouldRevokeAllRefreshTokens() throws Exception {
        String[] tokens = loginAndGetTokens("passuser");
        String accessToken = tokens[0];
        String refreshToken = tokens[1];

        ChangePasswordRequest changePasswordRequest = new ChangePasswordRequest();
        changePasswordRequest.setUserId(String.valueOf(testUser.getId()));
        changePasswordRequest.setOldPassword("password123");
        changePasswordRequest.setNewPassword("newPassword123");

        mockMvc.perform(post("/v1/user/change-password")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(changePasswordRequest)))
                .andExpect(status().isOk());

        assertFalse(refreshTokenService.isRefreshTokenValid("passuser", refreshToken));

        mockMvc.perform(post("/v1/auth/refresh")
                        .param("refreshToken", refreshToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("6. 账户注销(注销)成功后，名下所有的 Refresh Token 均应被吊销")
    void testDeleteAccount_ShouldRevokeAllRefreshTokens() throws Exception {
        String[] tokens = loginAndGetTokens("deleteuser");
        String accessToken = tokens[0];
        String refreshToken = tokens[1];

        mockMvc.perform(delete("/v1/user/delete-account")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());

        assertFalse(refreshTokenService.isRefreshTokenValid("deleteuser", refreshToken));

        mockMvc.perform(post("/v1/auth/refresh")
                        .param("refreshToken", refreshToken))
                .andExpect(status().isUnauthorized());
    }
}
