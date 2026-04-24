-- ============================================================
-- V19: Infrastructure attachment module tables
-- Entity source: AttachmentAssetEntity, AttachmentVersionEntity, AttachmentBindingEntity
-- PK type: NVARCHAR(64) (UUID persisted as string)
-- ============================================================

CREATE TABLE dbo.infra_attachment_asset (
    id                NVARCHAR(64)   NOT NULL PRIMARY KEY,
    storage_key       NVARCHAR(256)  NOT NULL,
    original_filename NVARCHAR(256)  NOT NULL,
    content_type      NVARCHAR(128)  NOT NULL,
    size_bytes        BIGINT         NOT NULL,
    checksum          NVARCHAR(128)  NOT NULL,
    storage_provider  NVARCHAR(32)   NOT NULL,
    preview_status    NVARCHAR(32)   NOT NULL CONSTRAINT DF_infra_attachment_asset_preview_status DEFAULT ('NONE'),
    latest_version_no INT            NOT NULL CONSTRAINT DF_infra_attachment_asset_version_no DEFAULT ((1)),
    permission_mode   NVARCHAR(32)   NOT NULL CONSTRAINT DF_infra_attachment_asset_permission_mode DEFAULT ('INHERIT_BUSINESS'),
    tenant_id         NVARCHAR(64)   NOT NULL,
    created_by        NVARCHAR(64)   NULL,
    created_at        DATETIME2(6)   NOT NULL,
    updated_at        DATETIME2(6)   NOT NULL
);

CREATE UNIQUE INDEX UX_infra_attachment_asset_storage_key
    ON dbo.infra_attachment_asset (storage_key);
CREATE INDEX IX_infra_attachment_asset_tenant_preview
    ON dbo.infra_attachment_asset (tenant_id, preview_status, updated_at DESC);
CREATE INDEX IX_infra_attachment_asset_checksum
    ON dbo.infra_attachment_asset (checksum);

CREATE TABLE dbo.infra_attachment_version (
    id                  NVARCHAR(64)   NOT NULL PRIMARY KEY,
    attachment_asset_id NVARCHAR(64)   NOT NULL,
    version_no          INT            NOT NULL,
    storage_key         NVARCHAR(256)  NOT NULL,
    checksum            NVARCHAR(128)  NOT NULL,
    size_bytes          BIGINT         NOT NULL,
    created_by          NVARCHAR(64)   NULL,
    created_at          DATETIME2(6)   NOT NULL,
    CONSTRAINT FK_infra_attachment_version_asset
        FOREIGN KEY (attachment_asset_id) REFERENCES dbo.infra_attachment_asset(id)
);

CREATE UNIQUE INDEX UX_infra_attachment_version_asset_version
    ON dbo.infra_attachment_version (attachment_asset_id, version_no);
CREATE INDEX IX_infra_attachment_version_asset_created
    ON dbo.infra_attachment_version (attachment_asset_id, created_at DESC);

CREATE TABLE dbo.infra_attachment_binding (
    id                  NVARCHAR(64)   NOT NULL PRIMARY KEY,
    attachment_asset_id NVARCHAR(64)   NOT NULL,
    business_type       NVARCHAR(64)   NOT NULL,
    business_id         NVARCHAR(64)   NOT NULL,
    binding_role        NVARCHAR(32)   NOT NULL,
    active              BIT            NOT NULL CONSTRAINT DF_infra_attachment_binding_active DEFAULT ((1)),
    CONSTRAINT FK_infra_attachment_binding_asset
        FOREIGN KEY (attachment_asset_id) REFERENCES dbo.infra_attachment_asset(id)
);

CREATE UNIQUE INDEX UX_infra_attachment_binding_active_role
    ON dbo.infra_attachment_binding (attachment_asset_id, business_type, business_id, binding_role)
    WHERE active = 1;
CREATE INDEX IX_infra_attachment_binding_business
    ON dbo.infra_attachment_binding (business_type, business_id, active);
