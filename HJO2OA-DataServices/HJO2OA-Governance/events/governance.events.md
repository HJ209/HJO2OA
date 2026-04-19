# Governance 事件说明

## 发布事件

| 事件 | 触发时机 | 关键载荷 | 下游用途 | 幂等/补偿/投影约束 |
|------|----------|----------|----------|--------------------|
| `data.governance.alerted` | 告警规则命中并生成有效告警记录后 | `governanceId`、`targetCode`、`targetType`、`alertLevel`、`alertType`、`occurredAt` | 供 `06` 触达通知，供各管理端刷新风险状态，供审计链路归档 | 以 `alertId` 或 `governanceId + targetCode + occurredAtBucket` 幂等；恢复后应以新的告警状态事件补充，不覆盖原告警 |

## 消费事件

| 事件 | 来源 | 消费目的 | 关键载荷 | 幂等/补偿/投影约束 |
|------|------|----------|----------|--------------------|
| `data.api.published`、`data.api.deprecated` | `HJO2OA-OpenApi` | 维护接口版本治理和发布审计视图 | `apiId`、`code`、`version`、`status` | 按对象版本幂等，重复消费不重复建档 |
| `data.connector.updated` | `HJO2OA-Connector` | 刷新连接器健康检查目标和依赖状态 | `connectorId`、`code`、`status` | 只更新治理投影，不回写连接器定义 |
| `data.sync.completed`、`data.sync.failed` | `HJO2OA-DataSync` | 更新同步任务健康态、失败率和告警命中 | `taskId`、`code`、`checkpoint`、`executionId` | 按执行批次幂等；失败后通过告警和人工处理补偿 |
| `data.report.refreshed` | `HJO2OA-Report` | 监控报表刷新成功率和新鲜度 | `reportId`、`code`、`snapshotAt` | 只更新报表运行态投影 |
| `data.service.activated` | `HJO2OA-DataService` | 建立服务启用与版本纳管记录 | `serviceId`、`code`、`serviceType` | 以服务状态版本幂等 |

## 事件约束

| 项 | 说明 |
|----|------|
| 告警原则 | 只针对开放能力运行态与版本状态告警，不针对业务主数据本身告警 |
| 补偿原则 | 治理事件失败时优先保留告警记录并重放通知，不得静默丢弃高等级告警 |
| 投影限制 | 健康状态、告警中心、异常链路均为治理投影，不替代来源模块的定义真相源 |
