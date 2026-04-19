# 审计日志领域事件

事件信封统一遵循 `D:\idea-workspace\local\HJO2OA\docs\contracts\unified-event-contract.md`。

## 发布事件

| 事件类型 | 触发时机 | 关键载荷字段 | 说明 |
|----------|----------|--------------|------|
| `infra.audit.archived` | 审计归档任务完成后 | `archiveJobId`、`archiveScope`、`recordCount`、`archivedAt`、`tenantId` | 用于通知归档仓、治理台和合规查询侧更新状态。 |

## 消费事件

| 事件类型 | 消费目的 | 关键载荷字段 | 约束 |
|----------|----------|--------------|------|
| `infra.config.updated` | 记录关键配置治理动作 | `configKey`、`changeType`、`scopeType`、`scopeId` | 只记录配置事实，不控制配置状态。 |
| `infra.cache.invalidated` | 记录手工或事件驱动的缓存失效治理动作 | `namespace`、`invalidateKey`、`reasonType`、`reasonRef` | 缓存治理必须可追溯。 |
| `org.account.login-succeeded` | 留存关键安全审计足迹 | `accountId`、`loginIp`、`tenantId` | 作为平台级安全审计输入。 |
| `process.task.completed` | 追踪跨模块关键业务动作 | `taskId`、`instanceId`、`actionCode` | 审计只记录事实，不参与流程流转。 |

## 幂等、补偿与投影约束

- 审计消费按 `eventId` 去重，同一事件重复投递不得产生重复审计记录。
- 审计记录 append-only；任何补偿动作都应新增一条新的审计记录描述补偿结果，而不是修改旧记录。
- 归档属于运行治理动作，只改变 `archiveStatus` 与存储位置，不改变历史语义。
- 审计查询页、导出文件和合规报表属于投影结果，可重建；审计表本身才是留痕真相源。
- 本模块不根据审计事件反向驱动业务回滚，补偿入口仍在原业务模块或原基础设施模块。
