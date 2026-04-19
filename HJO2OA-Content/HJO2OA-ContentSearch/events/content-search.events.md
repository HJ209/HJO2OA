# content-search 领域事件

## 发布事件

| 事件 | 触发时机 | 关键载荷 | 主要消费方 |
|------|----------|----------|------------|
| `content.search-index.refreshed` | 索引刷新或重建完成后 | `{scope, articleIds, refreshAt}` | 运维看板、`07-DataServices` |
| `content.subscription.changed` | 用户新增/更新/取消订阅后 | `{userId, targetType, targetId, action}` | `06-Messaging`、个人中心 |
| `content.favorite.changed` | 收藏状态变化后 | `{userId, articleId, action}` | `content-statistics` |

## 消费事件

| 事件 | 来源 | 处理动作 | 幂等/补偿要求 |
|------|------|----------|----------------|
| `content.article.published` | `content-lifecycle` | 建立或更新索引文档。 | 以 `articleId + versionId` 去重刷新 |
| `content.article.unpublished` | `content-lifecycle` | 将索引文档标记失效或删除。 | 重复消费保持删除结果一致 |
| `content.visibility.changed` | `content-permission` | 刷新可见范围相关索引投影。 | 以 `publicationId + scopeVersion` 幂等处理 |
| `content.category.changed` | `category-management` | 更新分类、栏目和专题投影信息。 | 结构刷新失败可按对象重放 |

## 投影、幂等与补偿约束

- 索引刷新事件只能描述投影结果，不可作为内容是否发布的判断依据。
- 订阅与收藏事件需要脱敏处理，避免外部消费者获取完整搜索结果。
- 大批量重建时应按栏目或时间窗口切片，避免单次全量重放。
