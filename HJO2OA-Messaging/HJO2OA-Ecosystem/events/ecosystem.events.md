# ecosystem 事件契约

## 1. 事件角色

`ecosystem` 以消费为主，默认不发布独立领域事件。

- 本子模块不新增 `ecosystem.*` 命名空间。
- 若发送适配器部署在本模块内，也只复用 `msg.channel.*` 契约，不再派生新事件名。

## 2. 发布事件

| 类型 | 说明 |
|------|------|
| 无 | 一期不发布独立领域事件 |

## 3. 消费事件

### 3.1 投递链路消费

| 事件类型 | 触发动作 | 关键载荷字段 | 说明 |
|----------|----------|--------------|------|
| `msg.channel.delivery-requested` | 对外部生态渠道执行适配调用或治理检查 | `deliveryTaskId`、`notificationId`、`channelType`、`endpointId`、`providerConfigRef` | `INBOX` 不进入本模块 |
| `msg.channel.failed` | 更新接入健康状态或告警计数 | `deliveryTaskId`、`channelType`、`errorCode`、`retryCount` | 连续失败可将健康状态降为 `DEGRADED` |

### 3.2 回调输入

- 第三方回调默认通过开放回调接口进入，不强制转换为总线事件。
- 回调处理完成后只写审计和状态更新，不新增独立出站事件。

## 4. 关键载荷字段要求

- 外部渠道投递事件必须带 `providerConfigRef` 或可解析到该配置引用的端点信息。
- 回调幂等至少需要 `providerEventId` 或 `providerMessageId`。
- 所有消费处理必须带租户上下文，禁止跨租户复用连接配置。

## 5. 至少一次消费、重试与幂等

- 事件消费语义为至少一次。
- `msg.channel.delivery-requested` 以 `deliveryTaskId + attemptNo` 作为执行幂等键。
- 回调处理以 `providerEventId + integrationId` 做幂等。
- 连接调用失败按投递链路重试策略处理；本模块自身不再叠加第二套退避时间表。

## 6. 死信与补偿说明

- 外部渠道调用或回调处理超过重试上限后，记录死信和审计摘要。
- 死信补偿允许重放同一投递任务或回调请求，但不得修改原始配置引用。
- 健康状态降级是治理结果，不等价于删除接入配置。
