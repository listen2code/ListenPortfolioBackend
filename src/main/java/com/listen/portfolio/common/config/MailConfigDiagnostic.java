package com.listen.portfolio.common.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

/**
 * 邮件配置诊断工具
 * 说明：显示当前邮件配置状态，便于调试
 * 使用场景：启动时查看配置是否正确加载
 */
@Configuration
public class MailConfigDiagnostic {

    private static final Logger logger = LoggerFactory.getLogger(MailConfigDiagnostic.class);

    // Spring Boot 邮件配置
    @Value("${spring.mail.host}")
    private String springMailHost;
    
    @Value("${spring.mail.port}")
    private String springMailPort;
    
    @Value("${spring.mail.username}")
    private String springMailUsername;
    
    @Value("${spring.mail.password}")
    private String springMailPassword;

    // 直接从 .env 文件读取
    @Value("${MAIL_HOST:smtp.gmail.com}")
    private String envMailHost;
    
    @Value("${MAIL_PORT:587}")
    private String envMailPort;
    
    @Value("${MAIL_USERNAME:your-email@gmail.com}")
    private String envMailUsername;
    
    @Value("${MAIL_PASSWORD:your-app-password}")
    private String envMailPassword;

    @PostConstruct
    public void diagnoseMailConfiguration() {
        logger.info("=== Mail Configuration Diagnostic Report ===");
        
        // 1. 检查 Spring Boot 配置
        logger.info("--- Spring Boot Mail Configuration ---");
        logger.info("spring.mail.host: {}", springMailHost);
        logger.info("spring.mail.port: {}", springMailPort);
        logger.info("spring.mail.username: {}", springMailUsername);
        logPassword("spring.mail.password", springMailPassword);
        
        // 2. 检查环境变量
        logger.info("--- Environment Variables Configuration ---");
        logger.info("MAIL_HOST: {}", envMailHost);
        logger.info("MAIL_PORT: {}", envMailPort);
        logger.info("MAIL_USERNAME: {}", envMailUsername);
        logPassword("MAIL_PASSWORD", envMailPassword);
        
        // 3. 检查系统环境变量
        logger.info("--- System Environment Variables ---");
        logger.info("System.getenv(MAIL_HOST): {}", System.getenv("MAIL_HOST"));
        logger.info("System.getenv(MAIL_PORT): {}", System.getenv("MAIL_PORT"));
        logger.info("System.getenv(MAIL_USERNAME): {}", System.getenv("MAIL_USERNAME"));
        logPassword("System.getenv(MAIL_PASSWORD)", System.getenv("MAIL_PASSWORD"));
        
        // 4. 检查系统属性
        logger.info("--- System Properties ---");
        logger.info("System.getProperty(MAIL_HOST): {}", System.getProperty("MAIL_HOST"));
        logger.info("System.getProperty(MAIL_PORT): {}", System.getProperty("MAIL_PORT"));
        logger.info("System.getProperty(MAIL_USERNAME): {}", System.getProperty("MAIL_USERNAME"));
        logPassword("System.getProperty(MAIL_PASSWORD)", System.getProperty("MAIL_PASSWORD"));
        
        // 5. 配置一致性检查
        logger.info("--- Configuration Consistency Check ---");
        checkConsistency("MAIL_HOST", envMailHost, System.getProperty("MAIL_HOST"));
        checkConsistency("MAIL_PORT", envMailPort, System.getProperty("MAIL_PORT"));
        checkConsistency("MAIL_USERNAME", envMailUsername, System.getProperty("MAIL_USERNAME"));
        checkConsistency("MAIL_PASSWORD", envMailPassword, System.getProperty("MAIL_PASSWORD"));
        
        // 6. 密码格式验证
        logger.info("--- Password Format Validation ---");
        validatePasswordFormat(envMailPassword);
        
        // 7. 最终诊断结果
        logger.info("--- Diagnostic Results ---");
        provideDiagnosis();
        
        logger.info("========================");
    }
    
    private void logPassword(String key, String password) {
        if (password != null) {
            String masked = password.substring(0, Math.min(4, password.length())) + "****";
            logger.info("{}: {} (length: {})", key, masked, password.length());
        } else {
            logger.info("{}: null", key);
        }
    }
    
    /**
     * 检查配置一致性
     * 说明：比较 Spring 配置与系统环境变量是否一致
     */
    private void checkConsistency(String key, String springValue, String systemValue) {
        boolean consistent = (springValue == null && systemValue == null) || 
                           (springValue != null && springValue.equals(systemValue));
        
        if (consistent) {
            logger.info("✅ {}: Spring config consistent with system property", key);
        } else {
            logger.warn("❌ {}: Spring config inconsistent with system property", key);
            logger.warn("   Spring: {}", springValue);
            logger.warn("   System: {}", systemValue);
        }
    }
    
    /**
     * 验证密码格式
     * 说明：检查密码是否符合 Gmail 应用专用密码格式
     */
    private void validatePasswordFormat(String password) {
        if (password == null) {
            logger.error("❌ Password is null");
            return;
        }
        
        if (password.length() != 16) {
            logger.error("❌ Password length incorrect: {} (expected: 16)", password.length());
            return;
        }
        
        if (!password.matches("[a-z]{16}")) {
            logger.error("❌ Password format incorrect: {} (expected: 16 lowercase letters)", password);
            return;
        }
        
        logger.info("✅ Password format correct: 16 lowercase letters");
    }
    
    /**
     * 提供诊断结果
     * 说明：根据配置检查结果提供诊断建议
     */
    private void provideDiagnosis() {
        boolean springConfigOk = springMailHost != null && springMailPort != null && 
                               springMailUsername != null && springMailPassword != null;
        
        boolean envConfigOk = envMailHost != null && envMailPort != null && 
                             envMailUsername != null && envMailPassword != null;
        
        boolean systemPropertyOk = System.getProperty("MAIL_HOST") != null && 
                                 System.getProperty("MAIL_PORT") != null && 
                                 System.getProperty("MAIL_USERNAME") != null && 
                                 System.getProperty("MAIL_PASSWORD") != null;
        
        if (springConfigOk && envConfigOk && systemPropertyOk) {
            logger.info("🎉 All configurations loaded successfully!");
            logger.info("   - Spring Boot config: ✅");
            logger.info("   - Environment variables: ✅");
            logger.info("   - System properties: ✅");
            logger.info("   Email sending should work perfectly!");
        } else {
            logger.warn("⚠️ Configuration issues found:");
            if (!springConfigOk) logger.warn("   - Spring Boot config incomplete");
            if (!envConfigOk) logger.warn("   - Environment variables config incomplete");
            if (!systemPropertyOk) logger.warn("   - System properties incomplete");
            
            logger.info("💡 Check your .env file and PortfolioApplication.loadDotenv()");
        }
    }
}
