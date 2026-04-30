IF OBJECT_ID(N'dbo.content_category', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.content_category (
        id UNIQUEIDENTIFIER NOT NULL PRIMARY KEY,
        code NVARCHAR(64) NOT NULL,
        name NVARCHAR(128) NOT NULL,
        category_type NVARCHAR(32) NOT NULL,
        parent_id UNIQUEIDENTIFIER NULL,
        route_path NVARCHAR(256) NULL,
        sort_order INT NOT NULL DEFAULT 0,
        visible_mode NVARCHAR(32) NOT NULL,
        status NVARCHAR(32) NOT NULL,
        version_no INT NOT NULL DEFAULT 1,
        tenant_id UNIQUEIDENTIFIER NOT NULL,
        created_by UNIQUEIDENTIFIER NULL,
        updated_by UNIQUEIDENTIFIER NULL,
        created_at DATETIME2(7) NOT NULL DEFAULT SYSUTCDATETIME(),
        updated_at DATETIME2(7) NOT NULL DEFAULT SYSUTCDATETIME(),
        CONSTRAINT uk_content_category_tenant_code UNIQUE (tenant_id, code)
    );
    CREATE INDEX ix_content_category_tenant_parent
        ON dbo.content_category (tenant_id, parent_id, sort_order);
    CREATE INDEX ix_content_category_tenant_status
        ON dbo.content_category (tenant_id, status);
END;

IF OBJECT_ID(N'dbo.content_category_permission', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.content_category_permission (
        id UNIQUEIDENTIFIER NOT NULL PRIMARY KEY,
        category_id UNIQUEIDENTIFIER NOT NULL,
        tenant_id UNIQUEIDENTIFIER NOT NULL,
        subject_type NVARCHAR(32) NOT NULL,
        subject_id UNIQUEIDENTIFIER NULL,
        effect NVARCHAR(16) NOT NULL,
        scope_type NVARCHAR(32) NOT NULL,
        sort_order INT NOT NULL DEFAULT 0,
        created_at DATETIME2(7) NOT NULL DEFAULT SYSUTCDATETIME()
    );
    CREATE INDEX ix_content_category_permission_category
        ON dbo.content_category_permission (tenant_id, category_id, scope_type, sort_order);
END;

IF OBJECT_ID(N'dbo.content_article', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.content_article (
        id UNIQUEIDENTIFIER NOT NULL PRIMARY KEY,
        article_no NVARCHAR(64) NOT NULL,
        title NVARCHAR(256) NOT NULL,
        summary NVARCHAR(1024) NULL,
        content_type NVARCHAR(32) NOT NULL,
        main_category_id UNIQUEIDENTIFIER NOT NULL,
        author_id UNIQUEIDENTIFIER NOT NULL,
        author_name NVARCHAR(128) NULL,
        source_type NVARCHAR(32) NOT NULL,
        source_url NVARCHAR(512) NULL,
        current_draft_version_no INT NULL,
        current_published_version_no INT NULL,
        status NVARCHAR(32) NOT NULL,
        tenant_id UNIQUEIDENTIFIER NOT NULL,
        created_by UNIQUEIDENTIFIER NULL,
        updated_by UNIQUEIDENTIFIER NULL,
        created_at DATETIME2(7) NOT NULL DEFAULT SYSUTCDATETIME(),
        updated_at DATETIME2(7) NOT NULL DEFAULT SYSUTCDATETIME(),
        CONSTRAINT uk_content_article_tenant_no UNIQUE (tenant_id, article_no)
    );
    CREATE INDEX ix_content_article_tenant_status
        ON dbo.content_article (tenant_id, status, updated_at DESC);
    CREATE INDEX ix_content_article_tenant_category
        ON dbo.content_article (tenant_id, main_category_id, status);
    CREATE INDEX ix_content_article_tenant_author
        ON dbo.content_article (tenant_id, author_id, created_at DESC);
END;

IF OBJECT_ID(N'dbo.content_article_version', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.content_article_version (
        id UNIQUEIDENTIFIER NOT NULL PRIMARY KEY,
        article_id UNIQUEIDENTIFIER NOT NULL,
        tenant_id UNIQUEIDENTIFIER NOT NULL,
        version_no INT NOT NULL,
        title_snapshot NVARCHAR(256) NOT NULL,
        summary_snapshot NVARCHAR(1024) NULL,
        body_format NVARCHAR(32) NOT NULL,
        body_text NVARCHAR(MAX) NOT NULL,
        body_checksum NVARCHAR(128) NULL,
        cover_attachment_id UNIQUEIDENTIFIER NULL,
        attachments_json NVARCHAR(MAX) NULL,
        tags_json NVARCHAR(MAX) NULL,
        editor_id UNIQUEIDENTIFIER NOT NULL,
        status NVARCHAR(32) NOT NULL,
        source_version_no INT NULL,
        idempotency_key NVARCHAR(128) NULL,
        created_at DATETIME2(7) NOT NULL DEFAULT SYSUTCDATETIME(),
        updated_at DATETIME2(7) NOT NULL DEFAULT SYSUTCDATETIME(),
        CONSTRAINT uk_content_version_article_no UNIQUE (tenant_id, article_id, version_no)
    );
    CREATE INDEX ix_content_version_article
        ON dbo.content_article_version (tenant_id, article_id, version_no DESC);
    CREATE UNIQUE INDEX ux_content_version_idempotency
        ON dbo.content_article_version (tenant_id, article_id, idempotency_key)
        WHERE idempotency_key IS NOT NULL;
END;

IF OBJECT_ID(N'dbo.content_article_version', N'U') IS NOT NULL
   AND COL_LENGTH(N'dbo.content_article_version', N'idempotency_key') IS NULL
BEGIN
    ALTER TABLE dbo.content_article_version ADD idempotency_key NVARCHAR(128) NULL;
END;

IF OBJECT_ID(N'dbo.content_article_version', N'U') IS NOT NULL
   AND NOT EXISTS (
       SELECT 1
       FROM sys.indexes
       WHERE name = N'ux_content_version_idempotency'
         AND object_id = OBJECT_ID(N'dbo.content_article_version')
   )
BEGIN
    CREATE UNIQUE INDEX ux_content_version_idempotency
        ON dbo.content_article_version (tenant_id, article_id, idempotency_key)
        WHERE idempotency_key IS NOT NULL;
END;

IF OBJECT_ID(N'dbo.content_publication', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.content_publication (
        id UNIQUEIDENTIFIER NOT NULL PRIMARY KEY,
        article_id UNIQUEIDENTIFIER NOT NULL,
        tenant_id UNIQUEIDENTIFIER NOT NULL,
        target_version_no INT NOT NULL,
        review_mode NVARCHAR(32) NOT NULL,
        review_status NVARCHAR(32) NOT NULL,
        workflow_instance_id UNIQUEIDENTIFIER NULL,
        publication_status NVARCHAR(32) NOT NULL,
        start_at DATETIME2(7) NULL,
        end_at DATETIME2(7) NULL,
        published_at DATETIME2(7) NULL,
        published_by UNIQUEIDENTIFIER NULL,
        offline_at DATETIME2(7) NULL,
        offline_by UNIQUEIDENTIFIER NULL,
        archive_at DATETIME2(7) NULL,
        archived_by UNIQUEIDENTIFIER NULL,
        reason NVARCHAR(512) NULL,
        created_at DATETIME2(7) NOT NULL DEFAULT SYSUTCDATETIME(),
        updated_at DATETIME2(7) NOT NULL DEFAULT SYSUTCDATETIME()
    );
    CREATE INDEX ix_content_publication_article
        ON dbo.content_publication (tenant_id, article_id, publication_status, updated_at DESC);
END;

IF OBJECT_ID(N'dbo.content_review_record', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.content_review_record (
        id UNIQUEIDENTIFIER NOT NULL PRIMARY KEY,
        publication_id UNIQUEIDENTIFIER NOT NULL,
        article_id UNIQUEIDENTIFIER NOT NULL,
        tenant_id UNIQUEIDENTIFIER NOT NULL,
        action NVARCHAR(32) NOT NULL,
        operator_id UNIQUEIDENTIFIER NOT NULL,
        opinion NVARCHAR(1024) NULL,
        created_at DATETIME2(7) NOT NULL DEFAULT SYSUTCDATETIME()
    );
    CREATE INDEX ix_content_review_publication
        ON dbo.content_review_record (tenant_id, publication_id, created_at);
END;

IF OBJECT_ID(N'dbo.content_publication_scope', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.content_publication_scope (
        id UNIQUEIDENTIFIER NOT NULL PRIMARY KEY,
        publication_id UNIQUEIDENTIFIER NOT NULL,
        article_id UNIQUEIDENTIFIER NOT NULL,
        tenant_id UNIQUEIDENTIFIER NOT NULL,
        subject_type NVARCHAR(32) NOT NULL,
        subject_id UNIQUEIDENTIFIER NULL,
        effect NVARCHAR(16) NOT NULL,
        sort_order INT NOT NULL DEFAULT 0,
        scope_version INT NOT NULL DEFAULT 1,
        created_at DATETIME2(7) NOT NULL DEFAULT SYSUTCDATETIME()
    );
    CREATE INDEX ix_content_publication_scope_publication
        ON dbo.content_publication_scope (tenant_id, publication_id, scope_version, sort_order);
END;

IF OBJECT_ID(N'dbo.content_search_index', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.content_search_index (
        article_id UNIQUEIDENTIFIER NOT NULL PRIMARY KEY,
        publication_id UNIQUEIDENTIFIER NOT NULL,
        tenant_id UNIQUEIDENTIFIER NOT NULL,
        category_id UNIQUEIDENTIFIER NOT NULL,
        title NVARCHAR(256) NOT NULL,
        summary NVARCHAR(1024) NULL,
        body_text NVARCHAR(MAX) NULL,
        author_id UNIQUEIDENTIFIER NOT NULL,
        author_name NVARCHAR(128) NULL,
        tags_json NVARCHAR(MAX) NULL,
        status NVARCHAR(32) NOT NULL,
        published_at DATETIME2(7) NOT NULL,
        updated_at DATETIME2(7) NOT NULL,
        visible_scope_json NVARCHAR(MAX) NULL,
        hot_score DECIMAL(18,4) NOT NULL DEFAULT 0
    );
    CREATE INDEX ix_content_search_tenant_published
        ON dbo.content_search_index (tenant_id, status, published_at DESC);
    CREATE INDEX ix_content_search_tenant_category
        ON dbo.content_search_index (tenant_id, category_id, status, published_at DESC);
    CREATE INDEX ix_content_search_tenant_author
        ON dbo.content_search_index (tenant_id, author_id, status, published_at DESC);
END;

IF OBJECT_ID(N'dbo.content_read_record', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.content_read_record (
        id UNIQUEIDENTIFIER NOT NULL PRIMARY KEY,
        article_id UNIQUEIDENTIFIER NOT NULL,
        tenant_id UNIQUEIDENTIFIER NOT NULL,
        person_id UNIQUEIDENTIFIER NULL,
        assignment_id UNIQUEIDENTIFIER NULL,
        action_type NVARCHAR(32) NOT NULL,
        idempotency_key NVARCHAR(128) NULL,
        occurred_at DATETIME2(7) NOT NULL DEFAULT SYSUTCDATETIME()
    );
    CREATE INDEX ix_content_read_article
        ON dbo.content_read_record (tenant_id, article_id, action_type, occurred_at DESC);
    CREATE UNIQUE INDEX ux_content_read_idempotency
        ON dbo.content_read_record (tenant_id, idempotency_key)
        WHERE idempotency_key IS NOT NULL;
END;

IF OBJECT_ID(N'dbo.content_engagement_snapshot', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.content_engagement_snapshot (
        id UNIQUEIDENTIFIER NOT NULL PRIMARY KEY,
        article_id UNIQUEIDENTIFIER NOT NULL,
        tenant_id UNIQUEIDENTIFIER NOT NULL,
        stat_bucket NVARCHAR(32) NOT NULL,
        read_count BIGINT NOT NULL DEFAULT 0,
        unique_reader_count BIGINT NOT NULL DEFAULT 0,
        download_count BIGINT NOT NULL DEFAULT 0,
        favorite_count BIGINT NOT NULL DEFAULT 0,
        hot_score DECIMAL(18,4) NOT NULL DEFAULT 0,
        last_aggregated_at DATETIME2(7) NULL,
        created_at DATETIME2(7) NOT NULL DEFAULT SYSUTCDATETIME(),
        updated_at DATETIME2(7) NOT NULL DEFAULT SYSUTCDATETIME(),
        CONSTRAINT uk_content_snapshot_article_bucket UNIQUE (tenant_id, article_id, stat_bucket)
    );
    CREATE INDEX ix_content_snapshot_rank
        ON dbo.content_engagement_snapshot (tenant_id, stat_bucket, hot_score DESC);
END;
