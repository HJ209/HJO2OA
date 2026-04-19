# process-monitor API

根据父模块 `module-design.md`，`process-monitor` 一期暂缓，当前不开放生产接口。以下内容为后续实现预留契约，默认状态为“未启用”。

## 接口分组

| 分组 | 预留接口 | 当前状态 | 说明 |
|------|----------|----------|------|
| 耗时分析 | `GET /api/v1/process/monitor/duration` | 未启用 | 按流程、版本、节点聚合耗时。 |
| 停滞分析 | `GET /api/v1/process/monitor/stalls` | 未启用 | 查询停滞节点、停滞实例和停滞时长。 |
| 拥塞分析 | `GET /api/v1/process/monitor/congestion` | 未启用 | 查询审批负载和待办堆积。 |
| 超时观察 | `GET /api/v1/process/monitor/overdue` | 未启用 | 查询超时任务及提醒线索。 |

## 关键查询/写入接口

| 方法 | 路径 | 当前状态 | 关键约束 |
|------|------|----------|----------|
| `GET` | `/api/v1/process/monitor/duration` | 未启用 | 仅管理端只读查询。 |
| `GET` | `/api/v1/process/monitor/stalls` | 未启用 | 必须按时间范围、定义或分类过滤。 |
| `GET` | `/api/v1/process/monitor/congestion` | 未启用 | 仅返回聚合统计，不返回完整业务表单正文。 |
| `GET` | `/api/v1/process/monitor/overdue` | 未启用 | 仅作为超时观察入口，不直接发起催办。 |

## 分页与筛选

- 若后续启用，列表型接口统一使用 `page`、`size`、`sort`。
- 建议支持筛选字段：
  - `filter[definitionId]`
  - `filter[definitionCode]`
  - `filter[category]`
  - `filter[assigneeId]`
  - `filter[startTime]gte` / `filter[startTime]lte`
  - `filter[dueTime]gte` / `filter[dueTime]lte`
- 统计图表接口可不分页，但必须限制最大时间窗口，避免全表扫描。

## 幂等

- 当前无开放写接口。
- 若后续只开放查询接口，则不要求 `X-Idempotency-Key`。
- 若后续增加导出或离线统计任务，应单独遵循异步操作与幂等规范。

## 审批动作、草稿、归档边界

- 监控模块不提供审批动作接口，不得直接催办、转办、终止实例。
- 监控模块不拥有草稿与归档数据，只允许按只读方式查询指标。
- 任何需要改变流程状态的操作都必须回到 `process-instance` 或 `action-engine`。

## 权限与审计约束

- 仅管理端具备流程监控权限的角色可访问。
- 若后续开放接口，所有查询都必须按租户隔离，并记录查询审计。
- 不允许通过监控接口绕过业务权限直接查看业务单据正文。

## 错误场景

| 场景 | HTTP | 错误码建议 | 说明 |
|------|------|------------|------|
| 一期未启用即被调用 | `501` 或 `404` | `PROCESS_MONITOR_DISABLED` | 由网关或服务统一拦截。 |
| 时间范围过大 | `422` | `PROCESS_MONITOR_RANGE_INVALID` | 避免大范围扫描。 |
| 当前身份无监控权限 | `403` | `PROCESS_MONITOR_FORBIDDEN` | 非管理端角色不可访问。 |
