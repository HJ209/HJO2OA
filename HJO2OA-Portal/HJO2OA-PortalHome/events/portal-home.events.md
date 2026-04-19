# portal-home 事件合同

## 事件定位

`portal-home` 是渲染层，不发布领域总线事件。一期也不直接依赖服务端 WebSocket 推送，它消费的是上游状态变化后的重新拉取结果，通过页面重装配和局部刷新反映事件影响。

## 发布事件

| 事件类型 | 说明 |
|----------|------|
| 无 | `portal-home` 不拥有领域真相源，不发布领域事件。 |

## 消费事件/状态变化

| 事件或状态变化 | 来源模块 | 页面处理动作 | 一期说明 |
|----------------|----------|--------------|----------|
| `portal.publication.activated` / `portal.publication.offlined` | `portal-model` | 清空当前页面壳并重新拉取模板解析结果。 | 通过下一次页面装配请求生效。 |
| `portal.personalization.saved` / `portal.personalization.reset` | `personalization` | 重新拉取个性化视图并局部重排卡片。 | 无需整页硬刷新。 |
| `portal.snapshot.refreshed` / `portal.snapshot.failed` | `aggregation-api` | 重新拉取受影响卡片或展示降级态。 | 通过轮询/手动刷新触发。 |
| `org.identity.switched` | 组织权限域 | 丢弃旧身份下的页面壳、个性化和卡片数据并全量重装配。 | 必须立即切断旧身份视图。 |

## 幂等、快照刷新与投影说明

- `portal-home` 不维护持久化投影表，只维护当前页面的前端装配状态。
- 重复收到同一状态变化时，页面只需要保留最后一次成功拉取到的模板壳和卡片数据。
- 一期刷新机制是“状态变化 -> 失效本地视图 -> 重新调用上游接口”，而不是“事件直推前端数据包”。
- 卡片级失败只影响局部区域，不应导致整个门户首页或办公中心不可用。
