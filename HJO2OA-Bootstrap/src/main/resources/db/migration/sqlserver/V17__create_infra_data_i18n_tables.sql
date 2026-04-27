-- ============================================================
-- V17: Infrastructure data-i18n module tables
-- Entity source: TranslationEntryEntity
-- PK type: UNIQUEIDENTIFIER (UUID id)
-- ============================================================

CREATE TABLE dbo.infra_translation_entry (
    id                 UNIQUEIDENTIFIER NOT NULL PRIMARY KEY,
    entity_type        NVARCHAR(64)     NOT NULL,
    entity_id          NVARCHAR(64)     NOT NULL,
    field_name         NVARCHAR(128)    NOT NULL,
    locale             NVARCHAR(16)     NOT NULL,
    translated_value   NVARCHAR(MAX)    NOT NULL,
    translation_status NVARCHAR(32)     NOT NULL CONSTRAINT DF_infra_translation_entry_translation_status DEFAULT ('DRAFT'),
    tenant_id          UNIQUEIDENTIFIER NULL,
    updated_by         UNIQUEIDENTIFIER NULL,
    updated_at         DATETIME2(6)     NOT NULL CONSTRAINT DF_infra_translation_entry_updated_at DEFAULT (SYSUTCDATETIME())
);

CREATE UNIQUE INDEX IX_infra_translation_entry_entity_field_locale
    ON dbo.infra_translation_entry (entity_type, entity_id, field_name, locale);
CREATE INDEX IX_infra_translation_entry_tenant_locale
    ON dbo.infra_translation_entry (tenant_id, locale);
