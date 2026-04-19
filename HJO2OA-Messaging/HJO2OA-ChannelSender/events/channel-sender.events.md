# channel-sender 事件契约

## 1. 事件角色

`channel-sender` 是 `06` 模块内“投递事实”的主要发布方，负责发布 `msg.channel.*` 事件，并消费消息创建相关事件驱动投递任务生成。

## 2. 发布事件

| 事件类型 | 触发时机 | 关键载荷字段 | 说明 |
|----------|----------|--------------|------|
| `msg.channel.delivery-requested` | 生成 `DeliveryTask` 后 | `deliveryTaskId`、`notificationId`、`channelType`、`routeOrder`、`endpointId` | 标识渠道投递任务已创建 |
| `msg.channel.delivered` | 渠道确认送达后 | `deliveryTaskId`、`notificationId`、`channelType`、`providerMessageId`、`deliveredAt` | `INBOX` 入箱成功也走同一语义 |
| `msg.channel.failed` | 单次尝试失败或最终放弃后 | `deliveryTaskId`、`notificationId`、`channelType`、`retryCount`、`errorCode`、`nextRetryAt`、`gaveUp` | 用于运维告警与健康降级 |

补充说明：

- 跨模块总线对外统一可映射为 `msg.notification.sent` / `msg.notification.delivered`。
- `msg.channel.failed` 在最终放弃时必须带 `gaveUp=true`。

## 3. 消费事件

| 事件类型 | 触发动作 | 幂等键 | 说明 |
|----------|----------|--------|------|
| `msg.message.created` | 创建投递任务并启动首轮发送 | `notificationId + channelType + routeOrder` | 一条消息可拆分多条渠道任务 |
| `msg.message.revoked` | 取消未发送完成的外部任务 | `notificationId` | 已送达事实不回滚 |
| `msg.message.expired` | 取消未执行的后续重试 | `notificationId` | 已完成尝试记录保留 |

## 4. 关键载荷字段说明

### 4.1 投递请求事件

- `deliveryTaskId`：投递任务主键。
- `notificationId`：消息主键。
- `channelType`：`INBOX / EMAIL / SMS / WECHAT_WORK / DINGTALK / APP_PUSH`。
- `routeOrder`：本次渠道在策略中的顺序。
- `endpointId`：使用的端点；`INBOX` 允许为空或内建值。

### 4.2 投递结果事件

- `providerMessageId`：第三方平台消息 ID，回执与回调关联键。
- `retryCount`：当前失败后累计重试次数。
- `nextRetryAt`：若继续重试则必须返回该字段。

## 5. 至少一次投递、重试与幂等

- 消费 `msg.message.created` 采用至少一次语义，必须按幂等键去重。
- 单条 `DeliveryTask` 的重试序列固定为 `1s -> 5s -> 30s -> 5m -> 30m`。
- `DeliveryAttempt` 以 `deliveryTaskId + attemptNo` 保证唯一。
- 第三方回执或回调需以 `providerMessageId` 做结果幂等。
- 外部渠道成功或失败都不得回写修改 `Notification` 的已读事实。

## 6. 死信与补偿说明

- 超过最大重试次数后，任务状态置为 `GAVE_UP`，并写入死信/人工补偿队列。
- 死信记录至少保留 `deliveryTaskId`、`notificationId`、`channelType`、最近一次错误、已重试次数。
- 人工补偿只允许基于现有 `DeliveryTask` 追加新尝试，不允许重建历史模板版本或篡改原始消息快照。
