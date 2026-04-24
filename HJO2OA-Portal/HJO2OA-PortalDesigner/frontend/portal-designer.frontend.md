# portal-designer 前端合同

## 页面清单

| 页面/区域 | 页面职责 | 使用人群 |
|-----------|----------|----------|
| 设计器主页面 | 承载页面结构编辑、区域拆分和卡片拖拽布置。 | 门户管理员、设计管理员 |
| 组件面板 | 展示可拖拽的卡片和设计组件。 | 门户管理员、设计管理员 |
| 属性面板 | 编辑区域、卡片布置和基础样式属性。 | 门户管理员、设计管理员 |
| 预览页/弹窗 | 展示 PC 与移动端预览效果。 | 门户管理员、设计管理员 |
| 发布确认弹窗 | 校验草稿并发起发布命令。 | 门户管理员 |

## 页面职责与关键交互

| 交互 | 说明 | 依赖模块 |
|------|------|----------|
| 拖拽组件入画布 | 从组件面板拖入卡片，生成区域布置。 | `widget-config` + `portal-model` |
| 调整区域结构 | 拆分区域、修改排序、设置必保留属性。 | `portal-model` |
| 编辑组件属性 | 修改布置级覆写参数和基础样式。 | `portal-model` |
| 多端预览 | 在 PC/移动端视角下校验布局效果。 | `portal-designer` + `portal-model` |
| 草稿保存与发布 | 保存当前画布并调用发布确认流程。 | `portal-model` |

- 预览查询默认读取最近一次保存成功的草稿画布，并返回可直接渲染的页面视图结构。

## 模板、卡片、个性化、移动端约束

- 模板约束：设计器编辑的是模板草稿，不是线上运行时页面；已发布版本不能直接编辑。
- 卡片约束：组件面板来源于 `widget-config`，设计器只能布置卡片，不能改写卡片协议和数据源定义。
- 个性化约束：设计器不编辑用户个性化数据，预览只反映模板默认结构或指定预览上下文。
- 移动端约束：移动预览必须基于同一模板体系下的移动页和 `MOBILE_LIGHT` 布局，不得独立建模。

## 身份切换影响

- 当前管理员身份切换会影响设计器可操作权限和可见模板范围，但不会改变正在编辑的草稿真相源。
- 若设计器支持按目标身份预览，切换预览身份只影响预览上下文，不写入模板或个性化数据。
- 身份切换后应清空旧身份下缓存的组件面板权限和发布状态提示。
## Incremental Delivery Notes

- The template draft list page can load `/api/v1/portal/designer/templates` and optionally filter by `sceneType`.
- Returned rows reuse the existing designer template status payload so the list can show version and publication indicators without extra joins.
- The publication panel can load `/api/v1/portal/designer/templates/{templateId}/publications` and optionally pass `clientType` and `status` together for narrowed record queries.
- Publication rows are scoped to the current tenant and current template only, and stay in deterministic `publicationId` order across refreshes.
- Preview identity switching can call the existing preview endpoint with `tenantId`, `personId`, `accountId`, `assignmentId`, and `positionId`, and the response now echoes the effective preview identity used for rendering.
- The preview payload now includes an `overlay` block so the UI can surface whether personalization rules were applied or bypassed, along with the baseline and resolved live publication ids.
- When previewing a draft, hide/order rules reuse the same source placement and widget identifiers as live assembly; if the explicit preview identity resolves to a different live publication baseline, the UI should treat the returned overlay status as an explicit bypass.
