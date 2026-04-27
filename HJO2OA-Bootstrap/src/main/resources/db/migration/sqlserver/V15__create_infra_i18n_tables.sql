-- ============================================================
-- V15: Infrastructure i18n module tables
-- Entity source: LocaleBundleEntity, LocaleResourceEntryEntity
-- PK type: UNIQUEIDENTIFIER (UUID id)
-- ============================================================

CREATE TABLE dbo.infra_locale_bundle (
    id               UNIQUEIDENTIFIER NOT NULL PRIMARY KEY,
    bundle_code      NVARCHAR(128)    NOT NULL,
    module_code      NVARCHAR(64)     NOT NULL,
    locale           NVARCHAR(16)     NOT NULL,
    fallback_locale  NVARCHAR(16)     NULL,
    status           NVARCHAR(32)     NOT NULL CONSTRAINT DF_infra_locale_bundle_status DEFAULT ('DRAFT'),
    tenant_id        UNIQUEIDENTIFIER NULL,
    created_at       DATETIME2(6)     NOT NULL CONSTRAINT DF_infra_locale_bundle_created_at DEFAULT (SYSUTCDATETIME()),
    updated_at       DATETIME2(6)     NOT NULL CONSTRAINT DF_infra_locale_bundle_updated_at DEFAULT (SYSUTCDATETIME())
);

CREATE UNIQUE INDEX IX_infra_locale_bundle_code_locale
    ON dbo.infra_locale_bundle (bundle_code, locale);
CREATE INDEX IX_infra_locale_bundle_module_status
    ON dbo.infra_locale_bundle (module_code, status);

CREATE TABLE dbo.infra_locale_resource_entry (
    id                 UNIQUEIDENTIFIER NOT NULL PRIMARY KEY,
    locale_bundle_id   UNIQUEIDENTIFIER NOT NULL,
    resource_key       NVARCHAR(256)    NOT NULL,
    resource_value     NVARCHAR(MAX)    NOT NULL,
    version            INT              NOT NULL CONSTRAINT DF_infra_locale_resource_entry_version DEFAULT (1),
    active             BIT              NOT NULL CONSTRAINT DF_infra_locale_resource_entry_active DEFAULT (1),
    CONSTRAINT FK_infra_locale_resource_entry_bundle
        FOREIGN KEY (locale_bundle_id) REFERENCES dbo.infra_locale_bundle(id)
);

CREATE INDEX IX_infra_locale_resource_entry_bundle_key
    ON dbo.infra_locale_resource_entry (locale_bundle_id, resource_key);
