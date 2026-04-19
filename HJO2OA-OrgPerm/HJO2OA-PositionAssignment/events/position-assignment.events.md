# position-assignment 事件契约

## 发布事件

| 事件类型 | 触发时机 | 关键载荷字段 | 用途 |
|----------|----------|--------------|------|
| `org.position.created` | 岗位创建成功后 | `eventId`、`tenantId`、`positionId`、`organizationId`、`departmentId`、`category`、`status`、`occurredAt` | 初始化岗位读模型与角色继承输入 |
| `org.position.updated` | 岗位基础信息或归属更新后 | `eventId`、`tenantId`、`positionId`、`changedFields`、`version`、`occurredAt` | 触发身份和权限相关缓存重算 |
| `org.position.disabled` | 岗位停用后 | `eventId`、`tenantId`、`positionId`、`status`、`reason`、`occurredAt` | 通知下游禁止新增任职并刷新上下文 |
| `org.assignment.created` | 新增任职成功后 | `eventId`、`tenantId`、`assignmentId`、`personId`、`positionId`、`type`、`startDate`、`endDate` | 驱动身份上下文初始化和授权继承 |
| `org.assignment.primary-changed` | 主岗切换成功后 | `eventId`、`tenantId`、`personId`、`oldPositionId`、`newPositionId`、`occurredAt`、`reason` | 触发默认身份重算 |
| `org.assignment.removed` | 任职移除成功后 | `eventId`、`tenantId`、`assignmentId`、`personId`、`positionId`、`occurredAt`、`reason` | 驱动上下文失效和审计记录 |
| `org.assignment.expired` | 任职因到期自动失效后 | `eventId`、`tenantId`、`assignmentId`、`personId`、`positionId`、`endDate`、`occurredAt` | 驱动异步清理当前身份缓存 |

## 消费事件

| 事件类型 | 来源 | 消费动作 | 说明 |
|----------|------|----------|------|
| `org.organization.disabled` | `org-structure` | 刷新岗位归属校验缓存，阻止向停用组织新增岗位或调岗 | 不直接删除历史岗位 |
| `org.department.disabled` | `org-structure` | 刷新岗位归属校验缓存，阻止向停用部门新增岗位 | 保留既有历史引用 |
| `org.person.disabled` | `person-account` | 将该人员当前有效任职转为历史状态或标记不可用 | 保障停用人员不能继续作为当前身份 |
| `org.person.resigned` | `person-account` | 批量结束该人员有效任职并发布补充审计 | 离职后任职只保留历史 |
| `org.role.position-bound` | `role-resource-auth` | 刷新岗位角色来源只读投影 | 不在本模块写入绑定关系 |
| `org.role.position-unbound` | `role-resource-auth` | 刷新岗位角色来源只读投影 | 仅更新查看结果 |

## 关键载荷字段说明

| 字段 | 说明 |
|------|------|
| `type` | 任职类型，取值 `PRIMARY` 或 `SECONDARY`。 |
| `changedFields` | 岗位归属、名称、类别、等级等变化的字段摘要。 |
| `reason` | 主岗切换、任职移除、岗位停用等操作原因，审计必填。 |
| `version` | 岗位或任职聚合版本，供消费端丢弃旧事件。 |

## 幂等、补偿与投影说明

- 任职相关事件必须保证“同一人员任职集合”的顺序一致性，建议按 `personId` 做分区。
- 消费端以 `eventId` 去重，并以 `personId + version` 或 `positionId + version` 做顺序保护。
- 主岗切换失败时不得发布半成品事件；补偿应通过新的 `primary-changed` 或 `removed` 事件体现。
- 角色来源查看、岗位任职摘要等读模型可异步投影，但不得作为任职真相源回写本模块。
