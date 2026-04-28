-- ============================================================
-- V34: Event outbox table for RabbitMQ domain-event delivery
-- ============================================================

CREATE TABLE dbo.event_outbox (
    id               UNIQUEIDENTIFIER NOT NULL PRIMARY KEY,
    aggregate_type   NVARCHAR(128)    NOT NULL,
    aggregate_id     NVARCHAR(128)    NULL,
    event_type       NVARCHAR(128)    NOT NULL,
    payload_json     NVARCHAR(MAX)    NOT NULL,
    status           NVARCHAR(32)     NOT NULL CONSTRAINT DF_event_outbox_status DEFAULT ('PENDING'),
    created_at       DATETIME2(6)     NOT NULL CONSTRAINT DF_event_outbox_created_at DEFAULT (SYSUTCDATETIME()),
    published_at     DATETIME2(6)     NULL,
    retry_count      INT              NOT NULL CONSTRAINT DF_event_outbox_retry_count DEFAULT (0)
);

CREATE INDEX IX_event_outbox_status_created
    ON dbo.event_outbox (status, created_at);

CREATE INDEX IX_event_outbox_event_type_created
    ON dbo.event_outbox (event_type, created_at);
