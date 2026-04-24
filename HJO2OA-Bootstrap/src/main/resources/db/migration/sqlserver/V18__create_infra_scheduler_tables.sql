CREATE TABLE infra_scheduled_job (
    id UNIQUEIDENTIFIER NOT NULL PRIMARY KEY,
    job_code NVARCHAR(128) NOT NULL,
    name NVARCHAR(256) NOT NULL,
    trigger_type NVARCHAR(32) NOT NULL,
    cron_expr NVARCHAR(128) NULL,
    timezone_id NVARCHAR(64) NULL,
    concurrency_policy NVARCHAR(32) NOT NULL,
    timeout_seconds INT NULL,
    retry_policy NVARCHAR(MAX) NULL,
    status NVARCHAR(32) NOT NULL,
    tenant_id UNIQUEIDENTIFIER NULL,
    created_at DATETIME2 NOT NULL,
    updated_at DATETIME2 NOT NULL
);

CREATE UNIQUE INDEX uk_infra_scheduled_job_job_code
    ON infra_scheduled_job(job_code);

CREATE INDEX idx_infra_scheduled_job_tenant_status
    ON infra_scheduled_job(tenant_id, status);

CREATE INDEX idx_infra_scheduled_job_trigger_type
    ON infra_scheduled_job(trigger_type);

CREATE TABLE infra_job_execution_record (
    id UNIQUEIDENTIFIER NOT NULL PRIMARY KEY,
    scheduled_job_id UNIQUEIDENTIFIER NOT NULL,
    trigger_source NVARCHAR(32) NOT NULL,
    execution_status NVARCHAR(32) NOT NULL,
    started_at DATETIME2 NOT NULL,
    finished_at DATETIME2 NULL,
    error_code NVARCHAR(64) NULL,
    error_message NVARCHAR(512) NULL,
    execution_log NVARCHAR(MAX) NULL,
    CONSTRAINT fk_infra_job_execution_record_job
        FOREIGN KEY (scheduled_job_id) REFERENCES infra_scheduled_job(id)
);

CREATE INDEX idx_infra_job_execution_record_job_started_at
    ON infra_job_execution_record(scheduled_job_id, started_at);

CREATE INDEX idx_infra_job_execution_record_status_started_at
    ON infra_job_execution_record(execution_status, started_at);
