# org-sync-audit 事件契约

## 发布事件

| 事件类型 | 触发时机 | 关键载荷字段 | 用途 |
|----------|----------|--------------|------|
| `org.sync.completed` | 同步任务成功结束后 | `eventId`、`tenantId`、`syncTaskId`、`sourceId`、`sourceType`、`syncMode`、`createdCount`、`updatedCount`、`failedCount`、`occurredAt` | 通知运维、监控和任务看板刷新 |
| `org.sync.failed` | 同步任务失败或部分失败后 | `eventId`、`tenantId`、`syncTaskId`、`sourceId`、`sourceType`、`syncMode`、`errorCode`、`errorSummary`、`occurredAt` | 通知失败告警和重试流程 |
| `org.audit.org-changed` | 组织、部门、岗位、任职类变更写入审计后 | `eventId`、`tenantId`、`auditLogId`、`entityType`、`entityId`、`action`、`operatorId`、`occurredAt` | 对外提供组织主数据治理轨迹 |
| `org.audit.auth-changed` | 角色、资源权限、数据权限变更写入审计后 | `eventId`、`tenantId`、`auditLogId`、`entityType`、`entityId`、`action`、`operatorId`、`occurredAt` | 对外提供授权治理轨迹 |
| `org.audit.account-changed` | 账号状态、安全动作写入审计后 | `eventId`、`tenantId`、`auditLogId`、`entityType`、`entityId`、`action`、`operatorId`、`occurredAt` | 对外提供账号治理轨迹 |

## 消费事件

| 事件类型 | 来源 | 消费动作 | 说明 |
|----------|------|----------|------|
| `org.organization.*`、`org.department.*` | `org-structure` | 生成组织类审计日志和同步对账基线 | 组织树变化进入治理时间线 |
| `org.position.*`、`org.assignment.*` | `position-assignment` | 生成岗位/任职审计日志，更新同步对账状态 | 任职变化影响身份与同步治理 |
| `org.person.*`、`org.account.*` | `person-account` | 生成人员/账号审计日志和失败补偿线索 | 账号锁定、离职等都需留痕 |
| `org.role.*`、`org.resource-permission.changed` | `role-resource-auth` | 生成授权治理日志 | 角色、绑定、资源权限配置都需回溯 |
| `org.data-permission.*` | `data-permission` | 生成数据权限治理日志 | 行级/字段级权限变化进入审计 |
| `org.identity.switched` | `identity-context` | 可选记录身份切换操作轨迹 | 仅做运行治理留痕，不改变主数据 |

## 关键载荷字段说明

| 字段 | 说明 |
|------|------|
| `syncMode` | 同步方式，取值 `FULL` 或 `INCREMENTAL`。 |
| `errorSummary` | 失败摘要，用于告警和任务看板展示。 |
| `auditLogId` | 审计记录唯一标识，用于详情跳转。 |
| `entityType/entityId` | 被审计实体类型与标识，支持定位真相源对象。 |
| `operatorId` | 操作者或任务触发者；系统任务可使用系统账号标识。 |

## 幂等、补偿与投影说明

- 审计日志写入应以 `eventId` 幂等去重，避免上游重复投递导致时间线重复。
- 同步任务事件建议按 `syncTaskId` 保序，任务看板和告警系统以任务为聚合维度刷新。
- 差异处理或补偿成功后，不应修改原审计记录，而应追加新的审计节点保持全链路可追溯。
- 组织、授权、账号三类审计可以拥有不同投影视图，但都必须可反查原始领域事件和关联任务。
