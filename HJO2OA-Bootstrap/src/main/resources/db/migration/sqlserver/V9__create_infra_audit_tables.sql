-- ============================================================
-- V9: Infrastructure audit module tables
-- Entity source: AuditRecordEntity, AuditFieldChangeEntity
-- PK type: UNIQUEIDENTIFIER (UUID id)
-- ============================================================

CREATE TABLE dbo.infra_audit_record (
    id                   UNIQUEIDENTIFIER NOT NULL PRIMARY KEY,
    module_code          NVARCHAR(64)     NOT NULL,
    object_type          NVARCHAR(64)     NOT NULL,
    object_id            NVARCHAR(64)     NOT NULL,
    action_type          NVARCHAR(64)     NOT NULL,
    operator_account_id  UNIQUEIDENTIFIER NULL,
    operator_person_id   UNIQUEIDENTIFIER NULL,
    tenant_id            UNIQUEIDENTIFIER NULL,
    trace_id             NVARCHAR(128)    NULL,
    summary              NVARCHAR(512)    NULL,
    occurred_at          DATETIME2(6)     NOT NULL CONSTRAINT DF_infra_audit_record_occurred_at DEFAULT (SYSUTCDATETIME()),
    archive_status       NVARCHAR(32)     NOT NULL CONSTRAINT DF_infra_audit_record_archive_status DEFAULT ('ACTIVE'),
    created_at           DATETIME2(6)     NOT NULL CONSTRAINT DF_infra_audit_record_created_at DEFAULT (SYSUTCDATETIME())
);

CREATE INDEX IX_infra_audit_record_tenant_occurred_at
    ON dbo.infra_audit_record (tenant_id, occurred_at DESC);
CREATE INDEX IX_infra_audit_record_module_object_occurred_at
    ON dbo.infra_audit_record (module_code, object_type, object_id, occurred_at DESC);
CREATE INDEX IX_infra_audit_record_action_occurred_at
    ON dbo.infra_audit_record (action_type, occurred_at DESC);

CREATE TABLE dbo.infra_audit_field_change (
    id                UNIQUEIDENTIFIER NOT NULL PRIMARY KEY,
    audit_record_id   UNIQUEIDENTIFIER NOT NULL,
    field_name        NVARCHAR(128)    NOT NULL,
    old_value         NVARCHAR(MAX)    NULL,
    new_value         NVARCHAR(MAX)    NULL,
    sensitivity_level NVARCHAR(32)     NULL,
    CONSTRAINT FK_infra_audit_field_change_record
        FOREIGN KEY (audit_record_id) REFERENCES dbo.infra_audit_record(id)
);

CREATE INDEX IX_infra_audit_field_change_record_field
    ON dbo.infra_audit_field_change (audit_record_id, field_name);
