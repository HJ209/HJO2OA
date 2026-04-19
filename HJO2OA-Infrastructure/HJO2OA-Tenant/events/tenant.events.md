# tenant 领域事件

所有事件均遵循 `docs/contracts/unified-event-contract.md` 的统一信封，至少包含 `eventId`、`eventType`、`eventVersion`、`source`、`timestamp`、`correlationId`、`operatorAccountId`、`operatorPersonId`、`tenantId`、`traceId` 与 `payload`。

## 发布事件

| 事件类型 | eventVersion | 触发时机 | payload 关键字段 | 典型消费者 | 幂等/补偿/投影约束 |
|----------|--------------|----------|------------------|------------|-------------------|
| `infra.tenant.created` | `1` | 租户档案和基础配额创建完成 | `tenantId`、`tenantCode`、`tenantName`、`packageCode`、`isolationMode` | `config`、`dictionary`、`portal`、`process`、运维台 | 消费端按 `tenantId` 幂等初始化自己的租户级投影，不得重复创建租户主档。 |
| `infra.tenant.updated` | `1` | 租户名称、默认语言/时区、套餐或状态等信息发生变更 | `tenantId`、`tenantCode`、`changedFields`、`packageCode`、`defaultLocale`、`defaultTimezone` | `scheduler`、`security`、运维投影 | 仅用于刷新派生配置，真相源仍在 `TenantProfile`。 |
| `infra.tenant.disabled` | `1` | 租户被治理性停用 | `tenantId`、`tenantCode`、`reason`、`disabledAt` | `scheduler`、接入网关、业务模块 | 重复消费只保持禁用态；消费者不得据此物理删除业务数据。 |
| `infra.tenant.initialized` | `1` | 初始化编排全部完成并可对外提供服务 | `tenantId`、`tenantCode`、`initBatchId`、`initializedAt`、`completedSteps` | 各业务模块、运维台 | 允许消费者按 `tenantId + initBatchId` 去重；事件只表明平台初始化完成，不代表业务数据已全部填充。 |
| `infra.tenant.quota-warning` | `1` | 配额使用达到预警阈值或接近上限 | `tenantId`、`quotaType`、`usedValue`、`limitValue`、`warningThreshold`、`observedAt` | 运维告警、消息模块、配额看板 | 预警是通知事件，不直接变更配额真相源，也不自动执行资源删除。 |

## 消费事件

| 事件类型 | 来源模块 | 消费目的 | payload 关键字段 | 幂等/补偿/投影约束 |
|----------|----------|----------|------------------|-------------------|
| `infra.config.updated` | `config` | 刷新默认套餐模板、初始化步骤模板和配额阈值规则 | `configKey`、`scopeType`、`scopeId`、`newValue` | 仅更新策略缓存，配置真相源仍属于 `config`。 |
| `infra.security.policy-updated` | `security` | 记录租户基线安全策略版本变更，供后续初始化或巡检使用 | `policyCode`、`policyType`、`changedFields` | 仅刷新引用关系，不复制安全策略主数据。 |
| `infra.scheduler.task-succeeded` | `scheduler` | 收敛初始化编排或配额对账任务的异步完成状态 | `jobCode`、`executionId`、`tenantId`、`finishedAt` | 仅更新初始化/对账投影，不改变租户档案真相源。 |
| `infra.scheduler.task-failed` | `scheduler` | 标记初始化步骤或对账任务失败，暴露人工重试入口 | `jobCode`、`executionId`、`tenantId`、`errorCode` | 不得自动停用租户；是否补偿由治理动作确认。 |

## 幂等、补偿与投影约束

| 维度 | 约束 |
|------|------|
| 发布幂等 | 租户创建、停用、初始化完成等事件通过租户版本号和 outbox 保证语义唯一。 |
| 消费幂等 | 下游模块至少按 `eventId` 去重；租户初始化类联动还应按 `tenantId + initBatchId` 做自然幂等。 |
| 补偿边界 | tenant 只提供初始化重试和配额回滚入口，不负责自动撤销业务模块已创建的业务数据。 |
| 投影原则 | 初始化进度、配额看板、预警列表等均为可重建投影；租户真相源始终是 `TenantProfile` 与 `TenantQuota`。 |
