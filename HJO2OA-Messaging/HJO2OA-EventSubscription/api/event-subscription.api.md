# event-subscription API 契约

## 1. 范围与边界

本文件定义 `event-subscription` 的订阅规则、用户偏好、事件接入状态和执行审计接口。

- 本模块负责：规则管理、偏好管理、执行审计、已注册事件类型展示。
- 本模块不负责：通用 Webhook 接入、开放事件订阅、消息列表、消息发送、移动会话、生态配置。
- 事件实际消费来自总线，不通过本模块暴露公共接收接口。

## 2. 通用约束

### 2.1 鉴权

- 规则管理、事件接入状态和执行审计要求租户管理员或审计角色。
- 用户偏好接口只允许当前登录人访问本人配置。

### 2.2 分页与筛选

- 统一分页参数：`pageNo`、`pageSize`，默认 `20`，最大 `100`。
- 执行审计按 `occurredAt desc` 排序。

### 2.3 幂等与审计

- 规则创建/更新、偏好更新必须支持 `Idempotency-Key`。
- 运行时事件处理以 `eventId + ruleCode + recipientPersonId` 作为幂等键。
- 所有规则变更、启停、强制类别策略变更都必须记录审计。

## 3. 接口分组

| 分组 | 说明 | 主要调用方 |
|------|------|-----------|
| 规则管理 | 订阅规则列表、详情、创建、启停 | 管理端 |
| 偏好管理 | 当前用户的订阅偏好查询与修改 | 用户侧、移动端 |
| 事件接入状态 | 查看一期已注册事件和启用状态 | 管理端 |
| 执行审计 | 查询事件命中、去重、静默、失败记录 | 管理端、审计端 |

## 4. 接口定义

### 4.1 GET `/admin/event-subscription/rules`

规则列表。

查询参数：

| 参数 | 必填 | 说明 |
|------|------|------|
| `pageNo` / `pageSize` | 否 | 分页参数 |
| `ruleCode` | 否 | 规则编码 |
| `eventTypePattern` | 否 | 事件类型模式 |
| `notificationCategory` | 否 | 目标消息类别 |
| `enabled` | 否 | 是否启用 |
| `templateCode` | 否 | 模板编码 |

返回关键字段：

- `ruleId`
- `ruleCode`
- `eventTypePattern`
- `notificationCategory`
- `targetResolverType`
- `templateCode`
- `defaultPriority`
- `enabled`

### 4.2 POST `/admin/event-subscription/rules`

创建或更新规则。

请求体关键字段：

- `ruleCode`
- `eventTypePattern`
- `notificationCategory`
- `targetResolverType`
- `targetResolverConfig`
- `templateCode`
- `defaultPriority`
- `dedupWindowSeconds`
- `enabled`

规则：

- `eventTypePattern` 只能引用父模块已注册事件类型。
- `templateCode` 必须引用已发布模板。
- 待办类规则应优先解析 `assignmentId`、`positionId`。

### 4.3 POST `/admin/event-subscription/rules/{ruleId}/toggle`

启停规则。

请求体关键字段：

- `enabled`
- `reason`

规则：

- 停用规则后仅阻断后续新事件处理，不回溯撤销已生成消息。

### 4.4 GET `/api/event-subscription/preferences`

当前用户偏好查询。

返回关键字段：

- `category`
- `allowedChannels`
- `quietWindow`
- `digestMode`
- `escalationOptIn`
- `muteNonWorkingHours`
- `enabled`

### 4.5 PUT `/api/event-subscription/preferences/{category}`

更新当前用户某类消息偏好。

请求体关键字段：

- `allowedChannels`
- `quietWindow`
- `digestMode`
- `escalationOptIn`
- `muteNonWorkingHours`
- `enabled`

规则：

- 不能把安全类、账号锁定类消息配置为完全禁用。
- `allowedChannels` 只表达“用户允许”，最终是否发出仍受系统规则和渠道健康状态约束。

### 4.6 GET `/admin/event-subscription/event-types`

一期已注册事件类型。

返回关键字段：

- `eventType`
- `sourceModule`
- `phase`
- `enabledForSubscription`
- `payloadFields`

规则：

- 该接口只做只读展示，不承担通用事件注册平台职责。

### 4.7 GET `/admin/event-subscription/execution-logs`

执行审计列表。

查询参数：

| 参数 | 必填 | 说明 |
|------|------|------|
| `eventType` | 否 | 事件类型 |
| `ruleCode` | 否 | 规则编码 |
| `result` | 否 | `MATCHED / DEDUP_SKIPPED / QUIET_SKIPPED / CREATED / FAILED` |
| `recipientPersonId` | 否 | 接收人 |
| `occurredFrom` / `occurredTo` | 否 | 时间范围 |

返回关键字段：

- `eventId`
- `eventType`
- `ruleCode`
- `recipientPersonId`
- `result`
- `message`
- `occurredAt`

## 5. 权限与审计约束

| 场景 | 权限要求 | 审计要求 |
|------|----------|----------|
| 规则创建、启停、更新 | 租户管理员 | 必须记录规则前后值、操作者、原因 |
| 偏好更新 | 当前登录用户 | 必须记录更新前后值 |
| 执行审计查询 | 审计、管理员 | 记录访问日志 |

## 6. 错误场景

| 场景 | HTTP 状态 | 说明 |
|------|-----------|------|
| 规则引用未注册事件类型 | `422` | 禁止越界消费私有事件 |
| 规则引用未发布模板 | `409` | 不能生成不可用提醒 |
| 目标解析配置非法 | `422` | 缺少必要字段或解析方式不支持 |
| 用户尝试静默强制消息 | `409` | 安全类消息不可被完全关闭 |
| 查询他人偏好 | `403` | 只允许查看本人偏好 |
| 执行审计分页过大 | `400` | `pageSize` 超限 |
