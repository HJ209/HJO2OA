-- ============================================================
-- V6: Report module tables
-- Entity source: ReportDefinitionDO, ReportMetricDefinitionDO,
--               ReportDimensionDefinitionDO, ReportSnapshotDO
-- PK type: NVARCHAR(64) (IdType.INPUT, String id)
-- ============================================================

CREATE TABLE dbo.data_report_def (
    id                   NVARCHAR(64)   NOT NULL PRIMARY KEY,
    code                 NVARCHAR(64)   NOT NULL,
    name                 NVARCHAR(128)  NOT NULL,
    report_type          NVARCHAR(32)   NOT NULL,
    source_scope         NVARCHAR(64)   NULL,
    refresh_mode         NVARCHAR(32)   NOT NULL,
    visibility_mode      NVARCHAR(32)   NULL,
    status               NVARCHAR(32)   NOT NULL,
    tenant_id            NVARCHAR(64)   NOT NULL,
    definition_version   INT            NULL,
    caliber_definition   NVARCHAR(MAX)  NULL,
    refresh_config       NVARCHAR(MAX)  NULL,
    card_protocol        NVARCHAR(MAX)  NULL,
    last_refreshed_at    DATETIME2(6)   NULL,
    last_freshness_status NVARCHAR(32)  NULL,
    last_refresh_batch   NVARCHAR(128)  NULL,
    next_refresh_at      DATETIME2(6)   NULL,
    deleted              BIT            NOT NULL CONSTRAINT DF_data_report_def_deleted DEFAULT ((0)),
    created_at           DATETIME2(6)   NOT NULL CONSTRAINT DF_data_report_def_created_at DEFAULT (SYSUTCDATETIME()),
    updated_at           DATETIME2(6)   NOT NULL CONSTRAINT DF_data_report_def_updated_at DEFAULT (SYSUTCDATETIME())
);

CREATE UNIQUE INDEX UX_data_report_def_tenant_code
    ON dbo.data_report_def (tenant_id, code) WHERE deleted = 0;
CREATE INDEX IX_data_report_def_refresh_due
    ON dbo.data_report_def (refresh_mode, status, next_refresh_at);

CREATE TABLE dbo.data_report_metric_def (
    id               NVARCHAR(64)   NOT NULL PRIMARY KEY,
    report_id        NVARCHAR(64)   NOT NULL,
    metric_code      NVARCHAR(64)   NOT NULL,
    metric_name      NVARCHAR(128)  NOT NULL,
    aggregation_type NVARCHAR(32)   NOT NULL,
    source_field     NVARCHAR(128)  NULL,
    formula          NVARCHAR(512)  NULL,
    filter_expression NVARCHAR(512) NULL,
    unit             NVARCHAR(32)   NULL,
    trend_enabled    BIT            NULL,
    rank_enabled     BIT            NULL,
    display_order    INT            NULL,
    deleted          BIT            NOT NULL CONSTRAINT DF_data_report_metric_def_deleted DEFAULT ((0)),
    created_at       DATETIME2(6)   NOT NULL CONSTRAINT DF_data_report_metric_def_created_at DEFAULT (SYSUTCDATETIME()),
    updated_at       DATETIME2(6)   NOT NULL CONSTRAINT DF_data_report_metric_def_updated_at DEFAULT (SYSUTCDATETIME()),
    CONSTRAINT FK_data_report_metric_def_report
        FOREIGN KEY (report_id) REFERENCES dbo.data_report_def(id)
);

CREATE UNIQUE INDEX UX_data_report_metric_def_report_code
    ON dbo.data_report_metric_def (report_id, metric_code) WHERE deleted = 0;

CREATE TABLE dbo.data_report_dimension_def (
    id               NVARCHAR(64)   NOT NULL PRIMARY KEY,
    report_id        NVARCHAR(64)   NOT NULL,
    dimension_code   NVARCHAR(64)   NOT NULL,
    dimension_name   NVARCHAR(128)  NOT NULL,
    dimension_type   NVARCHAR(32)   NOT NULL,
    source_field     NVARCHAR(128)  NULL,
    time_granularity NVARCHAR(32)   NULL,
    filterable       BIT            NULL,
    display_order    INT            NULL,
    deleted          BIT            NOT NULL CONSTRAINT DF_data_report_dimension_def_deleted DEFAULT ((0)),
    created_at       DATETIME2(6)   NOT NULL CONSTRAINT DF_data_report_dimension_def_created_at DEFAULT (SYSUTCDATETIME()),
    updated_at       DATETIME2(6)   NOT NULL CONSTRAINT DF_data_report_dimension_def_updated_at DEFAULT (SYSUTCDATETIME()),
    CONSTRAINT FK_data_report_dimension_def_report
        FOREIGN KEY (report_id) REFERENCES dbo.data_report_def(id)
);

CREATE UNIQUE INDEX UX_data_report_dimension_def_report_code
    ON dbo.data_report_dimension_def (report_id, dimension_code) WHERE deleted = 0;

CREATE TABLE dbo.data_report_snapshot (
    id               NVARCHAR(64)   NOT NULL PRIMARY KEY,
    report_id        NVARCHAR(64)   NOT NULL,
    snapshot_at      DATETIME2(6)   NOT NULL,
    refresh_batch    NVARCHAR(128)  NOT NULL,
    scope_signature  NVARCHAR(128)  NOT NULL,
    payload          NVARCHAR(MAX)  NULL,
    freshness_status NVARCHAR(32)   NOT NULL,
    trigger_mode     NVARCHAR(32)   NOT NULL,
    trigger_reason   NVARCHAR(256)  NULL,
    error_message    NVARCHAR(1024) NULL,
    deleted          BIT            NOT NULL CONSTRAINT DF_data_report_snapshot_deleted DEFAULT ((0)),
    created_at       DATETIME2(6)   NOT NULL CONSTRAINT DF_data_report_snapshot_created_at DEFAULT (SYSUTCDATETIME()),
    updated_at       DATETIME2(6)   NOT NULL CONSTRAINT DF_data_report_snapshot_updated_at DEFAULT (SYSUTCDATETIME()),
    CONSTRAINT FK_data_report_snapshot_report
        FOREIGN KEY (report_id) REFERENCES dbo.data_report_def(id)
);

CREATE UNIQUE INDEX UX_data_report_snapshot_report_batch
    ON dbo.data_report_snapshot (report_id, refresh_batch) WHERE deleted = 0;
CREATE INDEX IX_data_report_snapshot_freshness
    ON dbo.data_report_snapshot (freshness_status, snapshot_at DESC) WHERE deleted = 0;
