# 开发环境设置指南

**Status**: `Implemented with Current-Setup Scope`

> 本文档仅记录当前仓库中可验证的开发环境配置与启动方式。
> 如与仓库文件冲突，以 `docker-compose.yml`、`src/main/resources/application*.properties`、`README.md` 为准。
> 当前配置策略是：`application.properties` 作为主配置，`application-docker.properties` 只覆盖 Docker 网络差异。

## 概述

本指南用于帮助你快速拉起 ListenPortfolioBackend 的当前开发环境。重点是最小可运行路径，而不是覆盖所有可能的个人化工具链配置。

## 🎯 环境要求

### 基础环境

| 组件 | 最低版本 | 推荐版本 | 说明 |
|------|----------|----------|------|
| **Java** | 17 | 17 LTS | 必须使用 Java 17 |
| **Maven** | 3.8.0 | 3.9.0+ | 构建工具 |
| **MySQL** | 8.0 | 8.0.33+ | 数据库 |
| **Redis** | 7.0 | 7.2+ | 缓存和黑名单 |
| **Git** | 2.30 | 2.40+ | 版本控制 |

### 开发工具

| 工具 | 推荐版本 | 说明 |
|------|----------|------|
| **IDE** | IntelliJ IDEA 2023.3+ / VS Code | Java 开发 |
| **Docker** | 20.10+ | 容器化开发 |
| **Docker Compose** | 2.0+ | 多容器编排 |
| **Postman** | 10.0+ | API 测试 |
| **Redis Desktop** | 2023+ | Redis 管理 |

## 🚀 快速开始

### 1. 克隆项目

```bash
# 克隆项目
git clone <repository-url>
cd ListenPortfolioBackend

# 查看项目结构
tree -L 2
```

### 2. 环境配置

#### 环境变量配置（推荐）

**方式 1：系统环境变量（推荐本地开发）**
```bash
# PowerShell
$env:DB_USERNAME="your_db_user"
$env:DB_PASSWORD="your_db_password"
$env:JWT_SECRET="your-super-strong-secret-key-at-least-256-bits-long"
$env:MAIL_PASSWORD="your-gmail-app-password"

# Linux/Mac
export DB_USERNAME="your_db_user"
export DB_PASSWORD="your_db_password"
export JWT_SECRET="your-super-strong-secret-key-at-least-256-bits-long"
export MAIL_PASSWORD="your-gmail-app-password"
```

**方式 2：Docker 部署（使用 .env 文件）**
```bash
cp .env.example .env
# 编辑 .env 文件填入真实密码
```

> 本地直接执行 `./mvnw spring-boot:run` 时，默认使用 `application.properties` 中的 localhost 配置，无需额外指定 `dev` profile。

### 3. 启动依赖服务

#### 使用 Docker Compose（推荐）

```bash
# 启动数据库和 Redis
docker-compose up -d db redis

# 查看服务状态
docker-compose ps

# 查看日志
docker-compose logs -f db redis
```

#### 手动安装（可选）

```bash
# 安装 MySQL 8.0
# macOS
brew install mysql@8.0
brew services start mysql@8.0

# Ubuntu/Debian
sudo apt update
sudo apt install mysql-server-8.0

# 安装 Redis 7
# macOS
brew install redis@7.0
brew services start redis@7.0

# Ubuntu/Debian
sudo apt install redis-server
```

### 4. 数据库初始化

```bash
# 创建数据库
mysql -u root -p -e "CREATE DATABASE portfolio CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"

# 创建用户（可选）
mysql -u root -p -e "CREATE USER 'portfolio_user'@'localhost' IDENTIFIED BY 'secure_password';"
mysql -u root -p -e "GRANT ALL PRIVILEGES ON portfolio.* TO 'portfolio_user'@'localhost';"
mysql -u root -p -e "FLUSH PRIVILEGES;"
```

### 5. 启动应用

```bash
# 方式 1：使用 Maven Wrapper
./mvnw spring-boot:run

# 方式 2：使用 IDE 运行 PortfolioApplication.java

# 方式 3：构建后运行
./mvnw clean package -DskipTests
java -jar target/portfolio-0.0.1-SNAPSHOT.jar
```

> Docker 场景下由 `docker-compose.yml` 注入 `SPRING_PROFILES_ACTIVE=docker`；本地直跑通常不需要显式设置该变量。

### 6. 验证安装

```bash
# 健康检查
curl http://localhost:8080/actuator/health

# API 文档访问
open http://localhost:8080/swagger-ui.html

# Prometheus 指标
curl http://localhost:8080/actuator/prometheus
```

## 🐳 Docker 开发环境

### Docker 全栈开发

```bash
# 一键部署全栈（推荐）
./docker_deploy.ps1

# 手动启动全栈
cp .env.example .env  # 可选：覆盖默认密码
docker-compose --profile local up -d --build

# 查看所有服务
docker-compose ps

# 查看应用日志
docker-compose logs -f app
```

### 当前 Docker 入口说明

当前仓库没有 `docker-compose.override.yml`。开发时请直接以根目录下的 `docker-compose.yml` 为准：

- `app`：Spring Boot 应用，自动注入 `SPRING_PROFILES_ACTIVE=docker`
- `db`：MySQL 8，默认映射到宿主机 `3307`
- `redis`：Redis 7，默认映射到宿主机 `6379`
- `prometheus`：指标采集，默认 `9090`
- `grafana`：监控面板，默认 `3000`

`application-docker.properties` 当前只覆盖两项：

```properties
spring.datasource.url=jdbc:mysql://db:3306/portfolio?...
spring.data.redis.host=redis
```

## 💻 IDE 配置

### 推荐最小配置

本项目不要求统一的 IDE 模板文件。你只需要保证：

- 使用 Java 17
- 以 Maven 项目方式导入仓库
- 运行主类 `com.listen.portfolio.PortfolioApplication`
- 本地直跑时不强制指定 profile；Docker 调试时再显式设置 `SPRING_PROFILES_ACTIVE=docker`

如需远程调试，可在自行启动 JVM 时添加 JDWP 参数；这属于个人调试方式，不是仓库默认配置。

## 📊 开发监控

### 本地监控入口

当前监控栈来自 `docker-compose.yml`：

- Grafana: `http://localhost:3000`
- Prometheus: `http://localhost:9090`
- Actuator Prometheus: `http://localhost:8080/actuator/prometheus`

当前日志配置文件为 `classpath:logback-spring.xml`，并不存在 `logback-dev.xml`。

## 🧪 测试开发

### 当前测试配置说明

仓库中存在 `src/main/resources/application-test.properties` 与 `src/test/resources/application-test.properties`，但测试是否能完整通过仍依赖具体外部服务状态，尤其是部分 Redis 相关测试。

因此这里不再把某一份测试配置文件解释为“完整测试环境方案”，更建议直接以实际测试命令和失败日志为准：

```bash
./mvnw test
./mvnw clean test jacoco:report
```

## 🔍 调试指南

### 常用调试方式

- 本地直跑：`./mvnw spring-boot:run`
- Docker 全栈：`docker-compose --profile local up -d --build`
- 查看应用日志：`docker-compose logs -f app`
- 查看健康检查：`curl http://localhost:8080/actuator/health`

如需更细粒度日志，可通过 JVM 参数或环境变量临时覆盖 `logging.level.*`，不建议为此维护额外的本地专用日志配置文件。

## 🚨 故障排除

### 常见问题

#### 1. 端口冲突

```bash
# 查看端口占用
lsof -i :8080
netstat -tulpn | grep :8080

# 杀死占用进程
kill -9 $(lsof -t -i:8080)
```

#### 2. 数据库连接失败

```bash
# 检查 MySQL 状态
brew services list | grep mysql
systemctl status mysql

# 测试数据库连接
mysql -h localhost -P 3307 -u root -p

# 重置数据库密码
mysql_secure_installation
```

#### 3. Redis 连接问题

```bash
# 检查 Redis 状态
redis-cli ping

# 重启 Redis
brew services restart redis
systemctl restart redis

# 查看 Redis 日志
tail -f /usr/local/var/log/redis.log
```

#### 4. Maven 依赖问题

```bash
# 清理 Maven 缓存
./mvnw clean

# 重新下载依赖
./mvnw dependency:resolve

# 强制更新快照
./mvnw -U clean install
```

#### 5. IDE 识别问题

```bash
# 重新导入 Maven 项目
# IntelliJ / VS Code 中执行 Reimport Maven Project 即可

# 命令行刷新依赖
./mvnw -U dependency:resolve
```

## 📚 开发资源

### 有用的链接

- [Spring Boot 官方文档](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [Spring Security 参考](https://docs.spring.io/spring-security/reference/)
- [MySQL 8.0 文档](https://dev.mysql.com/doc/refman/8.0/en/)
- [Redis 文档](https://redis.io/documentation)
- [Maven 用户指南](https://maven.apache.org/guides/)

### 开发工具推荐

| 工具 | 用途 | 推荐理由 |
|------|------|----------|
| **Postman** | API 测试 | 界面友好，支持自动化测试 |
| **Redis Desktop Manager** | Redis 管理 | 图形化管理界面 |
| **DBeaver** | 数据库管理 | 支持多种数据库 |
| **JProfiler** | 性能分析 | 强大的 Java 性能分析工具 |
| **Actuator** | 应用监控 | Spring Boot 内置监控 |

### 学习资源

- [Spring Boot 实战教程](https://spring.io/guides/)
- [Java 17 新特性](https://openjdk.org/jeps/)
- [Docker 入门指南](https://docs.docker.com/get-started/)
- [Git Pro 书籍](https://git-scm.com/book)

---

## 🎯 下一步

设置完开发环境后，您可以：

1. **阅读项目文档**：了解项目架构和功能
2. **运行测试套件**：确保环境正常工作
3. **尝试 API 调用**：使用 Postman 测试接口
4. **查看监控面板**：了解应用运行状态
5. **开始开发**：创建新的功能或修复问题

---

**最后更新**: 2026-03-31  
**维护者**: Development Team
