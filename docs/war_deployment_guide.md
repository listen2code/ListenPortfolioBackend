# WAR 部署完整指南

## 概述

本项目支持 WAR（Web Application Archive）打包方式，适用于传统的 Java EE 应用服务器部署环境。WAR 包方式提供了与现有企业基础设施的良好兼容性，支持多应用共享服务器资源。

## 🎯 为什么选择 WAR 包？

### WAR 包优势
- **企业兼容性**：兼容传统 Java EE 应用服务器
- **资源共享**：多个应用可共享服务器资源和连接池
- **统一管理**：便于企业级环境的统一运维管理
- **JNDI 支持**：支持 JNDI 数据源和资源注入
- **集群部署**：支持应用服务器的集群和负载均衡

### JAR vs WAR 对比

| 特性 | JAR 包 | WAR 包 |
|------|--------|--------|
| **部署方式** | 内嵌服务器 | 外部应用服务器 |
| **启动方式** | `java -jar` | 服务器自动部署 |
| **资源管理** | 应用独占 | 服务器共享 |
| **配置灵活性** | 应用配置 | 服务器级配置 |
| **企业集成** | 有限 | 丰富 |
| **运维复杂度** | 简单 | 中等 |

## 🏗️ 架构适配

### 项目结构适配

```
src/
├── main/
│   ├── java/
│   │   └── com/listen/portfolio/
│   │       ├── PortfolioApplication.java      # 主应用类
│   │       └── ServletInitializer.java         # WAR 包入口
│   ├── resources/
│   │   ├── application.properties              # 应用配置
│   │   └── ...
│   └── webapp/                                # Web 资源目录（可选）
│       ├── WEB-INF/
│       │   ├── web.xml                        # 部署描述符（可选）
│       │   └── ...
│       └── ...
└── test/
    └── ...
```

### 关键适配文件

#### 1. ServletInitializer.java
```java
package com.listen.portfolio;

import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

/**
 * WAR 包部署的 Servlet 初始化器
 * 
 * 说明：
 * - 继承 SpringBootServletInitializer 支持 WAR 包部署
 * - 提供应用配置的入口点
 * - 在外部 Servlet 容器中启动 Spring Boot 应用
 */
public class ServletInitializer extends SpringBootServletInitializer {

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        // 配置应用的主类和源包
        return application
                .sources(PortfolioApplication.class)
                .properties("spring.profiles.active=war");
    }
}
```

#### 2. pom.xml 打包配置
```xml
<packaging>war</packaging>

<dependencies>
    <!-- 内嵌 Tomcat 作用域设置为 provided -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-tomcat</artifactId>
        <scope>provided</scope>
    </dependency>
</dependencies>

<build>
    <plugins>
        <plugin>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-maven-plugin</artifactId>
            <configuration>
                <!-- 排除内嵌 Tomcat，避免冲突 -->
                <excludeGroupIds>
                    <groupId>org.springframework.boot</groupId>
                </excludeGroupIds>
            </configuration>
        </plugin>
    </plugins>
</build>
```

## 🚀 构建与部署

### 构建 WAR 包

```bash
# 1. 清理并构建
./mvnw clean package -DskipTests

# 2. 查看生成的 WAR 包
ls -la target/portfolio-0.0.1-SNAPSHOT.war

# 3. 验证 WAR 包内容
jar tf target/portfolio-0.0.1-SNAPSHOT.war | head -20
```

### WAR 包结构

```
portfolio-0.0.1-SNAPSHOT.war
├── WEB-INF/
│   ├── classes/                    # 编译后的类文件
│   │   ├── application.properties
│   │   └── com/listen/portfolio/
│   ├── lib/                        # 依赖库
│   │   ├── spring-boot-*.jar
│   │   ├── mysql-connector-*.jar
│   │   └── ...
│   ├── generated-web.xml           # 生成的 web.xml
│   └── ...
├── org/                           # Spring Boot 加载器
└── ...
```

## 🐳 应用服务器部署

### Apache Tomcat 部署

#### 1. 准备 Tomcat 环境

```bash
# 下载 Tomcat 9+
wget https://archive.apache.org/dist/tomcat/tomcat-9/v9.0.85/bin/apache-tomcat-9.0.85.tar.gz

# 解压
tar -xzf apache-tomcat-9.0.85.tar.gz

# 设置环境变量
export CATALINA_HOME=/path/to/apache-tomcat-9.0.85
export PATH=$CATALINA_HOME/bin:$PATH
```

#### 2. 配置数据库连接池

**编辑 `$CATALINA_HOME/conf/context.xml`**：
```xml
<Context>
    <!-- JNDI 数据源配置 -->
    <Resource name="jdbc/portfolioDB" 
              auth="Container"
              type="javax.sql.DataSource"
              factory="org.apache.commons.dbcp2.BasicDataSourceFactory"
              driverClassName="com.mysql.cj.jdbc.Driver"
              url="jdbc:mysql://localhost:3306/portfolio?useSSL=false&amp;serverTimezone=Asia/Tokyo"
              username="portfolio_user"
              password="secure_password"
              maxTotal="20"
              maxIdle="5"
              maxWaitMillis="10000"
              validationQuery="SELECT 1"
              testOnBorrow="true"/>
              
    <!-- Redis 连接配置 -->
    <Resource name="redis/redisFactory"
              auth="Container"
              type="org.springframework.data.redis.connection.jedis.JedisConnectionFactory"
              factory="org.apache.naming.factory.BeanFactory"
              hostName="localhost"
              port="6379"
              poolConfig="redisPoolConfig"/>
              
    <!-- Redis 连接池配置 -->
    <Resource name="redisPoolConfig"
              auth="Container"
              type="redis.clients.jedis.JedisPoolConfig"
              factory="org.apache.naming.factory.BeanFactory"
              maxTotal="100"
              maxIdle="50"
              minIdle="10"
              testOnBorrow="true"/>
</Context>
```

#### 3. 部署 WAR 包

```bash
# 方法 1：复制到 webapps 目录
cp target/portfolio-0.0.1-SNAPSHOT.war $CATALINA_HOME/webapps/portfolio.war

# 方法 2：使用 Manager 应用部署
# 访问 http://localhost:8080/manager/html
# 上传 WAR 文件

# 方法 3：使用命令行部署
$CATALINA_HOME/bin/catalina.sh deploy \
    -path /portfolio \
    -file target/portfolio-0.0.1-SNAPSHOT.war
```

#### 4. 启动和验证

```bash
# 启动 Tomcat
$CATALINA_HOME/bin/startup.sh

# 查看启动日志
tail -f $CATALINA_HOME/logs/catalina.out

# 验证应用启动
curl http://localhost:8080/portfolio/actuator/health
```

### JBoss/WildFly 部署

#### 1. 准备 WildFly 环境

```bash
# 下载 WildFly
wget https://download.jboss.org/wildfly/28.0.1.Final/wildfly-28.0.1.Final.zip

# 解压
unzip wildfly-28.0.1.Final.zip

# 启动 WildFly
./wildfly-28.0.1.Final/bin/standalone.sh
```

#### 2. 配置数据源

```bash
# 使用 CLI 工具配置
./wildfly-28.0.1.Final/bin/jboss-cli.sh --connect

# 执行以下命令
data-source add --name=PortfolioDS --jndi-name=java:/jdbc/portfolioDB \
    --driver-name=mysql --connection-url=jdbc:mysql://localhost:3306/portfolio \
    --user-name=portfolio_user --password=secure_password

# 部署应用
deploy target/portfolio-0.0.1-SNAPSHOT.war
```

### IBM WebSphere 部署

#### 1. 准备环境

```bash
# WebSphere 传统部署方式
# 1. 登录 WebSphere 管理控制台
# 2. 配置 JDBC 数据源
# 3. 配置 JMS 资源（如需要）
# 4. 部署 WAR 包
```

#### 2. 配置数据源

```xml
<!-- 在 WebSphere 中配置数据源 -->
<Resource name="jdbc/portfolioDB"
          auth="Container"
          type="javax.sql.DataSource"
          factory="com.ibm.ws.rsadapter.jdbc.DataStoreFactory"
          driverClassName="com.mysql.cj.jdbc.Driver"
          url="jdbc:mysql://db-server:3306/portfolio"
          username="portfolio_user"
          password="secure_password"/>
```

## ⚙️ 配置适配

### 应用配置调整

#### 1. 创建 war-specific 配置

**application-war.properties**：
```properties
# WAR 包特定配置
spring.profiles.active=war
spring.jmx.enabled=true

# JNDI 数据源配置
spring.datasource.jndi-name=java:/jdbc/portfolioDB
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# Redis JNDI 配置
spring.data.redis.jndi-name=java:/redis/redisFactory

# 日志配置
logging.config=classpath:logback-spring.xml
logging.file=/var/log/portfolio/portfolio.log

# 监控配置
management.endpoints.web.exposure.include=health,info,prometheus
management.endpoint.health.show-details=when-authorized
```

#### 2. 修改主应用类

```java
@SpringBootApplication
public class PortfolioApplication {

    public static void main(String[] args) {
        SpringApplication.run(PortfolioApplication.class, args);
    }

    @Bean
    @Profile("war")
    public ServletWebServerFactory servletContainer() {
        // WAR 包部署时禁用内嵌服务器
        return new TomcatServletWebServerFactory() {
            @Override
            public void setPort(int port) {
                // WAR 包不设置端口
            }
        };
    }
}
```

### 环境变量配置

#### Tomcat 环境变量

```bash
# 设置 Java 选项
export JAVA_OPTS="-Xms512m -Xmx2048m -XX:+UseG1GC"

# 设置应用配置
export SPRING_PROFILES_ACTIVE=war
export DB_HOST=localhost
export DB_PORT=3306
export DB_NAME=portfolio
export DB_USERNAME=portfolio_user
export DB_PASSWORD=secure_password

# Redis 配置
export REDIS_HOST=localhost
export REDIS_PORT=6379

# JWT 配置
export JWT_SECRET=your-super-strong-secret-key-for-production
export JWT_EXPIRATION=300000
export JWT_REFRESH_EXPIRATION=86400000
```

#### 系统服务配置

**创建 systemd 服务**：
```ini
# /etc/systemd/system/tomcat.service
[Unit]
Description=Apache Tomcat Web Application Container
After=network.target

[Service]
Type=forking
Environment=CATALINA_PID=/opt/tomcat/tomcat.pid
Environment=JAVA_HOME=/usr/lib/jvm/java-17-openjdk
Environment=CATALINA_HOME=/opt/tomcat
Environment=CATALINA_BASE=/opt/tomcat
Environment='JAVA_OPTS=-Xms512m -Xmx2048m -XX:+UseG1GC'
ExecStart=/opt/tomcat/bin/startup.sh
ExecStop=/opt/tomcat/bin/shutdown.sh
ExecReload=/bin/kill -15 $MAINPID
KillMode=process
RestartSec=10
Restart=always
User=tomcat
Group=tomcat

[Install]
WantedBy=multi-user.target
```

## 🔧 高级配置

### 集群部署

#### Tomcat 集群配置

**server.xml**：
```xml
<!-- 启用集群支持 -->
<Cluster className="org.apache.catalina.ha.tcp.SimpleTcpCluster">
    <Channel className="org.apache.catalina.tribes.group.GroupChannel">
        <Membership className="org.apache.catalina.tribes.membership.McastService"
                   address="228.0.0.4"
                   port="45564"
                   frequency="500"
                   dropTime="3000"/>
        <Receiver className="org.apache.catalina.tribes.transport.nio.NioReceiver"
                  address="auto"
                  port="4000"
                  autoBind="100"
                  selectorTimeout="5000"
                  maxThreads="6"/>
        <Sender className="org.apache.catalina.tribes.transport.ReplicationTransmitter">
            <Transport className="org.apache.catalina.tribes.transport.nio.PooledMultiSender">
                <Transport className="org.apache.catalina.tribes.transport.nio.PooledMultiSender"
                          address="localhost"
                          port="5000"
                          autoBind="false"
                          selectorTimeout="5000"
                          maxThreads="6"/>
            </Transport>
        </Sender>
        <Interceptor className="org.apache.catalina.tribes.group.interceptors.TcpFailureDetector"/>
        <Interceptor className="org.apache.catalina.tribes.group.interceptors.MessageDispatch15Interceptor"/>
    </Channel>
    
    <Valve className="org.apache.catalina.ha.session.ReplicationValve"
           filter=".*\.gif;.*\.js;.*\.jpg;.*\.htm;.*\.html;.*\.css;.*\.png;.*\.jpeg;.*\.swf;.*\.xhtml;.*\.jsp;.*\.jspx;.*\.woff;.*\.woff2;.*\.ttf"/>
    <Deployer className="org.apache.catalina.ha.session.FarmWarDeployer"
             tempDir="/tmp/war-temp/"
             watchDir="/tmp/war-listen/"
             watchEnabled="false"/>
    <ClusterListener className="org.apache.catalina.ha.session.ClusterSessionListener"/>
</Cluster>
```

#### 负载均衡配置

**Apache HTTP Server 配置**：
```apache
# 启用模块
LoadModule proxy_module modules/mod_proxy.so
LoadModule proxy_balancer_module modules/mod_proxy_balancer.so
LoadModule proxy_http_module modules/mod_proxy_http.so

# 负载均衡配置
<Proxy balancer://portfolio-cluster>
    BalancerMember http://node1:8080/portfolio route=node1
    BalancerMember http://node2:8080/portfolio route=node2
    ProxySet stickysession=JSESSIONID|jsessionid
</Proxy>

# 虚拟主机配置
<VirtualHost *:80>
    ServerName portfolio.example.com
    ProxyPass / balancer://portfolio-cluster/
    ProxyPassReverse / balancer://portfolio-cluster/
    
    # 健康检查
    ProxyPass /balancer-manager !
    <Location /balancer-manager>
        SetHandler balancer-manager
        Require ip 127.0.0.1
    </Location>
</VirtualHost>
```

### SSL/TLS 配置

#### Tomcat SSL 配置

**server.xml**：
```xml
<Connector port="8443"
           protocol="org.apache.coyote.http11.Http11NioProtocol"
           maxThreads="150"
           SSLEnabled="true"
           scheme="https"
           secure="true"
           keystoreFile="/etc/ssl/certs/portfolio.jks"
           keystorePass="changeit"
           clientAuth="false"
           sslProtocol="TLS"
           sslEnabledProtocols="TLSv1.2,TLSv1.3"
           ciphers="TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384,TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384"/>
```

#### 应用 HTTPS 配置

**application-war.properties**：
```properties
# 强制 HTTPS
server.ssl.enabled=true
server.ssl.key-store=classpath:keystore.p12
server.ssl.key-store-password=changeit
server.ssl.key-store-type=PKCS12
server.ssl.key-alias=portfolio

# 重定向到 HTTPS
server.servlet.context-parameters.redirect_https=true
```

## 🔍 监控与运维

### 应用监控

#### JMX 监控配置

```xml
<!-- 启用 JMX -->
<Listener className="org.apache.catalina.mbeans.JmxRemoteLifecycleListener"
          rmiRegistryPortPlatform="8081"
          rmiServerPortPlatform="8082"/>
```

#### 健康检查端点

```bash
# 应用健康检查
curl http://localhost:8080/portfolio/actuator/health

# 详细健康信息
curl http://localhost:8080/portfolio/actuator/health/details

# Prometheus 指标
curl http://localhost:8080/portfolio/actuator/prometheus
```

### 日志管理

#### 日志配置

**logback-spring.xml**：
```xml
<configuration>
    <!-- 控制台输出 -->
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder class="net.logstash.logback.encoder.LoggingEventCompositeJsonEncoder">
            <providers>
                <timestamp/>
                <logLevel/>
                <loggerName/>
                <message/>
                <mdc/>
            </providers>
        </encoder>
    </appender>
    
    <!-- 文件输出 -->
    <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>/var/log/portfolio/portfolio.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>/var/log/portfolio/portfolio.%d{yyyy-MM-dd}.log.gz</fileNamePattern>
            <maxHistory>30</maxHistory>
        </rollingPolicy>
        <encoder class="net.logstash.logback.encoder.LoggingEventCompositeJsonEncoder">
            <providers>
                <timestamp/>
                <logLevel/>
                <loggerName/>
                <message/>
                <mdc/>
            </providers>
        </encoder>
    </appender>
    
    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
        <appender-ref ref="FILE"/>
    </root>
</configuration>
```

#### 日志轮转

**logrotate 配置**：
```bash
# /etc/logrotate.d/portfolio
/var/log/portfolio/*.log {
    daily
    rotate 30
    compress
    delaycompress
    missingok
    notifempty
    create 644 tomcat tomcat
    postrotate
        /bin/kill -USR1 `cat /var/run/tomcat/tomcat.pid 2> /dev/null` 2> /dev/null || true
    endscript
}
```

## 🚨 故障排除

### 常见问题

#### 1. 应用启动失败

**症状**：Tomcat 启动但应用无法访问

**排查步骤**：
```bash
# 1. 查看应用日志
tail -f $CATALINA_HOME/logs/catalina.out | grep "portfolio"

# 2. 检查 WAR 包完整性
jar tf target/portfolio-0.0.1-SNAPSHOT.war | grep WEB-INF/web.xml

# 3. 验证依赖冲突
$CATALINA_HOME/bin/diagnostic.sh --war target/portfolio-0.0.1-SNAPSHOT.war
```

#### 2. 数据库连接失败

**症状**：应用启动时数据库连接错误

**解决方案**：
```bash
# 1. 检查 JNDI 配置
grep -A 10 "jdbc/portfolioDB" $CATALINA_HOME/conf/context.xml

# 2. 测试数据库连接
mysql -h localhost -u portfolio_user -p portfolio

# 3. 检查驱动程序
ls $CATALINA_HOME/webapps/portfolio/WEB-INF/lib/mysql-connector*.jar
```

#### 3. Redis 连接问题

**症状**：Token 黑名单功能异常

**排查步骤**：
```bash
# 1. 检查 Redis 服务
redis-cli ping

# 2. 验证 JNDI 配置
grep -A 5 "redis/" $CATALINA_HOME/conf/context.xml

# 3. 测试应用连接
curl http://localhost:8080/portfolio/actuator/health | jq '.components.redis.status'
```

#### 4. 内存不足

**症状**：OutOfMemoryError 或频繁 GC

**解决方案**：
```bash
# 1. 调整 JVM 内存
export JAVA_OPTS="-Xms1024m -Xmx4096m -XX:MaxMetaspaceSize=512m"

# 2. 启用 GC 日志
export JAVA_OPTS="$JAVA_OPTS -XX:+PrintGCDetails -XX:+PrintGCTimeStamps -Xloggc:/var/log/portfolio/gc.log"

# 3. 监控内存使用
jstat -gc -t $(pgrep java) 5s
```

### 性能调优

#### JVM 调优

```bash
# 生产环境 JVM 参数
JAVA_OPTS="-Xms2048m -Xmx4096m \
           -XX:+UseG1GC \
           -XX:MaxGCPauseMillis=100 \
           -XX:G1HeapRegionSize=16m \
           -XX:+UseStringDeduplication \
           -XX:+OptimizeStringConcat \
           -Djava.security.egd=file:/dev/./urandom \
           -Dspring.profiles.active=war"
```

#### 连接池优化

```xml
<!-- Tomcat 连接池优化 -->
<Resource name="jdbc/portfolioDB" 
          auth="Container"
          type="javax.sql.DataSource"
          factory="org.apache.commons.dbcp2.BasicDataSourceFactory"
          driverClassName="com.mysql.cj.jdbc.Driver"
          url="jdbc:mysql://localhost:3306/portfolio?useSSL=false&amp;serverTimezone=Asia/Tokyo"
          username="portfolio_user"
          password="secure_password"
          maxTotal="50"
          maxIdle="20"
          minIdle="10"
          maxWaitMillis="30000"
          validationQuery="SELECT 1"
          testOnBorrow="true"
          testOnReturn="false"
          testWhileIdle="true"
          timeBetweenEvictionRunsMillis="30000"
          minEvictableIdleTimeMillis="60000"/>
```

## 🎯 最佳实践

### 1. 部署策略

- **蓝绿部署**：维护两个相同环境，零停机部署
- **滚动更新**：逐步更新集群节点
- **金丝雀发布**：先小范围测试，再全量发布

### 2. 安全加固

- **最小权限原则**：应用服务使用专用用户
- **网络安全**：防火墙限制访问端口
- **定期更新**：及时更新应用服务器和依赖

### 3. 备份策略

- **配置备份**：定期备份服务器配置
- **应用备份**：保留多个版本的 WAR 包
- **数据备份**：定期备份数据库和 Redis

---

## 📚 相关文档

- [Tomcat 官方文档](https://tomcat.apache.org/tomcat-9.0-doc/)
- [Spring Boot WAR 部署](https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/#howto-create-a-deployable-war-file)
- [WildFly 部署指南](https://docs.wildfly.org/wildfly-admin-guide.html)
- [WebSphere 部署](https://www.ibm.com/docs/en/was/9.0.5)

---

**最后更新**: 2026-03-31  
**维护者**: Development Team
