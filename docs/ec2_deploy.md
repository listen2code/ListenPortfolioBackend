# 部署到 AWS 指南 (本地构建版)

本指南介绍如何**在本地构建 WAR 包**，然后将其上传到 AWS EC2，并使用 Docker Compose 启动应用和数据库。这种方式可以避免在服务器上安装 Maven 和下载大量依赖，也能规避服务器上 Docker Buildx 的兼容性问题。

## 1. 准备 AWS EC2 实例

1.  登录 AWS 控制台，启动一个新的 **EC2 实例**。
    *   **AMI**: 推荐使用 *Amazon Linux 2023* 或 *Ubuntu Server 22.04 LTS*。
    *   **实例类型**: 建议至少使用 `t3.small` (2GB 内存) 以支持 Java 应用和 MySQL。
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

# 安装 Docker Compose (独立二进制文件，无需 buildx)
sudo curl -L "https://github.com/docker/compose/releases/download/1.29.2/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose
```
*注：执行完 `usermod` 后，建议断开 SSH 并重新连接以使用户组更改生效。*

### Ubuntu:
```bash
sudo apt-get update
sudo apt-get install -y docker.io
sudo usermod -aG docker $USER
# 安装 Docker Compose
sudo curl -L "https://github.com/docker/compose/releases/download/1.29.2/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose
# 重新登录生效
```

## 3. 本地构建 WAR 包

在你的本地开发机（Windows/Mac）上执行 Maven 打包命令。

**Windows (CMD/PowerShell):**
```cmd
cd "C:\...\portfolio"
mvnw clean package -DskipTests
```

构建成功后，你会在 `target` 目录下看到一个 `.war` 文件（例如 `portfolio-0.0.1-SNAPSHOT.war`）。

## 4. 上传文件到 EC2

你需要将编译好的 WAR 包和 Docker 配置文件上传到 EC2 实例。

**需要上传的文件：**
1.  `target/portfolio-0.0.1-SNAPSHOT.war` (重命名为 `app.war` 上传，方便配置)
2.  `Dockerfile`
3.  `docker-compose.yml`

**使用 SCP 上传示例 (在本地终端执行):**

```bash
# 1. 创建远程目录
ssh -i tool/listen.pem ec2-user@18.181.198.209 "mkdir -p ~/portfolio"

# 2. 上传 WAR 包 (重命名为 app.war)
scp -i tool/listen.pem target/portfolio-0.0.1-SNAPSHOT.war ec2-user@18.181.198.209:~/portfolio/app.war

# 3. 上传配置文件
scp -i tool/listen.pem Dockerfile docker-compose.yml ec2-user@18.181.198.209:~/portfolio/
```

## 5. 启动服务

在 EC2 实例上，进入项目目录并启动。

```bash
cd portfolio
## clean old db
docker compose down -v
## start build
docker compose up -d --build
```

*注意：这里使用的是 `docker compose` (带连字符)，它不需要 buildx 插件。*

## 6. 验证部署

1.  查看日志：
    ```bash
    docker compose logs -f
    ```
2.  在浏览器中访问：
    `http://18.181.198.209:8080`

---

## 附录：配置文件调整

为了配合这种部署方式，请确保你的 `Dockerfile` 和 `docker-compose.yml` 内容如下：

### Dockerfile (简化版)
由于我们直接上传了 WAR 包，Dockerfile 不需要再进行 Maven 构建，只需要运行环境。

```dockerfile
FROM eclipse-temurin:17-jdk
VOLUME /tmp
# 直接复制上传的 app.war
COPY app.war app.war
ENTRYPOINT ["java","-jar","/app.war"]
```

### docker-compose.yml
```yaml
version: '3.8'
services:
  app:
    build: .
    ports:
      - "8080:8080"
    depends_on:
      - db
    environment:
      SPRING_DATASOURCE_URL: jdbc:mysql://db:3306/portfolio?useSSL=false&serverTimezone=Asia/Tokyo&allowPublicKeyRetrieval=true
      SPRING_DATASOURCE_USERNAME: root
      SPRING_DATASOURCE_PASSWORD: Ls-88888888
    networks:
      - portfolio-network

  db:
    image: mysql:8.0
    restart: always
    environment:
      MYSQL_DATABASE: portfolio
      MYSQL_ROOT_PASSWORD: Ls-88888888
    ports:
      - "3307:3306"
    volumes:
      - db_data:/var/lib/mysql
    networks:
      - portfolio-network

networks:
  portfolio-network:
    driver: bridge

volumes:
  db_data:
```
