# content-statistics API 合同

## 接口分组

| 分组 | 查询接口 | 写入接口 | 约束 |
|------|----------|----------|------|
| 行为上报 | - | `POST /statistics/read`、`POST /statistics/download`、`POST /statistics/favorite` | 上报以 `eventId` 或客户端幂等键去重，防止重复点击放大数据 |
| 热点排行 | `GET /statistics/rankings`、`GET /statistics/articles/{id}` | - | 支持按栏目、时间窗、指标类型筛选 |
| 看板分析 | `GET /statistics/dashboard`、`GET /statistics/conversion` | `POST /statistics/snapshots/refresh` | 刷新快照仅管理端与调度任务可调用 |

## 关键查询/写入约束

- 行为上报接口必须允许匿名会话标识与登录身份并存，但输出统计时按权限裁剪。
- 排行查询默认读取聚合快照，不直接扫描明细表。
- 刷新接口需要记录刷新范围、时间窗口和耗时，避免大范围重复计算。
- 典型错误包括事件重复、内容不存在、时间窗口非法、快照尚未生成。

## 权限、审计与错误处理

- 查询接口默认基于当前身份上下文裁剪结果，不允许跨租户或跨组织越权读取。
- 写接口必须记录操作者、目标对象、前后版本或状态变化，并进入统一审计。
- 对于可重试动作，要求提供幂等键、业务主键组合或状态前置校验，避免重复执行。
- 所有错误码需区分权限不足、状态非法、资源不存在、并发冲突和下游依赖失败。
