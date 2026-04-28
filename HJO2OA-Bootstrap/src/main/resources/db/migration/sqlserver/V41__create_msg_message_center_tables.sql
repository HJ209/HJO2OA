IF OBJECT_ID(N'dbo.msg_notification', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.msg_notification (
        notification_id NVARCHAR(128) NOT NULL,
        dedup_key NVARCHAR(255) NOT NULL,
        tenant_id NVARCHAR(128) NOT NULL,
        recipient_id NVARCHAR(128) NOT NULL,
        target_assignment_id NVARCHAR(128) NULL,
        target_position_id NVARCHAR(128) NULL,
        title NVARCHAR(255) NOT NULL,
        body_summary NVARCHAR(1024) NOT NULL,
        deep_link NVARCHAR(512) NOT NULL,
        category NVARCHAR(64) NOT NULL,
        priority NVARCHAR(64) NOT NULL,
        inbox_status NVARCHAR(64) NOT NULL,
        source_module NVARCHAR(128) NOT NULL,
        source_event_type NVARCHAR(255) NOT NULL,
        source_business_id NVARCHAR(128) NOT NULL,
        created_at DATETIME2(7) NOT NULL,
        updated_at DATETIME2(7) NOT NULL,
        read_at DATETIME2(7) NULL,
        archived_at DATETIME2(7) NULL,
        revoked_at DATETIME2(7) NULL,
        expired_at DATETIME2(7) NULL,
        status_reason NVARCHAR(512) NULL,
        CONSTRAINT pk_msg_notification PRIMARY KEY (notification_id),
        CONSTRAINT uk_msg_notification_dedup UNIQUE (dedup_key)
    );
    CREATE INDEX ix_msg_notification_recipient
        ON dbo.msg_notification (tenant_id, recipient_id, inbox_status, created_at);
END;

IF OBJECT_ID(N'dbo.msg_notification_action', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.msg_notification_action (
        action_id NVARCHAR(128) NOT NULL,
        notification_id NVARCHAR(128) NOT NULL,
        action_type NVARCHAR(64) NOT NULL,
        operator_id NVARCHAR(128) NOT NULL,
        occurred_at DATETIME2(7) NOT NULL,
        CONSTRAINT pk_msg_notification_action PRIMARY KEY (action_id)
    );
    CREATE INDEX ix_msg_notification_action_notification
        ON dbo.msg_notification_action (notification_id, occurred_at);
END;

IF OBJECT_ID(N'dbo.msg_notification_delivery_record', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.msg_notification_delivery_record (
        delivery_id NVARCHAR(128) NOT NULL,
        notification_id NVARCHAR(128) NOT NULL,
        channel NVARCHAR(64) NOT NULL,
        status NVARCHAR(64) NOT NULL,
        attempt_count INT NOT NULL,
        created_at DATETIME2(7) NOT NULL,
        updated_at DATETIME2(7) NOT NULL,
        delivered_at DATETIME2(7) NULL,
        last_error_code NVARCHAR(128) NULL,
        CONSTRAINT pk_msg_notification_delivery_record PRIMARY KEY (delivery_id),
        CONSTRAINT uk_msg_notification_delivery_channel UNIQUE (notification_id, channel)
    );
END;

IF OBJECT_ID(N'dbo.msg_notification_projection_event', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.msg_notification_projection_event (
        event_id UNIQUEIDENTIFIER NOT NULL,
        processed_at DATETIME2(7) NOT NULL,
        CONSTRAINT pk_msg_notification_projection_event PRIMARY KEY (event_id)
    );
END;
