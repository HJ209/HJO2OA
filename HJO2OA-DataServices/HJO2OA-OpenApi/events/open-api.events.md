# OpenApi 事件说明

## 发布事件

| 事件 | 触发时机 | 关键载荷 | 下游用途 | 幂等/补偿/投影约束 |
|------|----------|----------|----------|--------------------|
| `data.api.published` | 接口版本发布并进入可调用状态后 | `apiId`、`code`、`path`、`httpMethod`、`version`、`authType`、`publishedAt` | 通知治理模块纳管新版本，通知相关系统刷新对接清单 | 以 `apiId + version` 幂等；重复投递不重复建档；只表示开放边界发布，不表示业务数据变化 |
| `data.api.deprecated` | 接口版本被废弃或计划下线后 | `apiId`、`code`、`version`、`deprecatedAt`、`sunsetAt` | 通知治理模块、调用方和审计系统更新生命周期状态 | 以 `apiId + version + deprecatedAt` 幂等；恢复或延后下线需新事件补充，不覆盖历史 |

## 消费事件

| 事件 | 来源 | 消费目的 | 关键载荷 | 幂等/补偿/投影约束 |
|------|------|----------|----------|--------------------|
| `data.service.activated` | `HJO2OA-DataService` | 将可复用服务定义纳入开放接口可选目录 | `serviceId`、`code`、`serviceType`、`permissionMode` | 按服务状态版本幂等；只更新引用候选清单 |
| `infra.config.updated` | `00` 配置域 | 刷新签名、网关、回源策略或限流配置引用 | `configKey`、`scope`、`changedAt` | 以配置版本幂等；刷新失败进入治理告警 |
| `data.governance.alerted` | `HJO2OA-Governance` | 根据治理结果标记接口风险状态或触发人工下线审批 | `targetCode`、`targetType`、`alertLevel` | 告警只更新风险投影，不直接删除或篡改接口定义 |

## 事件约束

| 项 | 说明 |
|----|------|
| 发布范围 | 一期只发布接口生命周期事件，不发布每次调用明细事件作为领域事件 |
| 幂等原则 | 调用方和治理侧必须按接口版本幂等消费，防止重复发布/重复废弃 |
| 投影限制 | 调用日志、配额消耗、错误分布是运行投影，不替代 `OpenApiEndpoint` 真相源 |
