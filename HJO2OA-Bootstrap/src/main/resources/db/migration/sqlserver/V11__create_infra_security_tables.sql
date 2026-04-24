-- ============================================================
-- V11: Infrastructure security module tables
-- Entity source: SecurityPolicyEntity, SecretKeyMaterialEntity,
--                MaskingRuleEntity, RateLimitRuleEntity
-- PK type: UNIQUEIDENTIFIER
-- ============================================================

CREATE TABLE dbo.infra_security_policy (
    id              UNIQUEIDENTIFIER NOT NULL PRIMARY KEY,
    policy_code     NVARCHAR(64)     NOT NULL,
    policy_type     NVARCHAR(32)     NOT NULL,
    name            NVARCHAR(128)    NOT NULL,
    status          NVARCHAR(32)     NOT NULL,
    tenant_id       UNIQUEIDENTIFIER NULL,
    config_snapshot NVARCHAR(MAX)    NOT NULL,
    created_at      DATETIME2(3)     NOT NULL CONSTRAINT DF_infra_security_policy_created_at DEFAULT (SYSUTCDATETIME()),
    updated_at      DATETIME2(3)     NOT NULL CONSTRAINT DF_infra_security_policy_updated_at DEFAULT (SYSUTCDATETIME())
);

CREATE UNIQUE INDEX UX_infra_security_policy_policy_code
    ON dbo.infra_security_policy (policy_code);

CREATE INDEX IX_infra_security_policy_type_status
    ON dbo.infra_security_policy (policy_type, status);

CREATE TABLE dbo.infra_secret_key_material (
    id                 UNIQUEIDENTIFIER NOT NULL PRIMARY KEY,
    security_policy_id UNIQUEIDENTIFIER NOT NULL,
    key_ref            NVARCHAR(128)    NOT NULL,
    algorithm          NVARCHAR(64)     NOT NULL,
    key_status         NVARCHAR(32)     NOT NULL,
    rotated_at         DATETIME2(3)     NULL,
    CONSTRAINT FK_infra_secret_key_material_policy
        FOREIGN KEY (security_policy_id) REFERENCES dbo.infra_security_policy(id)
);

CREATE INDEX IX_infra_secret_key_material_policy
    ON dbo.infra_secret_key_material (security_policy_id, key_status);

CREATE UNIQUE INDEX UX_infra_secret_key_material_policy_key_ref
    ON dbo.infra_secret_key_material (security_policy_id, key_ref);

CREATE TABLE dbo.infra_masking_rule (
    id                 UNIQUEIDENTIFIER NOT NULL PRIMARY KEY,
    security_policy_id UNIQUEIDENTIFIER NOT NULL,
    data_type          NVARCHAR(64)     NOT NULL,
    rule_expr          NVARCHAR(256)    NOT NULL,
    active             BIT              NOT NULL CONSTRAINT DF_infra_masking_rule_active DEFAULT ((1)),
    CONSTRAINT FK_infra_masking_rule_policy
        FOREIGN KEY (security_policy_id) REFERENCES dbo.infra_security_policy(id)
);

CREATE INDEX IX_infra_masking_rule_policy_data_type
    ON dbo.infra_masking_rule (security_policy_id, data_type, active);

CREATE TABLE dbo.infra_rate_limit_rule (
    id                 UNIQUEIDENTIFIER NOT NULL PRIMARY KEY,
    security_policy_id UNIQUEIDENTIFIER NOT NULL,
    subject_type       NVARCHAR(32)     NOT NULL,
    window_seconds     INT              NOT NULL,
    max_requests       INT              NOT NULL,
    active             BIT              NOT NULL CONSTRAINT DF_infra_rate_limit_rule_active DEFAULT ((1)),
    CONSTRAINT FK_infra_rate_limit_rule_policy
        FOREIGN KEY (security_policy_id) REFERENCES dbo.infra_security_policy(id)
);

CREATE INDEX IX_infra_rate_limit_rule_policy_subject_type
    ON dbo.infra_rate_limit_rule (security_policy_id, subject_type, active);
