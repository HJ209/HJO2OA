-- ============================================================
-- V12: Error code module tables
-- Entity source: ErrorCodeDefinitionEntity
-- PK type: UNIQUEIDENTIFIER-compatible string payload
-- ============================================================

CREATE TABLE dbo.infra_error_code_def (
    id           NVARCHAR(64)   NOT NULL PRIMARY KEY,
    code         NVARCHAR(64)   NOT NULL,
    module_code  NVARCHAR(32)   NOT NULL,
    category     NVARCHAR(64)   NULL,
    severity     NVARCHAR(16)   NOT NULL,
    http_status  INT            NOT NULL,
    message_key  NVARCHAR(256)  NOT NULL,
    retryable    BIT            NOT NULL CONSTRAINT DF_infra_error_code_def_retryable DEFAULT ((0)),
    deprecated   BIT            NOT NULL CONSTRAINT DF_infra_error_code_def_deprecated DEFAULT ((0)),
    created_at   DATETIME2(3)   NOT NULL CONSTRAINT DF_infra_error_code_def_created_at DEFAULT (SYSUTCDATETIME()),
    updated_at   DATETIME2(3)   NOT NULL CONSTRAINT DF_infra_error_code_def_updated_at DEFAULT (SYSUTCDATETIME()),
    CONSTRAINT CK_infra_error_code_def_http_status
        CHECK (http_status BETWEEN 100 AND 599),
    CONSTRAINT CK_infra_error_code_def_severity
        CHECK (severity IN ('INFO', 'WARN', 'ERROR', 'FATAL'))
);

CREATE UNIQUE INDEX UX_infra_error_code_def_code
    ON dbo.infra_error_code_def (code);

CREATE INDEX IX_infra_error_code_def_module_severity
    ON dbo.infra_error_code_def (module_code, severity, code);
