-- V76: seed system UTC timezone and add lookup indexes.

IF NOT EXISTS (
    SELECT 1
    FROM dbo.infra_timezone_setting
    WHERE scope_type = 'SYSTEM'
      AND scope_id IS NULL
      AND active = 1
)
BEGIN
    INSERT INTO dbo.infra_timezone_setting (
        id,
        scope_type,
        scope_id,
        timezone_id,
        is_default,
        effective_from,
        active,
        tenant_id,
        created_at,
        updated_at
    )
    VALUES (
        NEWID(),
        'SYSTEM',
        NULL,
        'UTC',
        1,
        SYSUTCDATETIME(),
        1,
        NULL,
        SYSUTCDATETIME(),
        SYSUTCDATETIME()
    );
END;

IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = 'IX_infra_timezone_setting_timezone'
      AND object_id = OBJECT_ID('dbo.infra_timezone_setting')
)
BEGIN
    CREATE INDEX IX_infra_timezone_setting_timezone
        ON dbo.infra_timezone_setting (timezone_id, active);
END;
