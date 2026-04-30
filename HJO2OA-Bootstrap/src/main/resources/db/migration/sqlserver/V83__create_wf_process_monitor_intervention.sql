IF OBJECT_ID(N'dbo.wf_process_intervention', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.wf_process_intervention (
        intervention_id UNIQUEIDENTIFIER NOT NULL,
        tenant_id UNIQUEIDENTIFIER NOT NULL,
        instance_id UNIQUEIDENTIFIER NOT NULL,
        task_id UNIQUEIDENTIFIER NULL,
        action_type NVARCHAR(64) NOT NULL,
        operator_id UNIQUEIDENTIFIER NOT NULL,
        target_assignee_id UNIQUEIDENTIFIER NULL,
        reason NVARCHAR(1024) NULL,
        created_at DATETIME2(7) NOT NULL,
        CONSTRAINT pk_wf_process_intervention PRIMARY KEY (intervention_id)
    );
    CREATE INDEX ix_wf_process_intervention_instance
        ON dbo.wf_process_intervention (tenant_id, instance_id, created_at);
END;
