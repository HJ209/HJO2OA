# mobile-support API 契约

## 1. 范围与边界

本文件定义 `mobile-support` 的设备绑定、移动会话、首页聚合和移动消息轻量接口。

- 本模块负责：设备绑定、push token 管理、移动会话、首页与消息高频接口。
- 本模块不负责：审批/待办业务写操作真相、消息发送、订阅规则、生态配置。
- 认证本身由统一认证体系或生态登录完成，本模块只承接移动会话落地。

## 2. 通用约束

### 2.1 鉴权

- 设备绑定、会话刷新、首页聚合、消息查询都要求有效登录态。
- 管理员可执行设备撤销、风险冻结，但普通用户只能操作自己的设备与会话。

### 2.2 分页与筛选

- 移动消息列表统一分页参数：`pageNo`、`pageSize`，默认 `20`，最大 `50`。
- 首页聚合接口不分页，只返回摘要。

### 2.3 幂等与审计

- 设备绑定以 `tenantId + deviceId` 作为天然幂等键。
- `pushToken` 更新、会话刷新、设备撤销必须记录审计。
- 读操作允许缓存兜底，但写操作必须以服务端确认结果为准。

## 3. 接口分组

| 分组 | 说明 | 主要调用方 |
|------|------|-----------|
| 设备绑定 | 绑定设备、更新 token、撤销设备 | 移动端、管理端 |
| 会话管理 | 创建会话、续期、查询当前会话状态 | 移动端 |
| 首页聚合 | 未读数、待办摘要、快捷入口 | 移动端首页 |
| 移动消息 | 轻量消息列表、未读数、跳转元数据 | 移动端消息页 |

## 4. 接口定义

### 4.1 POST `/api/mobile-support/devices/bind`

绑定设备。

请求体关键字段：

- `deviceId`
- `deviceFingerprint`
- `platform`
- `appType`
- `pushToken`

返回关键字段：

- `deviceBindingId`
- `bindStatus`
- `riskLevel`
- `lastLoginAt`

规则：

- 同一租户同一 `deviceId` 只允许一个 `ACTIVE` 绑定。
- 若设备已标记 `LOST / REVOKED`，需要管理员解除后才能重新绑定。

### 4.2 POST `/api/mobile-support/devices/{deviceId}/push-token`

更新 push token。

请求体关键字段：

- `pushToken`

规则：

- 重复提交相同 token 视为幂等成功。
- 设备状态非 `ACTIVE` 时拒绝更新。

### 4.3 POST `/api/mobile-support/sessions`

创建移动会话。

请求体关键字段：

- `deviceId`
- `accountId`
- `currentAssignmentId`
- `currentPositionId`

返回关键字段：

- `sessionId`
- `sessionStatus`
- `issuedAt`
- `expiresAt`
- `currentAssignmentId`
- `currentPositionId`

规则：

- 调用前必须已通过统一认证。
- 会话快照只用于移动端快速鉴权和展示过滤。

### 4.4 POST `/api/mobile-support/sessions/{sessionId}/refresh`

续期移动会话。

请求体关键字段：

- `refreshVersion`

规则：

- 只有 `ACTIVE` 会话可续期。
- 服务端需校验设备绑定仍有效且未被风险冻结。

### 4.5 GET `/api/mobile-support/session/current`

查询当前移动会话状态。

返回关键字段：

- `sessionId`
- `sessionStatus`
- `expiresAt`
- `deviceBindingId`
- `currentAssignmentId`
- `currentPositionId`

### 4.6 GET `/api/mobile-support/home`

移动首页聚合。

查询参数：

| 参数 | 必填 | 说明 |
|------|------|------|
| `currentAssignmentId` | 否 | 当前身份，缺省取会话快照 |

返回关键字段：

- `unreadMessageCount`
- `todoSummary`
- `approvalSummary`
- `quickEntries`
- `latestNotice`

规则：

- 只返回高频摘要，不直接返回大字段正文。
- 若部分下游服务不可用，允许返回带降级标记的部分结果。

### 4.7 GET `/api/mobile-support/messages`

移动端轻量消息列表。

查询参数：

| 参数 | 必填 | 说明 |
|------|------|------|
| `pageNo` / `pageSize` | 否 | 分页参数 |
| `inboxStatus` | 否 | 消息状态 |
| `category` | 否 | 消息类别 |
| `currentAssignmentId` | 否 | 当前身份过滤 |

返回关键字段：

- `notificationId`
- `title`
- `category`
- `priority`
- `inboxStatus`
- `deepLink`
- `createdAt`

规则：

- 该接口以轻量展示为主，详情正文仍可回跳 `message-center` 详情接口或聚合详情页。

### 4.8 POST `/admin/mobile-support/devices/{deviceId}/revoke`

撤销设备。

请求体关键字段：

- `reason`

规则：

- 撤销后设备绑定状态置为 `REVOKED`。
- 关联 `pushToken` 和 `MobileSession` 必须立即失效。

## 5. 权限与审计约束

| 场景 | 权限要求 | 审计要求 |
|------|----------|----------|
| 本人设备绑定、token 更新、会话续期 | 登录用户 | 记录设备、时间、结果 |
| 首页聚合、消息查询 | 登录用户 | 记录访问日志 |
| 设备撤销、风险冻结 | 管理员或安全角色 | 记录操作者、原因、影响会话 |

## 6. 错误场景

| 场景 | HTTP 状态 | 说明 |
|------|-----------|------|
| 设备不存在或不属于当前用户 | `404 / 403` | 禁止跨用户访问 |
| 设备已撤销、丢失或冻结 | `409` | 不允许续期和更新 token |
| 会话已过期或被风险冻结 | `401 / 423` | 前端需引导重新登录 |
| `refreshVersion` 冲突 | `409` | 防止并发续期覆盖 |
| 聚合下游部分不可用 | `206 / 503` | 返回降级标记和建议重试时间 |
