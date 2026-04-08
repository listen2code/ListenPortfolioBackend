# 密码重置 API 使用指南
  
  **Status**: `Implemented with Current Endpoint Scope`
  
  > 本文档仅描述当前仓库中已经实现的密码重置接口、邮件链接格式与配置项。
  > 如与 `AuthController`、`AuthService`、`EmailService`、`application.properties` 冲突，以代码为准。
  > React / Vue 片段仅为前端接入示意，不代表仓库内已有对应前端页面实现。
  
  ## 完整流程

### 1. 请求密码重置（发送邮件）

**接口**：`POST /v1/auth/forgot-password`

**请求示例**：
```bash
curl -X POST http://localhost:8080/v1/auth/forgot-password \
  -H "Content-Type: application/json" \
  -d '{"email":"listen2code@gmail.com"}'
```

  **响应**：
  ```json
  {
    "result": "0",
    "messageId": "",
    "message": "If the email exists, a password reset link has been sent",
    "body": null
  }
  ```
  
  **说明**：
  - 当前接口无论邮箱是否存在，都会返回成功响应，以降低邮箱枚举风险
  - 只有邮箱存在时才会尝试发送邮件
  - 链接格式为：`{FRONTEND_URL}/password-reset-out-email.html?token=xxx`
  - 默认配置下，链接会指向：`http://localhost:8080/password-reset-out-email.html?token=xxx`
  - Token 有效期：1 小时
  - 当前限流由 `AuthController` 上的 `@RateLimit` 注解控制：同一 IP / Email 1 分钟内最多 10 次

### 2. 重置密码（使用 Token）

**接口**：`POST /v1/auth/reset-password`

**请求示例**：
```bash
curl -X POST http://localhost:8080/v1/auth/reset-password \
  -H "Content-Type: application/json" \
  -d '{
    "token":"KjDg1-I5QDydu8Se5vT88qPh-IZgk6AJhyo9gNpvkn4",
    "newPassword":"newPassword123"
  }'
```

**成功响应**：
```json
{
  "result": "0",
  "messageId": "",
  "message": "",
  "body": null
}
```

**失败响应**（Token 无效或过期）：
```json
{
  "result": "1",
  "messageId": "INVALID_TOKEN",
  "message": "The reset link is invalid or has expired",
  "body": null
}
```

## 前端集成示例

### React 示例

```jsx
// 密码重置页面
import { useSearchParams } from 'react-router-dom';
import { useState } from 'react';

function ResetPasswordPage() {
  const [searchParams] = useSearchParams();
  const token = searchParams.get('token');
  const [newPassword, setNewPassword] = useState('');
  const [message, setMessage] = useState('');

  const handleSubmit = async (e) => {
    e.preventDefault();
    
    try {
      const response = await fetch('http://localhost:8080/v1/auth/reset-password', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          token: token,
          newPassword: newPassword,
        }),
      });

      const data = await response.json();

      if (data.result === '0') {
        setMessage('密码重置成功！请使用新密码登录。');
        // 跳转到登录页
        setTimeout(() => {
          window.location.href = '/login';
        }, 2000);
      } else {
        setMessage(data.message || '重置失败，请重试');
      }
    } catch (error) {
      setMessage('网络错误，请稍后重试');
    }
  };

  return (
    <div>
      <h1>重置密码</h1>
      <form onSubmit={handleSubmit}>
        <input
          type="password"
          placeholder="请输入新密码"
          value={newPassword}
          onChange={(e) => setNewPassword(e.target.value)}
          minLength={6}
          required
        />
        <button type="submit">重置密码</button>
      </form>
      {message && <p>{message}</p>}
    </div>
  );
}
```

### Vue 示例

```vue
<template>
  <div>
    <h1>重置密码</h1>
    <form @submit.prevent="handleSubmit">
      <input
        v-model="newPassword"
        type="password"
        placeholder="请输入新密码"
        minlength="6"
        required
      />
      <button type="submit">重置密码</button>
    </form>
    <p v-if="message">{{ message }}</p>
  </div>
</template>

<script>
export default {
  data() {
    return {
      newPassword: '',
      message: '',
    };
  },
  computed: {
    token() {
      return this.$route.query.token;
    },
  },
  methods: {
    async handleSubmit() {
      try {
        const response = await fetch('http://localhost:8080/v1/auth/reset-password', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
          },
          body: JSON.stringify({
            token: this.token,
            newPassword: this.newPassword,
          }),
        });

        const data = await response.json();

        if (data.result === '0') {
          this.message = '密码重置成功！请使用新密码登录。';
          setTimeout(() => {
            this.$router.push('/login');
          }, 2000);
        } else {
          this.message = data.message || '重置失败，请重试';
        }
      } catch (error) {
        this.message = '网络错误，请稍后重试';
      }
    },
  },
};
</script>
```

## 无前端测试方法

如果你还没有前端应用，可以直接使用 Postman 或 curl 测试：

### 步骤 1：获取 Token

1. 调用 `/forgot-password` 接口
2. 检查邮箱，复制邮件中的 Token（URL 中的 `token=` 后面的部分）

### 步骤 2：重置密码

```bash
# 替换 YOUR_TOKEN 为实际的 Token
curl -X POST http://localhost:8080/v1/auth/reset-password \
  -H "Content-Type: application/json" \
  -d '{
    "token":"YOUR_TOKEN",
    "newPassword":"newPassword123"
  }'
```

### 步骤 3：验证

使用新密码登录：

```bash
curl -X POST http://localhost:8080/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "userName":"yourUsername",
    "password":"newPassword123"
  }'
```

## 安全特性
  
  1. **Token 一次性使用**：Token 使用后立即失效
  2. **Token 过期时间**：默认 1 小时后自动失效
  3. **限流保护**：
     - `POST /v1/auth/forgot-password`：同一 IP / Email 1 分钟内最多 10 次
     - `POST /v1/auth/reset-password`：同一 IP / Token 1 分钟内最多 10 次
  4. **防止邮箱枚举**：无论邮箱是否存在，都返回成功
  5. **密码加密**：使用 BCrypt 加密存储

## 错误处理

| 错误码 | 说明 | 解决方案 |
|--------|------|----------|
| `INVALID_TOKEN` | Token 无效或已过期 | 重新请求密码重置邮件 |
| `RATE_LIMIT_EXCEEDED` | 请求过于频繁 | 等待几分钟后重试 |

## 配置说明
  
  ### 修改 Token 有效期
  
  在 `application.properties` 或环境变量中配置：
  
  ```properties
  # Token 有效期（秒），默认 3600 = 1 小时
  app.password-reset.token-expiration=3600
  ```

  ### 修改前端 URL
  
  ```properties
  # 前端应用地址
  app.frontend.url=http://localhost:3000
  ```
  
  > 若不覆盖该配置，默认值为 `http://localhost:8080`，邮件中的重置链接会指向后端静态页面 `/password-reset-out-email.html`。
  
  ### 修改限流参数
  
  当前密码重置接口的限流阈值定义在 `AuthController` 的 `@RateLimit` 注解上，而不是 `RateLimitService` 的默认辅助方法中：
  
  ```java
  @RateLimit(
      types = {RateLimitType.IP, RateLimitType.EMAIL},
      maxRequests = 10,
      timeWindowSeconds = 60
  )
  public ResponseEntity<ApiResponse<Object>> forgotPassword(...) { ... }

  @RateLimit(
      types = {RateLimitType.IP, RateLimitType.TOKEN},
      maxRequests = 10,
      timeWindowSeconds = 60
  )
  public ResponseEntity<ApiResponse<Object>> resetPassword(...) { ... }
  ```

## 相关接口

- `POST /v1/auth/forgot-password` - 请求密码重置
- `POST /v1/auth/reset-password` - 重置密码
- `POST /v1/auth/login` - 登录
- `POST /v1/auth/signUp` - 注册
