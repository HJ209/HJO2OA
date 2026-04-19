# contract-asset API 说明

## 接口分组

| 分组 | 关键接口 | 关键查询/写入 | 分页与筛选 | 幂等 | 权限与审计 | 错误处理 |
|------|----------|---------------|------------|------|------------|----------|
| 合同台账查询 | `GET /contracts` `GET /contracts/{id}` | 查询合同主档、履约节点、提醒规则和附件摘要。 | 支持 `page`、`size`、`sort`；筛选 `contractNo`、`contractType`、`status`、`ownerOrgId`、`counterpartyName`、`effectiveTo`。 | 查询天然幂等。 | 合同经办人、组织管理员、审计角色可见；访问写审计。 | `403` 越权查看，`404` 合同不存在。 |
| 合同写入与审批 | `POST /contracts` `PUT /contracts/{id}` `POST /contracts/{id}/submit` `POST /contracts/process-callback` | 创建/修改合同草稿、提交审批、接收审批回写。 | 写接口不分页。 | 创建按 `Idempotency-Key` 或 `contractNo` 去重；回调按 `processInstanceId + callbackState` 幂等。 | 仅合同经办人与授权管理员可写；提交、审批回写必须审计。 | `409` 编号重复或状态冲突，`422` 缺履约信息。 |
| 履约节点与提醒 | `GET /contracts/{id}/milestones` `PUT /contracts/{id}/milestones` `PUT /contracts/{id}/reminders` | 查询/维护履约节点和提醒规则。 | 支持按 `status`、`dueDate` 筛选。 | 节点更新按 `contractId + milestoneName + dueDate` 去重；提醒按 `contractId` 幂等覆盖。 | 仅合同经办人、组织管理员可维护；节点变更写审计。 | `404` 节点不存在，`409` 已归档合同禁止修改。 |
| 资产台账查询 | `GET /assets` `GET /assets/{id}` `GET /assets/{id}/transfers` | 查询资产主档、当前状态、保管人和流转历史。 | 支持 `page`、`size`、`sort`；筛选 `assetCode`、`categoryCode`、`status`、`ownerOrgId`、`custodianId`。 | 查询天然幂等。 | 资产管理员、保管人、审计角色可见；访问写审计。 | `403` 越权查看，`404` 资产不存在。 |
| 资产写入与流转 | `POST /assets` `PUT /assets/{id}` `POST /assets/{id}/transfers` | 新建/修改资产主档，发起领用、借用、归还、盘点、报废流转。 | 写接口不分页。 | 资产创建按 `assetCode` 去重；流转按 `assetId + actionType + requestId` 去重。 | 仅资产管理员可维护主档；流转要求记录操作者、目标责任人和发生时间。 | `409` 状态冲突，`422` 缺目标责任人或不允许的流转动作。 |

## 通用约束

| 主题 | 说明 |
|------|------|
| 认证与权限 | 统一复用 `01` 的组织、岗位、人员和数据权限。 |
| 审批接入 | 审批统一复用 `02`；审批结果显式回写合同/资产主档。 |
| 审计要求 | 合同创建、激活、节点变更、资产流转、状态变更都必须全量审计。 |
| 错误口径 | 重点区分对象不存在、编号重复、状态冲突、依赖失败、越权操作。 |
| 开放边界 | 对外数据交换统一经 `07` 暴露，本模块不直接建设开放接口。 |
