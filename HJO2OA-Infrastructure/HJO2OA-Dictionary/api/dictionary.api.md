# 数据字典 API

## 接口分组

| 分组 | 主要接口 | 说明 |
|------|----------|------|
| 字典类型管理 | `GET /api/v1/infra/dictionary-types`、`POST /api/v1/infra/dictionary-types`、`GET /api/v1/infra/dictionary-types/{typeId}`、`PUT /api/v1/infra/dictionary-types/{typeId}` | 管理字典类型基础属性、层级开关和租户归属。 |
| 字典项管理 | `GET /api/v1/infra/dictionary-types/{typeId}/items`、`POST /api/v1/infra/dictionary-types/{typeId}/items`、`PUT /api/v1/infra/dictionary-items/{itemId}`、`POST /api/v1/infra/dictionary-items/{itemId}/enable`、`POST /api/v1/infra/dictionary-items/{itemId}/disable` | 管理字典项树、排序、启停和展示值。 |
| 引用查询 | `GET /api/v1/infra/dictionaries/{code}/items`、`GET /api/v1/infra/dictionaries/{code}/resolve` | 面向业务模块和前端组件输出稳定查询接口。 |
| 导入导出 | `POST /api/v1/infra/dictionary-types/{typeId}/import-jobs`、`POST /api/v1/infra/dictionary-types/{typeId}/export-jobs` | 提供批量治理和模板导入导出。 |
| 变更治理 | `GET /api/v1/infra/dictionary-types/{typeId}/changes`、`POST /api/v1/infra/dictionary-items/reorder` | 查询变更记录并执行批量排序。 |

## 关键查询/写入接口

| 方法 | 路径 | 说明 | 关键约束 |
|------|------|------|----------|
| `POST` | `/api/v1/infra/dictionary-types` | 新建字典类型 | 写操作必须携带 `X-Idempotency-Key`；`code` 唯一。 |
| `PUT` | `/api/v1/infra/dictionary-types/{typeId}` | 修改字典类型名称、分类、缓存属性 | 不允许跨租户迁移已有类型。 |
| `POST` | `/api/v1/infra/dictionary-types/{typeId}/items` | 新增字典项 | 层级字典必须校验父子关系，不得形成环路。 |
| `PUT` | `/api/v1/infra/dictionary-items/{itemId}` | 修改展示值、多语言值、排序和父项 | 停用项可修改，但需保留历史编码。 |
| `POST` | `/api/v1/infra/dictionary-items/reorder` | 批量调整同层级排序 | 仅允许同一 `typeId + parentItemId` 范围内重排。 |
| `GET` | `/api/v1/infra/dictionaries/{code}/items` | 按字典编码查询启用项 | 默认返回启用项，支持树形或平铺输出。 |
| `POST` | `/api/v1/infra/dictionary-types/{typeId}/import-jobs` | 批量导入字典项 | 同一批次文件需支持失败项明细和重复提交幂等。 |

## 分页与筛选

- 列表接口统一使用 `page`、`size`、`sort`。
- 默认排序：
  - 字典类型列表按 `updatedAt,desc`
  - 字典项列表按 `parentItemId,asc`、`sortOrder,asc`
- 支持筛选字段：
  - `filter[code]like`
  - `filter[name]like`
  - `filter[category]`
  - `filter[status]`
  - `filter[tenantId]`
  - `filter[hierarchical]`
  - `filter[parentItemId]`
  - `filter[enabled]`
- 引用查询接口不分页，但允许 `tree=true`、`enabledOnly=true`、`locale=zh-CN` 等参数。

## 幂等

- `POST /dictionary-types`、`POST /items`、`POST /import-jobs`、`POST /reorder`、启停接口必须携带 `X-Idempotency-Key`。
- 同一幂等键在 24 小时内重复提交时返回首次结果；若资源状态已变化，返回 `409 IDEMPOTENT_DUPLICATE`。
- 批量导入以 `typeId + 文件摘要 + 幂等键` 判定重复，避免重复入库。
- 只读查询天然幂等，不要求额外幂等键。

## 权限与审计约束

- 仅基础设施管理员、字典管理员可新增、修改、启停、导入导出字典数据。
- 业务模块和普通前端只允许读，不允许通过业务接口直接写字典真相源。
- 所有写接口必须记录统一审计，至少包含 `typeId/itemId`、变更摘要、操作者、租户、`traceId` 与前后值差异。
- 批量导入、手工修复、启停和排序调整属于治理动作，需具备二次确认和审计追踪。

## 错误处理

| 场景 | HTTP | 错误码建议 | 说明 |
|------|------|------------|------|
| 字典类型不存在 | `404` | `INFRA_DICTIONARY_TYPE_NOT_FOUND` | `typeId` 无效或租户不匹配。 |
| 字典编码冲突 | `409` | `INFRA_DICTIONARY_CODE_CONFLICT` | 同作用域内 `code` 已存在。 |
| 字典项编码冲突 | `409` | `INFRA_DICTIONARY_ITEM_CODE_CONFLICT` | 同一字典类型内 `itemCode` 已存在。 |
| 层级形成环路 | `422` | `INFRA_DICTIONARY_HIERARCHY_INVALID` | 父子关系非法。 |
| 停用项仍被治理流程锁定 | `409` | `INFRA_DICTIONARY_ITEM_STATE_INVALID` | 治理动作与当前状态冲突。 |
| 导入文件格式错误 | `422` | `INFRA_DICTIONARY_IMPORT_INVALID` | 模板列缺失、编码非法或重复。 |
