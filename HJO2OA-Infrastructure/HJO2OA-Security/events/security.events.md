# security 领域事件

所有事件均遵循 `docs/contracts/unified-event-contract.md` 的统一信封，至少包含 `eventId`、`eventType`、`eventVersion`、`source`、`timestamp`、`correlationId`、`operatorAccountId`、`operatorPersonId`、`tenantId`、`traceId` 与 `payload`。

## 发布事件

| 事件类型 | eventVersion | 触发时机 | payload 关键字段 | 典型消费者 | 幂等/补偿/投影约束 |
|----------|--------------|----------|------------------|------------|-------------------|
| `infra.security.policy-updated` | `1` | 策略创建、更新、发布或回滚后生效版本发生变化 | `policyId`、`policyCode`、`policyType`、`policyVersion`、`scopeType`、`changedFields` | API 网关、开放接口、`tenant`、运维台 | 事件只同步策略版本与差异，不携带密钥明文；消费者按 `policyId + policyVersion` 幂等刷新。 |
| `infra.security.sensitive-operation` | `1` | 发生密钥轮换、策略回滚、强制解除限频等高风险操作 | `operationCode`、`targetType`、`targetId`、`result`、`riskLevel`、`occurredAt` | 审计、运维告警、消息模块 | 该事件是留痕与告警事件，不直接改变账号或权限主状态。 |
| `infra.security.anomaly-detected` | `1` | 命中异常检测规则，如异常登录、暴力破解、签名异常、策略越权修改 | `anomalyId`、`anomalyType`、`subjectType`、`subjectId`、`riskLevel`、`evidenceRefs`、`detectedAt` | 运维台、消息模块、审计 | 异常投影允许重放重建；重复消费应合并到同一异常编号。 |

## 消费事件

| 事件类型 | 来源模块 | 消费目的 | payload 关键字段 | 幂等/补偿/投影约束 |
|----------|----------|----------|------------------|-------------------|
| `org.account.login-succeeded` | `01-组织与权限` | 更新账号风险窗口、清理失败计数、补充登录成功审计 | `accountId`、`personId`、`loginIp`、`timestamp` | 只更新安全投影，不回写账号主模型。 |
| `org.account.login-failed` | `01-组织与权限` | 累积失败次数、识别暴力破解和异常地点登录 | `accountId`、`personId`、`reason`、`loginIp`、`timestamp` | 必须按 `eventId` 去重，避免重复累加失败次数。 |
| `infra.tenant.updated` | `tenant` | 刷新租户级安全策略作用域和默认基线 | `tenantId`、`changedFields`、`packageCode` | 只刷新作用域投影，不复制租户主数据。 |
| `infra.tenant.disabled` | `tenant` | 对停用租户的安全策略执行降权或冻结处理 | `tenantId`、`disabledAt`、`reason` | 不主动删除策略历史，只冻结生效范围。 |
| `infra.config.updated` | `config` | 刷新黑白名单、阈值、会话超时等全局安全配置映射 | `configKey`、`scopeType`、`newValue` | 配置变化只更新安全策略缓存，配置真相源仍属于 `config`。 |

## 幂等、补偿与投影约束

| 维度 | 约束 |
|------|------|
| 发布幂等 | `policy-updated` 通过策略版本和 outbox 保证语义唯一；敏感操作与异常事件按操作号或异常号唯一化。 |
| 消费幂等 | 登录结果和租户变化等输入事件必须至少按 `eventId` 去重，避免异常计数膨胀。 |
| 补偿边界 | security 只回滚安全策略版本，不自动回滚账号状态、角色权限或业务数据。 |
| 投影原则 | 异常列表、风险分布、敏感操作看板均视为可重建投影；策略真相源始终是 `SecurityPolicy` 及其关联规则。 |
