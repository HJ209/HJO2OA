IF OBJECT_ID(N'dbo.biz_collab_workspace', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.biz_collab_workspace (
        workspace_id NVARCHAR(128) NOT NULL,
        tenant_id NVARCHAR(128) NOT NULL,
        code NVARCHAR(128) NOT NULL,
        name NVARCHAR(255) NOT NULL,
        description NVARCHAR(1024) NULL,
        status NVARCHAR(32) NOT NULL,
        visibility NVARCHAR(32) NOT NULL,
        owner_id NVARCHAR(128) NOT NULL,
        created_at DATETIME2(7) NOT NULL,
        updated_at DATETIME2(7) NOT NULL,
        CONSTRAINT pk_biz_collab_workspace PRIMARY KEY (workspace_id),
        CONSTRAINT uk_biz_collab_workspace_code UNIQUE (tenant_id, code)
    );
    CREATE INDEX ix_biz_collab_workspace_tenant_status
        ON dbo.biz_collab_workspace (tenant_id, status, updated_at);
END;

IF OBJECT_ID(N'dbo.biz_collab_workspace_member', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.biz_collab_workspace_member (
        member_id NVARCHAR(128) NOT NULL,
        tenant_id NVARCHAR(128) NOT NULL,
        workspace_id NVARCHAR(128) NOT NULL,
        person_id NVARCHAR(128) NOT NULL,
        role_code NVARCHAR(32) NOT NULL,
        permissions NVARCHAR(512) NOT NULL,
        status NVARCHAR(32) NOT NULL,
        joined_at DATETIME2(7) NOT NULL,
        updated_at DATETIME2(7) NOT NULL,
        CONSTRAINT pk_biz_collab_workspace_member PRIMARY KEY (member_id),
        CONSTRAINT uk_biz_collab_workspace_member_person UNIQUE (tenant_id, workspace_id, person_id)
    );
    CREATE INDEX ix_biz_collab_workspace_member_person
        ON dbo.biz_collab_workspace_member (tenant_id, person_id, status);
END;

IF OBJECT_ID(N'dbo.biz_collab_discussion', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.biz_collab_discussion (
        discussion_id NVARCHAR(128) NOT NULL,
        tenant_id NVARCHAR(128) NOT NULL,
        workspace_id NVARCHAR(128) NOT NULL,
        title NVARCHAR(255) NOT NULL,
        body NVARCHAR(MAX) NOT NULL,
        author_id NVARCHAR(128) NOT NULL,
        status NVARCHAR(32) NOT NULL,
        pinned BIT NOT NULL,
        created_at DATETIME2(7) NOT NULL,
        updated_at DATETIME2(7) NOT NULL,
        closed_at DATETIME2(7) NULL,
        CONSTRAINT pk_biz_collab_discussion PRIMARY KEY (discussion_id)
    );
    CREATE INDEX ix_biz_collab_discussion_workspace
        ON dbo.biz_collab_discussion (tenant_id, workspace_id, status, pinned, updated_at);
END;

IF OBJECT_ID(N'dbo.biz_collab_discussion_reply', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.biz_collab_discussion_reply (
        reply_id NVARCHAR(128) NOT NULL,
        tenant_id NVARCHAR(128) NOT NULL,
        discussion_id NVARCHAR(128) NOT NULL,
        author_id NVARCHAR(128) NOT NULL,
        body NVARCHAR(MAX) NOT NULL,
        deleted BIT NOT NULL,
        created_at DATETIME2(7) NOT NULL,
        deleted_at DATETIME2(7) NULL,
        CONSTRAINT pk_biz_collab_discussion_reply PRIMARY KEY (reply_id)
    );
    CREATE INDEX ix_biz_collab_discussion_reply_discussion
        ON dbo.biz_collab_discussion_reply (tenant_id, discussion_id, created_at);
END;

IF OBJECT_ID(N'dbo.biz_collab_discussion_read', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.biz_collab_discussion_read (
        read_id NVARCHAR(128) NOT NULL,
        tenant_id NVARCHAR(128) NOT NULL,
        discussion_id NVARCHAR(128) NOT NULL,
        person_id NVARCHAR(128) NOT NULL,
        read_at DATETIME2(7) NOT NULL,
        CONSTRAINT pk_biz_collab_discussion_read PRIMARY KEY (read_id),
        CONSTRAINT uk_biz_collab_discussion_read_person UNIQUE (tenant_id, discussion_id, person_id)
    );
END;

IF OBJECT_ID(N'dbo.biz_collab_comment', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.biz_collab_comment (
        comment_id NVARCHAR(128) NOT NULL,
        tenant_id NVARCHAR(128) NOT NULL,
        workspace_id NVARCHAR(128) NOT NULL,
        object_type NVARCHAR(64) NOT NULL,
        object_id NVARCHAR(128) NOT NULL,
        author_id NVARCHAR(128) NOT NULL,
        body NVARCHAR(MAX) NOT NULL,
        deleted BIT NOT NULL,
        created_at DATETIME2(7) NOT NULL,
        updated_at DATETIME2(7) NOT NULL,
        deleted_at DATETIME2(7) NULL,
        CONSTRAINT pk_biz_collab_comment PRIMARY KEY (comment_id)
    );
    CREATE INDEX ix_biz_collab_comment_object
        ON dbo.biz_collab_comment (tenant_id, object_type, object_id, created_at);
END;

IF OBJECT_ID(N'dbo.biz_collab_task', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.biz_collab_task (
        task_id NVARCHAR(128) NOT NULL,
        tenant_id NVARCHAR(128) NOT NULL,
        workspace_id NVARCHAR(128) NOT NULL,
        title NVARCHAR(255) NOT NULL,
        description NVARCHAR(MAX) NULL,
        creator_id NVARCHAR(128) NOT NULL,
        assignee_id NVARCHAR(128) NOT NULL,
        status NVARCHAR(32) NOT NULL,
        priority NVARCHAR(32) NOT NULL,
        due_at DATETIME2(7) NULL,
        completed_at DATETIME2(7) NULL,
        created_at DATETIME2(7) NOT NULL,
        updated_at DATETIME2(7) NOT NULL,
        CONSTRAINT pk_biz_collab_task PRIMARY KEY (task_id)
    );
    CREATE INDEX ix_biz_collab_task_assignee
        ON dbo.biz_collab_task (tenant_id, assignee_id, status, due_at);
END;

IF OBJECT_ID(N'dbo.biz_collab_task_participant', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.biz_collab_task_participant (
        participant_id NVARCHAR(128) NOT NULL,
        tenant_id NVARCHAR(128) NOT NULL,
        task_id NVARCHAR(128) NOT NULL,
        person_id NVARCHAR(128) NOT NULL,
        role_code NVARCHAR(32) NOT NULL,
        CONSTRAINT pk_biz_collab_task_participant PRIMARY KEY (participant_id),
        CONSTRAINT uk_biz_collab_task_participant_person UNIQUE (tenant_id, task_id, person_id)
    );
END;

IF OBJECT_ID(N'dbo.biz_collab_meeting', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.biz_collab_meeting (
        meeting_id NVARCHAR(128) NOT NULL,
        tenant_id NVARCHAR(128) NOT NULL,
        workspace_id NVARCHAR(128) NOT NULL,
        title NVARCHAR(255) NOT NULL,
        agenda NVARCHAR(MAX) NULL,
        organizer_id NVARCHAR(128) NOT NULL,
        status NVARCHAR(32) NOT NULL,
        start_at DATETIME2(7) NOT NULL,
        end_at DATETIME2(7) NOT NULL,
        location NVARCHAR(255) NULL,
        reminder_minutes_before INT NOT NULL,
        minutes NVARCHAR(MAX) NULL,
        minutes_published_at DATETIME2(7) NULL,
        created_at DATETIME2(7) NOT NULL,
        updated_at DATETIME2(7) NOT NULL,
        CONSTRAINT pk_biz_collab_meeting PRIMARY KEY (meeting_id)
    );
    CREATE INDEX ix_biz_collab_meeting_participant_time
        ON dbo.biz_collab_meeting (tenant_id, workspace_id, status, start_at);
END;

IF OBJECT_ID(N'dbo.biz_collab_meeting_participant', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.biz_collab_meeting_participant (
        participant_id NVARCHAR(128) NOT NULL,
        tenant_id NVARCHAR(128) NOT NULL,
        meeting_id NVARCHAR(128) NOT NULL,
        person_id NVARCHAR(128) NOT NULL,
        role_code NVARCHAR(32) NOT NULL,
        response_status NVARCHAR(32) NOT NULL,
        CONSTRAINT pk_biz_collab_meeting_participant PRIMARY KEY (participant_id),
        CONSTRAINT uk_biz_collab_meeting_participant_person UNIQUE (tenant_id, meeting_id, person_id)
    );
END;

IF OBJECT_ID(N'dbo.biz_collab_meeting_reminder', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.biz_collab_meeting_reminder (
        reminder_id NVARCHAR(128) NOT NULL,
        tenant_id NVARCHAR(128) NOT NULL,
        meeting_id NVARCHAR(128) NOT NULL,
        participant_id NVARCHAR(128) NOT NULL,
        remind_at DATETIME2(7) NOT NULL,
        status NVARCHAR(32) NOT NULL,
        sent_at DATETIME2(7) NULL,
        CONSTRAINT pk_biz_collab_meeting_reminder PRIMARY KEY (reminder_id),
        CONSTRAINT uk_biz_collab_meeting_reminder_due UNIQUE (tenant_id, meeting_id, participant_id, remind_at)
    );
    CREATE INDEX ix_biz_collab_meeting_reminder_due
        ON dbo.biz_collab_meeting_reminder (tenant_id, status, remind_at);
END;

IF OBJECT_ID(N'dbo.biz_collab_attachment_link', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.biz_collab_attachment_link (
        attachment_link_id NVARCHAR(128) NOT NULL,
        tenant_id NVARCHAR(128) NOT NULL,
        owner_type NVARCHAR(64) NOT NULL,
        owner_id NVARCHAR(128) NOT NULL,
        attachment_id NVARCHAR(128) NOT NULL,
        file_name NVARCHAR(255) NOT NULL,
        created_at DATETIME2(7) NOT NULL,
        CONSTRAINT pk_biz_collab_attachment_link PRIMARY KEY (attachment_link_id)
    );
    CREATE INDEX ix_biz_collab_attachment_link_owner
        ON dbo.biz_collab_attachment_link (tenant_id, owner_type, owner_id);
END;

IF OBJECT_ID(N'dbo.biz_collab_audit_log', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.biz_collab_audit_log (
        audit_id NVARCHAR(128) NOT NULL,
        tenant_id NVARCHAR(128) NOT NULL,
        actor_id NVARCHAR(128) NOT NULL,
        action_code NVARCHAR(64) NOT NULL,
        resource_type NVARCHAR(64) NOT NULL,
        resource_id NVARCHAR(128) NOT NULL,
        request_id NVARCHAR(128) NOT NULL,
        detail NVARCHAR(1024) NULL,
        occurred_at DATETIME2(7) NOT NULL,
        CONSTRAINT pk_biz_collab_audit_log PRIMARY KEY (audit_id)
    );
    CREATE INDEX ix_biz_collab_audit_log_resource
        ON dbo.biz_collab_audit_log (tenant_id, resource_type, resource_id, occurred_at);
END;

IF OBJECT_ID(N'dbo.biz_collab_idempotency_record', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.biz_collab_idempotency_record (
        idempotency_id NVARCHAR(128) NOT NULL,
        tenant_id NVARCHAR(128) NOT NULL,
        operation_code NVARCHAR(128) NOT NULL,
        idempotency_key NVARCHAR(255) NOT NULL,
        resource_type NVARCHAR(64) NOT NULL,
        resource_id NVARCHAR(128) NOT NULL,
        created_at DATETIME2(7) NOT NULL,
        CONSTRAINT pk_biz_collab_idempotency_record PRIMARY KEY (idempotency_id),
        CONSTRAINT uk_biz_collab_idempotency UNIQUE (tenant_id, operation_code, idempotency_key)
    );
END;
