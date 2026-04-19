# message-center 事件契约

## 1. 事件角色

`message-center` 是 `06` 模块内“消息事实”的主要发布方，负责在消息创建和状态流转后发布 `msg.message.*` 事件。

- 该子模块对外不新增独立 `message-center.*` 命名空间。
- 跨模块总线事件统一复用父模块口径：`msg.notification.sent(channel=INBOX)`、`msg.notification.read`。
- 稳定消费方式以内部命令为主，不定义独立的总线入站契约。

## 2. 发布事件

| 事件类型 | 触发时机 | 关键载荷字段 | 说明 |
|----------|----------|--------------|------|
| `msg.message.created` | `Notification` 成功入箱后 | `notificationId`、`recipientPersonId`、`category`、`priority`、`targetAssignmentId`、`targetPositionId` | 供 `channel-sender` 创建投递任务 |
| `msg.message.read` | 用户首次将消息标记为已读后 | `notificationId`、`recipientPersonId`、`readAt` | 重复已读不重复发布 |
| `msg.message.revoked` | 管理员或内部命令撤回消息后 | `notificationId`、`reason`、`revokedBy` | 供下游刷新展示和统计 |
| `msg.message.expired` | 系统调度或业务联动置为过期后 | `notificationId`、`expiredAt`、`reason` | 供下游刷新展示和统计 |

补充说明：

- 当 `msg.message.created` 被 `channel-sender` 接受并落地 `INBOX` 投递任务时，上层可统一映射为 `msg.notification.sent`。
- 当 `msg.message.read` 对外公开给门户或统计能力时，上层可统一映射为 `msg.notification.read`。

## 3. 消费事件

| 类型 | 说明 |
|------|------|
| 无稳定总线消费契约 | 消息创建、过期和撤回主要通过内部命令进入本模块，而不是通过额外总线事件回放 |

## 4. 载荷字段要求

### 4.1 `msg.message.created`

- `notificationId`：消息主键，作为后续所有投递与展示链路的主关联键。
- `recipientPersonId`：接收人主键。
- `category`：消息类别。
- `priority`：优先级。
- `targetAssignmentId` / `targetPositionId`：任职相关消息必须携带。

### 4.2 状态流转事件

- `notificationId` 必填。
- 时间字段统一由服务端产生。
- 撤回、过期事件必须带 `reason`。

## 5. 投递语义、重试与幂等

- 发布语义为至少一次投递。
- `Notification` 创建与事件出箱写入必须同事务提交。
- 消费方必须以 `notificationId` 作为幂等键；对状态事件再叠加 `actionType` 控制重复消费。
- `msg.message.read` 只在首次已读时发布，避免重复刷新未读统计。
- 事件发布失败按统一出箱补偿机制重试，超过重试上限进入死信/人工补偿队列。

## 6. 死信与补偿说明

- 死信记录至少保存 `notificationId`、事件类型、最近错误、重试次数、最后处理时间。
- 补偿只重发事件，不重建 `Notification` 主记录。
- 对已撤回或已过期消息的补偿，必须按当前最终状态重新发布，禁止回放成旧状态。
