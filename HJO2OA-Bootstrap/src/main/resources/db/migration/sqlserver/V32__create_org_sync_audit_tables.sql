CREATE TABLE dbo.org_sync_source_config (
    id UNIQUEIDENTIFIER NOT NULL PRIMARY KEY,
    tenant_id UNIQUEIDENTIFIER NOT NULL,
    source_code VARCHAR(64) NOT NULL,
    source_name NVARCHAR(128) NOT NULL,
    source_type VARCHAR(32) NOT NULL,
    endpoint NVARCHAR(512) NULL,
    config_ref NVARCHAR(256) NOT NULL,
    scope_expression NVARCHAR(1000) NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'DISABLED',
    created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME()
);

CREATE UNIQUE INDEX uk_org_sync_source_config_tenant_code
    ON dbo.org_sync_source_config (tenant_id, source_code);

CREATE INDEX idx_org_sync_source_config_tenant_status
    ON dbo.org_sync_source_config (tenant_id, status);

CREATE TABLE dbo.org_sync_task (
    id UNIQUEIDENTIFIER NOT NULL PRIMARY KEY,
    tenant_id UNIQUEIDENTIFIER NOT NULL,
    source_id UNIQUEIDENTIFIER NOT NULL,
    task_type VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    retry_of_task_id UNIQUEIDENTIFIER NULL,
    trigger_source VARCHAR(64) NOT NULL,
    operator_id UNIQUEIDENTIFIER NULL,
    started_at DATETIME2 NULL,
    finished_at DATETIME2 NULL,
    success_count INT NOT NULL DEFAULT 0,
    failure_count INT NOT NULL DEFAULT 0,
    diff_count INT NOT NULL DEFAULT 0,
    failure_reason NVARCHAR(1000) NULL,
    created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT fk_org_sync_task_source
        FOREIGN KEY (source_id) REFERENCES dbo.org_sync_source_config (id),
    CONSTRAINT fk_org_sync_task_retry
        FOREIGN KEY (retry_of_task_id) REFERENCES dbo.org_sync_task (id)
);

CREATE INDEX idx_org_sync_task_tenant_source_status
    ON dbo.org_sync_task (tenant_id, source_id, status);

CREATE INDEX idx_org_sync_task_tenant_created
    ON dbo.org_sync_task (tenant_id, created_at);

CREATE TABLE dbo.org_sync_diff_record (
    id UNIQUEIDENTIFIER NOT NULL PRIMARY KEY,
    tenant_id UNIQUEIDENTIFIER NOT NULL,
    task_id UNIQUEIDENTIFIER NOT NULL,
    entity_type VARCHAR(64) NOT NULL,
    entity_key VARCHAR(128) NOT NULL,
    diff_type VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    source_snapshot NVARCHAR(MAX) NULL,
    local_snapshot NVARCHAR(MAX) NULL,
    suggestion NVARCHAR(1000) NULL,
    resolved_by UNIQUEIDENTIFIER NULL,
    resolved_at DATETIME2 NULL,
    created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT fk_org_sync_diff_task
        FOREIGN KEY (task_id) REFERENCES dbo.org_sync_task (id)
);

CREATE INDEX idx_org_sync_diff_tenant_task_status
    ON dbo.org_sync_diff_record (tenant_id, task_id, status);

CREATE INDEX idx_org_sync_diff_tenant_entity
    ON dbo.org_sync_diff_record (tenant_id, entity_type, entity_key);

CREATE TABLE dbo.org_sync_conflict_record (
    id UNIQUEIDENTIFIER NOT NULL PRIMARY KEY,
    tenant_id UNIQUEIDENTIFIER NOT NULL,
    diff_record_id UNIQUEIDENTIFIER NOT NULL,
    conflict_field VARCHAR(128) NOT NULL,
    source_value NVARCHAR(MAX) NULL,
    local_value NVARCHAR(MAX) NULL,
    severity VARCHAR(32) NOT NULL,
    created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT fk_org_sync_conflict_diff
        FOREIGN KEY (diff_record_id) REFERENCES dbo.org_sync_diff_record (id)
);

CREATE INDEX idx_org_sync_conflict_tenant_diff
    ON dbo.org_sync_conflict_record (tenant_id, diff_record_id);

CREATE TABLE dbo.org_sync_compensation_record (
    id UNIQUEIDENTIFIER NOT NULL PRIMARY KEY,
    tenant_id UNIQUEIDENTIFIER NOT NULL,
    task_id UNIQUEIDENTIFIER NOT NULL,
    diff_record_id UNIQUEIDENTIFIER NOT NULL,
    action_type VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    request_payload NVARCHAR(MAX) NULL,
    result_payload NVARCHAR(MAX) NULL,
    operator_id UNIQUEIDENTIFIER NULL,
    created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT fk_org_sync_compensation_task
        FOREIGN KEY (task_id) REFERENCES dbo.org_sync_task (id),
    CONSTRAINT fk_org_sync_compensation_diff
        FOREIGN KEY (diff_record_id) REFERENCES dbo.org_sync_diff_record (id)
);

CREATE INDEX idx_org_sync_compensation_tenant_task
    ON dbo.org_sync_compensation_record (tenant_id, task_id);

CREATE INDEX idx_org_sync_compensation_tenant_diff
    ON dbo.org_sync_compensation_record (tenant_id, diff_record_id);

CREATE TABLE dbo.org_audit_record (
    id UNIQUEIDENTIFIER NOT NULL PRIMARY KEY,
    tenant_id UNIQUEIDENTIFIER NOT NULL,
    category VARCHAR(64) NOT NULL,
    action_type VARCHAR(64) NOT NULL,
    entity_type VARCHAR(64) NOT NULL,
    entity_id VARCHAR(128) NULL,
    task_id UNIQUEIDENTIFIER NULL,
    trigger_source VARCHAR(64) NOT NULL,
    operator_id UNIQUEIDENTIFIER NULL,
    before_snapshot NVARCHAR(MAX) NULL,
    after_snapshot NVARCHAR(MAX) NULL,
    summary NVARCHAR(1000) NULL,
    occurred_at DATETIME2 NOT NULL,
    created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME()
);

CREATE INDEX idx_org_audit_record_tenant_entity
    ON dbo.org_audit_record (tenant_id, entity_type, entity_id);

CREATE INDEX idx_org_audit_record_tenant_task
    ON dbo.org_audit_record (tenant_id, task_id);

CREATE INDEX idx_org_audit_record_tenant_occurred
    ON dbo.org_audit_record (tenant_id, occurred_at);
