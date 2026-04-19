# Timezone 事件说明

`timezone` 负责发布时区策略和用户偏好变化事件。事件只表达时间解释基线变化，不回写业务主数据的 UTC 存储值。

## 发布事件

| 事件 | 触发时机 | 关键载荷 | 下游用途 | 幂等/补偿/投影约束 |
|------|----------|----------|----------|--------------------|
| `infra.timezone.setting-changed` | 系统或租户作用域的默认时区、生效窗口或启停状态变更后 | `scopeType`、`scopeId`、`timezoneId`、`effectiveFrom`、`changedAt`、`operatorId` | 通知调度、门户、协同和导出场景刷新时区解释基线 | 以 `scopeType + scopeId + effectiveFrom` 幂等；重复消费只刷新同一生效窗口投影；不允许借此回写业务历史时间 |
| `infra.timezone.user-changed` | 用户自助切换时区或管理员代设个人时区后 | `personId`、`tenantId`、`oldTimezoneId`、`newTimezoneId`、`changedAt` | 通知前端壳层、消息提醒和个人工作台刷新当前展示时区 | 以 `personId + changedAt` 幂等；重复消费不重复生成偏好记录；只影响展示和解释，不改变业务超时判定历史 |

## 消费事件

| 事件 | 来源 | 消费目的 | 关键载荷 | 幂等/补偿/投影约束 |
|------|------|----------|----------|--------------------|
| `infra.config.updated` | `HJO2OA-Config` | 刷新可用时区白名单、默认时区策略和 DST 解释参数 | `configKey`、`scope`、`configVersion`、`changedAt` | 按 `configKey + configVersion` 幂等；只更新解释策略投影，不改动已有 `TimezoneSetting` 历史记录 |

## 事件约束

| 项 | 说明 |
|----|------|
| 发布范围 | 一期只发布策略变化和用户偏好变化，不发布每次时间转换或页面展示事件 |
| 补偿原则 | 策略误配通过新策略或撤销未来生效记录补偿，不直接篡改审计历史 |
| 投影限制 | 页面本地时间、导出时间文本和调度解释缓存都是运行投影，不替代 `TimezoneSetting` 真相源 |
