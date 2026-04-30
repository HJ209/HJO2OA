-- ============================================================
-- V70: Security + attachment runtime support tables
-- Window 05 runtime additions: attachment access audit.
-- Security runtime policies continue to use infra_security_policy
-- config snapshots from V11.
-- ============================================================

CREATE TABLE dbo.infra_attachment_access_audit (
    id                  NVARCHAR(64)  NOT NULL PRIMARY KEY,
    attachment_asset_id NVARCHAR(64)  NOT NULL,
    version_no          INT           NULL,
    action              NVARCHAR(32)  NOT NULL,
    tenant_id           NVARCHAR(64)  NULL,
    operator_id         NVARCHAR(64)  NULL,
    client_ip           NVARCHAR(64)  NULL,
    occurred_at         DATETIME2(6)  NOT NULL,
    CONSTRAINT FK_infra_attachment_access_audit_asset
        FOREIGN KEY (attachment_asset_id) REFERENCES dbo.infra_attachment_asset(id)
);

CREATE INDEX IX_infra_attachment_access_audit_asset_time
    ON dbo.infra_attachment_access_audit (attachment_asset_id, occurred_at DESC);

CREATE INDEX IX_infra_attachment_access_audit_tenant_time
    ON dbo.infra_attachment_access_audit (tenant_id, occurred_at DESC);
