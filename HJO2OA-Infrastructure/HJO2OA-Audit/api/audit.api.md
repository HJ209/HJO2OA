# 审计日志 API

## 接口分组

| 分组 | 主要接口 | 说明 |
|------|----------|------|
| 内部写入 | `POST /api/v1/infra/audits` | 面向内部模块和基础设施组件的统一审计写入入口。 |
| 列表查询 | `GET /api/v1/infra/audits` | 按模块、对象、操作者、租户、时间范围查询审计记录。 |
| 详情查询 | `GET /api/v1/infra/audits/{auditId}`、`GET /api/v1/infra/audits/{auditId}/field-changes` | 查询审计详情和字段级差异。 |
| 归档治理 | `POST /api/v1/infra/audits/archive-jobs`、`GET /api/v1/infra/audits/archive-jobs/{jobId}` | 发起归档并查询归档结果。 |
| 导出治理 | `POST /api/v1/infra/audits/export-jobs`、`GET /api/v1/infra/audits/export-jobs/{jobId}` | 发起导出任务并获取结果。 |

## 关键查询/写入接口

| 方法 | 路径 | 说明 | 关键约束 |
|------|------|------|----------|
| `POST` | `/api/v1/infra/audits` | 写入一条审计记录及字段差异 | 仅允许追加写入，不允许更新既有记录。 |
| `GET` | `/api/v1/infra/audits` | 查询审计日志列表 | 默认按 `occurredAt,desc` 返回。 |
| `GET` | `/api/v1/infra/audits/{auditId}` | 查看审计详情 | 敏感字段按权限脱敏。 |
| `GET` | `/api/v1/infra/audits/{auditId}/field-changes` | 查看字段前后值差异 | 默认屏蔽高敏原值。 |
| `POST` | `/api/v1/infra/audits/archive-jobs` | 归档指定时间范围审计记录 | 归档后原记录只改 `archiveStatus`，不改业务含义。 |
| `POST` | `/api/v1/infra/audits/export-jobs` | 导出审计记录 | 大范围导出必须走异步任务。 |

## 分页与筛选

- 列表查询使用统一分页参数 `page`、`size`、`sort`。
- 默认排序为 `occurredAt,desc`。
- 支持筛选字段：
  - `filter[moduleCode]`
  - `filter[objectType]`
  - `filter[objectId]`
  - `filter[actionType]`
  - `filter[operatorAccountId]`
  - `filter[operatorPersonId]`
  - `filter[tenantId]`
  - `filter[traceId]`
  - `filter[archiveStatus]`
  - `filter[occurredAt]gte` / `filter[occurredAt]lte`
- 详情接口不分页，字段差异接口可按 `fieldName` 过滤。

## 幂等

- `POST /audits`、归档和导出接口必须携带 `X-Idempotency-Key`。
- 审计写入建议以 `sourceType + sourceId + actionType + idempotencyKey` 判定重复，避免同一事件或同一请求重复留痕。
- 归档作业以 `时间范围 + 模块范围 + 幂等键` 判定重复，避免重复归档。
- 查询接口天然幂等。

## 权限与审计约束

- 业务模块只能写入本模块的审计入口，不能直接修改审计表。
- 审计员、合规管理员可查询与导出；敏感原值查看权限应单独控制。
- 归档和大范围导出属于高风险治理动作，需二次确认并写入新的审计记录。
- 审计模块自身的归档、导出、权限校验失败也应记录操作日志，但不反向篡改原始审计记录。

## 错误处理

| 场景 | HTTP | 错误码建议 | 说明 |
|------|------|------------|------|
| 审计记录不存在 | `404` | `INFRA_AUDIT_RECORD_NOT_FOUND` | `auditId` 无效。 |
| 重复写入 | `409` | `INFRA_AUDIT_DUPLICATE` | 同一来源重复提交。 |
| 字段差异体过大 | `422` | `INFRA_AUDIT_FIELD_CHANGE_TOO_LARGE` | 超过一期允许的单次差异载荷。 |
| 敏感字段无权查看 | `403` | `INFRA_AUDIT_SENSITIVE_ACCESS_DENIED` | 仅返回脱敏视图。 |
| 归档范围非法 | `422` | `INFRA_AUDIT_ARCHIVE_SCOPE_INVALID` | 时间范围或筛选条件不合法。 |
| 导出范围过大 | `422` | `INFRA_AUDIT_EXPORT_SCOPE_TOO_LARGE` | 需缩小范围或分批导出。 |
