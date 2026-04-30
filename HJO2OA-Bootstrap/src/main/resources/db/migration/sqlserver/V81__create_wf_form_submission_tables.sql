IF OBJECT_ID(N'dbo.wf_form_submission', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.wf_form_submission (
        submission_id UNIQUEIDENTIFIER NOT NULL,
        metadata_id UNIQUEIDENTIFIER NOT NULL,
        metadata_code NVARCHAR(64) NOT NULL,
        metadata_version INT NOT NULL,
        process_instance_id UNIQUEIDENTIFIER NULL,
        form_data_id UNIQUEIDENTIFIER NULL,
        node_id NVARCHAR(64) NULL,
        status NVARCHAR(32) NOT NULL,
        form_data NVARCHAR(MAX) NOT NULL,
        attachment_ids NVARCHAR(MAX) NOT NULL,
        validation_result NVARCHAR(MAX) NOT NULL,
        idempotency_key NVARCHAR(128) NOT NULL,
        submitted_by UNIQUEIDENTIFIER NOT NULL,
        tenant_id UNIQUEIDENTIFIER NOT NULL,
        created_at DATETIME2(7) NOT NULL,
        updated_at DATETIME2(7) NOT NULL,
        submitted_at DATETIME2(7) NULL,
        CONSTRAINT pk_wf_form_submission PRIMARY KEY (submission_id),
        CONSTRAINT uk_wf_form_submission_idem UNIQUE (tenant_id, idempotency_key)
    );
    CREATE INDEX ix_wf_form_submission_actor
        ON dbo.wf_form_submission (tenant_id, submitted_by, status, updated_at);
    CREATE INDEX ix_wf_form_submission_instance
        ON dbo.wf_form_submission (tenant_id, process_instance_id, status);
END;
