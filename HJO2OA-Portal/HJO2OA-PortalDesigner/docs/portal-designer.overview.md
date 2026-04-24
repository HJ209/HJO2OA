# 4.6 门户设计器

## 模块定位

`portal-designer` 是门户模板的可视化设计工具层，负责管理员在草稿版本上进行布局编辑、卡片布置、属性调整和预览发布操作。它不单独持有模板真相源，所有设计结果都必须回写到 `portal-model`。

## 核心职责

- 提供门户设计画布、页面结构编辑、区域拆分和卡片拖拽布置能力。
- 提供组件面板、属性面板和多端预览入口。
- 对设计结果执行结构校验，并调用 `portal-model` 保存草稿和触发发布。
- 在设计阶段消费 `widget-config` 卡片定义，保证设计器与运行时卡片协议一致。
- 预览查询直接基于当前草稿画布组装页面视图，不依赖当前线上生效发布。

## 上下游边界

| 方向 | 模块 | 边界说明 |
|------|------|----------|
| 上游 | `portal-model` | 设计器只操作模板草稿和发布命令，不直接维护模板主表。 |
| 上游 | `widget-config` | 组件面板展示的是卡片定义，不复制卡片协议。 |
| 下游 | `portal-home` | 预览只验证渲染效果，不在设计器层产出独立运行时页面。 |
| 下游 | `personalization` | 不负责个性化编辑，个性化视图仅用于预览参考。 |
| 外部边界 | 脚本/资源体系 | 一期不支持脚本、资源文件、自定义代码型组件编排。 |
| 外部边界 | 领域事件 | 设计器自身不是领域真相源，不发布独立领域事件。 |

## 核心能力表

| 能力 | 说明 | 一期实现口径 |
|------|------|--------------|
| 设计器画布 | 编辑页面、区域树和卡片布置。 | 支持基础画布、拖拽和排序。 |
| 组件面板 | 展示可复用卡片、导航、快捷入口等设计组件。 | 组件来源以 `widget-config` 标准卡片为主。 |
| 属性编辑 | 编辑布局属性、布置属性和基础样式。 | 一期仅提供基础属性编辑，不支持高级脚本。 |
| 预览与校验 | 在 PC/移动视图下预览并校验模板结构。 | 重点校验默认页、布局模式、区域编码和必保留元素。 |
| 发布串联 | 从设计器发起草稿保存、发布确认和版本切换。 | 真正的发布状态流转仍由 `portal-model` 承担。 |

## 典型业务流

1. 管理员打开某个模板草稿版本进入设计器。
2. 在画布中编辑页面、区域和卡片布置，组件面板从 `widget-config` 读取可用卡片。
3. 属性面板修改布局、样式和布置级覆写参数，系统实时执行结构校验。
4. 保存后由 `portal-model` 生成或更新草稿版本。
5. 预览确认后调用 `portal-model` 发起发布，`portal-home` 使用发布结果渲染最终页面。

## 一期实现约束

- 设计器不单独持久化模板数据，不建设独立模板表或设计稿表替代 `portal-model`。
- 一期只支持基础画布、属性编辑、草稿保存和多端预览。
- 不支持脚本、资源文件、复杂多端联动和多人实时协同编辑。
- 设计器中的发布动作本质上是调用 `portal-model` 的发布接口，而不是自行改变线上状态。
## Incremental Delivery Notes

- Designer now exposes a local template draft list query backed by the template projection.
- The list query is tenant-aware and supports optional `sceneType` filtering for list-page hydration.
- Designer publication panel queries now reuse the current-tenant portal publication read path and additionally constrain results to the requested template.
- Publication panel reads support optional `clientType` and `status` filters and preserve deterministic ordering by `publicationId`.
- Preview queries can now override `tenantId`, `personId`, `accountId`, `assignmentId`, and `positionId` as a synthetic identity context without mutating draft or personalization state.
- Preview assembly now resolves personalization overlays against the explicit preview identity instead of the current session, and returns a separate overlay-status block that explains whether rules were applied or bypassed.
- Preview overlay application is aligned with live semantics: rules only apply when the profile baseline publication matches the live publication resolved for the preview identity, while draft save-validation remains unchanged.
- Save, publish, and preview now share the same widget-reference guard from `portal-model`; drafts that still reference `REPAIR_REQUIRED` widgets are blocked before mutation or preview assembly.
- Blocked operations surface stable business validation errors that enumerate the affected `widgetCode`, `placementCode`, `pageCode`, and `regionCode`.
