# 📧 邮件服务配置指南

## 概述

本项目已集成邮件发送功能，用于密码重置等场景。支持多种 SMTP 服务提供商。

## 功能特性

- ✅ 密码重置邮件（带重置链接）
- ✅ HTML 邮件模板（美观、响应式）
- ✅ 安全的 Token 生成（SecureRandom + Redis）
- ✅ Token 过期机制（默认 1 小时）
- ✅ STARTTLS 加密传输
- ✅ 一次性 Token（使用后立即失效）

## 环境变量配置

### 必需配置

在 `.env` 文件或系统环境变量中配置以下参数：

```bash
# SMTP 服务器配置
MAIL_HOST=smtp.gmail.com          # SMTP 服务器地址
MAIL_PORT=587                      # SMTP 端口（587 为 TLS，465 为 SSL）
MAIL_USERNAME=your-email@gmail.com # 发件人邮箱
MAIL_PASSWORD=your-app-password    # 邮箱密码或应用专用密码

# 前端 URL 配置
FRONTEND_URL=http://localhost:3000 # 前端应用地址，用于生成重置链接

# Token 过期时间（可选，默认 3600 秒 = 1 小时）
PASSWORD_RESET_TOKEN_EXPIRATION=3600
```

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
