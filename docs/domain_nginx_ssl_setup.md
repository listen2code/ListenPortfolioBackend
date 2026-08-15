# 🌐 域名注册、Nginx 反向代理与 Let's Encrypt HTTPS 配置指南

本文档记录了从注册免费开发者域名 `listen2code.is-a.dev`，到在 AWS EC2 服务器上配置 Nginx 反向代理（隐藏端口号 `:8080`），以及通过 Certbot 自动化集成 Let's Encrypt 免费 HTTPS 安全证书的全流程与实操命令。

---

## 📋 1. 前提条件与准备工作

在配置反向代理与 HTTPS 之前，请确保完成以下准备：

1. **域名注册**：通过在 GitHub [is-a-dev/register](https://github.com/is-a-dev/register) 仓库提交 PR，成功将 `listen2code.is-a.dev` 的 **A 记录** 指向 AWS EC2 公网 IP：`13.218.192.181`。
2. **AWS 安全组配置**：
   在 AWS EC2 控制台的 **Security Groups（安全组）** 入站规则（Inbound rules）中，确保放行了以下端口：
   * **HTTP (80)**：`0.0.0.0/0`
   * **HTTPS (443)**：`0.0.0.0/0`
   * **Custom TCP (8080)**：`0.0.0.0/0`（已放行）
   * **SSH (22)**：用于远程管理

---

## 🔍 2. 步骤一：验证域名解析已生效

在域名 PR 合并后，通过命令行验证域名是否成功解析到 AWS IP：

```bash
# 验证 DNS 解析
ping listen2code.is-a.dev

# 验证直接访问 8080 端口
curl.exe -s -i http://listen2code.is-a.dev:8080/v1/projects
```

若能正确返回 `HTTP/1.1 200 OK` 且 IP 对应 `13.218.192.181`，说明域名生效。

---

## 🛠️ 3. 步骤二：安装并配置 Nginx 反向代理

通过 Nginx 将外部对 `http://listen2code.is-a.dev` (80端口) 的访问隐式代理转发给本地 Docker 容器的 Spring Boot 服务 (`127.0.0.1:8080`)。

### 3.1 登录 EC2 安装 Nginx
使用 SSH 私钥登录服务器：
```bash
ssh -i tool/listen.pem ec2-user@13.218.192.181
```

在 Amazon Linux 2023 实例上安装并启动 Nginx：
```bash
sudo dnf install nginx -y
sudo systemctl enable --now nginx
```

### 3.2 创建站点反向代理配置文件
创建并编辑站点配置文件 `/etc/nginx/conf.d/portfolio.conf`：
```bash
sudo nano /etc/nginx/conf.d/portfolio.conf
```

填入以下 Nginx 代理规则：
```nginx
server {
    listen 80;
    server_name listen2code.is-a.dev;

    # 代理所有后端 API 请求至端口 8080
    location / {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        
        # 保持连接超时时间
        proxy_connect_timeout 60s;
        proxy_read_timeout 60s;
        proxy_send_timeout 60s;
    }
}
```

### 3.3 检查语法并重载 Nginx
```bash
sudo nginx -t
sudo systemctl reload nginx
```

此时，无需加上 `:8080`，直接访问 `http://listen2code.is-a.dev/v1/projects` 即可打通！

---

## 🔒 4. 步骤三：使用 Certbot 一键签发与配置 HTTPS

Let's Encrypt 官方推荐使用 **Certbot（ACME 客户端）** 全自动化完成证书申请与配置。

### 4.1 安装 Certbot Nginx 插件
```bash
sudo dnf install python3-certbot-nginx -y
```

### 4.2 执行证书签发与自动注入
运行 Certbot 命令行：
```bash
sudo certbot --nginx -d listen2code.is-a.dev
```

根据提示输入相关信息：
1. **Enter email address**：输入你的电子邮箱（用于接收到期提醒通知）。
2. **Terms of Service**：输入 `Y` 同意服务条款。
3. **Share email**：按需输入 `N`。
4. **Redirect HTTP to HTTPS**：Certbot 会自动修改 `/etc/nginx/conf.d/portfolio.conf`，插入 SSL 443 端口规则，并配置 HTTP 自动强制重定向到 HTTPS。

### 4.3 验证 HTTPS 服务
申请完成后，访问：
👉 `https://listen2code.is-a.dev/v1/projects`

浏览器将呈现安全的绿色安全锁（SSL 加密链路）。

### 4.4 验证证书自动续期（Cron）
Let's Encrypt 证书有效期为 90 天，Certbot 会自动注册定时任务。可以通过模拟命令验证自动续签是否正常：
```bash
sudo certbot renew --dry-run
```

---

## 📱 5. 步骤四：更新 Flutter 客户端项目配置

HTTPS 部署完成后，更新 Flutter 客户端的环境配置文件 [env_config.dart](file:///c:/Users/liste/Downloads/github/ListenPortfolioFlutter/lib/shared/constants/env_config.dart)：

```dart
  prod(
    env: AppEnvironment.prod,
    baseUrl: 'https://listen2code.is-a.dev',
  )
```

重新编译并运行移动端 App，即可完美打通安全端到端通信。
