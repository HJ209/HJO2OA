# schedule-task 事件说明

## 发布事件

| 事件 | 触发时机 | 关键载荷 | 下游使用 | 幂等/补偿/投影约束 |
|------|----------|----------|----------|--------------------|
| `biz.schedule.created` | 新建自建日程或来源型系统日程落账后发布。 | `scheduleId`、`ownerType`、`ownerId`、`sourceType`、`startAt`、`endAt`。 | `04` 展示我的日程，`06` 生成提醒任务。 | 按 `scheduleId` 幂等；来源型日程重复事件不得重复建账。 |
| `biz.schedule.updated` | 日程时间、可见范围、状态或提醒规则变更后发布。 | `scheduleId`、`changedFields`、`status`、`updatedAt`。 | `04/06` 刷新日程视图与提醒计划。 | 同一事务内聚合为一次事件；补偿以最新主档快照覆盖投影。 |
| `biz.task.assigned` | 任务首次分配或重新指派负责人后发布。 | `taskId`、`assigneeId`、`dueAt`、`priority`、`originType`。 | `04` 展示我的任务，`06` 发送任务提醒。 | 按 `taskId + assigneeId + dueAt` 去重；重复分配只刷新负责人投影。 |
| `biz.task.completed` | 任务完成并写入反馈后发布。 | `taskId`、`assigneeId`、`completedAt`、`originType`。 | `04` 关闭任务卡片，其他业务按需消费完成事实。 | 同一 `taskId` 仅允许一次完成事件；补偿通过追加反馈，不回退历史完成事件。 |
| `biz.task.overdue` | 任务到期未完成，经调度判定逾期时发布。 | `taskId`、`assigneeId`、`dueAt`、`priority`。 | `06` 发逾期提醒，`04` 展示逾期标签。 | 以 `taskId + dueAt` 去重；续期成功后应撤销逾期投影。 |

## 消费事件

| 事件 | 来源 | 消费用途 | 关键载荷 | 幂等/补偿/投影约束 |
|------|------|----------|----------|--------------------|
| `biz.meeting.created` | `meeting-mgmt` | 为组织者和参会人生成来源于会议的系统日程。 | `meetingId`、`organizerId`、`startTime`、`endTime`、`meetingType`。 | 按 `sourceType=MEETING + meetingId` 幂等；时间变更以最新事件覆盖系统日程。 |
| `biz.meeting.cancelled` | `meeting-mgmt` | 撤销或关闭来源于会议的系统日程。 | `meetingId`、`cancelledAt`、`reason`。 | 按 `meetingId + cancelledAt` 幂等；仅更新来源型日程，不影响自建日程。 |
| `org.person.resigned` | `01` | 对未完成任务触发移交流程或待分配状态校验。 | `personId`、`orgId`、`effectiveDate`。 | 仅处理 `NEW/IN_PROGRESS/BLOCKED` 任务；重复事件按 `personId + effectiveDate` 去重。 |
