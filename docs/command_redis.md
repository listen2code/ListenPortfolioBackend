# docker安装redis
docker pull redis:latest

# 启动 Redis 和相关服务
docker-compose --profile local up -d redis

# 测试 Redis 连接
docker exec -it redis-local redis-cli ping

# 查看数据
docker exec -it redis-local redis-cli keys "*"