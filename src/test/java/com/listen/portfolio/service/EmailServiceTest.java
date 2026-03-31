package com.listen.portfolio.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.test.util.ReflectionTestUtils;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * EmailService 单元测试
 * 
 * 说明：测试邮件服务的所有核心功能
 * 目的：确保邮件发送、模板渲染、异常处理功能正常工作
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EmailService Unit Tests")
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private MimeMessage mimeMessage;

    @Mock
    private MimeMessageHelper messageHelper;

    @Mock
    private TemplateEngine templateEngine;

    @InjectMocks
    private EmailService emailService;

    @BeforeEach
    void setUp() throws IOException {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        // 设置测试用的 fromEmail
        ReflectionTestUtils.setField(emailService, "fromEmail", "test@example.com");
        ReflectionTestUtils.setField(emailService, "frontendUrl", "http://localhost:3000");
    }

    @Test
    @DisplayName("发送 HTML 邮件 - 成功")
    void testSendHtmlEmail_Success() throws MessagingException {
        // Given
        String to = "test@example.com";
        String subject = "测试邮件";
        String htmlContent = "<h1>测试内容</h1>";

        // When
        assertDoesNotThrow(() -> emailService.sendHtmlEmail(to, subject, htmlContent));

        // Then
        verify(mailSender).send(mimeMessage);
    }

    @Test
    @DisplayName("发送简单文本邮件 - 成功")
    void testSendSimpleEmail_Success() throws MessagingException {
        // Given
        String to = "test@example.com";
        String subject = "测试邮件";
        String content = "这是一封测试邮件";

        // When
        assertDoesNotThrow(() -> emailService.sendSimpleEmail(to, subject, content));

        // Then
        verify(mailSender).send(mimeMessage);
    }

    @Test
    @DisplayName("发送密码重置邮件 - 成功")
    void testSendPasswordResetEmail_Success() throws MessagingException {
        // Given
        String to = "user@example.com";
        String username = "testuser";
        String resetLink = "http://localhost:8080/reset?token=abc123";

        when(templateEngine.process(eq("email/password-reset-in-email"), any(Context.class)))
                .thenReturn("<h1>密码重置</h1><p>你好 testuser</p><a href=\"http://localhost:8080/reset?token=abc123\">重置密码</a>");

        // When
        assertDoesNotThrow(() -> emailService.sendPasswordResetEmail(to, username, resetLink));

        // Then
        verify(templateEngine).process(eq("email/password-reset-in-email"), any(Context.class));
        verify(mailSender).send(mimeMessage);
    }

    @Test
    @DisplayName("发送 HTML 邮件 - MessagingException 异常处理")
    void testSendHtmlEmail_MessagingException() throws MessagingException {
        // Given
        String to = "test@example.com";
        String subject = "测试邮件";
        String htmlContent = "<h1>测试内容</h1>";

        doThrow(new RuntimeException("邮件发送失败")).when(mailSender).send(mimeMessage);

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> 
            emailService.sendHtmlEmail(to, subject, htmlContent)
        );
        assertEquals("邮件发送失败", exception.getMessage());
    }

    @Test
    @DisplayName("发送简单文本邮件 - MessagingException 异常处理")
    void testSendSimpleEmail_MessagingException() throws MessagingException {
        // Given
        String to = "test@example.com";
        String subject = "测试邮件";
        String content = "这是一封测试邮件";

        doThrow(new RuntimeException("邮件发送失败")).when(mailSender).send(mimeMessage);

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> 
            emailService.sendSimpleEmail(to, subject, content)
        );
        assertEquals("邮件发送失败", exception.getMessage());
    }

    @Test
    @DisplayName("发送密码重置邮件 - 模板引擎异常")
    void testSendPasswordResetEmail_TemplateEngineException() throws MessagingException {
        // Given
        String to = "test@example.com";
        String username = "testuser";
        String resetLink = "http://localhost:8080/reset?token=abc123";

        when(templateEngine.process(eq("email/password-reset-in-email"), any(Context.class)))
                .thenThrow(new RuntimeException("模板渲染失败"));

        // When & Then
        Exception exception = assertThrows(RuntimeException.class, () -> 
            emailService.sendPasswordResetEmail(to, username, resetLink)
        );
        assertEquals("模板渲染失败", exception.getMessage());
    }

    @Test
    @DisplayName("邮件地址验证 - 空地址")
    void testSendHtmlEmail_EmptyAddress() {
        // Given
        String to = "";
        String subject = "测试邮件";
        String htmlContent = "<h1>测试内容</h1>";

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> 
            emailService.sendHtmlEmail(to, subject, htmlContent)
        );
        assertTrue(exception.getMessage().contains("邮件地址不能为空"));
    }

    @Test
    @DisplayName("邮件地址验证 - 无效格式")
    void testSendHtmlEmail_InvalidAddress() {
        // Given
        String to = "invalid-email";
        String subject = "测试邮件";
        String htmlContent = "<h1>测试内容</h1>";

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> 
            emailService.sendHtmlEmail(to, subject, htmlContent)
        );
        assertTrue(exception.getMessage().contains("邮件地址格式无效"));
    }

    @Test
    @DisplayName("邮件内容验证 - 空内容")
    void testSendHtmlEmail_EmptyContent() {
        // Given
        String to = "test@example.com";
        String subject = "测试邮件";
        String htmlContent = "";

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> 
            emailService.sendHtmlEmail(to, subject, htmlContent)
        );
        assertTrue(exception.getMessage().contains("邮件内容不能为空"));
    }

    @Test
    @DisplayName("批量发送邮件 - 成功")
    void testSendBulkEmails_Success() throws MessagingException {
        // Given
        String[] recipients = {"user1@example.com", "user2@example.com", "user3@example.com"};
        String subject = "批量测试";
        String htmlContent = "<h1>批量内容</h1>";

        // When
        for (String recipient : recipients) {
            assertDoesNotThrow(() -> emailService.sendHtmlEmail(recipient, subject, htmlContent));
        }

        // Then
        verify(mailSender, times(3)).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("批量发送邮件 - 部分失败")
    void testSendBulkEmails_PartialFailure() throws MessagingException {
        // Given
        String[] recipients = {"user1@example.com", "user2@example.com", "user3@example.com"};
        String subject = "批量测试";
        String htmlContent = "<h1>批量内容</h1>";

        doThrow(new RuntimeException("发送失败"))
                .doNothing()
                .doNothing()
                .when(mailSender).send(any(MimeMessage.class));

        // When
        int successCount = 0;
        int failureCount = 0;
        
        for (String recipient : recipients) {
            try {
                emailService.sendHtmlEmail(recipient, subject, htmlContent);
                successCount++;
            } catch (RuntimeException e) {
                failureCount++;
            }
        }

        // Then
        assertEquals(2, successCount, "应该有 2 个邮件发送成功");
        assertEquals(1, failureCount, "应该有 1 个邮件发送失败");
        verify(mailSender, times(3)).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("邮件模板变量验证")
    void testSendPasswordResetEmail_VariableValidation() throws MessagingException {
        // Given
        String to = "test@example.com";
        String username = "testuser";
        String resetLink = "http://localhost:8080/reset?token=abc123";

        when(templateEngine.process(eq("email/password-reset-in-email"), any(Context.class)))
                .thenReturn("<h1>你好 ${username}</h1><a href=\"${resetLink}\">重置密码</a>");

        // When
        assertDoesNotThrow(() -> emailService.sendPasswordResetEmail(to, username, resetLink));

        // Then
        verify(templateEngine).process(eq("email/password-reset-in-email"), any(Context.class));
        verify(mailSender).send(mimeMessage);
    }

    @Test
    @DisplayName("邮件主题验证 - 空主题")
    void testSendHtmlEmail_EmptySubject() {
        // Given
        String to = "test@example.com";
        String subject = "";
        String htmlContent = "<h1>测试内容</h1>";

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> 
            emailService.sendHtmlEmail(to, subject, htmlContent)
        );
        assertTrue(exception.getMessage().contains("邮件主题不能为空"));
    }

    @Test
    @DisplayName("邮件主题验证 - null 主题")
    void testSendHtmlEmail_NullSubject() {
        // Given
        String to = "test@example.com";
        String subject = null;
        String htmlContent = "<h1>测试内容</h1>";

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> 
            emailService.sendHtmlEmail(to, subject, htmlContent)
        );
        assertTrue(exception.getMessage().contains("邮件主题不能为空"));
    }

    @Test
    @DisplayName("邮件内容长度验证 - 极长内容")
    void testSendHtmlEmail_VeryLongContent() throws MessagingException {
        // Given
        String to = "test@example.com";
        String subject = "测试邮件";
        StringBuilder htmlContent = new StringBuilder();
        for (int i = 0; i < 10000; i++) {
            htmlContent.append("<p>这是第").append(i).append("行内容</p>");
        }

        // When
        assertDoesNotThrow(() -> emailService.sendHtmlEmail(to, subject, htmlContent.toString()));

        // Then
        verify(mailSender).send(mimeMessage);
    }

    @Test
    @DisplayName("邮件编码验证 - UTF-8 编码")
    void testSendHtmlEmail_UTF8Encoding() throws MessagingException {
        // Given
        String to = "test@example.com";
        String subject = "测试邮件 - 中文";
        String htmlContent = "<h1>中文内容测试</h1><p>支持中文、日本語、한글</p>";

        // When
        assertDoesNotThrow(() -> emailService.sendHtmlEmail(to, subject, htmlContent));

        // Then
        verify(mailSender).send(mimeMessage);
    }

    @Test
    @DisplayName("邮件发送器创建 MimeMessage 验证")
    void testSendHtmlEmail_MimeMessageCreation() throws MessagingException {
        // Given
        String to = "test@example.com";
        String subject = "测试邮件";
        String htmlContent = "<h1>测试内容</h1>";

        // When
        emailService.sendHtmlEmail(to, subject, htmlContent);

        // Then
        verify(mailSender, times(1)).createMimeMessage();
        verify(mailSender).send(mimeMessage);
    }
}
