-- ============================================================
-- V16: Infrastructure timezone tables
-- Entity source: TimezoneSettingEntity
-- PK type: UNIQUEIDENTIFIER
-- ============================================================

CREATE TABLE dbo.infra_timezone_setting (
    id             UNIQUEIDENTIFIER NOT NULL PRIMARY KEY,
    scope_type     NVARCHAR(32)     NOT NULL,
    scope_id       UNIQUEIDENTIFIER NULL,
    timezone_id    NVARCHAR(64)     NOT NULL,
    is_default     BIT              NOT NULL CONSTRAINT DF_infra_timezone_setting_is_default DEFAULT ((0)),
    effective_from DATETIME2(3)     NULL,
    active         BIT              NOT NULL CONSTRAINT DF_infra_timezone_setting_active DEFAULT ((1)),
    tenant_id      UNIQUEIDENTIFIER NULL,
    created_at     DATETIME2(3)     NOT NULL CONSTRAINT DF_infra_timezone_setting_created_at DEFAULT (SYSUTCDATETIME()),
    updated_at     DATETIME2(3)     NOT NULL CONSTRAINT DF_infra_timezone_setting_updated_at DEFAULT (SYSUTCDATETIME())
);

CREATE UNIQUE INDEX UX_infra_timezone_setting_scope
    ON dbo.infra_timezone_setting (scope_type, scope_id);

CREATE INDEX IX_infra_timezone_setting_scope_active_effective
    ON dbo.infra_timezone_setting (scope_type, scope_id, active, effective_from DESC);

CREATE INDEX IX_infra_timezone_setting_tenant_active_effective
    ON dbo.infra_timezone_setting (tenant_id, active, effective_from DESC);
