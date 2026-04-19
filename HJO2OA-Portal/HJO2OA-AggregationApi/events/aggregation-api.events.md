# aggregation-api 事件合同

## 事件定位

`aggregation-api` 是 `04` 中最主要的事件消费者，负责根据业务事件、模板事件和个性化事件失效或重建聚合快照。同时它发布快照刷新结果事件供渲染层与诊断逻辑使用。

## 发布事件

| 事件类型 | 触发时机 | 关键载荷字段 | 下游用途 |
|----------|----------|--------------|----------|
| `portal.snapshot.refreshed` | 某个卡片快照刷新成功 | `snapshotKey`、`cardType`、`refreshedAt` | 供 `portal-home` 或诊断逻辑判断卡片已可重新拉取。 |
| `portal.snapshot.failed` | 某个卡片快照刷新失败 | `snapshotKey`、`cardType`、`reason` | 供前端降级展示和运维告警使用。 |

## 消费事件

| 事件类型 | 来源模块 | 处理动作 | 刷新范围 |
|----------|----------|----------|----------|
| `todo.item.created` / `todo.item.completed` / `todo.item.overdue` | `02-todo-center` | 失效待办类快照并按需重建。 | 当前用户待办卡片 |
| `msg.notification.sent` / `msg.notification.read` | `06-message-center` | 失效消息类快照并刷新未读计数与列表。 | 当前用户消息卡片 |
| `process.instance.started` / `process.instance.completed` | 流程域 | 刷新基础流程统计和常用流程入口。 | 流程统计/快捷入口卡片 |
| `content.article.published` | 内容域 | 失效公告/内容类快照。 | 受影响范围内公告卡片 |
| `org.identity.switched` | 组织权限域 | 失效当前用户在旧身份下的全部门户快照。 | 当前用户全部 `snapshotKey` |
| `portal.widget.updated` / `portal.widget.disabled` | `widget-config` | 失效关联卡片定义的快照，必要时触发降级。 | 对应 `widgetId` 关联快照 |
| `portal.personalization.saved` / `portal.personalization.reset` | `personalization` | 失效受影响用户的顺序、显隐、快捷入口相关快照。 | 当前用户当前场景快照 |
| `portal.publication.activated` / `portal.publication.offlined` | `portal-model` | 失效受影响场景的模板相关快照并等待下一次读取重建。 | 对应场景下的模板相关快照 |

## 幂等、快照刷新与投影说明

- 本模块的投影真相源是 `AggregationCardSnapshot`，幂等主键为 `snapshotKey`。
- 重复消费同一事件时，只要 `snapshotKey` 已被标记为失效或已刷新，就不应重复生成多份快照。
- `sourceWatermark` 用于记录上游数据版本或时间戳，避免在数据未变化时重复回源。
- `cacheStatus=STALE` 允许返回旧值并异步刷新；`cacheStatus=FAILED` 允许门户局部降级，但不得阻塞整页渲染。
- 一期不把事件直接投送到前端，前端通过轮询、手动刷新和重新拉取读模型感知变更。
