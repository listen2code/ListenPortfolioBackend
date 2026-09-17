# 🔐 密码重置 API 规格与前后端集成指南 (Password Reset API & Integration Guide)

**Status**: `Implemented & Production-Ready`

本文档详细规范了 Listen Portfolio 后端系统中的**密码重置完整业务闭环**（包含申请密码重置、邮件安全链接下发、凭证核验、新密码密文更新与全端会话吊销），并提供针对 Flutter、React、Vue 以及命令行 cURL 的前后端联调集成代码。

---

## 一、核心设计理念与安全防御机制

密码重置链路是黑客实施账号劫持（Account Takeover）与用户数据枚举的高频攻击目标。本项目严格遵循 OWASP 认证与会话管理规范，构建了具备纵深防御特性的完整安全体系：

```
┌────────────────────────────────────────────────────────────────────────────────────────────────────────┐
│                              密码重置全链路安全防御与状态机流转                                           │
└────────────────────────────────────────────────────────────────────────────────────────────────────────┘

     阶段一：申请重置 (Forgot Password)
     ---------------------------------------------------------------------------------------------
     [ 客户端请求 ] ──▶ POST /v1/auth/forgot-password {"email":"user@example.com"}
                              │
                              ▼
     [ AOP 限流层 ] ──▶ RateLimitAspect (IP + EMAIL 双维度滑动窗口限流: 60s 内最多 10 次)
                              │
                              ▼
     [ 业务服务层 ] ──▶ AuthService.forgotPassword
                              │
                              ├─▶ 1. 查询数据库：若用户不存在 ──▶ 静默忽略 (防账号探测枚举)
                              │
                              └─▶ 2. 用户存在：
                                      │
                                      ├─▶ PasswordResetTokenService: CSPRNG 生成 256 位高熵随机数
                                      ├─▶ Redis: 写入 password_reset:<token> -> email (TTL: 3600s)
                                      └─▶ EmailService: 渲染 Thymeleaf HTML 模板并通过 SMTP 投递邮件
                              │
                              ▼
     [ 统一响应层 ] ──▶ 恒定返回 HTTP 200: "If the email exists, a password reset link has been sent"


     阶段二：执行重置 (Reset Password)
     ---------------------------------------------------------------------------------------------
     [ 邮件链接点击 ] ──▶ 用户打开 http://{frontend}/password-reset-out-email.html?token={token}
                              │
                              ▼
     [ 提交新密码 ] ──▶ POST /v1/auth/reset-password {"token":"...","newPassword":"..."}
                              │
                              ▼
     [ AOP 限流层 ] ──▶ RateLimitAspect (IP + TOKEN 双维度滑动窗口限流: 60s 内最多 10 次)
                              │
                              ▼
     [ 凭据核验层 ] ──▶ PasswordResetTokenService.getEmailByToken(token)
                              │
                              ├─▶ Token 不存在或已过期 ──▶ HTTP 400 Bad Request ("INVALID_TOKEN")
                              │
                              └─▶ Token 合法有效 ──▶ 获取关联用户 Email
                                      │
                                      ├─▶ 1. BCrypt 加盐哈希加密新密码 ──▶ 更新 MySQL users 表
                                      ├─▶ 2. 物理销毁 Token ──▶ Redis DEL password_reset:<token> (单次作废)
                                      └─▶ 3. 强制全端下线 ──▶ RefreshTokenService.revokeAllRefreshTokens(user)
                              │
                              ▼
     [ 最终结果响应 ] ──▶ HTTP 200 OK: 密码修改成功，历史登录凭据全部失效，引导跳转重新登录
```

### 核心安全架构原则 (Security Principles)

1. **防用户枚举嗅探 (Anti-User-Enumeration Pattern)**：
   - 传统接口若邮箱不存在则返回 `404 Not Found`，攻击者可通过编写自动化脚本遍历密码字典撞库探测注册名单；
   - 本接口对所有合法格式的邮箱请求**一律返回 HTTP 200 OK 与恒定提示文案**，彻底封死用户枚举攻击路径。
2. **密码学高熵安全凭据 (CSPRNG 256-bit SecureRandom)**：
   - 底层采用基于操作系统熵池的 `SecureRandom` 随机生成 32 字节（256 位）熵源，并经 URL-Safe Base64 编码（无补位符 `=`，避免 URL 参数转义问题），杜绝伪随机数预测。
3. **Redis 分布式时效生命周期管理 (Strict TTL)**：
   - 凭据仅存储于 Redis（默认 TTL 3600 秒 / 1 小时），超时由 Redis 自动驱逐；不产生持久化脏数据。
4. **单次使用即作废语义 (One-Time Semantic / Single-Use Token)**：
   - 密码成功重置后，立即调用 `redisTemplate.delete(key)` 物理销毁，即使 Token 处于 1 小时内也严禁二次复用，防范网络截获重放。
5. **全端多设备协同吊销 (Global Session Invalidation)**：
   - 重置成功后立即联动调用 `RefreshTokenService.revokeAllRefreshTokens(username)`，吊销该用户所有活跃的 Refresh Token，强制其在所有设备重新登录。
6. **双维度联合滑动时间窗口限流 (Dual-Dimension Rate Limiting)**：
   - 在 Controller 挂载 `@RateLimit`，`/forgot-password` 绑定 `IP + EMAIL` 联合限流；`/reset-password` 绑定 `IP + TOKEN` 联合限流，有效抵御针对单一受害者的邮件轰炸与自动化撞库。

---

## 二、端到端接口技术规格 (API Specifications)

### 1. 申请忘记密码与发送邮件

- **接口路径**：`POST /v1/auth/forgot-password`
- **请求方法**：`POST`
- **内容类型**：`application/json; charset=UTF-8`
- **鉴权要求**：公开接口（无需携带 Authorization 标头）
- **AOP 限流规则**：`@RateLimit(types = {IP, EMAIL}, maxRequests = 10, timeWindowSeconds = 60)`

#### 请求参数 (Request Body)
| 字段名 | 类型 | 必填 | 格式要求 / 约束 | 说明 |
| :--- | :--- | :---: | :--- | :--- |
| `email` | String | 是 | RFC 5322 邮箱格式，非空 | 用户注册时绑定的安全邮箱地址 |

```json
{
  "email": "listen2code@gmail.com"
}
```

#### 响应结果 (Response Body)
- **HTTP 状态码**：`200 OK`

```json
{
  "result": "0",
  "messageId": "",
  "message": "If the email exists, a password reset link has been sent",
  "body": null
}
```

> [!NOTE]
> 无论传入的邮箱是否在数据库中存在，响应结构与状态码完全一致，避免给黑客提供任何时序分析或差分线索。

---

### 2. 执行密码最终重置

- **接口路径**：`POST /v1/auth/reset-password`
- **请求方法**：`POST`
- **内容类型**：`application/json; charset=UTF-8`
- **鉴权要求**：公开接口（无需携带 Authorization 标头）
- **AOP 限流规则**：`@RateLimit(types = {IP, TOKEN}, maxRequests = 10, timeWindowSeconds = 60)`

#### 请求参数 (Request Body)
| 字段名 | 类型 | 必填 | 格式要求 / 约束 | 说明 |
| :--- | :--- | :---: | :--- | :--- |
| `token` | String | 是 | 非空，256 位 URL-Safe Base64 字符串 | 邮件内跳转链接中携带的重置凭证令牌 |
| `newPassword` | String | 是 | 长度为 6 到 100 字符之间，非空 | 用户设定的全新登录密码 |

```json
{
  "token": "KjDg1-I5QDydu8Se5vT88qPh-IZgk6AJhyo9gNpvkn4",
  "newPassword": "MyNewSecurePassword2026!"
}
```

#### 成功响应 (Success Response)
- **HTTP 状态码**：`200 OK`

```json
{
  "result": "0",
  "messageId": "",
  "message": "",
  "body": null
}
```

#### 失败响应 (Failure Response)
- **场景 A：Token 无效、伪造或已过期 (HTTP 400 Bad Request)**：
```json
{
  "result": "1",
  "messageId": "INVALID_TOKEN",
  "message": "The reset link is invalid or has expired",
  "body": null
}
```

- **场景 B：触发滑动窗口限流阈值 (HTTP 429 Too Many Requests)**：
```json
{
  "result": "1",
  "messageId": "RATE_LIMIT_EXCEEDED",
  "message": "Too many requests. Please try again later.",
  "body": null
}
```

- **场景 C：请求体参数校验不合法 (HTTP 400 Bad Request)**：
```json
{
  "result": "1",
  "messageId": "VALIDATION_FAILED",
  "message": "Password must be between 6 and 100 characters",
  "body": null
}
```

---

## 三、核心代码实现深度剖析 (Deep Dive)

### 1. 256 位 CSPRNG 高熵凭据生成与存储

在 [`PasswordResetTokenService.java`](file:///c:/Users/liste/Downloads/github/ListenPortfolioBackend/src/main/java/com/listen/portfolio/service/PasswordResetTokenService.java) 中：

```java
public String generateToken(String email) {
    // 1. 调用底层安全熵池生成 32 字节 (256 位) 密码学随机数
    byte[] randomBytes = new byte[32];
    secureRandom.nextBytes(randomBytes);
    
    // 2. 采用 URL 安全 Base64 编码并去除填充符 "="，避免前端 URL 转义异常
    String token = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);

    // 3. 存入 Redis 并赋予强时效 (默认 3600 秒)
    String key = TOKEN_PREFIX + token;
    redisTemplate.opsForValue().set(key, email, tokenExpiration, TimeUnit.SECONDS);

    return token;
}
```

### 2. 单次消费作废与全端会话联动注销

在 [`AuthService.java`](file:///c:/Users/liste/Downloads/github/ListenPortfolioBackend/src/main/java/com/listen/portfolio/service/AuthService.java) 中：

```java
@Transactional
public boolean resetPassword(String token, String newPassword) {
    // 1. 校验 Token 并检索关联邮箱
    String email = passwordResetTokenService.getEmailByToken(token);
    if (email == null) return false;

    // 2. 查询用户实体
    UserEntity user = userMapper.selectOne(
            new LambdaQueryWrapper<UserEntity>().eq(UserEntity::getEmail, email)
    );
    if (user == null) {
        passwordResetTokenService.deleteToken(token);
        return false;
    }

    // 3. BCrypt 密文落盘更新
    user.setPassword(passwordEncoder.encode(newPassword));
    userMapper.updateById(user);

    // 4. 关键安全步骤：物理删除 Token，确保不可二次使用
    passwordResetTokenService.deleteToken(token);

    // 5. 关键安全闭环：吊销该用户所有活跃的 Refresh Token，强制全端重新登录
    refreshTokenService.revokeAllRefreshTokens(user.getName());
    return true;
}
```

---

## 四、前后端完整集成代码实战

### 1. Flutter (Dart) 客户端集成示例

针对本项目配套的 Flutter 客户端，基于 Clean Architecture 与 Dio 封装的数据源层实现：

```dart
import 'package:dio/dio.dart';

class AuthRemoteDataSource {
  final Dio dio;
  AuthRemoteDataSource({required this.dio});

  /// 1. 发起忘记密码请求
  Future<void> forgotPassword(String email) async {
    try {
      final response = await dio.post(
        '/v1/auth/forgot-password',
        data: {'email': email},
      );
      // 后端始终返回 result == '0'
      if (response.data['result'] != '0') {
        throw Exception(response.data['message'] ?? '申请失败');
      }
    } on DioException catch (e) {
      throw Exception(e.response?.data['message'] ?? '网络异常，请重试');
    }
  }

  /// 2. 提交新密码与 Token 进行重置
  Future<bool> resetPassword({
    required String token,
    required String newPassword,
  }) async {
    try {
      final response = await dio.post(
        '/v1/auth/reset-password',
        data: {
          'token': token,
          'newPassword': newPassword,
        },
      );
      return response.data['result'] == '0';
    } on DioException catch (e) {
      if (e.response?.data['messageId'] == 'INVALID_TOKEN') {
        throw Exception('重置链接已失效或过期，请重新申请');
      }
      throw Exception(e.response?.data['message'] ?? '重置失败');
    }
  }
}
```

---

### 2. React (TypeScript + Hooks) 集成示例

```tsx
import React, { useState } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import axios from 'axios';

export const ResetPasswordForm: React.FC = () => {
  const [searchParams] = useSearchParams();
  const token = searchParams.get('token');
  const navigate = useNavigate();

  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [statusMsg, setStatusMsg] = useState<{ type: 'success' | 'error'; text: string } | null>(null);
  const [loading, setLoading] = useState(false);

  const handleReset = async (e: React.FormEvent) => {
    e.preventDefault();
    if (newPassword !== confirmPassword) {
      setStatusMsg({ type: 'error', text: '两次输入的密码不一致' });
      return;
    }
    if (!token) {
      setStatusMsg({ type: 'error', text: '重置 Token 无效或链接缺失' });
      return;
    }

    setLoading(true);
    try {
      const res = await axios.post('/v1/auth/reset-password', {
        token,
        newPassword,
      });

      if (res.data.result === '0') {
        setStatusMsg({ type: 'success', text: '密码重置成功！即将跳转至登录页面...' });
        setTimeout(() => navigate('/login'), 2000);
      } else {
        setStatusMsg({ type: 'error', text: res.data.message || '重置失败' });
      }
    } catch (err: any) {
      const msg = err.response?.data?.message || '网络连接超时';
      setStatusMsg({ type: 'error', text: msg });
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="reset-container">
      <h2>重置您的密码</h2>
      {statusMsg && <div className={`alert ${statusMsg.type}`}>{statusMsg.text}</div>}
      <form onSubmit={handleReset}>
        <input
          type="password"
          placeholder="请输入新密码 (至少 6 位)"
          value={newPassword}
          minLength={6}
          onChange={(e) => setNewPassword(e.target.value)}
          required
        />
        <input
          type="password"
          placeholder="请确认新密码"
          value={confirmPassword}
          onChange={(e) => setConfirmPassword(e.target.value)}
          required
        />
        <button type="submit" disabled={loading}>
          {loading ? '正在提交...' : '确认修改'}
        </button>
      </form>
    </div>
  );
};
```

---

### 3. Vue 3 (Composition API) 集成示例

```vue
<template>
  <div class="reset-box">
    <h2>设置新密码</h2>
    <form @submit.prevent="submitReset">
      <input
        v-model="newPassword"
        type="password"
        placeholder="新密码"
        minlength="6"
        required
      />
      <input
        v-model="confirmPassword"
        type="password"
        placeholder="确认新密码"
        required
      />
      <button :disabled="loading" type="submit">
        {{ loading ? '处理中...' : '提交重置' }}
      </button>
    </form>
    <p v-if="tipMessage" :class="isSuccess ? 'text-green' : 'text-red'">
      {{ tipMessage }}
    </p>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import axios from 'axios';

const route = useRoute();
const router = useRouter();
const token = route.query.token as string;

const newPassword = ref('');
const confirmPassword = ref('');
const tipMessage = ref('');
const isSuccess = ref(false);
const loading = ref(false);

const submitReset = async () => {
  if (newPassword.value !== confirmPassword.value) {
    tipMessage.value = '两次输入的密码不匹配';
    isSuccess.value = false;
    return;
  }

  loading.value = true;
  try {
    const res = await axios.post('/v1/auth/reset-password', {
      token,
      newPassword: newPassword.value,
    });
    if (res.data.result === '0') {
      isSuccess.value = true;
      tipMessage.value = '重置成功，2秒后跳转登录页';
      setTimeout(() => router.push('/login'), 2000);
    } else {
      isSuccess.value = false;
      tipMessage.value = res.data.message || '重置失败';
    }
  } catch (err: any) {
    isSuccess.value = false;
    tipMessage.value = err.response?.data?.message || '网络请求错误';
  } finally {
    loading.value = false;
  }
};
</script>
```

---

## 五、无前端命令行联调验证指南 (cURL & Redis)

在没有运行 Web/App 前端的情况下，可直接通过终端完成完整的联调与闭环验证：

### 步骤 1：触发忘记密码请求
```bash
curl -i -X POST http://localhost:8080/v1/auth/forgot-password   -H "Content-Type: application/json"   -d '{"email":"listen2code@gmail.com"}'
```
**预期输出**：
```http
HTTP/1.1 200 OK
Content-Type: application/json

{"result":"0","messageId":"","message":"If the email exists, a password reset link has been sent","body":null}
```

### 步骤 2：直接从 Redis 提取生成的安全 Token
```bash
# 查询当前有效的重置 Key
docker exec -it listen_portfolio_redis redis-cli keys "password_reset:*"
# 输出示例: 1) "password_reset:b2f0a1d4-test-token-value"

# 提取 Token 并查看 TTL
docker exec -it listen_portfolio_redis redis-cli get "password_reset:<TOKEN>"
docker exec -it listen_portfolio_redis redis-cli ttl "password_reset:<TOKEN>"
```

### 步骤 3：提交新密码重置
```bash
curl -i -X POST http://localhost:8080/v1/auth/reset-password   -H "Content-Type: application/json"   -d '{
    "token":"<TOKEN>",
    "newPassword":"NewSecurePassword123!"
  }'
```
**预期输出**：
```http
HTTP/1.1 200 OK
Content-Type: application/json

{"result":"0","messageId":"","message":"","body":null}
```

### 步骤 4：单次作废验证（再次使用相同 Token 提交）
```bash
# 再次尝试执行相同的重置命令
curl -i -X POST http://localhost:8080/v1/auth/reset-password   -H "Content-Type: application/json"   -d '{
    "token":"<TOKEN>",
    "newPassword":"AnotherPassword123!"
  }'
```
**预期输出（验证单次使用作废成功）**：
```http
HTTP/1.1 400 Bad Request
Content-Type: application/json

{"result":"1","messageId":"INVALID_TOKEN","message":"The reset link is invalid or has expired","body":null}
```

### 步骤 5：使用新密码验证登录
```bash
curl -i -X POST http://localhost:8080/v1/auth/login   -H "Content-Type: application/json"   -d '{
    "userName":"Listen",
    "password":"NewSecurePassword123!"
  }'
```
**预期输出**：返回包含全新 Access Token 与 Refresh Token 的 200 OK 登录成功响应。

---

## 六、错误码与排障速查手册 (Error Codes & Troubleshooting)

| HTTP 状态码 | 错误标识 (`messageId`) | 错误描述 | 常见诱因与解决方案 |
| :---: | :--- | :--- | :--- |
| **400** | `INVALID_TOKEN` | The reset link is invalid or has expired | 1. Token 超过 1 小时有效期已被 Redis 自动清除；<br>2. 链接已使用过一次（已物理删除）；<br>3. 用户输入或 URL 传参时缺失部分字符。 |
| **400** | `VALIDATION_FAILED` | Password must be between 6 and 100 characters | 新密码长度小于 6 位或超过 100 位，需在前端表单添加 `minLength` 校验。 |
| **429** | `RATE_LIMIT_EXCEEDED`| Too many requests. Please try again later. | 客户端在 60 秒内调用超过 10 次，触发了基于 IP 或 Email/Token 的滑动窗口限流，等待 1 分钟后自动解除。 |
| **500** | `INTERNAL_SERVER_ERROR` | An unexpected error occurred | Redis 连接断开或底层数据库死锁，请检查后端应用与容器日志：`docker logs -f app`。 |

---

## 七、架构演进与待办治理规划 (Roadmap)

根据本项目密码重置 API 的当前实现现状，在 [`docs/todo.md`](file:///c:/Users/liste/Downloads/github/ListenPortfolioBackend/docs/todo.md) **第 20 章节（密码重置业务安全与身份认证防御深度演进）** 中已建立以下改进规划：

1. **企业级密码复杂度策略与弱口令字典拦截 (Password Complexity & Dictionary Defense)**：
   - 引入密码复杂度校验器，强制新密码包含大小写字母、数字与特殊符号组合，并拦截常见 Top 1000 弱口令重置。
2. **密码修改成功异步二次邮件提醒与历史密码防重复机制 (Success Notification & Password History)**：
   - 重置成功后异步向邮箱补发安全确认邮件，告知修改时间与操作 IP；建立历史密码表，禁止重复使用最近 3 次旧密码。
3. **移动端/小程序原地 6 位短验证码 (OTP) 备用通道支持 (6-Digit Numeric OTP Support)**：
   - 在邮件中同步下发 6 位动态验证码，接口支持长 Token 与 `email + otp` 双模接入，优化移动端免跳转体验。
4. **混合签名 Token 与客户端防嗅探防重放加固 (Signed HMAC Token & Anti-Replay)**：
   - 引入应用层无状态 HMAC 验签机制，在进入 Redis 查询前快速甄别非法伪造 Token，极大降低缓存穿透压力。
