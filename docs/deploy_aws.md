# AWS 部署与云原生运维指南

本指南介绍如何将 Web 应用部署到 AWS 平台。主要包括两部分内容：
1. **轻量级单实例方案 (EC2 + Docker Compose)**：适合开发联调、演示及低成本轻量级环境运行，已针对 `t2.micro` 等低内存实例进行了性能加固。
2. **进阶云原生方案 (ECS Fargate / EKS / 基础设施即代码)**：适合生产环境、高可用自动弹性伸缩架构的选型与最佳实践。

---

## 第一部分：AWS EC2 容器化部署方案 (本地构建与低内存实例)

### 1. 准备 AWS EC2 实例与安全组

1. **选择实例与系统**：
   * **AMI**: 推荐使用 *Amazon Linux 2023*。
   * **实例类型**: `t2.micro` (1GB 内存，免费套餐适用) 或 `t3.small`。
2. **配置安全组 (Security Group) 入站规则**：
   确保放行以下端口以允许外部及自动化流水线访问：
   * `22` (SSH) — **必须放行 `0.0.0.0/0` (Anywhere)**，否则 GitHub Actions 的动态 IP 虚拟执行环境将无法连入部署。因为已禁用密码并强制密钥验证，所以这是绝对安全的。
   * `8080` — 用于外部设备访问 Web 应用 API（如果是真机调试，建议将源设置为 `0.0.0.0/0`）。
   * `3000` — （可选）用于访问 Grafana 监控大盘。

### 2. 低内存实例加固：配置 2GB Swap 虚拟内存 (关键)

对于 `t2.micro` (1GB 内存) 实例，同时跑起 MySQL + Spring Boot + 监控组件会因内存不足引起系统死锁挂起。开机后必须首先分配 Swap 虚拟内存（**本操作完全免费，仅占用已有磁盘空间**）：

通过 SSH 连接到 EC2 实例，并执行以下命令：
```bash
# 1. 写入一个 2GB 的虚拟内存占位文件
sudo dd if=/dev/zero of=/swapfile bs=128M count=16

# 2. 设置安全权限
sudo chmod 600 /swapfile

# 3. 建立并激活交换分区
sudo mkswap /swapfile
sudo swapon /swapfile

# 4. 设置开机自动挂载
echo '/swapfile swap swap defaults 0 0' | sudo tee -a /etc/fstab

# 5. 验证是否配置成功 (会看到 Swap 一行显示有 2.0G 空间)
free -h
```

### 3. 在 Amazon Linux 2023 上安装 Docker 与 Docker Compose v2

在 EC2 终端依次执行以下命令：

#### 3.1 安装并开启 Docker 运行时
```bash
# 安装 Docker 引擎
sudo dnf update -y
sudo dnf install -y docker

# 启动并设置开机自启
sudo systemctl enable --now docker

# 将当前登录用户加入 docker 组以获取免 sudo 权限
sudo usermod -aG docker ec2-user
```
*注：执行完 `usermod` 后，建议断开当前 SSH 并重新连接以使组更改生效。*

#### 3.2 手动安装最新的 Docker Compose v2 (CLI 插件模式)
```bash
# 创建 CLI 插件目录
sudo mkdir -p /usr/libexec/docker/cli-plugins

# 下载最新 x86_64 架构的 Docker Compose 二进制包 (以 v2.29.1 为例)
sudo curl -SL "https://github.com/docker/compose/releases/download/v2.29.1/docker-compose-linux-x86_64" -o /usr/libexec/docker/cli-plugins/docker-compose

# 授权执行权限
sudo chmod +x /usr/libexec/docker/cli-plugins/docker-compose

# 建立软链接使 docker-compose (带连字符) 命令同样可用
sudo ln -sf /usr/libexec/docker/cli-plugins/docker-compose /usr/local/bin/docker-compose

# 验证版本
docker compose version
docker-compose --version
```

### 4. 本地打包 Web 工程 (WAR)

在您的本地开发机（Windows / Mac）的后端项目根路径下，通过 Maven 快速打包：
```powershell
./mvnw clean package -DskipTests
```
构建成功后，在本地的 `target/` 目录下生成 `portfolio-0.0.1-SNAPSHOT.war` 文件。

### 5. 通过 SCP 将部署文件上传至 EC2

我们只需上传运行时必要的文件，在本地终端中执行：

```powershell
# 1. 登录 EC2 创建目标目录结构
ssh -i tool/listen.pem ec2-user@<EC2_PUBLIC_IP> "mkdir -p ~/portfolio/target"

# 2. 上传编译好的本地 WAR 包至 target 目录下（以适配 Dockerfile 的 COPY 指令）
scp -i tool/listen.pem target/portfolio-0.0.1-SNAPSHOT.war ec2-user@<EC2_PUBLIC_IP>:~/portfolio/target/

# 3. 递归上传 Dockerfile、docker-compose.yml 以及整个监控配置目录
scp -i tool/listen.pem Dockerfile docker-compose.yml ec2-user@<EC2_PUBLIC_IP>:~/portfolio/
scp -i tool/listen.pem -r monitoring ec2-user@<EC2_PUBLIC_IP>:~/portfolio/
```

### 6. EC2 容器构建与一键式启动

通过 SSH 重新登录到 EC2 实例：
```bash
# 进入部署目录
cd ~/portfolio

# 1. 彻底清理旧的无效卷或残留容器
docker compose --profile local down -v

# 2. 拉起 local 规格的微服务及监控集群并后台运行
docker compose --profile local up -d --build
```
> [!IMPORTANT]
> **参数说明**：
> - `--profile local`：必须指定，用于激活 `docker-compose.yml` 中的应用及 Prometheus、Grafana 等所有包含在该 profile 下的服务。
> - `-d`：后台静默运行。
> - `--build`：重新在云端打包并部署。

### 7. 部署健康监测与验证
* **访问接口**：`http://<EC2_PUBLIC_IP>:8080/v1/projects`（成功返回格式化的项目 JSON 数据）。
* **查看容器**：`docker compose --profile local ps`
* **查看日志**：`docker compose --profile local logs -f app`

---

### 8. GitHub Actions 自动化 CI/CD 部署配置

后端项目已集成 GitHub Actions 自动化流水线。在您向分支推送代码时，流水线可自动执行编译、单测、代码规范扫描以及向 AWS EC2 的一键式热发布。

#### 8.1 GitHub 仓库机密变量 (Secrets) 配置
要使 GitHub Actions 拥有向 AWS 实例部署的权限，必须在您的 GitHub 仓库的 **Settings -> Secrets and variables -> Actions** 中配置以下两个核心密钥：
* **`AWS_HOST`**：您的 EC2 实例的最新弹性公网 IP 或者是静态 IP（如 `13.218.192.181`），**切勿包含 `http://` 或端口号**。
* **`AWS_SSH_KEY`**：登录 EC2 的密钥对私钥文件的**完整文本内容**（即 `listen.pem` 文件内容，包含首尾的 `-----BEGIN...` 标识符）。

此外，为了保护云端部署的邮件验证等敏感环境变量，您可以在 GitHub Secrets 中配置以下**可选/推荐机密变量**：
* **`MAIL_USERNAME`**：发信邮箱账号（例如 `listen2code@gmail.com`，如果不配置，默认使用默认邮箱）。
* **`MAIL_PASSWORD`**：发信邮箱应用授权码密码（例如 `xqvfldvtlgbjvdnn`，如果不配置，默认使用当前最新验证的应用密码）。
* **`DB_PASSWORD`**：云端 MySQL 数据库的 root 密码（如不配置，自动使用默认密码 `Ls-88888888`）。
* **`JWT_SECRET`**：JWT 签名强密钥（在 CI 自动化部署时，流水线会读取 GitHub Actions Secrets 中的同名变量动态写入云端 `.env`；如不配置，自动使用系统默认的安全密钥）。

*如果未在 GitHub Secrets 中配置这些可选变量，CI 流程将默认使用项目当前已配置且通过验证的默认测试账号和最新发信密码进行安全部署，实现开箱即用。*

#### 8.2 GitHub Actions 部署稳定性架构设计
在 `.github/workflows/ci.yml` 中，针对常见的 CI 部署环境进行了如下稳定性保障：
* **免除 ssh-keyscan 报错依赖**：CI 虚拟环境直接在全局 `~/.ssh/config` 下配置了 `StrictHostKeyChecking no` 和 `UserKnownHostsFile /dev/null`，免去了对 `ssh-keyscan` 命令的调用，彻底避免因服务器防火墙暂时阻断该命令或者 IP 格式兼容引起的 CI 构建崩溃。
* **基于环境变量解密多行私钥**：在 Step 级别通过 `env` 将 GitHub Secret 注入，使用 Shell 自带的环境变量解密，避免了多行 PEM 证书直接在 YAML 执行区转义导致的格式破损或意外的 EOF。

#### 8.3 智能部署模式（增量 vs. 清库）
流水线默认采用安全的**增量热部署**，仅在特定指令下才执行**清库重置部署**：
* **常规增量部署（默认，保留数据）**：
  - **触发条件**：常规 Git Push，或者 Commit 消息中不包含清库指令。
  - **云端指令**：`docker compose up -d --build`
  - **表现**：**保留数据库和缓存中的全部数据卷**。以增量方式热替换后端应用包，停机时间控制在 3-5 秒，数据 100% 安全。
* **清库重置部署（特定指令）**：
  - **触发条件**：Git Commit 消息中包含 **`clean deploy`** 或 **`deploy-clean`**。
  - **云端指令**：`docker compose down -v && docker compose up -d --build`
  - **表现**：**清除包括 MySQL 数据库数据在内的所有 Docker 挂载数据卷**，并在容器重启时执行 Flyway 重新生成全新的空表及初始测试账号。适用于需要将环境数据进行大版本重构或彻底清洗的场景。

---

### 9. 常见部署故障诊断与 FAQ (Troubleshooting)

#### ❓ 故障一：SSH 连接在握手阶段提示 `Connection timed out during banner exchange` 或直接死锁挂起
* **原因**：EC2 实例物理内存（1GB）已被 MySQL、Spring Boot 和监控服务完全榨干。Linux 内核陷入内存页频繁换出的“内存抖动”（Thrashing）死锁中，导致系统包含 SSH 守护进程在内的所有服务处于死机状态。
* **解决方法**：
  1. 在 AWS 网页端控制台找到该实例，选择 **“实例状态 (Instance State)” -> “停止实例 (Stop instance)”**（如果常规停止超时，请勾选 **“强制停止 (Force stop)”**）。
  2. 待实例完全变为 Stopped 状态后，点击 **“启动实例 (Start instance)”**。
  3. 重新连入后，**立即按照本指南第 2 步配置 2GB Swap 虚拟交换分区**。

#### ❓ 故障二：GitHub Actions 运行部署步骤时，报错 `ssh: connect to host *** port 22: Connection timed out`
* **原因**：AWS 安全组的 SSH `22` 端口没有对 GitHub Actions 虚拟执行环境开放。因为 GitHub 的流水线服务器 IP 范围是动态的，如果只限制了您本人的公网 IP 访问 22 端口，流水线流量将会被 AWS 防火墙直接静默丢弃。
* **解决方法**：
  1. 登录 AWS EC2 控制台，找到应用绑定的安全组（Security Group）。
  2. 编辑入站规则，将端口 `22` (SSH) 的允许源修改为 **`0.0.0.0/0`** (允许所有人)。
  3. *安全性备注*：由于该 EC2 实例已在 `sshd_config` 中关闭了传统的密码认证，仅支持私钥证书登录，所以即使端口全开黑客也绝无可能暴力破解，符合安全标准。

---

### 10. 常用连接与运维管理指南 (How to Connect)

为了方便您在本地对 AWS 云端环境进行日常运维管理，以下整理了最常用的几种连接方式：

#### 10.1 SSH 终端连接（远程登录服务器）
在您的本地开发机终端（Windows PowerShell / CMD / Git Bash）中，使用您的 PEM 私钥进行远程连接。
* **连接命令**：
  ```powershell
  # 如果您的密钥在 Windows 默认下载路径，请运行（注意路径包含空格需要加双引号）：
  ssh -i "C:\Users\liste\Downloads\listen (1).pem" ec2-user@13.218.192.181
  ```
* **说明**：将上面的路径和 IP 替换为您的实际私钥路径和最新的 EC2 公网 IP 即可。

#### 10.2 Navicat / DBeaver 数据库连接（图形化连接 MySQL）
生产环境中，出于安全考虑，强烈**不建议**直接将数据库的端口（容器内的 `3306` 映射到宿主机 `3307`）暴露给公网。
推荐使用更安全的 **SSH 隧道 (SSH Tunnel)** 方式进行图形化工具连接：
1. **新建连接**：选择 MySQL 数据库连接类型；
2. **常规设置 (General)**：
   - **主机 (Host)**: `localhost`（注意：必须填 `localhost`，因为是通过 SSH 隧道从服务器内部转发）
   - **端口 (Port)**: `3307` (即宿主机映射的 MySQL 端口)
   - **用户名**: `root`
   - **密码**: `Ls-88888888` (您在 `.env` 中配置的密码)
3. **SSH 设置**：
   - **勾选 "使用 SSH 隧道" (Use SSH Tunnel)**
   - **SSH 主机**: `13.218.192.181` (您的 EC2 公网 IP)
   - **SSH 端口**: `22`
   - **用户名**: `ec2-user`
   - **认证方法**: `公钥 (Public Key)`
   - **私钥文件 (Private Key)**: 选择您本地的 `C:\Users\liste\Downloads\listen (1).pem` 文件。

#### 10.3 外部浏览器访问监控及服务端口
如果您的安全组放行了对应端口，您可以在本地浏览器中直接访问以下面板：
* **Spring Boot 业务接口**：`http://13.218.192.181:8080/v1/projects`
* **Prometheus 指标大盘**：`http://13.218.192.181:9090` (默认安全组阻断，仅内网容器抓取)
* **Grafana 数据可视化看板**：`http://13.218.192.181:3000` (需要安全组放行 `3000` 端口，默认管理员账号密码为 `admin / admin123`)

---

## 第二部分：AWS 进阶云原生方案 (高可用、生产级伸缩)

在将系统正式推向高可用的公网生产环境时，单台 EC2 虚机架构面临单点故障和手工维护成本高的问题。本部分提供了 AWS 核心云托管方案的设计指引。

### 1. 配置文件通用性分析

在向云原生迁移时，我们已有的微服务工程配置表现出了极佳的通用性：

| 模块/配置文件 | 兼容度 | 迁移说明 |
|--------------|-------|---------|
| **application.properties** | ✅ 100% | Spring Boot 配置完全标准，在云端通过系统环境变量覆盖属性即可 |
| **Dockerfile** | ✅ 100% | 适用于 ECS 任务定义或 EKS 的 Pod 节点基础容器环境 |
| **monitoring/prometheus.yml** | ⚠️ 需调整 | 本地静态抓取需调整为 AWS 基于 EC2 动态服务发现（EC2 Service Discovery）模式 |
| **docker-compose.yml** | ❌ 需替代 | 生产环境使用 AWS ECS Task Definition 或 Kubernetes YAML 进行编排 |

---

### 2. 🚀 三大云原生部署方案选型

#### 方案 A: AWS ECS Fargate (首选推荐)
* **适用场景**：无需管理底层虚机的无服务器容器架构，自动水平扩缩容。
* **优势**：
  * **零虚机运维**：不需升级 Linux 内核或担忧单机宕机。
  * **极致的安全隔离**：每个 Task 容器都拥有独立的虚拟化沙箱及专有的 IAM 角色权限。
* **部署命令示例**：
  ```bash
  # 通过云端 CloudFormation 堆堆声明一键拉起 ECS Fargate 任务
  aws cloudformation deploy \
      --template-file aws/cloudformation/monitoring-stack.yaml \
      --stack-name production-portfolio-monitoring \
      --parameter-overrides \
          Environment=production \
          DatabasePassword=your_secure_db_password \
          VpcId=vpc-xxxxxx \
          SubnetIds=subnet-xxx,subnet-yyy \
      --capabilities CAPABILITY_IAM
  ```

#### 方案 B: AWS EKS (托管 Kubernetes)
* **适用场景**：需要 K8s 标准生态、跨云调度或复杂微服务调用链的项目。
* **优势**：
  * 支持 Helm 一键编排；具备强大的服务发现与滚动更新策略。
* **部署步骤**：
  ```bash
  # 1. 在本地配置 EKS 集群连接
  aws eks update-kubeconfig --name production-portfolio-cluster --region us-east-1
  
  # 2. 应用 K8s Deployment 与 Service YAML
  kubectl apply -f aws/k8s/
  ```

---

### 3. 🔧 生产级 AWS 环境配置调整

#### 3.1 环境变量覆盖与数据库云托管 (RDS)
生产环境应废弃容器内的 MySQL，改用高可用的 **AWS RDS (Aurora/MySQL)**，通过环境变量注入 Spring 属性：
```properties
# 数据库：切换为 RDS 域名，使用 SSL 安全连接
spring.datasource.url=jdbc:mysql://portfolio-db-cluster.xxx.us-east-1.rds.amazonaws.com:3306/portfolio?useSSL=true
spring.datasource.username=db_admin
spring.datasource.password=${DATABASE_PASSWORD}
```

#### 3.2 可观测性深度集成
* **CloudWatch Logs**：通过在 ECS 任务定义中配置 `awslogs` 驱动，自动将 Spring Boot 控制台输出的业务和访问日志汇聚到 CloudWatch Logs。
* **AWS X-Ray**：在 POM 中引入 AWS X-Ray SDK。启动时配置 `-javaagent` 拦截器，自动将 Trace ID 贯穿到 AWS 各类托管服务中，渲染系统调用拓扑和服务地图。

---

### 4. 📊 Prometheus 动态服务发现配置 (AWS 版)

生产环境下，微服务容器在 Fargate/EKS 下的 IP 是动态变化的。Prometheus 需要基于 AWS EC2 过滤器进行弹性抓取，而不是使用静态 IP：

```yaml
# monitoring/prometheus-aws.yml
global:
  scrape_interval: 15s

scrape_configs:
  - job_name: 'portfolio-app-aws-discovery'
    # 使用 AWS EC2 动态服务发现
    ec2_sd_configs:
      - region: us-east-1
        port: 8080
        filters:
          - name: 'tag:Environment'
            values: ['production']
          - name: 'tag:Service'
            values: ['portfolio-api']
    metrics_path: '/actuator/prometheus'
```

---

### 5. 🔒 生产安全与成本优化最佳实践

1. **凭证隔离安全**：
   禁止将任何密钥硬编码在镜像或 properties 中。利用 **AWS Secrets Manager** 或 **System Manager (SSM) Parameter Store** 托管密码，在容器启动时动态解密拉取。
2. **ALB 负载均衡与 SSL**：
   在公网入口挂载 **Application Load Balancer (ALB)**，将 HTTPS (443) 证书终止在 ALB（通过 AWS Certificate Manager 免费申请管理证书），后方 App 服务安全收敛在私有子网，仅接受来自 ALB 安全组的入站流量。
3. **成本极客优化 (Fargate Spot)**：
   在非核心开发/测试环境部署时，配置 ECS 任务使用 **Fargate Spot** 计费模式，可降低多达 **70%** 的容器算力账单。
