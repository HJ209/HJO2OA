CREATE TABLE dbo.org_position (
    id UNIQUEIDENTIFIER NOT NULL PRIMARY KEY,
    code VARCHAR(64) NOT NULL,
    name NVARCHAR(128) NOT NULL,
    organization_id UNIQUEIDENTIFIER NOT NULL,
    department_id UNIQUEIDENTIFIER NULL,
    category VARCHAR(32) NOT NULL,
    level INT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    tenant_id UNIQUEIDENTIFIER NOT NULL,
    created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT fk_org_position_organization
        FOREIGN KEY (organization_id) REFERENCES dbo.org_organization (id),
    CONSTRAINT fk_org_position_department
        FOREIGN KEY (department_id) REFERENCES dbo.org_department (id),
    CONSTRAINT ck_org_position_category
        CHECK (category IN ('MANAGEMENT', 'PROFESSIONAL', 'TECHNICAL', 'OPERATIONAL', 'OTHER')),
    CONSTRAINT ck_org_position_status
        CHECK (status IN ('ACTIVE', 'DISABLED'))
);

CREATE UNIQUE INDEX uk_org_position_tenant_code
    ON dbo.org_position (tenant_id, code);

CREATE INDEX idx_org_position_org_dept
    ON dbo.org_position (tenant_id, organization_id, department_id);

CREATE INDEX idx_org_position_status
    ON dbo.org_position (tenant_id, status);

CREATE TABLE dbo.org_assignment (
    id UNIQUEIDENTIFIER NOT NULL PRIMARY KEY,
    person_id UNIQUEIDENTIFIER NOT NULL,
    position_id UNIQUEIDENTIFIER NOT NULL,
    type VARCHAR(32) NOT NULL,
    start_date DATE NULL,
    end_date DATE NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    tenant_id UNIQUEIDENTIFIER NOT NULL,
    created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT fk_org_assignment_person
        FOREIGN KEY (person_id) REFERENCES dbo.org_person (id),
    CONSTRAINT fk_org_assignment_position
        FOREIGN KEY (position_id) REFERENCES dbo.org_position (id),
    CONSTRAINT ck_org_assignment_type
        CHECK (type IN ('PRIMARY', 'SECONDARY', 'PART_TIME')),
    CONSTRAINT ck_org_assignment_status
        CHECK (status IN ('ACTIVE', 'INACTIVE')),
    CONSTRAINT ck_org_assignment_date_range
        CHECK (start_date IS NULL OR end_date IS NULL OR end_date >= start_date)
);

CREATE UNIQUE INDEX uk_org_assignment_active_person_position
    ON dbo.org_assignment (tenant_id, person_id, position_id)
    WHERE status = 'ACTIVE';

CREATE UNIQUE INDEX uk_org_assignment_active_primary_person
    ON dbo.org_assignment (tenant_id, person_id)
    WHERE status = 'ACTIVE' AND type = 'PRIMARY';

CREATE INDEX idx_org_assignment_person_type
    ON dbo.org_assignment (tenant_id, person_id, type);

CREATE INDEX idx_org_assignment_position
    ON dbo.org_assignment (tenant_id, position_id);

CREATE TABLE dbo.org_position_role (
    id UNIQUEIDENTIFIER NOT NULL PRIMARY KEY,
    position_id UNIQUEIDENTIFIER NOT NULL,
    role_id UNIQUEIDENTIFIER NOT NULL,
    tenant_id UNIQUEIDENTIFIER NOT NULL,
    created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT fk_org_position_role_position
        FOREIGN KEY (position_id) REFERENCES dbo.org_position (id)
);

CREATE UNIQUE INDEX uk_org_position_role_position_role
    ON dbo.org_position_role (position_id, role_id);

CREATE INDEX idx_org_position_role_role
    ON dbo.org_position_role (tenant_id, role_id);
