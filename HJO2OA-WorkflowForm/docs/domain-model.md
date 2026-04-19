# 02-流程与表单 领域模型

## 1. 文档目的

本文档细化 `02-流程与表单` 模块的核心领域实体、值对象、聚合根、实体关系和业务规则，作为后端工程建模、数据库设计和接口契约的统一依据。

对应架构决策编号：D08（身份上下文）、D10（02 领域模型优先细化）、D16（待办与消息分工）。

## 2. 领域总览

### 2.1 聚合根

| 聚合根 | 说明 | 目录 |
|--------|------|------|
| ProcessDefinition | 流程定义，版本化模型 | `process-definition/` |
| ProcessInstance | 流程实例，运行中的流程 | `process-instance/` |
| TaskInstance | 任务实例，流程中的待办节点 | `process-instance/` |
| ActionDefinition | 审批动作定义 | `action-engine/` |
| FormMetadata | 表单元数据，版本化模型 | `form-metadata/` |
| TodoItem | 待办项，统一待办中心视图 | `todo-center/` |

### 2.2 核心实体关系

```
ProcessDefinition ──1:N──> ProcessDefinition     (版本链)
ProcessDefinition ──1:N──> ProcessInstance       (定义 -> 实例)
ProcessDefinition ──1:N──> ActionDefinition      (定义 -> 动作配置)
ProcessDefinition ──1:1──> FormMetadata          (定义 -> 表单绑定)
ProcessInstance   ──1:N──> TaskInstance          (实例 -> 任务)
ProcessInstance   ──1:1──> FormMetadata           (实例 -> 表单快照)
TaskInstance      ──1:N──> TaskAction            (任务 -> 动作记录)
TaskInstance      ──1:1──> TodoItem              (任务 -> 待办项)
```

## 3. 核心聚合定义

### 3.1 ProcessDefinition（流程定义）

流程定义是流程的版本化模型，包含节点、路由和表单绑定。

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | UUID | PK | 流程定义唯一标识 |
| code | String(64) | UK, NOT NULL | 流程编码，租户内唯一 |
| name | String(128) | NOT NULL | 流程名称 |
| category | String(64) | NULLABLE | 流程分类（如请假、报销、发文） |
| version | Integer | NOT NULL, DEFAULT 1 | 版本号 |
| status | Enum | NOT NULL | 状态：DRAFT / PUBLISHED / DEPRECATED |
| formMetadataId | UUID | FK -> FormMetadata.id, NULLABLE | 绑定表单元数据 |
| startNodeId | String(64) | NULLABLE | 起始节点 ID |
| endNodeId | String(64) | NULLABLE | 结束节点 ID |
| nodes | JSON | NOT NULL | 节点定义（见 NodeDefinition 值对象） |
| routes | JSON | NOT NULL | 路由定义（见 RouteDefinition 值对象） |
| tenantId | UUID | FK -> Tenant.id, NOT NULL | 所属租户 |
| publishedAt | Timestamp | NULLABLE | 发布时间 |
| publishedBy | UUID | FK -> Person.id, NULLABLE | 发布人 |
| createdAt | Timestamp | NOT NULL | |
| updatedAt | Timestamp | NOT NULL | |

**UK 约束**：(code, version)

#### NodeDefinition（节点定义，值对象）

| 字段 | 类型 | 说明 |
|------|------|------|
| nodeId | String(64) | 节点 ID，定义内唯一 |
| name | String(128) | 节点名称 |
| type | Enum | 节点类型：START / END / USER_TASK / SERVICE_TASK / EXCLUSIVE_GATEWAY / PARALLEL_GATEWAY / INCLUSIVE_GATEWAY / SUB_PROCESS |
| participantRule | JSON | 参与者规则（见 ParticipantRule） |
| formOverride | JSON | NULLABLE，节点级表单字段权限覆盖 |
| actionCodes | List\<String\> | 可用动作编码列表 |
| timeoutRule | JSON | NULLABLE，超时规则 |
| multiInstance | JSON | NULLABLE，多实例配置（会签/或签） |
| position | JSON | 设计器坐标信息 |

#### ParticipantRule（参与者规则，值对象）

| 字段 | 类型 | 说明 |
|------|------|------|
| type | Enum | 参与者类型：INITIATOR / ORG_MANAGER / DEPT_MANAGER / POSITION_HOLDER / ROLE_HOLDER / SPECIFIC_PERSON / FORM_FIELD_VALUE / EXPRESSION |
| refId | UUID | NULLABLE，引用的组织/部门/岗位/人员/角色 ID |
| refFieldCode | String(128) | NULLABLE，表单字段编码（type=FORM_FIELD_VALUE 时） |
| expression | String(512) | NULLABLE，表达式（type=EXPRESSION 时） |
| fallback | JSON | NULLABLE，无匹配时的兜底规则 |

#### RouteDefinition（路由定义，值对象）

| 字段 | 类型 | 说明 |
|------|------|------|
| routeId | String(64) | 路由 ID |
| name | String(128) | 路由名称 |
| sourceNodeId | String(64) | 源节点 ID |
| targetNodeId | String(64) | 目标节点 ID |
| condition | JSON | NULLABLE，路由条件表达式 |
| isDefault | Boolean | 是否为默认路由 |
| sortOrder | Integer | 同源路由排序号 |

**业务规则**：

- 流程定义采用版本链管理，发布后不可修改，修改需创建新版本。
- 流程定义发布后才能发起实例，已发布版本可被新版本替代（DEPRECATED）。
- 节点类型中 USER_TASK 必须配置参与者规则。
- 路由条件支持简单表达式和表单字段值判断。
- 起始节点有且仅有一个，结束节点至少一个。

**领域事件**：

| 事件类型 | 触发时机 | 载荷 |
|----------|----------|------|
| `process.definition.created` | 定义创建 | definitionId, code, version |
| `process.definition.published` | 定义发布 | definitionId, code, version |
| `process.definition.deprecated` | 定义废弃 | definitionId, code, version |

### 3.2 ProcessInstance（流程实例）

流程实例是流程定义的一次运行，承载流程状态和审批轨迹。

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | UUID | PK | 流程实例唯一标识 |
| definitionId | UUID | FK -> ProcessDefinition.id, NOT NULL | 流程定义 |
| definitionVersion | Integer | NOT NULL | 使用的定义版本号 |
| definitionCode | String(64) | NOT NULL | 流程编码快照 |
| title | String(256) | NOT NULL | 流程标题 |
| category | String(64) | NULLABLE | 流程分类快照 |
| initiatorId | UUID | FK -> Person.id, NOT NULL | 发起人 |
| initiatorOrgId | UUID | FK -> Organization.id, NOT NULL | 发起人组织（发起时身份上下文快照） |
| initiatorDeptId | UUID | FK -> Department.id, NULLABLE | 发起人部门 |
| initiatorPositionId | UUID | FK -> Position.id, NOT NULL | 发起人岗位 |
| formMetadataId | UUID | FK -> FormMetadata.id, NOT NULL | 表单元数据快照 |
| formDataId | UUID | NOT NULL | 业务表单数据 ID |
| currentNodes | JSON | NOT NULL | 当前活跃节点列表 |
| status | Enum | NOT NULL | 状态：RUNNING / COMPLETED / TERMINATED / SUSPENDED |
| startTime | Timestamp | NOT NULL | 发起时间 |
| endTime | Timestamp | NULLABLE | 结束时间 |
| tenantId | UUID | FK -> Tenant.id, NOT NULL | 所属租户 |
| createdAt | Timestamp | NOT NULL | |
| updatedAt | Timestamp | NOT NULL | |

**业务规则**：

- 流程实例创建时，冻结当前定义版本和表单元数据快照。
- 发起人身份上下文（组织、部门、岗位）在发起时快照，后续身份切换不影响已发起流程。
- 流程实例运行中，节点流转由路由条件和动作驱动。
- 流程完成或终止后，所有未完成任务自动取消。
- 流程挂起后，超时计时器暂停。

**领域事件**：

| 事件类型 | 触发时机 | 载荷 |
|----------|----------|------|
| `process.instance.started` | 流程发起 | instanceId, definitionId, initiatorId |
| `process.instance.completed` | 流程完成 | instanceId, endTime |
| `process.instance.terminated` | 流程终止 | instanceId, reason |
| `process.instance.suspended` | 流程挂起 | instanceId, reason |
| `process.instance.resumed` | 流程恢复 | instanceId |

### 3.3 TaskInstance（任务实例）

任务实例是流程实例中一个用户任务节点的运行实例。

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | UUID | PK | 任务实例唯一标识 |
| instanceId | UUID | FK -> ProcessInstance.id, NOT NULL | 所属流程实例 |
| nodeId | String(64) | NOT NULL | 对应定义中的节点 ID |
| nodeName | String(128) | NOT NULL | 节点名称快照 |
| nodeType | Enum | NOT NULL | 节点类型快照 |
| assigneeId | UUID | FK -> Person.id, NULLABLE | 当前处理人 |
| assigneeOrgId | UUID | FK -> Organization.id, NULLABLE | 处理人组织 |
| assigneeDeptId | UUID | FK -> Department.id, NULLABLE | 处理人部门 |
| assigneePositionId | UUID | FK -> Position.id, NULLABLE | 处理人岗位 |
| candidateType | Enum | NULLABLE | 候选类型：PERSON / POSITION / ROLE / ORG_MANAGER / DEPT_MANAGER |
| candidateIds | JSON | NULLABLE | 候选人/候选岗位/候选角色 ID 列表 |
| multiInstanceType | Enum | NULLABLE | 多实例类型：NONE / SEQUENTIAL / PARALLEL |
| completionCondition | String(256) | NULLABLE | 完成条件（会签比例等） |
| status | Enum | NOT NULL | 状态：CREATED / CLAIMED / COMPLETED / TERMINATED / TRANSFERRED |
| claimTime | Timestamp | NULLABLE | 认领时间 |
| completedTime | Timestamp | NULLABLE | 完成时间 |
| dueTime | Timestamp | NULLABLE | 到期时间 |
| tenantId | UUID | FK -> Tenant.id, NOT NULL | 所属租户 |
| createdAt | Timestamp | NOT NULL | |
| updatedAt | Timestamp | NOT NULL | |

**业务规则**：

- 任务创建时，根据参与者规则计算候选人，候选人可认领（CLAIMED）。
- 单人任务直接分配给唯一候选人，无需认领。
- 会签任务需多人处理，按完成条件判定是否流转。
- 或签任务任一人处理即流转。
- 任务可转办（TRANSFERRED），转办后原处理人释放，新处理人接管。
- 任务到期后触发超时事件，由 `06-消息移动与生态` 发送催办提醒。

**领域事件**：

| 事件类型 | 触发时机 | 载荷 |
|----------|----------|------|
| `process.task.created` | 任务创建 | taskId, instanceId, nodeId, candidateType, candidateIds |
| `process.task.claimed` | 任务认领 | taskId, assigneeId |
| `process.task.completed` | 任务完成 | taskId, instanceId, actionCode |
| `process.task.terminated` | 任务终止 | taskId, reason |
| `process.task.transferred` | 任务转办 | taskId, fromPersonId, toPersonId |
| `process.task.overdue` | 任务超时 | taskId, instanceId, dueTime |

### 3.4 TaskAction（任务动作记录）

任务动作记录是审批轨迹的明细。

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | UUID | PK | |
| taskId | UUID | FK -> TaskInstance.id, NOT NULL | 所属任务 |
| instanceId | UUID | FK -> ProcessInstance.id, NOT NULL | 所属流程实例 |
| actionCode | String(64) | NOT NULL | 动作编码（如 approve, reject, return, delegate） |
| actionName | String(128) | NOT NULL | 动作名称 |
| operatorId | UUID | FK -> Person.id, NOT NULL | 操作人 |
| operatorOrgId | UUID | FK -> Organization.id, NOT NULL | 操作人组织 |
| operatorPositionId | UUID | FK -> Position.id, NOT NULL | 操作人岗位 |
| opinion | String(1024) | NULLABLE | 审批意见 |
| targetNodeId | String(64) | NULLABLE | 目标节点（退回/跳转时） |
| formDataPatch | JSON | NULLABLE | 表单数据变更 |
| createdAt | Timestamp | NOT NULL | |

**业务规则**：

- 每次任务处理必须记录动作，包括动作编码、操作人、意见和表单变更。
- 动作记录不可修改和删除，作为审计依据。
- 退回动作必须指定退回目标节点。

### 3.5 ActionDefinition（审批动作定义）

动作定义是流程定义中可配置的审批动作类型。

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | UUID | PK | |
| code | String(64) | UK, NOT NULL | 动作编码，租户内唯一 |
| name | String(128) | NOT NULL | 动作名称 |
| category | Enum | NOT NULL | 类别：APPROVE / REJECT / RETURN / DELEGATE / TRANSFER / ADD_SIGN / TERMINATE / SUSPEND / CUSTOM |
| routeTarget | Enum | NOT NULL | 路由目标：NEXT_NODE / SPECIFIC_NODE / PREVIOUS_NODE / END |
| requireOpinion | Boolean | NOT NULL, DEFAULT FALSE | 是否必填意见 |
| requireTarget | Boolean | NOT NULL, DEFAULT FALSE | 是否需要选择目标节点 |
| uiConfig | JSON | NULLABLE | 前端展示配置（按钮样式、图标、颜色） |
| tenantId | UUID | FK -> Tenant.id, NOT NULL | 所属租户 |

**预置动作**：

| code | name | category | routeTarget | requireOpinion |
|------|------|----------|-------------|----------------|
| approve | 同意 | APPROVE | NEXT_NODE | false |
| reject | 驳回 | REJECT | END | true |
| return | 退回 | RETURN | PREVIOUS_NODE | true |
| delegate | 委托 | DELEGATE | NEXT_NODE | false |
| transfer | 转办 | TRANSFER | NEXT_NODE | false |
| add_sign | 加签 | ADD_SIGN | NEXT_NODE | false |
| terminate | 终止 | TERMINATE | END | true |
| suspend | 挂起 | SUSPEND | NEXT_NODE | true |

### 3.6 FormMetadata（表单元数据）

表单元数据是表单的版本化模型，包含字段、布局、校验和权限映射。

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | UUID | PK | 表单元数据唯一标识 |
| code | String(64) | UK, NOT NULL | 表单编码，租户内唯一 |
| name | String(128) | NOT NULL | 表单名称 |
| version | Integer | NOT NULL, DEFAULT 1 | 版本号 |
| status | Enum | NOT NULL | 状态：DRAFT / PUBLISHED / DEPRECATED |
| fields | JSON | NOT NULL | 字段定义列表（见 FieldDefinition） |
| layout | JSON | NOT NULL | 布局定义（行/列/分组/Tab） |
| validations | JSON | NULLABLE | 校验规则列表 |
| fieldPermissionMap | JSON | NULLABLE | 节点级字段权限映射 |
| tenantId | UUID | FK -> Tenant.id, NOT NULL | 所属租户 |
| publishedAt | Timestamp | NULLABLE | 发布时间 |
| createdAt | Timestamp | NOT NULL | |
| updatedAt | Timestamp | NOT NULL | |

**UK 约束**：(code, version)

#### FieldDefinition（字段定义，值对象）

| 字段 | 类型 | 说明 |
|------|------|------|
| fieldCode | String(128) | 字段编码，表单内唯一 |
| fieldName | String(128) | 字段名称 |
| fieldType | Enum | 字段类型：TEXT / NUMBER / DATE / DATETIME / SELECT / MULTI_SELECT / PERSON / ORG / DEPT / POSITION / ROLE / ATTACHMENT / RICH_TEXT / IMAGE / TABLE / REFERENCE |
| required | Boolean | 是否必填 |
| defaultValue | JSON | 默认值 |
| dictionaryCode | String(64) | NULLABLE，字典编码（fieldType=SELECT/MULTI_SELECT 时） |
| multiValue | Boolean | 是否多值 |
| visible | Boolean | 默认是否可见 |
| editable | Boolean | 默认是否可编辑 |
| maxLength | Integer | NULLABLE，最大长度 |
| min | Number | NULLABLE，最小值 |
| max | Number | NULLABLE，最大值 |
| pattern | String(256) | NULLABLE，正则校验 |
| childFields | List\<FieldDefinition\> | NULLABLE，子表字段（fieldType=TABLE 时） |
| linkageRules | JSON | NULLABLE，联动规则 |

**业务规则**：

- 表单元数据采用版本链管理，发布后不可修改。
- 流程实例创建时冻结表单版本快照，后续表单修改不影响已发起实例。
- 字段权限映射以 `{nodeId -> {fieldCode -> {visible, editable, required}}}` 格式存储。
- 表单渲染器根据字段类型和权限映射动态渲染。

**领域事件**：

| 事件类型 | 触发时机 | 载荷 |
|----------|----------|------|
| `form.metadata.created` | 表单创建 | metadataId, code, version |
| `form.metadata.published` | 表单发布 | metadataId, code, version |
| `form.metadata.deprecated` | 表单废弃 | metadataId, code, version |

### 3.7 TodoItem（待办项）

待办项是统一待办中心的视图模型，从 TaskInstance 投影而来。

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | UUID | PK | 待办项唯一标识 |
| taskId | UUID | FK -> TaskInstance.id, UK, NOT NULL | 关联任务实例 |
| instanceId | UUID | FK -> ProcessInstance.id, NOT NULL | 关联流程实例 |
| type | Enum | NOT NULL | 待办类型：PENDING / PENDING_READ / DRAFT |
| category | String(64) | NULLABLE | 流程分类 |
| title | String(256) | NOT NULL | 待办标题 |
| assigneeId | UUID | FK -> Person.id, NOT NULL | 处理人 |
| assigneeOrgId | UUID | FK -> Organization.id, NOT NULL | 处理人组织 |
| assigneeDeptId | UUID | FK -> Department.id, NULLABLE | 处理人部门 |
| assigneePositionId | UUID | FK -> Position.id, NOT NULL | 处理人岗位 |
| initiatorId | UUID | FK -> Person.id, NOT NULL | 发起人 |
| initiatorName | String(64) | NOT NULL | 发起人姓名 |
| urgency | Enum | NOT NULL, DEFAULT NORMAL | 紧急程度：NORMAL / URGENT / CRITICAL |
| dueTime | Timestamp | NULLABLE | 到期时间 |
| status | Enum | NOT NULL | 状态：ACTIVE / COMPLETED / CANCELLED |
| readStatus | Enum | NULLABLE | 阅读状态：UNREAD / READ（PENDING_READ 类型时） |
| tenantId | UUID | FK -> Tenant.id, NOT NULL | 所属租户 |
| createdAt | Timestamp | NOT NULL | |
| updatedAt | Timestamp | NOT NULL | |

**业务规则**：

- 待办是事务处理视图，消息是通知触达视图，两者联动但不混同（D16）。
- 待办项从 TaskInstance 投影，不独立创建。
- 任务完成/终止后，对应待办项状态同步更新。
- 待办事件发布后，由 `06-消息移动与生态` 消费并决定触达渠道。
- 身份上下文切换后，待办列表和待办数量必须同步刷新（D08）。

**领域事件**：

| 事件类型 | 触发时机 | 载荷 |
|----------|----------|------|
| `todo.item.created` | 待办创建 | todoId, taskId, assigneeId, type, category |
| `todo.item.completed` | 待办完成 | todoId, taskId |
| `todo.item.cancelled` | 待办取消 | todoId, reason |
| `todo.item.overdue` | 待办超时 | todoId, dueTime |

## 4. 待办与消息分工（D16）

### 4.1 分工原则

| 视图 | 职责 | 管理模块 |
|------|------|----------|
| 待办视图 | 事务处理入口，待办/已办/我发起/抄送/草稿等任务列表的聚合与流转 | `02-流程与表单` |
| 消息视图 | 通知触达入口，站内信/邮件/短信/企业微信/钉钉等多渠道路由与送达 | `06-消息移动与生态` |

### 4.2 映射规则

- 本模块发布待办事件（`todo.item.created` / `todo.item.overdue`），`06` 消费后决定触达渠道和内容模板。
- 待办创建 -> 消息提醒（站内信 + 可选外部渠道）。
- 待办超时 -> 催办提醒（站内信 + 外部渠道升级）。
- 待办完成 -> 不产生消息（事务闭环）。
- 抄送到达 -> 阅知提醒（站内信）。

## 5. 身份上下文集成（D08）

### 5.1 流程发起时

- 发起人身份上下文（组织、部门、岗位）在发起时快照到 ProcessInstance。
- 后续身份切换不影响已发起流程的发起人信息。

### 5.2 任务分配时

- 任务分配基于当前定义中的参与者规则和 `01-组织与权限` 输出的身份上下文计算。
- 参与者规则支持按组织、部门、岗位、角色、表单字段值和表达式计算候选人。

### 5.3 任务处理时

- 任务处理人的身份上下文在处理时快照到 TaskAction。
- 权限判定（字段权限、数据权限）基于处理人当前身份上下文。

### 5.4 身份切换后

- 待办列表、待办数量、门户待办卡片必须同步刷新。
- 已分配任务不因身份切换而重新分配。

## 6. 领域事件汇总

所有领域事件遵循命名规范 `{模块前缀}.{子模块名}.{动作}`，模块前缀为 `process` 和 `form`。

| 事件类型 | 载荷关键字段 | 消费模块 |
|----------|-------------|----------|
| `process.definition.created` | definitionId, code, version | 04/06 |
| `process.definition.published` | definitionId, code, version | 04/06 |
| `process.definition.deprecated` | definitionId, code, version | 04/06 |
| `process.instance.started` | instanceId, definitionId, initiatorId | 04/06 |
| `process.instance.completed` | instanceId, endTime | 03/04/05/06 |
| `process.instance.terminated` | instanceId, reason | 04/06 |
| `process.instance.suspended` | instanceId, reason | 04/06 |
| `process.instance.resumed` | instanceId | 04/06 |
| `process.task.created` | taskId, instanceId, candidateIds | 04/06 |
| `process.task.claimed` | taskId, assigneeId | 04/06 |
| `process.task.completed` | taskId, instanceId, actionCode | 03/04/05/06 |
| `process.task.terminated` | taskId, reason | 04/06 |
| `process.task.transferred` | taskId, fromPersonId, toPersonId | 04/06 |
| `process.task.overdue` | taskId, instanceId, dueTime | 06 |
| `form.metadata.created` | metadataId, code, version | 04 |
| `form.metadata.published` | metadataId, code, version | 04 |
| `form.metadata.deprecated` | metadataId, code, version | 04 |
| `todo.item.created` | todoId, taskId, assigneeId, type | 04/06 |
| `todo.item.completed` | todoId, taskId | 04/06 |
| `todo.item.cancelled` | todoId, reason | 04/06 |
| `todo.item.overdue` | todoId, dueTime | 06 |

## 7. 跨模块依赖

| 依赖方向 | 依赖内容 | 说明 |
|----------|----------|------|
| 02 -> 00 | 事件总线、审计日志、错误码 | 基础设施依赖 |
| 02 -> 01 | 身份上下文、角色集合、组织树、岗位树 | 参与者计算和权限判定 |
| 02 -> 01 | 数据权限裁剪 | 流程实例和任务的数据范围 |
| 02 -> 00 | 附件中心 | 流程附件上传与预览 |
| 04 -> 02 | 待办聚合、流程发起入口 | 门户待办卡片和快捷入口 |
| 05 -> 02 | 流程发起、审批处理 | 业务应用绑定流程 |
| 06 -> 02 | 待办事件消费、催办触达 | 消息路由与多渠道送达 |

## 8. 数据库设计建议

### 8.1 核心表清单

| 表名 | 对应聚合根 | 说明 |
|------|-----------|------|
| proc_definition | ProcessDefinition | 流程定义主表 |
| proc_instance | ProcessInstance | 流程实例主表 |
| proc_task | TaskInstance | 任务实例主表 |
| proc_task_action | TaskAction | 任务动作记录表 |
| proc_action_def | ActionDefinition | 动作定义表 |
| form_metadata | FormMetadata | 表单元数据主表 |
| todo_item | TodoItem | 待办项表 |

### 8.2 索引建议

- `proc_definition`：(tenantId, code, version)、(tenantId, category, status)
- `proc_instance`：(tenantId, definitionId, status)、(tenantId, initiatorId, status)、(tenantId, status, startTime)
- `proc_task`：(tenantId, instanceId)、(tenantId, assigneeId, status)、(tenantId, status, dueTime)
- `proc_task_action`：(taskId, createdAt)、(instanceId, createdAt)
- `proc_action_def`：(tenantId, code)、(tenantId, category)
- `form_metadata`：(tenantId, code, version)、(tenantId, status)
- `todo_item`：(tenantId, assigneeId, type, status)、(tenantId, assigneePositionId, status)、(tenantId, initiatorId, status)、(tenantId, status, dueTime)

### 8.3 大字段存储建议

- `proc_definition.nodes`、`proc_definition.routes`、`form_metadata.fields`、`form_metadata.layout` 等 JSON 大字段建议使用 JSONB 类型（PostgreSQL）或压缩存储。
- 流程定义和表单元数据的 JSON 数据在发布后不再变更，可考虑归档到独立存储。
