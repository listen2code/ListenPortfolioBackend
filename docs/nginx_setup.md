# 🌐 Nginx 架构配置与静态资源/API 代理运维指南

本文档详细记录了在 AWS EC2 (`13.218.192.181`) 上配置 Nginx 的完整架构设计、配置文件实录、运作原理及运维调试命令。

---

## 🏗️ 1. 整体系统架构图

单台 EC2 服务器通过 **80 端口 Nginx** 实现了 Flutter Web 前端静态托管与 Spring Boot 后端 API/静态图片的统一门户代理：

```mermaid
graph TD
    Client[Web 浏览器 / 移动端 App] -->|HTTP 80 端口| Nginx[EC2 Nginx 80 端口]
    
    subgraph AWS EC2 实例 (13.218.192.181)
        Nginx -->|/ 静态页面/JS资源| WebDir[/var/www/listen_portfolio_web]
        Nginx -->|^~ /api/ 接口/图片反向代理| DockerApp[Spring Boot 容器 127.0.0.1:8080]
    end
```

### 💡 核心优势
1. **统一端口与域名**：App 和 Web 端统一请求 `http://13.218.192.181/api/...`，无需暴露内网容器端口 8080。
2. **同源零跨域 (Same-Origin)**：Flutter Web 与 API 接口共享 `80` 端口同源，从根源上消除了浏览器的 CORS 跨域拦截与 OPTIONS 预检报错。
3. **动态环境零硬编码**：图片路径与 API 接口均保持以 `localhost` 开头的相对映射，由前端 `toApiUrl()` 动态适配 `mock`、`dev` 和 `prod`（`http://13.218.192.181/api`）环境。
4. **单页应用 (SPA) 路由支持**：配置了 `try_files` 兜底，防止 Flutter Web 在浏览器刷新时出现 HTTP 404。

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

    # 2. API 反向代理与后端静态资产代理 (优先于正则匹配，防止 /api/images/xxx.jpg 被拦截)
    location ^~ /api/ {
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

    # 3. 前端静态资源长效缓存 (JS/CSS/图片/字体/WASM)
    location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg|wasm|otf|ttf|woff|woff2)$ {
        root /var/www/listen_portfolio_web;
        expires 7d;
        add_header Cache-Control "public, no-transform";
    }
}
```

---

## 🔍 3. 关键配置项解密

| 配置段 / 指令 | 作用描述 | 技术细节 |
| :--- | :--- | :--- |
| `location ^~ /api/` | 优先匹配与正则屏蔽修饰符 | **核心修正**：使用 `^~` 前缀匹配修饰符。当请求为 `/api/images/project1.jpg` 时，Nginx 优先匹配 `/api/` 并**跳过后续的 `.jpg` 正则规则**，确保图片请求正确透传至 Spring Boot 后端。 |
| `proxy_pass http://127.0.0.1:8080/;` | 接口与图片透传剥离 | 当前端访问 `/api/images/project1.jpg` 时，Nginx 自动剥离 `/api` 前缀，透传给后端 `http://127.0.0.1:8080/images/project1.jpg`。 |
| `try_files $uri $uri/ /index.html` | SPA 单页应用路由支持 | 当用户刷新非首页 URL 时，Nginx 返回 `index.html` 交由 Flutter 内部路由解析。 |

---

## 🛠️ 4. 常用运维与调试命令

```bash
# 1. 验证后端静态图片代理 (HTTP 200)
curl -I http://13.218.192.181/api/images/project1.jpg

# 2. 检查 Nginx 语法与重载配置
sudo nginx -t && sudo systemctl reload nginx
```
