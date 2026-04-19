# category-management API 合同

## 接口分组

| 分组 | 查询接口 | 写入接口 | 约束 |
|------|----------|----------|------|
| 栏目树管理 | `GET /categories/tree`、`GET /categories/{id}` | `POST /categories`、`PUT /categories/{id}`、`POST /categories/{id}/reorder` | 树查询支持懒加载；调整顺序需提交父节点与版本号 |
| 分类/标签/专题 | `GET /taxonomies`、`GET /topics/{id}` | `POST /taxonomies/tags`、`PUT /topics/{id}`、`POST /tags/merge` | 名称与编码在所属域内唯一；合并标签需记录映射审计 |
| 权限规则 | `GET /categories/{id}/permissions` | `PUT /categories/{id}/permissions`、`POST /categories/{id}/permissions/preview` | 写入必须带操作者身份；预览仅返回命中明细不落库 |

## 关键查询/写入约束

- 分页查询主要用于标签、专题和可选主体列表，树形接口默认不做全量深分页。
- 写接口要求基于版本号或更新时间做并发保护，避免多人同时改树造成覆盖。
- 所有权限规则写入都必须进入审计日志，并记录影响栏目与操作前后差异。
- 典型错误包括栏目编码冲突、循环父子关系、主体不存在、规则预览超时。

## 权限、审计与错误处理

- 查询接口默认基于当前身份上下文裁剪结果，不允许跨租户或跨组织越权读取。
- 写接口必须记录操作者、目标对象、前后版本或状态变化，并进入统一审计。
- 对于可重试动作，要求提供幂等键、业务主键组合或状态前置校验，避免重复执行。
- 所有错误码需区分权限不足、状态非法、资源不存在、并发冲突和下游依赖失败。
