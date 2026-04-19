# meeting-mgmt API 说明

## 接口分组

| 分组 | 关键接口 | 关键查询/写入 | 分页与筛选 | 幂等 | 权限与审计 | 错误处理 |
|------|----------|---------------|------------|------|------------|----------|
| 会议查询 | `GET /meetings` `GET /meetings/{id}` | 查询会议主档、参会人、会议室、审批摘要和签到统计。 | 支持 `page`、`size`、`sort`；筛选 `status`、`meetingType`、`organizerId`、`roomId`、`startAt/endAt`、`participantId`。 | 查询天然幂等。 | 仅会议组织者、参会人、会议室管理员或授权查看范围可见；访问写审计。 | `403` 越权查看，`404` 会议不存在。 |
| 会议写入与排期 | `POST /meetings` `PUT /meetings/{id}` `POST /meetings/{id}/submit` `POST /meetings/{id}/cancel` | 创建会议申请、修改草稿、提交审批、取消已排期会议。 | 写接口不分页；草稿回显按主键查询。 | 创建/提交要求 `Idempotency-Key`；取消按 `meetingId + cancelledAt` 去重。 | 仅组织者、会议秘书或管理员可写；取消动作强制记录原因和操作者。 | `400` 时间区间非法，`409` 会议室冲突或状态冲突，`422` 参会人为空。 |
| 会议室与冲突校验 | `GET /meeting-rooms` `GET /meeting-rooms/availability` | 查询会议室、空闲时段和冲突信息。 | 支持按 `location`、`capacity`、`equipmentTag`、`startAt/endAt` 查询。 | 查询天然幂等。 | 会议室基础信息对授权用户可见；冲突校验写系统审计。 | `404` 会议室不存在，`409` 请求时段被占用。 |
| 签到签退 | `POST /meetings/{id}/sign-records` `GET /meetings/{id}/sign-records` | 记录签到/签退事实并查询统计。 | 支持按 `participantId`、`signType`、`occurredAt` 过滤。 | 按 `meetingId + participantId + signType + occurredAt` 去重。 | 参会人可签到，组织者与会议秘书可查看全量记录；所有签到写审计。 | `403` 非参会人禁止签到，`409` 非 `ONGOING` 状态禁止签到。 |
| 纪要与审批回写 | `POST /meetings/{id}/minutes` `GET /meetings/{id}/minutes` `POST /meetings/process-callback` | 发布会议纪要、查询纪要详情、接收流程完成/终止回调。 | 纪要查询支持按 `meetingId`、`publishedAt` 检索。 | 纪要发布按 `meetingId` 幂等；回调按 `processInstanceId + callbackState` 幂等。 | 仅组织者、记录人或管理员可发布纪要；系统回调写系统审计。 | `409` 未完成会议不得发纪要，`422` 流程状态不匹配，`500` 回写失败需重试。 |

## 通用约束

| 主题 | 说明 |
|------|------|
| 认证与权限 | 统一复用 `01` 的组织、岗位、人员和会议室管理员权限。 |
| 审批接入 | 会议审批统一复用 `02`，本模块仅维护 `processInstanceId` 和业务状态。 |
| 审计要求 | 会议创建、提交、取消、签到、纪要发布、流程回写都必须可审计。 |
| 错误口径 | 重点区分参数错误、资源冲突、权限错误、对象不存在、依赖失败。 |
| 移动端接口 | 复用同一套查询接口，移动端不暴露独立业务主数据写模型。 |
