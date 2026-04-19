# message-center API 契约

## 1. 范围与边界

本文件定义 `message-center` 对外与对内可见的消息查询、详情、状态流转和未读汇总接口。

- 本模块负责：消息列表、消息详情、未读汇总、已读/归档/撤回/过期。
- 本模块不负责：消息发送、模板与渠道配置、订阅规则、移动会话、生态配置。
- 内部写接口仅供 `event-subscription`、`channel-sender` 或模块内调度使用，不对终端页面开放。

## 2. 通用约束

### 2.1 鉴权与身份

- 用户态接口只允许访问本人消息，且按当前身份上下文过滤可见范围。
- 管理态撤回/过期接口要求租户管理员或审计管理员权限。
- 内部接口要求服务身份，禁止浏览器直接调用。

### 2.2 分页与筛选

- 统一分页参数：`pageNo`、`pageSize`，`pageSize` 默认 `20`，最大 `100`。
- 统一时间筛选：`createdFrom`、`createdTo`，使用 ISO-8601 时间戳。
- 列表接口默认按 `createdAt desc` 排序。

### 2.3 幂等与审计

- `POST` 内部创建接口必须携带 `Idempotency-Key` 或显式 `dedupKey`。
- `READ / ARCHIVE / REVOKE / EXPIRE` 动作按“资源最终状态”幂等，重复提交返回最新状态。
- 所有状态变更必须写入 `NotificationAction` 审计轨迹。

## 3. 接口分组

| 分组 | 说明 | 主要调用方 |
|------|------|-----------|
| 用户消息查询 | 消息列表、详情、未读汇总 | 门户、工作台、移动端聚合层 |
| 用户状态动作 | 已读、批量已读、归档 | 门户、工作台、移动端聚合层 |
| 管理状态动作 | 撤回、过期、审计查询 | 租户管理员、审计管理员 |
| 内部写接口 | 创建消息、同步投递汇总、过期命令 | `event-subscription`、`channel-sender` |

## 4. 接口定义

### 4.1 GET `/api/message-center/messages`

消息列表查询。

查询参数：

| 参数 | 必填 | 说明 |
|------|------|------|
| `pageNo` | 否 | 页码，默认 `1` |
| `pageSize` | 否 | 每页条数，默认 `20`，最大 `100` |
| `category` | 否 | 消息类别 |
| `inboxStatus` | 否 | `UNREAD / READ / ARCHIVED / REVOKED / EXPIRED` |
| `sourceModule` | 否 | 来源模块，如 `todo-center` |
| `priority` | 否 | `NORMAL / URGENT / CRITICAL` |
| `targetAssignmentId` | 否 | 任职快照过滤 |
| `createdFrom` / `createdTo` | 否 | 创建时间范围 |
| `keyword` | 否 | 标题或摘要模糊搜索 |

返回关键字段：

- `notificationId`
- `title`
- `bodySummary`
- `category`
- `priority`
- `inboxStatus`
- `deliveryStatus`
- `sourceModule`
- `deepLink`
- `targetAssignmentId`
- `targetPositionId`
- `createdAt`

规则：

- 只返回当前登录人可见消息。
- 任职相关消息以生成时快照和当前身份共同判定展示范围。
- `REVOKED` 与 `EXPIRED` 默认不隐藏，便于用户理解消息失效原因。

### 4.2 GET `/api/message-center/messages/{notificationId}`

消息详情查询。

返回关键字段：

- 列表字段全部返回。
- 追加 `sourceEventType`、`sourceBusinessId`、`readAt`、`expiredAt`。
- 可选返回最近一次外部投递状态摘要，作为只读显示。

规则：

- 详情页不得暴露模板原文、渠道密钥、内部表名。
- 若消息已撤回，仅返回“已撤回”占位内容和撤回原因摘要。

### 4.3 POST `/api/message-center/messages/{notificationId}/read`

单条已读。

请求体：

| 字段 | 必填 | 说明 |
|------|------|------|
| `readAt` | 否 | 客户端时间，仅作展示；落库以服务端时间为准 |

规则：

- `UNREAD -> READ` 写入 `NotificationAction(READ)`。
- 对已是 `READ` 的消息重复调用返回 `200` 且不重复加审计轨迹。
- 对 `REVOKED / EXPIRED` 消息返回当前状态，不再回写为 `READ`。

### 4.4 POST `/api/message-center/messages/bulk-read`

批量已读。

请求体：

| 字段 | 必填 | 说明 |
|------|------|------|
| `notificationIds` | 是 | 消息 ID 列表，最大 `200` 条 |

规则：

- 仅处理当前用户有权限访问的消息。
- 逐条幂等，部分失败时返回成功与失败清单，不做全量回滚。

### 4.5 POST `/api/message-center/messages/{notificationId}/archive`

归档消息。

请求体：

| 字段 | 必填 | 说明 |
|------|------|------|
| `reason` | 否 | 归档原因，最大 `256` 字符 |

规则：

- 允许 `READ -> ARCHIVED`，也允许直接归档 `UNREAD` 消息。
- 归档仅影响收件箱视图，不影响外部投递记录。

### 4.6 POST `/admin/message-center/messages/{notificationId}/revoke`

管理端撤回消息。

请求体：

| 字段 | 必填 | 说明 |
|------|------|------|
| `reason` | 是 | 撤回原因 |

规则：

- 仅允许管理员或系统内部补偿任务调用。
- 撤回后消息详情保留占位内容和审计轨迹。
- 若已有外部渠道发送记录，仅标记消息撤回，不强制回滚第三方已送达事实。

### 4.7 GET `/api/message-center/unread-summary`

未读汇总。

查询参数：

| 参数 | 必填 | 说明 |
|------|------|------|
| `targetAssignmentId` | 否 | 当前身份过滤 |

返回关键字段：

- `totalUnreadCount`
- `categoryUnreadCounts`
- `latestNotificationIds`

规则：

- 门户与移动端必须共用同一口径。
- 身份切换后重新计算，不允许直接沿用旧身份缓存。

### 4.8 POST `/internal/message-center/messages`

内部创建消息。

请求体关键字段：

- `category`
- `sourceModule`
- `sourceEventType`
- `sourceBusinessId`
- `recipientPersonId`
- `targetAssignmentId`
- `targetPositionId`
- `title`
- `bodySummary`
- `deepLink`
- `priority`
- `dedupKey`
- `relatedTemplateCode`

规则：

- `dedupKey` 命中时返回已存在消息，不重复创建。
- 与消息创建同事务写入出箱事件，满足最终一致性要求。
- 该接口不做模板渲染，只接收已确定的消息快照。

### 4.9 POST `/internal/message-center/messages/{notificationId}/expire`

内部过期命令。

请求体关键字段：

- `reason`
- `expiredAt`

规则：

- 仅允许系统调度或业务完成联动调用。
- 过期后保留查询能力，不转物理删除。

## 5. 权限与审计约束

| 场景 | 权限要求 | 审计要求 |
|------|----------|----------|
| 查询本人消息 | 登录用户 | 仅记录访问日志，不落动作轨迹 |
| 已读/批量已读/归档 | 登录用户 | 必须落 `NotificationAction` |
| 撤回/过期 | 管理员或内部服务 | 必须记录操作者、原因、时间 |
| 创建消息 | 内部服务 | 必须记录来源模块、来源事件、`dedupKey` |

## 6. 错误场景

| 场景 | HTTP 状态 | 说明 |
|------|-----------|------|
| 消息不存在或不属于当前用户 | `404` | 不暴露跨租户或他人消息存在性 |
| 当前身份与消息快照不匹配 | `403` | 任职相关消息不允许越权查看 |
| 批量数量超过上限 | `400` | `notificationIds` 超过 `200` 条 |
| 撤回或过期状态下执行已读 | `409` | 返回当前状态，不覆盖失效事实 |
| 内部创建缺少 `dedupKey`/幂等头 | `400` | 防止重复建消息 |
| 深链或来源快照字段不完整 | `422` | 拒绝落入不完整消息 |
