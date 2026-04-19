# data-permission 事件契约

## 发布事件

| 事件类型 | 触发时机 | 关键载荷字段 | 用途 |
|----------|----------|--------------|------|
| `org.data-permission.row-changed` | 行级策略新增、更新、删除后 | `eventId`、`tenantId`、`policyId`、`subjectType`、`subjectId`、`businessObject`、`scopeType`、`effect`、`priority`、`occurredAt` | 通知决策缓存和审计投影刷新 |
| `org.data-permission.field-changed` | 字段策略新增、更新、删除后 | `eventId`、`tenantId`、`policyId`、`subjectType`、`subjectId`、`businessObject`、`usageScenario`、`fieldCode`、`action`、`effect`、`occurredAt` | 通知字段权限缓存与表单配置刷新 |

## 消费事件

| 事件类型 | 来源 | 消费动作 | 说明 |
|----------|------|----------|------|
| `org.role.disabled` | `role-resource-auth` | 刷新以角色为主体的数据权限缓存 | 角色停用后策略仍保留，但不再生效 |
| `org.role.person-granted` | `role-resource-auth` | 刷新人员最终数据权限预览缓存 | 角色新增会影响最终数据范围 |
| `org.role.person-revoked` | `role-resource-auth` | 刷新人员最终数据权限预览缓存 | 角色撤销会影响最终数据范围 |
| `org.assignment.primary-changed` | `position-assignment` | 刷新主岗相关数据权限结果 | 默认身份变化会影响岗位主体命中 |
| `org.position.disabled` | `position-assignment` | 失效岗位主体相关决策缓存 | 停用岗位不应再作为当前身份 |
| `org.person.disabled` | `person-account` | 失效该人员的决策缓存 | 停用人员不应继续持有有效数据权限 |
| `org.identity.switched` | `identity-context` | 刷新会话态预览缓存 | 切换兼岗后需重新计算数据范围 |

## 关键载荷字段说明

| 字段 | 说明 |
|------|------|
| `scopeType` | 行级范围类型，取值遵循领域模型。 |
| `priority` | 行级策略优先级，数值越大越优先。 |
| `usageScenario` | 字段权限使用场景，如 `view`、`edit`、`export`。 |
| `action` | 字段动作或资源动作，字段级遵循 `VISIBLE/EDITABLE/EXPORTABLE/DESENSITIZED/HIDDEN`。 |

## 幂等、补偿与投影说明

- 数据权限变更事件必须携带 `policyId` 和 `eventId`，消费端按此幂等去重。
- 决策缓存、权限预览读模型和审计投影都应消费这些事件，但不得反向覆盖策略真相源。
- 当策略删除或主体失效时，应通过新的 `row-changed/field-changed` 事件表达最新状态，不依赖消费者自行推断。
- 预览页可异步缓存结果，但缓存失效必须受 `identity.switched`、`role.*`、`assignment.*` 等事件驱动。
