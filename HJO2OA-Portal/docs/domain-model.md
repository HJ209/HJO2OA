# 04-门户与工作台 领域模型

## 1. 文档目的

本文档细化 `04-门户与工作台` 模块的核心领域实体、值对象、聚合根、实体关系和业务规则，作为后端工程建模、数据库设计、接口契约和聚合读模型设计的统一依据。

对应架构决策编号：D05（`04` 与 `07` 数据边界）、D08（身份上下文）、D15（门户聚合策略）、D20（一期实施范围）。

## 2. 领域定位与边界

### 2.1 领域职责

`04-门户与工作台` 负责系统入口、办公中心和用户侧聚合体验，核心职责包括：

- 定义门户模板、页面、区域、卡片装配和发布可见范围
- 为不同组织、角色、岗位和用户提供差异化首页模板与工作台布局
- 承担待办、消息、公告、日程、统计图卡、常用流程等卡片的用户侧聚合读模型
- 管理用户个性化配置，如卡片排序、隐藏规则、快捷入口和主题偏好
- 为门户首页、办公中心和移动工作台提供统一渲染协议与跳转协议

### 2.2 关键边界

- **门户拥有入口体验，不拥有业务主数据**：待办归 `02-todo-center`，消息归 `06-message-center`，内容归 `03`，业务统计主数据归对应业务域。
- **聚合接口不是通用数据开放接口**：`04-aggregation-api` 仅服务门户首页和办公中心场景，不承担对外开放 API 和第三方集成职责。
- **个性化不是重新造模板**：用户个性化只允许在授权范围内覆写模板布局和卡片顺序，不得突破模板强约束和发布范围。
- **门户首页不是设计器真相源**：`portal-home` 和 `portal-designer` 是交互/渲染层，领域真相源在 `portal-model`、`widget-config`、`personalization` 和 `aggregation-api`。
- **移动工作台不是独立门户底座**：移动端只消费 `04` 已定义的模板和聚合协议，不重复建设一套独立模型。

### 2.3 一期收敛口径

一期最小闭环优先实现以下能力：

- `portal-model`：门户模板、页面、区域、发布与可见范围
- `widget-config`：基础卡片定义、数据源绑定、展示协议
- `aggregation-api`：待办、消息、公告、快捷入口和基础统计卡片聚合
- `portal-home`：门户首页和办公中心渲染

一期简化或后置：

- `personalization`：先支持基础卡片排序、常用应用和主题偏好，不做复杂模板分叉
- `portal-designer`：先支持基础画布和属性编辑，不做复杂脚本与资源编排
- 高级多端模板联动、复杂灰度发布和深度看板能力后置

## 3. 领域总览

### 3.1 聚合根

| 聚合根 | 说明 | 目录 |
|--------|------|------|
| PortalTemplate | 门户模板与版本化门户模型，承载门户/办公中心的设计真相源 | `portal-model/` |
| WidgetDefinition | 卡片组件定义，负责类型、数据源、展示协议和可见规则 | `widget-config/` |
| PortalPublication | 模板发布与范围控制，决定谁在什么时间看到哪一版门户 | `portal-model/` |
| PersonalizationProfile | 用户个性化配置，承载卡片排序、隐藏规则、快捷入口和主题偏好 | `personalization/` |
| AggregationCardSnapshot | 面向用户身份上下文的卡片聚合快照，是门户读模型缓存真相源 | `aggregation-api/` |

### 3.2 非聚合子领域说明

以下子模块不单独拥有核心聚合根，而是消费或操作上述聚合：

| 子模块 | 角色 | 说明 |
|--------|------|------|
| `portal-home/` | 渲染层 | 消费模板、发布和快照数据进行首页/办公中心渲染 |
| `portal-designer/` | 设计器工具层 | 操作 `PortalTemplate` 草稿、页面和区域配置，不单独持有领域真相源 |

### 3.3 核心实体关系

```text
PortalTemplate      ──1:N──> PortalVersion            (模板版本链)
PortalVersion       ──1:N──> PortalPage               (版本 -> 页面)
PortalPage          ──1:N──> LayoutRegion             (页面 -> 区域)
LayoutRegion        ──1:N──> WidgetPlacement          (区域 -> 卡片布置)
WidgetPlacement     ──M:1──> WidgetDefinition         (区域中引用卡片定义)
PortalPublication   ──M:1──> PortalTemplate           (发布某个模板)
PortalPublication   ──M:1──> PortalVersion            (发布某个版本)
PortalPublication   ──1:N──> PortalAudienceBinding    (发布可见范围)
PersonalizationProfile ──M:1──> PortalPublication     (用户个性化基于生效发布模板)
PersonalizationProfile ──1:N──> QuickAccessEntry      (快捷入口)
AggregationCardSnapshot ──M:1──> WidgetDefinition     (聚合快照对应某种卡片)
AggregationCardSnapshot ──M:1──> PersonalizationProfile (个性化视图下的快照输出)
```

### 3.4 核心业务流

```text
门户模板设计
  -> 发布某个模板版本
  -> 根据组织/角色/岗位/用户范围生成有效发布
  -> 用户登录并计算身份上下文
  -> 解析当前用户有效模板
  -> 加载个性化覆写
  -> 查询/刷新 AggregationCardSnapshot
  -> portal-home 渲染首页或办公中心
```

## 4. 核心聚合定义

### 4.1 PortalTemplate（门户模板）

门户模板是门户和工作台体验的设计时真相源，支持版本化、页面装配、区域布局和卡片布置。

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | UUID | PK | 模板唯一标识 |
| code | String(64) | UK, NOT NULL | 模板编码，租户内唯一 |
| name | String(128) | NOT NULL | 模板名称 |
| sceneType | Enum | NOT NULL | 场景类型：ENTERPRISE_HOME / DEPARTMENT_HOME / TOPIC_HOME / PERSONAL_WORKBENCH / OFFICE_CENTER / MOBILE_WORKBENCH |
| category | Enum | NOT NULL | 模板类别：DEFAULT / ROLE_BASED / ORG_BASED / TOPIC / MOBILE |
| description | String(512) | NULLABLE | 模板说明 |
| defaultPageCode | String(64) | NOT NULL | 默认首页页面编码 |
| latestDraftVersion | Integer | NULLABLE | 最新草稿版本号 |
| latestPublishedVersion | Integer | NULLABLE | 最新发布版本号 |
| status | Enum | NOT NULL, DEFAULT ACTIVE | 状态：ACTIVE / DISABLED / ARCHIVED |
| tenantId | UUID | FK -> Tenant.id, NOT NULL | 所属租户 |
| createdAt | Timestamp | NOT NULL | |
| updatedAt | Timestamp | NOT NULL | |

#### 关联实体：PortalVersion（模板版本）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | UUID | PK | |
| portalTemplateId | UUID | FK -> PortalTemplate.id, NOT NULL | 所属模板 |
| versionNo | Integer | NOT NULL | 版本号 |
| status | Enum | NOT NULL | DRAFT / PUBLISHED / DEPRECATED |
| layoutMode | Enum | NOT NULL | THREE_SECTION / OFFICE_SPLIT / MOBILE_LIGHT / CUSTOM |
| schemaChecksum | String(128) | NULLABLE | 布局结构校验摘要 |
| publishedAt | Timestamp | NULLABLE | 发布时间 |
| publishedBy | UUID | FK -> Person.id, NULLABLE | 发布人 |
| createdAt | Timestamp | NOT NULL | |
| updatedAt | Timestamp | NOT NULL | |

#### 关联实体：PortalPage（门户页面）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | UUID | PK | |
| portalVersionId | UUID | FK -> PortalVersion.id, NOT NULL | 所属版本 |
| pageCode | String(64) | NOT NULL | 页面编码，版本内唯一 |
| name | String(128) | NOT NULL | 页面名称 |
| pageType | Enum | NOT NULL | HOME / OFFICE_CENTER / SUB_PAGE / MOBILE_PAGE / TOPIC_PAGE |
| routePath | String(256) | NOT NULL | 页面路由 |
| sortOrder | Integer | NOT NULL, DEFAULT 0 | 页面排序 |
| visible | Boolean | NOT NULL, DEFAULT TRUE | 默认可见 |

#### 关联实体：LayoutRegion（布局区域）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | UUID | PK | |
| pageId | UUID | FK -> PortalPage.id, NOT NULL | 所属页面 |
| regionCode | String(64) | NOT NULL | 区域编码，页面内唯一 |
| regionType | Enum | NOT NULL | HEADER / NAV / MAIN / ASIDE / FOOTER / PANEL / TAB / CUSTOM |
| parentRegionId | UUID | FK -> LayoutRegion.id, NULLABLE | 父区域 |
| sortOrder | Integer | NOT NULL, DEFAULT 0 | 排序 |
| collapsible | Boolean | NOT NULL, DEFAULT FALSE | 是否可折叠 |
| required | Boolean | NOT NULL, DEFAULT FALSE | 是否为必保留区域 |
| layoutProps | JSON | NULLABLE | 区域布局配置 |

#### 关联实体：WidgetPlacement（卡片布置）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | UUID | PK | |
| regionId | UUID | FK -> LayoutRegion.id, NOT NULL | 所属区域 |
| widgetDefinitionId | UUID | FK -> WidgetDefinition.id, NOT NULL | 引用卡片定义 |
| placementCode | String(64) | NOT NULL | 布置编码，区域内唯一 |
| sortOrder | Integer | NOT NULL, DEFAULT 0 | 排序 |
| required | Boolean | NOT NULL, DEFAULT FALSE | 是否为模板强制卡片 |
| defaultVisible | Boolean | NOT NULL, DEFAULT TRUE | 默认可见 |
| defaultCollapsed | Boolean | NOT NULL, DEFAULT FALSE | 默认折叠 |
| overrideProps | JSON | NULLABLE | 布置级展示覆写 |

**UK 约束**：

- `PortalTemplate.code` 租户内唯一
- `PortalVersion`：`(portalTemplateId, versionNo)`
- `PortalPage`：`(portalVersionId, pageCode)`
- `LayoutRegion`：`(pageId, regionCode)`
- `WidgetPlacement`：`(regionId, placementCode)`

**业务规则**：

- 门户模板采用版本链管理，已发布版本不可原地修改，修改必须基于新草稿版本。
- `sceneType=PERSONAL_WORKBENCH` 与 `sceneType=OFFICE_CENTER` 可共用模板根，但页面类型和布局模式可不同。
- `required=TRUE` 的区域与卡片不能被普通用户个性化删除，只能在模板层调整。
- `THREE_SECTION` 用于门户首页，`OFFICE_SPLIT` 用于办公中心，`MOBILE_LIGHT` 用于移动端工作台；模板必须显式声明布局模式。
- `portal-home` 与 `portal-designer` 不单独持久化模板数据，只操作本聚合产生的草稿与版本。

**领域事件**：

| 事件类型 | 触发时机 | 载荷 |
|----------|----------|------|
| `portal.template.created` | 模板创建 | templateId, code, sceneType |
| `portal.template.published` | 模板版本发布 | templateId, versionNo, sceneType |
| `portal.template.deprecated` | 旧版本废弃 | templateId, versionNo |

### 4.2 WidgetDefinition（卡片定义）

卡片定义负责描述门户和工作台中的可配置组件，包括数据来源、展示协议、刷新方式和可见规则。

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | UUID | PK | 卡片定义唯一标识 |
| code | String(64) | UK, NOT NULL | 卡片编码 |
| name | String(128) | NOT NULL | 卡片名称 |
| category | Enum | NOT NULL | TODO / MESSAGE / CONTENT / SCHEDULE / MEETING / DASHBOARD / QUICK_ACCESS / LINK / CUSTOM |
| renderType | Enum | NOT NULL | CARD / LIST / CHART / BANNER / GRID / SHORTCUT_GROUP |
| dataSourceType | Enum | NOT NULL | AGGREGATION_QUERY / STATIC_CONFIG / SHORTCUT / EXTERNAL_LINK |
| sourceModule | String(64) | NULLABLE | 来源模块，如 `todo-center`、`message-center` |
| permissionMode | Enum | NOT NULL | INHERIT_PORTAL / CUSTOM_SCOPE |
| refreshMode | Enum | NOT NULL | EVENT_DRIVEN / POLLING / MANUAL / ON_LOAD |
| cacheTtlSeconds | Integer | NULLABLE | 建议缓存 TTL |
| status | Enum | NOT NULL, DEFAULT ACTIVE | ACTIVE / DISABLED / DEPRECATED |
| tenantId | UUID | FK -> Tenant.id, NOT NULL | 所属租户 |
| createdAt | Timestamp | NOT NULL | |
| updatedAt | Timestamp | NOT NULL | |

#### 关联实体：WidgetDataSourceBinding（卡片数据源绑定）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | UUID | PK | |
| widgetDefinitionId | UUID | FK -> WidgetDefinition.id, NOT NULL | 所属卡片 |
| queryCode | String(64) | NULLABLE | 查询编码 |
| sourceResourceCode | String(128) | NULLABLE | 来源资源或查询标识 |
| requestSchema | JSON | NULLABLE | 请求参数协议 |
| responseSchema | JSON | NULLABLE | 响应协议 |
| fieldMapping | JSON | NULLABLE | 字段映射 |
| fallbackPolicy | JSON | NULLABLE | 回退策略 |

#### 关联实体：WidgetDisplayPolicy（卡片展示策略）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | UUID | PK | |
| widgetDefinitionId | UUID | FK -> WidgetDefinition.id, NOT NULL | 所属卡片 |
| minWidth | Integer | NULLABLE | 最小宽度 |
| maxItems | Integer | NULLABLE | 默认最大项数 |
| allowCollapse | Boolean | NOT NULL, DEFAULT TRUE | 是否允许折叠 |
| allowHide | Boolean | NOT NULL, DEFAULT TRUE | 是否允许隐藏 |
| styleConfig | JSON | NULLABLE | 样式配置 |

**业务规则**：

- 卡片定义只拥有展示协议和数据协议，不拥有待办、消息、公告、日程等源数据主模型。
- `dataSourceType=AGGREGATION_QUERY` 的卡片必须通过 `aggregation-api` 输出协议访问，不允许直接越过门户聚合层访问业务数据库。
- `refreshMode=EVENT_DRIVEN` 的卡片优先依赖业务事件触发缓存失效；一期不强制要求 WebSocket 实时推送。
- 卡片级 `allowHide=FALSE` 时，用户个性化不可将其隐藏。
- 同一 `WidgetDefinition` 可被多个模板、多个区域复用，布置级差异放在 `WidgetPlacement.overrideProps` 中。

**领域事件**：

| 事件类型 | 触发时机 | 载荷 |
|----------|----------|------|
| `portal.widget.updated` | 卡片定义或展示协议更新 | widgetId, code, changedFields |
| `portal.widget.disabled` | 卡片停用 | widgetId, code |

### 4.3 PortalPublication（模板发布）

模板发布负责决定“谁在什么时间看到哪一版门户模板”，是门户差异化和角色化配置的关键聚合。

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | UUID | PK | 发布唯一标识 |
| portalTemplateId | UUID | FK -> PortalTemplate.id, NOT NULL | 发布的模板 |
| portalVersionId | UUID | FK -> PortalVersion.id, NOT NULL | 发布的模板版本 |
| sceneType | Enum | NOT NULL | 对应门户场景 |
| clientType | Enum | NOT NULL | PC / MOBILE / ALL |
| publishMode | Enum | NOT NULL | IMMEDIATE / SCHEDULED / GRAY |
| priority | Integer | NOT NULL, DEFAULT 0 | 优先级，数值越大越优先 |
| startAt | Timestamp | NULLABLE | 生效开始时间 |
| endAt | Timestamp | NULLABLE | 生效结束时间 |
| status | Enum | NOT NULL | DRAFT / SCHEDULED / ACTIVE / OFFLINE / EXPIRED |
| tenantId | UUID | FK -> Tenant.id, NOT NULL | 所属租户 |
| createdAt | Timestamp | NOT NULL | |
| updatedAt | Timestamp | NOT NULL | |

#### 关联实体：PortalAudienceBinding（发布范围绑定）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | UUID | PK | |
| publicationId | UUID | FK -> PortalPublication.id, NOT NULL | 所属发布 |
| subjectType | Enum | NOT NULL | GLOBAL / ORGANIZATION / DEPARTMENT / POSITION / ROLE / PERSON |
| subjectId | UUID | NULLABLE | 主体 ID；GLOBAL 时为空 |
| effect | Enum | NOT NULL, DEFAULT ALLOW | ALLOW / DENY |
| sortOrder | Integer | NOT NULL, DEFAULT 0 | 规则顺序 |

**业务规则**：

- 发布的是某个具体模板版本，不是模板抽象本身。
- 同一 `sceneType + clientType` 下，用户最终只能解析出一个**有效模板发布**；冲突时按“主体优先级 > 显式优先级 > 发布时间”裁决。
- 建议的主体优先级为：`PERSON > POSITION > ROLE > DEPARTMENT > ORGANIZATION > GLOBAL`。
- `DENY` 规则优先于 `ALLOW` 规则，用于排除某些特殊人群。
- `SCHEDULED` 到时后自动转为 `ACTIVE`；超过 `endAt` 后自动转为 `EXPIRED`。
- 一期不实现复杂灰度百分比发布，`GRAY` 仅表示带显式范围的灰度模板发布。

**领域事件**：

| 事件类型 | 触发时机 | 载荷 |
|----------|----------|------|
| `portal.publication.activated` | 发布生效 | publicationId, templateId, sceneType, clientType |
| `portal.publication.offlined` | 发布下线 | publicationId, templateId |

### 4.4 PersonalizationProfile（个性化配置）

个性化配置用于在模板允许范围内保存用户的工作台偏好、快捷入口和显示微调。

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | UUID | PK | 个性化配置唯一标识 |
| personId | UUID | FK -> Person.id, NOT NULL | 用户 |
| assignmentId | UUID | FK -> Assignment.id, NULLABLE | 任职维度个性化；为空表示用户全局 |
| sceneType | Enum | NOT NULL | PERSONAL_WORKBENCH / OFFICE_CENTER / MOBILE_WORKBENCH |
| basePublicationId | UUID | FK -> PortalPublication.id, NOT NULL | 基于哪个生效发布模板 |
| themeCode | String(64) | NULLABLE | 主题编码 |
| layoutOverride | JSON | NULLABLE | 布局覆写 |
| widgetOrderOverride | JSON | NULLABLE | 卡片排序覆写 |
| hiddenPlacementCodes | JSON | NULLABLE | 被隐藏的布置编码列表 |
| status | Enum | NOT NULL, DEFAULT ACTIVE | ACTIVE / RESET / LOCKED |
| lastResolvedAt | Timestamp | NULLABLE | 最近解析时间 |
| tenantId | UUID | FK -> Tenant.id, NOT NULL | 所属租户 |
| createdAt | Timestamp | NOT NULL | |
| updatedAt | Timestamp | NOT NULL | |

#### 关联实体：QuickAccessEntry（快捷入口）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | UUID | PK | |
| personalizationProfileId | UUID | FK -> PersonalizationProfile.id, NOT NULL | 所属个性化配置 |
| entryType | Enum | NOT NULL | PROCESS / APP / LINK / CONTENT |
| targetCode | String(128) | NOT NULL | 目标编码 |
| targetLink | String(512) | NULLABLE | 跳转链接 |
| icon | String(128) | NULLABLE | 图标 |
| sortOrder | Integer | NOT NULL, DEFAULT 0 | 排序 |
| pinned | Boolean | NOT NULL, DEFAULT FALSE | 是否置顶 |

**业务规则**：

- 个性化配置的真相源是“用户在授权范围内对模板的覆写”，而不是复制出一份新模板。
- 若存在 `assignmentId` 级个性化，则优先于用户全局个性化；身份切换后可解析到不同的个性化视图。
- `required=TRUE` 的模板卡片不允许被隐藏；只允许调整顺序或折叠状态。
- `status=RESET` 表示用户已清空个性化覆写，渲染时回退到 `basePublicationId` 的默认模板配置。
- 快捷入口属于个性化配置的一部分，不构成独立业务应用目录真相源。

**领域事件**：

| 事件类型 | 触发时机 | 载荷 |
|----------|----------|------|
| `portal.personalization.saved` | 个性化保存 | profileId, personId, sceneType |
| `portal.personalization.reset` | 个性化重置 | profileId, personId, sceneType |

### 4.5 AggregationCardSnapshot（聚合卡片快照）

聚合卡片快照是门户用户侧读模型缓存真相源，保存某个用户/身份上下文/场景下某类卡片的聚合结果。

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | UUID | PK | 快照唯一标识 |
| snapshotKey | String(256) | UK, NOT NULL | 快照键，典型格式 `portal:{tenant}:{person}:{assignment}:{scene}:{card}` |
| personId | UUID | FK -> Person.id, NOT NULL | 用户 |
| assignmentId | UUID | FK -> Assignment.id, NULLABLE | 当前任职关系 |
| positionId | UUID | FK -> Position.id, NULLABLE | 当前岗位 |
| sceneType | Enum | NOT NULL | PERSONAL_WORKBENCH / OFFICE_CENTER / MOBILE_WORKBENCH |
| widgetDefinitionId | UUID | FK -> WidgetDefinition.id, NOT NULL | 对应卡片定义 |
| cardType | Enum | NOT NULL | TODO / MESSAGE / ANNOUNCEMENT / CONTENT / DASHBOARD / QUICK_ACCESS / SCHEDULE / MEETING |
| payload | JSON | NOT NULL | 聚合后的卡片数据 |
| sourceWatermark | JSON | NULLABLE | 上游数据版本/时间戳标记 |
| cacheStatus | Enum | NOT NULL | WARMING / READY / STALE / FAILED |
| lastRefreshAt | Timestamp | NULLABLE | 最近刷新时间 |
| expiresAt | Timestamp | NULLABLE | 过期时间 |
| tenantId | UUID | FK -> Tenant.id, NOT NULL | 所属租户 |
| createdAt | Timestamp | NOT NULL | |
| updatedAt | Timestamp | NOT NULL | |

**业务规则**：

- `AggregationCardSnapshot` 拥有的是**用户侧聚合输出**，不是待办、消息、公告等源数据所有权。
- 缓存键必须包含身份上下文（至少 `personId + assignmentId/positionId + sceneType + cardType`），否则身份切换后会出现读模型串用。
- `cacheStatus=STALE` 表示快照可返回但应触发异步刷新；`FAILED` 表示上次刷新失败但不能阻塞门户首页整体渲染。
- 一期默认采用 Redis 级缓存与异步失效策略，不要求所有卡片实时推送刷新。
- 聚合卡片只能通过 `aggregation-api` 对外输出，不得让前端直接拼装多个上游模块接口绕过聚合层。

**领域事件**：

| 事件类型 | 触发时机 | 载荷 |
|----------|----------|------|
| `portal.snapshot.refreshed` | 卡片快照刷新成功 | snapshotKey, cardType, refreshedAt |
| `portal.snapshot.failed` | 卡片快照刷新失败 | snapshotKey, cardType, reason |

## 5. 关键规则与解析策略

### 5.1 生效模板解析顺序

用户登录或切换身份后，门户模板按以下顺序解析：

1. 获取当前 `sceneType` 和 `clientType`
2. 查找命中 `PERSON` 的活动发布
3. 查找命中 `POSITION` 的活动发布
4. 查找命中 `ROLE` 的活动发布
5. 查找命中 `DEPARTMENT` 的活动发布
6. 查找命中 `ORGANIZATION` 的活动发布
7. 回退到 `GLOBAL` 默认发布
8. 再叠加 `PersonalizationProfile`

### 5.2 个性化覆盖原则

- 模板强制卡片不可删除
- 模板必保留区域不可移除
- 允许用户对卡片顺序、折叠状态、快捷入口和部分样式做有限覆盖
- 超出模板授权边界的配置应在保存时被拒绝，而不是渲染时隐式忽略

### 5.3 发布与版本原则

- 模板发布的是 `PortalVersion`，不是运行时页面快照。
- 已发布版本不可直接修改，只能基于新版本再发布。
- 同一模板可以同时服务 PC 与移动端，但推荐独立页面类型和布局模式。
- 下线发布不删除模板和历史版本，只改变发布状态。

### 5.4 门户聚合边界原则

- `04` 只负责用户侧聚合读模型，不承担通用数据查询接口职责。
- `04` 可聚合 `02/03/05/06` 输出的数据，但不得反向要求这些模块按门户页面结构改造其主模型。
- 统计类卡片可展示聚合指标，但指标口径真相源仍属于业务域或 `07` 报表能力。

### 5.5 身份上下文集成规则

- 身份切换后，生效模板、个性化配置、卡片可见范围和聚合快照都必须重算。
- `assignmentId` 是门户和办公中心最重要的上下文隔离键之一。
- 门户消息卡片、待办卡片和组织相关卡片必须以当前身份上下文裁剪。
- 已保存的个性化配置不得越权显示当前身份不可见的卡片或数据。

## 6. 事件模型

### 6.1 `04` 内部领域事件

| 事件类型 | 载荷关键字段 | 说明 |
|----------|-------------|------|
| `portal.template.created` | templateId, code, sceneType | 模板创建 |
| `portal.template.published` | templateId, versionNo, sceneType | 模板发布 |
| `portal.template.deprecated` | templateId, versionNo | 模板版本废弃 |
| `portal.publication.activated` | publicationId, templateId, sceneType, clientType | 发布生效 |
| `portal.publication.offlined` | publicationId, templateId | 发布下线 |
| `portal.widget.updated` | widgetId, code, changedFields | 卡片定义变更 |
| `portal.widget.disabled` | widgetId, code | 卡片停用 |
| `portal.personalization.saved` | profileId, personId, sceneType | 个性化保存 |
| `portal.personalization.reset` | profileId, personId, sceneType | 个性化重置 |
| `portal.snapshot.refreshed` | snapshotKey, cardType, refreshedAt | 聚合快照刷新 |
| `portal.snapshot.failed` | snapshotKey, cardType, reason | 聚合快照刷新失败 |

### 6.2 跨模块总线事件

当前 Portal 域内实际传播的核心事件以各子模块 `events/*.events.md` 为准；本节按发布子模块收敛父级事件清单，是否需要继续对外暴露到统一总线由全局 contract 单独约束。`portal-home` 与 `portal-designer` 在一期只消费事件，不单独发布新的领域事件。

| 发布子模块 | 事件类型 | 主要消费者 | 说明 |
|------------|----------|------------|------|
| `portal-model` | `portal.template.created` / `portal.template.published` / `portal.template.deprecated` | `portal-designer` | 负责模板目录、版本时间线与预览基线状态刷新 |
| `portal-model` | `portal.publication.activated` / `portal.publication.offlined` | `personalization`、`aggregation-api`、`portal-home`、`portal-designer` | 负责生效模板绑定、页面壳重算和相关快照失效 |
| `widget-config` | `portal.widget.updated` / `portal.widget.disabled` | `portal-model`、`aggregation-api`、`portal-designer` | 负责卡片引用校验、禁用态提示与相关聚合快照失效 |
| `personalization` | `portal.personalization.saved` / `portal.personalization.reset` | `aggregation-api`、`portal-home` | 负责个性化视图重装配与用户侧快照失效 |
| `aggregation-api` | `portal.snapshot.refreshed` / `portal.snapshot.failed` | `portal-home` | 负责卡片局部刷新成功/失败的前端反馈与降级展示 |

### 6.3 入站业务事件

`04` 重点消费以下事件以刷新聚合快照和门户卡片：

| 事件类型 | 处理方式 | 说明 |
|----------|----------|------|
| `todo.item.created` | 失效待办卡片快照 | 一期主链路 |
| `todo.item.completed` | 刷新待办数量与列表 | 一期主链路 |
| `todo.item.overdue` | 刷新紧急待办卡片 | 一期主链路 |
| `process.instance.started` | 刷新常用流程/流程统计 | 一期可选 |
| `process.instance.completed` | 刷新流程统计卡片 | 一期可选 |
| `msg.notification.sent` | 刷新消息未读数 | 一期主链路 |
| `msg.notification.read` | 刷新消息卡片状态 | 一期主链路 |
| `org.identity.switched` | 失效该用户全部门户快照 | 一期主链路 |
| `org.identity-context.invalidated` | 失效该用户全部门户快照，并按回退身份重建或要求重新登录 | 一期主链路 |
| `content.article.published` | 刷新公告/内容卡片 | 二期主链路 |
| `biz.meeting.created` | 刷新会议提醒卡片 | 三期业务接入 |

## 7. 跨模块依赖

| 模块关系 | 依赖内容 | 说明 |
|----------|----------|------|
| `04 -> 00` | 事件总线、缓存、审计、错误码、配置中心 | 基础设施依赖 |
| `04 -> 01` | 身份上下文、组织/岗位/角色可见范围 | 模板解析和可见性裁剪依赖 |
| `04 -> 02` | 待办数量、流程统计、常用流程 | 通过聚合协议消费 |
| `04 -> 03` | 公告、内容、订阅更新 | 二期通过聚合协议消费 |
| `04 -> 05` | 会议、日程、任务、业务看板数据 | 业务卡片数据来源 |
| `04 -> 06` | 消息未读数、最新消息、移动工作台联动 | 一期主链路 |
| `04 -> 07` | 无通用开放能力依赖 | `07` 是平台数据开放层，不反向被 `04` 替代 |

## 8. 数据库设计建议

### 8.1 核心表清单

| 表名 | 对应聚合根/实体 | 说明 |
|------|-----------------|------|
| `portal_template` | PortalTemplate | 门户模板主表 |
| `portal_template_version` | PortalVersion | 模板版本表 |
| `portal_page` | PortalPage | 门户页面表 |
| `portal_layout_region` | LayoutRegion | 页面布局区域表 |
| `portal_widget_placement` | WidgetPlacement | 区域卡片布置表 |
| `portal_widget_def` | WidgetDefinition | 卡片定义表 |
| `portal_widget_data_source` | WidgetDataSourceBinding | 卡片数据源绑定表 |
| `portal_widget_display_policy` | WidgetDisplayPolicy | 卡片展示策略表 |
| `portal_publication` | PortalPublication | 模板发布表 |
| `portal_publication_audience` | PortalAudienceBinding | 发布范围绑定表 |
| `portal_personalization_profile` | PersonalizationProfile | 用户个性化配置表 |
| `portal_quick_access_entry` | QuickAccessEntry | 快捷入口表 |
| `portal_agg_snapshot` | AggregationCardSnapshot | 聚合卡片快照表 |

### 8.2 索引建议

- `portal_template`：`(tenantId, code)`、`(tenantId, sceneType, status)`
- `portal_template_version`：`(portalTemplateId, versionNo)`、`(portalTemplateId, status, publishedAt)`
- `portal_page`：`(portalVersionId, pageCode)`、`(portalVersionId, pageType, sortOrder)`
- `portal_layout_region`：`(pageId, regionCode)`、`(pageId, regionType, sortOrder)`
- `portal_widget_placement`：`(regionId, placementCode)`、`(widgetDefinitionId, sortOrder)`
- `portal_widget_def`：`(tenantId, code)`、`(tenantId, category, status)`
- `portal_publication`：`(tenantId, sceneType, clientType, status)`、`(portalTemplateId, portalVersionId)`、`(startAt, endAt)`
- `portal_publication_audience`：`(publicationId, subjectType, subjectId)`
- `portal_personalization_profile`：`(tenantId, personId, assignmentId, sceneType)`、`(basePublicationId)`
- `portal_quick_access_entry`：`(personalizationProfileId, sortOrder)`
- `portal_agg_snapshot`：`(snapshotKey)`、`(tenantId, personId, assignmentId, sceneType, cardType)`、`(cacheStatus, expiresAt)`

### 8.3 大字段与缓存建议

- `layoutProps`、`overrideProps`、`requestSchema`、`responseSchema`、`layoutOverride`、`payload` 等建议采用 JSON 文本或 `NVARCHAR(MAX)` 存储。
- `portal_agg_snapshot` 可持久化到数据库，同时在 Redis 中保留热点缓存，以支持失效与回源。
- 模板版本发布后，可为前端渲染输出一份压缩后的渲染协议快照，避免每次动态拼装完整结构。

## 9. 一期建模优先级建议

### 9.1 一期必须先落的聚合

- `PortalTemplate`
- `WidgetDefinition`
- `PortalPublication`
- `AggregationCardSnapshot`

### 9.2 一期可简化实现的部分

- `PersonalizationProfile` 先支持卡片排序、快捷入口和基础隐藏规则
- `PortalPublication` 先支持显式范围发布，不做复杂灰度百分比策略
- `WidgetDefinition` 先支持标准卡片协议，不支持复杂脚本卡片
- `portal-designer` 先操作模板草稿，不做资源文件、脚本和多端联动高级能力

### 9.3 后续可继续补强的方向

- 复杂门户专题页与多级导航模型
- 模板市场与模板复制审计
- 高级统计图卡与 `07-report` 深度联动
- WebSocket 精准刷新和多端同步状态
- 多品牌、多皮肤、多租户主题体系

## 10. 结论

`04-门户与工作台` 的核心不在“再做一个首页页面”，而在于建立一套**模板可版本化、发布可解析、个性化可约束、聚合读模型可治理**的用户入口模型。其建模重点应围绕以下原则展开：

- 用 `PortalTemplate` 管设计时真相源
- 用 `PortalPublication` 管范围、生效与优先级
- 用 `PersonalizationProfile` 管有限覆写，而不是复制模板
- 用 `AggregationCardSnapshot` 管用户侧聚合输出，而不是越权持有业务主数据
- 用 `WidgetDefinition` 统一卡片协议，连接门户布局与上游业务模块

该文档可直接作为后续 `HJO2OA-Portal/docs/module-design.md`、门户建表、聚合 API 细化和前后端协同的基础。
