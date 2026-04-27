CREATE TABLE dbo.msg_ecosystem_integration (
    id UNIQUEIDENTIFIER NOT NULL PRIMARY KEY,
    integration_type VARCHAR(32) NOT NULL,
    display_name NVARCHAR(128) NOT NULL,
    auth_mode VARCHAR(32) NULL,
    callback_url VARCHAR(512) NULL,
    sign_algorithm VARCHAR(64) NULL,
    config_ref VARCHAR(128) NOT NULL,
    status VARCHAR(32) NOT NULL,
    health_status VARCHAR(32) NOT NULL,
    last_check_at DATETIME2 NULL,
    last_error_summary NVARCHAR(512) NULL,
    tenant_id UNIQUEIDENTIFIER NOT NULL,
    created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME()
);

CREATE INDEX idx_msg_ecosystem_integration_tenant_type_status
    ON dbo.msg_ecosystem_integration (tenant_id, integration_type, status);

CREATE TABLE dbo.msg_callback_audit (
    id UNIQUEIDENTIFIER NOT NULL PRIMARY KEY,
    integration_id UNIQUEIDENTIFIER NOT NULL,
    callback_type VARCHAR(64) NOT NULL,
    verify_result VARCHAR(32) NOT NULL,
    payload_summary NVARCHAR(MAX) NULL,
    error_message NVARCHAR(512) NULL,
    idempotency_key VARCHAR(128) NOT NULL,
    payload_digest VARCHAR(128) NOT NULL,
    occurred_at DATETIME2 NOT NULL,
    CONSTRAINT fk_msg_callback_audit_integration
        FOREIGN KEY (integration_id) REFERENCES dbo.msg_ecosystem_integration (id)
);

CREATE UNIQUE INDEX uk_msg_callback_audit_integration_idempotency
    ON dbo.msg_callback_audit (integration_id, idempotency_key);

CREATE INDEX idx_msg_callback_audit_integration_occurred_at
    ON dbo.msg_callback_audit (integration_id, occurred_at);

CREATE INDEX idx_msg_callback_audit_verify_result_occurred_at
    ON dbo.msg_callback_audit (verify_result, occurred_at);
