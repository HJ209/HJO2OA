CREATE TABLE dbo.org_data_permission (
    id UNIQUEIDENTIFIER NOT NULL PRIMARY KEY,
    subject_type VARCHAR(32) NOT NULL,
    subject_id UNIQUEIDENTIFIER NOT NULL,
    business_object VARCHAR(128) NOT NULL,
    scope_type VARCHAR(32) NOT NULL,
    condition_expr NVARCHAR(1024) NULL,
    effect VARCHAR(16) NOT NULL DEFAULT 'ALLOW',
    priority INT NOT NULL DEFAULT 0,
    tenant_id UNIQUEIDENTIFIER NULL,
    created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME()
);

CREATE INDEX idx_org_data_permission_business_subject
    ON dbo.org_data_permission (business_object, subject_type, subject_id);

CREATE INDEX idx_org_data_permission_tenant_subject
    ON dbo.org_data_permission (tenant_id, subject_type, subject_id);

CREATE INDEX idx_org_data_permission_scope_effect
    ON dbo.org_data_permission (business_object, scope_type, effect, priority);

CREATE TABLE dbo.org_field_permission (
    id UNIQUEIDENTIFIER NOT NULL PRIMARY KEY,
    subject_type VARCHAR(32) NOT NULL,
    subject_id UNIQUEIDENTIFIER NOT NULL,
    business_object VARCHAR(128) NOT NULL,
    usage_scenario VARCHAR(64) NOT NULL,
    field_code VARCHAR(128) NOT NULL,
    action VARCHAR(32) NOT NULL,
    effect VARCHAR(16) NOT NULL DEFAULT 'ALLOW',
    tenant_id UNIQUEIDENTIFIER NULL,
    created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME()
);

CREATE UNIQUE INDEX uk_org_field_permission_global
    ON dbo.org_field_permission (
        subject_type,
        subject_id,
        business_object,
        usage_scenario,
        field_code,
        action
    )
    WHERE tenant_id IS NULL;

CREATE UNIQUE INDEX uk_org_field_permission_tenant
    ON dbo.org_field_permission (
        subject_type,
        subject_id,
        business_object,
        usage_scenario,
        field_code,
        action,
        tenant_id
    )
    WHERE tenant_id IS NOT NULL;

CREATE INDEX idx_org_field_permission_business_field_subject
    ON dbo.org_field_permission (business_object, usage_scenario, field_code, subject_type, subject_id);
