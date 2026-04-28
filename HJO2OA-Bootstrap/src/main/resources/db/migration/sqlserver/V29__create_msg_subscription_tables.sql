CREATE TABLE dbo.msg_subscription_rule (
    id UNIQUEIDENTIFIER NOT NULL PRIMARY KEY,
    rule_code VARCHAR(64) NOT NULL,
    event_type_pattern VARCHAR(128) NOT NULL,
    notification_category VARCHAR(64) NOT NULL,
    target_resolver_type VARCHAR(64) NOT NULL,
    target_resolver_config NVARCHAR(MAX) NULL,
    template_code VARCHAR(64) NOT NULL,
    condition_expr NVARCHAR(1024) NULL,
    priority_mapping NVARCHAR(MAX) NULL,
    default_priority VARCHAR(32) NOT NULL DEFAULT 'NORMAL',
    enabled BIT NOT NULL DEFAULT 1,
    tenant_id UNIQUEIDENTIFIER NOT NULL,
    created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT ck_msg_subscription_rule_target_config_json
        CHECK (target_resolver_config IS NULL OR ISJSON(target_resolver_config) = 1),
    CONSTRAINT ck_msg_subscription_rule_priority_json
        CHECK (priority_mapping IS NULL OR ISJSON(priority_mapping) = 1)
);

CREATE UNIQUE INDEX uk_msg_subscription_rule_tenant_code
    ON dbo.msg_subscription_rule (tenant_id, rule_code);

CREATE INDEX idx_msg_subscription_rule_tenant_event
    ON dbo.msg_subscription_rule (tenant_id, event_type_pattern, enabled);

CREATE INDEX idx_msg_subscription_rule_tenant_category
    ON dbo.msg_subscription_rule (tenant_id, notification_category, enabled);

CREATE TABLE dbo.msg_subscription_preference (
    id UNIQUEIDENTIFIER NOT NULL PRIMARY KEY,
    person_id UNIQUEIDENTIFIER NOT NULL,
    category VARCHAR(64) NOT NULL,
    allowed_channels NVARCHAR(MAX) NOT NULL,
    quiet_window NVARCHAR(MAX) NULL,
    digest_mode VARCHAR(32) NOT NULL DEFAULT 'IMMEDIATE',
    escalation_opt_in BIT NOT NULL DEFAULT 1,
    mute_non_working_hours BIT NOT NULL DEFAULT 0,
    enabled BIT NOT NULL DEFAULT 1,
    tenant_id UNIQUEIDENTIFIER NOT NULL,
    created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT ck_msg_subscription_preference_channels_json
        CHECK (ISJSON(allowed_channels) = 1),
    CONSTRAINT ck_msg_subscription_preference_quiet_json
        CHECK (quiet_window IS NULL OR ISJSON(quiet_window) = 1)
);

CREATE UNIQUE INDEX uk_msg_subscription_preference_tenant_person_category
    ON dbo.msg_subscription_preference (tenant_id, person_id, category);

CREATE INDEX idx_msg_subscription_preference_person_enabled
    ON dbo.msg_subscription_preference (tenant_id, person_id, enabled);

CREATE INDEX idx_msg_subscription_preference_category
    ON dbo.msg_subscription_preference (tenant_id, category);
