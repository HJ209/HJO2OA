# form-metadata 领域事件

事件信封统一遵循 `docs/contracts/unified-event-contract.md`。事件前缀固定为 `form.metadata.*`。

## 发布事件

| 事件类型 | 触发时机 | 关键载荷字段 | 说明 |
|----------|----------|--------------|------|
| `form.metadata.created` | 新建一个表单元数据版本草稿后 | `metadataId`、`code`、`name`、`version`、`status` | 每个新版本都发布一次创建事件。 |
| `form.metadata.published` | 表单草稿校验通过并切换为 `PUBLISHED` 后 | `metadataId`、`code`、`version`、`publishedAt`、`fieldCount` | 供流程定义和渲染缓存刷新。 |
| `form.metadata.deprecated` | 已发布版本被显式废弃后 | `metadataId`、`code`、`version`、`deprecatedAt` | 不影响已运行实例中的快照。 |

## 消费事件

一期无强制消费事件。

- 字典、附件和组织选择器能力通过同步只读接口接入，不通过事件订阅更新表单定义。
- 运行时实例快照由 `process-instance` 自行冻结，本模块不消费实例事件回写版本状态。

## 投影、幂等与补偿

- 本子模块无独立读模型投影，`form_metadata` 即权威设计时数据。
- 发布事件采用 Outbox 发出，失败时通过重试补发，不允许跳过审计直接补库。
- 下游缓存刷新按 `eventId` 去重；同一 `metadataId + version + eventType` 的重复投递必须可幂等处理。
- 发布后即使后续废弃，也不得回写已运行实例中的表单快照。
