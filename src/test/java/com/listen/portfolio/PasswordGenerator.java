package com.listen.portfolio;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * 密码生成工具
 * 
 * 说明：生成 BCrypt 加密密码用于测试
 * 使用：运行此工具生成加密密码
 */
public class PasswordGenerator {
    
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        
        // 常用测试密码
        String[] passwords = {
            "password",
            "123456", 
            "admin",
            "listen123",
            "test123"
        };
        
        System.out.println("=== BCrypt 密码生成器 ===");
        System.out.println();
        
        for (String password : passwords) {
            String encoded = encoder.encode(password);
            System.out.println("明文密码: " + password);
            System.out.println("加密密码: " + encoded);
            System.out.println("验证结果: " + encoder.matches(password, encoded));
            System.out.println();
        }
        
        // 生成特定密码
        String specificPassword = "listen123";
        String specificEncoded = encoder.encode(specificPassword);
        System.out.println("=== 推荐的 Listen 用户密码 ===");
        System.out.println("明文密码: " + specificPassword);
        System.out.println("加密密码: " + specificEncoded);
        System.out.println();
        
        // 验证现有密码
        String existingHash = "$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iYqiSfFDYZt/I5/BFnhkSLsVBDSC";
        System.out.println("=== 验证现有密码 ===");
        System.out.println("验证 'password': " + encoder.matches("password", existingHash));
        System.out.println("验证 'listen123': " + encoder.matches("listen123", existingHash));
        System.out.println("验证 '123456': " + encoder.matches("123456", existingHash));
    }
}
