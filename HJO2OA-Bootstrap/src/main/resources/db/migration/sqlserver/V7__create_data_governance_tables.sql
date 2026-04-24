-- ============================================================
-- V7: Governance module tables
-- Entity source: GovernancePersistenceEntities (8 tables)
-- PK type: NVARCHAR(64) (IdType.INPUT, String xxxId)
-- ============================================================

CREATE TABLE dbo.data_governance_profile (
    governance_id    NVARCHAR(64)   NOT NULL PRIMARY KEY,
    code             NVARCHAR(64)   NOT NULL,
    scope_type       NVARCHAR(32)   NOT NULL,
    target_code      NVARCHAR(64)   NOT NULL,
    sla_policy_json  NVARCHAR(MAX)  NULL,
    alert_policy_json NVARCHAR(MAX) NULL,
    status           NVARCHAR(32)   NOT NULL,
    tenant_id        NVARCHAR(64)   NOT NULL,
    revision         BIGINT         NOT NULL CONSTRAINT DF_data_governance_profile_revision DEFAULT 0,
    deleted          INT            NOT NULL CONSTRAINT DF_data_governance_profile_deleted DEFAULT 0,
    created_at       DATETIME2(6)   NOT NULL,
    updated_at       DATETIME2(6)   NOT NULL
);

CREATE UNIQUE INDEX UX_data_governance_profile_tenant_code
    ON dbo.data_governance_profile (tenant_id, code) WHERE deleted = 0;
CREATE INDEX IX_data_governance_profile_target
    ON dbo.data_governance_profile (tenant_id, scope_type, target_code) WHERE deleted = 0;

CREATE TABLE dbo.data_health_check_rule (
    rule_id              NVARCHAR(64)    NOT NULL PRIMARY KEY,
    governance_id        NVARCHAR(64)    NOT NULL,
    rule_code            NVARCHAR(64)    NOT NULL,
    rule_name            NVARCHAR(128)   NOT NULL,
    check_type           NVARCHAR(32)    NOT NULL,
    severity             NVARCHAR(32)    NOT NULL,
    status               NVARCHAR(32)    NOT NULL,
    metric_name          NVARCHAR(64)    NOT NULL,
    comparison_operator  NVARCHAR(32)    NOT NULL,
    threshold_value      DECIMAL(18,4)   NOT NULL,
    window_minutes       INT             NULL,
    dedup_minutes        INT             NULL,
    schedule_expression  NVARCHAR(128)   NULL,
    strategy_json        NVARCHAR(MAX)   NULL,
    revision             BIGINT          NOT NULL CONSTRAINT DF_data_health_check_rule_revision DEFAULT 0,
    deleted              INT             NOT NULL CONSTRAINT DF_data_health_check_rule_deleted DEFAULT 0,
    created_at           DATETIME2(6)    NOT NULL,
    updated_at           DATETIME2(6)    NOT NULL,
    CONSTRAINT FK_data_health_check_rule_profile
        FOREIGN KEY (governance_id) REFERENCES dbo.data_governance_profile(governance_id)
);

CREATE UNIQUE INDEX UX_data_health_check_rule_code
    ON dbo.data_health_check_rule (governance_id, rule_code) WHERE deleted = 0;
CREATE INDEX IX_data_health_check_rule_status
    ON dbo.data_health_check_rule (status, severity) WHERE deleted = 0;

CREATE TABLE dbo.data_alert_rule (
    rule_id                NVARCHAR(64)    NOT NULL PRIMARY KEY,
    governance_id          NVARCHAR(64)    NOT NULL,
    rule_code              NVARCHAR(64)    NOT NULL,
    rule_name              NVARCHAR(128)   NOT NULL,
    source_rule_code       NVARCHAR(64)    NULL,
    metric_name            NVARCHAR(64)    NULL,
    alert_type             NVARCHAR(64)    NOT NULL,
    alert_level            NVARCHAR(32)    NOT NULL,
    status                 NVARCHAR(32)    NOT NULL,
    comparison_operator    NVARCHAR(32)    NOT NULL,
    threshold_value        DECIMAL(18,4)   NOT NULL,
    dedup_minutes          INT             NULL,
    escalation_minutes     INT             NULL,
    notification_policy_json NVARCHAR(MAX) NULL,
    strategy_json          NVARCHAR(MAX)   NULL,
    revision               BIGINT          NOT NULL CONSTRAINT DF_data_alert_rule_revision DEFAULT 0,
    deleted                INT             NOT NULL CONSTRAINT DF_data_alert_rule_deleted DEFAULT 0,
    created_at             DATETIME2(6)    NOT NULL,
    updated_at             DATETIME2(6)    NOT NULL,
    CONSTRAINT FK_data_alert_rule_profile
        FOREIGN KEY (governance_id) REFERENCES dbo.data_governance_profile(governance_id)
);

CREATE UNIQUE INDEX UX_data_alert_rule_code
    ON dbo.data_alert_rule (governance_id, rule_code) WHERE deleted = 0;
CREATE INDEX IX_data_alert_rule_status
    ON dbo.data_alert_rule (status, alert_level) WHERE deleted = 0;

CREATE TABLE dbo.data_service_version_record (
    version_record_id  NVARCHAR(64)    NOT NULL PRIMARY KEY,
    governance_id      NVARCHAR(64)    NOT NULL,
    target_type        NVARCHAR(32)    NOT NULL,
    target_code        NVARCHAR(64)    NOT NULL,
    version_no         NVARCHAR(32)    NOT NULL,
    compatibility_note NVARCHAR(512)   NULL,
    change_summary     NVARCHAR(1000)  NULL,
    status             NVARCHAR(32)    NOT NULL,
    registered_at      DATETIME2(6)    NOT NULL,
    published_at       DATETIME2(6)    NULL,
    deprecated_at      DATETIME2(6)    NULL,
    operator_id        NVARCHAR(64)    NULL,
    approval_note      NVARCHAR(512)   NULL,
    audit_trace_id     NVARCHAR(64)    NULL,
    revision           BIGINT          NOT NULL CONSTRAINT DF_data_service_version_record_revision DEFAULT 0,
    deleted            INT             NOT NULL CONSTRAINT DF_data_service_version_record_deleted DEFAULT 0,
    created_at         DATETIME2(6)    NOT NULL,
    updated_at         DATETIME2(6)    NOT NULL,
    CONSTRAINT FK_data_service_version_record_profile
        FOREIGN KEY (governance_id) REFERENCES dbo.data_governance_profile(governance_id)
);

CREATE UNIQUE INDEX UX_data_service_version_record_code
    ON dbo.data_service_version_record (governance_id, version_no) WHERE deleted = 0;
CREATE INDEX IX_data_service_version_record_status
    ON dbo.data_service_version_record (target_type, target_code, status) WHERE deleted = 0;

CREATE TABLE dbo.data_governance_runtime_signal (
    signal_id          NVARCHAR(64)    NOT NULL PRIMARY KEY,
    tenant_id          NVARCHAR(64)    NOT NULL,
    target_type        NVARCHAR(32)    NOT NULL,
    target_code        NVARCHAR(64)    NOT NULL,
    runtime_status     NVARCHAR(32)    NOT NULL,
    total_executions   BIGINT          NOT NULL,
    failure_count      BIGINT          NOT NULL,
    failure_rate       DECIMAL(18,4)   NOT NULL,
    last_duration_ms   BIGINT          NULL,
    freshness_lag_seconds BIGINT       NULL,
    last_success_at    DATETIME2(6)    NULL,
    last_failure_at    DATETIME2(6)    NULL,
    last_error_code    NVARCHAR(64)    NULL,
    last_error_message NVARCHAR(1000)  NULL,
    last_event_type    NVARCHAR(64)    NULL,
    last_execution_id  NVARCHAR(64)    NULL,
    trace_id           NVARCHAR(64)    NULL,
    payload_json       NVARCHAR(MAX)   NULL,
    revision           BIGINT          NOT NULL CONSTRAINT DF_data_governance_runtime_signal_revision DEFAULT 0,
    deleted            INT             NOT NULL CONSTRAINT DF_data_governance_runtime_signal_deleted DEFAULT 0,
    created_at         DATETIME2(6)    NOT NULL,
    updated_at         DATETIME2(6)    NOT NULL
);

CREATE UNIQUE INDEX UX_data_governance_runtime_signal_target
    ON dbo.data_governance_runtime_signal (tenant_id, target_type, target_code) WHERE deleted = 0;

CREATE TABLE dbo.data_governance_health_snapshot (
    snapshot_id     NVARCHAR(64)    NOT NULL PRIMARY KEY,
    governance_id   NVARCHAR(64)    NOT NULL,
    rule_id         NVARCHAR(64)    NOT NULL,
    target_type     NVARCHAR(32)    NOT NULL,
    target_code     NVARCHAR(64)    NOT NULL,
    rule_code       NVARCHAR(64)    NOT NULL,
    health_status   NVARCHAR(32)    NOT NULL,
    measured_value  DECIMAL(18,4)   NOT NULL,
    threshold_value DECIMAL(18,4)   NOT NULL,
    summary         NVARCHAR(512)   NOT NULL,
    trace_id        NVARCHAR(64)    NULL,
    checked_at      DATETIME2(6)    NOT NULL,
    deleted         INT             NOT NULL CONSTRAINT DF_data_governance_health_snapshot_deleted DEFAULT 0
);

CREATE INDEX IX_data_governance_health_snapshot_rule
    ON dbo.data_governance_health_snapshot (governance_id, rule_id, checked_at DESC) WHERE deleted = 0;
CREATE INDEX IX_data_governance_health_snapshot_target
    ON dbo.data_governance_health_snapshot (target_type, target_code, checked_at DESC) WHERE deleted = 0;

CREATE TABLE dbo.data_governance_alert_record (
    alert_id         NVARCHAR(64)    NOT NULL PRIMARY KEY,
    governance_id    NVARCHAR(64)    NOT NULL,
    rule_id          NVARCHAR(64)    NOT NULL,
    target_type      NVARCHAR(32)    NOT NULL,
    target_code      NVARCHAR(64)    NOT NULL,
    alert_level      NVARCHAR(32)    NOT NULL,
    alert_type       NVARCHAR(64)    NOT NULL,
    status           NVARCHAR(32)    NOT NULL,
    alert_key        NVARCHAR(200)   NOT NULL,
    summary          NVARCHAR(512)   NOT NULL,
    detail           NVARCHAR(MAX)   NULL,
    trace_id         NVARCHAR(64)    NULL,
    occurred_at      DATETIME2(6)    NOT NULL,
    acknowledged_at  DATETIME2(6)    NULL,
    acknowledged_by  NVARCHAR(64)    NULL,
    escalated_at     DATETIME2(6)    NULL,
    escalated_by     NVARCHAR(64)    NULL,
    closed_at        DATETIME2(6)    NULL,
    closed_by        NVARCHAR(64)    NULL,
    close_reason     NVARCHAR(512)   NULL,
    deleted          INT             NOT NULL CONSTRAINT DF_data_governance_alert_record_deleted DEFAULT 0
);

CREATE INDEX IX_data_governance_alert_record_status
    ON dbo.data_governance_alert_record (status, alert_level, occurred_at DESC) WHERE deleted = 0;
CREATE INDEX IX_data_governance_alert_record_target
    ON dbo.data_governance_alert_record (target_type, target_code, occurred_at DESC) WHERE deleted = 0;
CREATE INDEX IX_data_governance_alert_record_key
    ON dbo.data_governance_alert_record (alert_key) WHERE deleted = 0;

CREATE TABLE dbo.data_governance_trace_record (
    trace_id            NVARCHAR(64)    NOT NULL PRIMARY KEY,
    governance_id       NVARCHAR(64)    NOT NULL,
    target_type         NVARCHAR(32)    NOT NULL,
    target_code         NVARCHAR(64)    NOT NULL,
    trace_type          NVARCHAR(32)    NOT NULL,
    status              NVARCHAR(32)    NOT NULL,
    source_event_type   NVARCHAR(64)    NULL,
    source_execution_id NVARCHAR(64)    NULL,
    correlation_id      NVARCHAR(64)    NULL,
    summary             NVARCHAR(512)   NOT NULL,
    detail              NVARCHAR(MAX)   NULL,
    opened_at           DATETIME2(6)    NOT NULL,
    updated_at          DATETIME2(6)    NOT NULL,
    resolved_at         DATETIME2(6)    NULL,
    deleted             INT             NOT NULL CONSTRAINT DF_data_governance_trace_record_deleted DEFAULT 0
);

CREATE INDEX IX_data_governance_trace_record_target
    ON dbo.data_governance_trace_record (target_type, target_code, updated_at DESC) WHERE deleted = 0;
CREATE INDEX IX_data_governance_trace_record_status
    ON dbo.data_governance_trace_record (status, trace_type, opened_at DESC) WHERE deleted = 0;

CREATE TABLE dbo.data_governance_action_audit (
    audit_id        NVARCHAR(64)    NOT NULL PRIMARY KEY,
    governance_id   NVARCHAR(64)    NOT NULL,
    target_type     NVARCHAR(32)    NOT NULL,
    target_code     NVARCHAR(64)    NOT NULL,
    action_type     NVARCHAR(64)    NOT NULL,
    action_result   NVARCHAR(32)    NOT NULL,
    operator_id     NVARCHAR(64)    NOT NULL,
    operator_name   NVARCHAR(128)   NULL,
    reason          NVARCHAR(512)   NULL,
    request_id      NVARCHAR(64)    NOT NULL,
    payload_json    NVARCHAR(MAX)   NULL,
    result_message  NVARCHAR(512)   NULL,
    trace_id        NVARCHAR(64)    NULL,
    created_at      DATETIME2(6)    NOT NULL,
    completed_at    DATETIME2(6)    NULL,
    deleted         INT             NOT NULL CONSTRAINT DF_data_governance_action_audit_deleted DEFAULT 0
);

CREATE UNIQUE INDEX UX_data_governance_action_audit_request_id
    ON dbo.data_governance_action_audit (request_id) WHERE deleted = 0;
CREATE INDEX IX_data_governance_action_audit_target
    ON dbo.data_governance_action_audit (target_type, target_code, created_at DESC) WHERE deleted = 0;
