# 错误码体系 API

## 接口分组

| 分组 | 主要接口 | 说明 |
|------|----------|------|
| 定义管理 | `GET /api/v1/infra/error-codes`、`POST /api/v1/infra/error-codes`、`GET /api/v1/infra/error-codes/{codeId}`、`PUT /api/v1/infra/error-codes/{codeId}` | 管理错误码定义、分类、严重级别和 HTTP 状态映射。 |
| 文档查询 | `GET /api/v1/infra/error-codes/catalog`、`GET /api/v1/infra/error-codes/{code}/document` | 输出面向开发和前端的错误码目录。 |
| 兼容治理 | `POST /api/v1/infra/error-codes/{codeId}/deprecate`、`POST /api/v1/infra/error-codes/{codeId}/replace-with` | 管理废弃和替代关系。 |
| 使用分析 | `GET /api/v1/infra/error-codes/usage` | 查询错误码使用频次、最近引用模块和趋势。 |

## 关键查询/写入接口

| 方法 | 路径 | 说明 | 关键约束 |
|------|------|------|----------|
| `POST` | `/api/v1/infra/error-codes` | 新建错误码定义 | 写操作必须携带 `X-Idempotency-Key`；`code` 全局唯一。 |
| `PUT` | `/api/v1/infra/error-codes/{codeId}` | 修改分类、严重级别、HTTP 状态、消息键、可重试属性 | 不允许无兼容期地修改已发布错误码语义。 |
| `POST` | `/api/v1/infra/error-codes/{codeId}/deprecate` | 标记错误码废弃 | 废弃后仍需保留兼容查询能力。 |
| `POST` | `/api/v1/infra/error-codes/{codeId}/replace-with` | 指定替代错误码 | 替代码必须已存在且语义兼容。 |
| `GET` | `/api/v1/infra/error-codes/catalog` | 按模块、分类、严重级别查询目录 | 默认按 `moduleCode,asc`、`code,asc` 排序。 |
| `GET` | `/api/v1/infra/error-codes/usage` | 查询错误码使用统计 | 统计结果只用于分析，不作为异常真相源。 |

## 分页与筛选

- 列表接口统一使用 `page`、`size`、`sort`。
- 默认排序：
  - 目录查询按 `moduleCode,asc`、`code,asc`
  - 使用统计按 `count,desc`
- 支持筛选字段：
  - `filter[code]like`
  - `filter[moduleCode]`
  - `filter[category]`
  - `filter[severity]`
  - `filter[httpStatus]`
  - `filter[retryable]`
  - `filter[deprecated]`
  - `filter[messageKey]like`
- 文档详情接口不分页。

## 幂等

- 定义新增、修改、废弃和替代接口必须携带 `X-Idempotency-Key`。
- 同一幂等键在 24 小时内重复提交时返回首次结果；若目标错误码状态已改变，返回 `409 IDEMPOTENT_DUPLICATE`。
- 废弃和替代动作以 `codeId + targetCodeId + 幂等键` 判定重复。
- 目录查询和统计查询天然幂等。

## 权限与审计约束

- 仅错误码管理员或基础设施管理员可新增、修改、废弃错误码定义。
- 业务模块和前端默认只读消费目录，不允许绕过本模块自定义对外错误码。
- 所有写操作必须进入统一审计，至少记录 `code`、`moduleCode`、`changeType`、`messageKey`、操作者和兼容性影响说明。
- 废弃和替代属于治理动作，应有明确影响范围提示和二次确认。

## 错误处理

| 场景 | HTTP | 错误码建议 | 说明 |
|------|------|------------|------|
| 错误码定义不存在 | `404` | `INFRA_ERROR_CODE_NOT_FOUND` | `codeId` 或 `code` 无效。 |
| 错误码冲突 | `409` | `INFRA_ERROR_CODE_CONFLICT` | `code` 已存在。 |
| 国际化消息键缺失 | `422` | `INFRA_ERROR_CODE_MESSAGE_KEY_INVALID` | `messageKey` 无法解析。 |
| 替代关系非法 | `422` | `INFRA_ERROR_CODE_REPLACEMENT_INVALID` | 替代码不存在或语义不兼容。 |
| 已废弃错误码被非法重定义 | `409` | `INFRA_ERROR_CODE_DEPRECATED_MUTATION_FORBIDDEN` | 废弃后仅允许受控兼容治理。 |
| 统计范围非法 | `422` | `INFRA_ERROR_CODE_USAGE_SCOPE_INVALID` | 时间窗或筛选条件无效。 |
