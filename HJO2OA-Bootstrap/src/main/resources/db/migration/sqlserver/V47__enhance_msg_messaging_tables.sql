IF OBJECT_ID(N'dbo.msg_mobile_push_preference', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.msg_mobile_push_preference (
        id UNIQUEIDENTIFIER NOT NULL PRIMARY KEY,
        tenant_id UNIQUEIDENTIFIER NOT NULL,
        person_id UNIQUEIDENTIFIER NOT NULL,
        push_enabled BIT NOT NULL DEFAULT 1,
        quiet_starts_at TIME NULL,
        quiet_ends_at TIME NULL,
        muted_categories NVARCHAR(MAX) NULL,
        created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
        updated_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME()
    );

    CREATE UNIQUE INDEX uk_msg_mobile_push_preference_person
        ON dbo.msg_mobile_push_preference (tenant_id, person_id);
END;

IF COL_LENGTH(N'dbo.msg_notification_delivery_record', N'last_error_message') IS NULL
BEGIN
    ALTER TABLE dbo.msg_notification_delivery_record
        ADD last_error_message NVARCHAR(512) NULL;
END;

IF OBJECT_ID(N'dbo.msg_subscription_execution_log', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.msg_subscription_execution_log (
        id UNIQUEIDENTIFIER NOT NULL PRIMARY KEY,
        event_id UNIQUEIDENTIFIER NOT NULL,
        event_type VARCHAR(128) NOT NULL,
        rule_code VARCHAR(64) NOT NULL,
        recipient_id NVARCHAR(128) NULL,
        result VARCHAR(32) NOT NULL,
        message NVARCHAR(512) NULL,
        tenant_id UNIQUEIDENTIFIER NOT NULL,
        occurred_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME()
    );

    CREATE UNIQUE INDEX uk_msg_subscription_execution_log_dedup
        ON dbo.msg_subscription_execution_log (event_id, rule_code, recipient_id);

    CREATE INDEX idx_msg_subscription_execution_log_tenant_event
        ON dbo.msg_subscription_execution_log (tenant_id, event_type, occurred_at);
END;
