# 07-数据服务与集成 领域模型

## 1. 文档目的

本文档细化 `07-数据服务与集成` 模块的核心领域实体、值对象、聚合根、实体关系和业务规则，作为后端工程建模、数据库设计、开放接口契约、连接器实现和同步任务治理的统一依据。

对应架构决策编号：D05（`04` 与 `07` 数据边界）、D12（统一事件契约）、D13（最终一致性）、D17（数据开放分层）、D20（一期实施范围）。

## 2. 领域定位与边界

### 2.1 领域职责

`07-数据服务与集成` 负责平台对外开放、跨系统连接和数据运营治理，核心职责包括：

- 统一管理可复用的数据服务定义、输入输出协议和权限模型
- 提供开放 API、调用凭证、限流配额、调用审计和版本治理能力
- 管理 HTTP、数据库、消息队列、文件交换和 SaaS 等连接器
- 承担组织、字典、业务单据等跨系统同步任务和补偿处理
- 建设流程、内容、会议、考勤、任务等统计口径、报表快照和门户图卡数据源
- 维护接口健康检查、异常告警、调用追踪和版本兼容策略

### 2.2 关键边界

- **`07` 拥有开放与集成真相源**：服务定义、接口注册、连接器配置、同步任务定义、统计口径和治理规则归 `07`。
- **`07` 不拥有业务主数据**：组织、流程、内容、消息和协同业务主数据仍归其所属业务域，`07` 只持有映射、同步状态、统计快照和开放协议。
- **门户聚合不归 `07` 持有**：`04-门户与工作台` 的首页与办公中心聚合接口只服务用户入口，`07` 不承担门户场景的页面级读模型。
- **用户触达不归 `07` 持有**：消息渠道、站内信和移动入口归 `06-消息移动与生态`，`07` 只在需要时发布治理事件或通过 `06` 请求触达。
- **连接器是集成能力，不是业务语义**：连接器负责访问外部系统与资源，不拥有外部系统的业务规则解释权。
- **同步与报表是投影，不是主模型**：同步快照、对账结果和报表统计不可反向替代源系统或平台业务主数据。

### 2.3 一期收敛口径

`07` 不属于一期最小闭环必做范围（D20），但为后续对外集成和数据运营能力建设，建议优先收敛以下领域模型：

- `data-service`：服务定义、参数协议、字段映射和权限边界
- `open-api`：开放接口目录、凭证、限流配额和调用审计
- `connector`：统一连接器模型、参数管理和健康检查
- `data-sync`：同步任务、检查点、对账和补偿
- `report`：统计口径、聚合查询和报表快照
- `governance`：版本治理、健康状态和异常告警

一期简化或后置：

- 复杂服务编排、图形化连接流编排、跨云多环境联动后置
- 高级 BI 建模、复杂指标血缘和智能异常分析后置
- 大规模实时 CDC 与复杂流式计算后置

## 3. 领域总览

### 3.1 聚合根

| 聚合根 | 说明 | 目录 |
|--------|------|------|
| DataServiceDefinition | 可复用数据服务定义，承载查询/提交协议、参数模型、字段映射和权限边界 | `data-service/` |
| OpenApiEndpoint | 对外 API 注册、版本、鉴权、配额和调用策略的真相源 | `open-api/` |
| ConnectorDefinition | 外部系统、数据库、消息队列、文件交换和 SaaS 连接器配置真相源 | `connector/` |
| SyncExchangeTask | 同步任务、检查点、映射规则、补偿策略和执行约束 | `data-sync/` |
| ReportDefinition | 统计口径、维度、聚合规则、刷新策略和图卡输出协议 | `report/` |
| GovernanceProfile | 开放能力版本治理、健康检查、告警规则和异常处理策略 | `governance/` |

### 3.2 非聚合子领域说明

以下子模块主要围绕核心聚合提供运行态或投影能力，不独立持有新的业务真相源：

| 子模块 | 角色 | 说明 |
|--------|------|------|
| `open-api/` | 运行记录层 | 基于 `OpenApiEndpoint` 和凭证定义生成调用日志、配额消耗和错误分布 |
| `data-sync/` | 执行投影层 | 基于 `SyncExchangeTask` 生成执行记录、差异快照和补偿队列 |
| `report/` | 统计投影层 | 基于 `ReportDefinition` 生成报表快照、趋势排行和图卡数据 |
| `governance/` | 治理运行层 | 消费接口、连接器、同步和报表运行数据，输出健康态和告警事件 |

### 3.3 核心实体关系

```text
DataServiceDefinition  ──1:N──> ServiceParameterDefinition   (服务参数定义)
DataServiceDefinition  ──1:N──> ServiceFieldMapping          (字段映射)
DataServiceDefinition  ──M:1──> ConnectorDefinition          (可绑定外部连接器)
OpenApiEndpoint        ──M:1──> DataServiceDefinition        (开放 API 复用数据服务)
OpenApiEndpoint        ──1:N──> ApiCredentialGrant           (调用凭证授权)
OpenApiEndpoint        ──1:N──> ApiRateLimitPolicy           (限流与配额)
ConnectorDefinition    ──1:N──> ConnectorParameter           (连接参数)
ConnectorDefinition    ──1:N──> ConnectorHealthSnapshot      (健康检查快照)
SyncExchangeTask       ──M:1──> ConnectorDefinition          (源连接器)
SyncExchangeTask       ──M:1──> ConnectorDefinition          (目标连接器)
SyncExchangeTask       ──1:N──> SyncMappingRule              (同步映射规则)
SyncExchangeTask       ──1:N──> SyncExecutionRecord          (执行记录)
ReportDefinition       ──1:N──> ReportMetricDefinition       (指标定义)
ReportDefinition       ──1:N──> ReportDimensionDefinition    (维度定义)
ReportDefinition       ──1:N──> ReportSnapshot               (统计快照)
GovernanceProfile      ──1:N──> HealthCheckRule              (健康检查规则)
GovernanceProfile      ──1:N──> AlertRule                    (告警规则)
GovernanceProfile      ──1:N──> ServiceVersionRecord         (版本治理记录)
```

### 3.4 核心业务流

```text
业务域发布事件 / 外部系统调用开放 API
  -> 07 解析数据服务定义与接口协议
  -> 连接器访问内部/外部资源
  -> 同步任务或报表任务执行
  -> 治理层记录审计、健康状态和告警
  -> 结果输出到第三方系统 / 管理端分析页 / 门户图卡数据源
```

## 4. 核心聚合定义

### 4.1 DataServiceDefinition（数据服务定义）

数据服务定义负责管理平台可复用的查询、提交与转换协议，是 `07` 内部复用与对外开放的服务真相源。

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | UUID | PK | 服务定义唯一标识 |
| code | String(64) | UK, NOT NULL | 服务编码，租户内唯一 |
| name | String(128) | NOT NULL | 服务名称 |
| serviceType | Enum | NOT NULL | QUERY / COMMAND / EXPORT / CALLBACK |
| sourceMode | Enum | NOT NULL | INTERNAL_QUERY / CONNECTOR / MIXED |
| permissionMode | Enum | NOT NULL | PUBLIC_INTERNAL / APP_SCOPED / SUBJECT_SCOPED |
| cachePolicy | JSON | NULLABLE | 缓存与失效策略 |
| status | Enum | NOT NULL | DRAFT / ACTIVE / DEPRECATED / DISABLED |
| tenantId | UUID | NOT NULL | 所属租户 |
| createdAt | Timestamp | NOT NULL | |
| updatedAt | Timestamp | NOT NULL | |

#### 关联实体：ServiceParameterDefinition（服务参数定义）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | UUID | PK | |
| dataServiceId | UUID | FK -> DataServiceDefinition.id | 所属服务 |
| paramCode | String(64) | NOT NULL | 参数编码 |
| paramType | Enum | NOT NULL | STRING / NUMBER / BOOLEAN / DATE / JSON / PAGEABLE |
| required | Boolean | NOT NULL | 是否必填 |
| defaultValue | String(512) | NULLABLE | 默认值 |
| validationRule | JSON | NULLABLE | 校验规则 |

#### 关联实体：ServiceFieldMapping（字段映射）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | UUID | PK | |
| dataServiceId | UUID | FK -> DataServiceDefinition.id | 所属服务 |
| sourceField | String(128) | NOT NULL | 来源字段 |
| targetField | String(128) | NOT NULL | 输出字段 |
| transformRule | JSON | NULLABLE | 转换规则 |
| masked | Boolean | NOT NULL, DEFAULT FALSE | 是否脱敏 |

**业务规则**：

- 数据服务必须显式声明输入输出协议，不允许通过隐式 SQL 或临时脚本对外暴露不透明字段。
- `sourceMode=CONNECTOR` 的服务必须绑定已启用连接器，并通过连接器密钥引用访问外部资源。
- 对外开放的数据服务必须声明权限模式、配额和审计要求。
- 数据服务允许被 `open-api`、`report`、`data-sync` 复用，但复用不改变其真相源归属。

### 4.2 OpenApiEndpoint（开放接口）

开放接口聚合负责管理对外 API 的版本、鉴权与配额策略。

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | UUID | PK | 接口唯一标识 |
| code | String(64) | UK, NOT NULL | 接口编码 |
| path | String(256) | NOT NULL | 对外路径 |
| httpMethod | Enum | NOT NULL | GET / POST / PUT / DELETE |
| version | String(32) | NOT NULL | 接口版本 |
| authType | Enum | NOT NULL | APP_KEY / SIGNATURE / OAUTH2 / INTERNAL |
| quotaPolicy | JSON | NULLABLE | 调用配额 |
| rateLimitPolicy | JSON | NULLABLE | 限流策略 |
| status | Enum | NOT NULL | DRAFT / ACTIVE / DEPRECATED / OFFLINE |
| tenantId | UUID | NOT NULL | 所属租户 |

#### 关联实体：ApiCredentialGrant（凭证授权）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | UUID | PK | |
| openApiId | UUID | FK -> OpenApiEndpoint.id | 所属接口 |
| clientCode | String(64) | NOT NULL | 调用方编码 |
| secretRef | String(128) | NOT NULL | 密钥引用 |
| expiresAt | Timestamp | NULLABLE | 有效期 |
| status | Enum | NOT NULL | ACTIVE / REVOKED / EXPIRED |

**业务规则**：

- 接口版本发布后不可原地修改协议，破坏性变更必须创建新版本。
- 调用凭证必须与租户、调用方和接口范围绑定，禁止“全局万能凭证”。
- 接口调用日志和配额消耗属于运行态对象，不反向修改聚合本身。

### 4.3 ConnectorDefinition（连接器定义）

连接器定义负责统一管理外部系统访问能力。

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | UUID | PK | 连接器唯一标识 |
| code | String(64) | UK, NOT NULL | 连接器编码 |
| connectorType | Enum | NOT NULL | HTTP / DATABASE / MQ / FILE / SAAS |
| vendor | String(64) | NULLABLE | 厂商或适配器类型 |
| protocol | String(32) | NULLABLE | 协议 |
| authMode | Enum | NOT NULL | BASIC / TOKEN / SECRET_REF / NONE |
| timeoutConfig | JSON | NULLABLE | 超时与重试配置 |
| status | Enum | NOT NULL | DRAFT / ACTIVE / DISABLED |
| tenantId | UUID | NOT NULL | 所属租户 |

#### 关联实体：ConnectorParameter（连接参数）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | UUID | PK | |
| connectorId | UUID | FK -> ConnectorDefinition.id | 所属连接器 |
| paramKey | String(64) | NOT NULL | 参数名 |
| paramValueRef | String(256) | NOT NULL | 参数值或密钥引用 |
| sensitive | Boolean | NOT NULL | 是否敏感 |

**业务规则**：

- 敏感参数必须使用安全模块提供的密钥引用，不在文档或运行日志中直接暴露明文。
- 连接器负责协议适配、连接测试和健康快照，不负责业务字段解释。
- 同一连接器可被多个服务、同步任务和报表定义复用。

### 4.4 SyncExchangeTask（同步任务）

同步任务聚合负责跨系统数据交换、检查点和补偿策略。

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | UUID | PK | 任务唯一标识 |
| code | String(64) | UK, NOT NULL | 任务编码 |
| taskType | Enum | NOT NULL | IMPORT / EXPORT / BIDIRECTIONAL |
| syncMode | Enum | NOT NULL | FULL / INCREMENTAL / EVENT_DRIVEN |
| sourceConnectorId | UUID | FK -> ConnectorDefinition.id | 源连接器 |
| targetConnectorId | UUID | FK -> ConnectorDefinition.id | 目标连接器 |
| checkpointMode | Enum | NOT NULL | OFFSET / TIMESTAMP / VERSION / NONE |
| compensationPolicy | JSON | NULLABLE | 补偿策略 |
| scheduleConfig | JSON | NULLABLE | 调度配置 |
| status | Enum | NOT NULL | DRAFT / ACTIVE / PAUSED / ERROR |
| tenantId | UUID | NOT NULL | 所属租户 |

#### 关联实体：SyncMappingRule（同步映射规则）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | UUID | PK | |
| syncTaskId | UUID | FK -> SyncExchangeTask.id | 所属任务 |
| sourceField | String(128) | NOT NULL | 源字段 |
| targetField | String(128) | NOT NULL | 目标字段 |
| transformRule | JSON | NULLABLE | 转换规则 |
| conflictStrategy | Enum | NOT NULL | OVERWRITE / SKIP / MERGE / MANUAL |

#### 关联实体：SyncExecutionRecord（同步执行记录）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | UUID | PK | |
| syncTaskId | UUID | FK -> SyncExchangeTask.id | 所属任务 |
| startedAt | Timestamp | NOT NULL | 开始时间 |
| finishedAt | Timestamp | NULLABLE | 结束时间 |
| executionStatus | Enum | NOT NULL | RUNNING / SUCCESS / FAILED / COMPENSATING |
| checkpointValue | String(256) | NULLABLE | 检查点 |
| diffSummary | JSON | NULLABLE | 差异摘要 |

**业务规则**：

- 同步任务必须显式声明源、目标、检查点和冲突策略，禁止隐式双向覆盖。
- `EVENT_DRIVEN` 同步优先消费统一事件总线，不直接订阅业务数据库变更。
- 差异与补偿记录可反映运行状态，但不替代源数据与目标数据真相源。

### 4.5 ReportDefinition（报表定义）

报表定义负责平台级统计口径和图卡输出协议。

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | UUID | PK | 报表定义唯一标识 |
| code | String(64) | UK, NOT NULL | 报表编码 |
| name | String(128) | NOT NULL | 报表名称 |
| reportType | Enum | NOT NULL | TREND / RANK / SUMMARY / CARD |
| sourceScope | Enum | NOT NULL | PROCESS / CONTENT / MEETING / TASK / ATTENDANCE / MESSAGE / MIXED |
| refreshMode | Enum | NOT NULL | SCHEDULED / EVENT_DRIVEN / ON_DEMAND |
| visibilityMode | Enum | NOT NULL | INTERNAL / PORTAL_CARD / OPEN_API |
| status | Enum | NOT NULL | DRAFT / ACTIVE / ARCHIVED |
| tenantId | UUID | NOT NULL | 所属租户 |

#### 关联实体：ReportMetricDefinition（指标定义）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | UUID | PK | |
| reportId | UUID | FK -> ReportDefinition.id | 所属报表 |
| metricCode | String(64) | NOT NULL | 指标编码 |
| aggregationType | Enum | NOT NULL | COUNT / SUM / AVG / DISTINCT / RATIO |
| formula | String(512) | NULLABLE | 计算公式 |

#### 关联实体：ReportSnapshot（报表快照）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | UUID | PK | |
| reportId | UUID | FK -> ReportDefinition.id | 所属报表 |
| snapshotAt | Timestamp | NOT NULL | 快照时间 |
| payload | JSON | NOT NULL | 快照数据 |
| freshnessStatus | Enum | NOT NULL | READY / STALE / FAILED |

**业务规则**：

- 报表指标必须可解释、可追溯，不允许只有前端图卡而无统计口径定义。
- `visibilityMode=PORTAL_CARD` 的报表只输出图卡数据协议，门户页面编排仍归 `04`。
- 报表快照可以基于事件刷新或调度刷新，但都需记录刷新时间和数据新鲜度。

### 4.6 GovernanceProfile（治理配置）

治理配置负责开放能力运行治理、健康检查和告警。

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | UUID | PK | 治理配置唯一标识 |
| code | String(64) | UK, NOT NULL | 配置编码 |
| scopeType | Enum | NOT NULL | API / CONNECTOR / SYNC / REPORT / MODULE |
| targetCode | String(64) | NOT NULL | 目标编码 |
| slaPolicy | JSON | NULLABLE | SLA 目标 |
| alertPolicy | JSON | NULLABLE | 告警策略 |
| status | Enum | NOT NULL | ACTIVE / DISABLED |
| tenantId | UUID | NOT NULL | 所属租户 |

**业务规则**：

- 治理配置不直接改写业务数据，只能通过治理动作触发重试、降级、停用、告警和版本切换。
- 健康快照、告警记录和版本记录属于运行治理数据，不替代服务定义或连接器定义真相源。
- 告警通知优先通过统一事件与 `06` 协同触达。

## 5. 关键规则与解析策略

### 5.1 数据开放边界原则

- 对外接口优先复用 `DataServiceDefinition`，避免为每个调用方复制一套接口实现。
- `04` 的办公中心聚合接口不是 `07` 的替代品；`07` 面向通用开放和第三方集成，`04` 面向用户入口。
- 任何跨系统写回都必须具备幂等键、审计记录和补偿入口。

### 5.2 连接器与同步原则

- 连接器负责连接能力，映射和对账规则归同步任务持有。
- 同步任务必须支持检查点、差异记录和失败重试，必要时进入人工补偿。
- 高风险同步默认采用异步与最终一致性，不直接压主业务事务链路。

### 5.3 报表与开放权限原则

- 开放 API 权限与报表可见范围必须显式建模，不能仅依赖前端隐藏。
- 报表面向管理与运营，不应暴露超出调用方权限的数据范围。
- 统计口径变更需要版本化或审计留痕，避免前后期报表口径漂移。

## 6. 事件模型

### 6.1 `07` 内部领域事件

| 事件类型 | 载荷关键字段 | 说明 |
|----------|-------------|------|
| `data.service.activated` | serviceId, code, serviceType | 数据服务启用 |
| `data.api.published` | apiId, code, version | 开放接口发布 |
| `data.api.deprecated` | apiId, code, version | 开放接口废弃 |
| `data.connector.updated` | connectorId, code, connectorType | 连接器配置更新 |
| `data.sync.completed` | taskId, code, checkpoint | 同步完成 |
| `data.sync.failed` | taskId, code, reason | 同步失败 |
| `data.report.refreshed` | reportId, code, snapshotAt | 报表刷新 |
| `data.governance.alerted` | governanceId, targetCode, alertLevel | 治理告警 |

### 6.2 入站业务事件

| 事件类型 | 处理方式 | 说明 |
|----------|----------|------|
| `org.*` | 触发主数据同步或报表统计刷新 | 组织与权限相关同步 |
| `process.*` | 触发流程统计、开放回调或同步任务 | 流程类集成 |
| `content.*` | 触发内容检索、报表和外部订阅推送 | 内容类集成 |
| `msg.*` | 触发消息统计与治理分析 | 消息运营 |
| `infra.config.updated` | 刷新连接器、接口和调度策略 | 基础设施变更 |
| `infra.scheduler.*` | 触发同步、报表与治理任务运行态更新 | 任务调度联动 |

## 7. 跨模块依赖

| 模块关系 | 依赖内容 | 说明 |
|----------|----------|------|
| `07 -> 00` | 事件总线、调度、配置、安全、审计、缓存、错误码 | 基础设施依赖 |
| `07 -> 01` | 身份上下文、组织维度、数据权限 | 调用方身份与统计维度裁剪 |
| `07 -> 02` | 流程实例、待办、动作结果 | 统计与开放服务来源 |
| `07 -> 03` | 内容发布、阅读、订阅、检索 | 内容运营和开放服务来源 |
| `07 -> 04` | 报表卡片协议输出给门户消费 | `04` 消费 `07` 的图卡数据源 |
| `07 -> 05` | 会议、任务、考勤、合同、资产等业务数据 | 业务报表与对外同步来源 |
| `07 -> 06` | 治理告警、调用告警、外部通知协同 | 通过 `06` 触达运维与业务人员 |

## 8. 数据库设计建议

### 8.1 核心表清单

| 表名 | 对应聚合根/实体 | 说明 |
|------|-----------------|------|
| `data_service_def` | DataServiceDefinition | 数据服务定义主表 |
| `data_service_param_def` | ServiceParameterDefinition | 服务参数定义表 |
| `data_service_field_mapping` | ServiceFieldMapping | 服务字段映射表 |
| `data_open_api_endpoint` | OpenApiEndpoint | 开放接口主表 |
| `data_api_credential_grant` | ApiCredentialGrant | 调用凭证授权表 |
| `data_api_rate_limit_policy` | ApiRateLimitPolicy | 接口限流与配额表 |
| `data_connector_def` | ConnectorDefinition | 连接器主表 |
| `data_connector_param` | ConnectorParameter | 连接器参数表 |
| `data_connector_health_snapshot` | ConnectorHealthSnapshot | 连接器健康快照表 |
| `data_sync_task` | SyncExchangeTask | 同步任务主表 |
| `data_sync_mapping_rule` | SyncMappingRule | 同步映射规则表 |
| `data_sync_execution_record` | SyncExecutionRecord | 同步执行记录表 |
| `data_report_def` | ReportDefinition | 报表定义主表 |
| `data_report_metric_def` | ReportMetricDefinition | 报表指标定义表 |
| `data_report_snapshot` | ReportSnapshot | 报表快照表 |
| `data_governance_profile` | GovernanceProfile | 治理配置表 |
| `data_health_check_rule` | HealthCheckRule | 健康检查规则表 |
| `data_alert_rule` | AlertRule | 告警规则表 |
| `data_service_version_record` | ServiceVersionRecord | 版本治理记录表 |

### 8.2 索引建议

- `data_service_def`：`(tenant_id, code)`、`(tenant_id, service_type, status)`
- `data_open_api_endpoint`：`(tenant_id, code, version)`、`(tenant_id, path, http_method, status)`
- `data_api_credential_grant`：`(open_api_id, client_code, status)`、`(expires_at)`
- `data_connector_def`：`(tenant_id, code)`、`(tenant_id, connector_type, status)`
- `data_sync_task`：`(tenant_id, code)`、`(status, sync_mode)`、`(source_connector_id, target_connector_id)`
- `data_sync_execution_record`：`(sync_task_id, started_at desc)`、`(execution_status, finished_at)`
- `data_report_def`：`(tenant_id, code)`、`(source_scope, visibility_mode, status)`
- `data_report_snapshot`：`(report_id, snapshot_at desc)`、`(freshness_status)`
- `data_governance_profile`：`(tenant_id, scope_type, target_code)`、`(status)`

### 8.3 大字段与缓存建议

- 参数协议、映射规则、报表快照、告警策略等建议采用 JSON 文本或 `NVARCHAR(MAX)` 存储。
- 高频开放接口配额与报表快照建议同时使用 Redis 缓存，并通过事件驱动或调度刷新失效。
- 运行日志、同步差异、调用错误堆栈应与聚合定义表适度分离，避免主表膨胀。

## 9. 建模优先级建议

### 9.1 优先建模

- `DataServiceDefinition`
- `OpenApiEndpoint`
- `ConnectorDefinition`
- `SyncExchangeTask`

### 9.2 可简化实现

- `ReportDefinition` 先支持固定指标和图卡数据源，不做复杂自定义 BI
- `GovernanceProfile` 先支持基础健康检查和告警，不做复杂拓扑分析
- `ConnectorDefinition` 先覆盖 HTTP、数据库和消息队列三类主流连接器

### 9.3 后续补强方向

- 多环境发布与灰度接口治理
- 实时流式同步和 CDC
- 指标血缘、智能诊断和成本治理
- 连接器市场化与项目模板复用

## 10. 结论

`07-数据服务与集成` 的核心不是“再写一层接口”，而是建立一套**可定义、可开放、可连接、可同步、可统计、可治理**的数据与集成模型。其建模重点应围绕以下原则展开：

- 用 `DataServiceDefinition` 统一服务协议
- 用 `OpenApiEndpoint` 统一对外开放边界和版本治理
- 用 `ConnectorDefinition` 统一外部资源访问能力
- 用 `SyncExchangeTask` 统一跨系统数据交换和补偿
- 用 `ReportDefinition` 统一统计口径和门户图卡数据源
- 用 `GovernanceProfile` 统一开放能力运行治理和告警闭环

该文档可直接作为后续 `HJO2OA-DataServices/docs/module-design.md`、数据库表设计、开放接口细化和同步任务实现的基础。
