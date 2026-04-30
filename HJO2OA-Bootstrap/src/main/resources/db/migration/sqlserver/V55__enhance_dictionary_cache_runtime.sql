-- ============================================================
-- V55: Dictionary runtime cache and system dictionary metadata
-- ============================================================

IF COL_LENGTH('dbo.infra_dictionary_type', 'sort_order') IS NULL
BEGIN
    ALTER TABLE dbo.infra_dictionary_type
        ADD sort_order INT NOT NULL
            CONSTRAINT DF_infra_dictionary_type_sort_order DEFAULT 0;
END;

IF COL_LENGTH('dbo.infra_dictionary_type', 'system_managed') IS NULL
BEGIN
    ALTER TABLE dbo.infra_dictionary_type
        ADD system_managed BIT NOT NULL
            CONSTRAINT DF_infra_dictionary_type_system_managed DEFAULT 0;
END;

IF COL_LENGTH('dbo.infra_dictionary_item', 'default_item') IS NULL
BEGIN
    ALTER TABLE dbo.infra_dictionary_item
        ADD default_item BIT NOT NULL
            CONSTRAINT DF_infra_dictionary_item_default_item DEFAULT 0;
END;

IF COL_LENGTH('dbo.infra_dictionary_item', 'extension_json') IS NULL
BEGIN
    ALTER TABLE dbo.infra_dictionary_item
        ADD extension_json NVARCHAR(MAX) NULL;
END;

IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = 'IX_infra_dictionary_type_tenant_sort'
      AND object_id = OBJECT_ID('dbo.infra_dictionary_type')
)
BEGIN
    EXEC(N'CREATE INDEX IX_infra_dictionary_type_tenant_sort
        ON dbo.infra_dictionary_type (tenant_id, sort_order, code)
        INCLUDE (status, category, system_managed)');
END;

IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = 'IX_infra_dictionary_item_default'
      AND object_id = OBJECT_ID('dbo.infra_dictionary_item')
)
BEGIN
    EXEC(N'CREATE INDEX IX_infra_dictionary_item_default
        ON dbo.infra_dictionary_item (dictionary_type_id, default_item, enabled)');
END;

IF NOT EXISTS (SELECT 1 FROM dbo.infra_cache_policy WHERE namespace = N'infra.dictionary.runtime')
BEGIN
    INSERT INTO dbo.infra_cache_policy (
        id,
        namespace,
        backend_type,
        ttl_seconds,
        max_capacity,
        eviction_policy,
        invalidation_mode,
        metrics_enabled,
        active,
        created_at,
        updated_at
    )
    VALUES (
        NEWID(),
        N'infra.dictionary.runtime',
        N'HYBRID',
        300,
        10000,
        N'LRU',
        N'EVENT_DRIVEN',
        1,
        1,
        SYSUTCDATETIME(),
        SYSUTCDATETIME()
    );
END;

IF NOT EXISTS (SELECT 1 FROM dbo.infra_cache_policy WHERE namespace = N'infra.config.runtime')
BEGIN
    INSERT INTO dbo.infra_cache_policy (
        id,
        namespace,
        backend_type,
        ttl_seconds,
        max_capacity,
        eviction_policy,
        invalidation_mode,
        metrics_enabled,
        active,
        created_at,
        updated_at
    )
    VALUES (
        NEWID(),
        N'infra.config.runtime',
        N'HYBRID',
        300,
        10000,
        N'LRU',
        N'EVENT_DRIVEN',
        1,
        1,
        SYSUTCDATETIME(),
        SYSUTCDATETIME()
    );
END;

IF NOT EXISTS (SELECT 1 FROM dbo.infra_cache_policy WHERE namespace = N'infra.feature-flag.runtime')
BEGIN
    INSERT INTO dbo.infra_cache_policy (
        id,
        namespace,
        backend_type,
        ttl_seconds,
        max_capacity,
        eviction_policy,
        invalidation_mode,
        metrics_enabled,
        active,
        created_at,
        updated_at
    )
    VALUES (
        NEWID(),
        N'infra.feature-flag.runtime',
        N'HYBRID',
        300,
        10000,
        N'LRU',
        N'EVENT_DRIVEN',
        1,
        1,
        SYSUTCDATETIME(),
        SYSUTCDATETIME()
    );
END;
