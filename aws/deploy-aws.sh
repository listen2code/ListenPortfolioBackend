#!/bin/bash
# ===================================================================
# AWS 监控栈部署脚本
# ===================================================================
# 说明：在 AWS 上部署完整的监控栈
# 支持：ECS Fargate, EKS, CloudFormation
# 前置：AWS CLI 已配置，Docker 已安装
# ===================================================================

set -e

# 配置变量
AWS_REGION=${AWS_REGION:-"us-east-1"}
ENVIRONMENT=${ENVIRONMENT:-"production"}
ECR_REPOSITORY=${ECR_REPOSITORY:-"portfolio"}
TAG=${TAG:-"latest"}

echo "🚀 Starting AWS Deployment for Portfolio Monitoring"
echo "================================================="
echo "Region: $AWS_REGION"
echo "Environment: $ENVIRONMENT"
echo "Repository: $ECR_REPOSITORY:$TAG"
echo ""

# ===================================================================
# 步骤 1: 检查前置条件
# ===================================================================
check_prerequisites() {
    echo "🔍 Checking prerequisites..."
    
    # 检查 AWS CLI
    if ! command -v aws &> /dev/null; then
        echo "❌ AWS CLI not found. Please install AWS CLI."
        exit 1
    fi
    
    # 检查 Docker
    if ! command -v docker &> /dev/null; then
        echo "❌ Docker not found. Please install Docker."
        exit 1
    fi
    
    # 检查 AWS 凭证
    if ! aws sts get-caller-identity &> /dev/null; then
        echo "❌ AWS credentials not configured. Please run 'aws configure'."
        exit 1
    fi
    
    echo "✅ Prerequisites check passed"
}

# ===================================================================
# 步骤 2: 构建 Docker 镜像
# ===================================================================
build_docker_image() {
    echo "🔨 Building Docker image..."
    
    # 构建镜像
    docker build -t $ECR_REPOSITORY:$TAG .
    
    echo "✅ Docker image built successfully"
}

# ===================================================================
# 步骤 3: 推送到 ECR
# ===================================================================
push_to_ecr() {
    echo "📦 Pushing to ECR..."
    
    # 获取 ECR 登录令牌
    aws ecr get-login-password --region $AWS_REGION | docker login --username AWS --password-stdin $(aws sts get-caller-identity --query Account --output text).dkr.ecr.$AWS_REGION.amazonaws.com
    
    # 创建 ECR 仓库（如果不存在）
    aws ecr describe-repositories --repository-names $ECR_REPOSITORY --region $AWS_REGION || \
    aws ecr create-repository --repository-name $ECR_REPOSITORY --region $AWS_REGION
    
    # 标记镜像
    ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
    docker tag $ECR_REPOSITORY:$TAG $ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com/$ECR_REPOSITORY:$TAG
    
    # 推送镜像
    docker push $ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com/$ECR_REPOSITORY:$TAG
    
    echo "✅ Image pushed to ECR successfully"
}

# ===================================================================
# 步骤 4: 部署到 ECS (选项 1)
# ===================================================================
deploy_to_ecs() {
    echo "🚀 Deploying to ECS Fargate..."
    
    # 使用 CloudFormation 部署
    aws cloudformation deploy \
        --template-file aws/cloudformation/monitoring-stack.yaml \
        --stack-name $ENVIRONMENT-portfolio-monitoring \
        --parameter-overrides \
            Environment=$ENVIRONMENT \
            DatabasePassword=$DATABASE_PASSWORD \
            VpcId=$VPC_ID \
            SubnetIds=$SUBNET_IDS \
        --capabilities CAPABILITY_IAM \
        --region $AWS_REGION
    
    echo "✅ ECS deployment completed"
}

# ===================================================================
# 步骤 5: 部署到 EKS (选项 2)
# ===================================================================
deploy_to_eks() {
    echo "🚀 Deploying to EKS..."
    
    # 更新 kubeconfig
    aws eks update-kubeconfig --name $ENVIRONMENT-portfolio-cluster --region $AWS_REGION
    
    # 应用 Kubernetes 配置
    kubectl apply -f aws/k8s/
    
    # 等待部署完成
    kubectl rollout status deployment/portfolio-app -n monitoring
    kubectl rollout status deployment/prometheus -n monitoring
    kubectl rollout status deployment/grafana -n monitoring
    
    echo "✅ EKS deployment completed"
}

# ===================================================================
# 步骤 6: 配置监控
# ===================================================================
configure_monitoring() {
    echo "📊 Configuring monitoring..."
    
    # 等待服务启动
    echo "Waiting for services to be ready..."
    sleep 60
    
    # 获取服务 URL
    if [ "$DEPLOYMENT_TYPE" = "ecs" ]; then
        ALB_DNS=$(aws cloudformation describe-stacks \
            --stack-name $ENVIRONMENT-portfolio-monitoring \
            --query 'Stacks[0].Outputs[?OutputKey==`LoadBalancerDNS`].OutputValue' \
            --output text)
        echo "🌐 Application URL: http://$ALB_DNS"
        echo "📊 Grafana URL: http://$ALB_DNS:3000"
    else
        echo "🌐 Application URL: http://$(kubectl get service portfolio-service -n monitoring -o jsonpath='{.status.loadBalancer.ingress[0].hostname}')"
        echo "📊 Grafana URL: http://$(kubectl get service grafana-service -n monitoring -o jsonpath='{.status.loadBalancer.ingress[0].hostname}'):3000"
    fi
    
    echo "🔑 Grafana Login: admin / $DATABASE_PASSWORD"
}

# ===================================================================
# 主函数
# ===================================================================
main() {
    check_prerequisites
    build_docker_image
    push_to_ecr
    
    case $DEPLOYMENT_TYPE in
        "ecs")
            deploy_to_ecs
            ;;
        "eks")
            deploy_to_eks
            ;;
        *)
            echo "❌ Invalid DEPLOYMENT_TYPE. Use 'ecs' or 'eks'"
            exit 1
            ;;
    esac
    
    configure_monitoring
    
    echo ""
    echo "🎉 AWS deployment completed successfully!"
    echo "================================================="
}

# 执行主函数
main "$@"
