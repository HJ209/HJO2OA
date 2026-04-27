CREATE TABLE dbo.proc_instance (
    id UNIQUEIDENTIFIER NOT NULL PRIMARY KEY,
    definition_id UNIQUEIDENTIFIER NOT NULL,
    definition_version INT NOT NULL,
    definition_code VARCHAR(64) NOT NULL,
    title NVARCHAR(256) NOT NULL,
    category VARCHAR(64) NULL,
    initiator_id UNIQUEIDENTIFIER NOT NULL,
    initiator_org_id UNIQUEIDENTIFIER NOT NULL,
    initiator_dept_id UNIQUEIDENTIFIER NULL,
    initiator_position_id UNIQUEIDENTIFIER NOT NULL,
    form_metadata_id UNIQUEIDENTIFIER NOT NULL,
    form_data_id UNIQUEIDENTIFIER NOT NULL,
    current_nodes NVARCHAR(MAX) NOT NULL,
    status VARCHAR(32) NOT NULL,
    start_time DATETIME2 NOT NULL,
    end_time DATETIME2 NULL,
    tenant_id UNIQUEIDENTIFIER NOT NULL,
    created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME()
);

CREATE INDEX idx_proc_instance_definition_status
    ON dbo.proc_instance (tenant_id, definition_id, status);

CREATE INDEX idx_proc_instance_initiator_status
    ON dbo.proc_instance (tenant_id, initiator_id, status);

CREATE INDEX idx_proc_instance_status_start_time
    ON dbo.proc_instance (tenant_id, status, start_time);

CREATE TABLE dbo.proc_task (
    id UNIQUEIDENTIFIER NOT NULL PRIMARY KEY,
    instance_id UNIQUEIDENTIFIER NOT NULL,
    node_id VARCHAR(64) NOT NULL,
    node_name NVARCHAR(128) NOT NULL,
    node_type VARCHAR(32) NOT NULL,
    assignee_id UNIQUEIDENTIFIER NULL,
    assignee_org_id UNIQUEIDENTIFIER NULL,
    assignee_dept_id UNIQUEIDENTIFIER NULL,
    assignee_position_id UNIQUEIDENTIFIER NULL,
    candidate_type VARCHAR(32) NULL,
    candidate_ids NVARCHAR(MAX) NULL,
    multi_instance_type VARCHAR(32) NULL,
    completion_condition VARCHAR(256) NULL,
    status VARCHAR(32) NOT NULL,
    claim_time DATETIME2 NULL,
    completed_time DATETIME2 NULL,
    due_time DATETIME2 NULL,
    tenant_id UNIQUEIDENTIFIER NOT NULL,
    created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT fk_proc_task_instance
        FOREIGN KEY (instance_id) REFERENCES dbo.proc_instance (id)
);

CREATE INDEX idx_proc_task_instance
    ON dbo.proc_task (tenant_id, instance_id);

CREATE INDEX idx_proc_task_assignee_status
    ON dbo.proc_task (tenant_id, assignee_id, status);

CREATE INDEX idx_proc_task_status_due_time
    ON dbo.proc_task (tenant_id, status, due_time);

CREATE TABLE dbo.proc_task_action (
    id UNIQUEIDENTIFIER NOT NULL PRIMARY KEY,
    task_id UNIQUEIDENTIFIER NOT NULL,
    instance_id UNIQUEIDENTIFIER NOT NULL,
    action_code VARCHAR(64) NOT NULL,
    action_name NVARCHAR(128) NOT NULL,
    operator_id UNIQUEIDENTIFIER NOT NULL,
    operator_org_id UNIQUEIDENTIFIER NOT NULL,
    operator_position_id UNIQUEIDENTIFIER NOT NULL,
    opinion NVARCHAR(1024) NULL,
    target_node_id VARCHAR(64) NULL,
    form_data_patch NVARCHAR(MAX) NULL,
    created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT fk_proc_task_action_task
        FOREIGN KEY (task_id) REFERENCES dbo.proc_task (id),
    CONSTRAINT fk_proc_task_action_instance
        FOREIGN KEY (instance_id) REFERENCES dbo.proc_instance (id)
);

CREATE INDEX idx_proc_task_action_task_created
    ON dbo.proc_task_action (task_id, created_at);

CREATE INDEX idx_proc_task_action_instance_created
    ON dbo.proc_task_action (instance_id, created_at);
