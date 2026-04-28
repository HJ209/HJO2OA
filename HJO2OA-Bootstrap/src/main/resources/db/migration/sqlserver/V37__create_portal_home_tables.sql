IF OBJECT_ID(N'dbo.portal_home_refresh_state', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.portal_home_refresh_state (
        id NVARCHAR(512) NOT NULL,
        tenant_id NVARCHAR(128) NOT NULL,
        person_id NVARCHAR(128) NULL,
        assignment_id NVARCHAR(128) NULL,
        scene_type NVARCHAR(64) NOT NULL,
        status NVARCHAR(64) NOT NULL,
        trigger_event NVARCHAR(255) NULL,
        card_type NVARCHAR(64) NULL,
        message NVARCHAR(512) NULL,
        updated_at DATETIME2(7) NOT NULL,
        CONSTRAINT pk_portal_home_refresh_state PRIMARY KEY (id)
    );
    CREATE INDEX ix_portal_home_refresh_state_lookup
        ON dbo.portal_home_refresh_state (tenant_id, scene_type, person_id, assignment_id, updated_at);
END;
