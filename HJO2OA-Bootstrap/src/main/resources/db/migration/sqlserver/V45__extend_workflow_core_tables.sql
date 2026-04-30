IF COL_LENGTH(N'dbo.proc_instance', N'business_key') IS NULL
BEGIN
    ALTER TABLE dbo.proc_instance ADD business_key NVARCHAR(128) NULL;
END;

IF COL_LENGTH(N'dbo.proc_instance', N'idempotency_key') IS NULL
BEGIN
    ALTER TABLE dbo.proc_instance ADD idempotency_key NVARCHAR(255) NULL;
END;

IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = N'uk_proc_instance_tenant_idempotency'
      AND object_id = OBJECT_ID(N'dbo.proc_instance')
)
BEGIN
    EXEC(N'CREATE UNIQUE INDEX uk_proc_instance_tenant_idempotency
        ON dbo.proc_instance (tenant_id, idempotency_key)
        WHERE idempotency_key IS NOT NULL');
END;

IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = N'idx_proc_instance_tenant_business_key'
      AND object_id = OBJECT_ID(N'dbo.proc_instance')
)
BEGIN
    EXEC(N'CREATE INDEX idx_proc_instance_tenant_business_key
        ON dbo.proc_instance (tenant_id, definition_code, business_key)
        WHERE business_key IS NOT NULL');
END;

IF COL_LENGTH(N'dbo.proc_task_action', N'tenant_id') IS NULL
BEGIN
    ALTER TABLE dbo.proc_task_action ADD tenant_id UNIQUEIDENTIFIER NULL;
END;

IF COL_LENGTH(N'dbo.proc_task_action', N'operator_account_id') IS NULL
BEGIN
    ALTER TABLE dbo.proc_task_action ADD operator_account_id UNIQUEIDENTIFIER NULL;
END;

IF COL_LENGTH(N'dbo.proc_task_action', N'request_id') IS NULL
BEGIN
    ALTER TABLE dbo.proc_task_action ADD request_id NVARCHAR(128) NULL;
END;

IF COL_LENGTH(N'dbo.proc_task_action', N'idempotency_key') IS NULL
BEGIN
    ALTER TABLE dbo.proc_task_action ADD idempotency_key NVARCHAR(255) NULL;
END;

IF COL_LENGTH(N'dbo.proc_task_action', N'target_assignee_ids') IS NULL
BEGIN
    ALTER TABLE dbo.proc_task_action ADD target_assignee_ids NVARCHAR(MAX) NULL;
END;

IF COL_LENGTH(N'dbo.proc_task_action', N'result_status') IS NULL
BEGIN
    ALTER TABLE dbo.proc_task_action ADD result_status VARCHAR(32) NOT NULL
        CONSTRAINT DF_proc_task_action_result_status DEFAULT ('SUCCESS');
END;

IF OBJECT_ID(N'dbo.proc_node_history', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.proc_node_history (
        id UNIQUEIDENTIFIER NOT NULL PRIMARY KEY,
        instance_id UNIQUEIDENTIFIER NOT NULL,
        task_id UNIQUEIDENTIFIER NULL,
        node_id VARCHAR(64) NOT NULL,
        node_name NVARCHAR(128) NOT NULL,
        node_type VARCHAR(32) NOT NULL,
        history_status VARCHAR(32) NOT NULL,
        action_code VARCHAR(64) NULL,
        operator_id UNIQUEIDENTIFIER NULL,
        occurred_at DATETIME2 NOT NULL,
        tenant_id UNIQUEIDENTIFIER NOT NULL,
        CONSTRAINT fk_proc_node_history_instance
            FOREIGN KEY (instance_id) REFERENCES dbo.proc_instance (id)
    );

    CREATE INDEX idx_proc_node_history_instance_time
        ON dbo.proc_node_history (tenant_id, instance_id, occurred_at);
END;

IF OBJECT_ID(N'dbo.proc_variable_history', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.proc_variable_history (
        id UNIQUEIDENTIFIER NOT NULL PRIMARY KEY,
        instance_id UNIQUEIDENTIFIER NOT NULL,
        task_id UNIQUEIDENTIFIER NULL,
        variable_name NVARCHAR(128) NOT NULL,
        old_value NVARCHAR(MAX) NULL,
        new_value NVARCHAR(MAX) NULL,
        operator_id UNIQUEIDENTIFIER NULL,
        occurred_at DATETIME2 NOT NULL,
        tenant_id UNIQUEIDENTIFIER NOT NULL,
        CONSTRAINT fk_proc_variable_history_instance
            FOREIGN KEY (instance_id) REFERENCES dbo.proc_instance (id)
    );

    CREATE INDEX idx_proc_variable_history_instance_time
        ON dbo.proc_variable_history (tenant_id, instance_id, occurred_at);
END;
