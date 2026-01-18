
### ssh login
ssh -i xxx.pem [ec2 username]@[ec2 公有 IPv4 地址]
ssh -i listen.pem ec2-user@18.181.198.209

### upload files
scp -i tool/listen.pem -r src pom.xml Dockerfile docker-compose.yml ec2-user@18.181.198.209:/home/ec2-user/gallery/