CREATE TABLE dbo.proc_definition (
    id UNIQUEIDENTIFIER NOT NULL PRIMARY KEY,
    code VARCHAR(64) NOT NULL,
    name NVARCHAR(128) NOT NULL,
    category VARCHAR(64) NULL,
    version INT NOT NULL DEFAULT 1,
    status VARCHAR(32) NOT NULL,
    form_metadata_id UNIQUEIDENTIFIER NULL,
    start_node_id VARCHAR(64) NULL,
    end_node_id VARCHAR(64) NULL,
    nodes NVARCHAR(MAX) NOT NULL,
    routes NVARCHAR(MAX) NOT NULL,
    tenant_id UNIQUEIDENTIFIER NOT NULL,
    published_at DATETIME2 NULL,
    published_by UNIQUEIDENTIFIER NULL,
    created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME()
);

CREATE UNIQUE INDEX uk_proc_definition_tenant_code_version
    ON dbo.proc_definition (tenant_id, code, version);

CREATE INDEX idx_proc_definition_tenant_category_status
    ON dbo.proc_definition (tenant_id, category, status);

CREATE TABLE dbo.proc_action_def (
    id UNIQUEIDENTIFIER NOT NULL PRIMARY KEY,
    code VARCHAR(64) NOT NULL,
    name NVARCHAR(128) NOT NULL,
    category VARCHAR(32) NOT NULL,
    route_target VARCHAR(32) NOT NULL,
    require_opinion BIT NOT NULL DEFAULT 0,
    require_target BIT NOT NULL DEFAULT 0,
    ui_config NVARCHAR(MAX) NULL,
    tenant_id UNIQUEIDENTIFIER NOT NULL,
    created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME()
);

CREATE UNIQUE INDEX uk_proc_action_def_tenant_code
    ON dbo.proc_action_def (tenant_id, code);

CREATE INDEX idx_proc_action_def_tenant_category
    ON dbo.proc_action_def (tenant_id, category);
