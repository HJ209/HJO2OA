# Attachment 事件说明

`attachment` 负责发布附件元数据、版本和配额治理事件。事件只描述附件基础设施状态变化，不表达业务对象生命周期。

## 发布事件

| 事件 | 触发时机 | 关键载荷 | 下游用途 | 幂等/补偿/投影约束 |
|------|----------|----------|----------|--------------------|
| `infra.attachment.created` | 文件上传完成、元数据建档成功并至少形成一条业务绑定后 | `attachmentId`、`fileName`、`fileSize`、`contentType`、`businessType`、`businessId`、`createdAt` | 通知业务域、审计和读模型刷新附件摘要 | 以 `attachmentId` 或 `storageKey` 幂等；重复投递不重复建档；事件表示元数据已建档，不代表业务对象生命周期推进 |
| `infra.attachment.deleted` | 附件完成逻辑删除并解除业务绑定后 | `attachmentId`、`businessType`、`businessId`、`deletedAt`、`operatorId` | 通知业务域更新附件清单，通知治理模块跟踪后续物理清理 | 以 `attachmentId + deletedAt` 幂等；物理清理异步进行，不要求与删除事件同事务完成 |
| `infra.attachment.version-created` | 附件追加新版本并更新当前有效版本后 | `attachmentId`、`versionNo`、`checksum`、`createdAt` | 通知预览、审计和业务详情页刷新版本摘要 | 以 `attachmentId + versionNo` 幂等；旧版本不可覆盖，回滚通过切换引用或新版本表达 |
| `infra.attachment.quota-warning` | 租户或资源主体达到配置阈值并需要治理关注时 | `tenantId`、`ownerType`、`ownerId`、`usedBytes`、`totalBytes`、`threshold`、`warnedAt` | 供运维、消息触达和租户治理台消费 | 以 `tenantId + ownerType + ownerId + threshold + warnedAt` 幂等；预警只表达治理信号，不直接删除附件 |

## 消费事件

| 事件 | 来源 | 消费目的 | 关键载荷 | 幂等/补偿/投影约束 |
|------|------|----------|----------|--------------------|
| `infra.config.updated` | `HJO2OA-Config` | 刷新对象存储、预览转换、限速和配额阈值等运行策略 | `configKey`、`scope`、`configVersion`、`changedAt` | 按 `configKey + configVersion` 幂等；只更新运行配置投影，不篡改附件元数据和版本历史 |
| 已接入业务对象删除或归档事件（如 `content.article.archived`、`biz.document.archived`） | `03/05` 等业务域 | 将绑定关系标记为失效候选、生成孤儿文件治理清单 | `businessType`、`businessId`、`occurredAt`、`tenantId` | 按 `eventId` 或 `businessType + businessId + occurredAt` 幂等；只更新 `AttachmentBinding` 投影或治理候选，不自动拥有业务生命周期，也不直接物理删文件 |

## 事件约束

| 项 | 说明 |
|----|------|
| 命名约束 | 附件治理事件统一使用 `infra.attachment.*` 前缀和 kebab-case 动作命名 |
| 补偿原则 | 删除、版本回退、配额处理都通过新的治理动作表达，不允许跳过审计直接改表 |
| 投影限制 | 附件列表、预览面板、配额看板和孤儿文件清单都是运行投影，不替代 `AttachmentAsset` 真相源 |
