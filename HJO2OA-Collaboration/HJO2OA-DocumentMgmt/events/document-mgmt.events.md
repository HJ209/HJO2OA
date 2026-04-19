# document-mgmt 事件说明

## 发布事件

| 事件 | 触发时机 | 关键载荷 | 下游使用 | 幂等/补偿/投影约束 |
|------|----------|----------|----------|--------------------|
| `biz.document.submitted` | 公文从草稿进入审批流时发布。 | `documentId`、`documentType`、`initiatorId`、`processInstanceId`、`sponsorOrgId`。 | `02` 跟踪流程链路，`06` 生成审批提醒。 | 同一 `documentId + processInstanceId` 仅发布一次；提交失败需回滚为草稿态。 |
| `biz.document.approved` | 流程审批完成且公文回写为 `EFFECTIVE` 后发布。 | `documentId`、`documentType`、`documentNo`、`issuedAt`、`sponsorOrgId`。 | `04` 刷新门户卡片，`06` 触发生效通知，`03` 按需消费生效事实。 | 以 `documentId + status=EFFECTIVE` 去重；若文号生成失败，不得先发事件。 |
| `biz.document.archived` | 公文归档成功后发布。 | `documentId`、`documentType`、`archivedAt`、`operatorId`。 | `03` 按需索引归档事实，`04` 刷新归档查询入口。 | 同一 `documentId` 只允许一次归档事件；归档补偿只允许撤销投影，不回退归档主态。 |

## 消费事件

| 事件 | 来源 | 消费用途 | 关键载荷 | 幂等/补偿/投影约束 |
|------|------|----------|----------|--------------------|
| `process.instance.completed` | `02` | 将审批中的公文回写为 `EFFECTIVE`，补全文号、审批摘要和生效时间。 | `processInstanceId`、`businessKey`、`completedAt`、`result`。 | 按 `processInstanceId + completedAt` 幂等；若回写失败需重试，不得重复生成文号。 |
| `process.instance.terminated` | `02` | 将审批链路终止的公文回写为 `VOIDED`，并记录终止原因。 | `processInstanceId`、`businessKey`、`terminatedAt`、`reason`。 | 按 `processInstanceId + terminatedAt` 幂等；终止后不再允许进入归档投影。 |
| `infra.attachment.deleted` | `00` | 校验正文附件、签章证据引用是否失效，并阻断后续签发/归档。 | `attachmentId`、`deletedAt`、`operatorId`。 | 同一 `attachmentId` 重复事件只更新一次失效标记；补偿方式为重新绑定有效附件。 |
