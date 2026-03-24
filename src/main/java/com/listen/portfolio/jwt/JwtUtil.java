package com.listen.portfolio.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * JWT 工具类，用于生成、验证和从 JWT 令牌中提取信息。
 * 此组件处理应用程序中所有与 JWT 相关的操作。
 */
@Component
public class JwtUtil {

    // 日志记录器，用于记录 JWT 操作
    private static final Logger logger = LoggerFactory.getLogger(JwtUtil.class);

    // 从 application.properties 注入 jwt.secret（例如："my-secret-key-12345"）
    @Value("${jwt.secret}")
    private String secret;

    // 从 application.properties 注入 jwt.expiration（例如：86400000 表示 24 小时，以毫秒为单位）
    @Value("${jwt.expiration}")
    private Long expiration;
        
    @Value("${jwt.refresh-expiration}")
    private Long refreshExpiration;

    /**
     * 从 UserDetails 对象生成 JWT 令牌。
     * 这在登录时调用，为已认证用户创建新令牌。
     * 
     * @param userDetails 包含用户名的已认证用户详情
     * @return 签名的 JWT 令牌字符串
     */
    public String generateToken(UserDetails userDetails) {
        logger.info("开始为用户 {} 生成 JWT 令牌", userDetails.getUsername());
        
        // 初始化空的 claims 映射以存储额外信息（如果需要）
        Map<String, Object> claims = new HashMap<>();
        
        // 使用用户名作为主题创建并返回令牌
        String token = createToken(claims, userDetails.getUsername(), new Date(System.currentTimeMillis() + expiration));
        logger.info("成功为用户 {} 生成 JWT 令牌", userDetails.getUsername());
        return token;
    }

    /**
     * 使用 claims 和主题创建 JWT 令牌。
     * 这是构建和签名 JWT 令牌的核心方法。
     * 
     * @param claims 要包含在令牌中的自定义 claims 映射
     * @param subject 要设置为令牌主题的用户名
     * @return 紧凑的、签名的 JWT 令牌
     */
    private String createToken(Map<String, Object> claims, String subject, Date expirationDate) {
        logger.info("创建 JWT 令牌，主题: {}", subject);
        
        // 使用 HMAC SHA 算法从密钥字符串生成安全密钥
        // 此密钥用于签名令牌
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes());
        
        // 构建并返回 JWT 令牌
        String token = Jwts.builder()
                // 将任何自定义 claims（负载数据）添加到令牌
                .setClaims(claims)
                // 将用户名设置为令牌的主题
                .setSubject(subject)
                // 将令牌创建时间设置为当前时间
                .setIssuedAt(new Date(System.currentTimeMillis()))
                // 设置令牌过期时间（当前时间 + 过期持续时间）
                .setExpiration(expirationDate)
                // 使用生成的密钥以 HS256 算法（HMAC SHA-256）签名令牌
                .signWith(key, SignatureAlgorithm.HS256)
                // 将令牌序列化并压缩为紧凑字符串格式
                .compact();
        
        logger.info("JWT 令牌创建完成，主题: {}", subject);
        return token;
    }

    /**
     * 通过检查用户名是否匹配且令牌未过期来验证 JWT 令牌。
     * 
     * @param token 要验证的 JWT 令牌字符串
     * @param userDetails 要与令牌验证的用户详情
     * @return 如果令牌有效则返回 true，否则返回 false
     */
    public Boolean validateToken(String token, UserDetails userDetails) {
        logger.info("开始验证 JWT 令牌，用户: {}", userDetails.getUsername());
        
        try {
            // 从令牌中提取用户名
            final String username = extractUsername(token);
            
            // 检查提取的用户名是否与提供的用户匹配且令牌未过期
            boolean isValid = (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
            
            if (isValid) {
                logger.info("JWT 令牌验证成功，用户: {}", username);
            } else {
                logger.warn("JWT 令牌验证失败，用户: {}", username);
            }
            
            return isValid;
        } catch (Exception e) {
            logger.error("JWT 令牌验证过程中发生错误: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 从 JWT 令牌中提取用户名（主题）。
     * 
     * @param token JWT 令牌字符串
     * @return 存储在令牌主题字段中的用户名
     */
    public String extractUsername(String token) {
        logger.info("从 JWT 令牌中提取用户名");
        // 从令牌中提取主题 claim（即用户名）
        String username = extractClaim(token, Claims::getSubject);
        logger.info("提取到用户名: {}", username);
        return username;
    }

    /**
     * 从 JWT 令牌中提取过期日期。
     * 
     * @param token JWT 令牌字符串
     * @return 令牌的过期日期
     */
    public Date extractExpiration(String token) {
        logger.info("从 JWT 令牌中提取过期日期");
        // 从令牌中提取过期 claim
        Date expirationDate = extractClaim(token, Claims::getExpiration);
        logger.info("提取到过期日期: {}", expirationDate);
        return expirationDate;
    }

    /**
     * 检查令牌是否已过期。
     * 
     * @param token JWT 令牌字符串
     * @return 如果令牌已过期则返回 true，否则返回 false
     */
    public Boolean isTokenExpired(String token) {
        logger.info("检查 JWT 令牌是否过期");
        // 获取当前日期
        final Date currentDate = new Date();
        // 获取令牌过期日期
        final Date expirationDate = extractExpiration(token);
        // 比较当前日期是否在过期日期之后
        boolean isExpired = expirationDate.before(currentDate);
        
        if (isExpired) {
            logger.warn("JWT 令牌已过期，过期时间: {}", expirationDate);
        } else {
            logger.info("JWT 令牌未过期，过期时间: {}", expirationDate);
        }
        
        return isExpired;
    }

    /**
     * 使用自定义解析器函数从 JWT 令牌中提取任何 claim。
     * 可以提取令牌中任何 claim 的通用方法。
     * 
     * @param token JWT 令牌字符串
     * @param claimsResolver 指定要提取哪个 claim 的函数
     * @param <T> 要提取的 claim 的类型
     * @return 提取的 claim 值
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        logger.info("从 JWT 令牌中提取自定义 claim");
        // 从令牌获取所有 claims
        final Claims claims = extractAllClaims(token);
        // 应用解析器函数以提取特定 claim
        T claimValue = claimsResolver.apply(claims);
        logger.info("提取到 claim 值: {}", claimValue);
        return claimValue;
    }


    public String generateRefreshToken(String token) {
        logger.info("开始为用户 {} 生成 JWT 刷新令牌", token);
        
        // 从令牌中提取用户名
        String userName = extractUsername(token);
        logger.info("从令牌提取到用户名: {}", userName);
        
        // 初始化空的 claims 映射以存储额外信息（如果需要）
        Map<String, Object> claims = new HashMap<>();
        
        // 使用用户名作为主题创建并返回刷新令牌
        // 刷新令牌过期时间更长，例如 7 天
        String refreshToken = createToken(claims, userName, new Date(System.currentTimeMillis() + refreshExpiration)); 
        logger.info("成功为用户 {} 生成 JWT 刷新令牌", userName);
        return refreshToken;
    }

    /**
     * 从 JWT 令牌中提取并解析所有 claims。
     * 此方法解码令牌并验证其签名。
     * 
     * @param token JWT 令牌字符串
     * @return 包含所有令牌数据的 Claims 对象
     * @throws io.jsonwebtoken.JwtException 如果令牌无效或签名验证失败
     */
    private Claims extractAllClaims(String token) {
        logger.info("从 JWT 令牌中提取所有 claims");
        
        try {
            // 生成用于签名令牌的相同安全密钥
            SecretKey key = Keys.hmacShaKeyFor(secret.getBytes());
            
            // 解析并验证令牌
            Claims claims = Jwts.parserBuilder()
                    // 设置签名密钥以进行签名验证
                    .setSigningKey(key)
                    // 构建解析器
                    .build()
                    // 解析令牌并验证签名
                    // 如果签名不匹配（令牌被篡改），这将抛出异常
                    .parseClaimsJws(token)
                    // 从解析的令牌中提取主体（claims）
                    .getBody();
            
            logger.info("成功提取所有 claims");
            return claims;
        } catch (Exception e) {
            logger.error("提取 JWT 令牌 claims 时发生错误: {}", e.getMessage());
            throw e; // 重新抛出异常，让调用者处理
        }
    }

    public String refreshToken(String refreshToken) {
        logger.info("刷新 JWT 令牌，刷新令牌: {}", refreshToken);
        
        try {
            // 从刷新令牌中提取用户名
            String username = extractUsername(refreshToken);
            logger.info("从刷新令牌提取到用户名: {}", username);
            
            // 这里可以添加额外的验证，例如检查刷新令牌是否在数据库中有效
            
            // 创建新的访问令牌
            Map<String, Object> claims = new HashMap<>();
            String newToken = createToken(claims, username, new Date(System.currentTimeMillis() + expiration));
            logger.info("成功刷新 JWT 令牌，用户: {}", username);
            return newToken;
        } catch (Exception e) {
            logger.error("刷新 JWT 令牌失败: {}", e.getMessage());
            throw e; // 重新抛出异常，让调用者处理
        }
    }
}