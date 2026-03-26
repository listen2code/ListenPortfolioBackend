# 🌩 AWS 部署指南

## 📋 配置文件通用性分析

### ✅ **完全通用的文件**

| 文件 | 通用性 | 说明 |
|------|--------|------|
| **monitoring/prometheus.yml** | ✅ 100% 通用 | Prometheus 配置，AWS 上可直接使用 |
| **monitoring/grafana/provisioning/** | ✅ 100% 通用 | Grafana 自动配置，AWS 上可直接使用 |
| **monitoring/grafana/dashboards/** | ✅ 100% 通用 | 仪表板定义，AWS 上可直接使用 |
| **src/main/resources/application.properties** | ✅ 100% 通用 | Spring Boot 配置，AWS 完全兼容 |
| **Dockerfile** | ✅ 100% 通用 | 标准 Docker 镜像，AWS ECS/EKS 完全支持 |

### ⚠️ **需要调整的文件**

| 文件 | 调整内容 | AWS 替代方案 |
|------|----------|-------------|
| **docker-compose.yml** | 端口映射、网络配置 | ECS Task Definition / K8s Deployment |
| **start-monitoring-en.ps1** | PowerShell、本地 Docker | AWS CLI / CloudFormation |
| **start-monitoring-local.ps1** | 本地监控 | AWS CloudWatch / X-Ray |

## 🚀 AWS 部署方案

### **方案 1: AWS ECS Fargate (推荐)**

**适用场景**: 生产环境、无服务器、自动扩缩容

**优势**:
- 🚀 无服务器管理
- 📈 自动扩缩容
- 💰 按需付费
- 🔒 高安全性

**部署步骤**:
```bash
# 1. 设置环境变量
export AWS_REGION="us-east-1"
export ENVIRONMENT="production"
export DATABASE_PASSWORD="your_secure_password"
export VPC_ID="vpc-xxxxxxxx"
export SUBNET_IDS="subnet-xxx,subnet-yyy"

# 2. 执行部署
export DEPLOYMENT_TYPE="ecs"
./aws/deploy-aws.sh
```

### **方案 2: AWS EKS (Kubernetes)**

**适用场景**: 复杂微服务、需要 K8s 生态

**优势**:
- 🔧 容器编排
- 📦 Helm 支持
- 🔄 滚动更新
- 🏷️ 服务发现

**部署步骤**:
```bash
# 1. 创建 EKS 集群
aws eks create-cluster --name portfolio-cluster --region us-east-1

# 2. 部署应用
export DEPLOYMENT_TYPE="eks"
./aws/deploy-aws.sh
```

### **方案 3: AWS CloudFormation**

**适用场景**: 基础设施即代码、多环境管理

**优势**:
- 📝 基础设施即代码
- 🔄 版本控制
- 🌍 多环境支持
- 📋 资源管理

**部署步骤**:
```bash
aws cloudformation deploy \
    --template-file aws/cloudformation/monitoring-stack.yaml \
    --stack-name production-portfolio-monitoring \
    --parameter-overrides \
        Environment=production \
        DatabasePassword=your_password \
        VpcId=vpc-xxx \
        SubnetIds=subnet-xxx,subnet-yyy
```

## 🔧 AWS 特定配置

### **1. 环境变量调整**

**生产环境配置**:
```properties
# AWS RDS 连接
spring.datasource.url=jdbc:mysql://portfolio-db.cluster-xxx.us-east-1.rds.amazonaws.com:3306/portfolio
spring.datasource.username=portfolio_user
spring.datasource.password=${DATABASE_PASSWORD}

# AWS CloudWatch 日志
logging.level.root=WARN
logging.pattern=%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n

# AWS X-Ray 追踪
spring.sleuth.enabled=true
spring.sleuth.zipkin.enabled=false
spring.sleuth.aws.xray.enabled=true
```

### **2. 安全配置**

**IAM 角色**:
- ECS Task Execution Role
- RDS 访问权限
- CloudWatch 日志权限
- X-Ray 追踪权限

**安全组**:
- ALB 安全组（HTTP 80/443）
- 应用安全组（仅 ALB 访问）
- 数据库安全组（仅应用访问）
- 监控安全组（限制访问范围）

### **3. 监控集成**

**AWS CloudWatch**:
- 应用日志收集
- 系统指标监控
- 自定义指标
- 告警配置

**AWS X-Ray**:
- 分布式追踪
- 性能分析
- 错误追踪
- 服务地图

## 📊 监控配置调整

### **Prometheus AWS 配置**

```yaml
# monitoring/prometheus-aws.yml
global:
  scrape_interval: 15s
  evaluation_interval: 15s

scrape_configs:
  - job_name: 'portfolio-app-aws'
    ec2_sd_configs:
      - region: us-east-1
        port: 8080
        filters:
          - name: 'tag:Environment'
            values: ['production']
          - name: 'tag:Service'
            values: ['portfolio']
    metrics_path: '/actuator/prometheus'
    scrape_interval: 10s
    scrape_timeout: 5s
```

### **Grafana AWS 数据源**

```json
{
  "name": "CloudWatch",
  "type": "cloudwatch",
  "access": "proxy",
  "url": "http://localhost:3000",
  "jsonData": {
    "authType": "default",
    "defaultRegion": "us-east-1"
  }
}
```

## 🎯 部署最佳实践

### **1. 环境分离**

- **开发环境**: 单实例、小规格
- **测试环境**: 多实例、中等规格
- **生产环境**: 高可用、自动扩缩容

### **2. 安全配置**

- 使用 AWS Secrets Manager 管理密码
- 配置 VPC 网络隔离
- 启用 RDS 加密存储
- 配置 ALB SSL 证书

### **3. 监控告警**

```yaml
# CloudWatch 告警配置
- AlertName: HighCPUUtilization
  MetricName: CPUUtilization
  Threshold: 80
  ComparisonOperator: GreaterThanThreshold
  EvaluationPeriods: 2

- AlertName: HighMemoryUsage
  MetricName: MemoryUtilization
  Threshold: 85
  ComparisonOperator: GreaterThanThreshold
  EvaluationPeriods: 2
```

### **4. 成本优化**

- 使用 Fargate Spot 实例
- 配置自动扩缩容策略
- 优化 EBS 存储类型
- 使用 CloudWatch 日志保留策略

## 🔄 迁移步骤

### **从本地到 AWS**

1. **准备阶段**
   - 创建 AWS 账户和 VPC
   - 配置 AWS CLI 和凭证
   - 创建 ECR 仓库

2. **构建阶段**
   - 构建 Docker 镜像
   - 推送到 ECR
   - 创建 CloudFormation 模板

3. **部署阶段**
   - 部署基础设施
   - 部署应用服务
   - 配置监控和告警

4. **验证阶段**
   - 测试应用功能
   - 验证监控指标
   - 配置域名和 SSL

## 📚 相关资源

- **AWS ECS 文档**: https://docs.aws.amazon.com/ecs/
- **AWS EKS 文档**: https://docs.aws.amazon.com/eks/
- **CloudFormation 文档**: https://docs.aws.amazon.com/cloudformation/
- **AWS 监控最佳实践**: https://docs.aws.amazon.com/whitepapers/latest/monitoring-applications/

---

*最后更新: 2026-03-26*  
*维护者: DevOps Team*
