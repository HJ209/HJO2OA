# attendance 事件说明

## 发布事件

| 事件 | 触发时机 | 关键载荷 | 下游使用 | 幂等/补偿/投影约束 |
|------|----------|----------|----------|--------------------|
| `biz.attendance.requested` | 申请提交审批后发布。 | `requestId`、`applicantId`、`requestType`、`startAt`、`endAt`、`processInstanceId`。 | `06` 发送审批提醒，`04` 更新我的申请卡片。 | 按 `requestId + processInstanceId` 幂等；提交失败回滚到草稿态。 |
| `biz.attendance.result-changed` | 审批完成或补偿结果写回后发布。 | `personId`、`date`、`status`、`requestId`、`requestType`、`resultStatus`。 | `04/06/07` 刷新结果卡片、消息和外部同步。 | 以 `requestId + resultStatus` 去重；结果补偿用最新状态覆盖投影。 |

## 消费事件

| 事件 | 来源 | 消费用途 | 关键载荷 | 幂等/补偿/投影约束 |
|------|------|----------|----------|--------------------|
| `process.instance.completed` | `02` | 将申请回写为 `APPROVED/RESULTED` 并更新考勤结果。 | `processInstanceId`、`businessKey`、`completedAt`、`result`。 | 按 `processInstanceId + completedAt` 幂等；结果回写失败需重试，不得重复生成补偿结果。 |
| `process.instance.terminated` | `02` | 将审批终止的申请回写为 `REJECTED` 或 `CANCELLED`，关闭结果投影。 | `processInstanceId`、`businessKey`、`terminatedAt`、`reason`。 | 按 `processInstanceId + terminatedAt` 幂等；终止后不得再发布结果变更事件。 |
| `infra.attachment.deleted` | `00` | 校验佐证附件引用是否失效，阻断未完成申请继续流转。 | `attachmentId`、`deletedAt`、`operatorId`。 | 同一附件重复事件只更新一次失效标记；补偿方式为重新上传并绑定附件。 |
