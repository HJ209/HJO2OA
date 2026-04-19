# 配置中心领域事件

事件信封统一遵循 `D:\idea-workspace\local\HJO2OA\docs\contracts\unified-event-contract.md`。

## 发布事件

| 事件类型 | 触发时机 | 关键载荷字段 | 说明 |
|----------|----------|--------------|------|
| `infra.config.updated` | 默认值、覆盖链、启停状态或回滚生效后 | `configEntryId`、`configKey`、`changeType`、`scopeType`、`scopeId`、`resolvedVersion`、`tenantId` | 汇总配置变更事件，供下游刷新运行快照。 |
| `infra.feature-flag.changed` | Feature Flag 规则新增、调整、启停或灰度比例变化后 | `configEntryId`、`configKey`、`ruleType`、`ruleId`、`changeType`、`sortOrder`、`tenantId` | 只用于开关类配置的命中规则更新。 |

## 消费事件

| 事件类型 | 消费目的 | 关键载荷字段 | 约束 |
|----------|----------|--------------|------|
| `infra.tenant.initialized` | 为新租户写入默认覆盖或启用套餐级 Feature Flag | `tenantId`、`packageCode`、`initializedAt` | 只生成租户级配置，不改写全局默认值。 |
| `org.organization.disabled` | 识别失效的组织级覆盖范围 | `organizationId`、`tenantId`、`timestamp` | 组织停用后，相关覆盖可被标记为待治理，但不自动删除历史记录。 |
| `org.role.disabled` | 识别失效的角色级 Feature Rule | `roleId`、`tenantId`、`timestamp` | 角色停用后，相关规则需进入治理列表。 |

## 幂等、补偿与投影约束

- 配置事件必须带齐统一事件信封字段，并包含 `configEntryId` 与 `configKey` 作为稳定标识。
- 下游热更新和本地缓存必须按 `eventId` 去重；对同一 `configEntryId + resolvedVersion + eventType` 的重复投递应幂等处理。
- 配置事件只描述策略真相源变化，运行时快照和缓存属于投影结果，不得反向写回配置表。
- 回滚补偿必须先修改 `ConfigEntry/ConfigOverride/FeatureRule` 真相源，再重新发布 `infra.config.updated`，不允许只改缓存不改真相源。
- 若下游模块应用配置失败，应记录自身错误并重试，不得要求配置模块回滚业务状态。
