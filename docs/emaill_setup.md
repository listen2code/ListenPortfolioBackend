# 📧 邮件服务完整指南

## 概述

本项目实现了完整的邮件服务系统，支持多种 SMTP 提供商、HTML 模板渲染、密码重置等功能。邮件服务基于 Spring Boot Mail 和 Thymeleaf 模板引擎构建，提供企业级的邮件发送能力。

## 🎯 核心功能

### 已实现功能
- ✅ **多 SMTP 提供商支持**：Gmail、QQ、163、Outlook 等
- ✅ **HTML 邮件模板**：使用 Thymeleaf 渲染精美邮件
- ✅ **密码重置流程**：完整的重置链接生成和验证
- ✅ **STARTTLS 加密**：安全的邮件传输
- ✅ **连接池管理**：优化的连接复用
- ✅ **异常处理**：完善的错误处理和日志记录

### 邮件类型
- **密码重置邮件**：包含重置链接和有效期说明
- **验证邮件**：用户注册验证（可扩展）
- **通知邮件**：系统通知和提醒（可扩展）
- **营销邮件**：产品推广和活动通知（可扩展）

## 🏗️ 架构设计

### 核心组件

```
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│   Controller    │    │   EmailService   │    │  JavaMailSender │
│   控制器层       │───▶│   业务逻辑层      │───▶│   邮件发送器     │
└─────────────────┘    └──────────────────┘    └─────────────────┘
                                │
                                ▼
                       ┌─────────────────┐
                       │ TemplateEngine │
                       │   模板引擎       │
                       └─────────────────┘
                                │
                                ▼
                       ┌─────────────────┐
                       │   Templates     │
                       │   邮件模板       │
                       └─────────────────┘
```

### 服务依赖

- **EmailService**：核心邮件服务
- **TemplateEngine**：Thymeleaf 模板引擎
- **JavaMailSender**：Spring Mail 发送器
- **PasswordResetTokenService**：重置 Token 管理

## ⚙️ 配置详解

### 基础配置

在 `application.properties` 中配置 SMTP 参数：

```properties
# SMTP 服务器配置
spring.mail.host=${MAIL_HOST:smtp.gmail.com}
spring.mail.port=${MAIL_PORT:587}
spring.mail.username=${MAIL_USERNAME:your-email@gmail.com}
spring.mail.password=${MAIL_PASSWORD:your-app-password}

# 协议和加密
spring.mail.protocol=smtp
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
spring.mail.properties.mail.smtp.starttls.required=true

# 连接超时配置
spring.mail.properties.mail.smtp.connectiontimeout=5000
spring.mail.properties.mail.smtp.timeout=5000
spring.mail.properties.mail.smtp.writetimeout=5000

# 前端 URL（用于生成重置链接）
app.frontend.url=${FRONTEND_URL:http://localhost:8080}

# Token 有效期
app.password-reset.token-expiration=${PASSWORD_RESET_TOKEN_EXPIRATION:3600}
```

### 环境变量配置

```bash
# Gmail 配置
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=your-app-password  # Gmail 应用专用密码

# QQ 邮箱配置
MAIL_HOST=smtp.qq.com
MAIL_PORT=587
MAIL_USERNAME=your-qq@qq.com
MAIL_PASSWORD=your-authorization-code  # QQ 授权码

# 前端应用地址
FRONTEND_URL=http://localhost:3000

# Token 过期时间（秒）
PASSWORD_RESET_TOKEN_EXPIRATION=3600
```

## 🚀 快速开始

### 1. 配置 SMTP 提供商

#### Gmail 配置
```bash
# 1. 开启两步验证
# 2. 生成应用专用密码
# 3. 配置环境变量
export MAIL_HOST=smtp.gmail.com
export MAIL_PORT=587
export MAIL_USERNAME=your-email@gmail.com
export MAIL_PASSWORD=your-app-password
```

#### QQ 邮箱配置
```bash
# 1. 登录 QQ 邮箱 → 设置 → 账户
# 2. 开启 POP3/SMTP/IMAP/Exchange/CardDAV/CalDAV 服务
# 3. 生成授权码
export MAIL_HOST=smtp.qq.com
export MAIL_PORT=587
export MAIL_USERNAME=your-qq@qq.com
export MAIL_PASSWORD=your-authorization-code
```

### 2. 启动 Redis

邮件服务依赖 Redis 存储 Token：
```bash
docker-compose up -d redis
```

### 3. 测试邮件发送

```bash
# 启动应用
./mvnw spring-boot:run

# 发送测试请求
curl -X POST http://localhost:8080/v1/auth/forgot-password \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com"}'
```

## 📧 邮件模板

### 密码重置模板

位置：`src/main/resources/templates/email/password-reset-in-email.html`

#### 模板特性
- **响应式设计**：支持移动端和桌面端
- **现代化 UI**：渐变背景、卡片布局
- **安全提醒**：包含安全建议和有效期说明
- **品牌化**：可自定义 Logo 和品牌色

#### 模板变量

| 变量名 | 类型 | 说明 |
|--------|------|------|
| `${username}` | String | 用户名 |
| `${resetLink}` | String | 重置链接 |
| `${token}` | String | 重置 Token |
| `${expirationTime}` | String | 有效期描述 |

#### 模板结构

```html
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>密码重置</title>
    <style>
        /* 响应式样式 */
    </style>
</head>
<body>
    <div class="container">
        <div class="header">
            <h1>密码重置请求</h1>
        </div>
        <div class="content">
            <p>你好 <strong>${username}</strong>，</p>
            <p>我们收到了您的密码重置请求。</p>
            <a href="${resetLink}" class="button">重置密码</a>
            <p>此链接将在 <strong>${expirationTime}</strong> 后失效。</p>
        </div>
        <div class="footer">
            <p>如果这不是您的请求，请忽略此邮件。</p>
        </div>
    </div>
</body>
</html>
```

## 🔧 开发指南

### 添加新的邮件类型

#### 1. 创建邮件模板

```html
<!-- src/main/resources/templates/email/welcome.html -->
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>欢迎注册</title>
</head>
<body>
    <h1>欢迎 <span th:text="${username}">用户</span>！</h1>
    <p>感谢您注册我们的服务。</p>
</body>
</html>
```

#### 2. 扩展 EmailService

```java
@Service
public class EmailService {
    
    /**
     * 发送欢迎邮件
     */
    public void sendWelcomeEmail(String toEmail, String username) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom(fromEmail);
        helper.setTo(toEmail);
        helper.setSubject("欢迎注册 Portfolio");

        // 渲染模板
        Context context = new Context();
        context.setVariable("username", username);
        String htmlContent = templateEngine.process("email/welcome", context);
        
        helper.setText(htmlContent, true);
        mailSender.send(message);
    }
}
```

#### 3. 在业务逻辑中调用

```java
@Service
public class AuthService {
    
    public boolean signUp(SignUpRequest signUpRequest) {
        // 注册逻辑
        if (success) {
            try {
                emailService.sendWelcomeEmail(user.getEmail(), user.getName());
            } catch (MessagingException e) {
                logger.error("Failed to send welcome email", e);
            }
        }
        return success;
    }
}
```

### 自定义邮件模板

#### 使用 Thymeleaf 高级功能

```html
<!-- 条件渲染 -->
<div th:if="${user.isPremium}">
    <p>您是高级用户，享受专属权益！</p>
</div>

<!-- 循环渲染 -->
<ul>
    <li th:each="feature : ${features}" th:text="${feature}">功能</li>
</ul>

<!-- 链接生成 -->
<a th:href="@{https://app.example.com/profile(id=${userId})}">个人资料</a>

<!-- 日期格式化 -->
<p th:text="${#dates.format(createdAt, 'yyyy-MM-dd HH:mm')}">2024-03-31 20:15</p>
```

## 🔍 故障排除

### 常见问题

#### 1. 邮件发送失败

**症状**：日志显示 "发送密码重置邮件失败"

**排查步骤**：
```bash
# 1. 检查 SMTP 配置
echo $MAIL_HOST
echo $MAIL_USERNAME
echo $MAIL_PASSWORD

# 2. 测试 SMTP 连接
telnet smtp.gmail.com 587

# 3. 查看应用日志
docker-compose logs -f app | grep "EmailService"
```

**解决方案**：
- 确认 SMTP 服务器地址和端口正确
- 验证邮箱密码/授权码有效
- 检查网络连接和防火墙设置

#### 2. 邮件未收到

**症状**：接口返回成功，但邮箱未收到邮件

**可能原因**：
- 邮件被归类为垃圾邮件
- 邮箱地址错误
- 邮件服务器延迟

**解决方案**：
```bash
# 1. 检查垃圾邮件文件夹
# 2. 确认邮箱地址拼写
# 3. 等待几分钟后检查
# 4. 查看邮件发送日志
```

#### 3. 模板渲染错误

**症状**：邮件内容异常或模板变量未替换

**排查步骤**：
```java
// 检查模板文件是否存在
Resource resource = resourceLoader.getResource("classpath:templates/email/password-reset-in-email.html");
boolean exists = resource.exists();

// 检查模板变量
Context context = new Context();
context.setVariable("username", "test");
String result = templateEngine.process("email/password-reset-in-email", context);
```

### 调试技巧

#### 1. 启用邮件调试

```properties
# 启用 JavaMail 调试
logging.level.javax.mail=DEBUG
logging.level.com.sun.mail=DEBUG
```

#### 2. 使用 MailHog 测试

```bash
# 启动 MailHog（邮件测试工具）
docker run -d -p 1025:1025 -p 8025:8025 mailhog/mailhog

# 配置应用使用 MailHog
export MAIL_HOST=localhost
export MAIL_PORT=1025

# 访问 Web UI 查看邮件
open http://localhost:8025
```

#### 3. 模板渲染测试

```java
@Test
public void testEmailTemplate() {
    Context context = new Context();
    context.setVariable("username", "测试用户");
    context.setVariable("resetLink", "http://localhost:8080/reset?token=abc123");
    
    String result = templateEngine.process("email/password-reset-in-email", context);
    assertThat(result).contains("测试用户");
    assertThat(result).contains("http://localhost:8080/reset?token=abc123");
}
```

## 📊 监控与日志

### 日志记录

邮件服务会记录详细的操作日志：

```log
2024-03-31 20:15:30.123 INFO  --- EmailService : 准备发送密码重置邮件到: user@example.com
2024-03-31 20:15:30.456 INFO  --- EmailService : 密码重置邮件发送成功到: user@example.com
2024-03-31 20:15:31.789 ERROR --- EmailService : 发送密码重置邮件失败到: user@example.com, 错误: Authentication failed
```

### 监控指标

可以添加邮件相关的监控指标：

```java
@Component
public class EmailMetrics {
    
    private final Counter emailSentCounter = Counter.builder("email_sent_total")
        .description("Total number of emails sent")
        .register(Metrics.globalRegistry);
    
    private final Counter emailFailedCounter = Counter.builder("email_failed_total")
        .description("Total number of failed emails")
        .register(Metrics.globalRegistry);
    
    public void recordSent() {
        emailSentCounter.increment();
    }
    
    public void recordFailed() {
        emailFailedCounter.increment();
    }
}
```

## 🔒 安全考虑

### 1. 敏感信息保护

- **密码保护**：SMTP 密码通过环境变量注入
- **Token 安全**：重置 Token 使用加密随机数生成
- **日志脱敏**：不在日志中记录敏感信息

### 2. 防止滥用

- **发送频率限制**：结合限流系统防止邮件轰炸
- **邮箱验证**：发送前验证邮箱格式有效性
- **异常处理**：统一的异常处理，不泄露系统信息

### 3. 传输安全

- **STARTTLS 加密**：强制启用 TLS 加密传输
- **证书验证**：验证 SMTP 服务器 SSL 证书
- **连接超时**：设置合理的连接和读写超时

## 🎯 最佳实践

### 1. 邮件内容设计

- **移动优先**：确保在移动设备上显示良好
- **品牌一致性**：使用统一的品牌色彩和 Logo
- **行动导向**：明确的行动按钮和链接
- **加载优化**：控制图片大小和数量

### 2. 发送策略

- **批量发送**：对于大量邮件使用队列异步发送
- **重试机制**：网络异常时自动重试
- **发送时间**：考虑用户时区，避免深夜发送

### 3. 性能优化

- **连接池**：复用 SMTP 连接
- **模板缓存**：缓存编译后的模板
- **异步发送**：使用 @Async 异步发送邮件

## 🔮 扩展计划

### 短期计划
- [ ] 邮件队列系统
- [ ] 邮件模板管理界面
- [ ] 发送统计分析
- [ ] A/B 测试支持

### 长期计划
- [ ] 多语言邮件支持
- [ ] 个性化邮件内容
- [ ] 邮件营销自动化
- [ ] 第三方邮件服务集成

---

## 📚 相关文档

- [Spring Boot Mail 文档](https://docs.spring.io/spring-boot/docs/current/reference/html/io.html#io.email)
- [Thymeleaf 模板文档](https://www.thymeleaf.org/doc/tutorials/3.0/usingthymeleaf.html)
- [Gmail SMTP 配置](https://support.google.com/mail/answer/7126229)
- [QQ 邮箱 SMTP 设置](https://service.mail.qq.com/cgi-bin/help?subtype=1&&id=28&&no=1001256)

---

**最后更新**: 2026-03-31  
**维护者**: Development Team

---

## 常用 SMTP 服务提供商配置

### 1. Gmail

```bash
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=your-app-password
```

**重要提示**：
- Gmail 需要使用"应用专用密码"，不能使用账号密码
- 生成应用专用密码：Google 账号 → 安全性 → 两步验证 → 应用专用密码

### 2. QQ 邮箱

```bash
MAIL_HOST=smtp.qq.com
MAIL_PORT=587
MAIL_USERNAME=your-qq@qq.com
MAIL_PASSWORD=your-authorization-code
```

**重要提示**：
- QQ 邮箱需要使用"授权码"，不能使用 QQ 密码
- 获取授权码：QQ 邮箱 → 设置 → 账户 → POP3/IMAP/SMTP/Exchange/CardDAV/CalDAV服务 → 生成授权码

### 3. 163 邮箱

```bash
MAIL_HOST=smtp.163.com
MAIL_PORT=465
MAIL_USERNAME=your-email@163.com
MAIL_PASSWORD=your-authorization-code
```

### 4. Outlook/Hotmail

```bash
MAIL_HOST=smtp.office365.com
MAIL_PORT=587
MAIL_USERNAME=your-email@outlook.com
MAIL_PASSWORD=your-password
```

### 5. 阿里云企业邮箱

```bash
MAIL_HOST=smtp.mxhichina.com
MAIL_PORT=465
MAIL_USERNAME=your-email@your-domain.com
MAIL_PASSWORD=your-password
```

## Docker Compose 配置

在 `docker-compose.yml` 中添加环境变量：

```yaml
services:
  app:
    environment:
      - MAIL_HOST=smtp.gmail.com
      - MAIL_PORT=587
      - MAIL_USERNAME=${MAIL_USERNAME}
      - MAIL_PASSWORD=${MAIL_PASSWORD}
      - FRONTEND_URL=${FRONTEND_URL}
      - PASSWORD_RESET_TOKEN_EXPIRATION=3600
```

然后在 `.env` 文件中配置：

```bash
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=your-app-password
FRONTEND_URL=http://localhost:3000
```

## API 使用

### 忘记密码接口

**请求**：

```http
POST /v1/auth/forgot-password
Content-Type: application/json

{
  "email": "user@example.com"
}
```

**响应**：

成功（200）：
```json
{
  "result": "0",
  "messageId": "",
  "message": "",
  "body": null
}
```

失败（400）：
```json
{
  "result": "1",
  "messageId": "1",
  "message": "Password change failed",
  "body": null
}
```

### 工作流程

1. 用户在前端输入邮箱，调用 `/v1/auth/forgot-password` 接口
2. 后端生成安全的重置 Token（32 字节随机数）
3. Token 存储到 Redis，设置过期时间（默认 1 小时）
4. 发送包含重置链接的 HTML 邮件到用户邮箱
5. 用户点击邮件中的链接，跳转到前端重置密码页面
6. 前端提交新密码和 Token 到后端
7. 后端验证 Token，更新密码，删除 Token

## 邮件模板

邮件模板位于 `src/main/resources/templates/email/password-reset-in-email.html`。

模板特性：
- 响应式设计，支持移动端和桌面端
- 渐变色背景，现代化 UI
- 包含重置按钮和备用链接
- 安全提醒和有效期说明
- 品牌化页脚

模板变量：
- `${username}` - 用户名
- `${resetLink}` - 重置链接
- `${expirationTime}` - 有效期（如 "1小时"）

## 测试邮件发送

### 本地测试

1. 配置环境变量
2. 启动 Redis：`docker-compose up -d redis`
3. 启动应用：`./mvnw spring-boot:run`
4. 使用 Postman 或 curl 调用接口：

```bash
curl -X POST http://localhost:8080/v1/auth/forgot-password \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com"}'
```

5. 检查邮箱是否收到邮件

### 使用 MailHog 测试（推荐）

MailHog 是一个邮件测试工具，可以捕获所有发送的邮件，无需真实的 SMTP 服务器。

1. 启动 MailHog：
```bash
docker run -d -p 1025:1025 -p 8025:8025 mailhog/mailhog
```

2. 配置环境变量：
```bash
MAIL_HOST=localhost
MAIL_PORT=1025
MAIL_USERNAME=test@test.com
MAIL_PASSWORD=test
```

3. 访问 MailHog Web UI：http://localhost:8025
4. 发送测试邮件，在 Web UI 中查看

## 故障排查

### 1. 邮件发送失败

**症状**：日志显示 "发送密码重置邮件失败"

**可能原因**：
- SMTP 配置错误
- 邮箱密码/授权码错误
- 网络连接问题
- SMTP 服务器拒绝连接

**解决方案**：
1. 检查环境变量配置是否正确
2. 确认邮箱密码/授权码有效
3. 检查防火墙是否阻止 SMTP 端口
4. 查看应用日志获取详细错误信息

### 2. 邮件未收到

**症状**：接口返回成功，但邮箱未收到邮件

**可能原因**：
- 邮件被归类为垃圾邮件
- 邮箱地址错误
- 邮件服务器延迟

**解决方案**：
1. 检查垃圾邮件文件夹
2. 确认邮箱地址拼写正确
3. 等待几分钟后再检查
4. 查看应用日志确认邮件已发送

### 3. Token 验证失败

**症状**：用户点击链接后提示 Token 无效

**可能原因**：
- Token 已过期（超过 1 小时）
- Token 已被使用
- Redis 连接失败

**解决方案**：
1. 确认 Redis 服务正常运行
2. 检查 Token 是否在有效期内
3. 重新发送密码重置邮件

## 安全建议

1. **生产环境必须使用 HTTPS**：确保重置链接通过加密通道传输
2. **使用强密码**：SMTP 密码应使用应用专用密码，不要使用账号密码
3. **限制发送频率**：防止邮件轰炸攻击（建议：同一邮箱 5 分钟内最多 3 次）
4. **监控邮件发送**：记录所有邮件发送日志，便于审计
5. **Token 有效期**：建议设置为 15-60 分钟，平衡安全性和用户体验
6. **环境变量保护**：不要在代码中硬编码 SMTP 凭据

## 扩展功能

### 添加新的邮件模板

1. 在 `src/main/resources/templates/email/` 创建新模板
2. 在 `EmailService` 中添加新的发送方法
3. 调用 `templateEngine.process("email/your-template", context)`

### 支持多语言

1. 使用 Thymeleaf 的国际化功能
2. 创建 `messages_zh_CN.properties` 和 `messages_en_US.properties`
3. 在模板中使用 `#{message.key}` 语法

### 添加附件

```java
helper.addAttachment("filename.pdf", new FileSystemResource(file));
```

## 相关文件

- 邮件服务：`src/main/java/com/listen/portfolio/service/EmailService.java`
- Token 服务：`src/main/java/com/listen/portfolio/service/PasswordResetTokenService.java`
- 认证服务：`src/main/java/com/listen/portfolio/service/AuthService.java`
- 邮件模板：`src/main/resources/templates/email/password-reset-in-email.html`
- 配置文件：`src/main/resources/application.properties`

## 参考资料

- [Spring Boot Mail](https://docs.spring.io/spring-boot/docs/current/reference/html/io.html#io.email)
- [JavaMail API](https://javaee.github.io/javamail/)
- [Thymeleaf](https://www.thymeleaf.org/)
- [Gmail SMTP](https://support.google.com/mail/answer/7126229)
- [QQ 邮箱 SMTP](https://service.mail.qq.com/cgi-bin/help?subtype=1&&id=28&&no=1001256)
