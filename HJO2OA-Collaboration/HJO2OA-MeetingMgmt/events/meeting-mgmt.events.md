# meeting-mgmt 事件说明

## 发布事件

| 事件 | 触发时机 | 关键载荷 | 下游使用 | 幂等/补偿/投影约束 |
|------|----------|----------|----------|--------------------|
| `biz.meeting.created` | 会议审批完成并成功排期后发布。 | `meetingId`、`organizerId`、`roomId`、`startTime`、`endTime`、`meetingType`。 | `schedule-task` 生成系统日程，`04`/`06` 刷新会议卡片与提醒。 | 同一 `meetingId + status=SCHEDULED` 只发布一次；若排期失败不得发事件。 |
| `biz.meeting.cancelled` | 已排期会议取消后发布。 | `meetingId`、`reason`、`cancelledAt`、`operatorId`。 | `schedule-task` 取消系统日程，`06` 通知参会人。 | 按 `meetingId + cancelledAt` 幂等；重复取消不重复发通知。 |
| `biz.meeting.minutes-published` | 会议纪要发布成功后发布。 | `meetingId`、`minutesId`、`publishedAt`、`publisherId`。 | `04` 展示纪要入口，`06` 发送纪要查看提醒。 | 同一 `meetingId` 一期只维护一份正式纪要；重复发布只更新投影不新增版本。 |

## 消费事件

| 事件 | 来源 | 消费用途 | 关键载荷 | 幂等/补偿/投影约束 |
|------|------|----------|----------|--------------------|
| `process.instance.completed` | `02` | 将待审批会议回写为 `SCHEDULED`，固化审批结论与排期结果。 | `processInstanceId`、`businessKey`、`completedAt`、`result`。 | 按 `processInstanceId + completedAt` 幂等；冲突重放前必须重新校验会议室占用。 |
| `process.instance.terminated` | `02` | 将审批终止的会议回写为 `CANCELLED` 或保持草稿关闭态，不进入排期。 | `processInstanceId`、`businessKey`、`terminatedAt`、`reason`。 | 按 `processInstanceId + terminatedAt` 幂等；终止后需撤销未提交的提醒投影。 |
| `org.person.resigned` | `01` | 调整未来会议的组织者、记录人或主持人责任归属。 | `personId`、`orgId`、`effectiveDate`。 | 仅处理未来或未完成会议；重复事件按 `personId + effectiveDate` 去重。 |
| `infra.attachment.deleted` | `00` | 校验会议通知附件或纪要附件引用是否失效。 | `attachmentId`、`deletedAt`、`operatorId`。 | 重复事件只更新一次失效标记；补偿方式为重新绑定有效附件。 |
