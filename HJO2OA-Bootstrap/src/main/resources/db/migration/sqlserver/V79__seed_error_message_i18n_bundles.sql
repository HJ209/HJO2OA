-- V79: seed localized messages for shared and infrastructure error codes.

DECLARE @error_message_bundles TABLE (
    locale NVARCHAR(16) NOT NULL,
    fallback_locale NVARCHAR(16) NULL
);

INSERT INTO @error_message_bundles (locale, fallback_locale)
VALUES
    ('zh-cn', 'en-us'),
    ('en-us', NULL);

INSERT INTO dbo.infra_locale_bundle (
    id,
    bundle_code,
    module_code,
    locale,
    fallback_locale,
    status,
    tenant_id,
    created_at,
    updated_at
)
SELECT
    NEWID(),
    'error.messages',
    'infra',
    source.locale,
    source.fallback_locale,
    'ACTIVE',
    NULL,
    SYSUTCDATETIME(),
    SYSUTCDATETIME()
FROM @error_message_bundles source
WHERE NOT EXISTS (
    SELECT 1
    FROM dbo.infra_locale_bundle existing
    WHERE existing.bundle_code = 'error.messages'
      AND existing.locale = source.locale
      AND existing.tenant_id IS NULL
);

UPDATE bundle
SET
    module_code = 'infra',
    fallback_locale = source.fallback_locale,
    status = 'ACTIVE',
    updated_at = SYSUTCDATETIME()
FROM dbo.infra_locale_bundle bundle
JOIN @error_message_bundles source
    ON source.locale = bundle.locale
WHERE bundle.bundle_code = 'error.messages'
  AND bundle.tenant_id IS NULL;

DECLARE @error_messages TABLE (
    resource_key NVARCHAR(256) NOT NULL,
    zh_value NVARCHAR(MAX) NOT NULL,
    en_value NVARCHAR(MAX) NOT NULL
);

INSERT INTO @error_messages (resource_key, zh_value, en_value)
VALUES
    ('shared.bad_request', N'请求格式错误', N'The request is invalid.'),
    ('shared.validation_error', N'请求参数校验失败', N'The request data failed validation.'),
    ('shared.unauthorized', N'请先登录后再访问', N'Authentication is required.'),
    ('shared.forbidden', N'当前账号无权执行该操作', N'You do not have permission to perform this action.'),
    ('shared.tenant_required', N'缺少租户上下文', N'Tenant context is required.'),
    ('shared.tenant_access_denied', N'无权访问当前租户数据', N'You do not have access to the current tenant data.'),
    ('shared.resource_not_found', N'请求的资源不存在', N'The requested resource was not found.'),
    ('shared.conflict', N'资源状态冲突', N'The resource state conflicts with the request.'),
    ('shared.idempotency_conflict', N'幂等键冲突', N'The idempotency key conflicts with an existing request.'),
    ('shared.business_rule_violation', N'业务规则校验失败', N'The business rule validation failed.'),
    ('shared.internal_error', N'服务内部错误', N'An internal service error occurred.'),
    ('shared.service_unavailable', N'服务暂时不可用', N'The service is temporarily unavailable.'),
    ('infra.error_code.not_found', N'错误码定义不存在', N'The error code definition was not found.'),
    ('infra.i18n.message_not_found', N'国际化消息不存在', N'The localized message was not found.'),
    ('infra.timezone.invalid', N'时区无效或不受支持', N'The timezone is invalid or unsupported.'),
    ('infra.data_i18n.not_found', N'业务数据翻译不存在', N'The data translation was not found.');

DECLARE @zh_bundle_id UNIQUEIDENTIFIER = (
    SELECT TOP (1) id
    FROM dbo.infra_locale_bundle
    WHERE bundle_code = 'error.messages'
      AND locale = 'zh-cn'
      AND tenant_id IS NULL
);

DECLARE @en_bundle_id UNIQUEIDENTIFIER = (
    SELECT TOP (1) id
    FROM dbo.infra_locale_bundle
    WHERE bundle_code = 'error.messages'
      AND locale = 'en-us'
      AND tenant_id IS NULL
);


INSERT INTO dbo.infra_locale_resource_entry (
    id,
    locale_bundle_id,
    resource_key,
    resource_value,
    version,
    active
)
SELECT
    NEWID(),
    @zh_bundle_id,
    source.resource_key,
    source.zh_value,
    1,
    1
FROM @error_messages source
WHERE NOT EXISTS (
    SELECT 1
    FROM dbo.infra_locale_resource_entry existing
    WHERE existing.locale_bundle_id = @zh_bundle_id
      AND existing.resource_key = source.resource_key
);

INSERT INTO dbo.infra_locale_resource_entry (
    id,
    locale_bundle_id,
    resource_key,
    resource_value,
    version,
    active
)
SELECT
    NEWID(),
    @en_bundle_id,
    source.resource_key,
    source.en_value,
    1,
    1
FROM @error_messages source
WHERE NOT EXISTS (
    SELECT 1
    FROM dbo.infra_locale_resource_entry existing
    WHERE existing.locale_bundle_id = @en_bundle_id
      AND existing.resource_key = source.resource_key
);
