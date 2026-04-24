-- ============================================================
-- V4: DataSync module tables
-- Entity source: SyncExchangeTaskDO, SyncMappingRuleDO, SyncExecutionRecordDO
--               (all extend BaseEntityDO → UNIQUEIDENTIFIER id/tenant_id)
-- ============================================================

CREATE TABLE dbo.data_sync_task (
    id                       UNIQUEIDENTIFIER NOT NULL PRIMARY KEY,
    tenant_id                UNIQUEIDENTIFIER NOT NULL,
    code                     NVARCHAR(64)     NOT NULL,
    name                     NVARCHAR(128)    NOT NULL,
    description              NVARCHAR(512)    NULL,
    task_type                NVARCHAR(32)     NOT NULL,
    sync_mode                NVARCHAR(32)     NOT NULL,
    source_connector_id      UNIQUEIDENTIFIER NULL,
    target_connector_id      UNIQUEIDENTIFIER NULL,
    dependency_status        NVARCHAR(32)     NOT NULL,
    checkpoint_mode          NVARCHAR(32)     NULL,
    checkpoint_config_json   NVARCHAR(MAX)    NULL,
    trigger_config_json      NVARCHAR(MAX)    NULL,
    retry_policy_json        NVARCHAR(MAX)    NULL,
    compensation_policy_json NVARCHAR(MAX)    NULL,
    reconciliation_policy_json NVARCHAR(MAX)  NULL,
    schedule_config_json     NVARCHAR(MAX)    NULL,
    status                   NVARCHAR(32)     NOT NULL,
    deleted                  INT              NOT NULL CONSTRAINT DF_data_sync_task_deleted DEFAULT ((0)),
    created_at               DATETIME2(6)     NOT NULL CONSTRAINT DF_data_sync_task_created_at DEFAULT (SYSUTCDATETIME()),
    updated_at               DATETIME2(6)     NOT NULL CONSTRAINT DF_data_sync_task_updated_at DEFAULT (SYSUTCDATETIME())
);

CREATE UNIQUE INDEX UX_data_sync_task_tenant_code
    ON dbo.data_sync_task (tenant_id, code) WHERE deleted = 0;
CREATE INDEX IX_data_sync_task_status
    ON dbo.data_sync_task (status, task_type) WHERE deleted = 0;

CREATE TABLE dbo.data_sync_mapping_rule (
    id                  UNIQUEIDENTIFIER NOT NULL PRIMARY KEY,
    tenant_id           UNIQUEIDENTIFIER NOT NULL,
    sync_task_id        UNIQUEIDENTIFIER NOT NULL,
    source_field        NVARCHAR(128)    NOT NULL,
    target_field        NVARCHAR(128)    NOT NULL,
    transform_rule_json NVARCHAR(MAX)    NULL,
    conflict_strategy   NVARCHAR(32)     NULL,
    key_mapping         BIT              NULL,
    sort_order          INT              NULL,
    deleted             INT              NOT NULL CONSTRAINT DF_data_sync_mapping_rule_deleted DEFAULT ((0)),
    created_at          DATETIME2(6)     NOT NULL CONSTRAINT DF_data_sync_mapping_rule_created_at DEFAULT (SYSUTCDATETIME()),
    updated_at          DATETIME2(6)     NOT NULL CONSTRAINT DF_data_sync_mapping_rule_updated_at DEFAULT (SYSUTCDATETIME()),
    CONSTRAINT FK_data_sync_mapping_rule_task
        FOREIGN KEY (sync_task_id) REFERENCES dbo.data_sync_task(id)
);

CREATE UNIQUE INDEX UX_data_sync_mapping_rule_task_source_target
    ON dbo.data_sync_mapping_rule (sync_task_id, source_field, target_field) WHERE deleted = 0;

CREATE TABLE dbo.data_sync_execution_record (
    id                     UNIQUEIDENTIFIER NOT NULL PRIMARY KEY,
    tenant_id              UNIQUEIDENTIFIER NOT NULL,
    sync_task_id           UNIQUEIDENTIFIER NOT NULL,
    task_code              NVARCHAR(64)     NULL,
    parent_execution_id    UNIQUEIDENTIFIER NULL,
    execution_batch_no     NVARCHAR(128)    NULL,
    trigger_type           NVARCHAR(32)     NULL,
    execution_status       NVARCHAR(32)     NOT NULL,
    idempotency_key        NVARCHAR(128)    NULL,
    checkpoint_value       NVARCHAR(MAX)    NULL,
    retry_count            INT              NULL,
    retryable              BIT              NULL,
    result_summary_json    NVARCHAR(MAX)    NULL,
    diff_summary_json      NVARCHAR(MAX)    NULL,
    difference_details_json NVARCHAR(MAX)   NULL,
    trigger_context_json   NVARCHAR(MAX)    NULL,
    failure_code           NVARCHAR(64)     NULL,
    failure_message        NVARCHAR(1024)   NULL,
    reconciliation_status NVARCHAR(32)     NULL,
    operator_account_id    NVARCHAR(64)     NULL,
    operator_person_id     NVARCHAR(64)     NULL,
    started_at             DATETIME2(6)     NULL,
    finished_at            DATETIME2(6)     NULL,
    deleted                INT              NOT NULL CONSTRAINT DF_data_sync_execution_record_deleted DEFAULT ((0)),
    created_at             DATETIME2(6)     NOT NULL CONSTRAINT DF_data_sync_execution_record_created_at DEFAULT (SYSUTCDATETIME()),
    updated_at             DATETIME2(6)     NOT NULL CONSTRAINT DF_data_sync_execution_record_updated_at DEFAULT (SYSUTCDATETIME()),
    CONSTRAINT FK_data_sync_execution_record_task
        FOREIGN KEY (sync_task_id) REFERENCES dbo.data_sync_task(id)
);

CREATE INDEX IX_data_sync_execution_record_task_idempotency
    ON dbo.data_sync_execution_record (sync_task_id, idempotency_key);
CREATE INDEX IX_data_sync_execution_record_status
    ON dbo.data_sync_execution_record (execution_status, started_at DESC) WHERE deleted = 0;
