# 工程命名总表

对应架构决策：D11（接口契约）、D12（事件契约）、D19（工程启动条件）、D20（一期实施范围）。

## 1. 文档目的

本文档用于将 HJO2OA 当前已经收敛的模块目录名、建议包名、API 前缀、事件命名口径和数据所有权统一为一份工程启动基线。

后续凡涉及以下内容，均以本文档为默认命名依据：

- 新建工程目录
- Java 包结构设计
- API 路径分组
- 事件类型命名
- 事件 `source` 字段取值
- 子模块数据所有权划分

## 2. 权威来源与使用顺序

 工程命名冲突出现时，按以下顺序裁决：

  1. `docs/decisions/architecture-convergence-decisions.md`
  2. `docs/contracts/unified-api-contract.md`
  3. `docs/contracts/unified-event-contract.md`
  4. `docs/architecture/backend-architecture-spec.md`
  5. `docs/contracts/identity-context-protocol.md`
  6. 各模块 `docs/module-design.md` / `docs/domain-model.md`
  7. `docs/implementation/phase-1-implementation-slice.md`

 ## 3. 统一命名规则

 ### 3.1 目录命名规则

 - 所有模块和子模块目录统一使用 **HJO2OA-PascalCase** 命名格式，如 `HJO2OA-Infrastructure`、`HJO2OA-TodoCenter`
 - 顶层模块目录：`HJO2OA-Shared`、`HJO2OA-Bootstrap`、`HJO2OA-Infrastructure` 至 `HJO2OA-DataServices`
 - 子模块目录：`HJO2OA-EventBus`、`HJO2OA-OrgStructure`、`HJO2OA-TodoCenter` 等
 - 目录名与 Maven artifactId 保持一致
 - 子模块目录名一旦确定，不再为贴合事件主题或接口路径单独改目录名

 ### 3.1.1 文档命名关联规则

 - 模块级文档统一采用路径关联命名，如 `<module>/docs/module-overview.md`、`<module>/docs/domain-model.md`
 - 子模块级文档统一采用 `<submodule>.<aspect>.md` 命名
 - 文档名称必须能直接反映所属模块或子模块，不再使用泛化 `README.md` 作为正式索引文档名

### 3.2 Maven 构件命名规则

- 根聚合 POM：`groupId=com.hjo2oa`，`artifactId=hjo2oa`，`packaging=pom`
- 业务模块父 POM：`groupId=com.hjo2oa`，`artifactId=HJO2OA-<ModuleShortName>`，`packaging=pom`
- 子模块 jar：`groupId=com.hjo2oa`，`artifactId=HJO2OA-<SubmoduleName>`，`packaging=jar`
- 共享模块：`groupId=com.hjo2oa`，`artifactId=HJO2OA-Shared`，`packaging=jar`
- 启动模块：`groupId=com.hjo2oa`，`artifactId=HJO2OA-Bootstrap`，`packaging=jar`

示例：

| 子模块目录 | artifactId | Java 根包 |
|-----------|-----------|----------|
| `HJO2OA-Infrastructure/HJO2OA-EventBus/` | `HJO2OA-EventBus` | `com.hjo2oa.infra.event.bus` |
| `HJO2OA-OrgPerm/HJO2OA-OrgStructure/` | `HJO2OA-OrgStructure` | `com.hjo2oa.org.structure` |
| `HJO2OA-WorkflowForm/HJO2OA-TodoCenter/` | `HJO2OA-TodoCenter` | `com.hjo2oa.todo.center` |
| `HJO2OA-Messaging/HJO2OA-MessageCenter/` | `HJO2OA-MessageCenter` | `com.hjo2oa.msg.message.center` |

### 3.3 Java 包命名规则

- Java 根包建议统一使用 `com.hjo2oa`
- 模块根包使用英文业务前缀，不使用中文模块名
- 子模块包名由目录名按以下规则转换：

```text
HJO2OA-PascalCase 目录名 -> 去掉 HJO2OA- 前缀后 lower.dot.case 包名
```

示例：

- `HJO2OA-EventBus` -> 去前缀 `EventBus` -> `event.bus`
- `HJO2OA-PositionAssignment` -> 去前缀 `PositionAssignment` -> `position.assignment`
- `HJO2OA-RoleResourceAuth` -> 去前缀 `RoleResourceAuth` -> `role.resource.auth`
- `HJO2OA-DataI18n` -> 去前缀 `DataI18n` -> `data.i18n`

建议分层后缀：

- `application`
- `domain`
- `infrastructure`
- `interfaces`

示例：

```text
com.hjo2oa.todo.center.application
com.hjo2oa.todo.center.domain
com.hjo2oa.todo.center.infrastructure
com.hjo2oa.todo.center.interfaces
```

### 3.3 API 命名规则

统一 API 路径格式：

```text
/api/{version}/{module-prefix}/{resource}
```

其中 `module-prefix` 以 `docs/contracts/unified-api-contract.md` 为准：

- `infra`
- `org`
- `process`
- `form`
- `todo`
- `content`
- `portal`
- `biz`
- `msg`
- `data`

### 3.4 事件命名规则

统一事件类型格式：

```text
{模块前缀}.{事件主题}.{动作}
```

规则如下：

- 事件类型不强制等于子模块目录名
- 事件主题优先使用**稳定业务主语**，如 `organization`、`task`、`item`、`notification`
- 事件 `source` 统一使用**子模块稳定标识**：由子模块目录名去掉 `HJO2OA-` 前缀后转为 kebab-case，如 `HJO2OA-TodoCenter` -> `todo-center`
- 动作命名以 `docs/contracts/unified-event-contract.md` 为准，如 `created`、`updated`、`completed`、`overdue`

示例：

- 事件类型：`todo.item.created`
- 事件来源：`todo-center`

## 4. 模块根命名总表

| 模块 | 模块目录 | artifactId | 建议包根 | API 前缀 | 事件前缀 | 模块级数据所有权 | 当前阶段 |
|------|----------|-----------|----------|----------|----------|------------------|----------|
| 00-平台基础设施 | `HJO2OA-Infrastructure/` | `HJO2OA-Infrastructure` | `com.hjo2oa.infra` | `infra` | `infra.*` | 平台技术资产、附件、字典、配置、审计、租户、安全、调度等共享基础能力 | 一期核心 |
| 01-组织与权限 | `HJO2OA-OrgPerm/` | `HJO2OA-OrgPerm` | `com.hjo2oa.org` | `org` | `org.*` | 组织、部门、岗位、任职、人员、账号、角色、资源权限、数据权限、身份上下文 | 一期核心 |
| 02-流程与表单 | `HJO2OA-WorkflowForm/` | `HJO2OA-WorkflowForm` | `com.hjo2oa.process` / `com.hjo2oa.form` / `com.hjo2oa.todo` | `process` / `form` / `todo` | `process.*` / `form.*` / `todo.*` | 流程定义、流程实例、任务、动作、表单元数据、待办投影 | 一期核心 |
| 03-内容与知识 | `HJO2OA-Content/` | `HJO2OA-Content` | `com.hjo2oa.content` | `content` | `content.*` | 内容主模型、内容权限、检索、统计、推荐 | 二期预留 |
| 04-门户与工作台 | `HJO2OA-Portal/` | `HJO2OA-Portal` | `com.hjo2oa.portal` | `portal` | `portal.*` | 门户配置、工作台卡片配置、聚合读模型、首页渲染模型 | 一期核心 |
| 05-协同办公应用 | `HJO2OA-Collaboration/` | `HJO2OA-Collaboration` | `com.hjo2oa.biz` | `biz` | `biz.*` | 公文、会议、日程、考勤、合同、资产等业务主数据 | 二期预留 |
| 06-消息移动与生态 | `HJO2OA-Messaging/` | `HJO2OA-Messaging` | `com.hjo2oa.msg` | `msg` | `msg.*` | 通知消息、送达记录、订阅偏好、渠道路由、移动接入模型 | 一期核心 |
| 07-数据服务与集成 | `HJO2OA-DataServices/` | `HJO2OA-DataServices` | `com.hjo2oa.data` | `data` | `data.*` | 数据服务定义、开放接口、连接器、同步任务、报表与治理数据 | 二期预留 |

## 5. 一期必做子模块命名总表

### 5.1 00-平台基础设施

| 子模块 | 目录名 | 建议包根 | API 前缀 | 事件 `source` | 建议事件主题 | 主要数据所有权 | 一期状态 |
|--------|--------|----------|----------|---------------|--------------|----------------|----------|
| 事件总线 | `HJO2OA-EventBus` | `com.hjo2oa.infra.event.bus` | `infra` | `event-bus` | `event` | Outbox 记录、发布日志、订阅路由 | 必做 |
| 多租户 | `HJO2OA-Tenant` | `com.hjo2oa.infra.tenant` | `infra` | `tenant` | `tenant` | 租户、租户配置、配额 | 必做 |
| 审计日志 | `HJO2OA-Audit` | `com.hjo2oa.infra.audit` | `infra` | `audit` | `audit` | 审计日志、审计策略 | 必做 |
| 缓存管理 | `HJO2OA-Cache` | `com.hjo2oa.infra.cache` | `infra` | `cache` | `cache` | 缓存策略、失效规则 | 必做 |
| 数据字典 | `HJO2OA-Dictionary` | `com.hjo2oa.infra.dictionary` | `infra` | `dictionary` | `dictionary` | 字典类型、字典项 | 必做 |
| 配置中心 | `HJO2OA-Config` | `com.hjo2oa.infra.config` | `infra` | `config` | `config` | 系统参数、Feature Flag | 必做 |
| 安全工具 | `HJO2OA-Security` | `com.hjo2oa.infra.security` | `infra` | `security` | `security` | 密码策略、脱敏规则、签名规则 | 必做 |
| 错误码 | `HJO2OA-ErrorCode` | `com.hjo2oa.infra.error.code` | `infra` | `error-code` | `error-code` | 错误码注册表 | 必做 |
| 附件中心 | `HJO2OA-Attachment` | `com.hjo2oa.infra.attachment` | `infra` | `attachment` | `attachment` | 附件元数据、版本、预览记录 | 必做 |
| 国际化 | `HJO2OA-I18n` | `com.hjo2oa.infra.i18n` | `infra` | `i18n` | `i18n` | 语言包、国际化资源 | 必做 |
| 定时任务 | `HJO2OA-Scheduler` | `com.hjo2oa.infra.scheduler` | `infra` | `scheduler` | `scheduler` | 调度任务、执行记录 | 必做 |
| 时区管理 | `HJO2OA-Timezone` | `com.hjo2oa.infra.timezone` | `infra` | `timezone` | `timezone` | 时区偏好、转换规则 | 必做 |
| 多语言数据 | `HJO2OA-DataI18n` | `com.hjo2oa.infra.data.i18n` | `infra` | `data-i18n` | `data-i18n` | 多语言字段值、翻译状态 | 必做 |

### 5.2 01-组织与权限

| 子模块 | 目录名 | 建议包根 | API 前缀 | 事件 `source` | 建议事件主题 | 主要数据所有权 | 一期状态 |
|--------|--------|----------|----------|---------------|--------------|----------------|----------|
| 组织与部门管理 | `HJO2OA-OrgStructure` | `com.hjo2oa.org.structure` | `org` | `org-structure` | `organization` / `department` | Organization、Department | 必做 |
| 岗位与任职管理 | `HJO2OA-PositionAssignment` | `com.hjo2oa.org.position.assignment` | `org` | `position-assignment` | `position` / `assignment` | Position、Assignment | 必做 |
| 人员与账号管理 | `HJO2OA-PersonAccount` | `com.hjo2oa.org.person.account` | `org` | `person-account` | `person` / `account` | Person、Account（1:N，含主账号） | 必做 |
| 角色与资源授权管理 | `HJO2OA-RoleResourceAuth` | `com.hjo2oa.org.role.resource.auth` | `org` | `role-resource-auth` | `role` / `resource-permission` | Role、PositionRole、PersonRole、ResourcePermission | 必做 |
| 数据权限管理 | `HJO2OA-DataPermission` | `com.hjo2oa.org.data.permission` | `org` | `data-permission` | `data-permission` / `field-permission` | DataPermission、FieldPermission | 必做 |
| 身份切换与会话上下文 | `HJO2OA-IdentityContext` | `com.hjo2oa.org.identity.context` | `org` | `identity-context` | `identity` | 身份上下文缓存、权限快照版本、切换会话状态 | 必做 |

### 5.3 02-流程与表单

| 子模块 | 目录名 | 建议包根 | API 前缀 | 事件 `source` | 建议事件主题 | 主要数据所有权 | 一期状态 |
|--------|--------|----------|----------|---------------|--------------|----------------|----------|
| 流程定义与建模 | `HJO2OA-ProcessDefinition` | `com.hjo2oa.process.definition` | `process` | `process-definition` | `definition` | 流程定义、节点、路由、动作配置 | 必做 |
| 流程实例与任务管理 | `HJO2OA-ProcessInstance` | `com.hjo2oa.process.instance` | `process` | `process-instance` | `instance` / `task` | ProcessInstance、TaskInstance | 必做 |
| 审批动作与规则引擎 | `HJO2OA-ActionEngine` | `com.hjo2oa.process.action.engine` | `process` | `action-engine` | `task` | TaskAction、动作规则、动态参与者计算 | 必做 |
| 表单元数据与渲染协议 | `HJO2OA-FormMetadata` | `com.hjo2oa.form.metadata` | `form` | `form-metadata` | `metadata` | FormMetadata、字段/布局/校验/权限映射 | 必做 |
| 表单渲染器 | `HJO2OA-FormRenderer` | `com.hjo2oa.form.renderer` | `form` | `form-renderer` | `-` | 渲染协议消费层，不拥有独立业务主数据 | 必做 |
| 待办中心与聚合视图 | `HJO2OA-TodoCenter` | `com.hjo2oa.todo.center` | `todo` | `todo-center` | `item` | TodoItem 投影、待办聚合读模型 | 必做 |

### 5.4 04-门户与工作台

| 子模块 | 目录名 | 建议包根 | API 前缀 | 事件 `source` | 建议事件主题 | 主要数据所有权 | 一期状态 |
|--------|--------|----------|----------|---------------|--------------|----------------|----------|
| 工作台组件配置中心 | `HJO2OA-WidgetConfig` | `com.hjo2oa.portal.widget.config` | `portal` | `widget-config` | `widget` | 卡片类型、展示策略、数据源配置 | 必做 |
| 聚合数据接口 | `HJO2OA-AggregationApi` | `com.hjo2oa.portal.aggregation.api` | `portal` | `aggregation-api` | `-` | 聚合缓存、聚合读模型装配结果 | 必做 |
| 门户首页与办公中心 | `HJO2OA-PortalHome` | `com.hjo2oa.portal.home` | `portal` | `portal-home` | `portal` | 首页渲染配置、办公中心视图模型 | 必做 |

### 5.5 06-消息移动与生态

| 子模块 | 目录名 | 建议包根 | API 前缀 | 事件 `source` | 建议事件主题 | 主要数据所有权 | 一期状态 |
|--------|--------|----------|----------|---------------|--------------|----------------|----------|
| 统一消息中心 | `HJO2OA-MessageCenter` | `com.hjo2oa.msg.message.center` | `msg` | `message-center` | `notification` | Notification、消息状态、消息分类 | 必做 |
| 待办提醒与事件订阅 | `HJO2OA-EventSubscription` | `com.hjo2oa.msg.event.subscription` | `msg` | `event-subscription` | `subscription` | 订阅偏好、提醒策略、优先级升级规则 | 必做 |
| 多渠道发送能力 | `HJO2OA-ChannelSender` | `com.hjo2oa.msg.channel.sender` | `msg` | `channel-sender` | `notification` / `channel` | 渠道路由、发送任务、送达记录 | 必做 |
| 移动端接入支持 | `HJO2OA-MobileSupport` | `com.hjo2oa.msg.mobile.support` | `msg` | `mobile-support` | `-` | 移动端聚合视图、登录态适配、弱网容错状态 | 必做 |

补充约束：`06-消息移动与生态` 对外跨模块总线事件统一注册为 `msg.notification.*`；`message-center` 内部允许使用 `msg.message.*`，`channel-sender` 内部允许使用 `msg.channel.*`，但这两类名字不作为项目级统一事件契约注册项。

## 6. 一期后置但需预留命名的子模块

| 模块 | 子模块 | 目录名 | 建议包根 | API 前缀 | 事件 `source` | 当前处理策略 |
|------|--------|--------|----------|----------|---------------|--------------|
| 01-组织与权限 | 组织同步与审计 | `HJO2OA-OrgSyncAudit` | `com.hjo2oa.org.sync.audit` | `org` | `org-sync-audit` | 后置，按项目同步需求启用 |
| 02-流程与表单 | 流程监控与分析 | `HJO2OA-ProcessMonitor` | `com.hjo2oa.process.monitor` | `process` | `process-monitor` | 后置，作为读模型增强能力 |
| 04-门户与工作台 | 门户页面模型管理 | `HJO2OA-PortalModel` | `com.hjo2oa.portal.model` | `portal` | `portal-model` | 后置，不作为一期最小闭环验收前提 |
| 04-门户与工作台 | 个性化与角色化配置 | `HJO2OA-Personalization` | `com.hjo2oa.portal.personalization` | `portal` | `personalization` | 后置，一期以轻量配置替代 |
| 04-门户与工作台 | 门户设计器 | `HJO2OA-PortalDesigner` | `com.hjo2oa.portal.designer` | `portal` | `portal-designer` | 后置，不落完整设计器 |
| 06-消息移动与生态 | 第三方生态集成 | `HJO2OA-Ecosystem` | `com.hjo2oa.msg.ecosystem` | `msg` | `ecosystem` | 后置，按项目生态接入需求启用 |

## 7. 二期预留模块使用原则

对于 `03-内容与知识`、`05-协同办公应用`、`07-数据服务与集成`：

- 当前阶段仅冻结**模块根包**、**API 前缀**、**事件前缀**和**目录命名**
- 二期再细化到子模块级事件主题和数据表命名
- 在一期代码实现中，不得提前占用其他模块的命名空间

## 8. 使用示例

### 8.1 待办中心

- 目录：`HJO2OA-WorkflowForm/HJO2OA-TodoCenter/`
- artifactId：`HJO2OA-TodoCenter`
- 包根：`com.hjo2oa.todo.center`
- 接口前缀：`/api/v1/todo`
- 事件类型：`todo.item.created`
- 事件 `source`：`todo-center`
- 数据所有权：`TodoItem` 投影与待办聚合读模型

### 8.2 人员与账号管理

- 目录：`HJO2OA-OrgPerm/HJO2OA-PersonAccount/`
- artifactId：`HJO2OA-PersonAccount`
- 包根：`com.hjo2oa.org.person.account`
- 接口前缀：`/api/v1/org`
- 事件类型：`org.person.updated`、`org.account.locked`
- 事件 `source`：`person-account`
- 数据所有权：`Person`、`Account`

### 8.3 统一消息中心

- 目录：`HJO2OA-Messaging/HJO2OA-MessageCenter/`
- artifactId：`HJO2OA-MessageCenter`
- 包根：`com.hjo2oa.msg.message.center`
- 接口前缀：`/api/v1/msg`
- 跨模块事件类型：`msg.notification.sent`、`msg.notification.read`
- 内部领域事件：`msg.message.created`、`msg.message.read`、`msg.message.revoked`、`msg.message.expired`
- 事件 `source`：`message-center`
- 数据所有权：`Notification`、消息状态、消息分类

## 9. 执行约束

后续开始工程骨架搭建时，默认遵循以下约束：

- 不再新造 `task-center`、`form-render`、`notification` 这类已废弃口径
- `02` 持有待办投影数据所有权，`06` 只负责通知触达与订阅策略
- `eventType` 与 `source` 必须同时稳定：`eventType` 看业务主语，`source` 看由子模块目录推导出的稳定标识
- 代码包名、接口分组、事件 `source`、子模块目录名必须能相互映射
- 如未来需调整命名，先改本文档，再改工程实现

