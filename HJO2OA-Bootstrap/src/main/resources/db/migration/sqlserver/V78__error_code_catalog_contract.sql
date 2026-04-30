-- V78: align error-code table with the domain contract and seed shared codes.

UPDATE dbo.infra_error_code_def
SET message_key = code
WHERE message_key IS NULL;

ALTER TABLE dbo.infra_error_code_def
    ALTER COLUMN category NVARCHAR(64) NULL;

ALTER TABLE dbo.infra_error_code_def
    ALTER COLUMN message_key NVARCHAR(256) NOT NULL;

DECLARE @error_codes TABLE (
    code NVARCHAR(128) NOT NULL,
    module_code NVARCHAR(64) NOT NULL,
    category NVARCHAR(64) NULL,
    severity NVARCHAR(32) NOT NULL,
    http_status INT NOT NULL,
    message_key NVARCHAR(256) NOT NULL,
    retryable BIT NOT NULL
);

INSERT INTO @error_codes (code, module_code, category, severity, http_status, message_key, retryable)
VALUES
    ('BAD_REQUEST', 'shared', 'web', 'WARN', 400, 'shared.bad_request', 0),
    ('VALIDATION_ERROR', 'shared', 'web', 'WARN', 400, 'shared.validation_error', 0),
    ('UNAUTHORIZED', 'shared', 'security', 'WARN', 401, 'shared.unauthorized', 0),
    ('FORBIDDEN', 'shared', 'security', 'WARN', 403, 'shared.forbidden', 0),
    ('TENANT_REQUIRED', 'shared', 'tenant', 'WARN', 400, 'shared.tenant_required', 0),
    ('TENANT_ACCESS_DENIED', 'shared', 'tenant', 'WARN', 403, 'shared.tenant_access_denied', 0),
    ('RESOURCE_NOT_FOUND', 'shared', 'web', 'WARN', 404, 'shared.resource_not_found', 0),
    ('CONFLICT', 'shared', 'web', 'WARN', 409, 'shared.conflict', 0),
    ('IDEMPOTENCY_CONFLICT', 'shared', 'web', 'WARN', 409, 'shared.idempotency_conflict', 0),
    ('BUSINESS_RULE_VIOLATION', 'shared', 'web', 'WARN', 422, 'shared.business_rule_violation', 0),
    ('INTERNAL_ERROR', 'shared', 'web', 'ERROR', 500, 'shared.internal_error', 1),
    ('SERVICE_UNAVAILABLE', 'shared', 'web', 'ERROR', 503, 'shared.service_unavailable', 1),
    ('INFRA_ERROR_CODE_NOT_FOUND', 'infra', 'error-code', 'WARN', 404, 'infra.error_code.not_found', 0),
    ('INFRA_I18N_MESSAGE_NOT_FOUND', 'infra', 'i18n', 'WARN', 404, 'infra.i18n.message_not_found', 0),
    ('INFRA_TIMEZONE_INVALID', 'infra', 'timezone', 'WARN', 422, 'infra.timezone.invalid', 0),
    ('INFRA_DATA_I18N_TRANSLATION_NOT_FOUND', 'infra', 'data-i18n', 'WARN', 404, 'infra.data_i18n.not_found', 0);

INSERT INTO dbo.infra_error_code_def (
    id,
    code,
    module_code,
    category,
    severity,
    http_status,
    message_key,
    retryable,
    deprecated,
    created_at,
    updated_at
)
SELECT
    CONVERT(NVARCHAR(64), NEWID()),
    source.code,
    source.module_code,
    source.category,
    source.severity,
    source.http_status,
    source.message_key,
    source.retryable,
    0,
    SYSUTCDATETIME(),
    SYSUTCDATETIME()
FROM @error_codes source
WHERE NOT EXISTS (
    SELECT 1
    FROM dbo.infra_error_code_def existing
    WHERE existing.code = source.code
);
