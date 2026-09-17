# AWS 生产环境部署、运维与实战指令手册 (AWS EC2 & DevOps Operations Manual)

> 基于当前生产环境真实运行拓扑编撰，涵盖 AWS EC2 实例连接、Docker Compose 微服务多容器编排、Nginx 反向代理与动静分离、GitHub Actions CI/CD 流水线、Flyway 数据库自动化迁移、磁盘空间自愈引擎与生产级故障排查。
>
> - **EC2 实例规格**: AWS EC2 `t2.micro` (1 vCPU, 1 GiB RAM, 8 GB gp2 EBS 根磁盘)  
> - **公网 IPv4 地址**: `13.218.192.181`  
> - **操作系统**: Amazon Linux 2023 (Kernel 6.1, x86_64)  
> - **基础服务矩阵**:
>   - **Nginx** (Host Port 80/443, 反向代理与 Flutter Web 前端托管)
>   - **Spring Boot 3.4.2** (Container Port 8080, REST API 业务后台)
>   - **MySQL 8.0** (Container Port 3306 / Host Port 3307, 持久化存储)
>   - **Redis 7.2-alpine** (Container / Host Port 6379, 缓存、黑名单与令牌会话)
>   - **Prometheus** (Container / Host Port 9090, 监控度量抓取)
>   - **Grafana** (Container / Host Port 3000, 可视化大盘看板)

---

## 📑 目录

1. [🏛️ 云端架构拓扑与资源约束设计哲学](#1-云端架构拓扑与资源约束设计哲学)
2. [🔑 认证与远程连接指令 (SSH / SCP / Rsync)](#2-认证与远程连接指令-ssh--scp--rsync)
3. [🐳 Docker Compose 容器集群运维手册 (Lifecycle & Profiles)](#3-docker-compose-容器集群运维手册-lifecycle--profiles)
4. [🌐 Nginx 高性能反向代理与静态托管配置 (Nginx Ops)](#4-nginx-高性能反向代理与静态托管配置-nginx-ops)
5. [🗄️ MySQL 数据库运维与 Flyway 版本迁移操作](#5-mysql-数据库运维与-flyway-版本迁移操作)
6. [🚀 CI/CD 自动化流水线与两种部署模式](#6-cicd-自动化流水线与两种部署模式)
7. [🧹 磁盘空间治理与 Docker 孤儿层自愈引擎](#7-磁盘空间治理与-docker-孤儿层自愈引擎)
8. [🔬 重点与难点运维代码及设计实现深度剖析 (Deep-Dive)](#8-重点与难点运维代码及设计实现深度剖析-deep-dive)
9. [⚠️ 现有运维与基础设施不足及改善规划 (Roadmap)](#9-现有运维与基础设施不足及改善规划-roadmap)
10. [🚨 线上应急排查与故障恢复手册 (Troubleshooting Playbook)](#10-线上应急排查与故障恢复手册-troubleshooting-playbook)

---

## 1. 🏛️ 云端架构拓扑与资源约束设计哲学

AWS EC2 `t2.micro` 作为免费套餐实例，其系统物理资源受到严格限制：
- **CPU**: 1 个突发型 vCPU (Baseline 10% CPU 积分消耗模式)
- **内存**: 仅 1024 MB (1 GB)
- **磁盘**: 仅 8 GB EBS 卷 (`/dev/xvda1`)

为了在如此严苛的硬件条件下，稳定支撑 **Spring Boot + MySQL + Redis + Prometheus + Grafana + Nginx + Flutter Web** 完整生产级技术栈，系统在架构上贯彻了四大设计哲学：

```mermaid
graph TD
    User([客户端 / 浏览器 / App]) -->|HTTP :80| Nginx[Nginx Web Server :80]
    subgraph Host EC2 [AWS EC2 t2.micro (1GB RAM / 8GB EBS)]
        Nginx -->|/ 静态资源托管| FlutterWeb[/var/www/listen_portfolio_web/]
        Nginx -->|^~ /api/ 反向代理| App[portfolio-app-1 :8080]
        
        subgraph Docker Bridge [portfolio-network]
            App -->|Flyway 迁移 & 数据读写| MySQL[(portfolio-db-1 :3306)]
            App -->|Token黑名单/Refresh/限流| Redis[(redis-local :6379)]
            Prometheus[prometheus-local :9090] -->|抓取 /actuator/prometheus| App
            Grafana[grafana-local :3000] -->|查询时序数据| Prometheus
        end
        
        CleanTimer[docker-cleanup.timer] -->|每周日自动调用| CleanScript[clean_docker_orphans.py]
    end
```

### 1.1 动静分离与统一反向代理
- 外部所有 HTTP 请求统一由宿主机 Nginx 监听 80 端口。
- 前端 Flutter Web 编译生成的 HTML/JS/CanvasKit 纯静态资源由 Nginx 直接响应，并设置 `expires 7d` 长效客户端缓存，无需穿透容器。
- 动态接口统一前缀 `/api/`，通过 Nginx `proxy_pass http://127.0.0.1:8080/;` 转发至 Spring Boot 容器，内部完成 CORS 处理、请求头透明转发与端口隐藏。

### 1.2 极简 JVM 调优与容器内存受限约束
- 在单机 1GB 内存中，MySQL 8.0 默认占用约 350MB，Redis 约 40MB，Prometheus/Grafana 约 120MB，OS 系统保留约 150MB。
- Spring Boot 应用内存被严格约束在 **256MB 堆上限**：
  ```properties
  JAVA_OPTS: -Xms128m -Xmx256m -XX:+UseG1GC -XX:MaxGCPauseMillis=200
  ```
  采用 G1GC 垃圾收集器平衡吞吐量与停顿时间，规避容器触发 Linux 内核 OOM Killer。

### 1.3 磁盘“零泄漏”自愈闭环
- 构建上下文防膨胀：EC2 侧动态维护 `.dockerignore`，只向 Docker Daemon 发送 60MB WAR 包产物，杜绝将工程源码和中间产物打入镜像层。
- 孤儿层清理引擎：自研 `tools/clean_docker_orphans.py`，构建后自动通过 `docker inspect` 拓扑反查，彻底清除悬空中间物理目录。
- 日志尺寸硬限制：所有 Docker 容器配置 `json-file` 驱动，`max-size: 20m`，`max-file: 2`，杜绝日志无限膨胀撑爆磁盘。

---

## 2. 🔑 认证与远程连接指令 (SSH / SCP / Rsync)

所有连接均依赖私钥文件 `tools/listen.pem`，需确保本地私钥权限安全（Linux/Mac 执行 `chmod 400 tools/listen.pem`）。

### 2.1 SSH 终端连接
```bash
# 标准 SSH 登录（默认用户：ec2-user）
ssh -i tools/listen.pem ec2-user@13.218.192.181

# 跳过 HostKey 交互提示（适用于自动化脚本与 CI 环境）
ssh -i tools/listen.pem -o StrictHostKeyChecking=no ec2-user@13.218.192.181

# 带超时与重试参数的防抖连接（应对网络抖动）
ssh -i tools/listen.pem -o ConnectTimeout=30 -o ConnectionAttempts=5 ec2-user@13.218.192.181
```

### 2.2 文件传输指令 (SCP & Rsync)
```bash
# 1. 单向上传本地打包好的 WAR 文件至 EC2 部署目标目录
scp -i tools/listen.pem build/libs/portfolio-0.0.1-SNAPSHOT.war ec2-user@13.218.192.181:~/portfolio/target/

# 2. 上传工程配置文件（Dockerfile、docker-compose、运维工具脚本）
scp -i tools/listen.pem Dockerfile docker-compose.yml ec2-user@13.218.192.181:~/portfolio/
scp -i tools/listen.pem -r tools ec2-user@13.218.192.181:~/portfolio/

# 3. 从 EC2 下载远程应用容器日志或导出的 SQL 备份至本地
scp -i tools/listen.pem ec2-user@13.218.192.181:~/portfolio/backup.sql ./backup.sql

# 4. 使用 rsync 进行断点续传与增量同步（可选）
rsync -avz -e "ssh -i tools/listen.pem" --progress build/libs/portfolio-0.0.1-SNAPSHOT.war ec2-user@13.218.192.181:~/portfolio/target/
```

---

## 3. 🐳 Docker Compose 容器集群运维手册 (Lifecycle & Profiles)

服务端微服务基于 `docker-compose.yml` 统一编排，包含 `--profile local` 环境声明。

### 3.1 常用集群管理指令
```bash
# 进入工作目录
cd ~/portfolio

# 1. 启动全量微服务集群（在后台运行并在启动前重新构建 App 镜像）
sudo docker compose --profile local up -d --build

# 2. 增量热更新（仅重新编译 app 容器，无需重启 MySQL 与 Redis，耗时仅需数秒）
sudo docker compose --profile local build --no-cache app && sudo docker compose --profile local up -d app

# 3. 查看当前集群所有容器状态、运行端口与健康检查结果 (Health status)
sudo docker compose --profile local ps

# 4. 停止所有微服务（保留数据卷）
sudo docker compose --profile local stop

# 5. 完全销毁容器与网络（保留数据库数据卷，数据安全）
sudo docker compose --profile local down

# 6. 【高危】彻底销毁容器、网络并抹除所有持久化数据卷 (Volume Reset)
sudo docker compose --profile local down -v
```

### 3.2 容器实时日志与诊断
```bash
# 查看 Spring Boot 主应用日志（实时滚动，显示最后 100 行）
sudo docker logs -f --tail 100 portfolio-app-1

# 查看 MySQL 数据库容器日志
sudo docker logs --tail 50 portfolio-db-1

# 查看 Redis 缓存容器日志
sudo docker logs --tail 50 redis-local

# 查看 Prometheus 与 Grafana 状态
sudo docker logs --tail 50 prometheus-local
sudo docker logs --tail 50 grafana-local
```

### 3.3 容器内部调试交互
```bash
# 进入 Spring Boot 应用容器交互终端 (Alpine sh)
sudo docker exec -it portfolio-app-1 sh

# 检查容器内部 JVM 实时内存分配
sudo docker exec portfolio-app-1 java -XX:+PrintFlagsFinal -version | grep -iE 'heapsize|metaspace'

# 检查容器内 Spring Boot 监听端口
sudo docker exec portfolio-app-1 netstat -tuln
```

---

## 4. 🌐 Nginx 高性能反向代理与静态托管配置 (Nginx Ops)

### 4.1 配置文件结构与位置
- **主配置入口**: `/etc/nginx/nginx.conf`
- **作品集站点独立配置**: `/etc/nginx/conf.d/listen_portfolio.conf`
- **前端静态文件根路径**: `/var/www/listen_portfolio_web/`

### 4.2 核心配置原语解析
```nginx
server {
    listen 80;
    server_name _;
    client_max_body_size 10m;  # 允许最大 10MB 请求体 (满足 3MB Base64 头像上传)

    # 1. 前端 Flutter Web 单页面静态托管
    location / {
        root /var/www/listen_portfolio_web;
        index index.html;
        try_files $uri $uri/ /index.html; # SPA 路由重定向兜底
    }

    # 2. 后端动态 API 反向代理
    # ^~ 前缀匹配修饰符：确保优先级高于正则 location，防止图片静态规则捕获后端动态资源
    location ^~ /api/ {
        # 处理 CORS 跨域 OPTIONS 预检请求
        if ($request_method = 'OPTIONS') {
            add_header 'Access-Control-Allow-Origin' '*' always;
            add_header 'Access-Control-Allow-Methods' 'GET, POST, PUT, DELETE, PATCH, OPTIONS' always;
            add_header 'Access-Control-Allow-Headers' '*' always;
            add_header 'Access-Control-Max-Age' 1728000 always;
            add_header 'Content-Type' 'text/plain; charset=utf-8' always;
            add_header 'Content-Length' 0 always;
            return 204;
        }

        # 核心代理重写：末尾包含 /，实现将 /api/v1/projects 自动映射为后端 /v1/projects
        proxy_pass http://127.0.0.1:8080/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    # 3. 静态资源长效客户端缓存加速
    location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg|wasm|otf|ttf|woff|woff2)$ {
        root /var/www/listen_portfolio_web;
        expires 7d;
        add_header Cache-Control "public, no-transform";
    }
}
```

### 4.3 Nginx 服务运维指令
```bash
# 检查 Nginx 配置文件语法是否正确 (修改配置后必执行)
sudo nginx -t

# 平滑重载配置（零停机更新）
sudo nginx -s reload

# 查看 Nginx 运行状态
sudo systemctl status nginx

# 查看 Nginx 访问日志与错误日志
sudo tail -f /var/log/nginx/access.log
sudo tail -f /var/log/nginx/error.log
```

---

## 5. 🗄️ MySQL 数据库运维与 Flyway 版本迁移操作

MySQL 8.0 运行于独立容器 `portfolio-db-1` 中，数据持久化存储于 Docker Volume `portfolio_db_data`。

### 5.1 进入数据库命令行
```bash
# 在宿主机直接免交互执行 SQL 查询
sudo docker exec portfolio-db-1 mysql -u root -pLs-88888888 portfolio -e "SELECT * FROM projects;"

# 交互式进入 MySQL 控制台
sudo docker exec -it portfolio-db-1 mysql -u root -pLs-88888888 portfolio
```

### 5.2 Flyway 数据库迁移状态审查
```bash
# 查询 Flyway 迁移历史表，确认当前数据库处于的版本及迁移执行耗时
sudo docker exec portfolio-db-1 mysql -u root -pLs-88888888 portfolio -e "SELECT installed_rank, version, description, type, script, checksum, execution_time, success FROM flyway_schema_history;"
```
> **预期输出**：
> ```text
> installed_rank  version  description            type      script                       checksum    success
> 1               0        << Flyway Baseline >>  BASELINE  << Flyway Baseline >>        NULL        1
> 2               1        Create initial tables  SQL       V1__Create_initial_tables.sql 2077431072  1
> 3               2        Add test data          SQL       V2__Add_test_data.sql        493360139   1
> ```

### 5.3 数据备份与紧急恢复
```bash
# 1. 导出完整数据库备份 (SQL Dump)
sudo docker exec portfolio-db-1 mysqldump -u root -pLs-88888888 portfolio > ~/portfolio/portfolio_backup_$(date +%Y%m%d_%H%M%S).sql

# 2. 从本地 SQL 备份恢复数据库
sudo docker exec -i portfolio-db-1 mysql -u root -pLs-88888888 portfolio < ~/portfolio/portfolio_backup_xxx.sql
```

---

## 6. 🚀 CI/CD 自动化流水线与两种部署模式

项目配置了完整的 GitHub Actions 流水线（`.github/workflows/ci.yml`），每次向 `master` 分支推送代码时自动运行。

### 6.1 流水线触发与环境守卫
1. **触发时机**：`push: branches: [master]` 或 `pull_request`。
2. **构建测试阶段**：
   - 启动 Gradle 8.5 守护进程并执行全量单测：`./gradlew clean test -Dtest=!PerformanceTest`。
   - 生成 JaCoCo 测试覆盖率报告。
   - 执行生产 WAR 包编译：`./gradlew bootWar`。
3. **部署阶段 (Deploy to AWS EC2)**：
   - 提取 GitHub Secrets 证书建立 SSH 握手（带 `ConnectTimeout 30` 与 `ConnectionAttempts 5` 防抖）。
   - 执行预清理并上传最新 WAR 包及配置文件。
   - 动态识别 Git Commit Message，执行分流部署。

### 6.2 两种部署模式对比与触发方式

| 部署模式 | 触发方式 (Commit 关键词) | 底层执行动作 | 适用场景 |
|---------|------------------------|-------------|---------|
| **增量部署 (默认)** | 任意常规提交信息 | `docker compose build --no-cache app && docker compose up -d` | 日常代码迭代、BUG 修复。**保留 MySQL 与 Redis 数据卷**，数据不丢失。 |
| **清库重构部署 (Clean Deploy)** | 包含 `clean deploy` 或 `deploy-clean` | `docker compose down -v && docker compose build --no-cache app && docker compose up -d` | 数据库表结构发生不兼容重大重构、重置测试数据种子时使用。**物理清除数据卷**并重新执行全量 Flyway 迁移。 |

---

## 7. 🧹 磁盘空间治理与 Docker 孤儿层自愈引擎

针对 8GB EBS 磁盘经常满载的问题，系统建立了双层治理防线。

### 7.1 手动与应急空间排查
```bash
# 1. 查看宿主机当前磁盘空间分布
df -h /

# 2. 查看 Docker 占用的磁盘总览 (Images, Containers, Local Volumes, Build Cache)
sudo docker system df

# 3. 运行自动化深度自愈引擎（核心脚本）
sudo python3 ~/portfolio/tools/clean_docker_orphans.py
```

### 7.2 Systemd 周期自动化维护
脚本已挂载为 Linux 系统服务与定时器：
- **服务配置**: `/etc/systemd/system/docker-cleanup.service`
- **定时器配置**: `/etc/systemd/system/docker-cleanup.timer`
- **调度周期**: 每周日凌晨 03:00:00 自动触发，无须人工干预。

```bash
# 查看定时器下次触发时间与历史执行记录
systemctl list-timers --all | grep docker-cleanup

# 手动立刻触发一次清理服务
sudo systemctl start docker-cleanup.service

# 查看清理服务执行日志
journalctl -u docker-cleanup.service --no-pager -n 30
```

---

## 8. 🔬 重点与难点运维代码及设计实现深度剖析 (Deep-Dive)

### 8.1 难点 1: GraphDriver 拓扑反查安全清理算法 (`clean_docker_orphans.py`)
- **技术痛点**：在 Docker 持续构建中，部分 overlay2 目录因中断而成为“孤儿”，常规 `docker system prune` 无法识别；直接使用 `rm -rf /var/lib/docker/overlay2/*` 则会立刻破坏现有正在运行的容器根文件系统。
- **解决算法**：
  1. 通过 `docker inspect <container/image>` 提取运行中对象的 `GraphDriver.Data`；
  2. 获取 `LowerDir`（以冒号分割的多层基础层）、`UpperDir`（读写层）和 `WorkDir`；
  3. 将所有引用的目录名加入 `used_layers` 保护白名单；
  4. 读取物理磁盘目录，仅删除属于差集 `all_dirs - used_layers` 的孤儿目录，并调用 `find -xtype l -delete` 清除死软链接，做到 100% 绝对安全且精准释放空间。

### 8.2 难点 2: Nginx `^~` 修饰符与 URL 重写斜杠陷阱
- **技术痛点**：如果配置为普通的 `location /api/`，当有静态文件规则 `location ~* \.(jpg|png)$` 存在时，请求 `http://host/api/images/project1.jpg` 会被正则 location 优先劫持，导致前端找不到后端的图片资源报 404。
- **解决方案**：使用 `^~` 前缀匹配修饰符：
  ```nginx
  location ^~ /api/ {
      proxy_pass http://127.0.0.1:8080/;
  }
  ```
  `^~` 告诉 Nginx 一旦前缀匹配成功，**立刻停止向后搜索任何正则 location**；且 `proxy_pass` 末尾带有 `/`，会自动将 `/api/v1/projects` 剥离重写为 `/v1/projects`，优雅实现前后端契约对齐。

### 8.3 难点 3: Docker Compose 健康检查编排依赖 (`condition: service_healthy`)
- **技术痛点**：Spring Boot 启动速度极快，而 MySQL 8.0 容器拉起后需要约 10~15 秒进行初始化（创建表空间、监听 socket）。如果使用传统的 `depends_on: [db]`，Spring Boot 在 MySQL 尚未就绪时尝试连接数据库，会直接抛出 `CommunicationsException: Connection refused` 并崩溃退出。
- **解决方案**：在 `docker-compose.yml` 中引入容器探针：
  ```yaml
  db:
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost", "-u", "root", "-pLs-88888888"]
      interval: 10s
      timeout: 5s
      retries: 5
  app:
    depends_on:
      db:
        condition: service_healthy
  ```
  Docker Compose 仅在 MySQL `mysqladmin ping` 返回 0 且状态标记为 `healthy` 后，才正式启动 `portfolio-app-1` 容器，从根源上杜绝了启动时序竞争故障。

---

## 9. ⚠️ 现有运维与基础设施不足及改善规划 (Roadmap)

通过本次深度运维审计，识别出以下 4 项基础设施瓶颈（已收录至 [todo.md](file:///c:/Users/liste/Downloads/github/ListenPortfolioBackend/docs/todo.md)）：

1. **AWS 弹性 IP (EIP) 绑定与安全组端口收敛**：
   - 当前公网 IP `13.218.192.181` 为动态分配，需绑定 EIP 固化；
   - 安全组应限制仅公网暴露 80、443 与受限 SSH (22)，将 8080、3307、6379 收敛至本地回环网络。
2. **1GB 物理内存下的 Swap 虚拟内存保护**：
   - 5 个容器运行在 1GB 内存上，水位长期处于 85%~90%；
   - 需建立 2GB Swap 分区 (`/swapfile`) 并配置 `vm.swappiness=10`，防止突发流量触发 Linux 内核 OOM Killer 杀掉数据库进程。
3. **蓝绿双容器交替发布与零停机平滑割接**：
   - 目前单容器重新构建期间有 15~25 秒 502 窗口；
   - 后续可配置双端口（8080/8081）交替拉起，待健康探针就绪后执行 `nginx -s reload` 零停机平滑割接。
4. **MySQL 数据库定时云端异地归档备份**：
   - 目前依赖单一 EBS 块卷，缺少异地快照；
   - 需配置定时 Cron 执行 `mysqldump` 压缩并加密上传至私有 AWS S3 存储桶。

---

## 10. 🚨 线上应急排查与故障恢复手册 (Troubleshooting Playbook)

### 故障场景 1: 访问接口返回 `502 Bad Gateway`
```bash
# 步骤 1: 检查 Spring Boot 容器是否处于运行状态
sudo docker ps | grep portfolio-app-1

# 步骤 2: 查看应用容器最后崩溃日志
sudo docker logs --tail 50 portfolio-app-1

# 常见原因 A: Flyway 迁移脚本存在非法编码字符或 SQL 语法错误
# 解决: 修复迁移脚本后重新构建 app 容器。

# 常见原因 B: 磁盘已满导致应用无法创建临时日志
# 解决: 运行 `sudo python3 ~/portfolio/tools/clean_docker_orphans.py` 释放磁盘。
```

### 故障场景 2: 部署报 `No space left on device`
```bash
# 步骤 1: 彻底清理系统日志与悬空缓存
sudo python3 ~/portfolio/tools/clean_docker_orphans.py

# 步骤 2: 清理历史无用的旧版本 Docker 镜像
sudo docker image prune -a -f

# 步骤 3: 检查清理后磁盘空间 (确保可用空间 > 500MB)
df -h /
```

### 故障场景 3: 访问接口报 `500 Internal Server Error (Table doesn't exist)`
```bash
# 步骤 1: 检查数据库表是否存在
sudo docker exec portfolio-db-1 mysql -u root -pLs-88888888 portfolio -e "SHOW TABLES;"

# 步骤 2: 检查 Flyway 历史记录表
sudo docker exec portfolio-db-1 mysql -u root -pLs-88888888 portfolio -e "SELECT * FROM flyway_schema_history;"

# 步骤 3: 如果历史表记录异常，执行清库重置部署
cd ~/portfolio && sudo docker compose --profile local down -v && sudo docker compose --profile local up -d --build
```

### 故障场景 4: 容器高负载与内存耗尽排查
```bash
# 步骤 1: 实时查看所有容器的 CPU 与内存消耗
sudo docker stats --no-stream

# 步骤 2: 查看宿主机内存总体使用率
free -h

# 步骤 3: 查看 Linux 系统是否有触发 OOM Killer 杀掉进程的记录
sudo dmesg -T | grep -i -E 'killed process|oom_reaper'
```

---

📅 **手册最后修订时间**: 2026-09-17  
👨‍💻 **运维支持**: Listen Portfolio Cloud Operations
