-- V77: isolate data-i18n translations by tenant.

UPDATE dbo.infra_translation_entry
SET tenant_id = '00000000-0000-0000-0000-000000000000'
WHERE tenant_id IS NULL;

IF EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = 'IX_infra_translation_entry_entity_field_locale'
      AND object_id = OBJECT_ID('dbo.infra_translation_entry')
)
BEGIN
    DROP INDEX IX_infra_translation_entry_entity_field_locale
        ON dbo.infra_translation_entry;
END;

IF EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = 'IX_infra_translation_entry_tenant_locale'
      AND object_id = OBJECT_ID('dbo.infra_translation_entry')
)
BEGIN
    DROP INDEX IX_infra_translation_entry_tenant_locale
        ON dbo.infra_translation_entry;
END;

ALTER TABLE dbo.infra_translation_entry
    ALTER COLUMN tenant_id UNIQUEIDENTIFIER NOT NULL;

IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = 'UX_infra_translation_entry_tenant_entity_field_locale'
      AND object_id = OBJECT_ID('dbo.infra_translation_entry')
)
BEGIN
    CREATE UNIQUE INDEX UX_infra_translation_entry_tenant_entity_field_locale
        ON dbo.infra_translation_entry (tenant_id, entity_type, entity_id, field_name, locale);
END;

IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = 'IX_infra_translation_entry_tenant_locale'
      AND object_id = OBJECT_ID('dbo.infra_translation_entry')
)
BEGIN
    CREATE INDEX IX_infra_translation_entry_tenant_locale
        ON dbo.infra_translation_entry (tenant_id, locale);
END;
