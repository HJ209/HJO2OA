# content-search API 合同

## 接口分组

| 分组 | 查询接口 | 写入接口 | 约束 |
|------|----------|----------|------|
| 搜索查询 | `GET /search`、`POST /search/advanced`、`GET /search/facets` | - | 支持分页、关键字、栏目、标签、时间范围过滤 |
| 收藏与订阅 | `GET /me/favorites`、`GET /me/subscriptions` | `POST /me/favorites`、`DELETE /me/favorites/{id}`、`PUT /me/subscriptions/{id}` | 收藏与订阅写入以 `userId + targetType + targetId` 保证幂等 |
| 索引运维 | `GET /search/index/status` | `POST /search/index/rebuild`、`POST /search/index/refresh` | 重建仅管理端可触发，刷新支持按 `articleId` 定向执行 |

## 关键查询/写入约束

- 搜索分页默认返回总数估算与结果列表，避免高成本深翻页。
- 订阅接口只允许操作当前用户自己的偏好，不支持跨人代配。
- 索引运维接口必须记录触发来源、范围和耗时，并限制批量重建频率。
- 典型错误包括索引不可用、筛选项非法、订阅目标不存在、结果已不可见。

## 权限、审计与错误处理

- 查询接口默认基于当前身份上下文裁剪结果，不允许跨租户或跨组织越权读取。
- 写接口必须记录操作者、目标对象、前后版本或状态变化，并进入统一审计。
- 对于可重试动作，要求提供幂等键、业务主键组合或状态前置校验，避免重复执行。
- 所有错误码需区分权限不足、状态非法、资源不存在、并发冲突和下游依赖失败。
