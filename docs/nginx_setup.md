# 🌐 Nginx 边缘网关与动静分离反向代理深度指南 (Nginx Edge Gateway & Reverse Proxy Architecture Guide)

**Status**: `Production Deployed on AWS EC2 (13.218.192.181 / listen2code.is-a.dev)`

---

## 一、架构设计理念与技术全貌

在现代互联网架构中，**Nginx** 凭借其异步非阻塞（epoll / kqueue）高并发事件驱动模型，通常作为整个应用基础设施的**边缘门户网关 (Edge Gateway)** 与**七层反向代理 (Reverse Proxy)**。

在本项目生产拓扑中，单台 AWS EC2 实例（`t2.micro`，1vCPU / 1GB RAM）承载了 **Flutter Web 前端单页应用 (SPA)** 与 **Spring Boot 容器化后端微服务**。为了最大化硬件效能并保障系统稳定性，系统采用了“**Nginx 宿主机边缘网关 + Docker 容器业务集群**”的动静分离二层架构：

```
┌────────────────────────────────────────────────────────────────────────────────────────────────────────┐
│                                 Nginx 边缘网关与微服务动静分离网络拓扑                                     │
└────────────────────────────────────────────────────────────────────────────────────────────────────────┘

              [ 外部互联网客户端 (Web 浏览器 / Flutter 移动 App / 爬虫) ]
                                          │
                                          │ HTTP 80 (强制 301) / HTTPS 443
                                          ▼
   ┌──────────────────────────────────────────────────────────────────────────────┐
   │ AWS EC2 宿主机 Nginx 边缘网关 (13.218.192.181 / listen2code.is-a.dev)         │
   ├──────────────────────────────────────────────────────────────────────────────┤
   │ • SSL/TLS 握手终止与 Let's Encrypt 证书加固                                    │
   │ • HTTP/2 多路复用与请求头预处理 (Host, X-Real-IP, X-Forwarded-For, TraceId)   │
   │ • CORS Preflight (OPTIONS 204) 边缘直接拦截短路，零开销保护后端容器线程池        │
   │ • 智能多级缓存：JS/CSS/WASM/字体文件 30 天强缓存 (Cache-Control: public)        │
   └──────────────────────────────────────────────────────────────────────────────┘
                    │                                             │
      (静态文件请求) │                               (动态 API 与后端资产)│
      location /    │                               location ^~ /api/     │
      location ~*   │                                                     │
                    ▼                                                     ▼
   ┌────────────────────────────────┐            ┌────────────────────────────────┐
   │ 宿主机本地磁盘静态目录           │            │ Docker 容器化业务集群           │
   │ /var/www/listen_portfolio_web/ │            │ http://127.0.0.1:8080/         │
   ├────────────────────────────────┤            ├────────────────────────────────┤
   │ • index.html (SPA 路由兜底入口) │            │ • 业务 RESTful API (/v1/**)     │
   │ • main.dart.js (Flutter 前端)  │            │ • 后端上传图片 (/images/**)     │
   │ • canvaskit.wasm (图形渲染引擎) │            │ • Actuator 监控 (/actuator/**) │
   │ • assets/ (字体、图标素材)      │            │ • Spring Security 认证过滤链   │
   └────────────────────────────────┘            └────────────────────────────────┘
```

### 核心架构优势与设计哲学

1. **动静完全分离 (Dynamic-Static Separation)**：
   - **静态资源零 Java 线程开销**：Flutter Web 的编译产物（HTML/JS/CSS/Wasm）全部由 Nginx 借助 Linux 内核 `sendfile` 系统调用直接从磁盘写入网络 Socket，不经过 JVM，吞吐量提升一个数量级；
   - **动态请求反向代理**：仅将 `/api/` 请求透传给 Docker 内部的 Spring Boot 容器，大幅释放 JVM 堆内存与 Tomcat Worker 线程资源。
2. **同源零跨域 (Same-Origin Paradigm)**：
   - 前端 Web 页面（`listen2code.is-a.dev`）与后端 API（`listen2code.is-a.dev/api/...`）共享同一个域名与端口；
   - 彻底避免了因不同端口（如 80 vs 8080）或跨子域引发的浏览器跨域拦截（CORS Policy）以及昂贵的额外 preflight `OPTIONS` 网络往返。
3. **SPA 单页应用路由兜底 (HTML5 History Mode Support)**：
   - 借助 Nginx 的 `try_files $uri $uri/ /index.html;` 指令，当用户在浏览器刷新二级路由页面（如 `/about` 或 `/projects`）时，不会向服务器索取物理文件引发 404，而是自动回退返回 `index.html`，交由 Flutter 路由引擎在前端完成渲染。
4. **统一前缀剥离与后端解耦 (URI Rewrite & Stripping)**：
   - Nginx 使用带末尾斜杠的 `proxy_pass http://127.0.0.1:8080/;`，在转发前自动平滑剥离 `/api` 路由前缀；
   - 后端 Spring Boot 仅需专注于纯粹的业务路由（如 `/v1/projects` 与 `/images/**`），保持代码与基础设施物理路径解耦。

---

## 二、Nginx 生产完整配置实录

部署于 EC2 宿主机路径：`/etc/nginx/conf.d/listen_portfolio.conf`

```nginx
# ===================================================================
# Listen Portfolio 边缘网关与动静分离反向代理生产配置
# 部署主机: AWS EC2 (13.218.192.181 / listen2code.is-a.dev)
# ===================================================================

# 1. HTTP 80 端口服务块：强制 301 重定向至安全 HTTPS 443
server {
    listen 80;
    listen [::]:80;
    server_name listen2code.is-a.dev 13.218.192.181 localhost;

    # 允许 Certbot 自动续签时通过 ACME 挑战目录
    location /.well-known/acme-challenge/ {
        root /var/www/certbot;
    }

    # 其余所有流量一律 301 强跳至 HTTPS
    location / {
        return 301 https://$host$request_uri;
    }
}

# 2. HTTPS 443 端口服务块：核心业务网关
server {
    listen 443 ssl http2;
    listen [::]:443 ssl http2;
    server_name listen2code.is-a.dev;

    # 客户端上传请求体上限（支持 Base64 头像上传与大文件提交，默认 1M 易导致 413）
    client_max_body_size 10m;

    # ---------------------------------------------------------------
    # SSL/TLS 加固与证书配置 (由 Let's Encrypt Certbot 自动注入)
    # ---------------------------------------------------------------
    ssl_certificate /etc/letsencrypt/live/listen2code.is-a.dev/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/listen2code.is-a.dev/privkey.pem;
    include /etc/letsencrypt/options-ssl-nginx.conf;
    ssl_dhparam /etc/letsencrypt/ssl-dhparams.pem;

    # ---------------------------------------------------------------
    # 安全防护响应头
    # ---------------------------------------------------------------
    add_header X-Frame-Options "SAMEORIGIN" always;
    add_header X-XSS-Protection "1; mode=block" always;
    add_header X-Content-Type-Options "nosniff" always;
    add_header Referrer-Policy "no-referrer-when-downgrade" always;

    # ---------------------------------------------------------------
    # 路由规则 1: 托管 Flutter Web 单页应用静态文件
    # ---------------------------------------------------------------
    location / {
        root /var/www/listen_portfolio_web;
        index index.html;
        # SPA 核心兜底机制：优先查找物理文件与目录，不存在时回退至 index.html
        try_files $uri $uri/ /index.html;
    }

    # ---------------------------------------------------------------
    # 路由规则 2: API 接口与后端静态图片代理 (^~ 优先匹配，屏蔽正则)
    # ---------------------------------------------------------------
    location ^~ /api/ {
        # 边缘网关层直接短路 OPTIONS 预检请求，避免唤醒后方 Spring Boot
        if ($request_method = 'OPTIONS') {
            add_header 'Access-Control-Allow-Origin' '*' always;
            add_header 'Access-Control-Allow-Methods' 'GET, POST, PUT, DELETE, PATCH, OPTIONS' always;
            add_header 'Access-Control-Allow-Headers' '*' always;
            add_header 'Access-Control-Max-Age' 1728000 always;
            add_header 'Content-Type' 'text/plain; charset=utf-8' always;
            add_header 'Content-Length' 0 always;
            return 204;
        }

        # 透明代理至宿主机 127.0.0.1:8080 (Docker 容器映射端口)
        # 末尾携带斜杠 "/"，Nginx 会自动剥离匹配到的 "/api/" 前缀
        proxy_pass http://127.0.0.1:8080/;

        # 协议与版本优化
        proxy_http_version 1.1;

        # WebSocket 与长连接协议升级支持
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";

        # 真实客户端信息与分布式链路追踪透传
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_set_header X-Request-Id $request_id;

        # 超时与缓冲控制（防止慢客户端拖垮连接）
        proxy_connect_timeout 30s;
        proxy_read_timeout 60s;
        proxy_send_timeout 60s;
        proxy_buffering on;
        proxy_buffer_size 8k;
        proxy_buffers 8 8k;
    }

    # ---------------------------------------------------------------
    # 路由规则 3: 前端静态资源长效缓存 (JS/CSS/WASM/图片/字体)
    # ---------------------------------------------------------------
    location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg|wasm|otf|ttf|woff|woff2)$ {
        root /var/www/listen_portfolio_web;
        expires 30d;
        add_header Cache-Control "public, no-transform";
        access_log off; # 关闭静态资源日志，减轻磁盘 I/O
    }
}
```

---

## 三、重点与难点配置深度剖析 (Deep Dive)

### 1. Location 匹配优先级陷阱：为什么必须使用 `^~ /api/`？

在 Nginx 的匹配规则中，不同修饰符具有极其严格的优先级别。很多开发者常犯的错误是将 API 配置写为 `location /api/`，最终导致后端图片资源（如 `/api/images/project1.jpg`）遭遇 **HTTP 404**。

#### 📊 Nginx Location 匹配优先级表：

| 优先级 | 修饰符 | 匹配类型 | 说明 |
| :---: | :---: | :--- | :--- |
| **1** | `=` | 精确匹配 (Exact Match) | 仅当 URI 完全相同时命中，立即终止搜索。 |
| **2** | `^~` | **带屏蔽前缀匹配 (Preferential Prefix)** | **若匹配此前缀，立即终止后续所有正则表达式的搜索！** |
| **3** | `~` 或 `~*`| 正则表达式匹配 (Regex Match) | 区分或不区分大小写正则。**普通前缀匹配无法阻挡正则匹配！** |
| **4** | (空) | 普通前缀匹配 (Normal Prefix) | 暂存最长匹配项，但仍会继续向下扫描正则，若正则命中则覆盖普通前缀！ |
| **5** | `/` | 通用匹配 (Fallback) | 所有请求的最终兜底规则。 |

#### 💣 生产踩坑场景还原：
1. 假设静态资源规则配置为：
   `location ~* \.(jpg|png|svg)$ { root /var/www/listen_portfolio_web; }`
2. 若反向代理规则误配为普通前缀：
   `location /api/ { proxy_pass http://127.0.0.1:8080/; }`
3. 当客户端请求后端动态图片 `/api/images/project1.jpg` 时：
   - Nginx 首先暂存 `location /api/`；
   - 但因为规则是普通前缀，Nginx 会继续扫描正则，发现该 URI 命中了 `~* \.(jpg)$`！
   - Nginx 将请求派发给静态规则，去 `/var/www/listen_portfolio_web/api/images/project1.jpg` 查找物理文件；
   - 物理文件并不存在，Nginx 直接向客户端返回 **404 Not Found**，请求根本未能到达 Spring Boot！
4. **解决方案**：
   使用 `location ^~ /api/`。`^~` 修饰符强行告知 Nginx：“**一旦匹配到以 `/api/` 开头，坚决不再扫描任何后续正则规则！**”，确保无论是 `/api/v1/projects` 还是 `/api/images/project1.jpg`，100% 稳定投递至 Spring Boot 容器！

---

### 2. `proxy_pass` 末尾斜杠（URI 剥离 vs 保留）细节

Nginx 中 `proxy_pass` 指令末尾的 `/`（斜杠）具有截然不同的行为逻辑：

- **场景 A：末尾带有斜杠**：
  ```nginx
  location ^~ /api/ {
      proxy_pass http://127.0.0.1:8080/;
  }
  ```
  - **行为**：Nginx 会将匹配到的 location 模式串（`/api/`）自动剥离，将剩余部分追加至上游 URL；
  - **转换效果**：客户端请求 `http://listen2code.is-a.dev/api/v1/projects` ➔ 上游收到 `http://127.0.0.1:8080/v1/projects`；
  - **匹配后端**：Spring Boot 的 Controller 标注为 `@RequestMapping("/v1/projects")`，完美对齐。
- **场景 B：末尾没有斜杠**：
  ```nginx
  location ^~ /api/ {
      proxy_pass http://127.0.0.1:8080;
  }
  ```
  - **行为**：Nginx 会原封不动将客户端完整 URI 透传给后端；
  - **转换效果**：客户端请求 `/api/v1/projects` ➔ 上游收到 `/api/v1/projects`；
  - **不匹配**：若 Spring Boot 未配置全局 `server.servlet.context-path=/api`，将直接报 404。

---

### 3. 网关层 OPTIONS 预检请求 204 短路拦截

在前后端交互中，现代浏览器发起复杂跨域请求（如带有自定义 Header `Authorization` 或 `X-Request-Id`）时，会强制先发送一次 `OPTIONS` 预检探测请求：

```nginx
if ($request_method = 'OPTIONS') {
    add_header 'Access-Control-Allow-Origin' '*' always;
    add_header 'Access-Control-Allow-Methods' 'GET, POST, PUT, DELETE, PATCH, OPTIONS' always;
    add_header 'Access-Control-Allow-Headers' '*' always;
    add_header 'Access-Control-Max-Age' 1728000 always; # 20天免重复预检缓存
    add_header 'Content-Type' 'text/plain; charset=utf-8' always;
    add_header 'Content-Length' 0 always;
    return 204;
}
```
- **核心收益**：由 Nginx 在 C 语言网络边缘层直接返回 `204 No Content`，无需将无业务意义的预检流量打入后方的 Tomcat 线程池和 Spring Security 过滤链，极大降低 JVM 资源占用并提升前端首包响应速度。

---

### 4. 客户端真实 IP 与分布式全链路追踪透传

反向代理后，Spring Boot 如果直接通过 `request.getRemoteAddr()` 获取，只能拿到 `127.0.0.1`（即 Nginx 容器/宿主机 IP）。为确保后台限流切面（`RateLimitAspect`）与审计日志准确获取真实公网 IP，Nginx 注入了标准代理标头：

```nginx
proxy_set_header Host $host;
proxy_set_header X-Real-IP $remote_addr;
proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
proxy_set_header X-Forwarded-Proto $scheme;
proxy_set_header X-Request-Id $request_id;
```

#### 与后端代码的联动闭环：
- [`RequestLoggingFilter.java`](file:///c:/Users/liste/Downloads/github/ListenPortfolioBackend/src/main/java/com/listen/portfolio/common/config/RequestLoggingFilter.java) 优先从 `X-Forwarded-For` 与 `X-Real-IP` 提取客户端真实 IP；
- 同时提取 Nginx 生成的 `$request_id`，注入 SLF4J MDC 并在 HTTP 响应头回显 `X-Request-Id`，实现端到端分布式全链路日志串联。

---

## 四、前端静态资源与单页应用 (SPA) 路由协同

### 1. SPA 单页应用 404 刷新问题根治

```nginx
location / {
    root /var/www/listen_portfolio_web;
    index index.html;
    try_files $uri $uri/ /index.html;
}
```

- **工作机制**：
  1. `$uri`：检查物理文件是否存在（如请求 `/main.dart.js` 时直接返回该文件）；
  2. `$uri/`：检查物理目录是否存在；
  3. `/index.html`：当上述均未命中（如用户在浏览器直接访问或刷新 `/projects`、`/about` 等虚拟路由时），Nginx 返回 `index.html`，HTTP 状态码保持 200，随后浏览器加载前端 JS 引擎，由 Flutter 的路由器（GoRouter / Navigator）解析当前 URL 并渲染对应页面组件。

### 2. 静态图片资产与 Spring Boot [`WebConfig.java`](file:///c:/Users/liste/Downloads/github/ListenPortfolioBackend/src/main/java/com/listen/portfolio/common/config/WebConfig.java) 协同

在 Spring Boot 中：
```java
@Override
public void addResourceHandlers(ResourceHandlerRegistry registry) {
    registry.addResourceHandler("/images/**")
            .addResourceLocations("classpath:/static/images/", "classpath:/public/images/");
}
```
- 前端请求 `http://listen2code.is-a.dev/api/images/project1.jpg`；
- Nginx `location ^~ /api/` 命中，剥离 `/api`，转发至 Spring Boot 的 `/images/project1.jpg`；
- Spring Boot 的 `WebConfig` 将其映射至类路径下的 `static/images/project1.jpg`，完成高效读取。

---

## 五、常用运维与排障命令 (Troubleshooting)

### 1. 配置测试与零停机平滑重载
```bash
# 1. 验证配置文件语法是否正确 (确保配置零错误)
sudo nginx -t

# 2. 平滑重新加载配置 (不中断现有客户端连接与会话)
sudo nginx -s reload
# 或使用 systemctl
sudo systemctl reload nginx
```

### 2. 状态与实时日志排查
```bash
# 查看 Nginx 运行状态
sudo systemctl status nginx

# 实时追踪访问日志
sudo tail -f /var/log/nginx/access.log

# 实时追踪错误日志 (排查 502/403/404)
sudo tail -f /var/log/nginx/error.log
```

### 3. 高频错误排查速查

| HTTP 状态码 | 典型诱因 | 快速排查与解决方案 |
| :--- | :--- | :--- |
| **502 Bad Gateway** | Spring Boot 容器未启动或端口 8080 未就绪 | 检查后端容器状态：`docker ps`；查看后端日志：`docker logs -f portfolio-backend-prod`；测试端口：`curl -I http://127.0.0.1:8080/actuator/health`。 |
| **403 Forbidden** | Nginx 无权读取静态目录文件 | 检查 `/var/www/listen_portfolio_web` 权限：`sudo chmod -R 755 /var/www/listen_portfolio_web`；检查父目录 `/var/www` 是否具有可执行权限（`+x`）；若开启了 SELinux，执行 `chcon -Rt httpd_sys_content_t /var/www`。 |
| **413 Request Entity Too Large**| 上传的图片或请求体超过 Nginx 限制 | 在 `server` 或 `location` 块中配置 `client_max_body_size 10m;` 并 `nginx -s reload`。 |
| **404 Not Found (静态图片)** | 正则 location 截断了代理请求 | 检查 `/api/` 规则是否配置了 `^~` 优先匹配修饰符；确认 Spring Boot 的 `WebConfig` 路径映射与类路径文件是否存在。 |

---

## 六、网关架构演进与治理规划 (Roadmap)

根据本项目当前的边缘网关与动静分离架构现状，在 [`docs/todo.md`](file:///c:/Users/liste/Downloads/github/ListenPortfolioBackend/docs/todo.md) **第 18 章节（Nginx 边缘网关与高性能动静分离架构演进）** 中已建立以下改进规划：

1. **Gzip 与 Brotli 静态预压缩加固 (Brotli & Gzip Pre-compression for Flutter WASM)**：
   - 针对 Flutter Web 编译生成的 `canvaskit.wasm` 和 `main.dart.js` 等关键大文件启用 `gzip_static on;`，构建阶段生成 `.br` 与 `.gz` 文件，消除实时压缩对 EC2 单核 CPU 的消耗，网络下行体积骤减 60%~75%。
2. **Upstream 连接池与长连接优化 (Upstream Keepalive & Microservice Decoupling)**：
   - 引入 `upstream` 逻辑块并配置 `keepalive 32;`，避免频繁的短连接 TCP 握手与挥手导致本地端口分配耗尽与 TIME_WAIT 积压。
3. **边缘网关安全响应头与隐藏敏感文件过滤 (Security Headers & Hidden File Shielding)**：
   - 启用 `server_tokens off;` 隐藏 Nginx 版本号，配置 `location ~ /\. { deny all; }` 彻底杜绝 `.git`、`.env` 等隐藏敏感文件泄漏。
4. **访问日志 JSON 结构化与全栈可观测性采集 (Nginx Access Log JSON Structuring)**：
   - 将 Nginx 访问日志升级为结构化 JSON 格式，便于直接对接 Loki / ELK 采集系统，打通全链路耗时与状态码追踪。
