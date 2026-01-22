
### ssh login
ssh -i xxx.pem [ec2 username]@[ec2 公有 IPv4 地址]
ssh -i tool/listen.pem ec2-user@18.181.198.209

### upload files
scp -i tool/listen.pem -r src pom.xml Dockerfile docker-compose.yml ec2-user@18.181.198.209:/home/ec2-user/gallery/

### check mysql in docker
```
# 在 EC2 上执行
docker exec -it gallery-db-1 mysql -u root -pLs-88888888 gallery

# 进入 MySQL 命令行后：
show tables;
select * from user_info;
```

### clean when deploy
```
docker compose down -v
```