# portal-designer API 合同

## 当前已落地接口

- `GET /api/v1/portal/designer/templates/{templateId}/status`
  返回模板时间线和当前线上发布状态摘要。
- `GET /api/v1/portal/designer/templates/{templateId}/widget-palette`
  返回按模板场景过滤后的组件面板元数据。
- `GET /api/v1/portal/designer/templates/{templateId}/init`
  返回设计器初始化载荷，组合模板状态、当前画布结构和模板范围组件面板。
- `PUT /api/v1/portal/designer/templates/{templateId}/draft`
  保存设计器草稿画布，并返回刷新后的初始化载荷。
- `PUT /api/v1/portal/designer/templates/{templateId}/publish`
  发起指定版本的模板发布，并返回刷新后的设计器状态摘要。
- `PUT /api/v1/portal/designer/templates/{templateId}/publications/{publicationId}/activate`
  触发指定发布标识的线上生效，并返回刷新后的设计器状态摘要。
- `POST /api/v1/portal/designer/templates/{templateId}/publications/{publicationId}/offline`
  触发指定发布标识的线上下线，并返回刷新后的设计器状态摘要。
- `GET /api/v1/portal/designer/templates/{templateId}/preview`
  基于当前草稿画布返回预览页面视图，可按 `clientType` 指定预览端上下文。

当前设计器已支持最小草稿保存、模板发布、发布生效、下线与草稿预览边界，但仍不在本阶段引入发布审批或多人协作副作用。

## API 定位

`portal-designer` API 是设计器工具层的交互合同，用于加载草稿、保存画布、执行结构校验、预览和发起发布。设计器不拥有独立持久化真相源，所有保存和发布都必须回写 `portal-model`。

## 接口分组

| 分组 | 资源/对象 | 合同要求 |
|------|-----------|----------|
| 设计器初始化 | 草稿模板、页面树、区域树 | 加载指定模板草稿版本及其画布结构。 |
| 组件面板查询 | 卡片定义、组件分类 | 从 `widget-config` 获取组件面板所需卡片元数据和属性 schema。 |
| 画布保存 | 草稿布局与布置结果 | 保存页面、区域、卡片布置和基础样式，要求带版本号做并发控制。 |
| 结构校验与预览 | 预览上下文 | 在发布前校验默认页、布局模式、必保留元素，并支持 PC/移动端预览。 |
| 发布确认 | 发布命令 | 通过 `portal-model` 发起发布，不在设计器层直接切换线上状态。 |

## 边界口径

- 设计器只是一层工具交互，不建设模板主表、发布表或快照表。
- 模板发布归 `portal-model`，读模型归 `aggregation-api`，卡片定义归 `widget-config`。
- 设计器预览只校验模板渲染效果，不改写个性化配置或业务主数据。
- 一期不支持脚本、资源文件、代码型组件或多人实时协同编辑接口。

## 分页与筛选约定

| 查询对象 | 分页参数 | 主要筛选条件 |
|----------|----------|--------------|
| 模板草稿列表 | `pageNo`、`pageSize` | `sceneType`、`status`、`keyword` |
| 组件面板列表 | `pageNo`、`pageSize` | `category`、`renderType`、`status` |
| 页面树/区域树 | 不分页 | `portalVersionId`、`pageType` |

## 缓存、新鲜度、权限与审计约束

| 主题 | 合同要求 |
|------|----------|
| 缓存 | 草稿加载可短时缓存，但保存成功后必须立即刷新当前画布视图。 |
| 新鲜度 | 预览必须基于最近一次成功保存的草稿内容，不允许预览旧版本。 |
| 权限 | 仅门户管理员/设计管理员可进入设计器并执行保存、预览、发布。 |
| 审计 | 草稿保存、发布发起、预览确认等关键操作需要记录审计日志。 |

## 错误场景

| 场景 | 处理要求 |
|------|----------|
| 试图编辑已发布版本 | 拒绝保存，要求复制或新建草稿版本。 |
| 画布结构非法 | 拒绝保存，例如默认页缺失、区域编码重复、布局模式不匹配。 |
| 引用的卡片定义已停用 | 拒绝保存或预览，提示修复组件引用。 |
| 并发编辑导致草稿版本冲突 | 返回版本冲突错误，要求重新加载最新草稿。 |
| 预览客户端与模板布局模式不匹配 | 返回预览校验错误。 |
## Incremental Delivery Notes

- `GET /api/v1/portal/designer/templates`
  Returns current-tenant designer template draft statuses and supports optional `sceneType` filtering.
- Current results are ordered by `templateCode` and then `templateId` for deterministic reads.
- `GET /api/v1/portal/designer/templates/{templateId}/publications`
  Returns only current-tenant publications for the requested template and supports optional `clientType` and `status` filters.
- Publication query results are ordered by `publicationId` for deterministic panel refreshes.
- `GET /api/v1/portal/designer/templates/{templateId}/preview`
  Supports optional `tenantId`, `personId`, `accountId`, `assignmentId`, and `positionId` preview-context parameters and returns the effective preview identity explicitly.
- Preview responses now include a dedicated `overlay` block with `status` (`applied`/`bypassed`), `baselinePublicationId`, `resolvedLivePublicationId`, and `reason`.
- Preview overlay lookup is read-only and identity-bound: the personalization profile is resolved from the explicit preview identity, and overlay rules are bypassed unless `profile.basePublicationId` matches the live publication resolved for that preview identity.
- `PUT /api/v1/portal/designer/templates/{templateId}/draft`, `PUT /api/v1/portal/designer/templates/{templateId}/publish`, and `GET /api/v1/portal/designer/templates/{templateId}/preview` now reuse portal-model widget-reference enforcement and return `BUSINESS_RULE_VIOLATION` when the draft canvas contains `REPAIR_REQUIRED` widget references.
- The shared validation message includes `widgetCode`, `placementCode`, `pageCode`, and `regionCode` for each blocked placement so the designer can guide targeted repair.
