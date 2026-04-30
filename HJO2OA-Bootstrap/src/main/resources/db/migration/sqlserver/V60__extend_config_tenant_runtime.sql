-- ============================================================
-- V60: Config + tenant runtime extensions
-- Window 03 scope: tenant admins, extensible quotas, runtime governance metadata
-- ============================================================

IF COL_LENGTH('dbo.infra_tenant_profile', 'admin_account_id') IS NULL
BEGIN
    ALTER TABLE dbo.infra_tenant_profile ADD admin_account_id UNIQUEIDENTIFIER NULL;
END;

IF COL_LENGTH('dbo.infra_tenant_profile', 'admin_person_id') IS NULL
BEGIN
    ALTER TABLE dbo.infra_tenant_profile ADD admin_person_id UNIQUEIDENTIFIER NULL;
END;

IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = 'IX_infra_tenant_profile_admin_account'
      AND object_id = OBJECT_ID('dbo.infra_tenant_profile')
)
BEGIN
    EXEC(N'CREATE INDEX IX_infra_tenant_profile_admin_account
        ON dbo.infra_tenant_profile (admin_account_id)');
END;

IF COL_LENGTH('dbo.infra_config_override', 'created_at') IS NULL
BEGIN
    ALTER TABLE dbo.infra_config_override
        ADD created_at DATETIME2(6) NOT NULL
            CONSTRAINT DF_infra_config_override_created_at DEFAULT (SYSUTCDATETIME());
END;

IF COL_LENGTH('dbo.infra_config_override', 'updated_at') IS NULL
BEGIN
    ALTER TABLE dbo.infra_config_override
        ADD updated_at DATETIME2(6) NOT NULL
            CONSTRAINT DF_infra_config_override_updated_at DEFAULT (SYSUTCDATETIME());
END;

IF COL_LENGTH('dbo.infra_feature_rule', 'created_at') IS NULL
BEGIN
    ALTER TABLE dbo.infra_feature_rule
        ADD created_at DATETIME2(6) NOT NULL
            CONSTRAINT DF_infra_feature_rule_created_at DEFAULT (SYSUTCDATETIME());
END;

IF COL_LENGTH('dbo.infra_feature_rule', 'updated_at') IS NULL
BEGIN
    ALTER TABLE dbo.infra_feature_rule
        ADD updated_at DATETIME2(6) NOT NULL
            CONSTRAINT DF_infra_feature_rule_updated_at DEFAULT (SYSUTCDATETIME());
END;

IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = 'IX_infra_feature_rule_entry_active_order'
      AND object_id = OBJECT_ID('dbo.infra_feature_rule')
)
BEGIN
    EXEC(N'CREATE INDEX IX_infra_feature_rule_entry_active_order
        ON dbo.infra_feature_rule (config_entry_id, active, sort_order)');
END;
