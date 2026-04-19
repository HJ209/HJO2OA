# schedule-task API 说明

## 接口分组

| 分组 | 关键接口 | 关键查询/写入 | 分页与筛选 | 幂等 | 权限与审计 | 错误处理 |
|------|----------|---------------|------------|------|------------|----------|
| 日程查询 | `GET /schedules` `GET /schedules/{id}` | 查询个人/团队日程、来源业务和提醒规则。 | 支持 `page`、`size`、`sort`；筛选 `ownerType`、`ownerId`、`status`、`sourceType`、`startAt/endAt`、`visibility`。 | 查询天然幂等。 | 仅日程所有者、团队成员或授权范围可见；查看行为写访问审计。 | `403` 越权查看，`404` 日程不存在。 |
| 日程写入与提醒 | `POST /schedules` `PUT /schedules/{id}` `POST /schedules/{id}/cancel` `PUT /schedules/{id}/reminders` | 创建/修改自建日程、取消日程、维护提醒规则。 | 写接口不分页。 | 创建要求 `Idempotency-Key`；来源型日程按 `sourceType + sourceBizId` 去重；提醒更新按 `scheduleId` 幂等覆盖。 | 仅日程所有者、团队负责人或管理员可写；创建、取消、提醒修改均落审计。 | `400` 时间非法，`409` 来源型日程禁止手工改写，`422` 提醒规则不合法。 |
| 任务查询 | `GET /tasks` `GET /tasks/{id}` `GET /tasks/{id}/feedbacks` | 查询任务主档、来源业务、负责人和反馈轨迹。 | 支持 `page`、`size`、`sort`；筛选 `assigneeId`、`creatorId`、`status`、`priority`、`originType`、`dueAt`。 | 查询天然幂等。 | 任务创建人、负责人、管理者可见；查看反馈写审计。 | `403` 越权查看，`404` 任务不存在。 |
| 任务写入与反馈 | `POST /tasks` `PUT /tasks/{id}` `POST /tasks/{id}/assign` `POST /tasks/{id}/feedbacks` `POST /tasks/{id}/complete` | 创建任务、重新分配负责人、追加反馈、完成任务。 | 写接口不分页。 | 创建按 `Idempotency-Key` 去重；分配按 `taskId + assigneeId + requestId` 去重；反馈按 `taskId + action + requestId` 去重。 | 仅创建人、负责人、管理者可操作；所有分配、延期、完成动作必须留审计。 | `409` 状态冲突，`422` 缺少延期说明或截止时间。 |
| 事件驱动同步 | `POST /schedules/system-sync` | 接收会议等来源事件，生成或撤销系统日程。 | 不分页。 | 按 `sourceType + sourceBizId + action` 幂等。 | 仅系统内部调用，写系统审计。 | `500` 同步失败需重试，`422` 来源对象不存在。 |

## 通用约束

| 主题 | 说明 |
|------|------|
| 认证与权限 | 统一复用 `01` 身份、组织、团队与数据权限。 |
| 提醒机制 | 接口只管理提醒规则，不直接操作 `06` 送达渠道。 |
| 审计要求 | 日程创建/取消、任务分配/延期/完成、系统同步均必须审计。 |
| 状态约束 | 任务状态与日程状态由本模块主档控制，不能仅依赖事件投影。 |
| 开放边界 | 对外开放或跨系统同步统一经 `07` 暴露，本模块仅提供域内接口。 |
