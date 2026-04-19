# content-lifecycle 领域事件

## 发布事件

| 事件 | 触发时机 | 关键载荷 | 主要消费方 |
|------|----------|----------|------------|
| `content.article.created` | 新稿件创建成功后 | `{articleId, categoryId, creatorId}` | 审计、编辑台账 |
| `content.article.submitted` | 内容送审或发起流程后 | `{articleId, reviewMode, reviewerSet}` | `02-WorkflowForm`、审计 |
| `content.article.published` | 某个版本正式生效后 | `{articleId, versionId, publicationId, categoryId}` | `content-permission`、`content-search`、`06-Messaging`、`04-Portal` |
| `content.article.unpublished` | 内容下线后 | `{articleId, publicationId, reason}` | `content-search`、`04-Portal` |
| `content.article.archived` | 内容归档后 | `{articleId, archivedAt}` | `content-search`、统计归档任务 |

## 消费事件

| 事件 | 来源 | 处理动作 | 幂等/补偿要求 |
|------|------|----------|----------------|
| `workflow.instance.completed` | `02-WorkflowForm` | 将流程审核结果映射为通过/驳回结论。 | 以 `processInstanceId` 去重，重复事件不得重复发布 |
| `workflow.instance.rejected` | `02-WorkflowForm` | 回写送审失败并恢复到可编辑状态。 | 失败可按实例号重放；状态已回写时直接忽略 |
| `content.version.created` | `content-storage` | 更新稿件可选版本集并校验是否允许提交审核。 | 同一版本事件只登记一次 |

## 投影、幂等与补偿约束

- 发布类事件必须携带 `articleId + versionId + publicationId`，下游按该组合幂等消费。
- 审核结果回写失败时，需要保留待补偿任务，避免流程完成但内容状态未同步。
- 下线与归档事件只改变可见性，不删除历史审核和版本轨迹。
