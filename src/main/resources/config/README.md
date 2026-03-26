# 📁 Config 目录配置说明

本目录包含 Spring Boot 应用的多环境配置文件，用于不同部署环境的配置管理。

## 📋 配置文件概览

| 文件名 | 环境类型 | 主要用途 | 关键特性 |
|--------|----------|----------|----------|
| `application-dev.yml` | 开发环境 | 本地开发调试 | SQL显示、彩色日志 |
| `application-test.yml` | 测试环境 | CI/CD自动化测试 | 性能优化、精简日志 |
| `application-staging.yml` | 预生产环境 | 发布前验证 | 生产模拟、性能测试 |
| `application-prod.yml` | 生产环境 | 正式部署运行 | 安全、稳定、高性能 |

## 🚀 环境切换方式

### 1. 命令行方式
```bash
# 开发环境
java -jar portfolio.jar --spring.profiles.active=dev

# 测试环境
java -jar portfolio.jar --spring.profiles.active=test

# 预生产环境
java -jar portfolio.jar --spring.profiles.active=staging

# 生产环境
java -jar portfolio.jar --spring.profiles.active=prod
```

### 2. 环境变量方式
```bash
# Linux/Mac
export SPRING_PROFILES_ACTIVE=prod

# Windows
set SPRING_PROFILES_ACTIVE=prod

# Docker
docker run -e SPRING_PROFILES_ACTIVE=prod portfolio
```

### 3. application.properties 中指定
```properties
spring.profiles.active=prod
```

### 4. Maven 命令方式
```bash
# 测试
mvn test -Dspring.profiles.active=test

# 打包
mvn package -Dspring.profiles.active=prod
```

## 🔧 各环境详细配置

### 📱 application-dev.yml (开发环境)

**适用场景**: 本地开发、功能调试、接口测试

**关键配置**:
```yaml
spring:
  jpa:
    show-sql: true          # 显示SQL，便于调试
  output:
    ansi:
      enabled: always      # 启用彩色日志，提升可读性

logging:
  level:
    root: INFO
    org.springframework.web: INFO
```

**特点**:
- ✅ **SQL调试**: 显示所有数据库操作SQL
- ✅ **彩色日志**: 控制台输出彩色日志，便于区分日志级别
- ✅ **详细日志**: 包含Spring Web相关日志信息

**使用建议**:
- 日常开发编码时使用
- 接口调试和问题排查时使用
- 不建议在性能测试时使用

---

### 🧪 application-test.yml (测试环境)

**适用场景**: CI/CD流水线、自动化测试、单元测试

**关键配置**:
```yaml
spring:
  jpa:
    show-sql: false         # 关闭SQL显示，减少日志噪音

logging:
  level:
    root: INFO
```

**特点**:
- ✅ **性能优化**: 关闭SQL显示，减少测试执行时间
- ✅ **日志精简**: 只保留INFO级别，减少输出噪音
- ✅ **CI友好**: 适合自动化测试环境

**使用建议**:
- 持续集成流水线中使用
- 自动化测试执行时使用
- 性能测试时使用

---

### 🎭 application-staging.yml (预生产环境)

**适用场景**: 发布前验证、灰度测试、性能验证

**关键配置**:
```yaml
spring:
  jpa:
    show-sql: false         # 关闭SQL显示

logging:
  level:
    root: INFO
```

**特点**:
- ✅ **生产模拟**: 配置参数接近生产环境
- ✅ **性能测试**: 关闭调试功能，模拟真实性能
- ✅ **灰度验证**: 用于正式发布前的最后验证

**使用建议**:
- 生产发布前的预验证
- 灰度发布测试
- 性能压力测试

---

### 🏭 application-prod.yml (生产环境)

**适用场景**: 正式生产部署、线上服务运行

**关键配置**:
```yaml
spring:
  jpa:
    show-sql: false         # 关闭SQL显示，保护敏感信息

logging:
  level:
    root: INFO
```

**特点**:
- ✅ **安全第一**: 关闭SQL显示，避免敏感信息泄露
- ✅ **性能优先**: 最小化日志输出，提升运行性能
- ✅ **稳定可靠**: 配置谨慎，避免随意修改

**使用建议**:
- 生产环境部署使用
- 线上服务运行使用
- 配置修改需要谨慎评估

## 📊 配置差异对比

| 配置项 | dev | test | staging | prod |
|--------|-----|------|---------|------|
| **SQL显示** | ✅ `show-sql: true` | ❌ `show-sql: false` | ❌ `show-sql: false` | ❌ `show-sql: false` |
| **ANSI彩色** | ✅ `ansi.enabled: always` | ❌ 默认 | ❌ 默认 | ❌ 默认 |
| **日志级别** | `root: INFO` | `root: INFO` | `root: INFO` | `root: INFO` |
| **监控端点** | `health,info,prometheus` | 同左 | 同左 | 同左 |
| **主要用途** | 本地开发 | 自动化测试 | 预生产验证 | 生产运行 |

## 🔄 配置加载优先级

Spring Boot 配置文件加载顺序（优先级从高到低）:

1. **命令行参数** - `--spring.profiles.active=prod`
2. **环境变量** - `SPRING_PROFILES_ACTIVE=prod`
3. **application-{profile}.yml** - `application-prod.yml`
4. **application.yml** - 主配置文件
5. **application.properties** - 默认配置文件

## 🛡️ 安全注意事项

### 1. 敏感信息保护
```yaml
# ❌ 错误：生产环境不要显示SQL
spring:
  jpa:
    show-sql: false  # 生产环境必须关闭

# ❌ 错误：生产环境不要输出详细日志
logging:
  level:
    root: WARN      # 生产环境建议使用WARN或ERROR
```

### 2. 配置文件权限
```bash
# 限制配置文件访问权限
chmod 600 application-prod.yml
```

### 3. 环境变量注入
```properties
# ✅ 推荐：使用环境变量注入敏感配置
spring.datasource.url=${DB_URL}
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}
```

## 🚀 最佳实践

### 1. 开发流程
```bash
# 1. 本地开发
mvn spring-boot:run -Dspring.profiles.active=dev

# 2. 提交测试
git push origin main

# 3. CI/CD测试 (自动使用test配置)

# 4. 预生产验证
java -jar portfolio.jar --spring.profiles.active=staging

# 5. 生产部署
java -jar portfolio.jar --spring.profiles.active=prod
```

### 2. 配置管理原则
- **环境隔离**: 每个环境独立配置
- **安全优先**: 生产环境关闭调试功能
- **性能考虑**: 测试和生产环境优化性能
- **可维护性**: 配置文件添加详细注释

### 3. 监控和日志
```yaml
# 所有环境都启用的监控端点
management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus
```

## 🔍 故障排查

### 1. 配置未生效
```bash
# 检查当前激活的配置
curl http://localhost:8080/actuator/env

# 查看配置属性
curl http://localhost:8080/actuator/configprops
```

### 2. SQL显示问题
```bash
# 检查是否使用了正确的环境配置
grep -r "show-sql" src/main/resources/config/
```

### 3. 日志级别问题
```bash
# 查看当前日志配置
curl http://localhost:8080/actuator/loggers
```

## 📝 维护指南

### 1. 添加新配置
```yaml
# 1. 在 application.properties 中添加基础配置
spring.datasource.url=${DB_URL:default-url}

# 2. 在环境配置中覆盖特定配置
# application-prod.yml
spring:
  datasource:
    url: ${DB_URL:prod-default-url}
```

### 2. 配置验证
```bash
# 启动时验证配置
java -jar portfolio.jar --spring.profiles.active=prod --debug

# 检查配置加载情况
java -jar portfolio.jar --spring.profiles.active=prod --logging.level.org.springframework.boot=DEBUG
```

### 3. 版本控制
```gitignore
# .gitignore
# 敏感配置文件不要提交到版本控制
application-local.yml
application-secret.properties
```

---

## 📞 技术支持

如果在使用配置文件过程中遇到问题，可以：

1. **查看日志**: 检查应用启动日志中的配置加载信息
2. **使用Actuator**: 访问 `/actuator/env` 查看当前配置
3. **参考文档**: 查看 [Spring Boot Configuration](https://docs.spring.io/spring-boot/docs/current/reference/html/application-properties.html)
4. **团队协作**: 与运维团队确认环境配置要求

---

*最后更新: 2026-03-26*  
*维护者: Development Team*
