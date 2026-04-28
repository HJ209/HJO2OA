IF OBJECT_ID(N'dbo.wf_todo_item', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.wf_todo_item (
        todo_id NVARCHAR(128) NOT NULL,
        task_id NVARCHAR(128) NOT NULL,
        instance_id NVARCHAR(128) NOT NULL,
        assignee_id NVARCHAR(128) NOT NULL,
        todo_type NVARCHAR(64) NOT NULL,
        category NVARCHAR(128) NOT NULL,
        title NVARCHAR(255) NOT NULL,
        urgency NVARCHAR(64) NOT NULL,
        status NVARCHAR(64) NOT NULL,
        due_time DATETIME2(7) NULL,
        overdue_at DATETIME2(7) NULL,
        created_at DATETIME2(7) NOT NULL,
        updated_at DATETIME2(7) NOT NULL,
        completed_at DATETIME2(7) NULL,
        cancellation_reason NVARCHAR(512) NULL,
        CONSTRAINT pk_wf_todo_item PRIMARY KEY (todo_id),
        CONSTRAINT uk_wf_todo_item_task UNIQUE (task_id)
    );
    CREATE INDEX ix_wf_todo_item_assignee_status
        ON dbo.wf_todo_item (assignee_id, status, updated_at);
END;

IF OBJECT_ID(N'dbo.wf_copied_todo_item', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.wf_copied_todo_item (
        todo_id NVARCHAR(128) NOT NULL,
        task_id NVARCHAR(128) NOT NULL,
        instance_id NVARCHAR(128) NOT NULL,
        recipient_assignment_id NVARCHAR(128) NOT NULL,
        todo_type NVARCHAR(64) NOT NULL,
        category NVARCHAR(128) NOT NULL,
        title NVARCHAR(255) NOT NULL,
        urgency NVARCHAR(64) NOT NULL,
        read_status NVARCHAR(64) NOT NULL,
        created_at DATETIME2(7) NOT NULL,
        updated_at DATETIME2(7) NOT NULL,
        read_at DATETIME2(7) NULL,
        CONSTRAINT pk_wf_copied_todo_item PRIMARY KEY (todo_id)
    );
    CREATE INDEX ix_wf_copied_todo_assignment
        ON dbo.wf_copied_todo_item (recipient_assignment_id, read_status, created_at);
END;

IF OBJECT_ID(N'dbo.wf_todo_projection_event', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.wf_todo_projection_event (
        event_id UNIQUEIDENTIFIER NOT NULL,
        processed_at DATETIME2(7) NOT NULL,
        CONSTRAINT pk_wf_todo_projection_event PRIMARY KEY (event_id)
    );
END;
