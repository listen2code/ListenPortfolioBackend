# Docker 常用命令参考

## 📋 目录
- [容器管理](#容器管理)
- [镜像管理](#镜像管理)
- [网络管理](#网络管理)
- [数据卷管理](#数据卷管理)
- [Docker Compose](#docker-compose)
- [监控与调试](#监控与调试)
- [清理命令](#清理命令)
- [Portfolio 项目专用](#portfolio-项目专用)

---

## 🐳 容器管理

### 基础操作
```bash
# 查看运行中的容器
docker ps

# 查看所有容器（包括停止的）
docker ps -a

# 查看容器详细信息
docker inspect <container_name_or_id>

# 查看容器日志
docker logs <container_name_or_id>
docker logs -f <container_name_or_id>  # 实时日志
docker logs --tail 100 <container_name_or_id>  # 最后100行

# 进入容器
docker exec -it <container_name_or_id> /bin/bash
docker exec -it <container_name_or_id> /bin/sh

# 停止容器
docker stop <container_name_or_id>

# 启动容器
docker start <container_name_or_id>

# 重启容器
docker restart <container_name_or_id>

# 删除容器
docker rm <container_name_or_id>
docker rm -f <container_name_or_id>  # 强制删除运行中的容器
```

### 容器资源监控
```bash
# 查看容器资源使用情况
docker stats

# 查看特定容器资源使用
docker stats <container_name_or_id>

# 查看容器进程
docker top <container_name_or_id>
```

---

## 📦 镜像管理

### 基础操作
```bash
# 查看本地镜像
docker images

# 拉取镜像
docker pull <image_name>:<tag>

# 构建镜像
docker build -t <image_name>:<tag> .
docker build -t <image_name>:<tag> -f <Dockerfile_path> <build_context>

# 删除镜像
docker rmi <image_name>:<tag>
docker rmi -f <image_name>:<tag>  # 强制删除

# 查看镜像历史
docker history <image_name>:<tag>

# 查看镜像详细信息
docker inspect <image_name>:<tag>
```

### 镜像优化
```bash
# 清理悬空镜像
docker image prune

# 清理所有未使用的镜像
docker image prune -a

# 查看镜像大小
docker images --format "table {{.Repository}}\t{{.Tag}}\t{{.Size}}"
```

---

## 🌐 网络管理

### 基础操作
```bash
# 查看网络
docker network ls

# 创建网络
docker network create <network_name>
docker network create --driver bridge <network_name>

# 查看网络详情
docker network inspect <network_name>

# 连接容器到网络
docker network connect <network_name> <container_name>

# 断开容器网络连接
docker network disconnect <network_name> <container_name>

# 删除网络
docker network rm <network_name>

# 清理未使用的网络
docker network prune
```

---

## 💾 数据卷管理

### 基础操作
```bash
# 查看数据卷
docker volume ls

# 创建数据卷
docker volume create <volume_name>

# 查看数据卷详情
docker volume inspect <volume_name>

# 删除数据卷
docker volume rm <volume_name>

# 清理未使用的数据卷
docker volume prune
```

---

## 🚀 Docker Compose

### 基础操作
```bash
# 启动服务
docker-compose up
docker-compose up -d  # 后台运行

# 停止服务
docker-compose down

# 停止并删除数据卷
docker-compose down -v

# 重新构建并启动
docker-compose up --build

# 查看服务状态
docker-compose ps

# 查看服务日志
docker-compose logs
docker-compose logs -f <service_name>
docker-compose logs --tail 50 <service_name>

# 进入服务容器
docker-compose exec <service_name> /bin/bash

# 执行命令
docker-compose exec <service_name> <command>

# 重启特定服务
docker-compose restart <service_name>

# 停止特定服务
docker-compose stop <service_name>

# 启动特定服务
docker-compose start <service_name>

# 删除服务容器
docker-compose rm <service_name>
```

### Profile 管理
```bash
# 使用特定 profile 启动
docker-compose --profile <profile_name> up -d

# 查看可用 profile
docker-compose config --services
```

### 配置验证
```bash
# 验证 docker-compose.yml 语法
docker-compose config

# 查看最终配置
docker-compose config --resolve-env-vars
```

---

## 🔍 监控与调试

### 容器调试
```bash
# 查看容器内进程
docker exec <container_name> ps aux

# 查看容器内文件
docker exec <container_name> ls -la <path>

# 复制文件到容器
docker cp <local_path> <container_name>:<container_path>

# 从容器复制文件
docker cp <container_name>:<container_path> <local_path>

# 查看容器端口映射
docker port <container_name>
```

### 性能监控
```bash
# 查看容器资源使用
docker stats --no-stream

# 查看磁盘使用
docker system df

# 查看事件
docker events
docker events --since 1h
```

---

## 🧹 清理命令

### 系统清理
```bash
# 查看磁盘使用情况
docker system df

# 全面清理（危险！会删除所有未使用的资源）
docker system prune -a --volumes

# 清理停止的容器
docker container prune

# 清理未使用的网络
docker network prune

# 清理悬空镜像
docker image prune

# 清理未使用的数据卷
docker volume prune
```

---

## 🎯 Portfolio 项目专用

### 快速启动
```bash
# 启动完整监控栈
docker-compose --profile local up -d

# 仅启动数据库
docker-compose up -d db

# 仅启动监控服务
docker-compose up -d prometheus grafana

# 启动特定环境
docker-compose --profile aws up -d
docker-compose --profile staging up -d
docker-compose --profile prod up -d
```

### 开发调试
```bash
# 查看应用日志
docker-compose logs -f app

# 查看数据库日志
docker-compose logs -f db

# 查看监控日志
docker-compose logs -f prometheus grafana

# 进入数据库容器
docker-compose exec db mysql -u root -pLs-88888888

# 进入应用容器
docker-compose exec app /bin/bash

# 重启应用
docker-compose restart app

# 重新构建应用
docker-compose up --build app
```

### 数据库管理
```bash
# 备份数据库
docker-compose exec db mysqldump -u root -pLs-88888888 portfolio > backup.sql

# 恢复数据库
docker-compose exec -T db mysql -u root -pLs-88888888 portfolio < backup.sql

# 连接数据库
docker-compose exec db mysql -u root -pLs-88888888 portfolio

# 查看数据库状态
docker-compose exec db mysqladmin ping -h localhost -u root -pLs-88888888
```

### 监控管理
```bash
# 重新加载 Prometheus 配置
curl -X POST http://localhost:9090/-/reload

# 查看 Prometheus 目标
curl http://localhost:9090/api/v1/targets

# 查看 Grafana 健康状态
curl http://localhost:3000/api/health

# 查看应用指标
curl http://localhost:8080/actuator/prometheus

# 查看应用健康状态
curl http://localhost:8080/actuator/health
```

### 故障排除
```bash
# 检查端口占用
netstat -ano | findstr :8080
netstat -ano | findstr :3306
netstat -ano | findstr :9090
netstat -ano | findstr :3000

# 强制停止所有容器
docker-compose down --remove-orphans

# 清理并重新启动
docker-compose down -v
docker system prune -f
docker-compose up --build -d

# 查看容器详细信息
docker-compose ps
docker inspect <container_name>
```

---

## 📝 快速参考

### 常用组合命令
```bash
# 完整重启流程
docker-compose down && docker-compose up --build -d

# 查看所有服务状态
docker-compose ps && docker-compose logs --tail=10

# 开发环境快速检查
curl http://localhost:8080/actuator/health && \
curl http://localhost:9090/-/healthy && \
curl http://localhost:3000/api/health
```

### 端口映射表
| 服务 | 端口 | 用途 |
|------|------|------|
| **Portfolio App** | 8080 | 应用 API |
| **MySQL Database** | 3307 | 数据库管理 |
| **Prometheus** | 9090 | 监控数据 |
| **Grafana** | 3000 | 可视化界面 |

### 环境变量文件
- `.env.local` - 本地开发环境
- `.env.staging` - 预发布环境  
- `.env.prod` - 生产环境
- `.env.aws` - AWS 部署环境

---

## 💡 最佳实践

1. **定期清理**: 使用 `docker system prune` 清理未使用资源
2. **监控资源**: 使用 `docker stats` 监控容器资源使用
3. **日志管理**: 使用 `--log-driver` 配置日志轮转
4. **网络隔离**: 为不同环境创建独立网络
5. **数据持久化**: 重要数据使用数据卷持久化
6. **安全配置**: 生产环境使用非 root 用户运行容器

---

## 🔗 相关文档

- [Docker 官方文档](https://docs.docker.com/)
- [Docker Compose 文档](https://docs.docker.com/compose/)
- [Portfolio 项目监控文档](./MONITORING.md)
- [Portfolio 项目部署指南](../README.md)
