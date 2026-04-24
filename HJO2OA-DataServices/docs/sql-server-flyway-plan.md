# DataServices SQL Server 与 Flyway 迁移方案

## 1. 目标

依据 `module-design.md`、`domain-model.md`、`development-tasks.md`，为 `07-数据服务与集成` 的 P0 公共底座建立可直接复用的 SQL Server 建表与 Flyway 迁移方案，覆盖：

- 六个子模块的核心聚合根与关联实体主表
- 公共审计日志技术表
- 统一的主键、租户、逻辑删除、时间戳与索引约定
- Bootstrap 的 SQL Server Flyway 迁移目录接入

## 2. 命名与字段约定

- 表名前缀统一使用 `data_`
- 主键统一使用 `UNIQUEIDENTIFIER`，默认值使用 `NEWSEQUENTIALID()`
- 业务主表统一包含 `tenant_id`、`deleted`、`created_at`、`updated_at`
- JSON 或策略类大字段统一使用 `NVARCHAR(MAX)`
- 逻辑删除字段统一使用 `deleted BIT`
- 统一使用 `DATETIME2(3)` 记录 UTC 时间

## 3. Flyway 版本策略

- `V1__create_platform_shared_tables.sql`：平台共享配置与字典表
- `V2__create_data_services_foundation_tables.sql`：DataServices 公共底座与六个子模块核心表

后续版本按模块能力递增：

- `V3+`：具体子模块业务实现、运行态优化表、补偿/对账扩展表

## 4. 本次 V2 覆盖范围

### 4.1 公共技术表

- `data_audit_log`

### 4.2 data-service

- `data_service_def`
- `data_service_param_def`
- `data_service_field_mapping`

### 4.3 open-api

- `data_open_api_endpoint`
- `data_api_credential_grant`
- `data_api_rate_limit_policy`

### 4.4 connector

- `data_connector_def`
- `data_connector_param`
- `data_connector_health_snapshot`

### 4.5 data-sync

- `data_sync_task`
- `data_sync_mapping_rule`
- `data_sync_execution_record`

### 4.6 report

- `data_report_def`
- `data_report_metric_def`
- `data_report_dimension_def`
- `data_report_snapshot`

说明：
`domain-model.md` 的 3.3 明确了 `ReportDimensionDefinition`，但原核心表清单未落表，本次作为文档缺口补齐。

### 4.7 governance

- `data_governance_profile`
- `data_health_check_rule`
- `data_alert_rule`
- `data_service_version_record`

## 5. 与公共底座代码的对应关系

- `HJO2OA-DataCommon` 中的 `BaseEntityDO` 对应统一的 `id / tenant_id / deleted / created_at / updated_at`
- `DataServicesMetaObjectHandler` 对应插入和更新时的公共字段自动填充
- `DataAuditLogAspect` 对应 `data_audit_log` 的审计日志落表约束
- `DataServicesErrorCode` 对应接口统一错误码接入
- `SpringDataDomainEventPublisher` 对应 `07 -> 00` 统一事件总线发布抽象

## 6. 接入方式

- Flyway 已由 `HJO2OA-Bootstrap/src/main/resources/application.yml` 指向 `classpath:db/migration/sqlserver`
- 本次只需要在该目录新增 `V2__create_data_services_foundation_tables.sql`
- 后续六个子模块实现具体仓储时，直接复用 `HJO2OA-DataCommon` 的 MyBatis-Plus 配置与基类即可
