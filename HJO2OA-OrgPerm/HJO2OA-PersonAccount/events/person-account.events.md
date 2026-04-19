# person-account 事件契约

## 发布事件

| 事件类型 | 触发时机 | 关键载荷字段 | 用途 |
|----------|----------|--------------|------|
| `org.person.created` | 人员创建成功后 | `eventId`、`tenantId`、`personId`、`employeeNo`、`organizationId`、`departmentId`、`status`、`occurredAt` | 通知任职、身份上下文和审计模块初始化人员事实 |
| `org.person.updated` | 人员基础信息更新后 | `eventId`、`tenantId`、`personId`、`changedFields`、`version`、`occurredAt` | 刷新人员读模型和身份摘要 |
| `org.person.disabled` | 人员停用后 | `eventId`、`tenantId`、`personId`、`status`、`reason`、`occurredAt` | 驱动任职、上下文和账号状态联动 |
| `org.person.resigned` | 人员离职后 | `eventId`、`tenantId`、`personId`、`status`、`reason`、`occurredAt` | 驱动任职转历史、账号锁定和审计归档 |
| `org.account.created` | 账号创建成功后 | `eventId`、`tenantId`、`accountId`、`personId`、`username`、`authType`、`isPrimary`、`occurredAt` | 初始化账号读模型和身份绑定关系 |
| `org.account.locked` | 账号锁定后 | `eventId`、`tenantId`、`accountId`、`personId`、`reason`、`lockedUntil`、`occurredAt` | 通知身份上下文和安全运营模块 |
| `org.account.unlocked` | 账号解锁后 | `eventId`、`tenantId`、`accountId`、`personId`、`occurredAt` | 通知会话和安全状态恢复 |
| `org.account.login-succeeded` | 登录成功状态回写后 | `eventId`、`tenantId`、`accountId`、`personId`、`loginIp`、`occurredAt` | 更新安全画像与最后登录信息 |
| `org.account.login-failed` | 登录失败状态回写后 | `eventId`、`tenantId`、`accountId`、`personId`、`reason`、`occurredAt` | 触发失败累计、风险控制和审计 |

## 消费事件

| 事件类型 | 来源 | 消费动作 | 说明 |
|----------|------|----------|------|
| `org.organization.disabled` | `org-structure` | 刷新人员归属校验缓存 | 已有人员保留历史引用，新增/编辑需受限 |
| `org.department.disabled` | `org-structure` | 刷新人员归属校验缓存 | 禁止把新人员挂到停用部门 |
| `org.assignment.created` | `position-assignment` | 更新人员身份总览只读投影 | 用于人员详情展示主岗/兼岗 |
| `org.assignment.primary-changed` | `position-assignment` | 刷新人员主岗摘要和默认身份提示 | 不回写人员主属组织 |
| `org.assignment.removed` | `position-assignment` | 刷新人员任职总览 | 移除后详情页同步更新 |
| `org.assignment.expired` | `position-assignment` | 刷新人员任职总览 | 到期任职不再作为当前身份展示 |

## 关键载荷字段说明

| 字段 | 说明 |
|------|------|
| `employeeNo` | 人员工号，是人员档案业务唯一键。 |
| `authType` | 认证类型，取值与领域模型保持一致。 |
| `isPrimary` | 标识是否为人员当前主账号。 |
| `changedFields` | 人员或账号更新字段摘要，供审计和缓存刷新使用。 |
| `reason` | 停用、离职、锁定、登录失败等动作原因。 |

## 幂等、补偿与投影说明

- 人员和账号事件都必须携带 `eventId` 与聚合版本，消费端按 `personId/accountId + version` 保序。
- 账号锁定和解锁事件可能由安全策略重复触发，消费端必须按 `eventId` 去重，避免反复刷新会话。
- 人员身份总览是只读投影，来源于人员、账号、任职、角色多个事件；投影异常不允许反向覆盖真相源。
- 密码重置一期不发布独立领域事件，统一进入安全审计与账号更新审计，避免把临时密码传播到事件总线。
