# process-instance API

本文档约束运行时实例接口，统一遵循 `docs/contracts/unified-api-contract.md`。模块前缀使用 `process`。

## 接口分组

| 分组 | 主要接口 | 说明 |
|------|----------|------|
| 发起与草稿 | `POST /api/v1/process/instances`、`POST /api/v1/process/drafts`、`PUT /api/v1/process/drafts/{draftId}`、`POST /api/v1/process/drafts/{draftId}/submit` | 流程发起前的输入保存与正式提交。 |
| 实例查询 | `GET /api/v1/process/instances/{instanceId}`、`GET /api/v1/process/instances/{instanceId}/timeline`、`GET /api/v1/process/instances` | 实例详情、轨迹和管理端查询。 |
| 任务查询与认领 | `GET /api/v1/process/tasks/{taskId}`、`POST /api/v1/process/tasks/{taskId}/claim` | 读取任务上下文并进行候选人认领。 |
| 实例控制 | `POST /api/v1/process/instances/{instanceId}/suspend`、`POST /api/v1/process/instances/{instanceId}/resume`、`POST /api/v1/process/instances/{instanceId}/terminate` | 管理端控制运行中实例。 |
| 归档查询 | `GET /api/v1/process/archives`、`GET /api/v1/process/archives/{instanceId}` | 只读查询已结束实例的归档数据。 |

## 关键查询/写入接口

| 方法 | 路径 | 说明 | 关键约束 |
|------|------|------|----------|
| `POST` | `/api/v1/process/instances` | 直接发起流程实例 | 定义和表单版本必须已发布。 |
| `POST` | `/api/v1/process/drafts` | 创建发起草稿 | 草稿不生成流程实例 ID。 |
| `POST` | `/api/v1/process/drafts/{draftId}/submit` | 将草稿转换为正式实例 | 提交成功后草稿转只读或删除。 |
| `POST` | `/api/v1/process/tasks/{taskId}/claim` | 候选人认领任务 | 仅候选人可认领，已分配单人任务无需认领。 |
| `POST` | `/api/v1/process/instances/{instanceId}/suspend` | 挂起实例 | 仅 `RUNNING` 实例可挂起。 |
| `GET` | `/api/v1/process/instances/{instanceId}/timeline` | 查询实例轨迹、节点状态和动作记录摘要 | 只读，不返回可编辑表单结构。 |

## 分页与筛选

- 列表查询统一使用 `page`、`size`、`sort`。
- 默认排序：
  - 实例列表：`startTime,desc`
  - 草稿列表：`updatedAt,desc`
  - 归档列表：`endTime,desc`
- 支持筛选字段：
  - `filter[definitionId]`
  - `filter[definitionCode]`
  - `filter[status]`
  - `filter[initiatorId]`
  - `filter[category]`
  - `filter[startTime]gte` / `filter[startTime]lte`
  - `filter[endTime]gte` / `filter[endTime]lte`
  - `filter[title]like`

## 幂等

- `POST /instances`、`POST /drafts`、`PUT /drafts/{id}`、`POST /drafts/{id}/submit`、`POST /claim`、`POST /suspend`、`POST /resume`、`POST /terminate` 必须携带 `X-Idempotency-Key`。
- 同一草稿提交重复请求必须返回首次创建的 `instanceId`，不能重复创建实例。
- 实例控制类接口按 `instanceId + action + idempotencyKey` 去重。
- 归档和详情查询为只读接口，不要求幂等键。

## 审批动作、草稿、归档边界

- 流程审批动作执行不在本模块完成，统一由 `action-engine` 的动作接口处理。
- 本模块拥有“发起草稿”能力，但不拥有业务域主数据草稿。
- 归档为系统在实例完成/终止后生成的只读结果，客户端不能手工创建或修改归档。

## 权限与审计约束

- 发起、草稿保存和草稿提交仅限当前业务发起人或其代理人。
- 任务详情可由当前处理人、候选人、实例发起人和具备流程管理权限的管理员查看。
- 挂起、恢复、终止仅限流程管理员或被授予实例控制权限的角色。
- 所有写接口必须记录审计日志，至少包含 `instanceId` / `taskId`、操作者身份上下文、`formDataId`、动作前后状态。

## 错误场景

| 场景 | HTTP | 错误码建议 | 说明 |
|------|------|------------|------|
| 流程定义未发布 | `409` | `PROCESS_DEFINITION_NOT_PUBLISHED` | 草稿或直接发起时校验失败。 |
| 表单快照不可用 | `409` | `FORM_METADATA_NOT_PUBLISHED` | 绑定表单版本不可用于发起。 |
| 草稿不存在或状态非法 | `404/409` | `PROCESS_DRAFT_NOT_FOUND` / `PROCESS_DRAFT_STATUS_INVALID` | 已提交草稿不可再次编辑。 |
| 非候选人认领 | `403` | `PROCESS_TASK_CLAIM_FORBIDDEN` | 当前身份不在候选集合中。 |
| 实例已结束仍执行控制 | `409` | `PROCESS_INSTANCE_STATUS_INVALID` | 已完成/终止实例不可再挂起、恢复。 |
| 归档未生成 | `404` | `PROCESS_ARCHIVE_NOT_FOUND` | 仅已结束实例有归档结果。 |
