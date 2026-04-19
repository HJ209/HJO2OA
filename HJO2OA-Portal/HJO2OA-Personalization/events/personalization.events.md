# personalization 事件合同

## 事件定位

`personalization` 负责发布用户个性化变更事件，并消费模板发布状态变化以重新绑定基础模板。事件重点用于驱动门户视图重算和聚合快照失效，而不是复制模板或生成新模板投影。

## 发布事件

| 事件类型 | 触发时机 | 关键载荷字段 | 下游用途 |
|----------|----------|--------------|----------|
| `portal.personalization.saved` | 个性化保存成功 | `profileId`、`personId`、`sceneType` | 驱动 `portal-home` 重装配页面、驱动 `aggregation-api` 失效受影响快照。 |
| `portal.personalization.reset` | 个性化重置成功 | `profileId`、`personId`、`sceneType` | 驱动页面回退到基础模板默认视图。 |

## 消费事件

| 事件类型 | 来源模块 | 处理动作 | 一期说明 |
|----------|----------|----------|----------|
| `portal.publication.activated` | `portal-model` | 失效对应场景下的基础模板解析结果，等待下一次读取重新绑定 `basePublicationId`。 | 不主动复制模板，只更新解析基线。 |
| `portal.publication.offlined` | `portal-model` | 清理受影响个性化视图的基线引用缓存，促使回退或重解析。 | 若无可用发布模板，读取时返回空态。 |

## 幂等、快照刷新与投影说明

- 个性化事件以 `profileId` 为主键做幂等处理，重复保存通知只要求消费者以数据库当前状态重算。
- 本模块不维护聚合快照；保存和重置后只触发 `aggregation-api` 对相关 `snapshotKey` 失效。
- `portal.publication.activated`/`offlined` 只影响基础模板绑定与视图解析，不直接改写用户已保存的个性化字段。
- 个性化重置是状态回退，不删除历史审计或快捷入口变更记录。
