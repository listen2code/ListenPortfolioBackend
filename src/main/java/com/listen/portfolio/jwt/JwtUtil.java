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
 * 
 * 说明：
 * - 提供JWT令牌的完整生命周期管理：生成、验证、解析、刷新
 * - 支持访问令牌和刷新令牌的生成与验证
 * - 使用HMAC SHA-256算法进行签名，确保令牌安全性
 * - 密钥和过期时间通过配置文件注入，便于环境隔离
 * 
 * 设计原则：
 * - 所有敏感信息（如token内容）不在日志中输出，防止信息泄露
 * - 提供详细的错误日志，便于问题排查
 * - 支持令牌刷新机制，提升用户体验
 * 
 * 安全性：
 * - 使用强密钥进行签名，防止令牌伪造
 * - 支持令牌过期机制，降低令牌泄露风险
 * - 刷新令牌具有更长的有效期，减少频繁登录
 */
@Component
public class JwtUtil {

    private static final Logger logger = LoggerFactory.getLogger(JwtUtil.class);

    /**
     * JWT签名密钥，从配置文件中注入
     * 
     * 说明：
     * - 用于对JWT进行签名的密钥，确保令牌不被篡改
     * - 不同环境应使用不同的密钥，生产环境密钥应足够复杂
     * - 密钥长度建议至少256位，满足HMAC SHA-256算法要求
     */
    @Value("${jwt.secret}")
    private String secret;

    /**
     * 访问令牌过期时间（毫秒）
     * 
     * 说明：
     * - 控制访问令牌的有效期，默认建议设置为15-30分钟
     * - 较短的有效期可以降低令牌被盗用的风险
     * - 过期后需要使用刷新令牌获取新的访问令牌
     */
    @Value("${jwt.expiration}")
    private Long expiration;
    
    /**
     * 刷新令牌过期时间（毫秒）
     * 
     * 说明：
     * - 控制刷新令牌的有效期，默认建议设置为7-30天
     * - 较长的有效期避免频繁重新登录，提升用户体验
     * - 刷新令牌用于获取新的访问令牌，不应频繁使用
     */
    @Value("${jwt.refresh-expiration}")
    private Long refreshExpiration;

    /**
     * 从 UserDetails 对象生成 JWT 访问令牌。
     * 在用户成功登录时调用，用于创建短期有效的访问令牌。
     * 
     * 说明：
     * - 基于用户详情生成包含用户身份的JWT令牌
     * - 令牌包含签发时间和过期时间，确保时效性
     * - 使用配置文件中指定的过期时间，支持不同环境的灵活配置
     * 
     * @param userDetails 包含用户名的已认证用户详情
     * @return 签名的 JWT 访问令牌字符串
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
     * 创建 JWT 令牌的核心方法。
     * 负责构建和签名 JWT 令牌，支持自定义 claims 和过期时间。
     * 
     * 说明：
     * - 使用 HMAC SHA-256 算法对令牌进行签名，确保数据完整性
     * - 支持自定义 claims，可扩展存储用户角色、权限等信息
     * - 设置合理的签发时间和过期时间，防止令牌永久有效
     * - 使用配置的安全密钥，支持环境变量注入
     * 
     * @param claims 要包含在令牌中的自定义 claims 映射
     * @param subject 要设置为令牌主题的用户名
     * @param expirationDate 令牌过期时间
     * @return 紧凑的、签名的 JWT 令牌
     */
    private String createToken(Map<String, Object> claims, String subject, Date expirationDate) {
        logger.debug("创建 JWT 令牌，主题: {}", subject);
        
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
        
        logger.debug("JWT 令牌创建完成，主题: {}", subject);
        return token;
    }

/**
     * 验证 JWT 访问令牌的有效性。
     * 通过检查用户名匹配和令牌未过期来确认令牌合法。
     * 
     * 说明：
     * - 提取令牌中的用户名，与提供的用户详情进行比对
     * - 检查令牌是否已过期，过期令牌将被拒绝
     * - 捕获解析异常，处理格式错误或签名无效的令牌
     * - 提供详细的验证日志，便于安全审计和问题排查
     * 
     * 安全性：
     * - 防止伪造令牌：通过签名验证确保令牌来源可信
     * - 防止过期令牌：严格检查令牌有效期
     * - 防止用户冒充：验证令牌中的用户身份
     * 
     * @param token 要验证的 JWT 令牌字符串
     * @param userDetails 要与令牌验证的用户详情
     * @return 如果令牌有效则返回 true，否则返回 false
     */
    public Boolean validateToken(String token, UserDetails userDetails) {
        logger.debug("开始验证 JWT 令牌，用户: {}", userDetails.getUsername());
        
        try {
            // 从令牌中提取用户名
            final String username = extractUsername(token);
            
            // 检查提取的用户名是否与提供的用户匹配且令牌未过期
            boolean isValid = (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
            
            if (isValid) {
                logger.debug("JWT 令牌验证成功，用户: {}", username);
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
     * 用于获取令牌中存储的用户身份信息。
     * 
     * 说明：
     * - 解析JWT令牌的标准claims，提取sub(subject)字段
     * - 该字段在生成令牌时设置为用户名，用于身份标识
     * - 是令牌验证和刷新的基础方法
     * 
     * @param token JWT 令牌字符串
     * @return 存储在令牌主题字段中的用户名
     */
    public String extractUsername(String token) {
        String username = extractClaim(token, Claims::getSubject);
        logger.debug("从 JWT 令牌中提取到用户名: {}", username);
        return username;
    }

/**
     * 从 JWT 令牌中提取过期日期。
     * 用于检查令牌的生命周期状态。
     * 
     * 说明：
     * - 解析JWT令牌的标准claims，提取exp(expiration)字段
     * - 该字段决定令牌的有效期，是令牌验证的重要依据
     * - 与当前时间比较可判断令牌是否已过期
     * 
     * @param token JWT 令牌字符串
     * @return 令牌的过期日期
     */
    public Date extractExpiration(String token) {
        Date expirationDate = extractClaim(token, Claims::getExpiration);
        logger.debug("从 JWT 令牌中提取到过期日期: {}", expirationDate);
        return expirationDate;
    }

/**
     * 检查JWT令牌是否已过期
     * 
     * 说明：
     * - 通过比较当前时间与令牌过期时间，判断令牌是否失效
     * - 如果当前时间在过期时间之后，则令牌已过期
     * - 这是令牌验证的重要步骤，确保令牌仍在有效期内
     * - 过期检查与用户名验证共同确保令牌有效性
     * 
     * 判断逻辑：
     * - 获取当前系统时间作为参考点
     * - 从令牌中提取过期时间
     * - 比较当前时间是否在过期时间之后
     * - 如果是，则令牌已过期；否则令牌仍有效
     * 
     * @param token JWT 令牌字符串
     * @return 如果令牌已过期则返回 true，否则返回 false
     */
    public Boolean isTokenExpired(String token) {
        // 获取当前日期
        final Date currentDate = new Date();
        // 获取令牌过期日期
        final Date expirationDate = extractExpiration(token);
        // 比较当前日期是否在过期日期之后
        boolean isExpired = expirationDate.before(currentDate);
        
        if (isExpired) {
            logger.warn("JWT 令牌已过期，过期时间: {}", expirationDate);
        }
        
        return isExpired;
    }

/**
     * 从 JWT 令牌中提取指定类型的声明（通用提取方法）
     * 
     * 说明：
     * - 这是提取JWT声明的通用模板方法，支持提取任意类型的声明
     * - 使用Function函数式接口，可以传入不同的声明提取器
     * - 先提取所有声明，再应用特定的提取函数获取目标声明
     * - 支持提取标准声明（sub、iat、exp等）和自定义声明
     * 
     * 使用示例：
     * - extractClaim(token, Claims::getSubject) 提取用户名
     * - extractClaim(token, Claims::getExpiration) 提取过期时间
     * - extractClaim(token, claims -> claims.get("custom", String.class)) 提取自定义声明
     * 
     * @param token JWT 令牌字符串
     * @param claimsResolver 指定要提取哪个 claim 的函数
     * @param <T> 要提取的 claim 的类型
     * @return 提取的 claim 值
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        // 从令牌获取所有 claims
        final Claims claims = extractAllClaims(token);
        // 应用解析器函数以提取特定 claim
        T claimValue = claimsResolver.apply(claims);
        return claimValue;
    }


/**
     * 从访问令牌生成刷新令牌
     * 
     * 说明：
     * - 在用户登录成功后，同时生成访问令牌和刷新令牌
     * - 刷新令牌用于在访问令牌过期后获取新的访问令牌
     * - 刷新令牌具有更长的有效期，减少用户重新登录的频率
     * - 从当前有效的访问令牌中提取用户名，生成对应的刷新令牌
     * 
     * 安全考虑：
     * - 不在日志中记录任何令牌内容，防止敏感信息泄露
     * - 使用与访问令牌相同的用户名，确保一致性
     * - 刷新令牌的过期时间配置更长，但需要定期验证有效性
     * 
     * 使用场景：
     * - 用户首次登录时，同时生成访问令牌和刷新令牌
     * - 访问令牌过期时，使用刷新令牌获取新的访问令牌
     * - 实现无状态认证的同时，提供令牌刷新机制
     * 
     * @param token 当前有效的访问令牌
     * @return 新的刷新令牌字符串
     */
    public String generateRefreshToken(String token) {
        logger.debug("Starting to generate JWT refresh token");
        // 注意：不要在日志中输出任何 access/refresh token 内容，避免泄露
        logger.debug("No token content will be logged during refresh token generation");
        
        // 从令牌中提取用户名
        String userName = extractUsername(token);
        logger.debug("Extracted username from access token: {}", userName);
        
        // 初始化空的 claims 映射以存储额外信息（如果需要）
        Map<String, Object> claims = new HashMap<>();
        
        // 使用用户名作为主题创建并返回刷新令牌
        // 刷新令牌过期时间更长，例如 7 天
        String refreshToken = createToken(claims, userName, new Date(System.currentTimeMillis() + refreshExpiration)); 
        logger.debug("Successfully generated JWT refresh token for user: {}", userName);
        return refreshToken;
    }

/**
     * 从 JWT 令牌中提取并解析所有 claims。
     * 负责解码令牌内容并验证其数字签名。
     * 
     * 说明：
     * - 使用与签名时相同的密钥解析和验证令牌
     * - 验证令牌的数字签名，确保数据未被篡改
     * - 提取令牌中的所有 claims，包括标准claims和自定义claims
     * - 如果签名验证失败，会抛出JwtException异常
     * 
     * 安全性：
     * - 签名验证：确保令牌来源可信，防止伪造
     * - 完整性检查：检测令牌内容是否被修改
     * - 密钥验证：使用正确的密钥进行签名验证
     * 
     * @param token JWT 令牌字符串
     * @return 包含所有令牌数据的 Claims 对象
     * @throws io.jsonwebtoken.JwtException 如果令牌无效或签名验证失败
     */
    private Claims extractAllClaims(String token) {
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
            
            return claims;
        } catch (Exception e) {
            logger.error("提取 JWT 令牌 claims 时发生错误: {}", e.getMessage());
            throw e; // 重新抛出异常，让调用者处理
        }
    }

/**
     * 使用刷新令牌生成新的访问令牌。
     * 实现无感刷新的核心方法，提升用户体验。
     * 
     * 说明：
     * - 从刷新令牌中提取用户名，验证刷新令牌的有效性
     * - 生成新的访问令牌，延长用户的登录状态
     * - 支持在访问令牌过期后无缝续期，避免频繁登录
     * - 可扩展支持刷新令牌的额外验证（如数据库状态检查）
     * 
     * 安全性：
     * - 刷新令牌验证：确保只有合法的刷新令牌才能生成新令牌
     * - 用户身份一致性：新令牌的用户身份与刷新令牌保持一致
     * - 令牌有效期控制：新令牌具有独立的过期时间设置
     * 
     * 使用场景：
     * - 访问令牌即将过期时的自动续期
     * - 访问令牌已过期但刷新令牌仍有效时的恢复
     * - 移动端应用的后台静默刷新
     * 
     * @param refreshToken 用于生成新访问令牌的刷新令牌
     * @return 新的 JWT 访问令牌
     * @throws Exception 如果刷新令牌无效或已过期
     */
    public String refreshToken(String refreshToken) {
        logger.debug("Refreshing JWT token");
        
        try {
            // 从刷新令牌中提取用户名
            String username = extractUsername(refreshToken);
            logger.debug("Extracted username from refresh token: {}", username);
            
            // 这里可以添加额外的验证，例如检查刷新令牌是否在数据库中有效
            
            // 创建新的访问令牌
            Map<String, Object> claims = new HashMap<>();
            String newToken = createToken(claims, username, new Date(System.currentTimeMillis() + expiration));
            logger.debug("Successfully refreshed JWT token for user: {}", username);
            return newToken;
        } catch (Exception e) {
            logger.error("Failed to refresh JWT token: {}", e.getMessage());
            throw e; // 重新抛出异常，让调用者处理
        }
    }
}