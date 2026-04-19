# 缓存管理领域事件

事件信封统一遵循 `D:\idea-workspace\local\HJO2OA\docs\contracts\unified-event-contract.md`。

## 发布事件

| 事件类型 | 触发时机 | 关键载荷字段 | 说明 |
|----------|----------|--------------|------|
| `infra.cache.invalidated` | 手工失效、事件驱动失效或计划失效执行成功后 | `cachePolicyId`、`namespace`、`invalidateKey`、`reasonType`、`reasonRef`、`invalidatedAt` | 作为缓存治理轨迹通知，不表示业务数据被修改。 |

## 消费事件

| 事件类型 | 消费目的 | 关键载荷字段 | 约束 |
|----------|----------|--------------|------|
| `infra.dictionary.updated` | 刷新字典查询缓存和选择器投影 | `dictionaryTypeId`、`code`、`affectedItemIds` | 字典真相源仍在字典模块。 |
| `infra.config.updated` | 刷新配置解析缓存和运行时快照 | `configEntryId`、`configKey`、`changeType` | 不得把缓存结果当成配置真相源。 |
| `infra.feature-flag.changed` | 刷新 Feature Flag 命中缓存 | `configEntryId`、`ruleId`、`changeType` | 规则变更后优先重建命中结果。 |
| `portal.template.published` | 刷新门户模板聚合缓存 | `templateId`、`scopeType`、`scopeId` | 仅失效缓存，不改模板真相源。 |
| `content.article.published` | 刷新内容展示和检索相关缓存 | `articleId`、`categoryId`、`publicationId` | 内容真相源仍在内容域。 |

## 幂等、补偿与投影约束

- 缓存失效记录是运行轨迹对象，不是业务真相源；缓存值丢失或重建不影响业务事实。
- 同一 `eventId + namespace + invalidateKey` 的重复失效必须幂等，允许重复删除同一 Key。
- 若失效执行失败，应记录失败原因并重试；补偿方式是再次失效或回源重建，而不是手工写入伪造缓存值。
- 命中率、热点统计和门户/内容聚合缓存都是投影结果，可重新生成。
- 所有手工失效和批量刷新动作都必须附带审计上下文并进入统一审计。
