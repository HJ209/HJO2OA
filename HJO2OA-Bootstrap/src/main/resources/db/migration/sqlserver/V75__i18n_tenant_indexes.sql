-- V75: make i18n bundle uniqueness tenant-aware.

IF EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = 'IX_infra_locale_bundle_code_locale'
      AND object_id = OBJECT_ID('dbo.infra_locale_bundle')
)
BEGIN
    DROP INDEX IX_infra_locale_bundle_code_locale
        ON dbo.infra_locale_bundle;
END;

IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = 'UX_infra_locale_bundle_code_locale_tenant'
      AND object_id = OBJECT_ID('dbo.infra_locale_bundle')
)
BEGIN
    CREATE UNIQUE INDEX UX_infra_locale_bundle_code_locale_tenant
        ON dbo.infra_locale_bundle (bundle_code, locale, tenant_id);
END;

IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = 'IX_infra_locale_bundle_locale_tenant'
      AND object_id = OBJECT_ID('dbo.infra_locale_bundle')
)
BEGIN
    CREATE INDEX IX_infra_locale_bundle_locale_tenant
        ON dbo.infra_locale_bundle (locale, tenant_id, status);
END;
