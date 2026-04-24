CREATE TABLE infra_cache_policy (
    id UNIQUEIDENTIFIER NOT NULL PRIMARY KEY,
    namespace NVARCHAR(128) NOT NULL,
    backend_type NVARCHAR(32) NOT NULL,
    ttl_seconds INT NOT NULL,
    max_capacity INT NULL,
    eviction_policy NVARCHAR(16) NOT NULL,
    invalidation_mode NVARCHAR(32) NOT NULL,
    metrics_enabled BIT NOT NULL CONSTRAINT df_infra_cache_policy_metrics_enabled DEFAULT 1,
    active BIT NOT NULL CONSTRAINT df_infra_cache_policy_active DEFAULT 1,
    created_at DATETIME2 NOT NULL,
    updated_at DATETIME2 NOT NULL
);

CREATE UNIQUE INDEX uk_infra_cache_policy_namespace
    ON infra_cache_policy (namespace);

CREATE INDEX idx_infra_cache_policy_active
    ON infra_cache_policy (active);

CREATE TABLE infra_cache_invalidation (
    id UNIQUEIDENTIFIER NOT NULL PRIMARY KEY,
    cache_policy_id UNIQUEIDENTIFIER NOT NULL,
    invalidate_key NVARCHAR(256) NOT NULL,
    reason_type NVARCHAR(32) NOT NULL,
    reason_ref NVARCHAR(128) NULL,
    invalidated_at DATETIME2 NOT NULL,
    CONSTRAINT fk_infra_cache_invalidation_policy
        FOREIGN KEY (cache_policy_id) REFERENCES infra_cache_policy (id)
);

CREATE INDEX idx_infra_cache_invalidation_policy_time
    ON infra_cache_invalidation (cache_policy_id, invalidated_at);
