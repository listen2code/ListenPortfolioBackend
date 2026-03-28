package com.listen.portfolio.api.v1.user;

import com.listen.portfolio.jwt.JwtUtil;
import com.listen.portfolio.service.TokenBlacklistService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Logout Token 提取测试
 * 
 * 说明：测试改进后的 token 提取逻辑
 * 目的：验证从 HTTP 请求头和 SecurityContext 中提取 token 的功能
 */
@SpringBootTest
@ActiveProfiles("test")
public class LogoutTokenExtractionTest {

    @Autowired
    private UserController userController;
    
    @Autowired
    private JwtUtil jwtUtil;
    
    @Autowired
    private TokenBlacklistService tokenBlacklistService;

    @Test
    void testTokenExtractionFromAuthorizationHeader() {
        // 1. 创建测试用户和 token
        UserDetails userDetails = User.withUsername("testuser")
                .password("password")
                .roles("USER")
                .build();
        
        String token = jwtUtil.generateToken(userDetails);
        assertNotNull(token, "JWT token 生成失败");
        
        // 2. 模拟 HTTP 请求
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        
        // 3. 设置请求上下文
        ServletRequestAttributes attributes = new ServletRequestAttributes(request);
        RequestContextHolder.setRequestAttributes(attributes);
        
        // 4. 设置认证上下文
        Authentication authentication = org.springframework.security.authentication.UsernamePasswordAuthenticationToken
                .authenticated(token, null, null);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        
        System.out.println("✅ Token: " + token.substring(0, Math.min(20, token.length())) + "...");
        
        // 5. 测试 logout（应该能成功提取 token）
        var response = userController.logout();
        
        // 6. 验证响应
        assertEquals(200, response.getStatusCode().value(), "Logout 应该成功");
        assertEquals("Logout successful", response.getBody().getMessage(), "响应消息应该正确");
        
        // 7. 验证 token 在黑名单中
        assertTrue(tokenBlacklistService.isBlacklisted(token), "Token 应该在黑名单中");
        
        // 8. 清理
        tokenBlacklistService.removeFromBlacklist(token);
        RequestContextHolder.resetRequestAttributes();
        SecurityContextHolder.clearContext();
        
        System.out.println("✅ 从 Authorization header 提取 token 测试通过");
    }
    
    @Test
    void testTokenExtractionFromSecurityContext() {
        // 1. 创建测试用户和 token
        UserDetails userDetails = User.withUsername("testuser2")
                .password("password")
                .roles("USER")
                .build();
        
        String token = jwtUtil.generateToken(userDetails);
        assertNotNull(token, "JWT token 生成失败");
        
        // 2. 设置认证上下文（模拟 credentials 中有 token）
        Authentication authentication = org.springframework.security.authentication.UsernamePasswordAuthenticationToken
                .authenticated(token, token, null); // credentials 就是 token
        SecurityContextHolder.getContext().setAuthentication(authentication);
        
        System.out.println("✅ Token: " + token.substring(0, Math.min(20, token.length())) + "...");
        
        // 3. 测试 logout（应该能从 SecurityContext 提取 token）
        var response = userController.logout();
        
        // 4. 验证响应
        assertEquals(200, response.getStatusCode().value(), "Logout 应该成功");
        assertEquals("Logout successful", response.getBody().getMessage(), "响应消息应该正确");
        
        // 5. 验证 token 在黑名单中
        assertTrue(tokenBlacklistService.isBlacklisted(token), "Token 应该在黑名单中");
        
        // 6. 清理
        tokenBlacklistService.removeFromBlacklist(token);
        SecurityContextHolder.clearContext();
        
        System.out.println("✅ 从 SecurityContext 提取 token 测试通过");
    }
    
    @Test
    void testTokenExtractionFailure() {
        // 1. 清空所有上下文
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();
        
        // 2. 测试 logout（应该无法提取 token）
        var response = userController.logout();
        
        // 3. 验证响应（即使没有 token 也应该返回成功）
        assertEquals(200, response.getStatusCode().value(), "即使没有 token，logout 也应该返回成功");
        assertEquals("Logout successful", response.getBody().getMessage(), "响应消息应该正确");
        
        System.out.println("✅ 无 token 情况下的 logout 测试通过");
    }
}
