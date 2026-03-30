package com.listen.portfolio.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

/**
 * 邮件服务
 * 
 * 说明：提供邮件发送功能，支持 HTML 邮件和模板渲染
 * 使用场景：密码重置邮件、验证邮件、通知邮件等
 */
@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private TemplateEngine templateEngine;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    /**
     * 发送密码重置邮件
     * 
     * 说明：向用户发送包含密码重置链接的邮件
     * 
     * @param toEmail 收件人邮箱
     * @param resetToken 密码重置 Token
     * @param username 用户名
     * @throws MessagingException 邮件发送失败时抛出
     */
    public void sendPasswordResetEmail(String toEmail, String resetToken, String username) throws MessagingException {
        logger.info("准备发送密码重置邮件到: {}", toEmail);

        try {
            // 创建邮件消息
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            // 设置邮件基本信息
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("密码重置请求 - Portfolio");

            // 生成重置链接
            String resetLink = frontendUrl + "/password-reset-out-email.html?token=" + resetToken;

            // 使用 Thymeleaf 模板渲染邮件内容
            Context context = new Context();
            context.setVariable("username", username);
            context.setVariable("resetLink", resetLink);
            context.setVariable("token", resetToken);
            context.setVariable("expirationTime", "1小时");

            // 读取模板文件：src/main/resources/templates/email/password-reset-in-email.html
            // 将 context 中的变量（如 username、resetLink、token 等）替换到模板中
            // 生成最终的 HTML 内容
            String htmlContent = templateEngine.process("email/password-reset-in-email", context);
            helper.setText(htmlContent, true);

            // 发送邮件
            mailSender.send(message);
            logger.info("密码重置邮件发送成功到: {}", toEmail);

        } catch (MessagingException e) {
            logger.error("发送密码重置邮件失败到: {}, 错误: {}", toEmail, e.getMessage());
            throw e;
        }
    }

    /**
     * 发送简单文本邮件
     * 
     * 说明：发送纯文本邮件
     * 
     * @param toEmail 收件人邮箱
     * @param subject 邮件主题
     * @param content 邮件内容
     * @throws MessagingException 邮件发送失败时抛出
     */
    public void sendSimpleEmail(String toEmail, String subject, String content) throws MessagingException {
        logger.info("准备发送邮件到: {}, 主题: {}", toEmail, subject);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(content, false);

            mailSender.send(message);
            logger.info("邮件发送成功到: {}", toEmail);

        } catch (MessagingException e) {
            logger.error("发送邮件失败到: {}, 错误: {}", toEmail, e.getMessage());
            throw e;
        }
    }

    /**
     * 发送 HTML 邮件
     * 
     * 说明：发送 HTML 格式的邮件
     * 
     * @param toEmail 收件人邮箱
     * @param subject 邮件主题
     * @param htmlContent HTML 内容
     * @throws MessagingException 邮件发送失败时抛出
     */
    public void sendHtmlEmail(String toEmail, String subject, String htmlContent) throws MessagingException {
        logger.info("准备发送 HTML 邮件到: {}, 主题: {}", toEmail, subject);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            logger.info("HTML 邮件发送成功到: {}", toEmail);

        } catch (MessagingException e) {
            logger.error("发送 HTML 邮件失败到: {}, 错误: {}", toEmail, e.getMessage());
            throw e;
        }
    }
}
