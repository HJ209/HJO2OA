# identity-context 事件契约

## 发布事件

| 事件类型 | 触发时机 | 关键载荷字段 | 用途 |
|----------|----------|--------------|------|
| `org.identity.switched` | 用户切换兼岗或切回主岗成功后 | `eventId`、`tenantId`、`personId`、`accountId`、`fromPositionId`、`toPositionId`、`fromPositionType`、`toPositionType`、`occurredAt` | 通知门户、消息、待办、数据权限和缓存刷新 |

## 消费事件

| 事件类型 | 来源 | 消费动作 | 说明 |
|----------|------|----------|------|
| `org.assignment.created` | `position-assignment` | 若当前人员尚无上下文，则初始化默认主岗上下文 | 新增任职后可能产生新的默认身份 |
| `org.assignment.primary-changed` | `position-assignment` | 重算默认上下文并在必要时把当前身份切回新主岗 | 主岗变化是最核心触发器 |
| `org.assignment.removed` | `position-assignment` | 若移除的是当前身份对应任职，则回退到主岗或清空上下文 | 防止会话继续使用无效身份 |
| `org.assignment.expired` | `position-assignment` | 失效到期任职对应上下文缓存 | 到期兼岗不能继续切换或使用 |
| `org.position.updated` | `position-assignment` | 刷新当前岗位的组织/部门显示信息 | 岗位归属变化需要同步到上下文 |
| `org.position.disabled` | `position-assignment` | 若当前身份岗位停用，则回退到主岗或终止上下文 | 保证当前上下文始终有效 |
| `org.role.position-bound` | `role-resource-auth` | 刷新岗位继承角色集合 | 角色集合变化需重算上下文 |
| `org.role.position-unbound` | `role-resource-auth` | 刷新岗位继承角色集合 | 岗位解绑角色后应立即失效 |
| `org.role.person-granted` | `role-resource-auth` | 刷新人员直授角色集合 | 影响最终角色集合 |
| `org.role.person-revoked` | `role-resource-auth` | 刷新人员直授角色集合 | 影响最终角色集合 |
| `org.person.disabled` | `person-account` | 清空该人员会话上下文并失效缓存 | 停用后不得继续使用上下文 |
| `org.person.resigned` | `person-account` | 清空该人员会话上下文并终止切换能力 | 离职后上下文必须失效 |
| `org.account.locked` | `person-account` | 失效当前账号会话上下文 | 锁定账号后应阻断上下文继续使用 |
| `org.account.unlocked` | `person-account` | 允许后续重新建立上下文 | 解锁不直接恢复旧会话 |

## 关键载荷字段说明

| 字段 | 说明 |
|------|------|
| `fromPositionId` / `toPositionId` | 切换前后的岗位标识。 |
| `fromPositionType` / `toPositionType` | 切换前后的岗位类型，取值 `PRIMARY` 或 `SECONDARY`。 |
| `effectiveAt` | 当前上下文实际生效时间，体现在 `current` 接口返回中。 |

## 幂等、补偿与投影说明

- `org.identity.switched` 只在切换真正成功后发布，不允许预发布。
- 门户菜单、待办数量、消息范围、数据权限等缓存必须以 `eventId` 幂等去重，并按 `personId` 保序刷新。
- 后台重算失败时应保留旧上下文继续生效，并通过补偿任务重试，不允许留下半刷新状态。
- 一期不单独发布“context refreshed”事件，后台被动重算以内部刷新和缓存失效为主，避免事件噪音。
