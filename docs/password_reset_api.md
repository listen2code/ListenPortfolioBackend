# 密码重置 API 使用指南

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
  "message": "",
  "body": null
}
```

**说明**：
- 用户会收到包含重置链接的邮件
- 链接格式：`http://localhost:3000/password-reset-out-email?token=xxx`
- Token 有效期：1 小时
- 限流：同一邮箱 5 分钟内最多 3 次

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
  "message": "重置链接无效或已过期",
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
   - 同一邮箱 5 分钟内最多 3 次
   - 同一 IP 1 分钟内最多 10 次
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

### 修改限流参数

在 `RateLimitService.java` 中修改：

```java
// 邮箱限流：5 分钟内最多 3 次
public boolean isEmailAllowed(String email) {
    return isAllowed("email:" + email, 3, 300);
}

// IP 限流：1 分钟内最多 10 次
public boolean isIpAllowed(String ip) {
    return isAllowed("ip:" + ip, 10, 60);
}
```

## 相关接口

- `POST /v1/auth/forgot-password` - 请求密码重置
- `POST /v1/auth/reset-password` - 重置密码
- `POST /v1/auth/login` - 登录
- `POST /v1/auth/signUp` - 注册
