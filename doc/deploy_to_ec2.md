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

通过 SSH 连接到你的 EC2 实例，并执行以下命令安装 Docker 和 Docker Compose。

### Amazon Linux 2023:
```bash
sudo yum update -y
sudo yum install -y docker
sudo service docker start
sudo usermod -a -G docker ec2-user
# 安装 Docker Compose 插件
sudo mkdir -p /usr/local/lib/docker/cli-plugins
sudo curl -SL https://github.com/docker/compose/releases/latest/download/docker-compose-linux-x86_64 -o /usr/local/lib/docker/cli-plugins/docker-compose
sudo chmod +x /usr/local/lib/docker/cli-plugins/docker-compose
```
*注：执行完 `usermod` 后，建议断开 SSH 并重新连接以使用户组更改生效。*

### Ubuntu:
```bash
sudo apt-get update
sudo apt-get install -y docker.io docker-compose-plugin
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

## 注意事项

*   **数据库数据**: 数据库文件存储在 Docker 卷 `db_data` 中，重启容器数据不会丢失。
*   **生产环境建议**: 对于正式的生产环境，建议使用 AWS RDS 托管数据库，而不是在 Docker 中运行 MySQL，以获得更好的性能和备份功能。如果使用 RDS，请修改 `docker-compose.yml` 中的环境变量 `SPRING_DATASOURCE_URL` 指向 RDS 端点。
