-- ============================================================
-- V2: Connector module tables
-- Entity source: ConnectorDefinitionDO, ConnectorParameterDO, ConnectorHealthSnapshotDO
-- PK type: NVARCHAR(64) (IdType.INPUT, String id)
-- ============================================================

CREATE TABLE dbo.data_connector_def (
    id              NVARCHAR(64)   NOT NULL PRIMARY KEY,
    tenant_id       NVARCHAR(64)   NOT NULL,
    code            NVARCHAR(64)   NOT NULL,
    name            NVARCHAR(128)  NOT NULL,
    connector_type  NVARCHAR(32)   NOT NULL,
    vendor          NVARCHAR(64)   NULL,
    protocol        NVARCHAR(32)   NULL,
    auth_mode       NVARCHAR(32)   NOT NULL,
    timeout_config  NVARCHAR(MAX)  NULL,
    status          NVARCHAR(32)   NOT NULL,
    change_sequence BIGINT         NOT NULL CONSTRAINT DF_data_connector_def_change_sequence DEFAULT ((0)),
    deleted         BIT            NOT NULL CONSTRAINT DF_data_connector_def_deleted DEFAULT ((0)),
    created_at      DATETIME2(3)   NOT NULL CONSTRAINT DF_data_connector_def_created_at DEFAULT (SYSUTCDATETIME()),
    updated_at      DATETIME2(3)   NOT NULL CONSTRAINT DF_data_connector_def_updated_at DEFAULT (SYSUTCDATETIME())
);

CREATE UNIQUE INDEX UX_data_connector_def_tenant_code
    ON dbo.data_connector_def (tenant_id, code);
CREATE INDEX IX_data_connector_def_tenant_type_status
    ON dbo.data_connector_def (tenant_id, connector_type, status);

CREATE TABLE dbo.data_connector_param (
    id              NVARCHAR(64)   NOT NULL PRIMARY KEY,
    connector_id    NVARCHAR(64)   NOT NULL,
    param_key       NVARCHAR(64)   NOT NULL,
    param_value_ref NVARCHAR(512)  NOT NULL,
    sensitive       BIT            NOT NULL CONSTRAINT DF_data_connector_param_sensitive DEFAULT ((0)),
    created_at      DATETIME2(3)   NOT NULL CONSTRAINT DF_data_connector_param_created_at DEFAULT (SYSUTCDATETIME()),
    updated_at      DATETIME2(3)   NOT NULL CONSTRAINT DF_data_connector_param_updated_at DEFAULT (SYSUTCDATETIME()),
    CONSTRAINT FK_data_connector_param_connector
        FOREIGN KEY (connector_id) REFERENCES dbo.data_connector_def(id)
);

CREATE UNIQUE INDEX UX_data_connector_param_connector_key
    ON dbo.data_connector_param (connector_id, param_key);

CREATE TABLE dbo.data_connector_health_snapshot (
    id                 NVARCHAR(64)   NOT NULL PRIMARY KEY,
    connector_id       NVARCHAR(64)   NOT NULL,
    check_type         NVARCHAR(32)   NOT NULL,
    health_status      NVARCHAR(32)   NOT NULL,
    latency_ms         BIGINT         NOT NULL,
    error_code         NVARCHAR(64)   NULL,
    error_summary      NVARCHAR(512)  NULL,
    operator_id        NVARCHAR(64)   NULL,
    target_environment NVARCHAR(64)   NOT NULL,
    confirmed_by       NVARCHAR(64)   NULL,
    confirmation_note  NVARCHAR(256)  NULL,
    confirmed_at       DATETIME2(3)   NULL,
    change_sequence    BIGINT         NOT NULL CONSTRAINT DF_data_connector_health_change_sequence DEFAULT ((0)),
    checked_at         DATETIME2(3)   NOT NULL CONSTRAINT DF_data_connector_health_checked_at DEFAULT (SYSUTCDATETIME()),
    CONSTRAINT FK_data_connector_health_connector
        FOREIGN KEY (connector_id) REFERENCES dbo.data_connector_def(id)
);

CREATE INDEX IX_data_connector_health_connector_checked_at
    ON dbo.data_connector_health_snapshot (connector_id, checked_at DESC);
