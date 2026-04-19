# attendance API 说明

## 接口分组

| 分组 | 关键接口 | 关键查询/写入 | 分页与筛选 | 幂等 | 权限与审计 | 错误处理 |
|------|----------|---------------|------------|------|------------|----------|
| 申请查询 | `GET /attendance/requests` `GET /attendance/requests/{id}` | 查询申请主档、规则快照摘要、审批摘要和佐证附件。 | 支持 `page`、`size`、`sort`；筛选 `requestType`、`status`、`resultStatus`、`applicantId`、`orgId`、`startAt/endAt`。 | 查询天然幂等。 | 申请人本人、直属管理者、HR 或授权人员可见；查询写访问审计。 | `403` 越权查看，`404` 申请不存在。 |
| 申请写入与提交 | `POST /attendance/requests` `PUT /attendance/requests/{id}` `POST /attendance/requests/{id}/submit` `POST /attendance/requests/{id}/cancel` | 创建/修改申请、提交审批、取消未生效申请。 | 写接口不分页。 | 创建/提交要求 `Idempotency-Key`；同一申请重复提交按 `requestId + processTemplateId` 去重。 | 仅申请人本人或授权代办可写；保存、提交、取消都落审计。 | `400` 时间区间非法，`409` 状态冲突，`422` 缺佐证或规则快照不可用。 |
| 佐证与规则快照 | `POST /attendance/requests/{id}/evidences` `GET /attendance/requests/{id}/evidences` `GET /attendance/policy-snapshots/{code}` | 维护佐证附件，查看规则快照。 | 佐证支持按 `evidenceType` 筛选。 | 佐证写入按 `requestId + attachmentId + evidenceType` 去重。 | 仅申请人、审批相关人或 HR 可见；附件变更写审计。 | `404` 快照不存在，`409` 已提交后禁止删除关键佐证。 |
| 审批回写与结果查询 | `POST /attendance/process-callback` `GET /attendance/results` `GET /attendance/exceptions` | 接收流程结果回写，查询个人或组织维度结果/异常。 | 结果查询支持 `personId`、`dateRange`、`requestType`、`resultStatus`、`orgId`。 | 回调按 `processInstanceId + callbackState` 幂等。 | 回调仅系统内部调用；结果查询按组织和 HR 权限控制。 | `422` 流程状态与申请不匹配，`500` 回写失败需重试。 |

## 通用约束

| 主题 | 说明 |
|------|------|
| 认证与权限 | 统一复用 `01` 身份与组织权限，不在考勤模块单独维护审批人映射。 |
| 审批接入 | 所有需要审批的申请统一接入 `02`；本模块仅保存审批引用与结果。 |
| 审计要求 | 申请创建、提交、取消、佐证变更、流程回写必须全量审计。 |
| 错误口径 | 统一区分参数错误、权限错误、对象不存在、状态冲突、依赖失败。 |
| 统计口径 | 门户和报表使用已回写结果数据，不直接读取审批实例。 |
