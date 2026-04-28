IF OBJECT_ID(N'dbo.portal_widget_definition', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.portal_widget_definition (
        widget_id NVARCHAR(128) NOT NULL,
        tenant_id NVARCHAR(128) NOT NULL,
        widget_code NVARCHAR(128) NOT NULL,
        display_name NVARCHAR(255) NOT NULL,
        card_type NVARCHAR(64) NOT NULL,
        scene_type NVARCHAR(64) NULL,
        source_module NVARCHAR(128) NOT NULL,
        data_source_type NVARCHAR(64) NOT NULL,
        allow_hide BIT NOT NULL,
        allow_collapse BIT NOT NULL,
        max_items INT NOT NULL,
        status NVARCHAR(64) NOT NULL,
        created_at DATETIME2(7) NOT NULL,
        updated_at DATETIME2(7) NOT NULL,
        CONSTRAINT pk_portal_widget_definition PRIMARY KEY (widget_id),
        CONSTRAINT uk_portal_widget_definition_code UNIQUE (tenant_id, widget_code)
    );
END;
