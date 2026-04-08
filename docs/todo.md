# 📋 TODO 清单

## 项目现状判断

### 已确认的当前状态

- 分层结构、JWT、Redis 黑名单、邮件服务、监控、日志、Flyway、Docker 基础能力都已具备
- 这个项目已经是一个**可运行、可联调、可继续维护**的支撑型后端
- 当前最需要补的不是“再加很多功能”，而是**契约收口、可靠性、文档可信度与自动化**

### 当前主要风险

- Flutter / Backend 契约还有最后一段收尾工作
- OSIV 关闭还没正式启用，README 不能把它写成已完成
- Refresh Token 还不能主动吊销
- CI 仍为空白
- 部分安全 / 设计文档仍是 spec，不是已实现能力

## Now

### 1. Flutter 前后端 API 对齐

**现状**：大部分 Mock 数据已修正，但仍有 DTO 字段差异需要最终收口。  
**目标**：Flutter model、后端 DTO、mock 数据三方最终一致。  
**为什么现在做**：这是“真实 App 优先”下最关键的一步。  
**验收标准**：Flutter dev 环境调用后端 API 时，无解析异常、无字段歧义、无双标准。

**剩余差异**：

| 项目 | Flutter Mock / 现状 | 后端实际 | 处理建议 |
|------|---------------------|----------|----------|
| `ProjectDto.businessId` | Flutter 端未完全适配 | DTO 已有 `businessId` | Flutter model 增加字段或调整映射 |
| `StatDto.id` | 仍带历史语义混淆 | Long `id` + String `businessId` | 明确前端最终消费哪一个字段 |
| Flutter dev API URL | 仍需确认联调配置 | 后端已提供 `/v1/*` 路径 | 完成 dev 环境最终指向 |

### 2. Refresh Token 持久化与吊销

**现状**：刷新 Token 生成后不存储，无法主动吊销。  
**目标**：将 Refresh Token 持久化到 Redis 或 DB，支持主动吊销。  
**为什么现在做**：这是安全闭环里最容易被追问的缺口。  
**验收标准**：修改密码或注销后，旧 refresh token 失效。

### 3. delete-account 软删除修复

**现状**：`UserService.deleteAccount()` 仍执行硬删除，而 `UserEntity.deleted` 暗示了软删除设计方向。  
**目标**：改为软删除，并避免种子用户数据被误删。  
**为什么现在做**：当前行为与数据保留预期冲突。  
**验收标准**：userId=1 不会被硬删除，已删除用户默认不再出现在查询结果中。

### 4. OSIV 关闭验证与落地

**现状**：`spring.jpa.open-in-view=false` 仍处于注释状态。  
**目标**：正式关闭 OSIV，把懒加载问题收敛到 Service 层解决。  
**为什么现在做**：这直接关系到分层纪律是否真实成立。  
**验收标准**：应用正常启动，测试通过，无 `LazyInitializationException`。

### 5. GitHub Actions CI

**现状**：`.github/workflows/` 为空。  
**目标**：建立最小可信 CI：测试 + JaCoCo + SpotBugs。  
**为什么现在做**：当前代码质量只能本地证明，不能自动证明。  
**验收标准**：Push / PR 自动触发校验；README 可挂 badge。

## Next

### 1. 测试补强

**目标**：补上目前最有代表性的缺口测试。  
**建议补充**：

```text
src/test/java/com/listen/portfolio/
├── api/v1/auth/
│   └── AuthControllerRefreshTest.java
├── service/
│   ├── AuthServiceTest.java
│   └── ProjectServiceTest.java
└── repository/
    └── ProjectRepositoryTest.java
```

**验收标准**：关键链路覆盖更完整，JaCoCo 报告不再只靠现有样本测试支撑。

### 2. V2 迁移脚本

**现状**：V1 迁移适合新环境初始化，但已部署环境的数据更新仍需额外脚本。  
**目标**：为真实简历数据更新提供 `V2__Update_seed_data.sql`。  
**验收标准**：已部署环境执行 V2 后与当前真实简历内容一致。

### 3. 限流算法升级

**现状**：当前为 Redis INCR 固定窗口。  
**目标**：如确有必要，再升级为滑动窗口或令牌桶。  
**验收标准**：只有在真实需要更高精度时再推进，不为“概念更高级”而升级。

## Later

### 1. 生产安全与运维增强

- HTTPS / TLS
- 更完整的生产部署方案
- 更强的监控与安全指标

### 2. 安全能力扩展

- `PasswordPolicyValidator`
- 服务端文案 i18n
- `DataMaskingUtil`
- `SecurityMetrics`

### 3. security_features.md 设计落地

以下能力目前仍主要属于设计稿：

- `PasswordPolicyValidator`
- `DataMaskingUtil`
- `SecurityAuditService`
- `SecurityMetrics`
- `AnomalyDetectionService`
- `ThreatDetectionService`
- `AutomatedSecurityResponse`

这些内容只有在与项目定位匹配、且不挤压主线任务时才推进。

## Archive / Backup

### 已确认完成的基础能力

- 分层架构
- JWT 认证
- Token 黑名单
- BCrypt
- Spring Security 无状态配置
- 全局异常处理
- OpenAPI / Swagger UI
- Flyway 启动迁移
- Prometheus + Grafana
- 结构化 JSON 日志
- 请求日志
- JaCoCo / SpotBugs
- Docker Compose 完整栈
- 敏感配置外部化
- 邮件服务与密码重置能力
- AOP 限流
- `ErrorCode + BusinessException`

### 已降级或待重新判断的条目

- 不再保留“9/10”式自评分数表
- `LoginResponse.token` 是否改名为 `accessToken`，暂不作为当前主线任务
- 不再把“测试覆盖率 > 80%”直接写成已证明事实，除非有稳定报告支撑

## 简要检查清单

### 安全

- [x] JWT Token 认证
- [x] BCrypt 密码哈希
- [x] Token 黑名单（Redis）
- [x] 敏感配置环境变量化
- [ ] Refresh Token 持久化与吊销
- [x] 认证接口限流
- [ ] 生产环境 HTTPS

### Flutter 对接

- [x] Flutter mock 数据补全 `messageId`
- [x] Flutter projects.json 技术栈修正为 Spring Boot
- [x] Flutter aboutMe.json 替换为真实简历数据
- [x] Flutter login / refresh userId 修正为数字类型
- [x] V1 DB 种子数据替换为真实简历内容
- [x] 创建 `docs/api_reference.md`
- [ ] Flutter 端适配 `ProjectDto.businessId`
- [ ] Flutter 端适配 `StatDto.id` vs `businessId` 映射
- [ ] Flutter dev 环境配置指向后端 API URL
- [ ] V2 迁移脚本

### 工程化

- [x] Docker Compose 完整栈
- [x] Prometheus + Grafana
- [x] 健康检查 / 探针
- [x] 结构化 JSON 日志
- [ ] GitHub Actions CI
- [ ] OSIV 正式关闭

---

📅 **最后更新**: 2026-04-08