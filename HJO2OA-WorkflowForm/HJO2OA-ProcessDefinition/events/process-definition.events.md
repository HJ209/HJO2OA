# process-definition 领域事件

事件信封统一遵循 `docs/contracts/unified-event-contract.md`。本子模块只发布设计时定义事件，不发布运行时任务事件。

## 发布事件

| 事件类型 | 触发时机 | 关键载荷字段 | 说明 |
|----------|----------|--------------|------|
| `process.definition.created` | 新建一个流程定义版本草稿后 | `definitionId`、`code`、`name`、`version`、`category`、`status` | 每个新版本都发布一次创建事件。 |
| `process.definition.published` | 草稿校验通过并切换为 `PUBLISHED` 后 | `definitionId`、`code`、`version`、`category`、`formMetadataId`、`publishedAt` | 新实例只能使用该事件对应的发布版本。 |
| `process.definition.deprecated` | 发布版被显式废弃后 | `definitionId`、`code`、`version`、`deprecatedAt` | 仅影响后续选择，不影响已运行实例。 |

## 消费事件

一期无强制消费事件。

- 绑定表单版本通过同步查询 `form-metadata` 的已发布数据完成，不依赖异步订阅。
- 参与者规则预览通过同步调用身份上下文与角色接口完成，不在本子模块维护异步投影。

## 投影、幂等与补偿

- 本子模块无独立读模型投影，定义表本身即为设计时权威数据源。
- 事件发布使用本地事务 + Outbox；业务写入失败时不得发布事件。
- 下游消费端按 `eventId` 去重；同一 `definitionId + version + eventType` 的重复投递必须可幂等处理。
- 若发布事件出站失败，允许基于 Outbox 重试，不允许通过手工改库补发未审计事件。
