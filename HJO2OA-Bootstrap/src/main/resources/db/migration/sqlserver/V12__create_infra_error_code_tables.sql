-- ============================================================
-- V12: Infrastructure error-code module tables
-- Entity source: ErrorCodeDefinitionEntity
-- PK type: NVARCHAR(64) (String id)
-- ============================================================

CREATE TABLE dbo.infra_error_code_def (
    id             NVARCHAR(64)     NOT NULL PRIMARY KEY,
    code           NVARCHAR(128)    NOT NULL,
    module_code    NVARCHAR(64)     NOT NULL,
    category       NVARCHAR(64)     NOT NULL,
    severity       NVARCHAR(32)     NOT NULL,
    http_status    INT              NOT NULL,
    message_key    NVARCHAR(256)    NULL,
    retryable      BIT              NOT NULL CONSTRAINT DF_infra_error_code_def_retryable DEFAULT (0),
    deprecated     BIT              NOT NULL CONSTRAINT DF_infra_error_code_def_deprecated DEFAULT (0),
    created_at     DATETIME2(6)     NOT NULL CONSTRAINT DF_infra_error_code_def_created_at DEFAULT (SYSUTCDATETIME()),
    updated_at     DATETIME2(6)     NOT NULL CONSTRAINT DF_infra_error_code_def_updated_at DEFAULT (SYSUTCDATETIME())
);

CREATE UNIQUE INDEX IX_infra_error_code_def_code
    ON dbo.infra_error_code_def (code);
CREATE INDEX IX_infra_error_code_def_module_category
    ON dbo.infra_error_code_def (module_code, category);
CREATE INDEX IX_infra_error_code_def_severity
    ON dbo.infra_error_code_def (severity);
