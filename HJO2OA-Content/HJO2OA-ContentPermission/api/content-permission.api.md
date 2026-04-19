# content-permission API 合同

## 接口分组

| 分组 | 查询接口 | 写入接口 | 约束 |
|------|----------|----------|------|
| 发布范围管理 | `GET /publications/{id}/scopes`、`GET /publications/{id}/scope-templates` | `PUT /publications/{id}/scopes`、`POST /publications/{id}/scopes/clone` | 写入前必须校验内容已发布且发布批次有效 |
| 可见性裁决 | `POST /visibility/evaluate`、`POST /visibility/batch-evaluate` | `POST /visibility/preview` | 批量裁决用于搜索结果裁剪，预览仅限管理端使用 |
| 缓存与诊断 | `GET /visibility/cache/{key}` | `POST /visibility/cache/refresh` | 刷新接口仅管理端或调度任务可调用 |

## 关键查询/写入约束

- 批量裁决接口必须支持按主体和内容列表批量传参，避免搜索结果逐条判权。
- 写接口需记录规则版本号和操作者信息，作为审计和缓存失效依据。
- 典型错误包括发布批次不存在、主体范围非法、规则版本冲突、缓存刷新失败。
- 权限相关接口不返回明文敏感组织结构，只返回命中摘要。

## 权限、审计与错误处理

- 查询接口默认基于当前身份上下文裁剪结果，不允许跨租户或跨组织越权读取。
- 写接口必须记录操作者、目标对象、前后版本或状态变化，并进入统一审计。
- 对于可重试动作，要求提供幂等键、业务主键组合或状态前置校验，避免重复执行。
- 所有错误码需区分权限不足、状态非法、资源不存在、并发冲突和下游依赖失败。
