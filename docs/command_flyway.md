# 查看迁移状态
./mvnw flyway:info

# 手动执行迁移
./mvnw flyway:migrate

# 验证迁移
./mvnw flyway:validate

# 启动应用（自动执行迁移）
./mvnw spring-boot:run -DskipTests

# 删除数据库
./mvnw flyway:clean