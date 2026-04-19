# form-metadata API

本文档约束表单元数据接口，统一遵循 `docs/contracts/unified-api-contract.md`。模块前缀使用 `form`。

## 接口分组

| 分组 | 主要接口 | 说明 |
|------|----------|------|
| 表单定义管理 | `GET /api/v1/form/metadata`、`POST /api/v1/form/metadata`、`GET /api/v1/form/metadata/{metadataId}`、`PUT /api/v1/form/metadata/{metadataId}` | 管理草稿版表单定义、字段和布局。 |
| 校验与发布 | `POST /api/v1/form/metadata/{metadataId}/validate`、`POST /api/v1/form/metadata/{metadataId}/publish`、`POST /api/v1/form/metadata/{metadataId}/deprecate` | 设计时校验、发布和废弃。 |
| 版本链 | `POST /api/v1/form/metadata/{metadataId}/versions`、`GET /api/v1/form/metadata/{code}/versions` | 派生新版本、查询历史版本。 |
| 渲染协议输出 | `GET /api/v1/form/render-schemas/{code}`、`GET /api/v1/form/render-schemas/{code}/versions/{version}` | 输出稳定的渲染协议给前端渲染器和流程设计器。 |

## 关键查询/写入接口

| 方法 | 路径 | 说明 | 关键约束 |
|------|------|------|----------|
| `POST` | `/api/v1/form/metadata` | 新建表单元数据草稿 | 写操作必须带 `X-Idempotency-Key`。 |
| `PUT` | `/api/v1/form/metadata/{metadataId}` | 更新字段、布局、校验、联动和权限映射 | 仅 `DRAFT` 状态可写。 |
| `POST` | `/api/v1/form/metadata/{metadataId}/validate` | 校验字段编码、布局和权限映射 | 不改变当前版本状态。 |
| `POST` | `/api/v1/form/metadata/{metadataId}/publish` | 发布表单协议 | 发布后供流程定义绑定。 |
| `GET` | `/api/v1/form/render-schemas/{code}/versions/{version}` | 获取指定版本的标准渲染协议 | 仅返回发布版。 |

## 分页与筛选

- 列表查询统一使用 `page`、`size`、`sort`。
- 默认排序：
  - 表单列表：`updatedAt,desc`
  - 版本链：`version,desc`
- 支持筛选字段：
  - `filter[code]like`
  - `filter[name]like`
  - `filter[status]`
  - `filter[updatedAt]gte` / `filter[updatedAt]lte`
  - `filter[fieldType]in`（设计器搜索可选）

## 幂等

- `POST /metadata`、`PUT /metadata/{id}`、`POST /publish`、`POST /deprecate`、`POST /versions` 必须携带 `X-Idempotency-Key`。
- `POST /validate` 与渲染协议查询为天然幂等，不强制要求幂等键。
- 同一幂等键重复发布时应返回首次成功结果，不能重复生成新版本或重复发布事件。

## 审批动作、草稿、归档边界

- 本模块只拥有“表单定义草稿”，不拥有业务单据草稿。
- 审批动作属于 `action-engine`；本模块只输出字段状态协议，不执行审批行为。
- 归档属于实例运行结果，本模块没有归档读写能力。

## 权限与审计约束

- 仅具备表单设计权限的管理端用户可创建、编辑和发布表单元数据。
- 渲染协议查询允许被流程设计器、渲染器和运行时服务只读调用，但必须按租户隔离。
- 所有写操作必须记录字段、布局、权限映射和版本状态的变更审计。

## 错误场景

| 场景 | HTTP | 错误码建议 | 说明 |
|------|------|------------|------|
| 表单不存在 | `404` | `FORM_METADATA_NOT_FOUND` | `metadataId` 或 `code/version` 无效。 |
| 字段编码重复 | `422` | `FORM_FIELD_CODE_DUPLICATE` | 同一表单内字段编码必须唯一。 |
| 字段类型非法 | `422` | `FORM_FIELD_TYPE_INVALID` | 不支持的字段组件类型。 |
| 布局或子表结构非法 | `422` | `FORM_LAYOUT_INVALID` | 栅格、Tab 或子表定义不合法。 |
| 权限映射引用不存在字段/节点 | `422` | `FORM_PERMISSION_MAP_INVALID` | 仅允许映射已存在字段和节点。 |
| 发布状态冲突 | `409` | `FORM_METADATA_STATUS_INVALID` | 已发布/废弃版本不可修改。 |
