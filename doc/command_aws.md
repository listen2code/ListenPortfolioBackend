
### ssh login
```
ssh -i xxx.pem [ec2 username]@[ec2 公有 IPv4 地址]
ssh -i tool/listen.pem ec2-user@18.181.198.209
```

### upload files
```
scp -i tool/listen.pem -r src pom.xml Dockerfile docker-compose.yml ec2-user@18.181.198.209:/home/ec2-user/portfolio/
```
### check mysql in docker
```
# 在 EC2 上执行
# 注意：容器名称前缀通常取决于目录名（小写），如果目录是 portfolio，容器名可能是 portfolio-db-1
docker exec -it portfolio-db-1 mysql -u root -pLs-88888888 portfolio

# 或者使用 docker-compose (如果安装了)
# docker-compose exec db mysql -u root -pLs-88888888 portfolio

# 进入 MySQL 命令行后：
show tables;
select * from user_info;
```
