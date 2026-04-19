# process-definition API

本文档约束 `process-definition` 设计时接口，统一遵循 `docs/contracts/unified-api-contract.md`。模块前缀使用 `process`。

## 接口分组

| 分组 | 主要接口 | 说明 |
|------|----------|------|
| 定义管理 | `GET /api/v1/process/definitions`、`POST /api/v1/process/definitions`、`GET /api/v1/process/definitions/{definitionId}`、`PUT /api/v1/process/definitions/{definitionId}` | 管理草稿定义基础信息、节点和路由。 |
| 校验与发布 | `POST /api/v1/process/definitions/{definitionId}/validate`、`POST /api/v1/process/definitions/{definitionId}/publish`、`POST /api/v1/process/definitions/{definitionId}/deprecate` | 设计时质量门禁与版本切换。 |
| 版本链 | `POST /api/v1/process/definitions/{definitionId}/versions`、`GET /api/v1/process/definitions/{code}/versions` | 以已存在版本派生新草稿，不支持原地回滚覆盖。 |
| 设计器查询 | `GET /api/v1/process/definitions/{definitionId}/designer-schema` | 返回设计器画布、节点、动作编码和表单绑定快照。 |

## 关键查询/写入接口

| 方法 | 路径 | 说明 | 关键约束 |
|------|------|------|----------|
| `POST` | `/api/v1/process/definitions` | 新建流程定义草稿 | 写操作必须带 `X-Idempotency-Key`。 |
| `PUT` | `/api/v1/process/definitions/{definitionId}` | 更新草稿版节点、路由、参与者规则、动作编码 | 仅 `DRAFT` 状态可写。 |
| `POST` | `/api/v1/process/definitions/{definitionId}/validate` | 返回结构错误、告警和可发布判断 | 纯校验接口，不改变版本状态。 |
| `POST` | `/api/v1/process/definitions/{definitionId}/publish` | 发布草稿版本 | 绑定表单必须是已发布版本。 |
| `POST` | `/api/v1/process/definitions/{definitionId}/versions` | 基于当前版本复制出新草稿版本 | 老版本保持只读，供历史实例继续使用。 |
| `GET` | `/api/v1/process/definitions/{code}/versions` | 查询同一编码的版本链 | 默认按 `version,desc` 返回。 |

## 分页与筛选

- 列表查询使用统一分页参数：`page`、`size`、`sort`。
- 默认排序为 `updatedAt,desc`；版本链查询默认 `version,desc`。
- 支持筛选字段：
  - `filter[code]like`
  - `filter[name]like`
  - `filter[category]`
  - `filter[status]`
  - `filter[formMetadataId]`
  - `filter[updatedAt]gte` / `filter[updatedAt]lte`
- 设计器查询和详情查询不分页。

## 幂等

- `POST /definitions`、`PUT /definitions/{id}`、`POST /publish`、`POST /deprecate`、`POST /versions` 必须携带 `X-Idempotency-Key`。
- 同一幂等键 24 小时内重复提交，服务端返回首次结果；若资源状态已变化，返回 `409 IDEMPOTENT_DUPLICATE` 或 `409 VERSION_CONFLICT`。
- `POST /validate` 为天然幂等，不强制要求幂等键。

## 审批动作、草稿、归档边界

- 本模块只管理“流程定义草稿”，不管理业务草稿，也不管理运行中审批动作。
- 节点动作配置仅保存 `actionCodes` 和目标约束，真正的动作执行入口属于 `action-engine`。
- 归档属于 `process-instance` 运行时结果，本模块无归档接口。

## 权限与审计约束

- 仅工作流设计管理员或具备流程设计权限的管理端用户可创建、编辑、发布、废弃。
- 发布和废弃必须记录审计日志，至少包含 `requestId`、`definitionId`、`code`、`version`、操作者身份上下文和变更前后状态。
- 参与者规则预览可调用身份上下文服务，但不能在本模块落库组织权限快照。

## 错误场景

| 场景 | HTTP | 错误码建议 | 说明 |
|------|------|------------|------|
| 定义不存在 | `404` | `PROCESS_DEFINITION_NOT_FOUND` | `definitionId` 无效或租户不匹配。 |
| 草稿以外状态写入 | `409` | `PROCESS_DEFINITION_STATUS_INVALID` | 已发布或已废弃版本不可修改。 |
| 绑定表单未发布 | `409` | `PROCESS_FORM_METADATA_NOT_PUBLISHED` | 只允许绑定已发布表单版本。 |
| 缺少起止节点/路由不闭合 | `422` | `PROCESS_DEFINITION_VALIDATION_FAILED` | 发布前校验失败。 |
| 参与者规则非法 | `422` | `PROCESS_PARTICIPANT_RULE_INVALID` | 引用字段、组织或表达式不合法。 |
| 同编码版本冲突 | `409` | `PROCESS_DEFINITION_VERSION_CONFLICT` | 并发派生新版本或重复发布。 |
