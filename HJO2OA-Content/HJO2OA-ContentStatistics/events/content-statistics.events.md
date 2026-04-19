# content-statistics 领域事件

## 发布事件

| 事件 | 触发时机 | 关键载荷 | 主要消费方 |
|------|----------|----------|------------|
| `content.engagement.snapshot.refreshed` | 聚合快照刷新完成后 | `{scope, window, snapshotAt}` | `07-DataServices/report`、运营看板 |
| `content.read.threshold-reached` | 热度达到阈值后 | `{articleId, metricType, value}` | `06-Messaging`、运营提醒 |

## 消费事件

| 事件 | 来源 | 处理动作 | 幂等/补偿要求 |
|------|------|----------|----------------|
| `content.favorite.changed` | `content-search` | 更新收藏统计明细和转化结果。 | 以 `{userId, articleId, action, occurredAt}` 去重 |
| `content.article.published` | `content-lifecycle` | 初始化内容统计维度与窗口。 | 同一发布批次只初始化一次 |
| `content.article.unpublished` | `content-lifecycle` | 将内容从公开热榜候选集中移除。 | 重复消费保持移除结果一致 |

## 投影、幂等与补偿约束

- 聚合事件只反映统计结果，不作为内容是否公开的判断源。
- 明细去重依赖事件幂等键，聚合层不得重复累计同一行为。
- 阈值提醒失败时可重放快照事件，但不得重复发送同一阈值告警。
