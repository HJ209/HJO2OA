CREATE TABLE form_metadata (
    id UNIQUEIDENTIFIER NOT NULL PRIMARY KEY,
    code NVARCHAR(64) NOT NULL,
    name NVARCHAR(128) NOT NULL,
    name_i18n_key NVARCHAR(256) NULL,
    version INT NOT NULL DEFAULT 1,
    status NVARCHAR(32) NOT NULL,
    field_schema NVARCHAR(MAX) NOT NULL,
    layout NVARCHAR(MAX) NOT NULL,
    validations NVARCHAR(MAX) NULL,
    field_permission_map NVARCHAR(MAX) NULL,
    tenant_id UNIQUEIDENTIFIER NOT NULL,
    published_at DATETIME2 NULL,
    created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT uk_form_metadata_tenant_code_version UNIQUE (tenant_id, code, version)
);

CREATE INDEX idx_form_metadata_tenant_status
    ON form_metadata (tenant_id, status);

CREATE INDEX idx_form_metadata_tenant_code
    ON form_metadata (tenant_id, code);
