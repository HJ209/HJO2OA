-- ============================================================
-- V22: Org permission person-account module tables
-- Entity source: PersonEntity, AccountEntity
-- PK type: UNIQUEIDENTIFIER (UUID id)
-- ============================================================

CREATE TABLE dbo.org_person (
    id               UNIQUEIDENTIFIER NOT NULL PRIMARY KEY,
    employee_no      NVARCHAR(64)     NOT NULL,
    name             NVARCHAR(64)     NOT NULL,
    pinyin           NVARCHAR(128)    NULL,
    gender           NVARCHAR(16)     NULL,
    mobile           NVARCHAR(32)     NULL,
    email            NVARCHAR(128)    NULL,
    organization_id  UNIQUEIDENTIFIER NOT NULL,
    department_id    UNIQUEIDENTIFIER NULL,
    status           NVARCHAR(32)     NOT NULL CONSTRAINT DF_org_person_status DEFAULT ('ACTIVE'),
    tenant_id        UNIQUEIDENTIFIER NULL,
    created_at       DATETIME2(6)     NOT NULL CONSTRAINT DF_org_person_created_at DEFAULT (SYSUTCDATETIME()),
    updated_at       DATETIME2(6)     NOT NULL CONSTRAINT DF_org_person_updated_at DEFAULT (SYSUTCDATETIME()),
    CONSTRAINT FK_org_person_organization
        FOREIGN KEY (organization_id) REFERENCES dbo.org_organization(id),
    CONSTRAINT FK_org_person_department
        FOREIGN KEY (department_id) REFERENCES dbo.org_department(id),
    CONSTRAINT CK_org_person_status
        CHECK (status IN ('ACTIVE', 'DISABLED', 'RESIGNED'))
);

CREATE UNIQUE INDEX UX_org_person_tenant_employee_no
    ON dbo.org_person (tenant_id, employee_no)
    WHERE tenant_id IS NOT NULL;
CREATE UNIQUE INDEX UX_org_person_global_employee_no
    ON dbo.org_person (employee_no)
    WHERE tenant_id IS NULL;
CREATE INDEX IX_org_person_tenant_org_department
    ON dbo.org_person (tenant_id, organization_id, department_id);

CREATE TABLE dbo.org_account (
    id                    UNIQUEIDENTIFIER NOT NULL PRIMARY KEY,
    person_id             UNIQUEIDENTIFIER NOT NULL,
    username              NVARCHAR(64)     NOT NULL,
    credential            NVARCHAR(256)    NOT NULL,
    account_type          NVARCHAR(32)     NOT NULL,
    is_primary            BIT              NOT NULL CONSTRAINT DF_org_account_is_primary DEFAULT (0),
    locked                BIT              NOT NULL CONSTRAINT DF_org_account_locked DEFAULT (0),
    locked_until          DATETIME2(6)     NULL,
    last_login_at         DATETIME2(6)     NULL,
    last_login_ip         NVARCHAR(64)     NULL,
    password_changed_at   DATETIME2(6)     NULL,
    must_change_password  BIT              NOT NULL CONSTRAINT DF_org_account_must_change_password DEFAULT (0),
    status                NVARCHAR(32)     NOT NULL CONSTRAINT DF_org_account_status DEFAULT ('ACTIVE'),
    tenant_id             UNIQUEIDENTIFIER NULL,
    created_at            DATETIME2(6)     NOT NULL CONSTRAINT DF_org_account_created_at DEFAULT (SYSUTCDATETIME()),
    updated_at            DATETIME2(6)     NOT NULL CONSTRAINT DF_org_account_updated_at DEFAULT (SYSUTCDATETIME()),
    CONSTRAINT FK_org_account_person
        FOREIGN KEY (person_id) REFERENCES dbo.org_person(id),
    CONSTRAINT CK_org_account_type
        CHECK (account_type IN ('PASSWORD', 'LDAP', 'SSO', 'WECHAT', 'DINGTALK')),
    CONSTRAINT CK_org_account_status
        CHECK (status IN ('ACTIVE', 'DISABLED'))
);

CREATE UNIQUE INDEX UX_org_account_username
    ON dbo.org_account (username);
CREATE UNIQUE INDEX UX_org_account_person_type
    ON dbo.org_account (person_id, account_type);
CREATE UNIQUE INDEX UX_org_account_person_primary
    ON dbo.org_account (person_id)
    WHERE is_primary = 1;
CREATE INDEX IX_org_account_tenant_person
    ON dbo.org_account (tenant_id, person_id);
