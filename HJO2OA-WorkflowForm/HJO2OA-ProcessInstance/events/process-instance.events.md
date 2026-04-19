# process-instance 领域事件

事件信封统一遵循 `docs/contracts/unified-event-contract.md`。本子模块只发布父模块已定义的实例和任务事件，不额外发明草稿或归档事件。

## 发布事件

| 事件类型 | 触发时机 | 关键载荷字段 | 说明 |
|----------|----------|--------------|------|
| `process.instance.started` | 流程实例创建并生成首批任务后 | `instanceId`、`definitionId`、`definitionCode`、`definitionVersion`、`initiatorId`、`formDataId`、`currentNodes` | 作为运行时链路起点事件。 |
| `process.instance.completed` | 流程正常结束后 | `instanceId`、`definitionId`、`endTime`、`result` | 下游据此关闭待办并更新业务状态。 |
| `process.instance.terminated` | 管理员终止或异常终止后 | `instanceId`、`reason`、`endTime` | 所有未完成任务必须同步取消。 |
| `process.instance.suspended` | 实例被挂起后 | `instanceId`、`reason` | 超时计时同步暂停。 |
| `process.instance.resumed` | 挂起实例恢复后 | `instanceId` | 允许后续任务继续流转。 |
| `process.task.created` | 任务创建后 | `taskId`、`instanceId`、`nodeId`、`nodeName`、`candidateType`、`candidateIds`、`dueTime` | `todo-center` 基于该事件创建待办投影。 |
| `process.task.claimed` | 候选任务被认领后 | `taskId`、`instanceId`、`assigneeId`、`claimTime` | 用于刷新待办归属和审计。 |
| `process.task.completed` | 任务动作执行完成并推进成功后 | `taskId`、`instanceId`、`actionCode`、`completedTime`、`nextNodeIds` | 不单独发布 `process.action.*`。 |
| `process.task.terminated` | 任务因流程结束或异常被取消后 | `taskId`、`instanceId`、`reason` | 对应待办应取消。 |
| `process.task.transferred` | 任务转办成功后 | `taskId`、`instanceId`、`fromPersonId`、`toPersonId` | 用于刷新待办归属和通知。 |
| `process.task.overdue` | 任务超时后 | `taskId`、`instanceId`、`dueTime`、`overdueDuration` | 供消息中心发送催办提醒。 |

## 消费事件

一期无强制消费事件。

- 已发布流程定义和表单元数据通过同步读取加载，不通过事件订阅驱动主写模型。
- 业务状态回写由 `05-协同办公应用` 自行消费实例/任务事件完成，本子模块不反向消费业务事件。

## 投影、幂等与补偿

- `proc_instance`、`proc_task` 是权威写模型，`todo-center`、消息和监控均为下游投影。
- 事件发布采用事务内 Outbox；若主事务回滚，不得留下孤立实例事件。
- 消费端需按 `eventId` 去重；`process.task.overdue` 额外按 `taskId + dueTime` 去重，避免重复催办。
- 草稿保存、草稿恢复、归档生成均不发布领域事件；若下游需要展示，统一通过查询接口读取。
