-- ============================================================
-- V13: Infrastructure config center tables
-- Entity source: ConfigEntryEntity, ConfigOverrideEntity, FeatureRuleEntity
-- PK type: UNIQUEIDENTIFIER
-- ============================================================

CREATE TABLE dbo.infra_config_entry (
    id                  UNIQUEIDENTIFIER NOT NULL PRIMARY KEY,
    config_key          NVARCHAR(128)    NOT NULL,
    name                NVARCHAR(128)    NOT NULL,
    config_type         NVARCHAR(32)     NOT NULL,
    default_value       NVARCHAR(MAX)    NOT NULL,
    validation_rule     NVARCHAR(MAX)    NULL,
    mutable_at_runtime  BIT              NOT NULL CONSTRAINT DF_infra_config_entry_mutable DEFAULT ((1)),
    status              NVARCHAR(32)     NOT NULL,
    tenant_aware        BIT              NOT NULL CONSTRAINT DF_infra_config_entry_tenant_aware DEFAULT ((0)),
    created_at          DATETIME2(6)     NOT NULL CONSTRAINT DF_infra_config_entry_created_at DEFAULT (SYSUTCDATETIME()),
    updated_at          DATETIME2(6)     NOT NULL CONSTRAINT DF_infra_config_entry_updated_at DEFAULT (SYSUTCDATETIME())
);

CREATE UNIQUE INDEX UX_infra_config_entry_config_key
    ON dbo.infra_config_entry (config_key);
CREATE INDEX IX_infra_config_entry_status_type
    ON dbo.infra_config_entry (status, config_type);

CREATE TABLE dbo.infra_config_override (
    id               UNIQUEIDENTIFIER NOT NULL PRIMARY KEY,
    config_entry_id  UNIQUEIDENTIFIER NOT NULL,
    scope_type       NVARCHAR(32)     NOT NULL,
    scope_id         UNIQUEIDENTIFIER NOT NULL,
    override_value   NVARCHAR(MAX)    NOT NULL,
    active           BIT              NOT NULL CONSTRAINT DF_infra_config_override_active DEFAULT ((1)),
    CONSTRAINT FK_infra_config_override_entry
        FOREIGN KEY (config_entry_id) REFERENCES dbo.infra_config_entry(id)
);

CREATE UNIQUE INDEX UX_infra_config_override_scope
    ON dbo.infra_config_override (config_entry_id, scope_type, scope_id);
CREATE INDEX IX_infra_config_override_scope_type
    ON dbo.infra_config_override (scope_type, scope_id);

CREATE TABLE dbo.infra_feature_rule (
    id               UNIQUEIDENTIFIER NOT NULL PRIMARY KEY,
    config_entry_id  UNIQUEIDENTIFIER NOT NULL,
    rule_type        NVARCHAR(32)     NOT NULL,
    rule_value       NVARCHAR(MAX)    NOT NULL,
    sort_order       INT              NOT NULL CONSTRAINT DF_infra_feature_rule_sort_order DEFAULT ((0)),
    active           BIT              NOT NULL CONSTRAINT DF_infra_feature_rule_active DEFAULT ((1)),
    CONSTRAINT FK_infra_feature_rule_entry
        FOREIGN KEY (config_entry_id) REFERENCES dbo.infra_config_entry(id)
);

CREATE UNIQUE INDEX UX_infra_feature_rule_sort_order
    ON dbo.infra_feature_rule (config_entry_id, sort_order);
CREATE INDEX IX_infra_feature_rule_type
    ON dbo.infra_feature_rule (rule_type, sort_order);
