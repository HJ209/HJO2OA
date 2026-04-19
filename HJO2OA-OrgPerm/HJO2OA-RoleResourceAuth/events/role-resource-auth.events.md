# role-resource-auth 事件契约

## 发布事件

| 事件类型 | 触发时机 | 关键载荷字段 | 用途 |
|----------|----------|--------------|------|
| `org.role.created` | 角色创建成功后 | `eventId`、`tenantId`、`roleId`、`code`、`category`、`scope`、`status`、`occurredAt` | 初始化角色投影和权限缓存 |
| `org.role.updated` | 角色基础信息更新后 | `eventId`、`tenantId`、`roleId`、`changedFields`、`version`、`occurredAt` | 刷新角色读模型 |
| `org.role.disabled` | 角色停用后 | `eventId`、`tenantId`、`roleId`、`status`、`reason`、`occurredAt` | 触发继承失效和权限重算 |
| `org.role.position-bound` | 角色绑定岗位后 | `eventId`、`tenantId`、`roleId`、`positionId`、`occurredAt`、`reason` | 通知岗位继承链刷新 |
| `org.role.position-unbound` | 角色解绑岗位后 | `eventId`、`tenantId`、`roleId`、`positionId`、`occurredAt`、`reason` | 通知权限继承失效 |
| `org.role.person-granted` | 人员直授角色后 | `eventId`、`tenantId`、`roleId`、`personId`、`reason`、`expiresAt`、`occurredAt` | 通知最终角色集合重算 |
| `org.role.person-revoked` | 人员直授撤销后 | `eventId`、`tenantId`、`roleId`、`personId`、`reason`、`occurredAt` | 通知最终角色集合重算 |
| `org.resource-permission.changed` | 角色资源权限替换成功后 | `eventId`、`tenantId`、`roleId`、`resourceTypes`、`permissionCount`、`version`、`occurredAt` | 通知资源权限缓存失效 |

## 消费事件

| 事件类型 | 来源 | 消费动作 | 说明 |
|----------|------|----------|------|
| `org.position.updated` | `position-assignment` | 刷新岗位继承投影和绑定可用性校验 | 岗位归属变化会影响作用域解释 |
| `org.position.disabled` | `position-assignment` | 标记岗位绑定关系失效并刷新权限缓存 | 历史关系保留，不物理删除 |
| `org.assignment.created` | `position-assignment` | 触发人员最终角色集合重算 | 新任职会引入岗位继承角色 |
| `org.assignment.primary-changed` | `position-assignment` | 触发默认身份对应角色重算 | 影响主岗默认继承结果 |
| `org.assignment.removed` | `position-assignment` | 刷新人岗继承结果 | 移除后需剔除对应岗位角色 |
| `org.person.disabled` | `person-account` | 失效该人员的直授角色生效性并刷新缓存 | 审计轨迹仍保留 |
| `org.person.resigned` | `person-account` | 失效该人员的最终授权结果 | 历史授权记录不删除 |

## 关键载荷字段说明

| 字段 | 说明 |
|------|------|
| `scope` | 角色作用域，决定授权配置边界。 |
| `reason` | 人员直授、岗位绑定、停用、资源权限替换的业务原因。 |
| `resourceTypes` | 本次资源权限变更涉及的资源类型集合。 |
| `permissionCount` | 本次替换后的资源权限总条数，便于校验配置规模。 |
| `expiresAt` | 直授角色过期时间，为空表示长期有效。 |

## 幂等、补偿与投影说明

- 消费端必须按 `eventId` 幂等去重，并按 `roleId/personId/positionId + version` 做顺序控制。
- 资源权限配置建议发布“整角色快照变更事件”，不要按单条资源动作拆成大量细粒度事件。
- 角色模板应用不单独作为真相事件暴露，其结果体现在 `role.created`、`position-bound`、`resource-permission.changed` 等事实事件中。
- 角色停用、岗位解绑等补偿场景应发布新的失效事件表达，不允许依赖消费端自行猜测回滚逻辑。
