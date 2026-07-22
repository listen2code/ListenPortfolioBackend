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
2. **配置安全组 (Security Group)**：
   确保放行以下入站规则端口：
   * `22` (SSH) — 用于远程连接和传输构建物。
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
  # 通过云端 CloudFormation 堆栈声明一键拉起 ECS Fargate 任务
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
