IF OBJECT_ID(N'dbo.portal_personalization_profile', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.portal_personalization_profile (
        profile_id NVARCHAR(128) NOT NULL,
        tenant_id NVARCHAR(128) NOT NULL,
        person_id NVARCHAR(128) NOT NULL,
        assignment_id NVARCHAR(128) NULL,
        scene_type NVARCHAR(64) NOT NULL,
        base_publication_id NVARCHAR(128) NOT NULL,
        theme_code NVARCHAR(128) NULL,
        widget_order_json NVARCHAR(MAX) NOT NULL,
        hidden_placement_json NVARCHAR(MAX) NOT NULL,
        quick_access_json NVARCHAR(MAX) NOT NULL,
        status NVARCHAR(64) NOT NULL,
        last_resolved_at DATETIME2(7) NOT NULL,
        created_at DATETIME2(7) NOT NULL,
        updated_at DATETIME2(7) NOT NULL,
        CONSTRAINT pk_portal_personalization_profile PRIMARY KEY (profile_id)
    );
    CREATE UNIQUE INDEX uk_portal_personalization_profile_global
        ON dbo.portal_personalization_profile (tenant_id, person_id, scene_type)
        WHERE assignment_id IS NULL;
    CREATE UNIQUE INDEX uk_portal_personalization_profile_assignment
        ON dbo.portal_personalization_profile (tenant_id, person_id, assignment_id, scene_type)
        WHERE assignment_id IS NOT NULL;
END;
