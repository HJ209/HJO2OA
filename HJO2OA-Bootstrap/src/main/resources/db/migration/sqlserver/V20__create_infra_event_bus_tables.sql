-- ============================================================
-- V20: Infrastructure event-bus module tables
-- Domain source: EventDefinition, SubscriptionBinding, EventMessage, DeliveryAttempt
-- PK type: UNIQUEIDENTIFIER (UUID id)
-- ============================================================

CREATE TABLE dbo.infra_event_definition (
    id               UNIQUEIDENTIFIER NOT NULL PRIMARY KEY,
    event_type       NVARCHAR(128)    NOT NULL,
    module_prefix    NVARCHAR(32)     NOT NULL,
    version          NVARCHAR(16)     NOT NULL,
    payload_schema   NVARCHAR(MAX)    NOT NULL,
    description      NVARCHAR(512)    NULL,
    publish_mode     NVARCHAR(32)     NOT NULL CONSTRAINT DF_infra_event_definition_publish_mode DEFAULT ('ASYNC'),
    status           NVARCHAR(32)     NOT NULL CONSTRAINT DF_infra_event_definition_status DEFAULT ('DRAFT'),
    owner_module     NVARCHAR(64)     NOT NULL,
    tenant_scope     NVARCHAR(32)     NOT NULL CONSTRAINT DF_infra_event_definition_tenant_scope DEFAULT ('GLOBAL'),
    created_at       DATETIME2(6)     NOT NULL CONSTRAINT DF_infra_event_definition_created_at DEFAULT (SYSUTCDATETIME()),
    updated_at       DATETIME2(6)     NOT NULL CONSTRAINT DF_infra_event_definition_updated_at DEFAULT (SYSUTCDATETIME())
);

CREATE UNIQUE INDEX IX_infra_event_definition_type_version
    ON dbo.infra_event_definition (event_type, version);
CREATE INDEX IX_infra_event_definition_prefix_status
    ON dbo.infra_event_definition (module_prefix, status);

CREATE TABLE dbo.infra_event_subscription (
    id                    UNIQUEIDENTIFIER NOT NULL PRIMARY KEY,
    event_definition_id   UNIQUEIDENTIFIER NOT NULL,
    subscriber_code       NVARCHAR(64)     NOT NULL,
    match_mode            NVARCHAR(32)     NOT NULL CONSTRAINT DF_infra_event_subscription_match_mode DEFAULT ('EXACT'),
    retry_policy          NVARCHAR(MAX)    NULL,
    dead_letter_enabled   BIT              NOT NULL CONSTRAINT DF_infra_event_subscription_dead_letter_enabled DEFAULT (1),
    active                BIT              NOT NULL CONSTRAINT DF_infra_event_subscription_active DEFAULT (1),
    CONSTRAINT FK_infra_event_subscription_definition
        FOREIGN KEY (event_definition_id) REFERENCES dbo.infra_event_definition(id)
);

CREATE INDEX IX_infra_event_subscription_definition
    ON dbo.infra_event_subscription (event_definition_id, subscriber_code);

CREATE TABLE dbo.infra_event_message (
    id                    UNIQUEIDENTIFIER NOT NULL PRIMARY KEY,
    event_definition_id   UNIQUEIDENTIFIER NOT NULL,
    event_type            NVARCHAR(128)    NOT NULL,
    source                NVARCHAR(128)    NOT NULL,
    tenant_id             UNIQUEIDENTIFIER NULL,
    correlation_id        NVARCHAR(128)    NULL,
    trace_id              NVARCHAR(128)    NULL,
    operator_account_id   UNIQUEIDENTIFIER NULL,
    operator_person_id    UNIQUEIDENTIFIER NULL,
    payload               NVARCHAR(MAX)    NOT NULL,
    publish_status        NVARCHAR(32)     NOT NULL CONSTRAINT DF_infra_event_message_publish_status DEFAULT ('PENDING'),
    published_at          DATETIME2(6)     NULL,
    retained_until        DATETIME2(6)     NULL,
    created_at            DATETIME2(6)     NOT NULL CONSTRAINT DF_infra_event_message_created_at DEFAULT (SYSUTCDATETIME()),
    updated_at            DATETIME2(6)     NOT NULL CONSTRAINT DF_infra_event_message_updated_at DEFAULT (SYSUTCDATETIME()),
    CONSTRAINT FK_infra_event_message_definition
        FOREIGN KEY (event_definition_id) REFERENCES dbo.infra_event_definition(id)
);

CREATE INDEX IX_infra_event_message_type_status_created
    ON dbo.infra_event_message (event_type, publish_status, created_at DESC);
CREATE INDEX IX_infra_event_message_tenant_correlation
    ON dbo.infra_event_message (tenant_id, correlation_id);
CREATE INDEX IX_infra_event_message_trace
    ON dbo.infra_event_message (trace_id);

CREATE TABLE dbo.infra_event_delivery_attempt (
    id                  UNIQUEIDENTIFIER NOT NULL PRIMARY KEY,
    event_message_id    UNIQUEIDENTIFIER NOT NULL,
    subscriber_code     NVARCHAR(64)     NOT NULL,
    attempt_no          INT              NOT NULL,
    delivery_status     NVARCHAR(32)     NOT NULL,
    error_code          NVARCHAR(64)     NULL,
    error_message       NVARCHAR(512)    NULL,
    delivered_at        DATETIME2(6)     NULL,
    next_retry_at       DATETIME2(6)     NULL,
    request_snapshot    NVARCHAR(MAX)    NULL,
    response_snapshot   NVARCHAR(MAX)    NULL,
    CONSTRAINT FK_infra_event_delivery_attempt_message
        FOREIGN KEY (event_message_id) REFERENCES dbo.infra_event_message(id)
);

CREATE UNIQUE INDEX IX_infra_event_delivery_attempt_message_subscriber_no
    ON dbo.infra_event_delivery_attempt (event_message_id, subscriber_code, attempt_no);
CREATE INDEX IX_infra_event_delivery_attempt_status_retry
    ON dbo.infra_event_delivery_attempt (delivery_status, next_retry_at);
