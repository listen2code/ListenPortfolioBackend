# 开发环境设置指南

## 概述

本指南详细介绍了如何搭建 ListenPortfolioBackend 项目的完整开发环境，包括本地开发、Docker 开发、IDE 配置等。按照本指南操作，您可以快速建立一个功能完整的开发环境。

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

#### 环境变量配置（可选）

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

### Docker Compose 配置

**docker-compose.override.yml**（开发环境覆盖）：
```yaml
version: '3.8'

services:
  app:
    volumes:
      - .:/app  # 挂载源代码
      - /app/target  # 排除构建目录
    environment:
      - SPRING_PROFILES_ACTIVE=dev
      - JAVA_OPTS=-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005
    ports:
      - "5005:5005"  # 调试端口

  db:
    ports:
      - "3307:3306"  # 本地访问端口
    volumes:
      - mysql_data_dev:/var/lib/mysql
      - ./docker/mysql/init:/docker-entrypoint-initdb.d

  redis:
    ports:
      - "6379:6379"
    volumes:
      - redis_data_dev:/data

  mailhog:
    image: mailhog/mailhog:latest
    ports:
      - "1025:1025"  # SMTP 端口
      - "8025:8025"  # Web UI 端口
    profiles:
      - local

volumes:
  mysql_data_dev:
  redis_data_dev:
```

## 💻 IDE 配置

### IntelliJ IDEA 配置

#### 1. 项目导入

```bash
# 1. 打开 IntelliJ IDEA
# 2. 选择 "Open" -> 选择项目根目录
# 3. 等待 Maven 依赖下载完成
# 4. 设置 Project SDK 为 Java 17
```

#### 2. 代码格式化

```xml
<!-- .editorconfig -->
root = true

[*]
charset = utf-8
end_of_line = lf
indent_size = 4
indent_style = space
insert_final_newline = true
trim_trailing_whitespace = true

[*.java]
indent_size = 4

[*.{yml,yaml}]
indent_size = 2
```

#### 3. 代码模板

**Live Templates**：
```java
// logger 模板
private static final Logger logger = LoggerFactory.getLogger($CLASS_NAME$.class);

// test 模板
@Test
@DisplayName("$TEST_DESCRIPTION$")
void $TEST_METHOD_NAME$() {
    // Given
    $END$
    
    // When
    
    // Then
}
```

#### 4. 调试配置

```json
{
  "type": "java",
  "name": "Portfolio Application",
  "request": "launch",
  "mainClass": "com.listen.portfolio.PortfolioApplication",
  "projectName": "ListenPortfolioBackend",
  "args": "",
  "vmOptions": "-Dspring.profiles.active=docker",
  "env": {
    "SPRING_PROFILES_ACTIVE": "docker"
  }
}
```

### VS Code 配置

#### 1. 扩展安装

```json
{
  "recommendations": [
    "redhat.java",
    "vscjava.vscode-java-pack",
    "ms-vscode.vscode-spring-boot",
    "ms-vscode.vscode-spring-boot-dashboard",
    "ms-vscode-remote.remote-containers",
    "ms-vscode.vscode-docker",
    "humao.rest-client"
  ]
}
```

#### 2. 工作区配置

**.vscode/settings.json**：
```json
{
  "java.home": "/path/to/java-17",
  "java.configuration.updateBuildConfiguration": "automatic",
  "spring-boot.ls.checkJVM": false,
  "files.exclude": {
    "**/target": true,
    "**/.classpath": true,
    "**/.project": true,
    "**/.settings": true,
    "**/.factorypath": true
  },
  "editor.formatOnSave": true,
  "editor.codeActionsOnSave": {
    "source.organizeImports": true
  }
}
```

#### 3. 调试配置

**.vscode/launch.json**：
```json
{
  "version": "0.2.0",
  "configurations": [
    {
      "type": "java",
      "name": "Launch Portfolio Application",
      "request": "launch",
      "mainClass": "com.listen.portfolio.PortfolioApplication",
      "projectName": "ListenPortfolioBackend",
      "args": "",
      "vmOptions": "-Dspring.profiles.active=docker",
      "env": {
        "SPRING_PROFILES_ACTIVE": "docker"
      }
    }
  ]
}
```

## 🔧 开发工具配置

### Maven 配置

#### Maven Wrapper 更新

```bash
# 更新 Maven Wrapper 到最新版本
./mvnw wrapper:wrapper -DmavenWrapperVersion=3.9.0

# 验证版本
./mvnw -version
```

#### 本地 Maven 配置

**~/.m2/settings.xml**：
```xml
<settings>
  <profiles>
    <profile>
      <id>dev</id>
      <properties>
        <spring.profiles.active>dev</spring.profiles.active>
      </properties>
    </profile>
  </profiles>
  
  <activeProfiles>
    <activeProfile>dev</activeProfile>
  </activeProfiles>
  
  <mirrors>
    <mirror>
      <id>aliyun-maven</id>
      <mirrorOf>central</mirrorOf>
      <url>https://maven.aliyun.com/repository/central</url>
    </mirror>
  </mirrors>
</settings>
```

### Git 配置

#### Git Hooks

```bash
# 安装 pre-commit hook
cat > .git/hooks/pre-commit << 'EOF'
#!/bin/sh
# 运行测试
./mvnw test -q
if [ $? -ne 0 ]; then
    echo "Tests failed, commit aborted"
    exit 1
fi

# 代码格式检查
./mvnw spotbugs:check -q
if [ $? -ne 0 ]; then
    echo "Code quality checks failed, commit aborted"
    exit 1
fi
EOF

chmod +x .git/hooks/pre-commit
```

#### Git 配置

```bash
# 设置用户信息
git config user.name "Your Name"
git config user.email "your.email@example.com"

# 设置默认分支名
git config init.defaultBranch main

# 设置编辑器
git config core.editor "code --wait"
```

## 📊 开发监控

### 本地监控设置

```bash
### 启动本地监控栈

docker-compose --profile local up -d  # 包含 Prometheus + Grafana

# 访问 Grafana
open http://localhost:3000

# 访问 Prometheus
open http://localhost:9090
```

### 日志配置

**logback-dev.xml**：
```xml
<configuration>
    <include resource="org/springframework/boot/logging/logback/defaults.xml"/>
    
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>
    
    <logger name="com.listen.portfolio" level="DEBUG"/>
    <logger name="org.springframework.security" level="DEBUG"/>
    <logger name="org.springframework.web" level="DEBUG"/>
    
    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
    </root>
</configuration>
```

### 性能监控

```bash
# 启用 JFR（Java Flight Recorder）
java -XX:+FlightRecorder -XX:StartFlightRecording=duration=60s,filename=recording.jfr \
     -jar target/portfolio-0.0.1-SNAPSHOT.jar

# 使用 JProfiler（商业工具）
jprofiler -port 8849 -pid $(pgrep java)
```

## 🧪 测试开发

### 测试环境配置

**application-test.properties**：
```properties
# 使用 H2 内存数据库进行测试
spring.datasource.url=jdbc:h2:mem:testdb
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=

# JPA 测试配置
spring.jpa.hibernate.ddl-auto=create-drop
spring.jpa.show-sql=true

# Redis 测试配置（使用嵌入式 Redis）
spring.data.redis.host=localhost
spring.data.redis.port=6370

# 邮件测试配置
spring.mail.host=localhost
spring.mail.port=1025
```

### 测试数据管理

```java
@TestConfiguration
public class TestDataConfig {
    
    @Bean
    @Primary
    public DataSource testDataSource() {
        return new EmbeddedDatabaseBuilder()
            .setType(EmbeddedDatabaseType.H2)
            .addScript("schema.sql")
            .addScript("test-data.sql")
            .build();
    }
}
```

## 🔍 调试指南

### 远程调试

#### 1. 启动远程调试

```bash
# 启动应用时启用调试
java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005 \
     -jar target/portfolio-0.0.1-SNAPSHOT.jar
```

#### 2. IDE 连接调试

**IntelliJ IDEA**：
1. Run -> Edit Configurations
2. Add New Configuration -> Remote JVM Debug
3. Host: localhost, Port: 5005
4. 点击 Debug

**VS Code**：
```json
{
  "type": "java",
  "name": "Attach to Remote",
  "request": "attach",
  "hostName": "localhost",
  "port": 5005
}
```

### 日志调试

```bash
# 启用详细日志
export JAVA_OPTS="$JAVA_OPTS -Dlogging.level.com.listen.portfolio=TRACE"

# 启用 SQL 日志
export JAVA_OPTS="$JAVA_OPTS -Dlogging.level.org.springframework.jdbc=DEBUG"

# 启用 Spring Security 日志
export JAVA_OPTS="$JAVA_OPTS -Dlogging.level.org.springframework.security=TRACE"
```

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
./mvnw idea:idea  # IntelliJ
./mvnw eclipse:eclipse  # Eclipse

# 清理 IDE 文件
rm -rf .idea .classpath .project .factorypath
```

### 性能问题

#### 1. 内存不足

```bash
# 增加 JVM 内存
export JAVA_OPTS="-Xms1024m -Xmx2048m"

# 查看 JVM 内存使用
jstat -gc -t $(pgrep java) 5s

# 生成内存转储
jmap -dump:format=b,file=heap.hprof $(pgrep java)
```

#### 2. 数据库性能

```bash
# 查看 MySQL 慢查询
SHOW VARIABLES LIKE 'slow_query_log';
SHOW VARIABLES LIKE 'long_query_time';

# 分析查询性能
EXPLAIN SELECT * FROM users WHERE username = 'test';
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
