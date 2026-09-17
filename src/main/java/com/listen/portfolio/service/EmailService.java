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
 * 邮件通信与模板渲染核心服务 (Email Service)
 *
 * <h3>架构设计与职责说明：</h3>
 * <ul>
 *   <li><b>SMTP 协议客户端抽象：</b>
 *       底层封装 Spring Framework 官方提供的 {@link JavaMailSender}，通过 SMTP/STARTTLS 加密协议
 *       与外部邮件服务器（如 Gmail、QQ、163、AWS SES 等）建立安全网络信道。</li>
 *   <li><b>动态 HTML 邮件渲染：</b>
 *       整合 {@link TemplateEngine} (Thymeleaf)，将业务数据（用户名、重置链接、时效等）以 {@link Context}
 *       上下文变量注入预编译的 HTML 模板，实现现代响应式、品牌化的 HTML 邮件推送。</li>
 *   <li><b>防御式参数校验：</b>
 *       在进入耗时的 SMTP 握手前，提前在应用层对收件人地址格式、邮件主题与邮件正文进行非空与基本格式检查，
 *       避免无效网络 I/O 开销与垃圾邮件陷阱。</li>
 *   <li><b>故障隔离与异常传播：</b>
 *       捕获邮件组装与网络投递阶段的 {@link MessagingException}，记录详细错误上下文并向上层 Service 抛出，
 *       便于业务层决定是静默失败（防探测）还是记录审计。</li>
 * </ul>
 */
@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    /** Spring Boot 自动装配的邮件发送器组件（基于 Jakarta Mail） */
    @Autowired
    private JavaMailSender mailSender;

    /** Thymeleaf 模板引擎，用于将 HTML 模板文件与上下文动态变量融合渲染 */
    @Autowired
    private TemplateEngine templateEngine;

    /** 发件人邮箱地址，从 application.properties 中的 spring.mail.username 注入 */
    @Value("${spring.mail.username}")
    private String fromEmail;

    /** 前端 Web 应用的基础访问 URL，用于拼接邮件内的跳转链接 */
    @Value("${app.frontend.url}")
    private String frontendUrl;

    /**
     * 邮件基础参数防御式校验。
     *
     * <p>在发起网络连接前执行内存级别快速校验，拦截非法入参，保护底层网络 I/O 资源。
     *
     * @param toEmail 收件人邮箱地址
     * @param subject 邮件主题
     * @param content 邮件正文（纯文本或 HTML 片段）
     * @throws IllegalArgumentException 当任何必填参数为空或收件人邮箱格式不合法时抛出
     */
    private void validateEmailParameters(String toEmail, String subject, String content) {
        if (toEmail == null || toEmail.trim().isEmpty()) {
            throw new IllegalArgumentException("收件人邮箱不能为空");
        }
        
        if (subject == null || subject.trim().isEmpty()) {
            throw new IllegalArgumentException("邮件主题不能为空");
        }
        
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("邮件内容不能为空");
        }
        
        // 基础邮箱格式有效性粗检（必须包含 '@' 与 '.'）
        if (!toEmail.contains("@") || !toEmail.contains(".")) {
            throw new IllegalArgumentException("邮箱格式无效");
        }
    }

    /**
     * 发送密码重置专用 HTML 邮件。
     *
     * <h3>实现流程与关键细节：</h3>
     * <ol>
     *   <li><b>拼接前端重置页面 URL：</b>
     *       基于配置的基础路径（{@code frontendUrl}）拼接重置页面静态路径与 Query 参数：
     *       {@code {frontendUrl}/password-reset-out-email.html?token={token}}。</li>
     *   <li><b>构造 MIME 邮件容器：</b>
     *       通过 {@link JavaMailSender#createMimeMessage()} 创建标准的 MIME 协议多部件邮件，
     *       并使用 {@link MimeMessageHelper} 开启 UTF-8 编码与 HTML 内容支持。</li>
     *   <li><b>设置发件人、收件人与主题：</b>
     *       发件人采用配置绑定的官方服务邮箱（{@code fromEmail}），主题注明项目标识便于用户邮件归类。</li>
     *   <li><b>Thymeleaf 模板上下文填充与动态渲染：</b>
     *       构建 {@link Context} 实例，填入 {@code username}、{@code resetLink}、{@code token}
     *       与 {@code expirationTime}（当前预设为 1 小时），定位并解析模板文件
     *       {@code src/main/resources/templates/email/password-reset-in-email.html}，产出最终 HTML 字符串。</li>
     *   <li><b>SMTP 投递与错误处理：</b>
     *       通过底层网络调用 {@link JavaMailSender#send(MimeMessage)} 传输报文；
     *       若发生网络中断或认证失败则记录错误日志并重新抛出 {@link MessagingException}。</li>
     * </ol>
     *
     * @param toEmail  接收邮件的目标用户邮箱
     * @param username 目标用户的用户名（用于邮件称谓个性化展示）
     * @param token    由 CSPRNG 生成的 256 位高熵一次性重置令牌
     * @throws MessagingException 当邮件构建不合法或外部 SMTP 服务器投递失败时抛出
     */
    public void sendPasswordResetEmail(String toEmail, String username, String token) throws MessagingException {
        logger.info(">>> [EmailService] 准备向用户 [{}] 发送密码重置邮件, 邮箱: {}", username, toEmail);

        try {
            // 1. 构建前端密码重置页面的可访问完整链接（携带一次性安全 Token）
            String resetLink = frontendUrl + "/password-reset-out-email.html?token=" + token;
            
            // 2. 初始化 MIME 协议复合邮件消息与助手类（声明 UTF-8 避免中文乱码）
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            // 3. 配置 SMTP 协议标头信息
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("密码重置请求 - Portfolio");

            // 4. 将业务变量装配至 Thymeleaf Context 上下文环境
            Context context = new Context();
            context.setVariable("username", username);
            context.setVariable("resetLink", resetLink);
            context.setVariable("token", token);
            context.setVariable("expirationTime", "1小时");

            // 5. 调用模板引擎合并渲染 Thymeleaf 模板（对应 classpath:templates/email/password-reset-in-email.html）
            String htmlContent = templateEngine.process("email/password-reset-in-email", context);
            helper.setText(htmlContent, true);

            // 6. 执行远程 SMTP 发送
            mailSender.send(message);
            logger.info(">>> [EmailService] 密码重置邮件发送成功, 收件人: {}", toEmail);

        } catch (MessagingException e) {
            logger.error(">>> [EmailService] 发送密码重置邮件失败, 收件人: {}, 错误信息: {}", toEmail, e.getMessage());
            throw e;
        }
    }

    /**
     * 发送简单纯文本邮件 (Plain Text Email)。
     *
     * <p>适用于系统运行预警、运维通知、验证码下发等轻量级场景，开销低且无须加载 HTML 解析引擎。
     *
     * @param toEmail 收件人邮箱地址
     * @param subject 邮件主题
     * @param content 纯文本正文内容
     * @throws MessagingException 邮件网络投递失败时抛出
     */
    public void sendSimpleEmail(String toEmail, String subject, String content) throws MessagingException {
        logger.info(">>> [EmailService] 准备发送纯文本邮件, 收件人: {}, 主题: {}", toEmail, subject);

        // 前置参数格式与非空校验
        validateEmailParameters(toEmail, subject, content);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            // 第二个参数传 false 声明为纯文本模式
            helper.setText(content, false);

            mailSender.send(message);
            logger.info(">>> [EmailService] 纯文本邮件发送成功, 收件人: {}", toEmail);

        } catch (MessagingException e) {
            logger.error(">>> [EmailService] 发送纯文本邮件失败, 收件人: {}, 错误信息: {}", toEmail, e.getMessage());
            throw e;
        }
    }

    /**
     * 发送通用富文本 HTML 邮件 (Custom HTML Email)。
     *
     * <p>支持调用方直接传入完整的自定义 HTML 字符串，适用于自定义通知、公告发布或外部生成的 HTML 报告。
     *
     * @param toEmail     收件人邮箱地址
     * @param subject     邮件主题
     * @param htmlContent 包含 HTML 标签的富文本正文内容
     * @throws MessagingException 邮件网络投递失败时抛出
     */
    public void sendHtmlEmail(String toEmail, String subject, String htmlContent) throws MessagingException {
        logger.info(">>> [EmailService] 准备发送通用 HTML 邮件, 收件人: {}, 主题: {}", toEmail, subject);

        // 前置参数格式与非空校验
        validateEmailParameters(toEmail, subject, htmlContent);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            // 第二个参数传 true 声明为富文本 HTML 模式，邮件客户端将渲染样式与标签
            helper.setText(htmlContent, true);

            mailSender.send(message);
            logger.info(">>> [EmailService] 通用 HTML 邮件发送成功, 收件人: {}", toEmail);

        } catch (MessagingException e) {
            logger.error(">>> [EmailService] 发送通用 HTML 邮件失败, 收件人: {}, 错误信息: {}", toEmail, e.getMessage());
            throw e;
        }
    }
}
