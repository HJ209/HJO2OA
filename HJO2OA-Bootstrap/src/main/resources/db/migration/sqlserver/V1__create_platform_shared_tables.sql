IF OBJECT_ID(N'dbo.sys_config', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.sys_config (
        id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        config_key NVARCHAR(128) NOT NULL,
        config_value NVARCHAR(MAX) NULL,
        description NVARCHAR(512) NULL,
        enabled BIT NOT NULL CONSTRAINT DF_sys_config_enabled DEFAULT ((1)),
        deleted BIT NOT NULL CONSTRAINT DF_sys_config_deleted DEFAULT ((0)),
        created_at DATETIME2(3) NOT NULL CONSTRAINT DF_sys_config_created_at DEFAULT (SYSUTCDATETIME()),
        updated_at DATETIME2(3) NOT NULL CONSTRAINT DF_sys_config_updated_at DEFAULT (SYSUTCDATETIME())
    );
END;

IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = N'UX_sys_config_config_key'
      AND object_id = OBJECT_ID(N'dbo.sys_config')
)
BEGIN
    CREATE UNIQUE INDEX UX_sys_config_config_key
        ON dbo.sys_config (config_key);
END;

IF OBJECT_ID(N'dbo.sys_dictionary', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.sys_dictionary (
        id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        dictionary_type NVARCHAR(128) NOT NULL,
        dictionary_key NVARCHAR(128) NOT NULL,
        display_name NVARCHAR(256) NOT NULL,
        dictionary_value NVARCHAR(512) NULL,
        parent_id BIGINT NULL,
        sort_order INT NOT NULL CONSTRAINT DF_sys_dictionary_sort_order DEFAULT ((0)),
        enabled BIT NOT NULL CONSTRAINT DF_sys_dictionary_enabled DEFAULT ((1)),
        deleted BIT NOT NULL CONSTRAINT DF_sys_dictionary_deleted DEFAULT ((0)),
        created_at DATETIME2(3) NOT NULL CONSTRAINT DF_sys_dictionary_created_at DEFAULT (SYSUTCDATETIME()),
        updated_at DATETIME2(3) NOT NULL CONSTRAINT DF_sys_dictionary_updated_at DEFAULT (SYSUTCDATETIME())
    );
END;

IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = N'UX_sys_dictionary_type_key'
      AND object_id = OBJECT_ID(N'dbo.sys_dictionary')
)
BEGIN
    CREATE UNIQUE INDEX UX_sys_dictionary_type_key
        ON dbo.sys_dictionary (dictionary_type, dictionary_key);
END;
