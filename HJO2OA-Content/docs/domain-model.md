# 03-内容与知识 领域模型

## 1. 文档目的

本文档细化 `03-内容与知识` 模块的核心领域实体、值对象、聚合根、实体关系和业务规则，作为后端工程建模、数据库设计、检索索引设计和接口契约的统一依据。

对应架构决策编号：D03（`03` 与 `05` 模块边界）、D08（身份上下文）、D14（内容底座约束）、D20（一期实施范围）。

## 2. 领域定位与边界

### 2.1 领域职责

`03-内容与知识` 负责平台内所有内容型对象的统一底座，核心职责包括：

- 统一承载新闻、公告、制度、知识文档、专题内容等内容主模型
- 维护栏目、分类、标签、专题等语义组织结构和内容归属关系
- 承担稿件编辑、送审、发布、下线、归档的完整生命周期管理
- 管理正文、封面、附件引用、版本快照和内容间引用关系
- 提供发布范围、可见性校验、全文检索、收藏订阅和运营统计能力

### 2.2 关键边界

- **内容底座拥有内容真相源**：内容条目、版本、发布范围、订阅收藏、检索索引和互动统计归 `03`，`05` 不再自建公告、制度、知识、网盘文件等内容底层模型（D03、D14）。
- **附件二进制不归内容域持有**：`03` 只管理正文引用和附件绑定关系，文件元数据、对象存储和预览能力归 `00-attachment`。
- **审批引擎不归内容域持有**：送审、审核可接入 `02-流程与表单`，但审核结果解释、发布状态和归档状态仍由 `03` 维护。
- **内容域不负责用户触达**：门户卡片聚合归 `04`，通知发送和订阅触达归 `06`，`03` 只发布内容事件和提供查询能力。
- **搜索与统计是投影，不是主模型**：全文索引、推荐候选和热度排行是从已发布内容投影出的读模型，不能反向替代内容主数据。

### 2.3 一期收敛口径

平台一期最小闭环不以 `03` 为前提（D20），但为了后续场景接入，建模建议优先收敛以下能力：

- `category-management`：栏目、分类、标签、专题和可见范围骨架
- `content-lifecycle`：稿件、版本、送审、发布、归档
- `content-storage`：正文、封面、附件引用和版本快照
- `content-permission`：发布范围和可见性规则

一期简化或后置：

- `content-search` 先收敛基础全文检索、分类筛选和收藏订阅，不做复杂推荐算法
- `content-statistics` 先支持阅读、下载、收藏的基础统计，不做复杂运营漏斗和智能分析
- 多语言内容深度运营、复杂专题编排、知识图谱关系后置

## 3. 领域总览

### 3.1 聚合根

| 聚合根 | 说明 | 目录 |
|--------|------|------|
| ContentCategory | 栏目、分类、标签、专题及其层级与可见规则骨架 | `category-management/` |
| ContentArticle | 内容条目、正文、封面、附件引用与版本链 | `content-lifecycle/`、`content-storage/` |
| ContentPublication | 送审、审核、发布、下线、归档和可见范围 | `content-lifecycle/`、`content-permission/` |
| ContentSubscriptionProfile | 收藏、订阅目标、通知偏好和个人内容偏好 | `content-search/` |
| ContentEngagementSnapshot | 阅读、下载、收藏等互动累计与排行快照 | `content-statistics/` |

### 3.2 非聚合子领域说明

以下子模块不单独拥有新的业务真相源，而是围绕核心聚合构建投影或操作能力：

| 子模块 | 角色 | 说明 |
|--------|------|------|
| `content-search/` | 检索投影层 | 消费已发布内容构建全文索引、筛选索引和相关推荐候选 |
| `content-storage/` | 存储映射层 | 持久化正文快照、封面引用和附件绑定，但不拥有附件二进制 |
| `content-statistics/` | 运营投影层 | 消费阅读/下载/收藏行为事件，输出热度排行与发布效果分析 |

### 3.3 核心实体关系

```text
ContentCategory           ──1:N──> CategoryNode              (栏目/分类树)
ContentCategory           ──1:N──> CategoryPermissionRule    (栏目可见规则)
ContentCategory           ──1:N──> TagDefinition             (标签定义)
ContentCategory           ──1:N──> TopicDefinition           (专题定义)
ContentArticle            ──M:1──> CategoryNode              (主栏目归属)
ContentArticle            ──1:N──> ContentVersion            (稿件版本链)
ContentVersion            ──1:N──> ArticleAttachmentBinding  (版本附件绑定)
ContentVersion            ──1:N──> ArticleRelation           (引用/相关文章)
ContentPublication        ──M:1──> ContentArticle            (针对某篇内容的发布)
ContentPublication        ──1:N──> ReviewRecord             (审核记录)
ContentPublication        ──1:N──> PublicationScopeRule     (发布范围规则)
ContentSubscriptionProfile ──1:N──> SubscriptionTarget      (订阅目标)
ContentSubscriptionProfile ──1:N──> FavoriteRecord          (收藏记录)
ContentEngagementSnapshot ──M:1──> ContentArticle           (统计快照归属内容)
ContentEngagementSnapshot ──1:N──> ContentReadRecord        (原始互动记录)
```

### 3.4 核心业务流

```text
配置栏目/分类/标签/专题
  -> 创建内容稿件
  -> 编辑正文、封面、附件和版本
  -> 发起送审或直接发布
  -> 生成发布范围与可见规则
  -> 构建全文索引和订阅目标命中结果
  -> 用户阅读/下载/收藏
  -> 更新统计快照、热度排行和运营看板
```

## 4. 核心聚合定义

### 4.1 ContentCategory（内容分类骨架）

内容分类骨架负责统一管理栏目、分类、标签、专题及其权限规则，是内容组织结构的真相源。

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | UUID | PK | 分类骨架唯一标识 |
| code | String(64) | UK, NOT NULL | 分类编码，租户内唯一 |
| name | String(128) | NOT NULL | 名称 |
| categoryType | Enum | NOT NULL | COLUMN / CATEGORY / TAG_GROUP / TOPIC |
| parentId | UUID | FK -> ContentCategory.id, NULLABLE | 父节点 |
| routePath | String(256) | NULLABLE | 前端访问路径或专题路径 |
| sortOrder | Integer | NOT NULL, DEFAULT 0 | 排序 |
| visibleMode | Enum | NOT NULL | PUBLIC / SCOPE_CONTROLLED / PRIVATE |
| status | Enum | NOT NULL | DRAFT / ACTIVE / DISABLED / ARCHIVED |
| tenantId | UUID | FK -> Tenant.id, NOT NULL | 所属租户 |
| createdAt | Timestamp | NOT NULL | |
| updatedAt | Timestamp | NOT NULL | |

#### 关联实体：CategoryPermissionRule（分类权限规则）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | UUID | PK | |
| categoryId | UUID | FK -> ContentCategory.id, NOT NULL | 所属栏目/分类 |
| subjectType | Enum | NOT NULL | GLOBAL / ORGANIZATION / DEPARTMENT / POSITION / ROLE / PERSON |
| subjectId | UUID | NULLABLE | 主体 ID，GLOBAL 时为空 |
| effect | Enum | NOT NULL, DEFAULT ALLOW | ALLOW / DENY |
| scopeType | Enum | NOT NULL | VIEW / PUBLISH / MANAGE |
| sortOrder | Integer | NOT NULL, DEFAULT 0 | 规则顺序 |

#### 关联实体：TagDefinition（标签定义）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | UUID | PK | |
| categoryId | UUID | FK -> ContentCategory.id, NOT NULL | 所属标签组 |
| code | String(64) | NOT NULL | 标签编码，组内唯一 |
| name | String(128) | NOT NULL | 标签名称 |
| status | Enum | NOT NULL | ACTIVE / DISABLED |

#### 关联实体：TopicDefinition（专题定义）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | UUID | PK | |
| categoryId | UUID | FK -> ContentCategory.id, NOT NULL | 所属专题骨架 |
| code | String(64) | NOT NULL | 专题编码 |
| name | String(128) | NOT NULL | 专题名称 |
| coverAttachmentId | UUID | FK -> AttachmentAsset.id, NULLABLE | 专题封面 |
| description | String(512) | NULLABLE | 专题说明 |
| status | Enum | NOT NULL | ACTIVE / DISABLED / CLOSED |

**业务规则**：

- 栏目/分类树不得形成环；同级 `code` 必须唯一。
- 内容稿件必须归属一个主栏目，标签与专题只作为补充语义关系，不作为内容真相源。
- 分类权限规则支持 `VIEW / PUBLISH / MANAGE` 三类能力，显式 `DENY` 优先于 `ALLOW`。
- 标签和专题可停用，但停用后不回写删除历史内容上的标签快照，只阻止新稿件继续引用。

**领域事件**：

| 事件类型 | 触发时机 | 载荷 |
|----------|----------|------|
| `content.category.created` | 栏目/分类创建 | categoryId, categoryType, code |
| `content.category.updated` | 栏目/分类更新 | categoryId, changedFields |
| `content.category.disabled` | 栏目/分类停用 | categoryId, categoryType |

### 4.2 ContentArticle（内容条目）

内容条目是内容域的核心主模型，负责标题、摘要、正文版本、封面、附件引用与内容关系维护。

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | UUID | PK | 内容唯一标识 |
| articleNo | String(64) | UK, NOT NULL | 内容编号，租户内唯一 |
| title | String(256) | NOT NULL | 标题 |
| contentType | Enum | NOT NULL | NEWS / ANNOUNCEMENT / POLICY / KNOWLEDGE / TOPIC_ARTICLE / DOCUMENT |
| mainCategoryId | UUID | FK -> ContentCategory.id, NOT NULL | 主栏目 |
| authorId | UUID | FK -> Person.id, NOT NULL | 作者 |
| sourceType | Enum | NOT NULL | ORIGINAL / REPRINT / EXTERNAL |
| sourceUrl | String(512) | NULLABLE | 外部来源地址 |
| summary | String(1024) | NULLABLE | 摘要 |
| coverAttachmentId | UUID | FK -> AttachmentAsset.id, NULLABLE | 封面附件 |
| currentDraftVersionNo | Integer | NULLABLE | 当前草稿版本号 |
| currentPublishedVersionNo | Integer | NULLABLE | 当前已发布版本号 |
| status | Enum | NOT NULL | DRAFT / IN_REVIEW / PUBLISHED / OFFLINE / ARCHIVED |
| tenantId | UUID | FK -> Tenant.id, NOT NULL | 所属租户 |
| createdAt | Timestamp | NOT NULL | |
| updatedAt | Timestamp | NOT NULL | |

#### 关联实体：ContentVersion（内容版本）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | UUID | PK | |
| articleId | UUID | FK -> ContentArticle.id, NOT NULL | 所属稿件 |
| versionNo | Integer | NOT NULL | 版本号 |
| titleSnapshot | String(256) | NOT NULL | 标题快照 |
| summarySnapshot | String(1024) | NULLABLE | 摘要快照 |
| bodyFormat | Enum | NOT NULL | HTML / MARKDOWN / RICH_TEXT / PLAIN_TEXT |
| bodyRef | String(256) | NOT NULL | 正文存储引用 |
| bodyChecksum | String(128) | NULLABLE | 正文校验值 |
| editorId | UUID | FK -> Person.id, NOT NULL | 编辑人 |
| status | Enum | NOT NULL | DRAFT / REVIEWED / PUBLISHED / ARCHIVED |
| createdAt | Timestamp | NOT NULL | |

#### 关联实体：ArticleAttachmentBinding（附件绑定）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | UUID | PK | |
| articleVersionId | UUID | FK -> ContentVersion.id, NOT NULL | 所属版本 |
| attachmentId | UUID | FK -> AttachmentAsset.id, NOT NULL | 附件引用 |
| usageType | Enum | NOT NULL | BODY_ATTACHMENT / COVER / INLINE / APPENDIX |
| sortOrder | Integer | NOT NULL, DEFAULT 0 | 排序 |

#### 关联实体：ArticleRelation（内容关系）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | UUID | PK | |
| articleVersionId | UUID | FK -> ContentVersion.id, NOT NULL | 所属版本 |
| relationType | Enum | NOT NULL | RELATED / REFERENCE / REPRINT_FROM / ATTACHMENT_ENTRY |
| targetArticleId | UUID | FK -> ContentArticle.id, NULLABLE | 关联内容 |
| targetUrl | String(512) | NULLABLE | 外部引用地址 |

**业务规则**：

- 内容条目采用版本链管理，已发布版本不可原地编辑，修改必须基于新草稿版本。
- 正文、摘要、附件绑定与内容关系均绑定到具体版本，保证历史版本可回溯。
- `contentType` 决定默认模板与检索权重，但不改变统一内容主模型。
- 外部转载类内容必须保留 `sourceUrl` 或原文来源说明。
- 封面与附件使用 `00-attachment` 的引用 ID，内容域不复制存储附件二进制。

**领域事件**：

| 事件类型 | 触发时机 | 载荷 |
|----------|----------|------|
| `content.article.created` | 稿件创建 | articleId, contentType, mainCategoryId |
| `content.article.updated` | 稿件更新 | articleId, changedFields |
| `content.article.versioned` | 新版本生成 | articleId, versionNo |

### 4.3 ContentPublication（内容发布）

内容发布负责送审、审核、发布时间窗、下线和归档，是内容从“草稿”变成“可见内容”的关键聚合。

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | UUID | PK | 发布唯一标识 |
| articleId | UUID | FK -> ContentArticle.id, NOT NULL | 所属内容 |
| targetVersionNo | Integer | NOT NULL | 待发布版本号 |
| reviewMode | Enum | NOT NULL | DIRECT / MANUAL_REVIEW / WORKFLOW |
| reviewStatus | Enum | NOT NULL | DRAFT / SUBMITTED / APPROVED / REJECTED / WITHDRAWN |
| workflowInstanceId | UUID | NULLABLE | 关联流程实例 |
| publicationStatus | Enum | NOT NULL | DRAFT / SCHEDULED / PUBLISHED / OFFLINE / ARCHIVED |
| startAt | Timestamp | NULLABLE | 生效开始时间 |
| endAt | Timestamp | NULLABLE | 失效时间 |
| publishedAt | Timestamp | NULLABLE | 发布时间 |
| publishedBy | UUID | FK -> Person.id, NULLABLE | 发布人 |
| archivedAt | Timestamp | NULLABLE | 归档时间 |
| tenantId | UUID | FK -> Tenant.id, NOT NULL | 所属租户 |
| createdAt | Timestamp | NOT NULL | |
| updatedAt | Timestamp | NOT NULL | |

#### 关联实体：ReviewRecord（审核记录）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | UUID | PK | |
| publicationId | UUID | FK -> ContentPublication.id, NOT NULL | 所属发布 |
| action | Enum | NOT NULL | SUBMIT / APPROVE / REJECT / WITHDRAW / PUBLISH / OFFLINE / ARCHIVE |
| operatorId | UUID | FK -> Person.id, NOT NULL | 操作人 |
| opinion | String(1024) | NULLABLE | 审核意见 |
| createdAt | Timestamp | NOT NULL | |

#### 关联实体：PublicationScopeRule（发布范围规则）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | UUID | PK | |
| publicationId | UUID | FK -> ContentPublication.id, NOT NULL | 所属发布 |
| subjectType | Enum | NOT NULL | GLOBAL / ORGANIZATION / DEPARTMENT / POSITION / ROLE / PERSON |
| subjectId | UUID | NULLABLE | 主体 ID，GLOBAL 时为空 |
| effect | Enum | NOT NULL, DEFAULT ALLOW | ALLOW / DENY |
| sortOrder | Integer | NOT NULL, DEFAULT 0 | 规则顺序 |

**业务规则**：

- 发布的是某个具体版本，审核通过后方可进入 `PUBLISHED`。
- 同一内容同一时刻只能存在一个有效发布版本，后续版本发布时应自动下线旧的有效发布。
- `reviewMode=WORKFLOW` 时，发布审核状态由 `02` 输出的流程结果驱动，但发布状态机仍归 `03` 控制。
- `DENY` 规则优先于 `ALLOW`，标签和专题不参与可见性判定。
- 已归档内容保留历史版本和审计记录，但不再参与前台展示与推荐。

**领域事件**：

| 事件类型 | 触发时机 | 载荷 |
|----------|----------|------|
| `content.article.submitted` | 发起送审 | articleId, publicationId, reviewMode |
| `content.article.published` | 内容发布 | articleId, categoryId, visibleScope |
| `content.article.archived` | 内容归档 | articleId, publicationId |

### 4.4 ContentSubscriptionProfile（订阅与收藏配置）

订阅与收藏配置负责用户对栏目、标签、专题和具体内容的关注与个人偏好。

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | UUID | PK | 配置唯一标识 |
| personId | UUID | FK -> Person.id, NOT NULL | 所属用户 |
| notifyChannel | Enum | NOT NULL | NONE / IN_APP / MESSAGE_CENTER |
| digestMode | Enum | NOT NULL | REALTIME / DAILY / WEEKLY |
| status | Enum | NOT NULL | ACTIVE / MUTED / DISABLED |
| tenantId | UUID | FK -> Tenant.id, NOT NULL | 所属租户 |
| createdAt | Timestamp | NOT NULL | |
| updatedAt | Timestamp | NOT NULL | |

#### 关联实体：SubscriptionTarget（订阅目标）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | UUID | PK | |
| profileId | UUID | FK -> ContentSubscriptionProfile.id, NOT NULL | 所属配置 |
| targetType | Enum | NOT NULL | CATEGORY / TAG / TOPIC / AUTHOR |
| targetId | UUID | NOT NULL | 目标 ID |
| priority | Integer | NOT NULL, DEFAULT 0 | 优先级 |

#### 关联实体：FavoriteRecord（收藏记录）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | UUID | PK | |
| profileId | UUID | FK -> ContentSubscriptionProfile.id, NOT NULL | 所属配置 |
| articleId | UUID | FK -> ContentArticle.id, NOT NULL | 收藏内容 |
| favoritedAt | Timestamp | NOT NULL | 收藏时间 |
| pinned | Boolean | NOT NULL, DEFAULT FALSE | 是否置顶 |

**业务规则**：

- 订阅目标只能指向现有栏目、标签、专题或作者，不允许悬挂引用。
- 收藏记录是个人行为，不改变内容本身的可见范围。
- 订阅命中只针对当前已发布且对该用户可见的内容。
- `status=MUTED` 时保留订阅目标，但不触发实时通知。

**领域事件**：

| 事件类型 | 触发时机 | 载荷 |
|----------|----------|------|
| `content.subscription.updated` | 订阅配置变更 | profileId, personId, changedFields |
| `content.favorite.added` | 新增收藏 | articleId, personId |
| `content.favorite.removed` | 取消收藏 | articleId, personId |

### 4.5 ContentEngagementSnapshot（内容互动统计）

内容互动统计负责汇聚阅读、下载、收藏等行为，用于热度排行和运营分析。

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | UUID | PK | 统计快照唯一标识 |
| articleId | UUID | FK -> ContentArticle.id, NOT NULL | 所属内容 |
| statBucket | String(32) | NOT NULL | ALL / DAY / WEEK / MONTH |
| readCount | Long | NOT NULL, DEFAULT 0 | 阅读次数 |
| uniqueReaderCount | Long | NOT NULL, DEFAULT 0 | 去重阅读人数 |
| downloadCount | Long | NOT NULL, DEFAULT 0 | 下载次数 |
| favoriteCount | Long | NOT NULL, DEFAULT 0 | 收藏次数 |
| hotScore | Decimal(18,4) | NOT NULL, DEFAULT 0 | 热度分值 |
| lastAggregatedAt | Timestamp | NULLABLE | 最近聚合时间 |
| tenantId | UUID | FK -> Tenant.id, NOT NULL | 所属租户 |
| createdAt | Timestamp | NOT NULL | |
| updatedAt | Timestamp | NOT NULL | |

#### 关联实体：ContentReadRecord（内容互动记录）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | UUID | PK | |
| articleId | UUID | FK -> ContentArticle.id, NOT NULL | 所属内容 |
| personId | UUID | FK -> Person.id, NULLABLE | 操作人 |
| actionType | Enum | NOT NULL | READ / PREVIEW / DOWNLOAD / FAVORITE |
| assignmentId | UUID | FK -> Assignment.id, NULLABLE | 当时身份上下文 |
| occurredAt | Timestamp | NOT NULL | 发生时间 |

**业务规则**：

- 原始互动记录采用追加式写入，不直接覆盖统计快照。
- 热度排行只统计当前已发布且未归档内容，已下线内容可保留历史统计但不参与前台排行。
- 阅读与下载统计允许异步聚合，最终一致即可，不阻塞前台访问。
- 身份上下文只用于分析维度和审计，不改变互动事实本身。

**领域事件**：

| 事件类型 | 触发时机 | 载荷 |
|----------|----------|------|
| `content.read.recorded` | 记录阅读/下载行为 | articleId, personId, actionType |
| `content.hotspot.recalculated` | 热度重算完成 | articleId, statBucket, hotScore |

## 5. 关键规则与解析策略

### 5.1 发布可见性解析

内容对用户可见必须同时满足：

1. `ContentPublication.publicationStatus=PUBLISHED`
2. 当前时间命中 `startAt/endAt`
3. 当前用户身份上下文命中 `PublicationScopeRule`
4. 栏目级 `CategoryPermissionRule` 允许当前用户查看

解析优先级建议为：

- `DENY` 优先于 `ALLOW`
- 主体优先级为 `PERSON > ROLE > POSITION > DEPARTMENT > ORGANIZATION > GLOBAL`
- 栏目权限与发布范围同时存在时，取更严格结果

### 5.2 版本与正文管理原则

- 正文、摘要、封面、附件绑定均以版本快照形式持有，不允许“只改正文不出版本”。
- 已发布版本不可直接编辑，必须派生新草稿版本。
- 归档不删除历史正文与附件绑定，只改变检索和展示状态。

### 5.3 检索与索引原则

- 只有已发布且当前有效的内容进入全文检索索引。
- 检索索引至少包含标题、摘要、正文、栏目、标签、专题和作者等字段。
- 下线或归档内容应触发索引移除或降权，而不是继续暴露为有效结果。
- 推荐和热搜只基于已发布内容的投影，不得直接依赖草稿数据。

### 5.4 订阅与通知原则

- 订阅命中由 `03` 负责计算，实际通知发送由 `06` 承担。
- 收藏、订阅和阅读行为可异步入库与聚合，不要求强事务一致。
- 订阅对象删除或停用后，相关订阅目标应标记失效，但保留审计轨迹。

### 5.5 审核与流程接入原则

- 内容审核流程由 `ContentPublication.reviewMode` 决定，不强制所有内容都接流程。
- 接入 `02` 时，流程实例只作为审核机制，内容域不把审核状态外包给流程域。
- 审核通过、驳回、撤回都必须回写到 `ReviewRecord`，形成完整生命周期轨迹。

## 6. 事件模型

### 6.1 `03` 内部领域事件

| 事件类型 | 载荷关键字段 | 说明 |
|----------|-------------|------|
| `content.category.created` | categoryId, categoryType, code | 栏目/分类创建 |
| `content.category.updated` | categoryId, changedFields | 栏目/分类更新 |
| `content.article.created` | articleId, contentType, mainCategoryId | 稿件创建 |
| `content.article.updated` | articleId, changedFields | 稿件更新 |
| `content.article.versioned` | articleId, versionNo | 内容新版本生成 |
| `content.article.submitted` | articleId, publicationId, reviewMode | 发起送审 |
| `content.article.published` | articleId, categoryId, visibleScope | 内容发布 |
| `content.article.archived` | articleId, publicationId | 内容归档 |
| `content.subscription.updated` | profileId, personId, changedFields | 订阅配置变更 |
| `content.favorite.added` | articleId, personId | 新增收藏 |
| `content.read.recorded` | articleId, personId, actionType | 阅读行为记录 |

### 6.2 跨模块总线事件

当前优先对外暴露并已在统一事件契约中占位的事件如下：

| 总线事件类型 | 触发时机 | payload 关键字段 |
|--------------|----------|------------------|
| `content.category.created` | 栏目/分类创建 | categoryId, categoryType, organizationId |
| `content.category.updated` | 栏目/分类更新 | categoryId, changedFields |
| `content.article.created` | 稿件创建 | articleId, categoryId, authorId, organizationId |
| `content.article.updated` | 稿件更新 | articleId, changedFields |
| `content.article.published` | 内容发布 | articleId, categoryId, visibleScope |
| `content.article.archived` | 内容归档 | articleId |

互动类事件如 `content.read.recorded`、`content.favorite.added` 建议先作为模块内统计投影事件使用，待统一事件契约扩展后再显式暴露。

### 6.3 入站业务事件

`03` 重点消费以下事件以驱动审核回写、权限刷新和附件一致性处理：

| 事件类型 | 处理方式 | 说明 |
|----------|----------|------|
| `process.instance.completed` | 回写工作流审核结果为通过 | 审核流主链路 |
| `process.instance.terminated` | 回写审核终止/驳回状态 | 审核流主链路 |
| `org.organization.hierarchy-changed` | 刷新栏目和发布范围缓存 | 可见性依赖组织树 |
| `org.role.person-granted` / `org.role.person-revoked` | 刷新受影响订阅和可见性裁剪缓存 | 角色可见性变更 |
| `infra.attachment.deleted` | 校验或清理内容附件引用 | 附件一致性维护 |

## 7. 跨模块依赖

| 模块关系 | 依赖内容 | 说明 |
|----------|----------|------|
| `03 -> 00` | 附件、字典、配置、审计、i18n、错误码 | 内容底座基础设施依赖 |
| `03 -> 01` | 组织/岗位/角色/人员、身份上下文、数据权限 | 发布范围与可见性裁剪 |
| `03 -> 02` | 内容审核流程、审核结果回写 | 可选工作流审核链路 |
| `04 -> 03` | 公告、制度、知识内容聚合查询 | 门户卡片消费内容读模型 |
| `05 -> 03` | 公告发布、知识展示、共享内容复用 | `05` 通过场景装配复用内容底座 |
| `06 -> 03` | 订阅命中内容、发布通知模板输入 | 内容发布后的用户触达 |
| `07 -> 03` | 开放 API、报表和外部同步数据源 | 内容对外开放与统计分析 |

## 8. 数据库设计建议

### 8.1 核心表清单

| 表名 | 对应聚合根/实体 | 说明 |
|------|-----------------|------|
| `content_category` | ContentCategory | 栏目/分类/专题骨架主表 |
| `content_category_permission` | CategoryPermissionRule | 栏目权限规则表 |
| `content_tag_def` | TagDefinition | 标签定义表 |
| `content_topic_def` | TopicDefinition | 专题定义表 |
| `content_article` | ContentArticle | 内容主表 |
| `content_article_version` | ContentVersion | 内容版本表 |
| `content_article_body` | ContentVersion.bodyRef | 正文存储引用表 |
| `content_article_attachment` | ArticleAttachmentBinding | 附件绑定表 |
| `content_article_relation` | ArticleRelation | 内容关联关系表 |
| `content_publication` | ContentPublication | 内容发布表 |
| `content_review_record` | ReviewRecord | 审核记录表 |
| `content_publication_scope` | PublicationScopeRule | 发布范围表 |
| `content_subscription_profile` | ContentSubscriptionProfile | 订阅配置表 |
| `content_subscription_target` | SubscriptionTarget | 订阅目标表 |
| `content_favorite_record` | FavoriteRecord | 收藏记录表 |
| `content_read_record` | ContentReadRecord | 阅读/下载行为明细表 |
| `content_engagement_snapshot` | ContentEngagementSnapshot | 内容统计快照表 |
| `content_search_index` | SearchIndexDocument | 全文检索投影表/索引入口 |

### 8.2 索引建议

- `content_category`：`(tenantId, code)`、`(tenantId, categoryType, status)`、`(parentId, sortOrder)`
- `content_category_permission`：`(categoryId, subjectType, subjectId, scopeType)`
- `content_article`：`(tenantId, articleNo)`、`(tenantId, mainCategoryId, status)`、`(tenantId, contentType, status, updatedAt)`
- `content_article_version`：`(articleId, versionNo)`、`(articleId, status, createdAt)`
- `content_article_attachment`：`(articleVersionId, sortOrder)`、`(attachmentId)`
- `content_publication`：`(articleId, publicationStatus)`、`(tenantId, publicationStatus, startAt, endAt)`、`(workflowInstanceId)`
- `content_publication_scope`：`(publicationId, subjectType, subjectId)`
- `content_subscription_profile`：`(tenantId, personId, status)`
- `content_subscription_target`：`(profileId, targetType, targetId)`
- `content_favorite_record`：`(profileId, articleId)`、`(articleId, favoritedAt)`
- `content_read_record`：`(articleId, occurredAt)`、`(personId, occurredAt)`、`(assignmentId, occurredAt)`
- `content_engagement_snapshot`：`(articleId, statBucket)`、`(tenantId, statBucket, hotScore)`

### 8.3 大字段与检索建议

- 正文、摘要快照、索引文本建议与主表拆分，正文正文体可采用 `TEXT/JSONB` 或独立大字段表。
- 一期全文检索建议采用 PostgreSQL 全文索引；二期可按 ADR-004 演进到 Elasticsearch。
- `visibleScope`、`changedFields`、`bodyRef`、`推荐候选` 等结构化字段建议采用 JSONB 存储。

## 9. 一期建模优先级建议

### 9.1 平台口径

- `03` 不属于平台一期最小闭环必做模块（D20）。
- 当前文档的优先级用于后续项目场景接入排序，而不是改变一期总范围。

### 9.2 若提前接入，必须先落的聚合

- `ContentCategory`
- `ContentArticle`
- `ContentPublication`

### 9.3 若提前接入，可简化实现的部分

- `ContentSubscriptionProfile` 先支持栏目/标签订阅和个人收藏
- `content-search` 先支持标题、摘要、正文的基础检索与分类筛选
- `ContentEngagementSnapshot` 先支持阅读、下载、收藏的基础累计和热度排行

### 9.4 后续可继续补强的方向

- 专题编排、专题首页和复杂栏目模板
- 多语言内容全文检索和多语言运营
- 复杂推荐算法、相似内容召回和知识图谱
- 内容引用血缘分析、制度确认回执和高级运营漏斗

## 10. 结论

`03-内容与知识` 的核心不是“做一个公告模块”，而是建立一套**统一内容主模型、版本可追溯、发布可控、可见性可裁剪、检索与统计可投影**的内容底座。其建模重点应围绕以下原则展开：

- 用 `ContentCategory` 管语义组织骨架与栏目权限
- 用 `ContentArticle` 管内容主数据、正文版本与附件引用
- 用 `ContentPublication` 管审核、发布、下线和归档
- 用 `ContentSubscriptionProfile` 管个人关注与收藏
- 用 `ContentEngagementSnapshot` 管阅读统计和热度投影

该文档可直接作为后续 `HJO2OA-Content/docs/module-design.md`、内容建表、全文检索方案细化和公告/知识场景接入的基础。
