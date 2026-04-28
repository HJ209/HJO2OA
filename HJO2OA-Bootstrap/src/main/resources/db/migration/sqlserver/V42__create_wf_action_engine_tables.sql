IF OBJECT_ID(N'dbo.wf_action_engine_definition', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.wf_action_engine_definition (
        id UNIQUEIDENTIFIER NOT NULL,
        code NVARCHAR(128) NOT NULL,
        name NVARCHAR(255) NOT NULL,
        category NVARCHAR(64) NOT NULL,
        route_target NVARCHAR(64) NOT NULL,
        require_opinion BIT NOT NULL,
        require_target BIT NOT NULL,
        ui_config_json NVARCHAR(MAX) NOT NULL,
        tenant_id NVARCHAR(128) NOT NULL,
        CONSTRAINT pk_wf_action_engine_definition PRIMARY KEY (id),
        CONSTRAINT uk_wf_action_engine_definition_code UNIQUE (tenant_id, code)
    );
END;

IF OBJECT_ID(N'dbo.wf_action_engine_task_action', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.wf_action_engine_task_action (
        id UNIQUEIDENTIFIER NOT NULL,
        task_id UNIQUEIDENTIFIER NOT NULL,
        instance_id UNIQUEIDENTIFIER NOT NULL,
        action_code NVARCHAR(128) NOT NULL,
        category NVARCHAR(64) NOT NULL,
        opinion NVARCHAR(1024) NULL,
        target_node_id NVARCHAR(128) NULL,
        target_assignee_ids_json NVARCHAR(MAX) NOT NULL,
        form_data_patch_json NVARCHAR(MAX) NOT NULL,
        operator_account_id NVARCHAR(128) NOT NULL,
        operator_person_id NVARCHAR(128) NULL,
        operator_position_id NVARCHAR(128) NULL,
        operator_org_id NVARCHAR(128) NULL,
        idempotency_key NVARCHAR(255) NOT NULL,
        result_status NVARCHAR(64) NOT NULL,
        created_at DATETIME2(7) NOT NULL,
        tenant_id NVARCHAR(128) NOT NULL,
        CONSTRAINT pk_wf_action_engine_task_action PRIMARY KEY (id),
        CONSTRAINT uk_wf_action_engine_task_action_idem UNIQUE (task_id, action_code, idempotency_key)
    );
    CREATE INDEX ix_wf_action_engine_task_action_task
        ON dbo.wf_action_engine_task_action (task_id, created_at);
END;
