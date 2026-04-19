# bulletin-fileshare API 说明

## 接口分组

| 分组 | 关键接口 | 关键查询/写入 | 分页与筛选 | 幂等 | 权限与审计 | 错误处理 |
|------|----------|---------------|------------|------|------------|----------|
| 公告场景查询 | `GET /bulletin-scenes/articles` `GET /bulletin-scenes/articles/{id}` | 查询已发布公告/制度/知识内容的场景列表与入口信息。 | 支持 `page`、`size`、`sort`；筛选 `contentType`、`scope`、`publishedAt`、`keyword`。 | 查询天然幂等。 | 可见范围由 `03 + 01` 联合控制；访问写场景审计。 | `403` 越权查看，`404` 内容入口不存在。 |
| 共享空间查询 | `GET /spaces` `GET /spaces/{id}` | 查询空间主档、成员摘要、资源数量和状态。 | 支持 `page`、`size`、`sort`；筛选 `spaceType`、`status`、`ownerOrgId`、`visibilityMode`、`managerId`。 | 查询天然幂等。 | 仅空间成员、组织管理员或授权范围可见；访问写审计。 | `403` 越权查看，`404` 空间不存在。 |
| 共享空间写入 | `POST /spaces` `PUT /spaces/{id}` `PUT /spaces/{id}/members` | 创建/修改空间主档，维护成员与权限。 | 写接口不分页。 | 创建按 `code` 或 `Idempotency-Key` 去重；成员维护按 `spaceId + subjectType + subjectId` 幂等覆盖。 | 仅空间管理员、组织管理员可写；成员与权限变更必须审计。 | `409` 编码重复或已关闭空间禁止编辑，`422` 权限主体非法。 |
| 资源绑定 | `GET /spaces/{id}/resources` `PUT /spaces/{id}/resources` | 查询并维护内容、附件、链接三类资源绑定。 | 支持按 `resourceType`、`sortOrder`、`resourceId` 筛选。 | 绑定按 `spaceId + resourceType + resourceId/resourceUrl` 去重。 | 仅空间管理员、编辑者可维护；资源绑定调整写审计。 | `404` 资源引用不存在，`409` `CLOSED` 空间禁止新增绑定。 |

## 通用约束

| 主题 | 说明 |
|------|------|
| 内容边界 | 不提供公告正文 CRUD，正文编辑、发布、检索统一复用 `03`。 |
| 附件边界 | 不提供附件二进制上传下载协议，统一复用 `00` 的附件能力。 |
| 审计要求 | 空间创建、成员变更、权限调整、资源绑定变更均必须审计。 |
| 错误口径 | 重点区分越权访问、资源引用失效、空间状态冲突、依赖失败。 |
| 对外开放 | 对外共享协议与开放能力统一由 `07` 承担，本模块只做域内接口。 |
