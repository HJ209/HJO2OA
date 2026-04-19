# scheduler 领域事件

所有事件均遵循 `docs/contracts/unified-event-contract.md` 的统一信封，至少包含 `eventId`、`eventType`、`eventVersion`、`source`、`timestamp`、`correlationId`、`operatorAccountId`、`operatorPersonId`、`tenantId`、`traceId` 与 `payload`。

## 发布事件

| 事件类型 | eventVersion | 触发时机 | payload 关键字段 | 典型消费者 | 幂等/补偿/投影约束 |
|----------|--------------|----------|------------------|------------|-------------------|
| `infra.scheduler.task-succeeded` | `1` | 某次任务执行完成且结果为成功 | `jobId`、`jobCode`、`executionId`、`triggerType`、`attemptNo`、`startedAt`、`finishedAt`、`durationMs` | `tenant`、运维看板、审计投影 | 消费端按 `eventId` 去重；该事件只表示“调度成功执行”，不表示业务补偿已完成。 |
| `infra.scheduler.task-failed` | `1` | 某次任务执行失败且当前未继续自动重试 | `jobId`、`jobCode`、`executionId`、`triggerType`、`attemptNo`、`errorCode`、`errorMessage`、`retryable` | 运维告警、审计、`tenant` | 失败事件用于暴露治理入口，不允许消费者据此直接宣告业务回滚。 |
| `infra.scheduler.task-retrying` | `1` | 任务失败后进入自动或人工重试队列 | `jobId`、`jobCode`、`executionId`、`currentAttempt`、`maxAttempts`、`nextRetryAt`、`backoffPolicy` | 运维台、异常投影、`tenant` | 重试事件只更新运行态投影；重复消费应合并为同一执行链路视图。 |

## 消费事件

| 事件类型 | 来源模块 | 消费目的 | payload 关键字段 | 幂等/补偿/投影约束 |
|----------|----------|----------|------------------|-------------------|
| `infra.tenant.initialized` | `tenant` | 为租户侧周期任务、巡检任务或初始化后的补齐任务开通调度资格 | `tenantId`、`tenantCode`、`initializedAt` | 同一租户重复初始化事件只做幂等刷新，不重复创建任务定义。 |
| `infra.tenant.updated` | `tenant` | 刷新租户默认时区、套餐配额或调度开关映射 | `tenantId`、`changedFields`、`packageCode` | 仅更新任务作用域与运行策略投影，不改写历史执行记录。 |
| `infra.tenant.disabled` | `tenant` | 停止租户范围内的新触发任务 | `tenantId`、`reason`、`disabledAt` | 重复消费只保持停用态；不强制删除历史执行记录。 |
| `infra.config.updated` | `config` | 刷新全局并发上限、默认超时、失败告警阈值等运行参数 | `configKey`、`scopeType`、`scopeId`、`newValue` | 事件只更新运行参数缓存，参数真相源仍在 `config`。 |
| `infra.security.policy-updated` | `security` | 更新任务执行时使用的签名、密钥引用或限频策略 | `policyCode`、`policyType`、`changedFields` | 只刷新策略引用；密钥明文不进入 scheduler 投影或事件 payload。 |

## 幂等、补偿与投影约束

| 维度 | 约束 |
|------|------|
| 发布幂等 | scheduler 通过执行记录主键与 outbox 保证同一执行结果不会重复发布不同语义事件。 |
| 消费幂等 | 所有消费者必须至少按 `eventId` 去重；涉及任务定义派生时，还要按 `tenantId + jobCode` 做天然幂等。 |
| 补偿边界 | scheduler 失败事件只提供补偿入口，不定义业务补偿动作；业务补偿仍由业务模块自行建模。 |
| 投影原则 | 运行看板、失败统计、重试链路等均视为可重建投影，真相源始终是 `ScheduledJob` 与 `JobExecutionRecord`。 |
