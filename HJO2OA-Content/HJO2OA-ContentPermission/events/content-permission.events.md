# content-permission 领域事件

## 发布事件

| 事件 | 触发时机 | 关键载荷 | 主要消费方 |
|------|----------|----------|------------|
| `content.visibility.changed` | 发布范围或可见性规则变更后 | `{publicationId, articleId, scopeVersion}` | `content-search`、`04-Portal` |
| `content.scope-template.changed` | 通用范围模板更新后 | `{templateId, action}` | 管理端缓存、治理任务 |

## 消费事件

| 事件 | 来源 | 处理动作 | 幂等/补偿要求 |
|------|------|----------|----------------|
| `content.article.published` | `content-lifecycle` | 初始化发布范围并生成默认可见规则。 | 以 `publicationId` 去重，重复事件不重复建模 |
| `content.article.unpublished` | `content-lifecycle` | 失效对应范围与缓存。 | 下线后重复消费应保持结果一致 |
| `content.category-permission.changed` | `category-management` | 重新计算继承了栏目权限的发布对象。 | 以 `categoryId + ruleVersion` 幂等刷新 |
| `org.organization.hierarchy-changed` | `01-OrgPerm / org-structure` | 刷新按组织范围命中的主体解析缓存与可见性预览结果。 | 以 `organizationId + version` 去重，失败可异步补偿 |
| `org.role.person-granted` | `01-OrgPerm / role-resource-auth` | 刷新角色主体命中预览与可见性裁剪缓存。 | 以 `eventId` 幂等去重，并按 `personId + roleId` 顺序刷新 |
| `org.role.person-revoked` | `01-OrgPerm / role-resource-auth` | 刷新角色主体命中预览与可见性裁剪缓存。 | 以 `eventId` 幂等去重，并按 `personId + roleId` 顺序刷新 |
| `org.identity.switched` | `01-OrgPerm / identity-context` | 刷新当前会话主体解析缓存与命中预览结果。 | 以 `eventId` 幂等去重，缓存刷新失败可异步补偿 |
| `org.identity-context.invalidated` | `01-OrgPerm / identity-context` | 失效当前会话主体解析缓存；若 `forceLogout=true` 则拒绝继续沿用旧上下文做预览裁剪。 | 以 `personId + invalidatedAssignmentId + reasonCode` 幂等处理，必要时回退到实时鉴权 |

## 投影、幂等与补偿约束

- 事件投影只维护可见性规则和缓存，不保存内容正文或搜索索引。
- 可见性变更需要带版本号供搜索与门户按新旧版本切换。
- 裁决失败时允许回退到实时计算，但不能放宽权限。
