# content-storage API 合同

## 接口分组

| 分组 | 查询接口 | 写入接口 | 约束 |
|------|----------|----------|------|
| 版本管理 | `GET /articles/{id}/versions`、`GET /versions/{id}` | `POST /articles/{id}/versions`、`PUT /versions/{id}`、`POST /versions/{id}/clone` | 版本写入仅允许草稿态；复制需记录来源版本 |
| 资源绑定 | `GET /versions/{id}/attachments` | `PUT /versions/{id}/attachments`、`PUT /versions/{id}/cover` | 绑定写入时校验附件归属和引用有效性 |
| 关联关系 | `GET /versions/{id}/relations` | `PUT /versions/{id}/relations` | 关系更新需校验目标内容存在且类型合法 |

## 关键查询/写入约束

- 版本详情接口应返回摘要、正文结构、封面和附件引用，但不返回文件二进制。
- 草稿版本更新建议带版本号或 `updatedAt` 做并发控制。
- 附件绑定修改必须记录增删差异，便于审计与问题回溯。
- 典型错误包括版本已发布不可编辑、附件引用失效、关联对象不存在。

## 权限、审计与错误处理

- 查询接口默认基于当前身份上下文裁剪结果，不允许跨租户或跨组织越权读取。
- 写接口必须记录操作者、目标对象、前后版本或状态变化，并进入统一审计。
- 对于可重试动作，要求提供幂等键、业务主键组合或状态前置校验，避免重复执行。
- 所有错误码需区分权限不足、状态非法、资源不存在、并发冲突和下游依赖失败。
