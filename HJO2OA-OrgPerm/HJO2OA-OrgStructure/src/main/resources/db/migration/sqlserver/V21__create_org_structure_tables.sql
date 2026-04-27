CREATE TABLE dbo.org_organization (
    id UNIQUEIDENTIFIER NOT NULL PRIMARY KEY,
    code VARCHAR(64) NOT NULL,
    name NVARCHAR(128) NOT NULL,
    short_name NVARCHAR(64) NULL,
    type VARCHAR(32) NOT NULL,
    parent_id UNIQUEIDENTIFIER NULL,
    level INT NOT NULL,
    path VARCHAR(512) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    tenant_id UNIQUEIDENTIFIER NOT NULL,
    created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT fk_org_organization_parent
        FOREIGN KEY (parent_id) REFERENCES dbo.org_organization (id)
);

CREATE UNIQUE INDEX uk_org_organization_tenant_code
    ON dbo.org_organization (tenant_id, code);

CREATE INDEX idx_org_organization_tenant_parent_sort
    ON dbo.org_organization (tenant_id, parent_id, sort_order);

CREATE INDEX idx_org_organization_tenant_path
    ON dbo.org_organization (tenant_id, path);

CREATE TABLE dbo.org_department (
    id UNIQUEIDENTIFIER NOT NULL PRIMARY KEY,
    code VARCHAR(64) NOT NULL,
    name NVARCHAR(128) NOT NULL,
    organization_id UNIQUEIDENTIFIER NOT NULL,
    parent_id UNIQUEIDENTIFIER NULL,
    level INT NOT NULL,
    path VARCHAR(512) NOT NULL,
    manager_id UNIQUEIDENTIFIER NULL,
    sort_order INT NOT NULL DEFAULT 0,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    tenant_id UNIQUEIDENTIFIER NOT NULL,
    created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT fk_org_department_organization
        FOREIGN KEY (organization_id) REFERENCES dbo.org_organization (id),
    CONSTRAINT fk_org_department_parent
        FOREIGN KEY (parent_id) REFERENCES dbo.org_department (id)
);

CREATE UNIQUE INDEX uk_org_department_tenant_code
    ON dbo.org_department (tenant_id, code);

CREATE INDEX idx_org_department_tenant_org_parent_sort
    ON dbo.org_department (tenant_id, organization_id, parent_id, sort_order);

CREATE INDEX idx_org_department_tenant_path
    ON dbo.org_department (tenant_id, path);
