# channel-sender API 契约

## 1. 范围与边界

本文件定义 `channel-sender` 的模板、渠道端点、路由策略、投递追踪和人工补偿接口。

- 本模块负责：模板管理、端点管理、发送策略、送达追踪、失败重试。
- 本模块不负责：消息列表与详情、订阅规则、移动接入、生态配置主数据。
- 外部连接参数校验与回调安全由 `ecosystem` 负责，本模块仅引用其 `configRef` 和健康状态。

## 2. 通用约束

### 2.1 鉴权

- 模板、端点、策略管理接口要求租户管理员权限。
- 投递任务查询和人工重试接口要求运维或审计角色。
- 内部投递创建接口只接受服务身份调用。

### 2.2 分页与筛选

- 统一分页参数：`pageNo`、`pageSize`，默认 `20`，最大 `100`。
- 投递任务默认按 `createdAt desc` 排序。
- 时间筛选统一使用 `createdFrom`、`createdTo`。

### 2.3 幂等与审计

- 模板创建、端点创建、策略创建、重试操作必须携带 `Idempotency-Key`。
- 发布模板、启停端点、启停策略、人工重试都必须记录操作者、变更前后值和原因。
- 投递任务创建使用 `notificationId + channelType + routeOrder` 作为天然幂等键。

## 3. 接口分组

| 分组 | 说明 | 主要调用方 |
|------|------|-----------|
| 模板管理 | 模板列表、详情、创建、发布、预览 | 管理端 |
| 端点管理 | 端点列表、启停、测试发送、限流配额查看 | 管理端、运维端 |
| 路由策略管理 | 路由规则列表、维护和启停 | 管理端 |
| 投递治理 | 投递任务查询、尝试记录、人工重试 | 运维端、审计端 |
| 内部调度 | 根据消息创建投递任务并驱动发送 | `message-center`、内部调度器 |

## 4. 接口定义

### 4.1 GET `/admin/channel-sender/templates`

模板列表。

查询参数：

| 参数 | 必填 | 说明 |
|------|------|------|
| `pageNo` / `pageSize` | 否 | 分页参数 |
| `code` | 否 | 模板编码 |
| `channelType` | 否 | 渠道类型 |
| `category` | 否 | 消息类别 |
| `status` | 否 | `DRAFT / PUBLISHED / DEPRECATED` |
| `locale` | 否 | 语言 |

返回关键字段：

- `templateId`
- `code`
- `name`
- `channelType`
- `category`
- `locale`
- `version`
- `status`
- `publishedAt`

### 4.2 POST `/admin/channel-sender/templates`

创建模板草稿。

请求体关键字段：

- `code`
- `name`
- `channelType`
- `category`
- `locale`
- `titleTemplate`
- `bodyTemplate`
- `actionLinkTemplate`
- `variableSchema`

规则：

- 同一租户下 `(code, channelType, locale, version)` 唯一。
- 草稿可编辑，已发布版本不可原地修改。
- 模板变量必须显式声明，不允许依赖未注册字段。

### 4.3 POST `/admin/channel-sender/templates/{templateId}/publish`

发布模板版本。

请求体关键字段：

- `publishComment`

规则：

- 发布前必须通过变量校验。
- 发布后该版本转只读；变更需新建更高版本。

### 4.4 POST `/admin/channel-sender/templates/{templateId}/preview`

模板预览。

请求体关键字段：

- `sampleVariables`

规则：

- 仅返回渲染结果，不写入任何业务数据。
- 缺失必填变量返回 `422`。

### 4.5 GET `/admin/channel-sender/endpoints`

渠道端点列表。

查询参数：

| 参数 | 必填 | 说明 |
|------|------|------|
| `channelType` | 否 | 渠道类型 |
| `providerType` | 否 | 提供方类型 |
| `status` | 否 | `ENABLED / DISABLED / DEGRADED` |

返回关键字段：

- `endpointId`
- `endpointCode`
- `channelType`
- `providerType`
- `displayName`
- `status`
- `configRef`
- `rateLimitPerMinute`
- `dailyQuota`
- `healthStatus`

规则：

- `configRef` 只显示掩码或引用名，不返回明文凭证。
- `INBOX` 端点为内建逻辑端点，不允许删除。

### 4.6 POST `/admin/channel-sender/endpoints/{endpointId}/send-test`

测试发送。

请求体关键字段：

- `target`
- `templateCode`
- `sampleVariables`

规则：

- 仅用于验证路由链路和端点可用性。
- 连接可用性由 `ecosystem` 测试接口校验；本接口只做一次测试投递。

### 4.7 GET `/admin/channel-sender/routing-policies`

路由策略列表。

查询参数：

| 参数 | 必填 | 说明 |
|------|------|------|
| `category` | 否 | 消息类别 |
| `priorityThreshold` | 否 | 优先级阈值 |
| `enabled` | 否 | 是否启用 |

返回关键字段：

- `policyId`
- `policyCode`
- `category`
- `priorityThreshold`
- `targetChannelOrder`
- `fallbackChannelOrder`
- `quietWindowBehavior`
- `dedupWindowSeconds`
- `enabled`

### 4.8 POST `/admin/channel-sender/routing-policies`

创建或更新路由策略。

请求体关键字段：

- `policyCode`
- `category`
- `priorityThreshold`
- `targetChannelOrder`
- `fallbackChannelOrder`
- `quietWindowBehavior`
- `dedupWindowSeconds`
- `escalationPolicy`
- `enabled`

规则：

- `targetChannelOrder` 第一位必须可推导出 `INBOX`。
- `CRITICAL` 允许绕过静默规则，但仍要记录绕过原因。

### 4.9 GET `/admin/channel-sender/delivery-tasks`

投递任务列表。

查询参数：

| 参数 | 必填 | 说明 |
|------|------|------|
| `notificationId` | 否 | 消息 ID |
| `channelType` | 否 | 渠道类型 |
| `status` | 否 | `PENDING / SENDING / DELIVERED / FAILED / GAVE_UP / CANCELLED` |
| `providerMessageId` | 否 | 第三方消息 ID |
| `createdFrom` / `createdTo` | 否 | 时间范围 |
| `lastErrorCode` | 否 | 最近错误码 |

返回关键字段：

- `deliveryTaskId`
- `notificationId`
- `channelType`
- `endpointId`
- `routeOrder`
- `status`
- `retryCount`
- `nextRetryAt`
- `providerMessageId`
- `lastErrorCode`
- `deliveredAt`

### 4.10 POST `/admin/channel-sender/delivery-tasks/{deliveryTaskId}/retry`

人工重试。

请求体关键字段：

- `reason`

规则：

- 仅允许 `FAILED / GAVE_UP` 状态重试。
- 重试会新增 `DeliveryAttempt`，不覆盖历史尝试记录。

### 4.11 POST `/internal/channel-sender/delivery-tasks`

内部创建投递任务。

请求体关键字段：

- `notificationId`
- `category`
- `priority`
- `templateCode`
- `recipientPersonId`
- `deepLink`
- `allowedChannels`
- `dedupKey`

规则：

- 本接口根据路由策略拆分出多条 `DeliveryTask`。
- 若外部链路全部不可用，仍必须创建并执行 `INBOX` 任务。

## 5. 权限与审计约束

| 场景 | 权限要求 | 审计要求 |
|------|----------|----------|
| 模板、端点、策略变更 | 租户管理员 | 记录变更前后值与原因 |
| 测试发送、人工重试 | 运维或管理员 | 记录发起人、目标、结果 |
| 投递任务查询 | 运维、审计、管理员 | 记录访问日志 |
| 内部创建投递任务 | 服务身份 | 记录来源消息、路由策略版本 |

## 6. 错误场景

| 场景 | HTTP 状态 | 说明 |
|------|-----------|------|
| 模板未发布或不存在 | `404 / 409` | 未发布模板不能进入正式发送 |
| 模板变量不完整 | `422` | 预览或发送前校验失败 |
| 端点被禁用或健康状态不可用 | `409` | 触发降级或拒绝测试发送 |
| 路由策略未包含 `INBOX` | `422` | 不符合一期必达约束 |
| 超过配额或速率限制 | `429` | 写入失败原因并等待重试 |
| 对非失败任务执行人工重试 | `409` | 保持当前状态不变 |
