IF OBJECT_ID(N'dbo.portal_designer_template_projection', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.portal_designer_template_projection (
        template_id NVARCHAR(128) NOT NULL,
        tenant_id NVARCHAR(128) NOT NULL,
        template_code NVARCHAR(128) NOT NULL,
        scene_type NVARCHAR(64) NOT NULL,
        versions_json NVARCHAR(MAX) NOT NULL,
        active_publication_ids_json NVARCHAR(MAX) NOT NULL,
        created_at DATETIME2(7) NOT NULL,
        updated_at DATETIME2(7) NOT NULL,
        CONSTRAINT pk_portal_designer_template_projection PRIMARY KEY (template_id)
    );
    CREATE INDEX ix_portal_designer_template_projection_tenant
        ON dbo.portal_designer_template_projection (tenant_id, template_code);
END;

IF OBJECT_ID(N'dbo.portal_designer_widget_palette_projection', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.portal_designer_widget_palette_projection (
        widget_id NVARCHAR(128) NOT NULL,
        widget_code NVARCHAR(128) NOT NULL,
        card_type NVARCHAR(64) NOT NULL,
        scene_type NVARCHAR(64) NULL,
        state NVARCHAR(64) NOT NULL,
        changed_fields_json NVARCHAR(MAX) NOT NULL,
        trigger_event_type NVARCHAR(255) NOT NULL,
        updated_at DATETIME2(7) NOT NULL,
        CONSTRAINT pk_portal_designer_widget_palette_projection PRIMARY KEY (widget_id)
    );
    CREATE INDEX ix_portal_designer_widget_palette_code
        ON dbo.portal_designer_widget_palette_projection (widget_code);
END;
