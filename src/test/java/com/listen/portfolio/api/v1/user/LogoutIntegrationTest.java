package com.listen.portfolio.api.v1.user;

import com.listen.portfolio.common.jwt.JwtUtil;
import com.listen.portfolio.integration.BaseIntegrationTest;
import com.listen.portfolio.service.TokenBlacklistService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Logout 功能集成测试
 * 
 * 说明：测试完整的 logout 流程，包括 token 黑名单机制
 * 目的：验证 JWT token 生成、黑名单添加和检查功能
 */
public class LogoutIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private JwtUtil jwtUtil;
    
    @Autowired
    private TokenBlacklistService tokenBlacklistService;

    @Test
    void testCompleteLogoutFlow() {
        // 1. 创建测试用户
        UserDetails userDetails = User.withUsername("testuser")
                .password("password")
                .roles("USER")
                .build();
        
        // 2. 生成 JWT token
        String token = jwtUtil.generateToken(userDetails);
        assertNotNull(token, "JWT token 生成失败");
        
        System.out.println("✅ JWT token 生成成功: " + token.substring(0, Math.min(20, token.length())) + "...");
        
        // 3. 验证 token 有效性
        assertTrue(jwtUtil.validateToken(token, userDetails), "JWT token 验证失败");
        assertFalse(tokenBlacklistService.isBlacklisted(token), "Token 不应该在黑名单中");
        
        System.out.println("✅ JWT token 验证通过，不在黑名单中");
        
        // 4. 模拟 logout：将 token 加入黑名单
        long expirationTime = jwtUtil.extractExpiration(token).getTime();
        tokenBlacklistService.addToBlacklist(token, expirationTime);
        
        // 5. 验证 token 已在黑名单中
        assertTrue(tokenBlacklistService.isBlacklisted(token), "Token 应该在黑名单中");
        
        System.out.println("✅ Token 已成功加入黑名单");
        
        // 6. 验证 token 仍然有效（JWT 本身没过期），但被黑名单拒绝
        assertTrue(jwtUtil.validateToken(token, userDetails), "JWT token 本身仍然有效");
        
        System.out.println("✅ JWT token 本身仍然有效，但被黑名单机制拦截");
        
        // 7. 清理测试数据
        tokenBlacklistService.removeFromBlacklist(token);
        assertFalse(tokenBlacklistService.isBlacklisted(token), "Token 已从黑名单中移除");
        
        System.out.println("✅ 测试数据清理完成");
        System.out.println("🎉 完整的 Logout 流程测试通过！");
    }
    
    @Test
    void testTokenExpirationAndBlacklist() {
        // 测试 token 过期时间和黑名单 TTL 的计算
        UserDetails userDetails = User.withUsername("testuser2")
                .password("password")
                .roles("USER")
                .build();
        
        // 生成 token
        String token = jwtUtil.generateToken(userDetails);
        long expirationTime = jwtUtil.extractExpiration(token).getTime();
        long currentTime = System.currentTimeMillis();
        
        // 验证过期时间合理（应该在当前时间 + 5分钟左右）
        long expectedExpiration = currentTime + 300000; // 5分钟
        assertTrue(Math.abs(expirationTime - expectedExpiration) < 10000, 
                  "Token 过期时间不合理");
        
        System.out.println("✅ Token 过期时间验证通过");
        
        // 将 token 加入黑名单
        tokenBlacklistService.addToBlacklist(token, expirationTime);
        
        // 验证黑名单记录存在
        assertTrue(tokenBlacklistService.isBlacklisted(token), "Token 应该在黑名单中");
        
        // 清理
        tokenBlacklistService.removeFromBlacklist(token);
        
        System.out.println("✅ Token 过期时间和黑名单 TTL 测试通过");
    }
}
