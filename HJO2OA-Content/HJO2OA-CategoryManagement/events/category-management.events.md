# category-management 领域事件

## 发布事件

| 事件 | 触发时机 | 关键载荷 | 主要消费方 |
|------|----------|----------|------------|
| `content.category.changed` | 栏目新增、改名、停用或排序调整后 | `{categoryId, parentId, status, version}` | `content-search`、`04-Portal` |
| `content.taxonomy.changed` | 分类、标签或专题定义变更后 | `{taxonomyType, objectId, action}` | `content-search`、运营看板 |
| `content.category-permission.changed` | 栏目权限规则发布新版本后 | `{categoryId, ruleVersion, scopeHash}` | `content-permission`、缓存刷新任务 |

## 消费事件

| 事件 | 来源 | 处理动作 | 幂等/补偿要求 |
|------|------|----------|----------------|
| `org.organization.hierarchy-changed` | `01-OrgPerm / org-structure` | 刷新栏目可见范围中引用的组织树缓存。 | 以 `organizationId + version` 去重，失败可重放刷新任务 |
| `org.role.person-granted` | `01-OrgPerm / role-resource-auth` | 重新计算角色型栏目权限预览缓存。 | 以 `eventId` 幂等去重，并按 `personId + roleId` 顺序刷新 |
| `org.role.person-revoked` | `01-OrgPerm / role-resource-auth` | 重新计算角色型栏目权限预览缓存。 | 以 `eventId` 幂等去重，并按 `personId + roleId` 顺序刷新 |

## 投影、幂等与补偿约束

- 栏目与分类事件只承载结构变化，不直接携带整棵树快照。
- 权限规则变更后允许异步刷新下游缓存，但必须保证版本号单调递增。
- 下游投影失败时应按 `categoryId + version` 维度补偿重放。
