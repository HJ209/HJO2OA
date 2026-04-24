-- ============================================================
-- V5: OpenApi module tables
-- Entity source: OpenApiEndpointEntity, ApiRateLimitPolicyEntity,
--               ApiQuotaUsageCounterEntity, ApiInvocationAuditLogEntity,
--               ApiCredentialGrantEntity
-- PK type: NVARCHAR(64) (IdType.INPUT, String id)
-- ============================================================

CREATE TABLE dbo.data_open_api_endpoint (
    id                  NVARCHAR(64)   NOT NULL PRIMARY KEY,
    tenant_id           NVARCHAR(64)   NOT NULL,
    code                NVARCHAR(64)   NOT NULL,
    name                NVARCHAR(128)  NOT NULL,
    data_service_id     NVARCHAR(64)   NULL,
    data_service_code   NVARCHAR(64)   NULL,
    data_service_name   NVARCHAR(128)  NULL,
    path                NVARCHAR(256)  NOT NULL,
    http_method         NVARCHAR(16)   NOT NULL,
    version             NVARCHAR(32)   NOT NULL,
    auth_type           NVARCHAR(32)   NOT NULL,
    compatibility_notes NVARCHAR(512)  NULL,
    status              NVARCHAR(32)   NOT NULL,
    published_at        DATETIME2(6)   NULL,
    deprecated_at       DATETIME2(6)   NULL,
    sunset_at           DATETIME2(6)   NULL,
    created_at          DATETIME2(6)   NOT NULL CONSTRAINT DF_data_open_api_endpoint_created_at DEFAULT (SYSUTCDATETIME()),
    updated_at          DATETIME2(6)   NOT NULL CONSTRAINT DF_data_open_api_endpoint_updated_at DEFAULT (SYSUTCDATETIME())
);

CREATE UNIQUE INDEX UX_data_open_api_endpoint_tenant_code
    ON dbo.data_open_api_endpoint (tenant_id, code);
CREATE INDEX IX_data_open_api_endpoint_status
    ON dbo.data_open_api_endpoint (status, http_method);

CREATE TABLE dbo.data_api_rate_limit_policy (
    id           NVARCHAR(64)   NOT NULL PRIMARY KEY,
    open_api_id  NVARCHAR(64)   NOT NULL,
    tenant_id    NVARCHAR(64)   NOT NULL,
    policy_code  NVARCHAR(64)   NOT NULL,
    client_code  NVARCHAR(64)   NULL,
    policy_type  NVARCHAR(32)   NOT NULL,
    window_value BIGINT         NOT NULL,
    window_unit  NVARCHAR(16)   NOT NULL,
    threshold    BIGINT         NOT NULL,
    status       NVARCHAR(32)   NOT NULL,
    description  NVARCHAR(512)  NULL,
    created_at   DATETIME2(6)   NOT NULL CONSTRAINT DF_data_api_rate_limit_policy_created_at DEFAULT (SYSUTCDATETIME()),
    updated_at   DATETIME2(6)   NOT NULL CONSTRAINT DF_data_api_rate_limit_policy_updated_at DEFAULT (SYSUTCDATETIME()),
    CONSTRAINT FK_data_api_rate_limit_policy_endpoint
        FOREIGN KEY (open_api_id) REFERENCES dbo.data_open_api_endpoint(id)
);

CREATE UNIQUE INDEX UX_data_api_rate_limit_policy_tenant_code
    ON dbo.data_api_rate_limit_policy (tenant_id, policy_code);

CREATE TABLE dbo.data_api_quota_usage_counter (
    id                NVARCHAR(64)   NOT NULL PRIMARY KEY,
    tenant_id         NVARCHAR(64)   NOT NULL,
    policy_id         NVARCHAR(64)   NOT NULL,
    api_id            NVARCHAR(64)   NOT NULL,
    client_code       NVARCHAR(64)   NULL,
    window_started_at DATETIME2(6)   NOT NULL,
    used_count        BIGINT         NOT NULL,
    created_at        DATETIME2(6)   NOT NULL CONSTRAINT DF_data_api_quota_usage_counter_created_at DEFAULT (SYSUTCDATETIME()),
    updated_at        DATETIME2(6)   NOT NULL CONSTRAINT DF_data_api_quota_usage_counter_updated_at DEFAULT (SYSUTCDATETIME()),
    CONSTRAINT FK_data_api_quota_usage_counter_policy
        FOREIGN KEY (policy_id) REFERENCES dbo.data_api_rate_limit_policy(id)
);

CREATE INDEX IX_data_api_quota_usage_counter_window
    ON dbo.data_api_quota_usage_counter (policy_id, api_id, window_started_at);

CREATE TABLE dbo.data_api_invocation_audit_log (
    id               NVARCHAR(64)    NOT NULL PRIMARY KEY,
    request_id       NVARCHAR(64)    NOT NULL,
    tenant_id        NVARCHAR(64)    NOT NULL,
    api_id           NVARCHAR(64)    NOT NULL,
    endpoint_code    NVARCHAR(64)    NULL,
    endpoint_version NVARCHAR(32)    NULL,
    path             NVARCHAR(256)   NOT NULL,
    http_method      NVARCHAR(16)    NOT NULL,
    client_code      NVARCHAR(64)    NULL,
    auth_type        NVARCHAR(32)    NOT NULL,
    outcome          NVARCHAR(32)    NOT NULL,
    response_status  INT             NOT NULL,
    error_code       NVARCHAR(64)    NULL,
    duration_ms      BIGINT          NOT NULL,
    request_digest   NVARCHAR(256)   NULL,
    remote_ip        NVARCHAR(64)    NULL,
    occurred_at      DATETIME2(6)    NOT NULL,
    abnormal_flag    BIT             NOT NULL CONSTRAINT DF_data_api_invocation_audit_log_abnormal DEFAULT ((0)),
    review_conclusion NVARCHAR(32)   NULL,
    note             NVARCHAR(512)   NULL,
    reviewed_by      NVARCHAR(64)    NULL,
    reviewed_at      DATETIME2(6)    NULL
);

CREATE UNIQUE INDEX UX_data_api_invocation_audit_log_request_id
    ON dbo.data_api_invocation_audit_log (request_id);
CREATE INDEX IX_data_api_invocation_audit_log_tenant_occurred
    ON dbo.data_api_invocation_audit_log (tenant_id, occurred_at DESC);
CREATE INDEX IX_data_api_invocation_audit_log_api_occurred
    ON dbo.data_api_invocation_audit_log (api_id, occurred_at DESC);

CREATE TABLE dbo.data_api_credential_grant (
    id           NVARCHAR(64)   NOT NULL PRIMARY KEY,
    open_api_id  NVARCHAR(64)   NOT NULL,
    tenant_id    NVARCHAR(64)   NOT NULL,
    client_code  NVARCHAR(64)   NOT NULL,
    secret_ref   NVARCHAR(256)  NOT NULL,
    scopes       NVARCHAR(MAX)  NULL,
    expires_at   DATETIME2(6)   NULL,
    status       NVARCHAR(32)   NOT NULL,
    created_at   DATETIME2(6)   NOT NULL CONSTRAINT DF_data_api_credential_grant_created_at DEFAULT (SYSUTCDATETIME()),
    updated_at   DATETIME2(6)   NOT NULL CONSTRAINT DF_data_api_credential_grant_updated_at DEFAULT (SYSUTCDATETIME()),
    CONSTRAINT FK_data_api_credential_grant_endpoint
        FOREIGN KEY (open_api_id) REFERENCES dbo.data_open_api_endpoint(id)
);

CREATE UNIQUE INDEX UX_data_api_credential_grant_api_client
    ON dbo.data_api_credential_grant (open_api_id, client_code);
