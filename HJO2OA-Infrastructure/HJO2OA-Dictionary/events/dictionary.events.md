# 数据字典领域事件

事件信封统一遵循 `D:\idea-workspace\local\HJO2OA\docs\contracts\unified-event-contract.md`。

## 发布事件

| 事件类型 | 触发时机 | 关键载荷字段 | 说明 |
|----------|----------|--------------|------|
| `infra.dictionary.type-created` | 新建字典类型并提交成功后 | `dictionaryTypeId`、`code`、`name`、`category`、`hierarchical`、`tenantId` | 用于感知新增字典定义。 |
| `infra.dictionary.item-changed` | 字典项新增、编辑、启停、排序或层级调整后 | `dictionaryTypeId`、`itemId`、`itemCode`、`changeType`、`parentItemId`、`enabled`、`sortOrder` | 粒度面向单项变更和批量重排拆分结果。 |
| `infra.dictionary.updated` | 字典类型或字典项变更完成并需要通知下游刷新时 | `dictionaryTypeId`、`code`、`changeScope`、`affectedItemIds`、`tenantId`、`occurredAt` | 汇总事件，供下游缓存和投影统一订阅。 |

## 消费事件

| 事件类型 | 消费目的 | 关键载荷字段 | 约束 |
|----------|----------|--------------|------|
| `infra.tenant.initialized` | 为新租户装载默认字典模板或启用租户级字典集 | `tenantId`、`packageCode`、`initializedAt` | 仅执行初始化装载，不修改全局字典真相源。 |

## 幂等、补偿与投影约束

- 字典事件必须带齐统一事件信封字段，并包含 `dictionaryTypeId` 作为稳定分区键。
- 下游投影和缓存刷新必须按 `eventId` 去重；对同一 `dictionaryTypeId + itemId + changeType` 的重复投递应天然幂等。
- 字典模块发布事件采用本地事务 + Outbox，业务写入失败时不得补发“幽灵事件”。
- 若下游刷新失败，应重新回源查询字典真相源并重建投影，不允许手工篡改缓存值替代补偿。
- 前端本地缓存、门户聚合和表单选项快照都属于投影结果，不是真相源；最终以字典查询接口返回为准。
