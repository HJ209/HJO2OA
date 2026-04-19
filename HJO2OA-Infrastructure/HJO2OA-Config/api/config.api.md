# 配置中心 API

## 接口分组

| 分组 | 主要接口 | 说明 |
|------|----------|------|
| 配置项管理 | `GET /api/v1/infra/config-entries`、`POST /api/v1/infra/config-entries`、`GET /api/v1/infra/config-entries/{entryId}`、`PUT /api/v1/infra/config-entries/{entryId}` | 管理配置键、默认值、类型和热更新属性。 |
| 覆盖链管理 | `GET /api/v1/infra/config-entries/{entryId}/overrides`、`POST /api/v1/infra/config-entries/{entryId}/overrides`、`PUT /api/v1/infra/config-overrides/{overrideId}`、`POST /api/v1/infra/config-overrides/{overrideId}/disable` | 管理租户/主体级覆盖链。 |
| Feature Flag 管理 | `GET /api/v1/infra/feature-flags`、`POST /api/v1/infra/feature-flags`、`POST /api/v1/infra/feature-flags/{entryId}/rules`、`PUT /api/v1/infra/feature-rules/{ruleId}` | 管理开关定义、规则命中顺序和灰度策略。 |
| 配置解析 | `GET /api/v1/infra/config-resolution`、`POST /api/v1/infra/config-resolution/preview` | 预览最终生效值和命中链路。 |
| 历史与回滚 | `GET /api/v1/infra/config-entries/{entryId}/changes`、`POST /api/v1/infra/config-entries/{entryId}/rollback` | 查询变更历史并执行回滚。 |

## 关键查询/写入接口

| 方法 | 路径 | 说明 | 关键约束 |
|------|------|------|----------|
| `POST` | `/api/v1/infra/config-entries` | 新建配置项 | 必须携带 `X-Idempotency-Key`；`configKey` 唯一。 |
| `PUT` | `/api/v1/infra/config-entries/{entryId}` | 修改默认值、校验规则、热更新属性 | 已废弃配置不可恢复为不同语义的同键配置。 |
| `POST` | `/api/v1/infra/config-entries/{entryId}/overrides` | 新增作用域覆盖 | 仅引用主体 ID，不复制主体主数据。 |
| `POST` | `/api/v1/infra/feature-flags/{entryId}/rules` | 新增 Feature Flag 命中规则 | 仅允许 `FEATURE_FLAG` 类型配置使用。 |
| `POST` | `/api/v1/infra/config-resolution/preview` | 预览给定上下文的最终生效值 | 纯预览，不落库。 |
| `POST` | `/api/v1/infra/config-entries/{entryId}/rollback` | 基于历史版本回滚 | 回滚生成新的当前版本，而不是覆盖旧版本。 |
| `GET` | `/api/v1/infra/config-resolution` | 查询当前生效值 | 默认返回命中后的最终值，可选展示命中链。 |

## 分页与筛选

- 列表接口统一使用 `page`、`size`、`sort`。
- 默认排序：
  - 配置项按 `updatedAt,desc`
  - 覆盖链按 `scopeType,asc`、`updatedAt,desc`
  - Feature Rule 按 `sortOrder,asc`
- 支持筛选字段：
  - `filter[configKey]like`
  - `filter[name]like`
  - `filter[configType]`
  - `filter[status]`
  - `filter[tenantAware]`
  - `filter[mutableAtRuntime]`
  - `filter[scopeType]`
  - `filter[scopeId]`
  - `filter[moduleCode]`
- 配置解析接口不分页，支持 `tenantId`、`organizationId`、`roleId`、`userId`、`includeTrace=true`。

## 幂等

- 所有写接口必须携带 `X-Idempotency-Key`，包括新增配置、覆盖链调整、规则变更和回滚。
- 同一幂等键在 24 小时内重复提交时返回首次结果；如配置状态已变化，返回 `409 IDEMPOTENT_DUPLICATE`。
- 回滚接口以 `entryId + targetVersion + 幂等键` 判定重复，避免重复回滚。
- 配置解析与预览接口天然幂等。

## 权限与审计约束

- 仅基础设施配置管理员可编辑默认值、覆盖链和 Feature Flag。
- 回滚、灰度调整、批量禁用覆盖属于高风险治理动作，应具备更高权限并要求二次确认。
- 所有写接口必须进入统一审计，至少记录 `configKey`、`scopeType`、`scopeId`、`changeType`、操作者、租户、`traceId` 与前后值。
- 业务模块只读消费配置，不允许绕过本模块直接修改配置表或缓存。

## 错误处理

| 场景 | HTTP | 错误码建议 | 说明 |
|------|------|------------|------|
| 配置项不存在 | `404` | `INFRA_CONFIG_ENTRY_NOT_FOUND` | `entryId` 无效。 |
| 配置键冲突 | `409` | `INFRA_CONFIG_KEY_CONFLICT` | `configKey` 已存在。 |
| 覆盖范围非法 | `422` | `INFRA_CONFIG_SCOPE_INVALID` | 作用域类型与配置能力不匹配。 |
| 非 Feature Flag 配置写规则 | `422` | `INFRA_FEATURE_FLAG_TYPE_INVALID` | 只有 `FEATURE_FLAG` 类型可配置规则。 |
| 规则顺序冲突或缺失 | `409` | `INFRA_FEATURE_RULE_ORDER_INVALID` | 命中顺序不明确。 |
| 禁止热更新的配置被即时修改 | `409` | `INFRA_CONFIG_RUNTIME_MUTATION_FORBIDDEN` | 需走受控生效流程。 |
| 回滚目标不存在 | `404` | `INFRA_CONFIG_ROLLBACK_TARGET_NOT_FOUND` | 历史版本不存在。 |
