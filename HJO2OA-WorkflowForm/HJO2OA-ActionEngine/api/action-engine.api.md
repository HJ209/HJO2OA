# action-engine API

本文档约束任务动作接口。模块前缀仍使用 `process`，因为动作执行属于流程运行时内部能力。

## 接口分组

| 分组 | 主要接口 | 说明 |
|------|----------|------|
| 动作可用性查询 | `GET /api/v1/process/tasks/{taskId}/actions` | 返回当前任务可用动作、禁用原因和附加输入要求。 |
| 动作执行 | `POST /api/v1/process/tasks/{taskId}/actions/execute` | 执行审批动作并落审计记录。 |
| 参与者预览 | `POST /api/v1/process/tasks/{taskId}/participants/preview` | 在提交前预览下一节点候选人。 |
| 动作留痕查询 | `GET /api/v1/process/action-records` | 查询不可变动作记录，供轨迹与审计使用。 |

## 关键查询/写入接口

| 方法 | 路径 | 说明 | 关键约束 |
|------|------|------|----------|
| `GET` | `/api/v1/process/tasks/{taskId}/actions` | 获取可用动作、是否要求意见、是否要求目标节点/目标参与人 | 无可用动作时返回空集合，不自动补默认动作。 |
| `POST` | `/api/v1/process/tasks/{taskId}/actions/execute` | 执行任务动作 | 请求体至少包含 `actionCode`，按动作要求补充 `opinion`、`targetNodeId`、`targetAssigneeIds`、`formDataPatch`。 |
| `POST` | `/api/v1/process/tasks/{taskId}/participants/preview` | 预览下一节点候选人集合 | 只做试算，不落动作记录。 |
| `GET` | `/api/v1/process/action-records` | 按实例、任务、操作者、动作类型查询动作留痕 | 默认按 `createdAt,desc` 排序。 |

## 分页与筛选

- 动作记录查询使用统一分页参数：`page`、`size`、`sort`。
- 支持筛选字段：
  - `filter[instanceId]`
  - `filter[taskId]`
  - `filter[actionCode]`
  - `filter[operatorId]`
  - `filter[createdAt]gte` / `filter[createdAt]lte`
- `GET /tasks/{taskId}/actions` 和参与者预览接口不分页。

## 幂等

- `POST /api/v1/process/tasks/{taskId}/actions/execute` 和参与者预览接口必须带 `X-Idempotency-Key`。
- 动作执行按 `taskId + actionCode + idempotencyKey` 去重，重复请求返回首次结果，禁止重复生成 `TaskAction`。
- 可用动作查询和动作留痕查询为只读接口，不要求幂等键。

## 审批动作、草稿、归档边界

- 本模块只拥有任务动作执行与动作留痕，不拥有流程发起草稿。
- 归档查询由 `process-instance`/`todo-center` 提供，本模块不提供归档接口。
- 实例级挂起、恢复、终止属于 `process-instance` 控制命令，不属于 `actions/execute` 的职责范围。

## 权限与审计约束

- 动作执行者必须是当前处理人、合法候选人或被授予特定流程管理权限的管理员。
- 每次动作执行必须记录操作者 `accountId/personId/positionId/orgId`、意见、目标节点、表单补丁和动作结果。
- 对于要求意见的动作，前端禁用不等于后端放行，后端仍必须强校验。

## 错误场景

| 场景 | HTTP | 错误码建议 | 说明 |
|------|------|------------|------|
| 任务不存在或已结束 | `404/409` | `PROCESS_TASK_NOT_FOUND` / `PROCESS_TASK_STATUS_INVALID` | 已处理任务不可再次执行动作。 |
| 动作未配置或不可用 | `409` | `PROCESS_ACTION_NOT_AVAILABLE` | 当前节点未开放该动作。 |
| 缺少意见或目标节点 | `422` | `PROCESS_ACTION_ARGUMENT_INVALID` | 动作定义要求的参数缺失。 |
| 候选人计算为空 | `422` | `PROCESS_PARTICIPANT_RESOLVE_EMPTY` | 下一节点无合法处理人。 |
| 路由条件冲突或无结果 | `422` | `PROCESS_ROUTE_RESOLUTION_FAILED` | 条件路由无法唯一确定目标。 |
| 当前身份无权执行 | `403` | `PROCESS_ACTION_FORBIDDEN` | 当前身份不满足动作权限。 |
