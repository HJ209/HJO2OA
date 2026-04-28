CREATE TABLE dbo.msg_template (
    id UNIQUEIDENTIFIER NOT NULL PRIMARY KEY,
    code VARCHAR(64) NOT NULL,
    channel_type VARCHAR(32) NOT NULL,
    locale VARCHAR(16) NOT NULL,
    version INT NOT NULL,
    category VARCHAR(32) NOT NULL,
    title_template NVARCHAR(256) NOT NULL,
    body_template NVARCHAR(4000) NOT NULL,
    variable_schema NVARCHAR(MAX) NULL,
    status VARCHAR(32) NOT NULL,
    system_locked BIT NOT NULL DEFAULT 0,
    tenant_id UNIQUEIDENTIFIER NULL,
    created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME()
);

CREATE UNIQUE INDEX uk_msg_template_global
    ON dbo.msg_template (code, channel_type, locale, version)
    WHERE tenant_id IS NULL;

CREATE UNIQUE INDEX uk_msg_template_tenant
    ON dbo.msg_template (tenant_id, code, channel_type, locale, version)
    WHERE tenant_id IS NOT NULL;

CREATE INDEX idx_msg_template_tenant_status_category
    ON dbo.msg_template (tenant_id, status, category);

CREATE TABLE dbo.msg_channel_endpoint (
    id UNIQUEIDENTIFIER NOT NULL PRIMARY KEY,
    endpoint_code VARCHAR(64) NOT NULL,
    channel_type VARCHAR(32) NOT NULL,
    provider_type VARCHAR(32) NOT NULL,
    display_name NVARCHAR(128) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'ENABLED',
    config_ref VARCHAR(128) NOT NULL,
    rate_limit_per_minute INT NULL,
    daily_quota INT NULL,
    tenant_id UNIQUEIDENTIFIER NULL,
    created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME()
);

CREATE UNIQUE INDEX uk_msg_channel_endpoint_global
    ON dbo.msg_channel_endpoint (endpoint_code)
    WHERE tenant_id IS NULL;

CREATE UNIQUE INDEX uk_msg_channel_endpoint_tenant
    ON dbo.msg_channel_endpoint (tenant_id, endpoint_code)
    WHERE tenant_id IS NOT NULL;

CREATE INDEX idx_msg_channel_endpoint_tenant_type_status
    ON dbo.msg_channel_endpoint (tenant_id, channel_type, status);

CREATE TABLE dbo.msg_routing_policy (
    id UNIQUEIDENTIFIER NOT NULL PRIMARY KEY,
    policy_code VARCHAR(64) NOT NULL,
    category VARCHAR(32) NOT NULL,
    priority_threshold VARCHAR(32) NOT NULL,
    target_channel_order NVARCHAR(MAX) NOT NULL,
    fallback_channel_order NVARCHAR(MAX) NULL,
    quiet_window_behavior VARCHAR(32) NOT NULL,
    dedup_window_seconds INT NOT NULL DEFAULT 300,
    escalation_policy NVARCHAR(MAX) NULL,
    enabled BIT NOT NULL DEFAULT 1,
    tenant_id UNIQUEIDENTIFIER NULL,
    created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME()
);

CREATE UNIQUE INDEX uk_msg_routing_policy_global
    ON dbo.msg_routing_policy (policy_code)
    WHERE tenant_id IS NULL;

CREATE UNIQUE INDEX uk_msg_routing_policy_tenant
    ON dbo.msg_routing_policy (tenant_id, policy_code)
    WHERE tenant_id IS NOT NULL;

CREATE INDEX idx_msg_routing_policy_tenant_category_priority_enabled
    ON dbo.msg_routing_policy (tenant_id, category, priority_threshold, enabled);

CREATE TABLE dbo.msg_delivery_task (
    id UNIQUEIDENTIFIER NOT NULL PRIMARY KEY,
    notification_id UNIQUEIDENTIFIER NOT NULL,
    channel_type VARCHAR(32) NOT NULL,
    endpoint_id UNIQUEIDENTIFIER NULL,
    route_order INT NOT NULL,
    status VARCHAR(32) NOT NULL,
    retry_count INT NOT NULL DEFAULT 0,
    next_retry_at DATETIME2 NULL,
    provider_message_id VARCHAR(128) NULL,
    last_error_code VARCHAR(64) NULL,
    last_error_message NVARCHAR(512) NULL,
    delivered_at DATETIME2 NULL,
    tenant_id UNIQUEIDENTIFIER NOT NULL,
    created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT fk_msg_delivery_task_endpoint
        FOREIGN KEY (endpoint_id) REFERENCES dbo.msg_channel_endpoint (id)
);

CREATE INDEX idx_msg_delivery_task_tenant_notification_channel
    ON dbo.msg_delivery_task (tenant_id, notification_id, channel_type);

CREATE INDEX idx_msg_delivery_task_tenant_status_retry
    ON dbo.msg_delivery_task (tenant_id, status, next_retry_at);

CREATE INDEX idx_msg_delivery_task_tenant_provider_message
    ON dbo.msg_delivery_task (tenant_id, provider_message_id);

CREATE TABLE dbo.msg_delivery_attempt (
    id UNIQUEIDENTIFIER NOT NULL PRIMARY KEY,
    delivery_task_id UNIQUEIDENTIFIER NOT NULL,
    attempt_no INT NOT NULL,
    request_payload_snapshot NVARCHAR(MAX) NULL,
    provider_response NVARCHAR(MAX) NULL,
    result_status VARCHAR(32) NOT NULL,
    error_code VARCHAR(64) NULL,
    error_message NVARCHAR(512) NULL,
    requested_at DATETIME2 NOT NULL,
    completed_at DATETIME2 NULL,
    CONSTRAINT fk_msg_delivery_attempt_task
        FOREIGN KEY (delivery_task_id) REFERENCES dbo.msg_delivery_task (id)
);

CREATE UNIQUE INDEX uk_msg_delivery_attempt_task_attempt
    ON dbo.msg_delivery_attempt (delivery_task_id, attempt_no);

CREATE INDEX idx_msg_delivery_attempt_requested_at
    ON dbo.msg_delivery_attempt (requested_at);
