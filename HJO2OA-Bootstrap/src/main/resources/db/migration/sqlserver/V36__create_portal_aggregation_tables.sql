IF OBJECT_ID(N'dbo.portal_card_snapshot', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.portal_card_snapshot (
        snapshot_id NVARCHAR(512) NOT NULL,
        tenant_id NVARCHAR(128) NOT NULL,
        person_id NVARCHAR(128) NOT NULL,
        assignment_id NVARCHAR(128) NOT NULL,
        position_id NVARCHAR(128) NOT NULL,
        scene_type NVARCHAR(64) NOT NULL,
        card_type NVARCHAR(64) NOT NULL,
        state NVARCHAR(64) NOT NULL,
        data_json NVARCHAR(MAX) NOT NULL,
        message NVARCHAR(512) NULL,
        refreshed_at DATETIME2(7) NOT NULL,
        CONSTRAINT pk_portal_card_snapshot PRIMARY KEY (snapshot_id)
    );
    CREATE INDEX ix_portal_card_snapshot_scope
        ON dbo.portal_card_snapshot (tenant_id, person_id, assignment_id, position_id, scene_type, card_type);
END;
