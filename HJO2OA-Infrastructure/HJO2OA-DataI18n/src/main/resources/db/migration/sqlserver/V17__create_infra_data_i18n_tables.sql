CREATE TABLE infra_translation_entry (
    id UNIQUEIDENTIFIER NOT NULL PRIMARY KEY,
    entity_type NVARCHAR(64) NOT NULL,
    entity_id NVARCHAR(64) NOT NULL,
    field_name NVARCHAR(128) NOT NULL,
    locale NVARCHAR(16) NOT NULL,
    translated_value NVARCHAR(MAX) NOT NULL,
    translation_status NVARCHAR(32) NOT NULL,
    tenant_id UNIQUEIDENTIFIER NOT NULL,
    updated_by UNIQUEIDENTIFIER NULL,
    updated_at DATETIME2 NOT NULL,
    CONSTRAINT uk_infra_translation_entry UNIQUE (entity_type, entity_id, field_name, locale)
);

CREATE INDEX idx_infra_translation_entry_entity
    ON infra_translation_entry (entity_type, entity_id, field_name);

CREATE INDEX idx_infra_translation_entry_locale
    ON infra_translation_entry (locale, translation_status);
