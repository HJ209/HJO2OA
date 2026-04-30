-- ============================================================
-- V116: Reliable EventBus outbox delivery, replay, idempotency and audit
-- Extends V34 event_outbox without rewriting the existing table.
-- ============================================================

ALTER TABLE dbo.event_outbox
    ADD event_id UNIQUEIDENTIFIER NULL,
        tenant_id NVARCHAR(64) NULL,
        occurred_at DATETIME2(6) NULL,
        trace_id NVARCHAR(128) NULL,
        schema_version NVARCHAR(16) NOT NULL
            CONSTRAINT DF_event_outbox_schema_version DEFAULT ('1'),
        next_retry_at DATETIME2(6) NULL,
        last_error NVARCHAR(MAX) NULL,
        dead_at DATETIME2(6) NULL;

EXEC(N'UPDATE dbo.event_outbox
   SET event_id = id
 WHERE event_id IS NULL');

EXEC(N'UPDATE dbo.event_outbox
   SET occurred_at = created_at
 WHERE occurred_at IS NULL');

EXEC(N'ALTER TABLE dbo.event_outbox
    ALTER COLUMN event_id UNIQUEIDENTIFIER NOT NULL');

EXEC(N'ALTER TABLE dbo.event_outbox
    ALTER COLUMN occurred_at DATETIME2(6) NOT NULL');

EXEC(N'CREATE UNIQUE INDEX UX_event_outbox_event_id
    ON dbo.event_outbox (event_id)');

EXEC(N'CREATE INDEX IX_event_outbox_status_retry
    ON dbo.event_outbox (status, next_retry_at, created_at)');

EXEC(N'CREATE INDEX IX_event_outbox_tenant_status_created
    ON dbo.event_outbox (tenant_id, status, created_at DESC)');

EXEC(N'CREATE INDEX IX_event_outbox_trace
    ON dbo.event_outbox (trace_id)');

CREATE TABLE dbo.consumed_event (
    id              UNIQUEIDENTIFIER NOT NULL PRIMARY KEY,
    event_id        UNIQUEIDENTIFIER NOT NULL,
    event_type      NVARCHAR(128)    NOT NULL,
    consumer_code   NVARCHAR(128)    NOT NULL,
    tenant_id       NVARCHAR(64)     NULL,
    trace_id        NVARCHAR(128)    NULL,
    status          NVARCHAR(32)     NOT NULL,
    last_error      NVARCHAR(MAX)    NULL,
    consumed_at     DATETIME2(6)     NULL,
    created_at      DATETIME2(6)     NOT NULL CONSTRAINT DF_consumed_event_created_at DEFAULT (SYSUTCDATETIME()),
    updated_at      DATETIME2(6)     NOT NULL CONSTRAINT DF_consumed_event_updated_at DEFAULT (SYSUTCDATETIME())
);

CREATE UNIQUE INDEX UX_consumed_event_event_consumer
    ON dbo.consumed_event (event_id, consumer_code);

CREATE INDEX IX_consumed_event_consumer_status
    ON dbo.consumed_event (consumer_code, status, updated_at DESC);

CREATE INDEX IX_consumed_event_tenant_type
    ON dbo.consumed_event (tenant_id, event_type, consumed_at DESC);

CREATE TABLE dbo.infra_event_operation_audit (
    id                    UNIQUEIDENTIFIER NOT NULL PRIMARY KEY,
    event_id              UNIQUEIDENTIFIER NULL,
    operation_type        NVARCHAR(32)     NOT NULL,
    operator_account_id   UNIQUEIDENTIFIER NULL,
    operator_person_id    UNIQUEIDENTIFIER NULL,
    tenant_id             NVARCHAR(64)     NULL,
    trace_id              NVARCHAR(128)    NULL,
    request_id            NVARCHAR(128)    NULL,
    idempotency_key       NVARCHAR(128)    NULL,
    reason                NVARCHAR(512)    NOT NULL,
    detail_json           NVARCHAR(MAX)    NULL,
    created_at            DATETIME2(6)     NOT NULL CONSTRAINT DF_infra_event_operation_audit_created_at DEFAULT (SYSUTCDATETIME())
);

CREATE INDEX IX_infra_event_operation_audit_event
    ON dbo.infra_event_operation_audit (event_id, created_at DESC);

CREATE INDEX IX_infra_event_operation_audit_operation
    ON dbo.infra_event_operation_audit (operation_type, created_at DESC);

CREATE INDEX IX_infra_event_operation_audit_tenant
    ON dbo.infra_event_operation_audit (tenant_id, created_at DESC);

CREATE UNIQUE INDEX UX_infra_event_operation_audit_idempotency
    ON dbo.infra_event_operation_audit (idempotency_key)
    WHERE idempotency_key IS NOT NULL;
