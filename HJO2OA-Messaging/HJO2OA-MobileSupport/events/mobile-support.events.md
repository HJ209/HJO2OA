# mobile-support 事件契约

## 1. 事件角色

`mobile-support` 默认不发布稳定领域事件，以消费事件做缓存刷新和会话快照同步为主。

## 2. 发布事件

| 类型 | 说明 |
|------|------|
| 无 | 一期不发布独立领域事件 |

## 3. 消费事件

| 事件类型 | 触发动作 | 关键载荷字段 | 说明 |
|----------|----------|--------------|------|
| `org.identity.switched` | 刷新移动会话中的当前身份快照 | `personId`、`currentAssignmentId`、`currentPositionId`、`occurredAt` | 仅同步快照，不重建会话 |
| `msg.message.created` | 失效消息未读数和首页摘要缓存 | `notificationId`、`recipientPersonId`、`category`、`priority` | 用于移动端角标刷新 |
| `msg.message.read` | 刷新未读数缓存 | `notificationId`、`recipientPersonId`、`readAt` | 已读后同步移动端角标 |
| `msg.message.revoked` | 刷新列表缓存 | `notificationId`、`reason` | 失效消息不应继续展示为可处理项 |
| `msg.message.expired` | 刷新列表缓存 | `notificationId`、`expiredAt` | 过期消息刷新展示状态 |

## 4. 关键载荷字段要求

- 身份切换事件必须提供当前任职与岗位快照。
- 消息状态事件至少携带 `notificationId`、`recipientPersonId` 和发生时间。
- 本模块只将这些事件用于缓存刷新和会话快照同步，不新增新的业务事实。

## 5. 至少一次消费、重试与幂等

- 消费语义为至少一次。
- 幂等键采用 `eventId`；若上游无统一 `eventId`，则使用 `notificationId + eventType + versionTime`。
- 缓存刷新失败允许重试，但不得影响主业务事务提交。
- 若缓存多次刷新失败，前端读取时必须支持回源重新计算。

## 6. 死信与补偿说明

- 死信仅影响缓存一致性，不影响 `DeviceBinding` 或 `MobileSession` 主数据。
- 对于缓存刷新死信，允许定时任务全量重建当前用户未读数和首页摘要。
- 身份快照同步失败时，优先在下一次移动会话校验时重新拉取 `01` 真相。
