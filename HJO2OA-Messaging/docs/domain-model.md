# 06-消息移动与生态 领域模型

## 1. 文档目的

本文档细化 `06-消息移动与生态` 模块的核心领域实体、值对象、聚合根、实体关系和业务规则，作为后端工程建模、数据库设计、接口契约和事件契约的统一依据。

对应架构决策编号：D04（`06` 与 `07` 模块边界）、D08（身份上下文）、D13（最终一致性）、D16（待办与消息分工）、D20（一期实施范围）。

## 2. 领域定位与边界

### 2.1 领域职责

`06-消息移动与生态` 负责平台面向用户的触达、提醒、移动入口和生态接入能力，核心职责包括：

- 将 `01`、`02`、`03`、`05` 等模块发布的业务事件转换为用户可感知的消息与提醒
- 统一管理站内消息、外部渠道发送、送达追踪、失败重试和触达审计
- 维护用户订阅偏好、静默规则、升级策略和移动端设备接入状态
- 承担企业微信、钉钉、邮件、短信、SSO、LDAP 等生态连接配置与状态管理

### 2.2 关键边界

- **消息不是待办**：待办是事务处理视图，由 `02-todo-center` 持有；消息是通知触达视图，由 `06-message-center` 持有。
- **触达不是数据开放**：`06` 负责用户触达、移动入口和生态登录接入；开放 API、Webhook、通用连接器、跨系统数据交换和服务治理由 `07-数据服务与集成` 负责。
- **移动端不是通用聚合平台**：`06-mobile-support` 仅面向移动端高频场景提供轻量聚合与设备风控，不承载平台级通用读模型。
- **生态接入不等于组织同步所有权**：企业微信、钉钉、LDAP、SSO 的连接配置与登录接入在 `06`；组织/人员主数据同步的最终落表、审计和差异治理由 `01-org-sync-audit` 或 `07-data-sync` 承担。

### 2.3 一期收敛口径

一期最小闭环优先实现以下能力：

- `message-center`：统一消息中心
- `channel-sender`：渠道发送与送达追踪
- `event-subscription`：待办提醒与事件订阅
- `mobile-support`：移动端设备接入与高频接口支撑

`ecosystem` 在一期可按项目需要接入最少一种生态通道，但不作为最小闭环验收前提。

## 3. 领域总览

### 3.1 聚合根

| 聚合根 | 说明 | 目录 |
|--------|------|------|
| Notification | 面向单个接收人的统一消息实体，承载站内消息视图和读取状态 | `message-center/` |
| MessageTemplate | 消息模板，按渠道、语言和版本管理渲染规则 | `channel-sender/` |
| ChannelEndpoint | 渠道接入配置，承载邮件、短信、企微、钉钉、Push 等连接参数 | `channel-sender/` |
| RoutingPolicy | 渠道路由策略，决定不同消息类别和优先级的发送路径 | `channel-sender/` |
| DeliveryTask | 单条消息在单一渠道上的投递任务与重试状态 | `channel-sender/` |
| SubscriptionRule | 业务事件到提醒场景的映射规则 | `event-subscription/` |
| SubscriptionPreference | 用户订阅偏好、静默规则、摘要策略和升级偏好 | `event-subscription/` |
| DeviceBinding | 移动设备绑定与 Push Token 注册信息 | `mobile-support/` |
| MobileSession | 面向移动端的设备会话与风险控制影子状态 | `mobile-support/` |
| EcosystemIntegration | 生态接入配置与连接健康状态 | `ecosystem/` |

### 3.2 核心实体关系

```text
SubscriptionRule ──1:N──> Notification             (业务事件命中订阅规则后生成消息)
Notification     ──1:N──> DeliveryTask             (一条消息可走多个渠道)
DeliveryTask     ──1:N──> DeliveryAttempt          (单渠道多次重试投递)
Notification     ──1:N──> NotificationAction       (已读/归档/撤回等动作轨迹)
MessageTemplate  ──1:N──> DeliveryTask             (投递时引用模板版本)
ChannelEndpoint  ──1:N──> DeliveryTask             (投递到具体渠道端点)
RoutingPolicy    ──1:N──> DeliveryTask             (路由规则产出渠道任务)
SubscriptionPreference ──1:N──> ChannelOverride    (按用户覆盖默认渠道偏好)
Person           ──1:N──> DeviceBinding            (一个人可绑定多个设备)
DeviceBinding    ──1:N──> MobileSession            (一个设备可存在多个历史会话)
EcosystemIntegration ──1:N──> CallbackAuditRecord  (生态回调/联机记录)
```

### 3.3 核心业务流

```text
业务事件（todo/process/content/biz/org）
  -> SubscriptionRule 命中
  -> 生成 Notification
  -> RoutingPolicy 决定渠道
  -> 生成 DeliveryTask
  -> ChannelEndpoint 投递
  -> DeliveryAttempt 记录结果
  -> 用户在 PC/移动端读取 Notification
```

## 4. 核心聚合定义

### 4.1 Notification（统一消息）

消息是面向**单个接收人**的统一通知实体，不做“多人共享一条消息”的聚合设计。这样可以天然支持每个人独立的已读、归档、撤回、过期和身份范围控制。

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | UUID | PK | 消息唯一标识 |
| category | Enum | NOT NULL | 消息类别：TODO_REMINDER / PROCESS_NOTICE / SYSTEM / ANNOUNCEMENT / MEETING / SCHEDULE / TASK / WARNING / SECURITY |
| sourceModule | String(64) | NOT NULL | 来源模块，如 `todo-center`、`content-lifecycle` |
| sourceEventType | String(128) | NOT NULL | 触发该消息的业务事件类型 |
| sourceBusinessId | String(64) | NULLABLE | 来源业务对象 ID |
| recipientPersonId | UUID | FK -> Person.id, NOT NULL | 接收人 |
| targetAssignmentId | UUID | FK -> Assignment.id, NULLABLE | 目标任职关系，身份相关消息必填 |
| targetPositionId | UUID | FK -> Position.id, NULLABLE | 目标岗位，身份相关消息必填 |
| title | String(256) | NOT NULL | 消息标题 |
| bodySummary | String(2000) | NOT NULL | 消息摘要 |
| deepLink | String(512) | NULLABLE | 跳转链接，面向门户或移动端场景 |
| priority | Enum | NOT NULL, DEFAULT NORMAL | 优先级：NORMAL / URGENT / CRITICAL |
| inboxStatus | Enum | NOT NULL, DEFAULT UNREAD | 站内状态：UNREAD / READ / ARCHIVED / REVOKED / EXPIRED |
| deliveryStatus | Enum | NOT NULL, DEFAULT PENDING | 触达状态：PENDING / PARTIAL_SUCCESS / SUCCESS / FAILED |
| dedupKey | String(256) | NULLABLE | 去重键，如 `todo:{taskId}:{personId}:{channelScope}` |
| relatedTemplateCode | String(64) | NULLABLE | 生成时使用的模板编码 |
| readAt | Timestamp | NULLABLE | 已读时间 |
| expiredAt | Timestamp | NULLABLE | 过期时间 |
| tenantId | UUID | FK -> Tenant.id, NOT NULL | 所属租户 |
| createdAt | Timestamp | NOT NULL | |
| updatedAt | Timestamp | NOT NULL | |

#### 关联实体：NotificationAction（消息动作轨迹）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | UUID | PK | |
| notificationId | UUID | FK -> Notification.id, NOT NULL | 消息 ID |
| actionType | Enum | NOT NULL | READ / ARCHIVE / REVOKE / EXPIRE / RESTORE |
| operatorPersonId | UUID | FK -> Person.id, NOT NULL | 操作人 |
| reason | String(512) | NULLABLE | 原因 |
| createdAt | Timestamp | NOT NULL | |

**业务规则**：

- 消息以“单接收人单消息”为粒度创建；群发场景在应用层拆分为多条 `Notification`。
- 站内读取状态与外部渠道送达状态分离建模，避免“已送达但未已读”与“站内已读但外部渠道失败”相互污染。
- 对待办提醒、身份相关提醒等场景，`targetAssignmentId` 与 `targetPositionId` 必须快照写入，保证身份切换后的可见性判定有据可依。
- `deepLink` 只能指向门户页、移动页或业务详情入口，不直接暴露内部数据表结构。
- 同一业务事件重复到达时，优先通过 `dedupKey` 去重，避免重复轰炸用户。
- 消息撤回、过期后仍保留审计轨迹，不做物理删除。

**内部领域事件**：

| 事件类型 | 触发时机 | 载荷 |
|----------|----------|------|
| `msg.message.created` | 消息成功入箱 | notificationId, recipientPersonId, category, priority |
| `msg.message.read` | 消息已读 | notificationId, recipientPersonId, readAt |
| `msg.message.revoked` | 消息撤回 | notificationId, reason |
| `msg.message.expired` | 消息过期 | notificationId, expiredAt |

### 4.2 MessageTemplate（消息模板）

模板负责把业务事件或业务对象快照渲染为面向具体渠道的消息文本。

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | UUID | PK | 模板唯一标识 |
| code | String(64) | NOT NULL | 模板编码，租户内唯一 |
| name | String(128) | NOT NULL | 模板名称 |
| category | Enum | NOT NULL | 适用消息类别 |
| channelType | Enum | NOT NULL | INBOX / EMAIL / SMS / WECHAT_WORK / DINGTALK / APP_PUSH |
| locale | String(16) | NOT NULL | 语言，如 `zh-CN` |
| version | Integer | NOT NULL, DEFAULT 1 | 版本号 |
| status | Enum | NOT NULL | DRAFT / PUBLISHED / DEPRECATED |
| titleTemplate | String(512) | NULLABLE | 标题模板 |
| bodyTemplate | Text | NOT NULL | 正文模板 |
| actionLinkTemplate | String(512) | NULLABLE | 跳转链接模板 |
| variableSchema | JSON | NULLABLE | 变量定义、示例和必填约束 |
| tenantId | UUID | FK -> Tenant.id, NOT NULL | 所属租户 |
| publishedAt | Timestamp | NULLABLE | 发布时间 |
| createdAt | Timestamp | NOT NULL | |
| updatedAt | Timestamp | NOT NULL | |

**UK 约束**：`(code, channelType, locale, version)`

**业务规则**：

- 模板采用版本链管理，已发布版本不可修改，只能创建新版本。
- 渠道渲染结果必须在发送时快照化，后续模板修改不得反向影响历史消息内容。
- 模板变量必须显式定义，避免业务事件载荷与模板依赖关系隐式扩散。
- 对于安全告警、账号锁定等高风险场景，应支持系统级不可关闭模板。

### 4.3 ChannelEndpoint（渠道端点）

渠道端点描述消息发送使用的实际通道配置。

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | UUID | PK | 端点唯一标识 |
| endpointCode | String(64) | UK, NOT NULL | 端点编码 |
| channelType | Enum | NOT NULL | INBOX / EMAIL / SMS / WECHAT_WORK / DINGTALK / APP_PUSH |
| providerType | Enum | NOT NULL | INTERNAL / SMTP / ALIYUN_SMS / TENCENT_SMS / WECHAT_WORK / DINGTALK / FCM / APNS |
| displayName | String(128) | NOT NULL | 端点名称 |
| status | Enum | NOT NULL, DEFAULT ENABLED | ENABLED / DISABLED / DEGRADED |
| configRef | String(128) | NOT NULL | 配置引用标识，指向 `00-config` 或密钥仓库 |
| rateLimitPerMinute | Integer | NULLABLE | 每分钟速率限制 |
| dailyQuota | Integer | NULLABLE | 每日配额 |
| tenantId | UUID | FK -> Tenant.id, NOT NULL | 所属租户 |
| createdAt | Timestamp | NOT NULL | |
| updatedAt | Timestamp | NOT NULL | |

**业务规则**：

- 端点配置只保存引用，不保存明文密钥、App Secret、SMTP 密码等敏感信息。
- `INBOX` 为平台内建逻辑端点，一期必须可用。
- 外部端点不可用时允许路由降级到其他渠道，但不得影响站内消息入箱。

### 4.4 RoutingPolicy（渠道路由策略）

路由策略负责决定“什么消息走什么渠道，以什么顺序和什么升级规则发送”。

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | UUID | PK | 策略唯一标识 |
| policyCode | String(64) | UK, NOT NULL | 策略编码 |
| category | Enum | NOT NULL | 适用消息类别 |
| priorityThreshold | Enum | NOT NULL | 触发阈值：NORMAL / URGENT / CRITICAL |
| targetChannelOrder | JSON | NOT NULL | 首选渠道顺序列表 |
| fallbackChannelOrder | JSON | NULLABLE | 失败降级渠道顺序 |
| quietWindowBehavior | Enum | NOT NULL | DEFER / BYPASS / SUPPRESS |
| dedupWindowSeconds | Integer | NOT NULL, DEFAULT 300 | 去重时间窗 |
| escalationPolicy | JSON | NULLABLE | 升级规则，如 10 分钟未读后短信升级 |
| enabled | Boolean | NOT NULL, DEFAULT TRUE | 是否启用 |
| tenantId | UUID | FK -> Tenant.id, NOT NULL | 所属租户 |
| createdAt | Timestamp | NOT NULL | |
| updatedAt | Timestamp | NOT NULL | |

**业务规则**：

- 一期默认所有消息必须至少进入 `INBOX`，外部渠道为可选增强触达。
- `CRITICAL` 级消息可绕过静默规则，并按升级策略触发更强渠道。
- 路由策略先于个人偏好生效，但必须允许在非强制场景下被个人订阅偏好部分覆盖。
- 同一类别消息在去重时间窗内若 `dedupKey` 一致，应合并或抑制重复发送。

### 4.5 DeliveryTask（投递任务）

投递任务描述一条消息在单一渠道上的发送生命周期。

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | UUID | PK | 投递任务唯一标识 |
| notificationId | UUID | FK -> Notification.id, NOT NULL | 对应消息 |
| channelType | Enum | NOT NULL | 投递渠道 |
| endpointId | UUID | FK -> ChannelEndpoint.id, NULLABLE | 渠道端点，`INBOX` 可为空 |
| routeOrder | Integer | NOT NULL | 路由顺序 |
| status | Enum | NOT NULL | PENDING / SENDING / DELIVERED / FAILED / GAVE_UP / CANCELLED |
| retryCount | Integer | NOT NULL, DEFAULT 0 | 重试次数 |
| nextRetryAt | Timestamp | NULLABLE | 下次重试时间 |
| providerMessageId | String(128) | NULLABLE | 外部渠道消息 ID |
| lastErrorCode | String(64) | NULLABLE | 最近错误码 |
| lastErrorMessage | String(512) | NULLABLE | 最近错误信息 |
| deliveredAt | Timestamp | NULLABLE | 送达时间 |
| tenantId | UUID | FK -> Tenant.id, NOT NULL | 所属租户 |
| createdAt | Timestamp | NOT NULL | |
| updatedAt | Timestamp | NOT NULL | |

#### 关联实体：DeliveryAttempt（投递尝试）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | UUID | PK | |
| deliveryTaskId | UUID | FK -> DeliveryTask.id, NOT NULL | 所属投递任务 |
| attemptNo | Integer | NOT NULL | 第几次尝试 |
| requestPayloadSnapshot | JSON | NULLABLE | 发送载荷快照 |
| providerResponse | JSON | NULLABLE | 渠道返回 |
| resultStatus | Enum | NOT NULL | SUCCESS / FAILED / TIMEOUT |
| errorCode | String(64) | NULLABLE | 渠道错误码 |
| errorMessage | String(512) | NULLABLE | 错误信息 |
| requestedAt | Timestamp | NOT NULL | 发起时间 |
| completedAt | Timestamp | NULLABLE | 完成时间 |

**业务规则**：

- 一条 `Notification` 可生成多个 `DeliveryTask`，例如 `INBOX + EMAIL`。
- 外部渠道发送失败不得回滚站内消息入箱；触达失败只影响 `deliveryStatus` 和渠道重试。
- 重试策略遵循《最终一致性与补偿机制文档》的默认退避参数：`1s -> 5s -> 30s -> 5m -> 30m`。
- 达到最大重试次数后进入 `GAVE_UP`，必须保留人工补偿入口。
- `providerMessageId` 用于与外部平台回执、已送达回调、失败回调做关联。

**内部领域事件**：

| 事件类型 | 触发时机 | 载荷 |
|----------|----------|------|
| `msg.channel.delivery-requested` | 生成渠道投递任务 | deliveryTaskId, notificationId, channelType |
| `msg.channel.delivered` | 渠道确认送达 | deliveryTaskId, notificationId, channelType, deliveredAt |
| `msg.channel.failed` | 渠道投递失败 | deliveryTaskId, notificationId, channelType, retryCount, errorCode |

### 4.6 SubscriptionRule（事件订阅规则）

订阅规则负责把平台业务事件映射成消息场景，是 `event-subscription` 的核心模型。

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | UUID | PK | 规则唯一标识 |
| ruleCode | String(64) | UK, NOT NULL | 规则编码 |
| eventTypePattern | String(128) | NOT NULL | 事件模式，支持精确和通配匹配 |
| notificationCategory | Enum | NOT NULL | 转换后的消息类别 |
| targetResolverType | Enum | NOT NULL | PAYLOAD_PERSON / PAYLOAD_ASSIGNMENT / INITIATOR / ROLE_MEMBERS / ORG_MEMBERS / EXPRESSION |
| targetResolverConfig | JSON | NULLABLE | 目标解析配置 |
| templateCode | String(64) | NOT NULL | 默认模板编码 |
| conditionExpr | String(1024) | NULLABLE | 触发条件表达式 |
| priorityMapping | JSON | NULLABLE | 事件优先级映射规则 |
| enabled | Boolean | NOT NULL, DEFAULT TRUE | 是否启用 |
| tenantId | UUID | FK -> Tenant.id, NOT NULL | 所属租户 |
| createdAt | Timestamp | NOT NULL | |
| updatedAt | Timestamp | NOT NULL | |

**业务规则**：

- 规则只消费统一事件契约中已注册的业务事件，不依赖发布方私有模型。
- 一个事件可命中多条规则，但必须经过去重键计算，避免产生重复提醒。
- 对待办相关事件，目标解析应优先取 `assignmentId/positionId`，保证消息与身份上下文对齐。
- 订阅规则是提醒生成规则，不承担通用数据交换职责。

### 4.7 SubscriptionPreference（订阅偏好）

订阅偏好描述用户在不同消息场景下的触达接受策略。

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | UUID | PK | 偏好唯一标识 |
| personId | UUID | FK -> Person.id, NOT NULL | 所属人员 |
| category | Enum | NOT NULL | 消息类别 |
| allowedChannels | JSON | NOT NULL | 用户允许的渠道集合 |
| quietWindow | JSON | NULLABLE | 静默时间窗，如 `22:00-08:00` |
| digestMode | Enum | NOT NULL, DEFAULT IMMEDIATE | IMMEDIATE / PERIODIC_DIGEST / DISABLED |
| escalationOptIn | Boolean | NOT NULL, DEFAULT TRUE | 是否允许升级触达 |
| muteNonWorkingHours | Boolean | NOT NULL, DEFAULT FALSE | 是否在非工作时间静默 |
| enabled | Boolean | NOT NULL, DEFAULT TRUE | 是否启用 |
| tenantId | UUID | FK -> Tenant.id, NOT NULL | 所属租户 |
| createdAt | Timestamp | NOT NULL | |
| updatedAt | Timestamp | NOT NULL | |

#### 值对象：ChannelOverride

| 字段 | 类型 | 说明 |
|------|------|------|
| channelType | Enum | 具体渠道 |
| enabled | Boolean | 是否启用该渠道 |
| maxFrequencyPerDay | Integer | 单日最大发送次数 |
| bypassForCritical | Boolean | CRITICAL 是否允许绕过 |

**业务规则**：

- 用户偏好只能覆盖“非强制触达”部分，安全告警、账号锁定等系统强制类消息不可被完全静默。
- 个人偏好优先级低于系统强制策略，高于普通默认路由策略。
- 静默策略应影响外部渠道和移动 Push，不影响站内消息留存。

### 4.8 DeviceBinding（设备绑定）

设备绑定是移动端能力的基础，用于维护设备身份、Push Token 和设备风险状态。

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | UUID | PK | 设备绑定唯一标识 |
| personId | UUID | FK -> Person.id, NOT NULL | 设备所属人 |
| accountId | UUID | FK -> Account.id, NOT NULL | 登录账号 |
| deviceId | String(128) | NOT NULL | 设备唯一标识 |
| deviceFingerprint | String(256) | NULLABLE | 指纹摘要 |
| platform | Enum | NOT NULL | IOS / ANDROID / H5 / PWA |
| appType | Enum | NOT NULL | NATIVE_APP / WECHAT_H5 / DINGTALK_H5 / ENTERPRISE_APP / PWA |
| pushToken | String(512) | NULLABLE | 推送 Token |
| bindStatus | Enum | NOT NULL | ACTIVE / REVOKED / LOST / DISABLED |
| riskLevel | Enum | NOT NULL, DEFAULT LOW | LOW / MEDIUM / HIGH |
| lastLoginAt | Timestamp | NULLABLE | 最近登录时间 |
| lastSeenAt | Timestamp | NULLABLE | 最近活跃时间 |
| tenantId | UUID | FK -> Tenant.id, NOT NULL | 所属租户 |
| createdAt | Timestamp | NOT NULL | |
| updatedAt | Timestamp | NOT NULL | |

**业务规则**：

- 同一 `deviceId` 在同一租户下只能存在一个活动绑定。
- 设备标记为 `LOST` 或 `REVOKED` 后，关联 Push Token 必须立即失效。
- 设备绑定只描述终端接入，不拥有身份上下文真值；身份真值仍由 `01-identity-context` 输出。

### 4.9 MobileSession（移动会话）

移动会话用于承载设备级会话状态和移动端续期控制，是 `00-安全工具` 会话体系在移动端场景下的影子模型。

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | UUID | PK | 会话唯一标识 |
| deviceBindingId | UUID | FK -> DeviceBinding.id, NOT NULL | 所属设备 |
| personId | UUID | FK -> Person.id, NOT NULL | 当前人员 |
| accountId | UUID | FK -> Account.id, NOT NULL | 当前账号 |
| currentAssignmentId | UUID | FK -> Assignment.id, NULLABLE | 当前任职关系快照 |
| currentPositionId | UUID | FK -> Position.id, NULLABLE | 当前岗位快照 |
| sessionStatus | Enum | NOT NULL | ACTIVE / EXPIRED / REVOKED / RISK_FROZEN |
| issuedAt | Timestamp | NOT NULL | 签发时间 |
| expiresAt | Timestamp | NOT NULL | 过期时间 |
| lastHeartbeatAt | Timestamp | NULLABLE | 最近续期时间 |
| refreshVersion | Integer | NOT NULL, DEFAULT 0 | 续期版本 |
| tenantId | UUID | FK -> Tenant.id, NOT NULL | 所属租户 |
| createdAt | Timestamp | NOT NULL | |
| updatedAt | Timestamp | NOT NULL | |

**业务规则**：

- 会话中的 `currentAssignmentId`、`currentPositionId` 是移动端快速判定所需快照，不替代 `01` 的权威身份计算结果。
- 身份切换后，移动端会话应同步刷新当前身份快照，并触发待办/消息数量刷新。
- 风险控制可将会话冻结为 `RISK_FROZEN`，但不得直接删除设备绑定。
- `mobile-support` 本身不发布跨模块领域事件，以 API 和缓存刷新为主。

### 4.10 EcosystemIntegration（生态接入配置）

生态接入配置负责统一维护企业微信、钉钉、邮件、短信、SSO、LDAP 等外部接入配置与健康状态。

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | UUID | PK | 接入配置唯一标识 |
| integrationType | Enum | NOT NULL | WECHAT_WORK / DINGTALK / EMAIL / SMS / SSO / LDAP |
| displayName | String(128) | NOT NULL | 接入名称 |
| authMode | Enum | NULLABLE | OAUTH2 / SIGNATURE / BASIC / API_KEY / LDAP_BIND |
| callbackUrl | String(512) | NULLABLE | 回调地址 |
| signAlgorithm | String(64) | NULLABLE | 签名算法 |
| configRef | String(128) | NOT NULL | 外部配置引用 |
| status | Enum | NOT NULL | DRAFT / ENABLED / DISABLED / ERROR |
| healthStatus | Enum | NOT NULL | UNKNOWN / HEALTHY / DEGRADED / UNREACHABLE |
| lastCheckAt | Timestamp | NULLABLE | 最近检测时间 |
| lastErrorSummary | String(512) | NULLABLE | 最近错误摘要 |
| tenantId | UUID | FK -> Tenant.id, NOT NULL | 所属租户 |
| createdAt | Timestamp | NOT NULL | |
| updatedAt | Timestamp | NOT NULL | |

#### 关联实体：CallbackAuditRecord

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | UUID | PK | |
| integrationId | UUID | FK -> EcosystemIntegration.id, NOT NULL | 所属集成 |
| callbackType | String(64) | NOT NULL | 回调类型 |
| verifyResult | Enum | NOT NULL | PASSED / FAILED |
| payloadSummary | JSON | NULLABLE | 回调摘要 |
| errorMessage | String(512) | NULLABLE | 错误信息 |
| occurredAt | Timestamp | NOT NULL | 发生时间 |

**业务规则**：

- `EcosystemIntegration` 只拥有连接配置、健康状态和回调审计，不拥有第三方业务数据主数据所有权。
- 邮件、短信、企业微信、钉钉等渠道实际发送仍通过 `ChannelEndpoint` 和 `DeliveryTask` 执行；`EcosystemIntegration` 更偏向管理视角。
- LDAP/SSO 的连接与认证接入配置属于 `06`，但通用连接器治理和批量同步调度不属于 `06`。
- `ecosystem` 子模块默认不发布跨模块领域事件，以消费事件并对外触达为主。

## 5. 核心规则与状态机

### 5.1 待办与消息的分工

- `02-todo-center` 负责待办、已办、我发起、抄送、草稿等事务处理视图。
- `06-message-center` 负责通知触达、提醒留痕、已读状态和跨渠道送达。
- `todo.item.created` 可以触发消息提醒，但消息已读不代表待办已处理。
- 待办完成后可联动把相关提醒标为已失效或已归档，但不应回写改变待办事实状态。

### 5.2 渠道路由判定顺序

路由判定按以下顺序执行：

1. 系统强制规则
2. 租户级 `RoutingPolicy`
3. 用户 `SubscriptionPreference`
4. `ChannelEndpoint` 可用性与配额检查
5. 失败后的降级渠道

### 5.3 优先级、静默和升级

- `NORMAL`：默认遵守静默规则和摘要规则。
- `URGENT`：默认立即入箱，可按策略绕过摘要，必要时触发外部渠道。
- `CRITICAL`：默认绕过静默规则，并允许升级到更强渠道，如短信、企微或钉钉。
- 静默规则只影响主动触达，不影响消息在站内列表中的留存。

### 5.4 身份上下文集成

- 对任职相关消息，`Notification` 必须快照 `targetAssignmentId` 与 `targetPositionId`。
- 用户切换身份后，门户消息卡片、移动端未读数和列表过滤条件必须同步刷新。
- 已生成的消息不因用户后续切换身份而重新归属；消息的“生成时语义”以快照为准。
- 若当前身份不匹配消息目标任职，可在界面层隐藏为“非当前身份消息”，但底层数据不删除。

### 5.5 一致性与补偿

- `Notification` 创建与 Outbox 事件写入必须在同一事务中完成。
- `DeliveryTask` 失败时按统一退避策略重试，超过上限进入人工补偿队列。
- 外部渠道失败不得撤销已入箱消息；`deliveryStatus` 可为 `PARTIAL_SUCCESS`。
- 所有消息生成、送达、已读、失败与补偿动作都必须保留审计轨迹。

## 6. 事件模型

### 6.1 `06` 内部领域事件

| 事件类型 | 载荷关键字段 | 说明 |
|----------|-------------|------|
| `msg.message.created` | notificationId, recipientPersonId, category, priority | 消息入箱 |
| `msg.message.read` | notificationId, recipientPersonId, readAt | 消息已读 |
| `msg.message.revoked` | notificationId, reason | 消息撤回 |
| `msg.channel.delivery-requested` | deliveryTaskId, notificationId, channelType | 渠道投递启动 |
| `msg.channel.delivered` | deliveryTaskId, notificationId, channelType, deliveredAt | 渠道送达 |
| `msg.channel.failed` | deliveryTaskId, notificationId, channelType, retryCount, errorCode | 渠道失败 |

### 6.2 跨模块总线事件口径

为与 `docs/contracts/unified-event-contract.md` 保持一致，跨模块总线统一注册 `msg.notification.*` 事件；`msg.message.*` 与 `msg.channel.*` 可作为 `06` 模块内部领域事件命名。

| 总线事件类型 | 触发时机 | payload 关键字段 |
|--------------|----------|------------------|
| `msg.notification.sent` | 某渠道投递任务被系统接受；`channel=INBOX` 表示站内消息成功入箱 | notificationId, recipientId, channel, category |
| `msg.notification.delivered` | 渠道确认送达 | notificationId, channel, deliveredAt |
| `msg.notification.read` | 接收人已读站内消息 | notificationId, recipientId |

### 6.3 入站业务事件

`06` 重点消费以下业务事件：

| 事件类型 | 处理方式 | 说明 |
|----------|----------|------|
| `todo.item.created` | 生成待办提醒消息 | 一期主链路 |
| `todo.item.overdue` | 生成催办/升级提醒 | 一期主链路 |
| `process.task.overdue` | 生成超时提醒 | 一期主链路 |
| `process.instance.completed` | 生成流程结果通知 | 一期可选 |
| `content.article.published` | 生成公告发布提醒 | 二期主链路 |
| `biz.meeting.created` | 生成会议提醒 | 三期业务接入 |
| `biz.attendance.result-changed` | 生成考勤异常提醒 | 三期业务接入 |
| `org.account.locked` | 生成安全告警消息 | 平台安全场景 |
| `org.identity.switched` | 刷新未读数与身份相关缓存 | 不重新生成消息 |
| `org.identity-context.invalidated` | 失效身份相关缓存，并按回退身份重建或强制退出当前会话 | 不重新生成消息 |

## 7. 跨模块依赖

| 模块关系 | 依赖内容 | 说明 |
|----------|----------|------|
| `06 -> 00` | 事件总线、配置中心、安全工具、审计日志、缓存、国际化 | 基础设施依赖 |
| `06 -> 01` | Person、Account、Assignment、IdentityContext | 接收人解析、身份快照与风控依赖 |
| `02 -> 06` | `todo.*`、`process.*` 事件 | 06 消费后生成提醒与触达 |
| `03 -> 06` | 内容发布与内容权限事件 | 二期内容提醒依赖 |
| `04 -> 06` | 未读消息数、最新消息列表、消息卡片跳转 | 门户读模型消费 06 输出 |
| `05 -> 06` | 会议、日程、考勤、合同等业务事件 | 06 统一生成业务提醒 |
| `06 -> 07` | 无直接主数据依赖 | 开放 API、通用连接器、跨系统同步归 07 |

## 8. 数据库设计建议

### 8.1 核心表清单

| 表名 | 对应聚合根/实体 | 说明 |
|------|-----------------|------|
| `msg_notification` | Notification | 消息主表 |
| `msg_notification_action` | NotificationAction | 消息动作轨迹表 |
| `msg_template` | MessageTemplate | 模板版本主表 |
| `msg_channel_endpoint` | ChannelEndpoint | 渠道端点表 |
| `msg_routing_policy` | RoutingPolicy | 路由策略表 |
| `msg_delivery_task` | DeliveryTask | 投递任务表 |
| `msg_delivery_attempt` | DeliveryAttempt | 投递尝试记录表 |
| `msg_subscription_rule` | SubscriptionRule | 事件订阅规则表 |
| `msg_subscription_preference` | SubscriptionPreference | 用户订阅偏好表 |
| `msg_device_binding` | DeviceBinding | 设备绑定表 |
| `msg_mobile_session` | MobileSession | 移动会话表 |
| `msg_ecosystem_integration` | EcosystemIntegration | 生态接入配置表 |
| `msg_callback_audit` | CallbackAuditRecord | 生态回调审计表 |

### 8.2 索引建议

- `msg_notification`：`(tenantId, recipientPersonId, inboxStatus, createdAt)`、`(tenantId, targetAssignmentId, inboxStatus)`、`(tenantId, category, priority, createdAt)`
- `msg_notification_action`：`(notificationId, createdAt)`
- `msg_template`：`(tenantId, code, channelType, locale, version)`、`(tenantId, status, category)`
- `msg_channel_endpoint`：`(tenantId, channelType, status)`、`(tenantId, endpointCode)`
- `msg_routing_policy`：`(tenantId, category, priorityThreshold, enabled)`
- `msg_delivery_task`：`(tenantId, notificationId, channelType)`、`(tenantId, status, nextRetryAt)`、`(tenantId, providerMessageId)`
- `msg_delivery_attempt`：`(deliveryTaskId, attemptNo)`、`(requestedAt)`
- `msg_subscription_rule`：`(tenantId, eventTypePattern, enabled)`
- `msg_subscription_preference`：`(tenantId, personId, category, enabled)`
- `msg_device_binding`：`(tenantId, deviceId, bindStatus)`、`(tenantId, personId, bindStatus)`
- `msg_mobile_session`：`(tenantId, deviceBindingId, sessionStatus)`、`(tenantId, personId, sessionStatus)`
- `msg_ecosystem_integration`：`(tenantId, integrationType, status)`
- `msg_callback_audit`：`(integrationId, occurredAt)`、`(verifyResult, occurredAt)`

### 8.3 多租户与大字段建议

- 所有表都必须带 `tenantId`，通过统一多租户拦截器做隔离。
- `variableSchema`、`targetResolverConfig`、`escalationPolicy`、`providerResponse` 等建议采用 JSONB 存储。
- 历史消息正文应存渲染后快照，避免模板升级影响历史展示。

## 9. 一期建模优先级建议

### 9.1 一期必须先落的聚合

- `Notification`
- `DeliveryTask`
- `SubscriptionRule`
- `SubscriptionPreference`
- `DeviceBinding`

### 9.2 一期可简化实现的部分

- `MessageTemplate` 先支持基础变量替换，不做复杂可视化模板设计器
- `RoutingPolicy` 先支持固定规则和简单升级策略，不做复杂条件编排
- `MobileSession` 先做设备绑定、续期和风险冻结，不做复杂离线命令队列
- `EcosystemIntegration` 先打通一种生态渠道或登录方式，不做全量生态矩阵

### 9.3 后续可继续补强的方向

- 周期摘要、批量 Digest、节假日静默策略
- 多级升级链路与送达效果分析
- 移动 Push 回执聚合和弱网补偿机制
- 生态回调自动对账与异常自愈

## 10. 结论

`06-消息移动与生态` 的核心不在“再做一个消息表”，而在于建立一套**事件驱动、身份感知、渠道可治理、移动可接入、生态可扩展**的统一触达模型。其建模重点应围绕以下原则展开：

- 用 `Notification` 持有用户通知真相，用 `DeliveryTask` 持有渠道投递真相
- 用 `SubscriptionRule` 和 `SubscriptionPreference` 解耦业务事件与用户触达策略
- 用 `DeviceBinding` 和 `MobileSession` 支撑移动端高频处理和风险控制
- 用 `EcosystemIntegration` 管理外部渠道与登录接入，但不侵入 `07` 的开放与同步边界

该文档可直接作为后续 `06` 模块建表、接口细化、事件补全和开发任务拆解的基础。
