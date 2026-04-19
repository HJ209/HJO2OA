# EventBus 事件说明

`event-bus` 是 `00` 基础设施域中 `infra.event.*` 技术事件的核心发布者，同时是所有已注册跨模块事件的统一入口消费者。它只发布总线治理状态，不替代业务域事件语义。

## 发布事件

| 事件 | 触发时机 | 关键载荷 | 下游用途 | 幂等/补偿/投影约束 |
|------|----------|----------|----------|--------------------|
| `infra.event.schema-registered` | 事件定义注册完成并进入可发布状态后 | `eventType`、`eventVersion`、`modulePrefix`、`schemaChecksum`、`registeredAt` | 通知开发治理工具、订阅方和审计模块刷新契约目录 | 以 `eventType + eventVersion` 幂等；重复投递只更新同一版本元数据；仅表示契约就绪，不表示业务事件已发生 |
| `infra.event.schema-deprecated` | 事件定义进入废弃窗口并声明 Sunset 策略后 | `eventType`、`eventVersion`、`deprecatedAt`、`sunsetAt`、`replacementEventType` | 通知订阅方、治理工具和审计模块准备迁移或停止消费旧契约 | 以 `eventType + eventVersion` 幂等；重复投递只刷新废弃元数据；只表示契约治理状态变化，不表示业务事实变化 |
| `infra.event.published` | 事件信封完成持久化并进入投递生命周期后 | `eventId`、`eventType`、`eventVersion`、`source`、`tenantId`、`correlationId`、`publishedAt` | 通知审计、缓存治理、监控和消息触达进入后续链路 | 以 `eventId` 幂等；若后续投递失败不会回滚该事件，而是进入失败/死信治理链路 |
| `infra.event.delivery-failed` | 某个消费者投递失败并进入重试或待人工处置时 | `eventId`、`eventType`、`subscriberCode`、`attemptNo`、`errorCode`、`nextRetryAt` | 通知运维、监控与触达系统聚焦异常投递 | 以 `eventId + subscriberCode + attemptNo` 幂等；失败补偿通过后续重试或人工回放完成，不重写原事件信封 |
| `infra.event.dead-lettered` | 消费重试耗尽或人工判定转入死信时 | `eventId`、`eventType`、`subscriberCode`、`deadLetterId`、`deadLetteredAt`、`traceId` | 供运维治理台、审计和告警模块消费 | 以 `eventId + subscriberCode` 幂等；死信回放只生成新的投递尝试，不重新发布业务事件 |

## 消费事件

| 事件 | 来源 | 消费目的 | 关键载荷 | 幂等/补偿/投影约束 |
|------|------|----------|----------|--------------------|
| 已注册跨模块领域事件（`org.*` / `process.*` / `content.*` / `portal.*` / `biz.*` / `msg.*` / `data.*` / `infra.*`） | `00-07` 各模块 | 执行事件契约校验、事件信封落库、订阅路由和可靠投递 | 标准事件信封：`eventId`、`eventType`、`eventVersion`、`source`、`timestamp`、`tenantId`、`payload` | 按 `eventId` 幂等入总线；总线只做契约校验、路由和投递，不推断业务含义；发布方必须自行保证 Outbox 事务性 |
| `infra.config.updated` | `HJO2OA-Config` | 刷新重试阈值、死信保留期、事件留存期和告警阈值等运行参数 | `configKey`、`scope`、`configVersion`、`changedAt` | 按 `configKey + configVersion` 幂等；只更新运行参数投影，不回改历史事件或投递记录 |

## 事件约束

| 项 | 说明 |
|----|------|
| 命名约束 | 平台总线治理事件统一使用 `infra.event.*` 前缀和 kebab-case 动作命名 |
| 信封约束 | 所有入总线事件必须符合统一事件契约，包含 `eventId`、`eventType`、`eventVersion`、`tenantId`、`traceId` 等基础字段 |
| 补偿约束 | 回放只针对投递链路，业务补偿、业务回滚和幂等仍归发布方或消费方领域负责 |
| 投影约束 | 运行统计、监控看板、死信面板都是运行投影，不替代 `EventDefinition` 和 `EventMessage` 真相源 |
