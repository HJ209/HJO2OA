# event-subscription 事件契约

## 1. 事件角色

`event-subscription` 以消费事件为主，不作为稳定领域事件发布方。

- 本子模块不发布独立 `event-subscription.*` 事件。
- 输出统一为内部消息创建命令，不对总线追加新命名空间。
- 重点职责是把统一事件契约映射为消息创建请求。

## 2. 发布事件

| 类型 | 说明 |
|------|------|
| 无 | 一期不发布独立领域事件 |

## 3. 消费事件

### 3.1 一期必须支持

| 事件类型 | 触发动作 | 关键载荷字段 | 说明 |
|----------|----------|--------------|------|
| `todo.item.created` | 生成待办提醒 | `eventId`、`tenantId`、`taskId`、`assigneePersonId`、`assignmentId`、`positionId`、`title`、`deepLink` | 一期主链路 |
| `todo.item.overdue` | 生成催办或升级提醒 | `eventId`、`taskId`、`assigneePersonId`、`overdueAt`、`priorityHint` | 一期主链路 |
| `process.task.overdue` | 生成流程超时提醒 | `eventId`、`processTaskId`、`assigneePersonId`、`assignmentId`、`positionId`、`deepLink` | 一期主链路 |
| `org.account.locked` | 生成安全告警消息 | `eventId`、`accountId`、`personId`、`reason`、`occurredAt` | 强制消息，不允许静默 |

### 3.2 一期可选接入

| 事件类型 | 触发动作 | 说明 |
|----------|----------|------|
| `process.instance.completed` | 生成流程结果通知 | 可按项目需要开启 |
| `content.article.published` | 生成公告发布提醒 | 属于后续阶段能力 |

## 4. 关键载荷字段要求

- `eventId`：事件幂等主键，必填。
- `tenantId`：多租户隔离字段，必填。
- `occurredAt`：事件发生时间，必填。
- `sourceModule`：来源模块，必填。
- 待办类事件必须尽量提供 `assignmentId`、`positionId`。
- 规则只能依赖统一事件契约中已注册字段，不得读取发布方私有扩展字段作为硬依赖。

## 5. 至少一次消费、重试与幂等

- 消费语义为至少一次。
- 幂等键采用 `eventId + ruleCode + recipientPersonId`。
- 在去重窗口内命中同一 `dedupKey` 时，结果记为 `DEDUP_SKIPPED`，不得重复创建消息。
- 若消息创建命令已被 `message-center` 接受，再次重试时必须识别并直接返回成功。

## 6. 死信与补偿说明

- 规则解析、身份解析、模板引用校验失败时进入失败重试。
- 超过重试上限后写入死信/审计表，记录 `eventId`、`ruleCode`、失败原因、最近重试时间。
- 死信补偿只允许重放已注册事件，不允许注入新的私有载荷结构。
