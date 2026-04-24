-- ============================================================
-- V8: Tenant infrastructure tables
-- Entity source: TenantProfileEntity, TenantQuotaEntity
-- PK type: UNIQUEIDENTIFIER (domain UUID)
-- ============================================================

CREATE TABLE dbo.infra_tenant_profile (
    id                UNIQUEIDENTIFIER NOT NULL PRIMARY KEY,
    tenant_code       NVARCHAR(64)     NOT NULL,
    name              NVARCHAR(128)    NOT NULL,
    status            NVARCHAR(32)     NOT NULL,
    isolation_mode    NVARCHAR(32)     NOT NULL,
    package_code      NVARCHAR(64)     NULL,
    default_locale    NVARCHAR(16)     NULL,
    default_timezone  NVARCHAR(64)     NULL,
    initialized       BIT              NOT NULL CONSTRAINT DF_infra_tenant_profile_initialized DEFAULT ((0)),
    created_at        DATETIME2(3)     NOT NULL CONSTRAINT DF_infra_tenant_profile_created_at DEFAULT (SYSUTCDATETIME()),
    updated_at        DATETIME2(3)     NOT NULL CONSTRAINT DF_infra_tenant_profile_updated_at DEFAULT (SYSUTCDATETIME())
);

CREATE UNIQUE INDEX UX_infra_tenant_profile_tenant_code
    ON dbo.infra_tenant_profile (tenant_code);
CREATE INDEX IX_infra_tenant_profile_status_initialized
    ON dbo.infra_tenant_profile (status, initialized);

CREATE TABLE dbo.infra_tenant_quota (
    id                 UNIQUEIDENTIFIER NOT NULL PRIMARY KEY,
    tenant_profile_id  UNIQUEIDENTIFIER NOT NULL,
    quota_type         NVARCHAR(32)     NOT NULL,
    limit_value        BIGINT           NOT NULL,
    used_value         BIGINT           NOT NULL CONSTRAINT DF_infra_tenant_quota_used_value DEFAULT ((0)),
    warning_threshold  BIGINT           NULL,
    CONSTRAINT FK_infra_tenant_quota_profile
        FOREIGN KEY (tenant_profile_id) REFERENCES dbo.infra_tenant_profile(id)
);

CREATE UNIQUE INDEX UX_infra_tenant_quota_profile_type
    ON dbo.infra_tenant_quota (tenant_profile_id, quota_type);
CREATE INDEX IX_infra_tenant_quota_type_warning
    ON dbo.infra_tenant_quota (quota_type, warning_threshold);
