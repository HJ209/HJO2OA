# org-structure 事件契约

## 发布事件

| 事件类型 | 触发时机 | 关键载荷字段 | 用途 |
|----------|----------|--------------|------|
| `org.organization.created` | 组织创建成功后 | `eventId`、`tenantId`、`organizationId`、`code`、`parentId`、`status`、`occurredAt`、`operatorId` | 通知同步治理、审计和下游读模型初始化 |
| `org.organization.updated` | 组织基础信息更新后 | `eventId`、`tenantId`、`organizationId`、`changedFields`、`version`、`occurredAt` | 触发读模型刷新与审计归档 |
| `org.organization.hierarchy-changed` | 组织层级调整成功后 | `eventId`、`tenantId`、`organizationId`、`oldParentId`、`newParentId`、`affectedDescendantIds`、`version` | 触发组织树投影刷新和影响范围重算 |
| `org.organization.disabled` | 组织状态切为 `DISABLED` 后 | `eventId`、`tenantId`、`organizationId`、`status`、`reason`、`occurredAt` | 通知下游限制新增绑定并刷新缓存 |
| `org.department.created` | 部门创建成功后 | `eventId`、`tenantId`、`departmentId`、`organizationId`、`parentId`、`status`、`occurredAt` | 初始化部门相关投影 |
| `org.department.updated` | 部门基础信息更新后 | `eventId`、`tenantId`、`departmentId`、`changedFields`、`version`、`occurredAt` | 触发部门读模型刷新 |
| `org.department.hierarchy-changed` | 部门层级或所属组织调整成功后 | `eventId`、`tenantId`、`departmentId`、`organizationId`、`oldParentId`、`newParentId`、`affectedDescendantIds` | 触发部门树和组织归属投影重算 |
| `org.department.disabled` | 部门状态切为 `DISABLED` 后 | `eventId`、`tenantId`、`departmentId`、`organizationId`、`reason`、`occurredAt` | 通知下游限制新增岗位/人员绑定 |

## 消费事件

| 事件类型 | 来源 | 一期是否消费 | 说明 |
|----------|------|--------------|------|
| 无强制消费事件 | - | 是 | 一期组织主数据仍以同步命令/API 为唯一写入口，避免多个异步入口同时改写树结构。 |

## 关键载荷字段说明

| 字段 | 说明 |
|------|------|
| `eventId` | 事件唯一标识，用于消费端幂等去重。 |
| `tenantId` | 只用于隔离消费数据，不参与组织层级语义判断。 |
| `changedFields` | 返回变更字段名列表和新旧值摘要，供审计与缓存失效使用。 |
| `affectedDescendantIds` | 层级调整时受影响的所有后代节点标识，供下游批量刷新投影。 |
| `version` | 聚合版本号，消费端可据此丢弃旧事件。 |
| `reason` | 启停用、导入确认等高风险操作的业务原因，审计必填。 |

## 幂等、补偿与投影说明

- 事件发布必须与组织/部门写事务保持同一提交边界，推荐使用本地消息表或事务消息。
- 消费端应以 `eventId` 去重，并结合 `organizationId/departmentId + version` 做顺序保护。
- 层级变更事件只发布最终结果，不发布中间状态；失败补偿通过后续更正事件表达，不允许回滚为未定义状态。
- 组织树、部门树、影响摘要等读模型都应以 `hierarchy-changed` 为刷新触发点，不自行推导树路径。
- 审计模块应同时落地原始事件载荷，保证组织主数据变更可回放、可追责。
