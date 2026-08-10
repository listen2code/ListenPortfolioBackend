# 🌐 Nginx 架构配置与静态资源/API 代理运维指南

本文档详细记录了在 AWS EC2 (`13.218.192.181`) 上配置 Nginx 的完整架构设计、配置文件实录、运作原理及运维调试命令。

---

## 🏗️ 1. 整体系统架构图

单台 EC2 服务器通过 **80 端口 Nginx** 实现了 Flutter Web 前端静态托管与 Spring Boot 后端 API 的统一门户代理：

```mermaid
graph TD
    Client[Web 浏览器 / 移动端 App] -->|HTTP 80 端口| Nginx[EC2 Nginx 80 端口]
    
    subgraph AWS EC2 实例 (13.218.192.181)
        Nginx -->|/ 静态页面/JS资源| WebDir[/var/www/listen_portfolio_web]
        Nginx -->|/api/ 接口反向代理| DockerApp[Spring Boot 容器 127.0.0.1:8080]
    end
```

### 💡 核心优势
1. **统一端口与域名**：App 和 Web 端统一请求 `http://13.218.192.181/api/...`，无需暴露内网容器端口 8080。
2. **同源零跨域 (Same-Origin)**：Flutter Web 与 API 接口共享 `80` 端口同源，从根源上消除了浏览器的 CORS 跨域拦截与 OPTIONS 预检报错。
3. **单页应用 (SPA) 路由支持**：配置了 `try_files` 兜底，防止 Flutter Web 在浏览器刷新时出现 HTTP 404。

---

## 📜 2. Nginx 配置文件实录

配置文件存放在 EC2 服务器路径：`/etc/nginx/conf.d/listen_portfolio.conf`

```nginx
server {
    listen 80;
    server_name _;

    # 1. 托管 Flutter Web 单页应用静态文件
    location / {
        root /var/www/listen_portfolio_web;
        index index.html;
        # SPA 路由兜底：请求文件不存在时回退至 index.html
        try_files $uri $uri/ /index.html;
    }

    # 2. 静态资源长效缓存 (JS/CSS/图片/字体/WASM)
    location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg|wasm|otf|ttf|woff|woff2)$ {
        root /var/www/listen_portfolio_web;
        expires 7d;
        add_header Cache-Control "public, no-transform";
    }

    # 3. API 反向代理与 CORS 预检支持
    location /api/ {
        # 针对 CORS OPTIONS 预检请求直接返回 204
        if ($request_method = 'OPTIONS') {
            add_header 'Access-Control-Allow-Origin' '*' always;
            add_header 'Access-Control-Allow-Methods' 'GET, POST, PUT, DELETE, PATCH, OPTIONS' always;
            add_header 'Access-Control-Allow-Headers' '*' always;
            add_header 'Access-Control-Max-Age' 1728000 always;
            add_header 'Content-Type' 'text/plain; charset=utf-8' always;
            add_header 'Content-Length' 0 always;
            return 204;
        }

        # 透明代理至本地 Spring Boot 8080 端口 (末尾斜杠会自动剥离 /api 前缀)
        proxy_pass http://127.0.0.1:8080/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

---

## 🔍 3. 关键配置项解密

| 配置段 / 指令 | 作用描述 | 技术细节 |
| :--- | :--- | :--- |
| `try_files $uri $uri/ /index.html` | SPA 单页应用路由支持 | 当用户直接访问 `http://13.218.192.181/settings` 并刷新页面时，Nginx 不会在磁盘寻找 `/settings` 文件，而是返回 `index.html` 交由 Flutter 内部路由解析。 |
| `proxy_pass http://127.0.0.1:8080/;` | 接口转发与路径前缀剥离 | **注意末尾斜杠 `/`**。当客户端发送 `/api/v1/auth/login` 时，Nginx 会自动剥离 `/api`，真实传给 Spring Boot 的路径为 `/v1/auth/login`，因而后端无需修改任何 ApiMappings。 |
| `if ($request_method = 'OPTIONS')` | 拦截与响应 CORS 预检 | 对复杂的跨域请求，Nginx 直接在边缘节点响应 204 并带上 `Access-Control-Allow-*` 响应头，无需将 Preflight 打到后端 Java 进程。 |
| `chmod -R 755 /var/www/listen_portfolio_web` | Linux 文件访问权限 | Nginx 默认进程为 `nginx` 用户，必须保证根目录及所有子目录具有 `755` (可读可执行) 权限，防止静态文件（如 `FontManifest.json` / `main.dart.js`）触发 403 Forbidden。 |

---

## 🛠️ 4. 常用运维与调试命令

### 4.1 Nginx 检查与生效
```bash
# 检查 Nginx 语法正确性
sudo nginx -t

# 平滑重载 Nginx 配置（不中断线上访问）
sudo systemctl reload nginx

# 重启 Nginx 服务
sudo systemctl restart nginx
```

### 4.2 Web 静态资源手工更新与赋权
```bash
# 解压/覆盖 web 产物后，必须对 Web 目录赋予可读与搜索权限
sudo chmod -R 755 /var/www/listen_portfolio_web
```

### 4.3 接口代理与 CORS 预检测试
```bash
# 1. 验证静态资源 HTTP 200
curl -I http://13.218.192.181/

# 2. 验证 API 代理（从 80 端口透传至 8080 端口后端）
curl -I http://13.218.192.181/api/v1/test

# 3. 测试 CORS OPTIONS 预检响应
curl -I -X OPTIONS http://13.218.192.181/api/v1/test \
  -H "Origin: http://13.218.192.181" \
  -H "Access-Control-Request-Method: POST" \
  -H "Access-Control-Request-Headers: Content-Type, Authorization"
```

### 4.4 查看日志
```bash
# 查看实时访问日志
sudo tail -f /var/log/nginx/access.log

# 查看实时错误日志（排查 403 / 502 报错）
sudo tail -f /var/log/nginx/error.log
```
