# org-sync-audit API 契约

## 接口分组

| 分组 | 目标 | 核心接口 |
|------|------|----------|
| 同步源管理 | 查询和维护同步源 | `GET /api/org-perm/sync-sources`、`POST /api/org-perm/sync-sources`、`PUT /api/org-perm/sync-sources/{sourceId}`、`POST /api/org-perm/sync-sources/{sourceId}/status` |
| 同步任务管理 | 查询、触发、重试同步任务 | `GET /api/org-perm/sync-tasks`、`GET /api/org-perm/sync-tasks/{taskId}`、`POST /api/org-perm/sync-tasks/run`、`POST /api/org-perm/sync-tasks/{taskId}/retry` |
| 差异治理 | 查询差异、处理冲突、执行人工修正 | `GET /api/org-perm/sync-diffs`、`GET /api/org-perm/sync-diffs/{diffId}`、`POST /api/org-perm/sync-diffs/{diffId}/resolve` |
| 审计查询 | 查询审计日志和详情时间线 | `GET /api/org-perm/audit-logs`、`GET /api/org-perm/audit-logs/{auditLogId}` |

## 关键查询接口

| 方法 | 路径 | 说明 | 关键参数 | 关键返回 |
|------|------|------|----------|----------|
| `GET` | `/api/org-perm/sync-sources` | 同步源分页查询 | `sourceType`、`status`、`keyword`、`pageNo`、`pageSize` | 返回源类型、状态、上次同步时间、最近结果 |
| `GET` | `/api/org-perm/sync-tasks` | 同步任务分页查询 | `sourceId`、`syncMode`、`status`、`startedFrom`、`startedTo`、`pageNo`、`pageSize` | 返回任务状态、成功/失败统计、重试次数 |
| `GET` | `/api/org-perm/sync-tasks/{taskId}` | 查询任务详情 | 路径参数 `taskId` | 返回处理明细、错误摘要、关联差异数 |
| `GET` | `/api/org-perm/sync-diffs` | 查询差异列表 | `taskId`、`entityType`、`resolutionStatus`、`keyword`、`pageNo`、`pageSize` | 返回源端值、本地值、冲突类型、推荐动作 |
| `GET` | `/api/org-perm/audit-logs` | 查询审计日志 | `auditType`、`entityType`、`entityId`、`operatorId`、`timeFrom`、`timeTo`、`pageNo`、`pageSize` | 返回时间线摘要、操作者、触发来源、关联任务号 |
| `GET` | `/api/org-perm/audit-logs/{auditLogId}` | 查询审计详情 | 路径参数 `auditLogId` | 返回前后快照、事件来源、关联同步任务和原始事件摘要 |

## 关键写入接口

| 方法 | 路径 | 说明 | 关键入参 | 写入约束 |
|------|------|------|----------|----------|
| `POST` | `/api/org-perm/sync-sources` | 新增同步源 | `sourceType`、`name`、`connectionConfig`、`syncScopes`、`scheduleConfig` | 敏感凭据必须加密存储；创建后需连通性校验 |
| `PUT` | `/api/org-perm/sync-sources/{sourceId}` | 编辑同步源 | 同创建字段 + `version` | 变更连接信息前建议校验连通性 |
| `POST` | `/api/org-perm/sync-sources/{sourceId}/status` | 启停用同步源 | `status`、`reason`、`version` | 停用后不得继续触发新任务 |
| `POST` | `/api/org-perm/sync-tasks/run` | 手工触发同步任务 | `sourceId`、`syncMode=FULL|INCREMENTAL`、`reason` | 同一源同一时刻只能存在一个运行中任务 |
| `POST` | `/api/org-perm/sync-tasks/{taskId}/retry` | 重试失败任务 | `reason` | 仅失败或部分失败任务允许重试 |
| `POST` | `/api/org-perm/sync-diffs/{diffId}/resolve` | 处理差异 | `resolutionAction=ACCEPT_SOURCE|KEEP_LOCAL|MANUAL_FIX`、`reason`、`manualPayload` | `MANUAL_FIX` 必须调用真相源模块接口，不允许直接改差异快照代替落库 |

## 通用约束

### 筛选与分页

- 同步任务、差异清单和审计日志统一使用 `pageNo/pageSize`，`pageSize` 一期建议上限 `100`。
- 审计日志必须支持按实体类型、操作者、时间范围和来源任务号筛选。
- 任务详情和审计详情接口不分页，应返回完整摘要和关键信息。

### 批量操作

- 一期同步任务以“任务维度”批量处理，不开放外部直接提交大批量差异修正。
- 模板化的差异处理可在后续补充，一期先支持逐条处理并记录每条决策原因。
- 同一任务内的重试明细应按实体返回逐条结果，方便实施排障。

### 幂等要求

- 触发同步任务、任务重试和差异处理都应支持 `X-Idempotency-Key`。
- 同一源的同一批次同步请求若重复提交，应复用已有运行中任务或返回幂等结果。
- 差异处理接口必须使用 `diffId + version` 保证幂等和并发安全。

### 权限与审计约束

- 页面和接口权限至少区分 `sync-source.read`、`sync-source.write`、`sync-task.run`、`sync-diff.resolve`、`audit-log.read`。
- 同步源配置、任务运行、差异处理和重试都必须进入审计日志。
- 差异处理的 `reason` 必填，且应保留处理前后的快照与关联任务号。

## 错误场景

| 场景 | HTTP 建议 | 错误码 | 说明 |
|------|-----------|--------|------|
| 同步源配置校验失败 | `400` | `SYNC_SOURCE_INVALID` | 连接配置缺失或格式错误 |
| 同一源已有运行中任务 | `409` | `SYNC_TASK_ALREADY_RUNNING` | 不允许并发运行多个任务 |
| 重试了不可重试状态的任务 | `409` | `SYNC_TASK_NOT_RETRYABLE` | 仅失败或部分失败任务可重试 |
| 差异已被他人处理 | `409` | `SYNC_DIFF_ALREADY_RESOLVED` | 需刷新后查看最新状态 |
| 人工修正越过真相源模块 | `403` | `SYNC_MANUAL_FIX_OUT_OF_SCOPE` | 禁止直接改快照替代落库 |
| 下游真相源写入失败 | `502` | `SYNC_TARGET_WRITE_FAILED` | 需记录目标模块、错误码和补偿建议 |
