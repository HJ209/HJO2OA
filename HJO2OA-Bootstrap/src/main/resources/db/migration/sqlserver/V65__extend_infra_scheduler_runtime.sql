IF COL_LENGTH('dbo.infra_scheduled_job', 'handler_name') IS NULL
BEGIN
    ALTER TABLE dbo.infra_scheduled_job ADD handler_name NVARCHAR(128) NULL;
    EXEC('UPDATE dbo.infra_scheduled_job SET handler_name = job_code WHERE handler_name IS NULL');
    ALTER TABLE dbo.infra_scheduled_job ALTER COLUMN handler_name NVARCHAR(128) NOT NULL;
END;

IF COL_LENGTH('dbo.infra_job_execution_record', 'parent_execution_id') IS NULL
BEGIN
    ALTER TABLE dbo.infra_job_execution_record ADD parent_execution_id UNIQUEIDENTIFIER NULL;
END;

IF COL_LENGTH('dbo.infra_job_execution_record', 'attempt_no') IS NULL
BEGIN
    ALTER TABLE dbo.infra_job_execution_record
        ADD attempt_no INT NOT NULL CONSTRAINT DF_infra_job_execution_record_attempt_no DEFAULT (1);
END;

IF COL_LENGTH('dbo.infra_job_execution_record', 'max_attempts') IS NULL
BEGIN
    ALTER TABLE dbo.infra_job_execution_record
        ADD max_attempts INT NOT NULL CONSTRAINT DF_infra_job_execution_record_max_attempts DEFAULT (1);
END;

IF COL_LENGTH('dbo.infra_job_execution_record', 'duration_ms') IS NULL
BEGIN
    ALTER TABLE dbo.infra_job_execution_record ADD duration_ms BIGINT NULL;
END;

IF COL_LENGTH('dbo.infra_job_execution_record', 'error_stack') IS NULL
BEGIN
    ALTER TABLE dbo.infra_job_execution_record ADD error_stack NVARCHAR(MAX) NULL;
END;

IF COL_LENGTH('dbo.infra_job_execution_record', 'trigger_context') IS NULL
BEGIN
    ALTER TABLE dbo.infra_job_execution_record ADD trigger_context NVARCHAR(MAX) NULL;
END;

IF COL_LENGTH('dbo.infra_job_execution_record', 'idempotency_key') IS NULL
BEGIN
    ALTER TABLE dbo.infra_job_execution_record ADD idempotency_key NVARCHAR(128) NULL;
END;

IF COL_LENGTH('dbo.infra_job_execution_record', 'next_retry_at') IS NULL
BEGIN
    ALTER TABLE dbo.infra_job_execution_record ADD next_retry_at DATETIME2 NULL;
END;

IF NOT EXISTS (
    SELECT 1 FROM sys.indexes
    WHERE name = 'IX_infra_scheduled_job_status_trigger'
      AND object_id = OBJECT_ID('dbo.infra_scheduled_job')
)
BEGIN
    EXEC(N'CREATE INDEX IX_infra_scheduled_job_status_trigger
        ON dbo.infra_scheduled_job(status, trigger_type, tenant_id)');
END;

IF NOT EXISTS (
    SELECT 1 FROM sys.indexes
    WHERE name = 'IX_infra_job_execution_parent'
      AND object_id = OBJECT_ID('dbo.infra_job_execution_record')
)
BEGIN
    EXEC(N'CREATE INDEX IX_infra_job_execution_parent
        ON dbo.infra_job_execution_record(parent_execution_id)');
END;

IF NOT EXISTS (
    SELECT 1 FROM sys.indexes
    WHERE name = 'IX_infra_job_execution_status_retry'
      AND object_id = OBJECT_ID('dbo.infra_job_execution_record')
)
BEGIN
    EXEC(N'CREATE INDEX IX_infra_job_execution_status_retry
        ON dbo.infra_job_execution_record(execution_status, next_retry_at)');
END;

IF NOT EXISTS (
    SELECT 1 FROM sys.indexes
    WHERE name = 'UX_infra_job_execution_idempotency'
      AND object_id = OBJECT_ID('dbo.infra_job_execution_record')
)
BEGIN
    EXEC(N'CREATE UNIQUE INDEX UX_infra_job_execution_idempotency
        ON dbo.infra_job_execution_record(scheduled_job_id, idempotency_key)
        WHERE idempotency_key IS NOT NULL');
END;
