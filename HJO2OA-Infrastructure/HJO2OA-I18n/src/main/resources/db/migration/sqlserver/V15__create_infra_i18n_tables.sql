CREATE TABLE dbo.infra_locale_bundle (
    id UNIQUEIDENTIFIER NOT NULL PRIMARY KEY,
    bundle_code VARCHAR(64) NOT NULL,
    module_code VARCHAR(64) NOT NULL,
    locale VARCHAR(16) NOT NULL,
    fallback_locale VARCHAR(16) NULL,
    status VARCHAR(32) NOT NULL,
    tenant_id UNIQUEIDENTIFIER NULL,
    created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME()
);

CREATE UNIQUE INDEX uk_infra_locale_bundle_global
    ON dbo.infra_locale_bundle (bundle_code, locale)
    WHERE tenant_id IS NULL;

CREATE UNIQUE INDEX uk_infra_locale_bundle_tenant
    ON dbo.infra_locale_bundle (bundle_code, locale, tenant_id)
    WHERE tenant_id IS NOT NULL;

CREATE INDEX idx_infra_locale_bundle_module_status
    ON dbo.infra_locale_bundle (module_code, status);

CREATE TABLE dbo.infra_locale_resource_entry (
    id UNIQUEIDENTIFIER NOT NULL PRIMARY KEY,
    locale_bundle_id UNIQUEIDENTIFIER NOT NULL,
    resource_key VARCHAR(256) NOT NULL,
    resource_value NVARCHAR(4000) NOT NULL,
    version INT NOT NULL DEFAULT 1,
    active BIT NOT NULL DEFAULT 1,
    CONSTRAINT fk_infra_locale_resource_entry_bundle
        FOREIGN KEY (locale_bundle_id) REFERENCES dbo.infra_locale_bundle (id)
);

CREATE UNIQUE INDEX uk_infra_locale_resource_entry_bundle_key
    ON dbo.infra_locale_resource_entry (locale_bundle_id, resource_key);

CREATE INDEX idx_infra_locale_resource_entry_bundle_active
    ON dbo.infra_locale_resource_entry (locale_bundle_id, active);
