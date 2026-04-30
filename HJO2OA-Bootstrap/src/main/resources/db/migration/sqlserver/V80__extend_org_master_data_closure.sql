IF OBJECT_ID('dbo.org_assignment_history', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.org_assignment_history (
        id UNIQUEIDENTIFIER NOT NULL PRIMARY KEY,
        assignment_id UNIQUEIDENTIFIER NOT NULL,
        person_id UNIQUEIDENTIFIER NOT NULL,
        position_id UNIQUEIDENTIFIER NOT NULL,
        type VARCHAR(32) NOT NULL,
        start_date DATE NULL,
        end_date DATE NULL,
        status VARCHAR(32) NOT NULL,
        action VARCHAR(64) NOT NULL,
        tenant_id UNIQUEIDENTIFIER NOT NULL,
        changed_at DATETIME2(6) NOT NULL DEFAULT SYSUTCDATETIME()
    );

    CREATE INDEX IX_org_assignment_history_person
        ON dbo.org_assignment_history (tenant_id, person_id, changed_at DESC);

    CREATE INDEX IX_org_assignment_history_assignment
        ON dbo.org_assignment_history (tenant_id, assignment_id, changed_at DESC);
END;

IF OBJECT_ID('dbo.org_master_import_job', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.org_master_import_job (
        id UNIQUEIDENTIFIER NOT NULL PRIMARY KEY,
        import_type VARCHAR(64) NOT NULL,
        status VARCHAR(32) NOT NULL DEFAULT 'COMPLETED',
        total_count INT NOT NULL DEFAULT 0,
        success_count INT NOT NULL DEFAULT 0,
        failure_count INT NOT NULL DEFAULT 0,
        error_message NVARCHAR(1000) NULL,
        tenant_id UNIQUEIDENTIFIER NOT NULL,
        request_id NVARCHAR(128) NULL,
        idempotency_key NVARCHAR(128) NULL,
        created_at DATETIME2(6) NOT NULL DEFAULT SYSUTCDATETIME(),
        updated_at DATETIME2(6) NOT NULL DEFAULT SYSUTCDATETIME(),
        CONSTRAINT CK_org_master_import_job_status
            CHECK (status IN ('PENDING', 'RUNNING', 'COMPLETED', 'FAILED'))
    );

    CREATE INDEX IX_org_master_import_job_tenant_created
        ON dbo.org_master_import_job (tenant_id, created_at DESC);
END;

IF OBJECT_ID('dbo.org_operation_idempotency', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.org_operation_idempotency (
        id UNIQUEIDENTIFIER NOT NULL PRIMARY KEY,
        tenant_id UNIQUEIDENTIFIER NOT NULL,
        idempotency_key NVARCHAR(128) NOT NULL,
        operation VARCHAR(128) NOT NULL,
        resource_id UNIQUEIDENTIFIER NULL,
        request_hash NVARCHAR(128) NULL,
        status VARCHAR(32) NOT NULL DEFAULT 'SUCCEEDED',
        created_at DATETIME2(6) NOT NULL DEFAULT SYSUTCDATETIME(),
        updated_at DATETIME2(6) NOT NULL DEFAULT SYSUTCDATETIME(),
        CONSTRAINT CK_org_operation_idempotency_status
            CHECK (status IN ('STARTED', 'SUCCEEDED', 'FAILED'))
    );

    CREATE UNIQUE INDEX UX_org_operation_idempotency_key
        ON dbo.org_operation_idempotency (tenant_id, operation, idempotency_key);
END;

IF COL_LENGTH('dbo.org_organization', 'version') IS NULL
    ALTER TABLE dbo.org_organization ADD version BIGINT NOT NULL CONSTRAINT DF_org_organization_version DEFAULT 0;

IF COL_LENGTH('dbo.org_department', 'version') IS NULL
    ALTER TABLE dbo.org_department ADD version BIGINT NOT NULL CONSTRAINT DF_org_department_version DEFAULT 0;

IF COL_LENGTH('dbo.org_position', 'version') IS NULL
    ALTER TABLE dbo.org_position ADD version BIGINT NOT NULL CONSTRAINT DF_org_position_version DEFAULT 0;

IF COL_LENGTH('dbo.org_assignment', 'version') IS NULL
    ALTER TABLE dbo.org_assignment ADD version BIGINT NOT NULL CONSTRAINT DF_org_assignment_version DEFAULT 0;

IF COL_LENGTH('dbo.org_person', 'version') IS NULL
    ALTER TABLE dbo.org_person ADD version BIGINT NOT NULL CONSTRAINT DF_org_person_version DEFAULT 0;

IF COL_LENGTH('dbo.org_account', 'version') IS NULL
    ALTER TABLE dbo.org_account ADD version BIGINT NOT NULL CONSTRAINT DF_org_account_version DEFAULT 0;

IF EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'UX_org_person_tenant_employee_no' AND object_id = OBJECT_ID('dbo.org_person'))
    DROP INDEX UX_org_person_tenant_employee_no ON dbo.org_person;

IF EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'UX_org_person_global_employee_no' AND object_id = OBJECT_ID('dbo.org_person'))
    DROP INDEX UX_org_person_global_employee_no ON dbo.org_person;

IF EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_org_person_tenant_org_department' AND object_id = OBJECT_ID('dbo.org_person'))
    DROP INDEX IX_org_person_tenant_org_department ON dbo.org_person;

IF EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_org_account_tenant_person' AND object_id = OBJECT_ID('dbo.org_account'))
    DROP INDEX IX_org_account_tenant_person ON dbo.org_account;

ALTER TABLE dbo.org_person ALTER COLUMN tenant_id UNIQUEIDENTIFIER NOT NULL;
ALTER TABLE dbo.org_account ALTER COLUMN tenant_id UNIQUEIDENTIFIER NOT NULL;

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'UX_org_person_tenant_employee_no' AND object_id = OBJECT_ID('dbo.org_person'))
    CREATE UNIQUE INDEX UX_org_person_tenant_employee_no ON dbo.org_person (tenant_id, employee_no);

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_org_person_tenant_org_department' AND object_id = OBJECT_ID('dbo.org_person'))
    CREATE INDEX IX_org_person_tenant_org_department ON dbo.org_person (tenant_id, organization_id, department_id);

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_org_account_tenant_person' AND object_id = OBJECT_ID('dbo.org_account'))
    CREATE INDEX IX_org_account_tenant_person ON dbo.org_account (tenant_id, person_id);

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'UX_org_person_tenant_id_id' AND object_id = OBJECT_ID('dbo.org_person'))
    CREATE UNIQUE INDEX UX_org_person_tenant_id_id ON dbo.org_person (tenant_id, id);

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'UX_org_position_tenant_id_id' AND object_id = OBJECT_ID('dbo.org_position'))
    CREATE UNIQUE INDEX UX_org_position_tenant_id_id ON dbo.org_position (tenant_id, id);

IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'FK_org_account_tenant_person')
    ALTER TABLE dbo.org_account ADD CONSTRAINT FK_org_account_tenant_person
        FOREIGN KEY (tenant_id, person_id) REFERENCES dbo.org_person (tenant_id, id);

IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'FK_org_assignment_tenant_person')
    ALTER TABLE dbo.org_assignment ADD CONSTRAINT FK_org_assignment_tenant_person
        FOREIGN KEY (tenant_id, person_id) REFERENCES dbo.org_person (tenant_id, id);

IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'FK_org_assignment_tenant_position')
    ALTER TABLE dbo.org_assignment ADD CONSTRAINT FK_org_assignment_tenant_position
        FOREIGN KEY (tenant_id, position_id) REFERENCES dbo.org_position (tenant_id, id);
