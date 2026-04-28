CREATE TABLE dbo.msg_device_binding (
    id UNIQUEIDENTIFIER NOT NULL PRIMARY KEY,
    person_id UNIQUEIDENTIFIER NOT NULL,
    account_id UNIQUEIDENTIFIER NOT NULL,
    device_id VARCHAR(128) NOT NULL,
    device_fingerprint VARCHAR(256) NULL,
    platform VARCHAR(32) NOT NULL,
    app_type VARCHAR(32) NOT NULL,
    push_token VARCHAR(512) NULL,
    bind_status VARCHAR(32) NOT NULL,
    risk_level VARCHAR(32) NOT NULL DEFAULT 'LOW',
    last_login_at DATETIME2 NULL,
    last_seen_at DATETIME2 NULL,
    tenant_id UNIQUEIDENTIFIER NOT NULL,
    created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME()
);

CREATE UNIQUE INDEX uk_msg_device_binding_active_device
    ON dbo.msg_device_binding (tenant_id, device_id)
    WHERE bind_status = 'ACTIVE';

CREATE INDEX idx_msg_device_binding_tenant_device_status
    ON dbo.msg_device_binding (tenant_id, device_id, bind_status);

CREATE INDEX idx_msg_device_binding_tenant_person_status
    ON dbo.msg_device_binding (tenant_id, person_id, bind_status);

CREATE TABLE dbo.msg_mobile_session (
    id UNIQUEIDENTIFIER NOT NULL PRIMARY KEY,
    device_binding_id UNIQUEIDENTIFIER NOT NULL,
    person_id UNIQUEIDENTIFIER NOT NULL,
    account_id UNIQUEIDENTIFIER NOT NULL,
    current_assignment_id UNIQUEIDENTIFIER NULL,
    current_position_id UNIQUEIDENTIFIER NULL,
    session_status VARCHAR(32) NOT NULL,
    risk_level_snapshot VARCHAR(32) NOT NULL DEFAULT 'LOW',
    risk_frozen_at DATETIME2 NULL,
    risk_reason VARCHAR(512) NULL,
    issued_at DATETIME2 NOT NULL,
    expires_at DATETIME2 NOT NULL,
    last_heartbeat_at DATETIME2 NULL,
    refresh_version INT NOT NULL DEFAULT 0,
    tenant_id UNIQUEIDENTIFIER NOT NULL,
    created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT fk_msg_mobile_session_device_binding
        FOREIGN KEY (device_binding_id) REFERENCES dbo.msg_device_binding (id)
);

CREATE INDEX idx_msg_mobile_session_tenant_device_status
    ON dbo.msg_mobile_session (tenant_id, device_binding_id, session_status);

CREATE INDEX idx_msg_mobile_session_tenant_person_status
    ON dbo.msg_mobile_session (tenant_id, person_id, session_status);

CREATE INDEX idx_msg_mobile_session_expires_at
    ON dbo.msg_mobile_session (expires_at);
