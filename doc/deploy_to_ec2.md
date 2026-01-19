# 部署到 AWS 指南

本项目已配置为使用 Docker 和 Docker Compose 进行容器化部署。以下是将项目（包含应用和 MySQL 数据库）部署到 AWS EC2 实例的步骤。

## 1. 准备 AWS EC2 实例

1.  登录 AWS 控制台，启动一个新的 **EC2 实例**。
    *   **AMI**: 推荐使用 *Amazon Linux 2023* 或 *Ubuntu Server 22.04 LTS*。
    *   **实例类型**: `t2.micro` (免费层) 可能内存不足，建议至少使用 `t3.small` 或 `t3.medium` 以支持 Java 应用和 MySQL。
    *   **安全组 (Security Group)**: 确保开放以下端口：
        *   `22` (SSH) - 用于远程连接。
        *   `8080` - 用于访问 Web 应用。

## 2. 在 EC2 上安装 Docker

通过 SSH 连接到你的 EC2 实例，并执行以下命令安装 Docker、Buildx 和 Docker Compose。

### Amazon Linux 2023:
```bash
sudo yum update -y
sudo yum install -y docker
sudo service docker start
sudo usermod -a -G docker ec2-user

# 创建插件目录 (系统级)
sudo mkdir -p /usr/local/lib/docker/cli-plugins

# 安装 Docker Compose
sudo curl -SL https://github.com/docker/compose/releases/latest/download/docker-compose-linux-x86_64 -o /usr/local/lib/docker/cli-plugins/docker-compose
sudo chmod +x /usr/local/lib/docker/cli-plugins/docker-compose

# 安装 Docker Buildx (关键步骤)
sudo curl -SL https://github.com/docker/buildx/releases/latest/download/buildx-linux-amd64 -o /usr/local/lib/docker/cli-plugins/docker-buildx
sudo chmod +x /usr/local/lib/docker/cli-plugins/docker-buildx
```
*注：执行完 `usermod` 后，建议断开 SSH 并重新连接以使用户组更改生效。*

### Ubuntu:
```bash
# 设置 Docker 官方仓库以获取最新版本（推荐）
sudo apt-get update
sudo apt-get install -y ca-certificates curl gnupg
sudo install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg
sudo chmod a+r /etc/apt/keyrings/docker.gpg

echo \
  "deb [arch="$(dpkg --print-architecture)" signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu \
  "$(. /etc/os-release && echo "$VERSION_CODENAME")" stable" | \
  sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

sudo apt-get update
sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin

sudo usermod -aG docker $USER
# 重新登录生效
```

## 3. 上传项目文件

你需要将项目文件上传到 EC2 实例。你可以使用 `scp` 命令或者在服务器上安装 `git` 并拉取代码。

**需要上传的文件/目录：**
*   `src/`
*   `pom.xml`
*   `Dockerfile`
*   `docker-compose.yml`

**使用 SCP 示例 (在本地终端执行):**
```bash
# 假设你的密钥文件是 key.pem，EC2 地址是 ec2-user@1.2.3.4
scp -i /path/to/key.pem -r src pom.xml Dockerfile docker-compose.yml ec2-user@1.2.3.4:/home/ec2-user/gallery/
```

## 4. 启动服务

在 EC2 实例上，进入项目目录并启动 Docker Compose：

```bash
cd gallery
docker compose up -d --build
```

*   `--build`: 强制构建镜像。
*   `-d`: 后台运行。

## 5. 验证部署

等待几分钟让构建完成并启动服务。

1.  查看日志确保没有错误：
    ```bash
    docker compose logs -f
    ```
2.  在浏览器中访问：
    `http://<EC2-公网-IP>:8080`

## 常见问题与故障排除

### 问题 1: "docker: 'buildx' is not a docker command"

如果按照上述步骤安装后仍然报错，请尝试以下方案：

**方案 A：重启 Docker 服务**
```bash
sudo service docker restart
```

**方案 B：安装到用户目录（推荐尝试）**
有时候系统目录 `/usr/local/lib/...` 不在 Docker 的搜索路径中。尝试安装到当前用户的目录下：
```bash
mkdir -p ~/.docker/cli-plugins
curl -SL https://github.com/docker/buildx/releases/download/v0.30.1/buildx-v0.30.1.linux-amd64 -o ~/.docker/cli-plugins/docker-buildx
chmod +x ~/.docker/cli-plugins/docker-buildx
```
安装完成后，运行 `docker buildx version` 验证。

**方案 C：检查文件是否下载正确**
运行 `ls -l /usr/local/lib/docker/cli-plugins/docker-buildx`。如果文件大小很小（例如几 KB），可能是下载失败（如下载到了 HTML 错误页）。请检查网络连接或 URL。

### 问题 2: "ERROR [internal] load metadata for docker.io/library/openjdk:17-jdk-slim "
* 
```
FROM openjdk:17-jdk-slim
改成：
FROM eclipse-temurin:17-jdk
```

### 问题 3: "Access denied for user 'root'@'172.18.0.3'"
* openjdk:17-jdk-slim 镜像已经从 Docker Hub 下架，不存在了

```
CREATE USER 'appuser'@'%' IDENTIFIED BY 'yourpassword';
GRANT ALL PRIVILEGES ON *.* TO 'appuser'@'%';
FLUSH PRIVILEGES;
```

* 然后在 Spring Boot 的 application.properties 改成

```
spring.datasource.username=appuser
spring.datasource.password=yourpassword
spring.datasource.url=jdbc:mysql://mysql:3306/yourdb?useSSL=false&serverTimezone=Asia/Tokyo

```

