# portal-model API 合同

## 当前已落地接口

- `POST /api/v1/portal/model/templates`
  创建模板源记录，并按场景初始化默认画布骨架。
- `GET /api/v1/portal/model/templates/{templateId}`
  返回当前模板元数据和版本时间线。
- `GET /api/v1/portal/model/templates/{templateId}/canvas`
  返回当前模板画布骨架，包含页面、区域和卡片布置。
- `PUT /api/v1/portal/model/templates/{templateId}/canvas`
  保存指定模板的当前画布结构，按整幅画布替换方式更新页面、区域和卡片布置。
- `PUT /api/v1/portal/model/templates/{templateId}/publish`
  按顺序发布模板版本。
- `POST /api/v1/portal/model/templates/{templateId}/versions/{versionNo}/deprecate`
  废弃指定模板版本。
- `GET /api/v1/portal/model/resolutions/active?sceneType=...&clientType=...`
  解析当前生效发布及其关联模板元数据。

当前 `canvas` 已支持最小保存边界，但仍不在本阶段引入拖拽编辑、多用户协同、预览渲染或版本分支工作流。

## API 定位

`portal-model` API 负责门户模板设计时模型和模板发布模型的管理接口，覆盖模板、页面、区域、卡片布置、版本链和发布范围。它不提供聚合读模型，也不承担设计器独立持久化接口。

## 接口分组

| 分组 | 资源/对象 | 合同要求 |
|------|-----------|----------|
| 模板管理 | `PortalTemplate` | 提供模板创建、编辑、启停、归档、按场景查询等能力；模板编码租户内唯一。 |
| 页面与区域建模 | `PortalPage`、`LayoutRegion`、`WidgetPlacement` | 提供页面树、区域树、卡片布置维护；必须校验默认页、区域编码和布置编码唯一性。 |
| 版本管理 | `PortalVersion` | 提供草稿创建、复制、发布前校验、历史版本查询；已发布版本只读。 |
| 发布管理 | `PortalPublication`、`PortalAudienceBinding` | 提供立即发布、定时发布、下线、范围绑定、优先级调整。 |
| 生效解析与预览 | 发布解析结果 | 提供按 `sceneType + clientType + identity` 的解析查询，供 `personalization`、`portal-home` 和预览场景消费。 |

## 边界口径

- 读模型归 `aggregation-api`，本模块只输出模板结构和发布解析结果。
- 设计器是工具层，设计器保存、发布必须回写到本模块，不得建立第二套模板主数据接口。
- 模板发布对象是 `PortalVersion`，不是运行时页面快照。
- 卡片数据协议归 `widget-config`，快捷入口和主题偏好归 `personalization`，本模块不越界保存。

## 分页与筛选约定

| 查询对象 | 分页参数 | 主要筛选条件 |
|----------|----------|--------------|
| 模板列表 | `pageNo`、`pageSize`、`sortBy`、`sortOrder` | `sceneType`、`category`、`status`、`code`、`name` |
| 版本列表 | `pageNo`、`pageSize` | `portalTemplateId`、`status`、`versionNo` |
| 发布列表 | `pageNo`、`pageSize` | `sceneType`、`clientType`、`status`、`publishMode`、`subjectType` |
| 页面/区域查询 | 默认不分页，按模板版本全量返回 | `portalVersionId`、`pageType`、`visible` |

## 缓存、新鲜度、权限与审计约束

| 主题 | 合同要求 |
|------|----------|
| 缓存 | 模板解析结果允许短时缓存，但写操作后必须主动失效相关解析缓存。 |
| 新鲜度 | 发布、下线、范围调整后，下游读取必须以最新发布状态为准，不允许读到已废弃版本。 |
| 权限 | 模板和发布管理仅对门户管理员/运营管理员开放；普通用户只可读取命中自身身份的解析结果。 |
| 审计 | 模板创建、版本发布、范围调整、下线、归档必须记录审计日志，记录操作人和关键字段变更。 |

## 错误场景

| 场景 | 处理要求 |
|------|----------|
| 模板编码重复 | 拒绝创建/保存，返回编码冲突错误。 |
| 默认页不存在或不可见 | 拒绝版本保存或发布。 |
| 已发布版本被直接修改 | 返回只读错误，要求基于新草稿版本调整。 |
| 引用的卡片定义不存在或已停用 | 拒绝保存/发布，提示修复卡片引用。 |
| 发布时间窗冲突或范围裁决不合法 | 拒绝发布，要求调整时间窗、优先级或范围规则。 |
## Incremental Delivery Notes

- `GET /api/v1/portal/model/templates`
  Returns current-tenant portal templates and supports optional `sceneType` filtering.
- Current results are ordered by `templateCode` and then `templateId` for deterministic reads.
- `GET /api/v1/portal/model/publications`
  Returns current-tenant publications and supports optional `sceneType`, `clientType`, and `status` filtering.

## Draft And Published Snapshot Notes

- `GET /api/v1/portal/model/templates/{templateId}/canvas` remains the mutable draft canvas endpoint for design-time save/read flows.
- `PUT /api/v1/portal/model/templates/{templateId}/publish` now freezes the current draft canvas into the published version snapshot at publish time.
- Draft saves after a publish update only the working canvas. They do not change the live published snapshot until the next publish.
- `PUT /api/v1/portal/model/templates/{templateId}/canvas` and `PUT /api/v1/portal/model/templates/{templateId}/publish` now fail fast with `BUSINESS_RULE_VIOLATION` when any draft placement references a widget whose widget-reference state is `REPAIR_REQUIRED`.
- The validation message is stable and includes the affected `widgetCode`, `placementCode`, `pageCode`, and `regionCode` so callers can surface concrete repair targets.

## Audience Increment (2026-04-21)

- `PUT /api/v1/portal/model/publications/{publicationId}/activate`
  accepts optional `assignmentId`, `positionId`, and `personId`.
- Publication activation may specify at most one audience scope.
- If all audience fields are omitted, the stored publication audience is explicitly `tenant-default`.
- `GET /api/v1/portal/model/publications`
  now returns `audience` for every publication row.
- `GET /api/v1/portal/model/publications/{publicationId}`
  now returns `audience`.
- `GET /api/v1/portal/model/publications/active`
  accepts optional explicit identity query params `assignmentId`, `positionId`, and `personId`, and returns the selected publication `audience`.
- `GET /api/v1/portal/model/resolutions/active`
  accepts optional explicit identity query params `assignmentId`, `positionId`, and `personId`, and returns the selected publication `audience`.
- Active selection is deterministic and uses this precedence:
  `assignment > person > position > tenant-default`.
- Event payloads are unchanged in this increment. Audience metadata is source-contract/read-model only.

### Audience Response Shape

```json
{
  "audience": {
    "type": "tenant-default|assignment|person|position",
    "subjectId": "optional for scoped audience"
  }
}
```

### Activate Request Examples

Tenant default publication:

```json
{
  "templateId": "template-home-default",
  "sceneType": "HOME",
  "clientType": "PC"
}
```

Person-scoped publication:

```json
{
  "templateId": "template-home-person",
  "sceneType": "HOME",
  "clientType": "PC",
  "personId": "person-1"
}
```
