# scheduler API

## 接口分组

| 分组 | 代表接口 | 说明 |
|------|----------|------|
| 任务定义 | `GET /api/v1/infra/scheduler/jobs`、`POST /api/v1/infra/scheduler/jobs`、`PUT /api/v1/infra/scheduler/jobs/{jobId}` | 管理任务定义、触发规则和处理器引用。 |
| 任务状态治理 | `POST /api/v1/infra/scheduler/jobs/{jobId}/enable`、`POST /api/v1/infra/scheduler/jobs/{jobId}/disable` | 启停任务，控制后续是否参与调度。 |
| 手工执行 | `POST /api/v1/infra/scheduler/jobs/{jobId}/trigger` | 立即执行任务，可覆盖本次执行参数。 |
| 执行记录 | `GET /api/v1/infra/scheduler/executions`、`GET /api/v1/infra/scheduler/executions/{executionId}` | 查询运行批次、错误明细、重试轨迹。 |
| 执行治理 | `POST /api/v1/infra/scheduler/executions/{executionId}/retry` | 对失败记录发起人工重试。 |
| 统计视图 | `GET /api/v1/infra/scheduler/dashboard/summary` | 返回任务运行总览、失败率与待处理异常。 |

## 关键查询/写入接口

| 方法 | 路径 | 用途 | 幂等/并发约束 | 权限与审计 |
|------|------|------|---------------|------------|
| `GET` | `/api/v1/infra/scheduler/jobs` | 查询任务定义列表 | 只读接口，无需幂等键 | 需要 `infra.scheduler.read`，记录查询审计。 |
| `POST` | `/api/v1/infra/scheduler/jobs` | 创建任务定义 | 必须传 `X-Idempotency-Key`；`jobCode` 全局唯一 | 需要 `infra.scheduler.manage`，记录定义快照。 |
| `PUT` | `/api/v1/infra/scheduler/jobs/{jobId}` | 更新任务定义 | 必须传 `expectedVersion` 防止覆盖并发修改 | 需要 `infra.scheduler.manage`，审计前后差异。 |
| `POST` | `/api/v1/infra/scheduler/jobs/{jobId}/enable` | 启用任务 | 自然幂等；重复启用返回当前状态 | 需要 `infra.scheduler.operate`，要求 `confirmToken` 与 `operationReason`。 |
| `POST` | `/api/v1/infra/scheduler/jobs/{jobId}/disable` | 停用任务 | 自然幂等；运行中批次不被强制终止 | 需要 `infra.scheduler.operate`，要求二次确认并写审计。 |
| `POST` | `/api/v1/infra/scheduler/jobs/{jobId}/trigger` | 手工触发一次执行 | 必须传 `X-Idempotency-Key`，避免重复点击造成重复批次 | 需要 `infra.scheduler.operate`，记录触发人、覆盖参数和原因。 |
| `GET` | `/api/v1/infra/scheduler/executions` | 查询执行记录 | 只读接口，无需幂等键 | 需要 `infra.scheduler.read`，敏感参数按安全策略脱敏。 |
| `GET` | `/api/v1/infra/scheduler/executions/{executionId}` | 查询单次执行详情 | 只读接口，无需幂等键 | 需要 `infra.scheduler.read`，返回完整重试链路。 |
| `POST` | `/api/v1/infra/scheduler/executions/{executionId}/retry` | 对失败执行进行人工重试 | 必须传 `X-Idempotency-Key`；仅失败且未被其他人重试的记录可操作 | 需要 `infra.scheduler.operate`，要求二次确认并生成治理审计。 |

## 分页与筛选

| 接口 | 分页规则 | 推荐筛选项 |
|------|----------|------------|
| `GET /jobs` | 使用 `page`、`size`、`sort` | `filter[jobCode]`、`filter[status]`、`filter[triggerType]`、`filter[tenantId]`、`filter[moduleCode]`、`filter[nextFireTime]gte/lte` |
| `GET /executions` | 使用 `page`、`size`、`sort=-startedAt` | `filter[jobCode]`、`filter[executionStatus]`、`filter[triggerType]`、`filter[tenantId]`、`filter[startedAt]gte/lte`、`filter[retryable]` |
| `GET /dashboard/summary` | 不分页 | 必须限制时间窗口，默认最近 24 小时，最大 31 天 |

## 幂等

| 场景 | 规则 |
|------|------|
| 创建任务 | `X-Idempotency-Key` + `jobCode` 双重约束，重复提交返回第一次结果。 |
| 手工触发 | 以 `jobId + idempotencyKey` 去重，同一键不得重复生成执行批次。 |
| 人工重试 | 以 `executionId + idempotencyKey` 去重，避免并发重复重试。 |
| 启停任务 | 状态写入天然幂等，重复调用仅返回当前状态。 |

## 权限与审计

| 维度 | 要求 |
|------|------|
| 权限分层 | `infra.scheduler.read` 仅可查询；`infra.scheduler.manage` 可维护定义；`infra.scheduler.operate` 可启停、触发、重试。 |
| 租户隔离 | 默认按租户可见范围过滤；全局任务仅平台管理员可见。 |
| 审计字段 | 记录 `operatorAccountId`、`operatorPersonId`、`tenantId`、`traceId`、操作原因、前后差异。 |
| 敏感数据 | 运行参数、错误上下文中的密钥或敏感字段必须按 `security` 脱敏规则输出。 |

## 治理动作约束

- 所有高风险动作都必须提交 `confirmToken` 和 `operationReason`，前端先展示影响面摘要再允许确认。
- `disable` 和 `retry` 操作必须返回 `operationId`，便于在审计页查看结果与后续重试链路。
- scheduler 只提供执行重试入口，不提供业务成功“回填”接口；如需回滚业务副作用，必须回到业务模块自己的治理接口。

## 错误处理

| 场景 | HTTP | 错误码建议 | 说明 |
|------|------|------------|------|
| Cron 非法 | `422` | `SCHEDULER_CRON_INVALID` | 表达式或时区不合法。 |
| 任务编码重复 | `409` | `SCHEDULER_JOB_CODE_DUPLICATE` | `jobCode` 已存在。 |
| 版本冲突 | `409` | `SCHEDULER_JOB_VERSION_CONFLICT` | 并发更新导致版本不一致。 |
| 任务已停用不可触发 | `409` | `SCHEDULER_JOB_DISABLED` | 停用任务不允许立即执行。 |
| 失败记录不可重试 | `422` | `SCHEDULER_EXECUTION_NOT_RETRYABLE` | 已成功、已重试或策略不允许。 |
| 执行超时 | `504` | `SCHEDULER_HANDLER_TIMEOUT` | 处理器超时，由执行记录保留错误上下文。 |
| 租户不可用 | `409` | `TENANT_DISABLED` | 关联租户已停用或未初始化。 |
