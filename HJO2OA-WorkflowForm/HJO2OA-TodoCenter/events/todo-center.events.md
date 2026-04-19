# todo-center 领域事件

事件信封统一遵循 `docs/contracts/unified-event-contract.md`。`todo-center` 既消费流程事件维护投影，也对外发布待办事件，供消息模块决定触达方式。

## 发布事件

| 事件类型 | 触发时机 | 关键载荷字段 | 说明 |
|----------|----------|--------------|------|
| `todo.item.created` | 待办投影首次创建后 | `todoId`、`taskId`、`instanceId`、`assigneeId`、`type`、`category`、`title`、`urgency`、`dueTime` | 供消息模块发送待办提醒。 |
| `todo.item.completed` | 待办项因任务完成转为已办后 | `todoId`、`taskId`、`instanceId`、`completedTime` | 仅表达待办视图完成，不表达业务结果。 |
| `todo.item.cancelled` | 待办项因任务终止/流程终止失效后 | `todoId`、`taskId`、`instanceId`、`reason` | 对应提醒链路可停止。 |
| `todo.item.overdue` | 活跃待办进入超时状态后 | `todoId`、`taskId`、`instanceId`、`dueTime`、`overdueDuration` | 供 `06` 决定催办触达。 |

## 消费事件

| 事件类型 | 消费目的 | 投影动作 | 幂等键 |
|----------|----------|----------|--------|
| `process.task.created` | 创建待办投影 | 新建或更新 `todo_item` | `eventId`，并以 `taskId` 做 upsert 键。 |
| `process.task.claimed` | 刷新当前处理人 | 更新投影归属 | `eventId` |
| `process.task.completed` | 关闭待办并转已办 | 将活跃待办置为 `COMPLETED` | `eventId` |
| `process.task.terminated` | 取消待办 | 将活跃待办置为 `CANCELLED` | `eventId` |
| `process.task.transferred` | 刷新待办归属 | 更新 `assignee*` 字段 | `eventId` |
| `process.task.overdue` | 标记待办超时 | 记录超时状态并发布 `todo.item.overdue` | `eventId` |
| `process.instance.started` | 刷新“我发起”视图缓存 | 失效或更新发起列表摘要 | `eventId` |
| `process.instance.completed` / `process.instance.terminated` | 刷新“我发起”与归档摘要 | 更新实例结束态展示 | `eventId` |

## 投影、幂等与补偿

- `todo_item` 是任务视图投影表，不直接承载草稿和归档；草稿与归档通过聚合查询权威表获得。
- 投影更新必须按 `taskId` 做 upsert，确保重复事件不会产生重复待办。
- `process.task.transferred` 只更新归属，不强制再次发布 `todo.item.created`，消息是否通知新处理人由消费方按流程事件自行决定。
- 若投影消费失败，可通过重放 `process.task.*` 事件或从 `proc_task`/`proc_instance` 重新构建。
