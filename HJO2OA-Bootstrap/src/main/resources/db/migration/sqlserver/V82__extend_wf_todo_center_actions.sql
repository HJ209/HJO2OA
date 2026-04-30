IF COL_LENGTH(N'dbo.wf_todo_item', N'tenant_id') IS NULL
BEGIN
    ALTER TABLE dbo.wf_todo_item ADD tenant_id NVARCHAR(128) NOT NULL CONSTRAINT df_wf_todo_item_tenant DEFAULT N'tenant-1';
END;

IF COL_LENGTH(N'dbo.wf_copied_todo_item', N'tenant_id') IS NULL
BEGIN
    ALTER TABLE dbo.wf_copied_todo_item ADD tenant_id NVARCHAR(128) NOT NULL CONSTRAINT df_wf_copied_todo_item_tenant DEFAULT N'tenant-1';
END;

IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = N'ix_wf_todo_item_tenant_assignee_status'
      AND object_id = OBJECT_ID(N'dbo.wf_todo_item')
)
BEGIN
    EXEC(N'CREATE INDEX ix_wf_todo_item_tenant_assignee_status
        ON dbo.wf_todo_item (tenant_id, assignee_id, status, updated_at)');
END;

IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = N'ix_wf_copied_todo_tenant_assignment'
      AND object_id = OBJECT_ID(N'dbo.wf_copied_todo_item')
)
BEGIN
    EXEC(N'CREATE INDEX ix_wf_copied_todo_tenant_assignment
        ON dbo.wf_copied_todo_item (tenant_id, recipient_assignment_id, read_status, created_at)');
END;

IF OBJECT_ID(N'dbo.wf_todo_action_log', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.wf_todo_action_log (
        idempotency_key NVARCHAR(128) NOT NULL,
        action_type NVARCHAR(64) NOT NULL,
        target_id NVARCHAR(1024) NOT NULL,
        processed_at DATETIME2(7) NOT NULL,
        CONSTRAINT pk_wf_todo_action_log PRIMARY KEY (idempotency_key)
    );
END;
