# 4.1 门户页面模型管理

## 模块定位

`portal-model` 是 `04-门户与工作台` 的设计时真相源和模板发布真相源，负责门户模板、版本、页面、区域、卡片布置以及发布范围控制。它定义门户首页、办公中心和移动工作台的结构，不承担聚合读模型、业务主数据查询或前端渲染职责。

## 核心职责

- 管理 `PortalTemplate`、`PortalVersion`、`PortalPage`、`LayoutRegion`、`WidgetPlacement` 与 `PortalPublication`。
- 维护模板版本链、默认页面、布局模式、发布时间窗和发布状态。
- 维护模板可见范围、优先级裁决和 `sceneType + clientType` 维度的生效口径。
- 为 `portal-designer` 提供草稿编辑宿主，为 `personalization`、`portal-home`、`aggregation-api` 提供结构真相源。

## 上下游边界

| 方向 | 模块 | 边界说明 |
|------|------|----------|
| 上游 | `portal-designer` | 设计器只操作草稿与发布命令，不单独持久化模板主数据。 |
| 上游 | `widget-config` | 只引用 `WidgetDefinition`，不复制卡片定义、数据协议和展示策略。 |
| 下游 | `personalization` | 仅基于已生效发布模板做有限覆写，不反向修改模板。 |
| 下游 | `portal-home` | 仅消费模板解析结果用于页面渲染，不拥有模板真相源。 |
| 下游 | `aggregation-api` | 根据模板中的卡片布置关系计算应聚合的卡片集合。 |
| 外部边界 | `02/03/05/06/07` | 不拥有待办、消息、内容、统计等业务主数据，也不提供平台级开放 API。 |

## 核心能力表

| 能力 | 说明 | 一期实现口径 |
|------|------|--------------|
| 模板管理 | 管理模板编码、场景、分类、默认页和生命周期状态。 | 支持企业门户、部门/专题门户、个人工作台、办公中心、移动工作台场景。 |
| 版本管理 | 管理草稿、发布、废弃和历史版本链。 | 已发布版本不可原地修改，只能基于新草稿版本调整。 |
| 页面与区域建模 | 定义页面、区域树、布局属性和必保留区域。 | 布局模式限定为 `THREE_SECTION`、`OFFICE_SPLIT`、`MOBILE_LIGHT`、`CUSTOM`。 |
| 卡片布置 | 在区域中布置卡片，声明排序、默认显隐、默认折叠和局部覆写。 | 支持 `overrideProps`，但卡片定义仍归 `widget-config`。 |
| 模板发布 | 发布具体 `PortalVersion`，配置 `clientType`、时间窗和状态流转。 | 一期支持立即发布、定时发布和显式范围灰度。 |
| 范围与优先级裁决 | 按人、岗位、角色、部门、组织、全局解析最终生效模板。 | 主体优先级遵循 `PERSON > POSITION > ROLE > DEPARTMENT > ORGANIZATION > GLOBAL`。 |

## 典型业务流

1. 管理员创建门户模板并生成首个草稿版本。
2. 在模板下配置页面、区域和卡片布置，引用 `widget-config` 中的卡片定义。
3. 通过管理页或 `portal-designer` 保存草稿，并校验默认页、布局模式、区域编码和布置唯一性。
4. 发布某个 `PortalVersion`，配置 `sceneType`、`clientType`、时间窗、优先级和受众范围。
5. 发布生效后，`personalization`、`portal-home`、`aggregation-api` 以该版本作为解析、渲染和聚合基础。

## 一期实现约束

- 模板真相源和发布真相源都归 `portal-model`，不得拆到 `portal-home` 或 `portal-designer`。
- 不建设独立运行时页面快照表替代版本模型。
- 不支持复杂百分比灰度、AB 实验和多端深度协同模板。
- 下线发布只改变发布状态，不删除模板、版本和历史审计记录。
