# 00-平台基础设施 领域模型

## 1. 文档目的

本文档细化 `00-平台基础设施` 模块的核心领域实体、值对象、聚合根、实体关系和业务规则，作为后端工程建模、共享能力抽象、数据库设计和接口契约的统一依据。

对应架构决策编号：D02（模块化单体 + 独立基础设施服务）、D06（租户与组织分离）、D11（统一接口契约）、D12（统一事件契约）、D13（最终一致性）、D20（一期实施范围）。

## 2. 领域定位与边界

### 2.1 领域职责

`00-平台基础设施` 是横跨所有业务域的共享能力层，负责提供统一的技术真相源、策略真相源和运行保障能力，核心职责包括：

- 提供跨模块事件发布、订阅、路由、持久化、重试和死信处理能力
- 提供国际化、时区、多语言数据、错误码等全局语义基础设施
- 提供附件、字典、配置、调度、审计、缓存、多租户、安全工具等共享底座能力
- 为 `01-07` 模块提供统一的技术边界和可复用协议，避免各业务模块重复建设

### 2.2 关键边界

- **`00` 拥有共享技术能力，不拥有业务语义主数据**：流程、消息、内容、门户、业务应用的业务事实仍归各自模块所有。
- **租户不等于组织**：`Tenant` 只负责系统隔离、配置隔离、配额和资源隔离；组织、部门、岗位、人员、角色属于 `01-组织与权限`。
- **事件总线不拥有业务动作语义**：`event-bus` 负责事件信封、订阅、投递和可靠性，不定义业务事件背后的业务规则。
- **缓存不是真相源**：缓存只承担性能优化与读模型加速，业务事实真相源仍在业务域或共享主数据域。
- **审计不是业务流程状态机**：审计记录只负责留痕和追溯，不承担业务审批、业务审批轨迹真相源的所有权。
- **安全工具不拥有认证身份主模型**：密码策略、密钥、脱敏、签名和风控策略归 `00`；账号、身份上下文、角色权限仍归 `01`。
- **错误码不等于异常实现**：错误码体系负责标准化定义和国际化输出，具体业务异常仍归各模块自己抛出。

### 2.3 一期收敛口径

一期最小闭环优先实现以下能力：

- `event-bus`
- `i18n`
- `timezone`
- `data-i18n`
- `attachment`
- `dictionary`
- `config`
- `audit`
- `error-code`

二期增强能力：

- `cache`
- `scheduler`
- `tenant`
- `security`

说明：即便 `tenant`、`cache`、`scheduler`、`security` 在一期不是最小闭环前提，领域模型仍需先收敛，以避免后续工程接入时再次重新定义边界。

## 3. 领域总览

`00` 与业务域不同，其核心聚合更多体现为**注册表、策略对象、版本对象、运行日志对象和共享资源对象**。

### 3.1 聚合根

| 聚合根 | 说明 | 目录 |
|--------|------|------|
| EventDefinition | 事件定义注册表，定义事件类型、载荷语义和订阅边界 | `event-bus/` |
| EventMessage | 事件总线中的事件信封与投递生命周期真相源 | `event-bus/` |
| LocaleBundle | 多语言资源包与语言配置真相源 | `i18n/` |
| TimezoneSetting | 系统、租户、用户的时区偏好与时区策略 | `timezone/` |
| TranslationEntry | 业务数据的多语言翻译值真相源 | `data-i18n/` |
| AttachmentAsset | 统一附件元数据、版本、绑定关系和配额占用 | `attachment/` |
| DictionaryType | 字典类型与字典项的真相源 | `dictionary/` |
| ConfigEntry | 系统参数、配置覆盖链和功能开关策略 | `config/` |
| ScheduledJob | 定时任务定义、执行策略和运行记录 | `scheduler/` |
| AuditRecord | 平台级审计留痕和变更追溯真相源 | `audit/` |
| CachePolicy | 缓存命名空间、TTL 策略和失效策略真相源 | `cache/` |
| TenantProfile | 租户生命周期、套餐、配额和初始化策略 | `tenant/` |
| ErrorCodeDefinition | 错误码、分组、严重级别和国际化消息映射真相源 | `error-code/` |
| SecurityPolicy | 密钥、脱敏、签名、密码规则和安全防护策略真相源 | `security/` |

### 3.2 核心实体关系

```text
EventDefinition   ──1:N──> SubscriptionBinding        (事件定义 -> 订阅关系)
EventDefinition   ──1:N──> EventMessage               (事件定义 -> 已发布事件)
EventMessage      ──1:N──> DeliveryAttempt            (事件 -> 多次投递尝试)
LocaleBundle      ──1:N──> LocaleResourceEntry        (资源包 -> 键值项)
TranslationEntry  ──M:1──> LocaleBundle               (翻译值引用语言配置)
AttachmentAsset   ──1:N──> AttachmentVersion          (文件 -> 版本链)
AttachmentAsset   ──1:N──> AttachmentBinding          (文件 -> 业务绑定关系)
DictionaryType    ──1:N──> DictionaryItem             (字典类型 -> 字典项)
ConfigEntry       ──1:N──> ConfigOverride             (配置定义 -> 覆盖链)
ConfigEntry       ──1:N──> FeatureRule                (配置定义 -> 功能开关规则)
ScheduledJob      ──1:N──> JobExecutionRecord         (任务 -> 执行记录)
AuditRecord       ──1:N──> AuditFieldChange           (审计记录 -> 字段差异)
CachePolicy       ──1:N──> CacheInvalidationRecord    (策略 -> 失效记录)
TenantProfile     ──1:N──> TenantQuota                (租户 -> 配额项)
ErrorCodeDefinition ──M:1──> LocaleBundle             (错误消息国际化引用)
SecurityPolicy    ──1:N──> SecretKeyMaterial          (安全策略 -> 密钥材料)
SecurityPolicy    ──1:N──> MaskingRule                (安全策略 -> 脱敏规则)
SecurityPolicy    ──1:N──> RateLimitRule              (安全策略 -> 限频规则)
```

### 3.3 核心业务流

```text
业务模块完成事务
  -> 通过 EventDefinition 校验事件契约
  -> 产生 EventMessage
  -> EventBus 路由到 SubscriptionBinding
  -> DeliveryAttempt 执行投递 / 重试 / 死信
  -> AuditRecord 记录发布与消费轨迹
  -> CachePolicy 根据事件触发缓存失效
```

## 4. 核心聚合定义

### 4.1 EventDefinition（事件定义）

事件定义是平台事件契约的注册真相源，用于约束事件名称、版本、载荷语义和订阅范围。

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | UUID | PK | 事件定义唯一标识 |
| eventType | String(128) | UK, NOT NULL | 事件类型，如 `process.task.completed` |
| modulePrefix | String(32) | NOT NULL | 模块前缀，如 `process`、`org`、`portal` |
| version | String(16) | NOT NULL | 契约版本，如 `v1` |
| payloadSchema | JSON | NOT NULL | 载荷结构定义 |
| description | String(512) | NULLABLE | 事件语义说明 |
| publishMode | Enum | NOT NULL | SYNC / ASYNC / OUTBOX |
| status | Enum | NOT NULL | DRAFT / ACTIVE / DEPRECATED |
| ownerModule | String(64) | NOT NULL | 事件所属模块 |
| tenantScope | Enum | NOT NULL | GLOBAL / TENANT_AWARE |
| createdAt | Timestamp | NOT NULL | |
| updatedAt | Timestamp | NOT NULL | |

#### 关联实体：SubscriptionBinding（订阅绑定）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | UUID | PK | |
| eventDefinitionId | UUID | FK -> EventDefinition.id, NOT NULL | 所属事件定义 |
| subscriberCode | String(64) | NOT NULL | 消费者编码 |
| matchMode | Enum | NOT NULL | EXACT / WILDCARD |
| retryPolicy | JSON | NULLABLE | 重试策略 |
| deadLetterEnabled | Boolean | NOT NULL, DEFAULT TRUE | 是否启用死信 |
| active | Boolean | NOT NULL, DEFAULT TRUE | 是否启用 |

**业务规则**：

- 所有跨模块事件都必须先在 `EventDefinition` 中注册，再允许发布。
- 事件定义只管理契约和路由信息，不拥有业务语义真相源。
- `DEPRECATED` 事件定义在兼容窗口结束前可继续被消费，但不允许新增发布方依赖。
- 一个事件定义可被多个订阅者消费，但每个订阅者必须独立声明重试和死信策略。
- 事件定义的命名、载荷和版本必须与统一事件契约保持一致。

**领域事件**：

| 事件类型 | 触发时机 | 载荷 |
|----------|----------|------|
| `infra.event.schema-registered` | 事件定义激活 | eventType, version |
| `infra.event.schema-deprecated` | 事件定义废弃 | eventType, version |

### 4.2 EventMessage（事件消息）

事件消息是事件总线中实际被发布、投递、重试和审计的运行时对象。

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | UUID | PK | 平台事件唯一标识 |
| eventDefinitionId | UUID | FK -> EventDefinition.id, NOT NULL | 所属事件定义 |
| eventType | String(128) | NOT NULL | 事件类型快照 |
| source | String(128) | NOT NULL | 事件来源 |
| tenantId | UUID | NULLABLE | 所属租户 |
| correlationId | String(128) | NULLABLE | 关联 ID |
| traceId | String(128) | NULLABLE | 链路追踪 ID |
| operatorAccountId | UUID | NULLABLE | 操作账号 |
| operatorPersonId | UUID | NULLABLE | 操作人员 |
| payload | JSON | NOT NULL | 事件载荷 |
| publishStatus | Enum | NOT NULL | PENDING / PUBLISHED / PARTIALLY_DELIVERED / DELIVERED / DEAD_LETTERED |
| publishedAt | Timestamp | NULLABLE | 发布时间 |
| retainedUntil | Timestamp | NULLABLE | 保留期限 |
| createdAt | Timestamp | NOT NULL | |
| updatedAt | Timestamp | NOT NULL | |

#### 关联实体：DeliveryAttempt（投递尝试）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | UUID | PK | |
| eventMessageId | UUID | FK -> EventMessage.id, NOT NULL | 所属事件 |
| subscriberCode | String(64) | NOT NULL | 目标消费者 |
| attemptNo | Integer | NOT NULL | 第几次尝试 |
| deliveryStatus | Enum | NOT NULL | SUCCESS / FAILED / RETRYING / DEAD_LETTERED |
| errorCode | String(64) | NULLABLE | 错误码 |
| errorMessage | String(512) | NULLABLE | 错误描述 |
| deliveredAt | Timestamp | NULLABLE | 投递完成时间 |
| nextRetryAt | Timestamp | NULLABLE | 下次重试时间 |
| requestSnapshot | JSON | NULLABLE | 投递请求快照 |
| responseSnapshot | JSON | NULLABLE | 消费响应快照 |

**业务规则**：

- `EventMessage` 是平台事件信封真相源，必须具备幂等标识和审计字段。
- 发布与持久化必须通过 Outbox 或等价机制保证“事务后必达”。
- 至少一次投递由 `DeliveryAttempt` 承担，重复消费的幂等由消费方保证。
- 进入死信队列的事件不可静默丢弃，必须保留人工干预入口。
- 事件保留期限可按合规要求配置，但删除前必须完成归档或审计留痕。

**领域事件**：

| 事件类型 | 触发时机 | 载荷 |
|----------|----------|------|
| `infra.event.published` | 事件成功出站 | eventId, eventType |
| `infra.event.delivery-failed` | 投递失败 | eventId, subscriberCode, attemptNo |
| `infra.event.dead-lettered` | 事件进入死信 | eventId, subscriberCode |

### 4.3 LocaleBundle（国际化资源包）

国际化资源包负责语言列表、回退策略和资源包键值管理。

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | UUID | PK | 资源包唯一标识 |
| bundleCode | String(64) | UK, NOT NULL | 资源包编码 |
| moduleCode | String(64) | NOT NULL | 所属模块 |
| locale | String(16) | NOT NULL | 语言区域，如 `zh-CN` |
| fallbackLocale | String(16) | NULLABLE | 回退语言 |
| status | Enum | NOT NULL | DRAFT / ACTIVE / DEPRECATED |
| tenantId | UUID | NULLABLE | 租户级资源包可选 |
| createdAt | Timestamp | NOT NULL | |
| updatedAt | Timestamp | NOT NULL | |

#### 关联实体：LocaleResourceEntry

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | UUID | PK | |
| localeBundleId | UUID | FK -> LocaleBundle.id, NOT NULL | 所属资源包 |
| resourceKey | String(256) | NOT NULL | 资源键 |
| resourceValue | Text | NOT NULL | 资源值 |
| version | Integer | NOT NULL, DEFAULT 1 | 版本 |
| active | Boolean | NOT NULL, DEFAULT TRUE | 是否生效 |

**业务规则**：

- 资源键在同一资源包内必须唯一。
- 回退语言链不能形成循环。
- 后端消息和错误码消息都应优先通过 `LocaleBundle` 解析，不允许业务模块各自持有多语言文本真相源。
- 资源包更新后应支持热更新，不要求服务重启。

### 4.4 TimezoneSetting（时区设置）

时区设置负责系统、租户和用户级别的时区策略与偏好。

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | UUID | PK | 时区设置唯一标识 |
| scopeType | Enum | NOT NULL | SYSTEM / TENANT / PERSON |
| scopeId | UUID | NULLABLE | 作用域 ID，SYSTEM 为空 |
| timezoneId | String(64) | NOT NULL | IANA 时区，如 `Asia/Shanghai` |
| isDefault | Boolean | NOT NULL, DEFAULT FALSE | 是否默认值 |
| effectiveFrom | Timestamp | NULLABLE | 生效时间 |
| active | Boolean | NOT NULL, DEFAULT TRUE | 是否生效 |
| tenantId | UUID | NULLABLE | 所属租户 |
| createdAt | Timestamp | NOT NULL | |
| updatedAt | Timestamp | NOT NULL | |

**业务规则**：

- 时间字段统一以 UTC 存储，`TimezoneSetting` 只决定解释和展示方式。
- 用户级时区优先于租户级时区，租户级优先于系统默认时区。
- 定时任务可以显式指定执行时区，否则按租户/系统默认时区解释。
- `01` 模块的身份或组织结构不参与时区主模型所有权。

### 4.5 TranslationEntry（多语言数据翻译项）

多语言翻译项负责业务对象字段的多语言值存储。

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | UUID | PK | 翻译项唯一标识 |
| entityType | String(64) | NOT NULL | 业务对象类型 |
| entityId | String(64) | NOT NULL | 业务对象 ID |
| fieldName | String(128) | NOT NULL | 业务字段名 |
| locale | String(16) | NOT NULL | 语言区域 |
| translatedValue | Text | NOT NULL | 翻译值 |
| translationStatus | Enum | NOT NULL | DRAFT / TRANSLATED / REVIEWED |
| tenantId | UUID | NOT NULL | 所属租户 |
| updatedBy | UUID | NULLABLE | 更新人 |
| updatedAt | Timestamp | NOT NULL | |

**UK 约束**：`(entityType, entityId, fieldName, locale)`

**业务规则**：

- 翻译项只拥有翻译值，不拥有业务对象主数据。
- 查询时若当前语言无翻译，应回退默认语言或原始值。
- 多语言翻译状态与业务对象状态解耦，不影响业务主对象生命周期。
- 批量导入导出必须校验 `entityType + entityId + fieldName + locale` 唯一性。

### 4.6 AttachmentAsset（附件资产）

附件资产负责文件元数据、版本、绑定关系、权限钩子和配额占用。

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | UUID | PK | 文件唯一标识 |
| storageKey | String(256) | UK, NOT NULL | 存储对象键 |
| originalFilename | String(256) | NOT NULL | 原始文件名 |
| contentType | String(128) | NOT NULL | MIME 类型 |
| sizeBytes | Long | NOT NULL | 文件大小 |
| checksum | String(128) | NOT NULL | 校验摘要 |
| storageProvider | Enum | NOT NULL | LOCAL / MINIO / S3 / OSS |
| previewStatus | Enum | NOT NULL | NONE / READY / FAILED / PROCESSING |
| latestVersionNo | Integer | NOT NULL, DEFAULT 1 | 最新版本号 |
| permissionMode | Enum | NOT NULL | INHERIT_BUSINESS / CUSTOM |
| tenantId | UUID | NOT NULL | 所属租户 |
| createdBy | UUID | NULLABLE | 创建人 |
| createdAt | Timestamp | NOT NULL | |
| updatedAt | Timestamp | NOT NULL | |

#### 关联实体：AttachmentVersion

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | UUID | PK | |
| attachmentAssetId | UUID | FK -> AttachmentAsset.id, NOT NULL | 所属文件 |
| versionNo | Integer | NOT NULL | 版本号 |
| storageKey | String(256) | NOT NULL | 版本文件存储键 |
| checksum | String(128) | NOT NULL | 版本校验摘要 |
| sizeBytes | Long | NOT NULL | 版本大小 |
| createdBy | UUID | NULLABLE | 版本创建人 |
| createdAt | Timestamp | NOT NULL | |

#### 关联实体：AttachmentBinding

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | UUID | PK | |
| attachmentAssetId | UUID | FK -> AttachmentAsset.id, NOT NULL | 所属文件 |
| businessType | String(64) | NOT NULL | 绑定业务类型 |
| businessId | String(64) | NOT NULL | 绑定业务对象 ID |
| bindingRole | Enum | NOT NULL | PRIMARY / ATTACHMENT / COVER / PREVIEW_SOURCE |
| active | Boolean | NOT NULL, DEFAULT TRUE | 是否生效 |

**业务规则**：

- 附件主模型拥有文件元数据、版本与存储位置真相源，不拥有业务对象可见范围真相源。
- `permissionMode=INHERIT_BUSINESS` 时，访问控制需回调业务域或统一权限域。
- 文件版本追加后不得覆盖旧版本，必须保留可追溯历史。
- 相同校验摘要可做秒传优化，但不能破坏绑定关系和审计记录。
- 配额统计按租户或指定资源主体累计，文件物理删除前必须校验业务绑定关系。

**领域事件**：

| 事件类型 | 触发时机 | 载荷 |
|----------|----------|------|
| `infra.attachment.created` | 文件上传完成并成功建档 | attachmentId, fileName, storageProvider |
| `infra.attachment.version-created` | 文件新版本创建 | attachmentId, versionNo |
| `infra.attachment.quota-warning` | 配额预警 | tenantId, attachmentId, threshold |

### 4.7 DictionaryType（字典类型）

字典类型负责系统枚举、级联选项和选项集管理。

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | UUID | PK | 字典类型唯一标识 |
| code | String(64) | UK, NOT NULL | 字典编码 |
| name | String(128) | NOT NULL | 字典名称 |
| category | String(64) | NULLABLE | 分类 |
| hierarchical | Boolean | NOT NULL, DEFAULT FALSE | 是否层级字典 |
| cacheable | Boolean | NOT NULL, DEFAULT TRUE | 是否缓存 |
| status | Enum | NOT NULL | ACTIVE / DISABLED |
| tenantId | UUID | NULLABLE | 租户级字典可选 |
| createdAt | Timestamp | NOT NULL | |
| updatedAt | Timestamp | NOT NULL | |

#### 关联实体：DictionaryItem

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | UUID | PK | |
| dictionaryTypeId | UUID | FK -> DictionaryType.id, NOT NULL | 所属字典类型 |
| itemCode | String(64) | NOT NULL | 项编码 |
| displayName | String(128) | NOT NULL | 展示值 |
| parentItemId | UUID | FK -> DictionaryItem.id, NULLABLE | 上级项 |
| sortOrder | Integer | NOT NULL, DEFAULT 0 | 排序 |
| enabled | Boolean | NOT NULL, DEFAULT TRUE | 是否启用 |
| multiLangValue | JSON | NULLABLE | 多语言展示值 |

**业务规则**：

- 字典编码在全局或租户范围内必须唯一。
- 级联字典不能形成环路。
- 业务模块只能引用字典类型和字典项，不得复制一份本地枚举真相源。
- 字典项停用不自动删除历史业务引用，但影响新增校验与新查询返回。
- 字典更新后应触发缓存刷新事件。

### 4.8 ConfigEntry（配置项）

配置项负责系统参数、覆盖链和功能开关策略。

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | UUID | PK | 配置项唯一标识 |
| configKey | String(128) | UK, NOT NULL | 配置键 |
| name | String(128) | NOT NULL | 配置名称 |
| configType | Enum | NOT NULL | STRING / NUMBER / BOOLEAN / JSON / FEATURE_FLAG |
| defaultValue | JSON | NOT NULL | 默认值 |
| validationRule | JSON | NULLABLE | 校验规则 |
| mutableAtRuntime | Boolean | NOT NULL, DEFAULT TRUE | 是否支持热更新 |
| status | Enum | NOT NULL | ACTIVE / DISABLED / DEPRECATED |
| tenantAware | Boolean | NOT NULL, DEFAULT FALSE | 是否支持租户覆盖 |
| createdAt | Timestamp | NOT NULL | |
| updatedAt | Timestamp | NOT NULL | |

#### 关联实体：ConfigOverride

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | UUID | PK | |
| configEntryId | UUID | FK -> ConfigEntry.id, NOT NULL | 所属配置项 |
| scopeType | Enum | NOT NULL | TENANT / ORGANIZATION / ROLE / USER |
| scopeId | UUID | NOT NULL | 作用域 ID |
| overrideValue | JSON | NOT NULL | 覆盖值 |
| active | Boolean | NOT NULL, DEFAULT TRUE | 是否生效 |

#### 关联实体：FeatureRule

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | UUID | PK | |
| configEntryId | UUID | FK -> ConfigEntry.id, NOT NULL | 所属 Feature Flag |
| ruleType | Enum | NOT NULL | GLOBAL / TENANT / ORG / ROLE / PERCENTAGE |
| ruleValue | JSON | NOT NULL | 规则值 |
| sortOrder | Integer | NOT NULL, DEFAULT 0 | 规则顺序 |
| active | Boolean | NOT NULL, DEFAULT TRUE | 是否启用 |

**业务规则**：

- 配置读取遵循“默认值 -> 覆盖链 -> Feature Rule”解析顺序。
- `FEATURE_FLAG` 类型配置必须显式声明规则顺序和回退策略。
- 热更新配置需区分“即时生效”和“需重载上下文”的场景。
- 组织、角色、用户级覆盖只引用 `01` 的主体 ID，不拥有这些主体的主数据。
- 所有配置变更必须写入审计记录，关键配置应支持回滚。

**领域事件**：

| 事件类型 | 触发时机 | 载荷 |
|----------|----------|------|
| `infra.config.updated` | 配置值变更 | configKey, scopeType, scopeId |
| `infra.feature-flag.changed` | 开关规则变更 | configKey, ruleType |

### 4.9 ScheduledJob（定时任务）

定时任务负责平台级任务定义、执行策略和运行记录。

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | UUID | PK | 任务唯一标识 |
| jobCode | String(64) | UK, NOT NULL | 任务编码 |
| name | String(128) | NOT NULL | 任务名称 |
| triggerType | Enum | NOT NULL | CRON / MANUAL / DEPENDENCY |
| cronExpr | String(64) | NULLABLE | Cron 表达式 |
| timezoneId | String(64) | NULLABLE | 执行时区 |
| concurrencyPolicy | Enum | NOT NULL | ALLOW / FORBID / REPLACE |
| timeoutSeconds | Integer | NULLABLE | 超时时间 |
| retryPolicy | JSON | NULLABLE | 重试策略 |
| status | Enum | NOT NULL | ACTIVE / PAUSED / DISABLED |
| tenantId | UUID | NULLABLE | 租户级任务可选 |
| createdAt | Timestamp | NOT NULL | |
| updatedAt | Timestamp | NOT NULL | |

#### 关联实体：JobExecutionRecord

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | UUID | PK | |
| scheduledJobId | UUID | FK -> ScheduledJob.id, NOT NULL | 所属任务 |
| triggerSource | Enum | NOT NULL | CRON / MANUAL / RETRY / DEPENDENCY |
| executionStatus | Enum | NOT NULL | RUNNING / SUCCESS / FAILED / TIMEOUT / CANCELLED |
| startedAt | Timestamp | NOT NULL | 开始时间 |
| finishedAt | Timestamp | NULLABLE | 结束时间 |
| errorCode | String(64) | NULLABLE | 错误码 |
| errorMessage | String(512) | NULLABLE | 错误信息 |
| executionLog | JSON | NULLABLE | 执行摘要 |

**业务规则**：

- 定时任务定义与执行记录分离，执行失败不得污染任务定义主数据。
- `DEPENDENCY` 触发的任务必须显式声明 DAG 依赖，不允许产生环路。
- 任务时区为空时回退租户或系统默认时区。
- 任务重试只针对执行记录，不创建新的任务定义。

### 4.10 AuditRecord（审计记录）

审计记录负责平台级统一留痕，是 append-only 的审计真相源。

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | UUID | PK | 审计记录唯一标识 |
| moduleCode | String(64) | NOT NULL | 来源模块 |
| objectType | String(64) | NOT NULL | 对象类型 |
| objectId | String(64) | NOT NULL | 对象 ID |
| actionType | String(64) | NOT NULL | 操作类型 |
| operatorAccountId | UUID | NULLABLE | 操作账号 |
| operatorPersonId | UUID | NULLABLE | 操作人 |
| tenantId | UUID | NULLABLE | 所属租户 |
| traceId | String(128) | NULLABLE | 链路追踪 ID |
| summary | String(512) | NULLABLE | 变更摘要 |
| occurredAt | Timestamp | NOT NULL | 发生时间 |
| archiveStatus | Enum | NOT NULL | ACTIVE / ARCHIVED |
| createdAt | Timestamp | NOT NULL | |

#### 关联实体：AuditFieldChange

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | UUID | PK | |
| auditRecordId | UUID | FK -> AuditRecord.id, NOT NULL | 所属审计记录 |
| fieldName | String(128) | NOT NULL | 字段名 |
| oldValue | Text | NULLABLE | 旧值 |
| newValue | Text | NULLABLE | 新值 |
| sensitivityLevel | Enum | NULLABLE | 敏感级别 |

**业务规则**：

- 审计记录一旦写入不得修改，只允许归档。
- 敏感字段变更应支持脱敏展示，原始值访问需要更高权限。
- 审计记录可由 API 调用、事件消费、配置变更、手工操作等多个来源统一写入。
- 业务模块自己的轨迹日志可并存，但平台级审计归 `00` 所有。

### 4.11 CachePolicy（缓存策略）

缓存策略负责命名空间级缓存规则、失效方式和监控策略。

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | UUID | PK | 策略唯一标识 |
| namespace | String(128) | UK, NOT NULL | 缓存命名空间 |
| backendType | Enum | NOT NULL | MEMORY / REDIS / HYBRID |
| ttlSeconds | Integer | NOT NULL | TTL |
| maxCapacity | Integer | NULLABLE | 最大容量 |
| evictionPolicy | Enum | NOT NULL | LRU / LFU / FIFO / NONE |
| invalidationMode | Enum | NOT NULL | MANUAL / EVENT_DRIVEN / TIME_BASED |
| metricsEnabled | Boolean | NOT NULL, DEFAULT TRUE | 是否监控 |
| active | Boolean | NOT NULL, DEFAULT TRUE | 是否生效 |
| createdAt | Timestamp | NOT NULL | |
| updatedAt | Timestamp | NOT NULL | |

#### 关联实体：CacheInvalidationRecord

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | UUID | PK | |
| cachePolicyId | UUID | FK -> CachePolicy.id, NOT NULL | 所属策略 |
| invalidateKey | String(256) | NOT NULL | 失效 Key |
| reasonType | Enum | NOT NULL | EVENT / MANUAL / TTL / DEPENDENCY |
| reasonRef | String(128) | NULLABLE | 原因引用，如事件 ID |
| invalidatedAt | Timestamp | NOT NULL | 失效时间 |

**业务规则**：

- 缓存值不是领域真相源，策略和失效轨迹才是平台级可建模对象。
- 命名空间应按业务场景定义，避免直接把具体 Redis Key 当领域对象。
- 事件驱动缓存失效应通过 `EventMessage` 和订阅关系触发，不允许业务模块随意跨域删缓存。
- 手动失效必须保留审计记录。

### 4.12 TenantProfile（租户档案）

租户档案负责系统隔离、套餐、配额和初始化生命周期。

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | UUID | PK | 租户唯一标识 |
| tenantCode | String(64) | UK, NOT NULL | 租户编码 |
| name | String(128) | NOT NULL | 租户名称 |
| status | Enum | NOT NULL | DRAFT / ACTIVE / SUSPENDED / ARCHIVED |
| isolationMode | Enum | NOT NULL | SHARED_DB / DEDICATED_DB |
| packageCode | String(64) | NULLABLE | 套餐编码 |
| defaultLocale | String(16) | NULLABLE | 默认语言 |
| defaultTimezone | String(64) | NULLABLE | 默认时区 |
| initialized | Boolean | NOT NULL, DEFAULT FALSE | 是否完成初始化 |
| createdAt | Timestamp | NOT NULL | |
| updatedAt | Timestamp | NOT NULL | |

#### 关联实体：TenantQuota

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | UUID | PK | |
| tenantProfileId | UUID | FK -> TenantProfile.id, NOT NULL | 所属租户 |
| quotaType | Enum | NOT NULL | USER_COUNT / STORAGE / API_CALL / JOB_COUNT |
| limitValue | Long | NOT NULL | 配额上限 |
| usedValue | Long | NOT NULL, DEFAULT 0 | 已使用值 |
| warningThreshold | Long | NULLABLE | 预警阈值 |

**业务规则**：

- `TenantProfile` 只描述系统租户，不拥有组织树或岗位树。
- 新租户初始化完成前，不得对外开放业务入口。
- `isolationMode` 一旦进入生产期不应随意切换，切换应视为平台迁移行为。
- 配额超限可触发告警或限制，但不直接删除租户资源。

**领域事件**：

| 事件类型 | 触发时机 | 载荷 |
|----------|----------|------|
| `infra.tenant.created` | 租户创建 | tenantId, tenantCode |
| `infra.tenant.initialized` | 租户初始化完成 | tenantId |
| `infra.tenant.quota-warning` | 配额预警 | tenantId, quotaType, warningThreshold |

### 4.13 ErrorCodeDefinition（错误码定义）

错误码定义负责统一错误码、分组、严重级别、HTTP 状态映射和国际化消息键。

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | UUID | PK | 错误码唯一标识 |
| code | String(64) | UK, NOT NULL | 错误码，如 `ORG_4001` |
| moduleCode | String(32) | NOT NULL | 所属模块 |
| category | String(64) | NULLABLE | 分类 |
| severity | Enum | NOT NULL | INFO / WARN / ERROR / FATAL |
| httpStatus | Integer | NOT NULL | HTTP 状态码 |
| messageKey | String(256) | NOT NULL | 国际化资源键 |
| retryable | Boolean | NOT NULL, DEFAULT FALSE | 是否可重试 |
| deprecated | Boolean | NOT NULL, DEFAULT FALSE | 是否废弃 |
| createdAt | Timestamp | NOT NULL | |
| updatedAt | Timestamp | NOT NULL | |

**业务规则**：

- 错误码一旦对外发布，不得随意变更语义；废弃错误码需保留兼容期。
- 错误码国际化消息应通过 `messageKey` 关联资源包，不直接存业务语言文本。
- 业务模块统一引用错误码定义，但异常堆栈和处理逻辑不归错误码模块所有。
- 可重试错误码应与调度、事件投递、开放接口等场景的自动重试策略联动。

### 4.14 SecurityPolicy（安全策略）

安全策略负责平台级密钥、脱敏、签名、密码和访问防护策略。

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | UUID | PK | 安全策略唯一标识 |
| policyCode | String(64) | UK, NOT NULL | 策略编码 |
| policyType | Enum | NOT NULL | KEY_MANAGEMENT / MASKING / SIGNATURE / PASSWORD / ACCESS_CONTROL |
| name | String(128) | NOT NULL | 策略名称 |
| status | Enum | NOT NULL | ACTIVE / DISABLED / DEPRECATED |
| tenantId | UUID | NULLABLE | 租户级策略可选 |
| configSnapshot | JSON | NOT NULL | 策略配置快照 |
| createdAt | Timestamp | NOT NULL | |
| updatedAt | Timestamp | NOT NULL | |

#### 关联实体：SecretKeyMaterial

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | UUID | PK | |
| securityPolicyId | UUID | FK -> SecurityPolicy.id, NOT NULL | 所属策略 |
| keyRef | String(128) | NOT NULL | 密钥引用 |
| algorithm | String(64) | NOT NULL | 算法 |
| keyStatus | Enum | NOT NULL | ACTIVE / ROTATING / REVOKED |
| rotatedAt | Timestamp | NULLABLE | 最近轮换时间 |

#### 关联实体：MaskingRule

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | UUID | PK | |
| securityPolicyId | UUID | FK -> SecurityPolicy.id, NOT NULL | 所属策略 |
| dataType | String(64) | NOT NULL | 数据类型，如手机号、身份证 |
| ruleExpr | String(256) | NOT NULL | 脱敏规则表达式 |
| active | Boolean | NOT NULL, DEFAULT TRUE | 是否启用 |

#### 关联实体：RateLimitRule

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | UUID | PK | |
| securityPolicyId | UUID | FK -> SecurityPolicy.id, NOT NULL | 所属策略 |
| subjectType | Enum | NOT NULL | IP / USER / TENANT / API_CLIENT |
| windowSeconds | Integer | NOT NULL | 时间窗 |
| maxRequests | Integer | NOT NULL | 最大次数 |
| active | Boolean | NOT NULL, DEFAULT TRUE | 是否启用 |

**业务规则**：

- 安全策略拥有平台级策略与密钥引用，不直接拥有账号主数据。
- 密钥只保存引用和元数据，不在业务表中明文持久化密钥内容。
- 脱敏规则和密码策略属于平台通用能力，业务模块不应重复定义一套。
- 访问限频和会话超时策略的执行应可审计、可回滚、可灰度。

**领域事件**：

| 事件类型 | 触发时机 | 载荷 |
|----------|----------|------|
| `infra.security.policy-updated` | 安全策略更新 | policyCode, policyType |
| `infra.security.anomaly-detected` | 检测到异常行为 | policyCode, subjectType |

## 5. 核心规则与状态机

### 5.1 共享能力的所有权原则

- `00` 只拥有共享能力真相源，不拥有上层业务事实真相源。
- 业务模块可以引用 `00` 的策略、资源和运行对象，但不能绕过 `00` 自建平替。
- `00` 的配置、资源、策略和日志都应支持租户维度隔离或全局维度共享。

### 5.2 租户与组织分离原则

- `tenantId` 只用于系统级隔离，不参与组织授权主体计算。
- `TenantProfile` 不拥有组织结构、部门结构、岗位关系和身份上下文。
- `01` 可以引用租户，但组织与身份模型归 `01` 所有。

### 5.3 事件可靠性原则

- 所有跨模块事件必须具备 `eventId`、`eventType`、`traceId`、`tenantId` 等统一字段。
- 发布与业务事务的一致性通过 Outbox 或等价机制保证。
- 投递失败进入重试后，仍需保留审计与人工补偿入口。
- 事件回放只能重新驱动消费，不应重演发布方业务事务。

### 5.4 配置与缓存原则

- 配置变更先修改 `ConfigEntry` / `ConfigOverride` 真相源，再触发缓存失效。
- 缓存刷新由配置、事件或人工触发，不允许业务模块跨域操作底层缓存实现细节。
- 热更新只影响标记为 `mutableAtRuntime=TRUE` 的配置项。

### 5.5 审计与安全原则

- 配置、租户、安全策略、关键资源变更都必须进入统一审计。
- 审计记录 append-only，不允许修改；敏感字段展示走脱敏规则。
- 安全异常检测结果应可被审计、告警和关联链路追踪消费。

## 6. 事件模型

### 6.1 `00` 内部与对外事件

| 事件类型 | 说明 |
|----------|------|
| `infra.event.schema-registered` | 注册事件定义 |
| `infra.event.published` | 事件已成功进入总线 |
| `infra.event.delivery-failed` | 事件投递失败 |
| `infra.event.dead-lettered` | 事件进入死信 |
| `infra.i18n.bundle-updated` | 资源包更新 |
| `infra.timezone.setting-changed` | 时区设置变更 |
| `infra.translation.updated` | 多语言翻译项更新 |
| `infra.attachment.created` | 附件上传并建档成功 |
| `infra.dictionary.updated` | 字典更新 |
| `infra.config.updated` | 配置变更 |
| `infra.feature-flag.changed` | 功能开关变更 |
| `infra.scheduler.task-failed` | 定时任务失败 |
| `infra.audit.archived` | 审计归档完成 |
| `infra.cache.invalidated` | 缓存失效 |
| `infra.tenant.created` | 租户创建 |
| `infra.tenant.initialized` | 租户初始化完成 |
| `infra.error-code.updated` | 错误码更新 |
| `infra.security.policy-updated` | 安全策略更新 |
| `infra.security.anomaly-detected` | 安全异常检测 |

### 6.2 关键消费方

| 事件类型 | 典型消费方 |
|----------|------------|
| `infra.config.updated` | 各业务模块、门户聚合缓存、前端配置拉取层 |
| `infra.feature-flag.changed` | 门户、流程、消息、开放接口控制层 |
| `infra.dictionary.updated` | 表单、考勤、业务应用选择器 |
| `infra.attachment.created` | 业务域附件绑定、审计记录 |
| `infra.security.anomaly-detected` | 安全审计、运维告警、消息触达 |
| `infra.tenant.initialized` | 启动模块、默认模板/字典/配置初始化逻辑 |

## 7. 跨模块依赖

| 模块关系 | 依赖内容 | 说明 |
|----------|----------|------|
| `00 -> 01` | 主体引用、身份上下文、审计操作人 | 只引用主体 ID 和身份，不拥有主数据 |
| `01/02/03/04/05/06/07 -> 00` | 事件总线、审计、配置、附件、字典、错误码、国际化等 | 所有业务模块统一依赖 |
| `00 -/-> 业务主模型` | 不反向拥有业务真相源 | 共享能力不侵入业务语义 |

## 8. 数据库设计建议

### 8.1 核心表清单

| 表名 | 对应聚合根/实体 | 说明 |
|------|-----------------|------|
| `infra_event_definition` | EventDefinition | 事件定义表 |
| `infra_event_subscription` | SubscriptionBinding | 事件订阅表 |
| `infra_event_message` | EventMessage | 事件消息表 |
| `infra_event_delivery_attempt` | DeliveryAttempt | 投递尝试表 |
| `infra_locale_bundle` | LocaleBundle | 资源包表 |
| `infra_locale_resource_entry` | LocaleResourceEntry | 资源项表 |
| `infra_timezone_setting` | TimezoneSetting | 时区设置表 |
| `infra_translation_entry` | TranslationEntry | 多语言翻译表 |
| `infra_attachment_asset` | AttachmentAsset | 附件主表 |
| `infra_attachment_version` | AttachmentVersion | 附件版本表 |
| `infra_attachment_binding` | AttachmentBinding | 附件绑定表 |
| `infra_dictionary_type` | DictionaryType | 字典类型表 |
| `infra_dictionary_item` | DictionaryItem | 字典项表 |
| `infra_config_entry` | ConfigEntry | 配置项表 |
| `infra_config_override` | ConfigOverride | 配置覆盖表 |
| `infra_feature_rule` | FeatureRule | 功能开关规则表 |
| `infra_scheduled_job` | ScheduledJob | 任务定义表 |
| `infra_job_execution_record` | JobExecutionRecord | 任务执行记录表 |
| `infra_audit_record` | AuditRecord | 审计记录表 |
| `infra_audit_field_change` | AuditFieldChange | 审计字段差异表 |
| `infra_cache_policy` | CachePolicy | 缓存策略表 |
| `infra_cache_invalidation` | CacheInvalidationRecord | 缓存失效轨迹表 |
| `infra_tenant_profile` | TenantProfile | 租户表 |
| `infra_tenant_quota` | TenantQuota | 租户配额表 |
| `infra_error_code_def` | ErrorCodeDefinition | 错误码表 |
| `infra_security_policy` | SecurityPolicy | 安全策略表 |
| `infra_secret_key_material` | SecretKeyMaterial | 密钥元数据表 |
| `infra_masking_rule` | MaskingRule | 脱敏规则表 |
| `infra_rate_limit_rule` | RateLimitRule | 限频规则表 |

### 8.2 索引建议

- `infra_event_definition`：`(eventType, version)`、`(modulePrefix, status)`
- `infra_event_message`：`(eventType, publishStatus, createdAt)`、`(tenantId, correlationId)`、`(traceId)`
- `infra_event_delivery_attempt`：`(eventMessageId, subscriberCode, attemptNo)`、`(deliveryStatus, nextRetryAt)`
- `infra_locale_bundle`：`(bundleCode, locale)`、`(moduleCode, status)`
- `infra_timezone_setting`：`(scopeType, scopeId, active)`
- `infra_translation_entry`：`(entityType, entityId, fieldName, locale)`、`(tenantId, locale)`
- `infra_attachment_asset`：`(tenantId, storageKey)`、`(checksum)`、`(previewStatus)`
- `infra_attachment_binding`：`(businessType, businessId, active)`
- `infra_dictionary_type`：`(code)`、`(tenantId, status)`
- `infra_dictionary_item`：`(dictionaryTypeId, itemCode)`、`(parentItemId, sortOrder)`
- `infra_config_entry`：`(configKey)`、`(status, tenantAware)`
- `infra_config_override`：`(configEntryId, scopeType, scopeId)`
- `infra_scheduled_job`：`(jobCode)`、`(status, triggerType)`
- `infra_job_execution_record`：`(scheduledJobId, startedAt)`、`(executionStatus, startedAt)`
- `infra_audit_record`：`(moduleCode, objectType, objectId)`、`(operatorPersonId, occurredAt)`、`(tenantId, occurredAt)`
- `infra_cache_policy`：`(namespace, active)`
- `infra_tenant_profile`：`(tenantCode)`、`(status, initialized)`
- `infra_error_code_def`：`(code)`、`(moduleCode, severity)`
- `infra_security_policy`：`(policyCode)`、`(policyType, status)`

### 8.3 大字段与保留建议

- `payloadSchema`、`payload`、`validationRule`、`retryPolicy`、`configSnapshot`、`executionLog`、`audit` 差异等建议采用 JSON 文本或 `NVARCHAR(MAX)` 存储。
- 事件消息、审计记录和任务执行记录都需要分层保留策略，避免在线表无限膨胀。
- 附件实体和版本实体应尽量只保存元数据，实际二进制内容走对象存储。

## 9. 一期建模优先级建议

### 9.1 一期必须先落的聚合

- `EventDefinition`
- `EventMessage`
- `LocaleBundle`
- `TimezoneSetting`
- `TranslationEntry`
- `AttachmentAsset`
- `DictionaryType`
- `ConfigEntry`
- `AuditRecord`
- `ErrorCodeDefinition`

### 9.2 一期可简化实现的部分

- `ScheduledJob` 先支持基础 Cron 和手工触发，不做复杂 DAG 编排
- `CachePolicy` 先支持 Redis 命名空间和基础 TTL，不做复杂多级缓存监控
- `TenantProfile` 先支持共享库 + 租户字段模式，不做独立库自动迁移
- `SecurityPolicy` 先支持基础密钥引用、脱敏规则和密码策略，不做复杂自适应风控

### 9.3 后续可继续补强的方向

- 事件回放控制台和精细化订阅治理
- 配置灰度发布与回滚工作台
- 缓存依赖图与热点分析
- 租户初始化模板市场
- 高级安全风控与异常行为画像

## 10. 结论

`00-平台基础设施` 的核心不在“列出一堆公共工具”，而在于建立一套**可注册、可治理、可审计、可隔离、可复用**的共享能力真相源。其建模重点应围绕以下原则展开：

- 用 `EventDefinition` 和 `EventMessage` 管事件契约与投递生命周期
- 用 `ConfigEntry`、`CachePolicy`、`SecurityPolicy` 管平台策略真相源
- 用 `AttachmentAsset`、`DictionaryType`、`LocaleBundle`、`TranslationEntry` 管共享资源与共享语义
- 用 `AuditRecord`、`JobExecutionRecord` 等运行对象保证可追溯性
- 用 `TenantProfile` 保持系统隔离边界，但不侵入组织与权限模型

该文档可直接作为后续 `HJO2OA-Infrastructure/docs/module-design.md`、共享基础设施建表、共享组件设计和工程骨架实现的基础。
