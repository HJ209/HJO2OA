CREATE TABLE dbo.org_resource (
    id UNIQUEIDENTIFIER NOT NULL PRIMARY KEY,
    resource_type VARCHAR(32) NOT NULL,
    resource_code VARCHAR(128) NOT NULL,
    name NVARCHAR(128) NOT NULL,
    parent_code VARCHAR(128) NULL,
    sort_order INT NOT NULL DEFAULT 0,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    tenant_id UNIQUEIDENTIFIER NOT NULL,
    created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT ck_org_resource_type
        CHECK (resource_type IN ('MENU', 'BUTTON', 'API', 'DATA_RESOURCE', 'PAGE', 'RESOURCE_ACTION')),
    CONSTRAINT ck_org_resource_status
        CHECK (status IN ('ACTIVE', 'DISABLED'))
);

CREATE UNIQUE INDEX uk_org_resource_tenant_type_code
    ON dbo.org_resource (tenant_id, resource_type, resource_code);

CREATE INDEX idx_org_resource_tenant_parent
    ON dbo.org_resource (tenant_id, parent_code, sort_order);

CREATE INDEX idx_org_resource_tenant_status
    ON dbo.org_resource (tenant_id, status, resource_type);
