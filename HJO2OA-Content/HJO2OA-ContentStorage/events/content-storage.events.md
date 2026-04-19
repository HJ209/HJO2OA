# content-storage 领域事件

## 发布事件

| 事件 | 触发时机 | 关键载荷 | 主要消费方 |
|------|----------|----------|------------|
| `content.version.created` | 新版本创建或克隆成功后 | `{articleId, versionId, sourceVersionId}` | `content-lifecycle` |
| `content.attachment.bound` | 封面或附件绑定关系更新后 | `{versionId, addedAttachmentIds, removedAttachmentIds}` | 审计、资源清理任务 |
| `content.relation.changed` | 内容关联关系变更后 | `{versionId, relationTypes}` | `content-search`、专题推荐 |

## 消费事件

| 事件 | 来源 | 处理动作 | 幂等/补偿要求 |
|------|------|----------|----------------|
| `infra.attachment.deleted` | `00-Infrastructure` | 将失效附件标记为待修复引用。 | 同一附件删除事件只处理一次 |
| `content.article.archived` | `content-lifecycle` | 将归档内容相关版本统一切换为只读。 | 重复消费保持只读状态一致 |

## 投影、幂等与补偿约束

- 事件只公布版本和资源引用变化，不泄露正文大文本内容。
- 附件删除造成的失效引用需保留补偿列表，由编辑端后续修复。
- 关系变更后下游索引刷新按 `versionId` 局部执行，避免全量重建。
