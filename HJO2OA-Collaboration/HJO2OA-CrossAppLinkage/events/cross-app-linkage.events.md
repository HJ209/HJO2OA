# cross-app-linkage 事件说明

## 发布事件

| 事件 | 触发时机 | 关键载荷 | 下游使用 | 幂等/补偿/投影约束 |
|------|----------|----------|----------|--------------------|
| 无新增对外业务事件 | 一期不对统一事件总线新增独立 `biz.cross-app.*` 契约。 | - | - | 本模块只重建入口与跳转缓存，不以事件形式承诺新的业务真相源。 |

## 消费事件

| 事件 | 来源 | 消费用途 | 关键载荷 | 幂等/补偿/投影约束 |
|------|------|----------|----------|--------------------|
| `biz.document.approved` `biz.document.archived` | `document-mgmt` | 刷新公文入口角标、归档快捷入口和深链可用性。 | `documentId`、`documentType`、`status`、`archivedAt`。 | 按 `bizType + bizId + status` 去重；入口缓存可重建。 |
| `biz.meeting.created` `biz.meeting.cancelled` | `meeting-mgmt` | 更新我的会议入口、会中快捷动作和移动端会议卡片。 | `meetingId`、`startTime`、`endTime`、`reason`。 | 按 `meetingId + status` 去重；取消后撤销入口投影。 |
| `biz.schedule.created` `biz.task.assigned` `biz.task.completed` `biz.task.overdue` | `schedule-task` | 刷新日程/任务入口、高优先级角标和逾期标签。 | `scheduleId/taskId`、`assigneeId`、`dueAt`、`priority`。 | 基于对象 ID 与状态幂等刷新；任务完成后撤销逾期投影。 |
| `biz.attendance.result-changed` | `attendance` | 刷新我的申请结果、异常入口和移动端考勤快捷入口。 | `personId`、`date`、`status`、`requestId`。 | 按 `requestId + resultStatus` 去重；以最新结果覆盖入口摘要。 |
| `biz.contract.expiring` `biz.asset.status-changed` `biz.space.updated` | `contract-asset` `bulletin-fileshare` | 更新临期合同、资产流转、共享空间入口和状态标签。 | `contractId/assetId/spaceId`、`status`、`updatedAt`。 | 入口缓存按对象 ID 幂等刷新；可随时全量重建，不持有业务真相。 |
