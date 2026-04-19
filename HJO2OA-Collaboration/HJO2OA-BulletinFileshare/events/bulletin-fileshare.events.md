# bulletin-fileshare 事件说明

## 发布事件

| 事件 | 触发时机 | 关键载荷 | 下游使用 | 幂等/补偿/投影约束 |
|------|----------|----------|----------|--------------------|
| `biz.space.updated` | 共享空间主档、成员权限或资源绑定变更后发布。 | `spaceId`、`changedFields`、`status`、`managerId`、`updatedAt`。 | `04` 刷新共享空间入口，`06` 按需发送空间变更提醒。 | 按 `spaceId + updatedAt` 幂等；补偿以最新空间快照覆盖投影。 |

## 消费事件

| 事件 | 来源 | 消费用途 | 关键载荷 | 幂等/补偿/投影约束 |
|------|------|----------|----------|--------------------|
| `content.article.published` | `03` | 刷新公告场景聚合列表与资源入口。 | `contentId`、`contentType`、`scope`、`publishedAt`。 | 按 `contentId + publishedAt` 幂等；文章下线时由场景重建投影。 |
| `infra.attachment.deleted` | `00` | 识别并清理失效附件绑定，阻断无效入口继续展示。 | `attachmentId`、`deletedAt`、`operatorId`。 | 重复事件只标记一次失效；补偿通过重新绑定有效附件。 |
| `org.person.resigned` | `01` | 清理或替换空间管理人、成员主体中的离职人员引用。 | `personId`、`orgId`、`effectiveDate`。 | 仅影响 `ACTIVE/READ_ONLY` 空间；重复事件按 `personId + effectiveDate` 去重。 |
