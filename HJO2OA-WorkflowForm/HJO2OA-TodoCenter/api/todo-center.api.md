# todo-center API

本文档约束待办中心聚合查询接口，统一遵循 `docs/contracts/unified-api-contract.md`。模块前缀使用 `todo`。

## 接口分组

| 分组 | 主要接口 | 说明 |
|------|----------|------|
| 待办与已办 | `GET /api/v1/todo/pending`、`GET /api/v1/todo/completed`、`GET /api/v1/todo/{todoId}` | 用户处理视图与详情跳转入口。 |
| 我发起与抄送 | `GET /api/v1/todo/initiated`、`GET /api/v1/todo/copied`、`POST /api/v1/todo/copied/{todoId}/read` | 查询发起记录和抄送阅知。 |
| 草稿与归档 | `GET /api/v1/todo/drafts`、`GET /api/v1/todo/archives`、`GET /api/v1/todo/archives/{instanceId}` | 聚合展示草稿箱和归档结果。 |
| 数量统计 | `GET /api/v1/todo/counts` | 返回待办、抄送未读、草稿等数量。 |

## 关键查询/写入接口

| 方法 | 路径 | 说明 | 关键约束 |
|------|------|------|----------|
| `GET` | `/api/v1/todo/pending` | 查询当前身份下的待办列表 | 结果必须受当前 `positionId/orgId` 裁剪。 |
| `GET` | `/api/v1/todo/completed` | 查询已办列表 | 展示处理结果和完成时间。 |
| `GET` | `/api/v1/todo/initiated` | 查询我发起列表 | 基于实例权威数据聚合。 |
| `GET` | `/api/v1/todo/copied` | 查询抄送列表 | 支持已读/未读筛选。 |
| `POST` | `/api/v1/todo/copied/{todoId}/read` | 标记抄送已阅 | 仅改变抄送读状态，不影响流程状态。 |
| `GET` | `/api/v1/todo/drafts` | 查询草稿箱 | 草稿数据来源于 `process-instance`。 |
| `GET` | `/api/v1/todo/archives` | 查询归档列表 | 只返回已完成/终止实例。 |

## 分页与筛选

- 所有列表接口统一使用 `page`、`size`、`sort`。
- 默认排序：
  - 待办：`dueTime,asc;createdAt,desc`
  - 已办：`completedTime,desc`
  - 我发起：`startTime,desc`
  - 抄送：`createdAt,desc`
  - 草稿：`updatedAt,desc`
  - 归档：`endTime,desc`
- 支持筛选字段：
  - `filter[category]`
  - `filter[urgency]`
  - `filter[title]like`
  - `filter[definitionCode]`
  - `filter[nodeName]like`
  - `filter[readStatus]`
  - `filter[dueTime]gte` / `filter[dueTime]lte`
  - `filter[startTime]gte` / `filter[endTime]lte`

## 幂等

- 查询接口为只读接口，不要求幂等键。
- `POST /copied/{todoId}/read` 为天然幂等；重复标记已读必须返回相同结果，不得报错。
- 若客户端仍携带 `X-Idempotency-Key`，服务端可照常记录，但不应改变天然幂等语义。

## 审批动作、草稿、归档边界

- 待办中心不提供审批动作执行接口，处理入口统一跳转到 `process-instance`/`action-engine`。
- 草稿保存、草稿提交由 `process-instance` 负责，待办中心只聚合展示草稿列表。
- 归档为已结束实例的只读结果，待办中心只提供查询视图。

## 权限与审计约束

- 所有列表、数量和详情都必须以当前身份上下文为过滤前提，身份切换后必须重新查询。
- “我发起”按 `initiatorId` 聚合，不因当前组织切换改变已发起记录归属。
- 抄送阅知写操作必须记录审计日志，包含 `todoId`、当前身份和读状态变化。

## 错误场景

| 场景 | HTTP | 错误码建议 | 说明 |
|------|------|------------|------|
| 待办或抄送项不存在 | `404` | `TODO_ITEM_NOT_FOUND` | `todoId` 不存在或不属于当前身份视图。 |
| 当前身份无权查看目标记录 | `403` | `TODO_ACCESS_FORBIDDEN` | 身份上下文不匹配。 |
| 筛选条件非法 | `422` | `TODO_FILTER_INVALID` | 时间范围或字段组合不合法。 |
| 归档不存在 | `404` | `TODO_ARCHIVE_NOT_FOUND` | 目标实例未结束或不在当前权限范围内。 |
