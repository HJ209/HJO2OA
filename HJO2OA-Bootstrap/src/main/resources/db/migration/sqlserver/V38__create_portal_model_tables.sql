IF OBJECT_ID(N'dbo.portal_template', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.portal_template (
        template_id NVARCHAR(128) NOT NULL,
        tenant_id NVARCHAR(128) NOT NULL,
        template_code NVARCHAR(128) NOT NULL,
        display_name NVARCHAR(255) NOT NULL,
        scene_type NVARCHAR(64) NOT NULL,
        pages_json NVARCHAR(MAX) NOT NULL,
        versions_json NVARCHAR(MAX) NOT NULL,
        published_snapshots_json NVARCHAR(MAX) NOT NULL,
        created_at DATETIME2(7) NOT NULL,
        updated_at DATETIME2(7) NOT NULL,
        CONSTRAINT pk_portal_template PRIMARY KEY (template_id),
        CONSTRAINT uk_portal_template_code UNIQUE (tenant_id, template_code)
    );
END;

IF OBJECT_ID(N'dbo.portal_publication', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.portal_publication (
        publication_id NVARCHAR(128) NOT NULL,
        tenant_id NVARCHAR(128) NOT NULL,
        template_id NVARCHAR(128) NOT NULL,
        scene_type NVARCHAR(64) NOT NULL,
        client_type NVARCHAR(64) NOT NULL,
        audience_type NVARCHAR(64) NOT NULL,
        audience_subject_id NVARCHAR(128) NULL,
        status NVARCHAR(64) NOT NULL,
        created_at DATETIME2(7) NOT NULL,
        updated_at DATETIME2(7) NOT NULL,
        activated_at DATETIME2(7) NULL,
        offlined_at DATETIME2(7) NULL,
        CONSTRAINT pk_portal_publication PRIMARY KEY (publication_id)
    );
    CREATE INDEX ix_portal_publication_active
        ON dbo.portal_publication (tenant_id, scene_type, client_type, status, publication_id);
END;

IF OBJECT_ID(N'dbo.portal_widget_reference_status', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.portal_widget_reference_status (
        widget_id NVARCHAR(128) NOT NULL,
        tenant_id NVARCHAR(128) NOT NULL,
        widget_code NVARCHAR(128) NOT NULL,
        card_type NVARCHAR(64) NOT NULL,
        scene_type NVARCHAR(64) NULL,
        state NVARCHAR(64) NOT NULL,
        changed_fields_json NVARCHAR(MAX) NOT NULL,
        trigger_event_type NVARCHAR(255) NOT NULL,
        updated_at DATETIME2(7) NOT NULL,
        CONSTRAINT pk_portal_widget_reference_status PRIMARY KEY (widget_id),
        CONSTRAINT uk_portal_widget_reference_status_code UNIQUE (tenant_id, widget_code)
    );
END;
