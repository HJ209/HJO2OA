# tenant API

## 接口分组

| 分组 | 代表接口 | 说明 |
|------|----------|------|
| 租户档案 | `GET /api/v1/infra/tenants`、`POST /api/v1/infra/tenants`、`PUT /api/v1/infra/tenants/{tenantId}` | 维护租户基本信息、默认语言/时区、套餐与隔离模式。 |
| 初始化编排 | `POST /api/v1/infra/tenants/{tenantId}/initialize`、`GET /api/v1/infra/tenants/{tenantId}/initializations` | 发起初始化、查看步骤状态和失败原因。 |
| 初始化治理 | `POST /api/v1/infra/tenants/{tenantId}/initializations/{batchId}/retry` | 对失败初始化批次进行人工重试。 |
| 配额治理 | `GET /api/v1/infra/tenants/{tenantId}/quotas`、`POST /api/v1/infra/tenants/{tenantId}/quota-adjustments` | 查询配额、调整阈值与上限。 |
| 配额回滚 | `POST /api/v1/infra/tenants/{tenantId}/quota-adjustments/{adjustmentId}/rollback` | 将最近一次配额调整恢复到历史快照。 |
| 状态管控 | `POST /api/v1/infra/tenants/{tenantId}/disable` | 停用租户，阻断后续业务准入。 |

## 关键查询/写入接口

| 方法 | 路径 | 用途 | 幂等/并发约束 | 权限与审计 |
|------|------|------|---------------|------------|
| `GET` | `/api/v1/infra/tenants` | 查询租户清单 | 只读接口，无需幂等键 | 需要 `infra.tenant.read`，默认按可见租户范围过滤。 |
| `POST` | `/api/v1/infra/tenants` | 创建租户档案与基础配额 | 必须传 `X-Idempotency-Key`；`tenantCode` 全局唯一 | 需要 `infra.tenant.manage`，记录创建快照。 |
| `PUT` | `/api/v1/infra/tenants/{tenantId}` | 更新租户名称、默认语言/时区、套餐等 | 必须传 `expectedVersion`，避免并发覆盖 | 需要 `infra.tenant.manage`，审计差异字段。 |
| `POST` | `/api/v1/infra/tenants/{tenantId}/initialize` | 发起租户初始化批次 | 必须传 `X-Idempotency-Key`；同一租户存在进行中批次时拒绝新建 | 需要 `infra.tenant.operate`，要求 `confirmToken` 与 `operationReason`。 |
| `GET` | `/api/v1/infra/tenants/{tenantId}/initializations` | 查看初始化步骤、耗时、失败原因 | 只读接口，无需幂等键 | 需要 `infra.tenant.read`，支持步骤级过滤。 |
| `POST` | `/api/v1/infra/tenants/{tenantId}/initializations/{batchId}/retry` | 重试失败的初始化批次或步骤 | 必须传 `X-Idempotency-Key`；仅失败批次可重试 | 需要 `infra.tenant.operate`，必须二次确认并记审计。 |
| `GET` | `/api/v1/infra/tenants/{tenantId}/quotas` | 查询配额上限、用量、预警状态 | 只读接口，无需幂等键 | 需要 `infra.tenant.read`，展示最近预警时间。 |
| `POST` | `/api/v1/infra/tenants/{tenantId}/quota-adjustments` | 调整配额上限或阈值 | 必须传 `X-Idempotency-Key`；按租户 + 变更单号去重 | 需要 `infra.tenant.govern`，要求二次确认、原因和生效时间。 |
| `POST` | `/api/v1/infra/tenants/{tenantId}/quota-adjustments/{adjustmentId}/rollback` | 回滚一次配额调整 | 必须传 `X-Idempotency-Key`；仅允许回滚处于窗口期的调整 | 需要 `infra.tenant.govern`，记录回滚前后快照。 |
| `POST` | `/api/v1/infra/tenants/{tenantId}/disable` | 停用租户 | 自然幂等；已停用租户再次调用仅返回当前状态 | 需要 `infra.tenant.govern`，必须展示影响摘要并写审计。 |

## 分页与筛选

| 接口 | 分页规则 | 推荐筛选项 |
|------|----------|------------|
| `GET /tenants` | 使用 `page`、`size`、`sort=-createdAt` | `filter[tenantCode]`、`filter[name]`、`filter[status]`、`filter[initialized]`、`filter[isolationMode]`、`filter[packageCode]`、`filter[createdAt]gte/lte` |
| `GET /tenants/{tenantId}/initializations` | 使用 `page`、`size`、`sort=-startedAt` | `filter[status]`、`filter[stepCode]`、`filter[startedAt]gte/lte` |
| `GET /tenants/{tenantId}/quotas` | 不分页 | `filter[quotaType]`、`filter[warningOnly]` |

## 幂等

| 场景 | 规则 |
|------|------|
| 创建租户 | `tenantCode` 唯一 + `X-Idempotency-Key` 去重。 |
| 发起初始化 | 同一租户在“进行中”期间只能存在一个初始化批次；重复请求返回既有批次。 |
| 初始化重试 | 同一 `batchId + idempotencyKey` 只允许生成一次重试操作。 |
| 配额调整/回滚 | 以调整单号或回滚单号去重，保证治理动作不会被重复执行。 |
| 停用租户 | 状态写入天然幂等，重复停用不重复发布副作用。 |

## 权限与审计

| 维度 | 要求 |
|------|------|
| 权限分层 | `infra.tenant.read` 仅查询；`infra.tenant.manage` 可维护档案；`infra.tenant.operate` 可初始化；`infra.tenant.govern` 可调配额和停用。 |
| 审计字段 | 必须记录 `operatorAccountId`、`operatorPersonId`、`tenantId`、`traceId`、操作原因、影响面摘要、前后差异。 |
| 隔离要求 | 非平台管理员只能查看授权范围内租户；租户内用户不得访问全局治理接口。 |
| 脱敏要求 | 初始化日志中的密钥引用、默认密码或敏感模板参数必须脱敏。 |

## 治理动作约束

- `initialize`、`retry`、`quota-adjustments`、`disable` 都属于治理动作，必须在前端先展示影响清单，再提交 `confirmToken`。
- 回滚仅针对配额调整或初始化编排元数据快照，不负责自动清理已经在业务模块生成的数据。
- tenant 不提供组织树、角色授权、账号开通等接口，这些能力必须回到 `01-组织与权限`。

## 错误处理

| 场景 | HTTP | 错误码建议 | 说明 |
|------|------|------------|------|
| 租户编码重复 | `409` | `TENANT_CODE_DUPLICATE` | `tenantCode` 已存在。 |
| 隔离模式不支持 | `422` | `TENANT_ISOLATION_MODE_UNSUPPORTED` | 一期不支持请求的隔离方式。 |
| 已有初始化进行中 | `409` | `TENANT_INITIALIZATION_IN_PROGRESS` | 同一租户存在未完成批次。 |
| 初始化失败步骤不可直接跳过 | `422` | `TENANT_INITIALIZATION_RETRY_REQUIRED` | 需通过重试入口恢复。 |
| 配额规则非法 | `422` | `TENANT_QUOTA_RULE_INVALID` | 上限或阈值不符合规则。 |
| 配额回滚窗口已过 | `409` | `TENANT_QUOTA_ROLLBACK_EXPIRED` | 超出允许回滚时间。 |
| 租户已停用 | `409` | `TENANT_DISABLED` | 不允许再次初始化或变更运行配置。 |
