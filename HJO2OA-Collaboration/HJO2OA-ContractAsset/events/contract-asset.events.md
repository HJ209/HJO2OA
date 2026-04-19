# contract-asset 事件说明

## 发布事件

| 事件 | 触发时机 | 关键载荷 | 下游使用 | 幂等/补偿/投影约束 |
|------|----------|----------|----------|--------------------|
| `biz.contract.expiring` | 合同命中提醒规则且进入临期窗口时发布。 | `contractId`、`contractNo`、`effectiveTo`、`ownerOrgId`、`counterpartyName`。 | `06` 发送临期提醒，`04` 展示到期合同卡片。 | 按 `contractId + effectiveTo + reminderWindow` 去重；续签后需撤销旧临期投影。 |
| `biz.asset.status-changed` | 资产流转成功并完成状态更新后发布。 | `assetId`、`assetCode`、`status`、`actionType`、`occurredAt`、`custodianId`。 | `04` 刷新资产卡片，`06` 发送流转提醒，`07` 供外部同步。 | 按 `assetId + occurredAt + actionType` 幂等；重放只覆盖最新状态投影。 |

## 消费事件

| 事件 | 来源 | 消费用途 | 关键载荷 | 幂等/补偿/投影约束 |
|------|------|----------|----------|--------------------|
| `process.instance.completed` | `02` | 合同审批通过后回写 `ACTIVE`，或将需审批的资产流转写为最终状态。 | `processInstanceId`、`businessKey`、`completedAt`、`result`。 | 按 `processInstanceId + completedAt` 幂等；回写成功后方可发布后续业务事件。 |
| `process.instance.terminated` | `02` | 合同审批驳回保持草稿态，需审批的资产流转撤回为原状态。 | `processInstanceId`、`businessKey`、`terminatedAt`、`reason`。 | 按 `processInstanceId + terminatedAt` 幂等；补偿必须恢复审批前主档快照。 |
| `org.person.resigned` | `01` | 调整合同经办人、资产保管人或待处理流转责任人。 | `personId`、`orgId`、`effectiveDate`。 | 仅处理未归档合同和未完成流转；重复事件按 `personId + effectiveDate` 去重。 |
| `infra.attachment.deleted` | `00` | 校验合同扫描件、补充协议或资产佐证附件引用失效。 | `attachmentId`、`deletedAt`、`operatorId`。 | 重复事件只更新一次引用失效状态；补偿通过重新绑定附件完成。 |
