-- ============================================================
-- V10: Infrastructure dictionary tables
-- Entity source: DictionaryType / DictionaryItem
-- PK type: UNIQUEIDENTIFIER
-- ============================================================

CREATE TABLE dbo.infra_dictionary_type (
    dictionary_type_id UNIQUEIDENTIFIER NOT NULL PRIMARY KEY,
    code               NVARCHAR(64)     NOT NULL,
    name               NVARCHAR(128)    NOT NULL,
    category           NVARCHAR(64)     NULL,
    hierarchical       BIT              NOT NULL,
    cacheable          BIT              NOT NULL,
    status             NVARCHAR(32)     NOT NULL,
    tenant_id          UNIQUEIDENTIFIER NULL,
    created_at         DATETIME2(6)     NOT NULL,
    updated_at         DATETIME2(6)     NOT NULL
);

CREATE UNIQUE INDEX UX_infra_dictionary_type_scope_code
    ON dbo.infra_dictionary_type (tenant_id, code);
CREATE INDEX IX_infra_dictionary_type_status
    ON dbo.infra_dictionary_type (status, tenant_id, code);

CREATE TABLE dbo.infra_dictionary_item (
    item_id             UNIQUEIDENTIFIER NOT NULL PRIMARY KEY,
    dictionary_type_id  UNIQUEIDENTIFIER NOT NULL,
    item_code           NVARCHAR(64)     NOT NULL,
    display_name        NVARCHAR(128)    NOT NULL,
    parent_item_id      UNIQUEIDENTIFIER NULL,
    sort_order          INT              NOT NULL CONSTRAINT DF_infra_dictionary_item_sort_order DEFAULT 0,
    enabled             BIT              NOT NULL CONSTRAINT DF_infra_dictionary_item_enabled DEFAULT 1,
    multi_lang_value    NVARCHAR(MAX)    NULL,
    CONSTRAINT FK_infra_dictionary_item_type
        FOREIGN KEY (dictionary_type_id) REFERENCES dbo.infra_dictionary_type(dictionary_type_id),
    CONSTRAINT FK_infra_dictionary_item_parent
        FOREIGN KEY (parent_item_id) REFERENCES dbo.infra_dictionary_item(item_id)
);

CREATE UNIQUE INDEX UX_infra_dictionary_item_type_code
    ON dbo.infra_dictionary_item (dictionary_type_id, item_code);
CREATE INDEX IX_infra_dictionary_item_parent
    ON dbo.infra_dictionary_item (dictionary_type_id, parent_item_id, sort_order, item_code);
