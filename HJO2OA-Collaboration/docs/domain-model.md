# 05-协同办公应用 领域模型

## 1. 文档目的

本文档细化 `05-协同办公应用` 模块的核心领域实体、值对象、聚合根、实体关系和业务规则，作为后端工程建模、数据库设计、业务应用接入和跨模块协作的统一依据。

对应架构决策编号：D03（`03` 与 `05` 模块边界）、D08（身份上下文）、D14（内容底座约束）、D16（待办与消息分工）、D20（一期实施范围）。

## 2. 领域定位与边界

### 2.1 领域职责

`05-协同办公应用` 负责在平台底座之上落地高频办公业务场景，核心职责包括：

- 承载公文、会议、日程、任务、考勤、合同、资产等业务语义和台账规则
- 将审批、消息、门户、附件、搜索等平台能力装配进具体业务应用
- 提供跨应用的统一入口、状态联动、跳转链路和移动端高频场景
- 沉淀业务侧状态机、业务台账、业务提醒和业务协同规则

### 2.2 关键边界

- **`05` 拥有业务语义，不拥有平台底座**：业务状态、业务台账、业务规则归 `05`，审批引擎归 `02`，消息触达归 `06`，附件底座归 `00`。
- **内容型场景必须复用 `03`**：公告、制度、知识展示、共享文件目录等场景只能复用 `03-内容与知识` 和 `00-attachment`，不得在 `05` 自建内容主模型（D03、D14）。
- **门户入口不归 `05` 持有**：`05` 提供业务卡片和业务入口，聚合展示和首页编排归 `04`。
- **跨系统开放不归 `05` 持有**：开放 API、连接器、同步交换和报表治理归 `07`，`05` 不自建私有开放平台。
- **`cross-app-linkage` 是编排层，不是主数据层**：统一入口、跨应用跳转和体验编排只消费业务事件和读模型，不持有独立业务真相源。

### 2.3 一期收敛口径

平台一期最小闭环不包含 `05`（D20），但若项目场景需要提前接入，建议优先顺序如下：

- 第一批：`document-mgmt`、`meeting-mgmt`、`schedule-task`、`attendance`
- 第二批：`contract-asset`
- 第三批：`bulletin-fileshare`、`cross-app-linkage`

一期简化或后置：

- `bulletin-fileshare` 先做场景装配和空间权限，不做独立内容底层模型
- `cross-app-linkage` 先支持统一入口和跳转，不做复杂流程编排和智能工作台
- 合同和资产先做基础台账与提醒，不做复杂财务/设备全生命周期

## 3. 领域总览

### 3.1 聚合根

| 聚合根 | 说明 | 目录 |
|--------|------|------|
| OfficialDocument | 收文、发文、拟稿、传阅、签发和归档的业务主模型 | `document-mgmt/` |
| MeetingPlan | 会议申请、会议安排、签到、纪要和会务状态 | `meeting-mgmt/` |
| ScheduleEntry | 个人/团队日程、提醒与时间占用安排 | `schedule-task/` |
| TaskAssignment | 任务分配、反馈、延期和完成闭环 | `schedule-task/` |
| AttendanceRequest | 请假、出差、加班、补卡、外出等考勤申请与结果 | `attendance/` |
| ContractLedger | 合同台账、履约节点、到期提醒和归档 | `contract-asset/` |
| AssetLedger | 固定资产台账、领用、借用、归还和报废流转 | `contract-asset/` |
| SharedSpace | 共享空间、成员范围和资源绑定策略 | `bulletin-fileshare/` |

### 3.2 非聚合子领域说明

以下子模块不单独拥有新的业务真相源，而是基于核心聚合构建交互或编排能力：

| 子模块 | 角色 | 说明 |
|--------|------|------|
| `bulletin-fileshare/` | 场景装配层 | 公告场景消费 `03` 的已发布内容；共享文件场景绑定 `SharedSpace` 与附件引用 |
| `cross-app-linkage/` | 编排层 | 统一业务入口、状态联动、跨应用跳转和移动端高频体验，不单独持有业务主数据 |

### 3.3 核心实体关系

```text
OfficialDocument   ──1:N──> DocumentCirculationRecord   (传阅记录)
OfficialDocument   ──1:N──> DocumentSealRecord          (签章记录)
OfficialDocument   ──1:1──> ProcessInstanceRef          (审批流程引用)
MeetingPlan        ──1:N──> MeetingParticipant          (参会人)
MeetingPlan        ──1:N──> MeetingSignRecord           (签到记录)
MeetingPlan        ──1:1──> MeetingMinutes              (会议纪要)
ScheduleEntry      ──1:N──> ReminderRule               (提醒规则)
TaskAssignment     ──M:1──> ScheduleEntry              (任务可来源于日程)
TaskAssignment     ──1:N──> TaskFeedback               (反馈记录)
AttendanceRequest  ──M:1──> AttendancePolicySnapshot   (规则快照)
AttendanceRequest  ──1:N──> AttendanceEvidence         (佐证材料)
ContractLedger     ──1:N──> ContractMilestone          (履约节点)
ContractLedger     ──1:N──> ContractReminder           (提醒策略)
AssetLedger        ──1:N──> AssetTransferRecord        (流转记录)
SharedSpace        ──1:N──> SpaceMember                (成员)
SharedSpace        ──1:N──> SpaceResourceBinding       (绑定内容/附件/链接)
```

### 3.4 核心业务流

```text
创建业务单据/会议/任务/考勤/台账
  -> 绑定审批流程或直接生效
  -> 生成业务状态、责任人和提醒规则
  -> 通过 06 发送通知，通过 04 输出业务卡片
  -> 通过 cross-app-linkage 提供统一入口和跳转
  -> 完成归档、统计和后续跟进
```

## 4. 核心聚合定义

### 4.1 OfficialDocument（公文）

公文聚合负责发文、收文和内部行文的业务语义、传阅和归档规则。

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | UUID | PK | 公文唯一标识 |
| documentNo | String(64) | UK, NULLABLE | 文号，签发后生成 |
| documentType | Enum | NOT NULL | RECEIVED / OUTGOING / INTERNAL |
| title | String(256) | NOT NULL | 标题 |
| sponsorOrgId | UUID | FK -> Organization.id, NOT NULL | 主办组织 |
| drafterId | UUID | FK -> Person.id, NOT NULL | 拟稿人 |
| confidentialityLevel | Enum | NOT NULL | PUBLIC / INTERNAL / SECRET / CONFIDENTIAL |
| urgencyLevel | Enum | NOT NULL | NORMAL / URGENT / CRITICAL |
| processInstanceId | UUID | NULLABLE | 关联审批流程 |
| bodyAttachmentId | UUID | FK -> AttachmentAsset.id, NULLABLE | 正文附件引用 |
| status | Enum | NOT NULL | DRAFT / IN_APPROVAL / EFFECTIVE / ARCHIVED / VOIDED |
| issuedAt | Timestamp | NULLABLE | 签发时间 |
| archivedAt | Timestamp | NULLABLE | 归档时间 |
| tenantId | UUID | FK -> Tenant.id, NOT NULL | 所属租户 |
| createdAt | Timestamp | NOT NULL | |
| updatedAt | Timestamp | NOT NULL | |

#### 关联实体：DocumentCirculationRecord（传阅记录）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | UUID | PK | |
| documentId | UUID | FK -> OfficialDocument.id, NOT NULL | 所属公文 |
| receiverId | UUID | FK -> Person.id, NOT NULL | 接收人 |
| receiverAssignmentId | UUID | FK -> Assignment.id, NULLABLE | 接收时身份 |
| readStatus | Enum | NOT NULL | UNREAD / READ / RETURNED |
| receivedAt | Timestamp | NOT NULL | 接收时间 |
| readAt | Timestamp | NULLABLE | 阅读时间 |

#### 关联实体：DocumentSealRecord（签章记录）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | UUID | PK | |
| documentId | UUID | FK -> OfficialDocument.id, NOT NULL | 所属公文 |
| sealType | Enum | NOT NULL | OFFICIAL / DEPARTMENT / PERSONAL |
| operatorId | UUID | FK -> Person.id, NOT NULL | 操作人 |
| sealedAt | Timestamp | NOT NULL | 签章时间 |
| evidenceRef | String(256) | NULLABLE | 签章证据引用 |

**业务规则**：

- 文号只能在公文进入有效态时生成，且租户内唯一。
- 已归档公文不可再编辑，只允许查看和导出。
- 审批流程是公文生效的可选前置条件，流程状态不等于公文状态。
- 传阅记录采用追加式记录，不覆盖已完成阅读轨迹。

**领域事件**：

| 事件类型 | 触发时机 | 载荷 |
|----------|----------|------|
| `biz.document.submitted` | 公文提交审批 | documentId, documentType, initiatorId |
| `biz.document.approved` | 公文审批通过/生效 | documentId, documentType |
| `biz.document.archived` | 公文归档 | documentId, documentType |

### 4.2 MeetingPlan（会议）

会议聚合负责会议申请、排期、签到、纪要和状态跟踪。

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | UUID | PK | 会议唯一标识 |
| title | String(256) | NOT NULL | 会议主题 |
| organizerId | UUID | FK -> Person.id, NOT NULL | 组织者 |
| roomId | UUID | NULLABLE | 会议室资源引用 |
| startAt | Timestamp | NOT NULL | 开始时间 |
| endAt | Timestamp | NOT NULL | 结束时间 |
| meetingType | Enum | NOT NULL | ONLINE / OFFLINE / HYBRID |
| processInstanceId | UUID | NULLABLE | 关联审批流程 |
| noticeContentRef | String(256) | NULLABLE | 通知内容引用 |
| status | Enum | NOT NULL | DRAFT / PENDING_APPROVAL / SCHEDULED / ONGOING / COMPLETED / CANCELLED |
| tenantId | UUID | FK -> Tenant.id, NOT NULL | 所属租户 |
| createdAt | Timestamp | NOT NULL | |
| updatedAt | Timestamp | NOT NULL | |

#### 关联实体：MeetingParticipant（参会人）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | UUID | PK | |
| meetingId | UUID | FK -> MeetingPlan.id, NOT NULL | 所属会议 |
| participantId | UUID | FK -> Person.id, NOT NULL | 参会人 |
| roleType | Enum | NOT NULL | HOST / ATTENDEE / RECORDER / GUEST |
| required | Boolean | NOT NULL, DEFAULT TRUE | 是否必到 |

#### 关联实体：MeetingSignRecord（签到记录）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | UUID | PK | |
| meetingId | UUID | FK -> MeetingPlan.id, NOT NULL | 所属会议 |
| participantId | UUID | FK -> Person.id, NOT NULL | 参会人 |
| signType | Enum | NOT NULL | SIGN_IN / SIGN_OUT |
| occurredAt | Timestamp | NOT NULL | 发生时间 |

#### 关联实体：MeetingMinutes（会议纪要）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | UUID | PK | |
| meetingId | UUID | FK -> MeetingPlan.id, NOT NULL | 所属会议 |
| contentRef | String(256) | NOT NULL | 纪要正文引用 |
| attachmentId | UUID | FK -> AttachmentAsset.id, NULLABLE | 纪要附件 |
| publishedAt | Timestamp | NULLABLE | 发布或确认时间 |

**业务规则**：

- 同一会议室同一时间段不得出现两个 `SCHEDULED/ONGOING` 会议。
- `COMPLETED` 会议才能形成正式纪要。
- 签到记录只记录事实，不反向修改会议状态。

**领域事件**：

| 事件类型 | 触发时机 | 载荷 |
|----------|----------|------|
| `biz.meeting.created` | 会议创建并排期成功 | meetingId, organizerId, startTime, endTime |
| `biz.meeting.cancelled` | 会议取消 | meetingId, reason |
| `biz.meeting.minutes-published` | 纪要发布 | meetingId, minutesId |

### 4.3 ScheduleEntry（日程）

日程聚合负责个人和团队时间安排、提醒和可见性控制。

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | UUID | PK | 日程唯一标识 |
| ownerType | Enum | NOT NULL | PERSON / TEAM |
| ownerId | UUID | NOT NULL | 所属人员或团队 |
| title | String(256) | NOT NULL | 日程标题 |
| startAt | Timestamp | NOT NULL | 开始时间 |
| endAt | Timestamp | NOT NULL | 结束时间 |
| allDay | Boolean | NOT NULL, DEFAULT FALSE | 是否全天 |
| sourceType | Enum | NOT NULL | SELF / MEETING / TASK / PROCESS |
| sourceBizId | UUID | NULLABLE | 来源业务对象 ID |
| visibility | Enum | NOT NULL | PRIVATE / TEAM / SCOPE_CONTROLLED |
| status | Enum | NOT NULL | ACTIVE / COMPLETED / CANCELLED |
| tenantId | UUID | FK -> Tenant.id, NOT NULL | 所属租户 |
| createdAt | Timestamp | NOT NULL | |
| updatedAt | Timestamp | NOT NULL | |

#### 关联实体：ReminderRule（提醒规则）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | UUID | PK | |
| scheduleId | UUID | FK -> ScheduleEntry.id, NOT NULL | 所属日程 |
| remindBeforeMinutes | Integer | NOT NULL | 提前提醒分钟数 |
| channelPreference | Enum | NOT NULL | NONE / IN_APP / MESSAGE_CENTER |

**业务规则**：

- 日程冲突可以提示但不强制禁止，来源于会议的系统日程以会议时间为准。
- `sourceType` 只用于关联来源，不改变日程自身可见规则。
- 日程取消不删除提醒规则与操作日志。

**领域事件**：

| 事件类型 | 触发时机 | 载荷 |
|----------|----------|------|
| `biz.schedule.created` | 日程创建 | scheduleId, ownerType, ownerId |
| `biz.schedule.updated` | 日程更新 | scheduleId, changedFields |

### 4.4 TaskAssignment（任务）

任务聚合负责任务分配、反馈、延期和完成闭环，是业务执行类事项的台账。

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | UUID | PK | 任务唯一标识 |
| title | String(256) | NOT NULL | 任务标题 |
| creatorId | UUID | FK -> Person.id, NOT NULL | 创建人 |
| assigneeId | UUID | FK -> Person.id, NOT NULL | 负责人 |
| originType | Enum | NOT NULL | SELF / MEETING / DOCUMENT / CONTRACT / ATTENDANCE |
| sourceBizId | UUID | NULLABLE | 来源业务对象 |
| priority | Enum | NOT NULL | LOW / NORMAL / HIGH / CRITICAL |
| dueAt | Timestamp | NULLABLE | 截止时间 |
| status | Enum | NOT NULL | NEW / IN_PROGRESS / BLOCKED / DONE / CANCELLED |
| tenantId | UUID | FK -> Tenant.id, NOT NULL | 所属租户 |
| createdAt | Timestamp | NOT NULL | |
| updatedAt | Timestamp | NOT NULL | |

#### 关联实体：TaskFeedback（任务反馈）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | UUID | PK | |
| taskId | UUID | FK -> TaskAssignment.id, NOT NULL | 所属任务 |
| operatorId | UUID | FK -> Person.id, NOT NULL | 操作人 |
| action | Enum | NOT NULL | START / COMMENT / DELAY / COMPLETE / CANCEL |
| content | String(1024) | NULLABLE | 反馈内容 |
| createdAt | Timestamp | NOT NULL | |

**业务规则**：

- 任务是业务执行台账，不替代 `02` 的待办事务处理视图。
- 任务延期必须留下反馈轨迹和新的截止时间。
- 已完成任务允许补充评论，但不允许重新进入未完成状态而不留痕。

**领域事件**：

| 事件类型 | 触发时机 | 载荷 |
|----------|----------|------|
| `biz.task.assigned` | 任务分配 | taskId, assigneeId, dueAt |
| `biz.task.completed` | 任务完成 | taskId, assigneeId |
| `biz.task.overdue` | 任务逾期 | taskId, assigneeId, dueAt |

### 4.5 AttendanceRequest（考勤申请）

考勤申请聚合负责请假、出差、加班、补卡、外出等流程型考勤事项及结果沉淀。

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | UUID | PK | 申请唯一标识 |
| requestType | Enum | NOT NULL | LEAVE / TRAVEL / OVERTIME / REISSUE / OUTBOUND |
| applicantId | UUID | FK -> Person.id, NOT NULL | 申请人 |
| startAt | Timestamp | NOT NULL | 开始时间 |
| endAt | Timestamp | NOT NULL | 结束时间 |
| durationValue | Decimal(10,2) | NOT NULL | 时长/天数 |
| processInstanceId | UUID | NULLABLE | 关联审批流程 |
| policySnapshotCode | String(64) | NULLABLE | 规则快照编码 |
| resultStatus | Enum | NOT NULL | PENDING / NORMAL / EXCEPTION / COMPENSATED |
| status | Enum | NOT NULL | DRAFT / SUBMITTED / APPROVED / REJECTED / CANCELLED / RESULTED |
| tenantId | UUID | FK -> Tenant.id, NOT NULL | 所属租户 |
| createdAt | Timestamp | NOT NULL | |
| updatedAt | Timestamp | NOT NULL | |

#### 关联实体：AttendanceEvidence（考勤佐证）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | UUID | PK | |
| requestId | UUID | FK -> AttendanceRequest.id, NOT NULL | 所属申请 |
| attachmentId | UUID | FK -> AttachmentAsset.id, NOT NULL | 佐证附件 |
| evidenceType | Enum | NOT NULL | PROOF / TICKET / LOCATION / OTHER |

**业务规则**：

- 申请提交后冻结规则快照，避免规则变更影响历史申请。
- 审批通过后才能写入最终考勤结果。
- 结果异常可以被补偿，但必须保留原始申请和审批轨迹。

**领域事件**：

| 事件类型 | 触发时机 | 载荷 |
|----------|----------|------|
| `biz.attendance.requested` | 提交考勤申请 | requestId, applicantId, requestType |
| `biz.attendance.result-changed` | 考勤结果变化 | personId, date, status |

### 4.6 ContractLedger（合同台账）

合同台账聚合负责合同生命周期、履约节点和到期提醒。

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | UUID | PK | 合同唯一标识 |
| contractNo | String(64) | UK, NOT NULL | 合同编号 |
| contractType | String(64) | NOT NULL | 合同类型 |
| ownerOrgId | UUID | FK -> Organization.id, NOT NULL | 归属组织 |
| counterpartyName | String(256) | NOT NULL | 相对方 |
| amount | Decimal(18,2) | NULLABLE | 合同金额 |
| currency | String(16) | NULLABLE | 币种 |
| effectiveFrom | Date | NULLABLE | 生效日期 |
| effectiveTo | Date | NULLABLE | 到期日期 |
| processInstanceId | UUID | NULLABLE | 审批流程引用 |
| status | Enum | NOT NULL | DRAFT / ACTIVE / EXPIRING / EXPIRED / TERMINATED / ARCHIVED |
| tenantId | UUID | FK -> Tenant.id, NOT NULL | 所属租户 |
| createdAt | Timestamp | NOT NULL | |
| updatedAt | Timestamp | NOT NULL | |

#### 关联实体：ContractMilestone（履约节点）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | UUID | PK | |
| contractId | UUID | FK -> ContractLedger.id, NOT NULL | 所属合同 |
| milestoneName | String(128) | NOT NULL | 节点名称 |
| dueDate | Date | NOT NULL | 到期日期 |
| status | Enum | NOT NULL | PENDING / DONE / DELAYED |

#### 关联实体：ContractReminder（提醒规则）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | UUID | PK | |
| contractId | UUID | FK -> ContractLedger.id, NOT NULL | 所属合同 |
| remindBeforeDays | Integer | NOT NULL | 提前提醒天数 |
| channelPreference | Enum | NOT NULL | NONE / IN_APP / MESSAGE_CENTER |

**业务规则**：

- 合同编号一经生效不可修改。
- 到期提醒由调度任务驱动，不依赖人工轮询。
- 合同归档后保留履约节点和附件引用，不再参与主动提醒。

### 4.7 AssetLedger（资产台账）

资产台账聚合负责资产主档、流转、盘点和报废状态。

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | UUID | PK | 资产唯一标识 |
| assetCode | String(64) | UK, NOT NULL | 资产编码 |
| assetName | String(256) | NOT NULL | 资产名称 |
| categoryCode | String(64) | NOT NULL | 资产分类 |
| ownerOrgId | UUID | FK -> Organization.id, NOT NULL | 所属组织 |
| custodianId | UUID | FK -> Person.id, NULLABLE | 当前保管人 |
| location | String(256) | NULLABLE | 存放地点 |
| originalValue | Decimal(18,2) | NULLABLE | 原值 |
| acquiredAt | Date | NULLABLE | 购入日期 |
| status | Enum | NOT NULL | IN_STOCK / IN_USE / BORROWED / MAINTAINING / SCRAPPED |
| tenantId | UUID | FK -> Tenant.id, NOT NULL | 所属租户 |
| createdAt | Timestamp | NOT NULL | |
| updatedAt | Timestamp | NOT NULL | |

#### 关联实体：AssetTransferRecord（资产流转记录）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | UUID | PK | |
| assetId | UUID | FK -> AssetLedger.id, NOT NULL | 所属资产 |
| actionType | Enum | NOT NULL | ASSIGN / BORROW / RETURN / INVENTORY / SCRAP |
| operatorId | UUID | FK -> Person.id, NOT NULL | 操作人 |
| targetPersonId | UUID | FK -> Person.id, NULLABLE | 目标责任人 |
| occurredAt | Timestamp | NOT NULL | 发生时间 |

**业务规则**：

- 资产状态变化必须记录流转记录，不允许无轨迹直接改状态。
- 报废后的资产不能再进入借用或领用状态。
- 盘点属于资产事实记录，不直接变更业务审批流程状态。

### 4.8 SharedSpace（共享空间）

共享空间聚合负责共享文件和共享内容入口的成员范围、空间策略和资源绑定。

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | UUID | PK | 空间唯一标识 |
| code | String(64) | UK, NOT NULL | 空间编码 |
| name | String(128) | NOT NULL | 空间名称 |
| spaceType | Enum | NOT NULL | TEAM / DEPARTMENT / PROJECT / PUBLIC |
| ownerOrgId | UUID | FK -> Organization.id, NOT NULL | 归属组织 |
| managerId | UUID | FK -> Person.id, NULLABLE | 管理员 |
| visibilityMode | Enum | NOT NULL | PUBLIC / SCOPE_CONTROLLED / PRIVATE |
| quotaMb | Integer | NULLABLE | 配额上限 |
| status | Enum | NOT NULL | ACTIVE / READ_ONLY / CLOSED |
| tenantId | UUID | FK -> Tenant.id, NOT NULL | 所属租户 |
| createdAt | Timestamp | NOT NULL | |
| updatedAt | Timestamp | NOT NULL | |

#### 关联实体：SpaceMember（空间成员）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | UUID | PK | |
| spaceId | UUID | FK -> SharedSpace.id, NOT NULL | 所属空间 |
| subjectType | Enum | NOT NULL | ORGANIZATION / DEPARTMENT / POSITION / ROLE / PERSON |
| subjectId | UUID | NOT NULL | 主体 ID |
| permissionLevel | Enum | NOT NULL | VIEWER / EDITOR / MANAGER |

#### 关联实体：SpaceResourceBinding（空间资源绑定）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | UUID | PK | |
| spaceId | UUID | FK -> SharedSpace.id, NOT NULL | 所属空间 |
| resourceType | Enum | NOT NULL | CONTENT / ATTACHMENT / LINK |
| resourceId | UUID | NULLABLE | 内容或附件引用 ID |
| resourceUrl | String(512) | NULLABLE | 外部链接 |
| sortOrder | Integer | NOT NULL, DEFAULT 0 | 排序 |

**业务规则**：

- 共享空间只管理成员范围和资源绑定，不复制存储 `03` 内容正文或 `00` 附件二进制。
- 公告场景通过绑定 `03` 的已发布内容实现，不形成新的公告主模型。
- `CLOSED` 空间不允许新增资源绑定，但保留历史浏览和审计信息。

## 5. 关键规则与解析策略

### 5.1 流程接入原则

- 业务对象可选择接入 `02` 审批流程，但审批引擎和待办视图不归 `05` 所有。
- 流程实例完成后，业务聚合必须显式回写自身状态，不允许直接把“流程完成”视为“业务完成”。
- 业务审批附件、意见和审计轨迹分别复用 `00` 和 `02` 的能力。

### 5.2 内容复用原则

- 公告、制度、知识、共享资料等内容型对象统一走 `03` 主模型。
- `bulletin-fileshare` 只做场景入口、空间范围和资源绑定，不自建正文、发布、检索主模型。
- 门户公告卡片、知识入口和共享空间展示均应优先消费 `03` 的已发布读模型。

### 5.3 通知与门户联动原则

- 业务提醒事件由 `05` 发布，送达渠道和发送状态由 `06` 管理。
- 门户、办公中心和移动工作台只消费 `05` 提供的业务读模型或事件结果，不回写业务状态。
- 任务、会议、合同到期等提醒应统一收敛为业务事件，而不是前端各自轮询业务表。

### 5.4 跨应用编排原则

- `cross-app-linkage` 只负责统一入口、跳转和状态联动，不复制业务主数据。
- 跨应用链路采用事件驱动优先，必要的只读查询走内部接口。
- 同一业务对象在不同应用中展示时，唯一真相源仍归其所属聚合。

### 5.5 台账与归档原则

- 合同、资产、公文等台账对象一旦进入归档态，应冻结关键标识和主档字段。
- 流转、传阅、反馈、签到等历史轨迹均采用追加式记录，保障审计可追溯。
- 所有高敏感操作应保留操作者、身份上下文和时间戳。

## 6. 事件模型

### 6.1 `05` 内部领域事件

| 事件类型 | 载荷关键字段 | 说明 |
|----------|-------------|------|
| `biz.document.submitted` | documentId, documentType, initiatorId | 公文提交审批 |
| `biz.document.approved` | documentId, documentType | 公文审批通过/生效 |
| `biz.document.archived` | documentId, documentType | 公文归档 |
| `biz.meeting.created` | meetingId, organizerId, startTime, endTime | 会议创建 |
| `biz.meeting.cancelled` | meetingId, reason | 会议取消 |
| `biz.schedule.created` | scheduleId, ownerType, ownerId | 日程创建 |
| `biz.task.assigned` | taskId, assigneeId, dueAt | 任务分配 |
| `biz.task.completed` | taskId, assigneeId | 任务完成 |
| `biz.attendance.requested` | requestId, applicantId, requestType | 提交考勤申请 |
| `biz.attendance.result-changed` | personId, date, status | 考勤结果变化 |
| `biz.contract.expiring` | contractId, effectiveTo | 合同临期 |
| `biz.asset.status-changed` | assetId, status | 资产状态变化 |
| `biz.space.updated` | spaceId, changedFields | 共享空间更新 |

### 6.2 跨模块总线事件

当前优先对外暴露并已在统一事件契约中占位的 `05` 事件如下：

| 总线事件类型 | 触发时机 | payload 关键字段 |
|--------------|----------|------------------|
| `biz.document.submitted` | 公文提交审批 | documentId, documentType, initiatorId |
| `biz.document.approved` | 公文审批通过/生效 | documentId, documentType |
| `biz.document.archived` | 公文归档 | documentId, documentType |
| `biz.meeting.created` | 会议创建 | meetingId, organizerId, startTime, endTime |
| `biz.meeting.cancelled` | 会议取消 | meetingId, reason |
| `biz.attendance.result-changed` | 考勤结果变化 | personId, date, status |

任务、合同、资产和共享空间类事件建议先在模块内闭环，待统一事件契约扩展后再逐步注册为跨模块总线事件。

### 6.3 入站业务事件

`05` 重点消费以下事件以驱动审批回写、内容复用和人员调整：

| 事件类型 | 处理方式 | 说明 |
|----------|----------|------|
| `process.instance.completed` | 回写业务对象审批通过状态 | 公文/会议/考勤/合同等审批主链路 |
| `process.instance.terminated` | 回写业务对象审批驳回或作废状态 | 审批异常链路 |
| `content.article.published` | 刷新公告场景和共享空间内容入口 | `bulletin-fileshare` 场景复用 |
| `org.person.resigned` | 触发任务移交、会议组织者替换、资产责任人调整 | 人员变动联动 |
| `infra.attachment.deleted` | 校验业务对象附件引用 | 附件一致性维护 |

## 7. 跨模块依赖

| 模块关系 | 依赖内容 | 说明 |
|----------|----------|------|
| `05 -> 00` | 附件、字典、审计、配置、调度、错误码 | 业务应用共用平台底座 |
| `05 -> 01` | 身份上下文、组织权限、人员岗位、数据权限 | 业务可见范围和操作者语义 |
| `05 -> 02` | 流程发起、审批处理、表单元数据、待办中心 | 业务审批流接入 |
| `05 -> 03` | 公告、知识、共享文件目录内容底座 | 内容型场景复用 |
| `04 -> 05` | 会议、日程、任务、合同提醒等业务卡片数据 | 门户消费业务读模型 |
| `06 -> 05` | 业务提醒、通知状态、移动端高频触达 | 消息和移动入口联动 |
| `05 -> 07` | 对外开放、第三方同步、业务报表 | 通过开放层暴露和同步业务数据 |

## 8. 数据库设计建议

### 8.1 核心表清单

| 表名 | 对应聚合根/实体 | 说明 |
|------|-----------------|------|
| `biz_document` | OfficialDocument | 公文主表 |
| `biz_document_circulation` | DocumentCirculationRecord | 传阅记录表 |
| `biz_document_seal_record` | DocumentSealRecord | 签章记录表 |
| `biz_meeting` | MeetingPlan | 会议主表 |
| `biz_meeting_participant` | MeetingParticipant | 参会人表 |
| `biz_meeting_sign_record` | MeetingSignRecord | 签到表 |
| `biz_meeting_minutes` | MeetingMinutes | 纪要表 |
| `biz_schedule_entry` | ScheduleEntry | 日程主表 |
| `biz_schedule_reminder_rule` | ReminderRule | 日程提醒规则表 |
| `biz_task_assignment` | TaskAssignment | 任务主表 |
| `biz_task_feedback` | TaskFeedback | 任务反馈表 |
| `biz_attendance_request` | AttendanceRequest | 考勤申请表 |
| `biz_attendance_evidence` | AttendanceEvidence | 考勤佐证表 |
| `biz_contract` | ContractLedger | 合同主表 |
| `biz_contract_milestone` | ContractMilestone | 履约节点表 |
| `biz_contract_reminder` | ContractReminder | 合同提醒表 |
| `biz_asset` | AssetLedger | 资产主表 |
| `biz_asset_transfer_record` | AssetTransferRecord | 资产流转表 |
| `biz_shared_space` | SharedSpace | 共享空间表 |
| `biz_space_member` | SpaceMember | 空间成员表 |
| `biz_space_resource_binding` | SpaceResourceBinding | 空间资源绑定表 |

### 8.2 索引建议

- `biz_document`：`(tenantId, documentNo)`、`(tenantId, documentType, status)`、`(processInstanceId)`
- `biz_document_circulation`：`(documentId, receiverId)`、`(receiverId, readStatus, receivedAt)`
- `biz_meeting`：`(tenantId, roomId, startAt, endAt)`、`(organizerId, status, startAt)`
- `biz_meeting_participant`：`(meetingId, participantId)`、`(participantId, roleType)`
- `biz_schedule_entry`：`(ownerType, ownerId, startAt, endAt)`、`(sourceType, sourceBizId)`
- `biz_task_assignment`：`(assigneeId, status, dueAt)`、`(originType, sourceBizId)`
- `biz_attendance_request`：`(applicantId, requestType, status)`、`(processInstanceId)`、`(resultStatus, startAt)`
- `biz_contract`：`(tenantId, contractNo)`、`(ownerOrgId, status, effectiveTo)`
- `biz_asset`：`(tenantId, assetCode)`、`(custodianId, status)`、`(ownerOrgId, status)`
- `biz_shared_space`：`(tenantId, code)`、`(ownerOrgId, status)`、`(visibilityMode)`
- `biz_space_resource_binding`：`(spaceId, resourceType, resourceId)`

### 8.3 大字段与引用建议

- 正文、纪要、合同扫描件等大字段建议采用 `attachmentId` 或内容引用方式，不在业务主表直接保存二进制。
- 审批表单数据建议由 `02` 表单实例或业务扩展表保存，业务主表仅保存 `processInstanceId` 与关键摘要字段。
- 共享空间资源绑定建议采用轻量引用表，而非重复复制内容或附件主数据。

## 9. 一期建模优先级建议

### 9.1 平台口径

- `05` 不属于平台一期最小闭环必做模块（D20）。
- 当前优先级用于业务项目提前接入时的功能排序。

### 9.2 若提前接入，第一批建议落地的聚合

- `OfficialDocument`
- `MeetingPlan`
- `ScheduleEntry`
- `TaskAssignment`
- `AttendanceRequest`

### 9.3 若提前接入，第二批建议落地的聚合

- `ContractLedger`
- `AssetLedger`

### 9.4 若提前接入，可简化实现的部分

- `SharedSpace` 先支持成员范围和资源绑定，不做复杂网盘目录树和版本协作
- `cross-app-linkage` 先支持统一入口、跳转链路和状态标识，不做复杂业务编排引擎
- 合同和资产先支持基础台账、提醒和流转，不做复杂财务核算和设备维保体系

## 10. 结论

`05-协同办公应用` 的核心不是“堆一组业务页面”，而是建立一套**业务语义清晰、平台能力可复用、跨应用状态可联动、内容与流程边界不冲突**的办公应用组合层。其建模重点应围绕以下原则展开：

- 用 `OfficialDocument`、`MeetingPlan`、`ScheduleEntry`、`TaskAssignment` 承载高频办公主场景
- 用 `AttendanceRequest`、`ContractLedger`、`AssetLedger` 沉淀业务台账和提醒规则
- 用 `SharedSpace` 管共享范围和资源绑定，而不是复制内容和附件主数据
- 用 `cross-app-linkage` 组织统一入口和联动体验，而不是新增一层业务真相源

该文档可直接作为后续 `HJO2OA-Collaboration/docs/module-design.md`、业务应用接入顺序和各业务子模块建表设计的基础。
