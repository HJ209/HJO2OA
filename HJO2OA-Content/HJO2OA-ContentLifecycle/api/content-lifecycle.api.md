# content-lifecycle API 合同

## 接口分组

| 分组 | 查询接口 | 写入接口 | 约束 |
|------|----------|----------|------|
| 稿件列表与详情 | `GET /articles`、`GET /articles/{id}`、`GET /articles/{id}/history` | `POST /articles`、`PUT /articles/{id}` | 列表支持栏目、状态、时间范围筛选与分页 |
| 审核与送审 | `GET /articles/{id}/reviews` | `POST /articles/{id}/submit`、`POST /articles/{id}/approve`、`POST /articles/{id}/reject` | 审核动作需带当前状态和操作者身份，避免越权流转 |
| 发布状态管理 | `GET /articles/{id}/publications` | `POST /articles/{id}/publish`、`POST /articles/{id}/unpublish`、`POST /articles/{id}/archive` | 发布和下线操作按 `articleId + versionId + action` 保证幂等 |

## 关键查询/写入约束

- 列表查询需要显式区分草稿态、审核态、已发布、已下线和已归档状态。
- 写接口必须校验栏目权限、操作者身份和版本可发布性，并记录审核意见。
- 所有状态迁移接口都需要进入审计日志，并保留前后状态、版本号和审核模式。
- 典型错误包括状态迁移非法、版本不存在、栏目已停用、工作流结果未回写。

## 权限、审计与错误处理

- 查询接口默认基于当前身份上下文裁剪结果，不允许跨租户或跨组织越权读取。
- 写接口必须记录操作者、目标对象、前后版本或状态变化，并进入统一审计。
- 对于可重试动作，要求提供幂等键、业务主键组合或状态前置校验，避免重复执行。
- 所有错误码需区分权限不足、状态非法、资源不存在、并发冲突和下游依赖失败。
