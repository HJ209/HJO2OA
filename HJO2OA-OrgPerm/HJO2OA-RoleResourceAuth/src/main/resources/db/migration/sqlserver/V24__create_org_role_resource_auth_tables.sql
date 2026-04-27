CREATE TABLE dbo.org_role (
    id UNIQUEIDENTIFIER NOT NULL PRIMARY KEY,
    code VARCHAR(64) NOT NULL,
    name NVARCHAR(128) NOT NULL,
    category VARCHAR(32) NOT NULL,
    scope VARCHAR(32) NOT NULL,
    description NVARCHAR(512) NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    tenant_id UNIQUEIDENTIFIER NOT NULL,
    created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME()
);

CREATE UNIQUE INDEX uk_org_role_tenant_code
    ON dbo.org_role (tenant_id, code);

CREATE INDEX idx_org_role_tenant_category_scope
    ON dbo.org_role (tenant_id, category, scope);

CREATE INDEX idx_org_role_tenant_status
    ON dbo.org_role (tenant_id, status);

CREATE TABLE dbo.org_resource_permission (
    id UNIQUEIDENTIFIER NOT NULL PRIMARY KEY,
    role_id UNIQUEIDENTIFIER NOT NULL,
    resource_type VARCHAR(32) NOT NULL,
    resource_code VARCHAR(128) NOT NULL,
    action VARCHAR(32) NOT NULL,
    effect VARCHAR(32) NOT NULL DEFAULT 'ALLOW',
    tenant_id UNIQUEIDENTIFIER NOT NULL,
    CONSTRAINT fk_org_resource_permission_role
        FOREIGN KEY (role_id) REFERENCES dbo.org_role (id)
);

CREATE UNIQUE INDEX uk_org_resource_permission_role_resource_action
    ON dbo.org_resource_permission (role_id, resource_type, resource_code, action);

CREATE INDEX idx_org_resource_permission_tenant_resource
    ON dbo.org_resource_permission (tenant_id, resource_type, resource_code);

CREATE TABLE dbo.org_person_role (
    id UNIQUEIDENTIFIER NOT NULL PRIMARY KEY,
    person_id UNIQUEIDENTIFIER NOT NULL,
    role_id UNIQUEIDENTIFIER NOT NULL,
    reason NVARCHAR(256) NOT NULL,
    expires_at DATETIME2 NULL,
    tenant_id UNIQUEIDENTIFIER NOT NULL,
    CONSTRAINT fk_org_person_role_role
        FOREIGN KEY (role_id) REFERENCES dbo.org_role (id)
);

CREATE UNIQUE INDEX uk_org_person_role_person_role
    ON dbo.org_person_role (person_id, role_id);

CREATE INDEX idx_org_person_role_tenant_person
    ON dbo.org_person_role (tenant_id, person_id);
