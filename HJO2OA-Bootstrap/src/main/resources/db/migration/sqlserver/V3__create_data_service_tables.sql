-- ============================================================
-- V3: DataService module tables
-- Entity source: DataServiceDefinitionEntity, ServiceParameterDefinitionEntity,
--               ServiceFieldMappingEntity (all extend BaseEntityDO)
-- BaseEntityDO: id UNIQUEIDENTIFIER, tenant_id UNIQUEIDENTIFIER,
--               created_at, updated_at, deleted INT
-- ============================================================

CREATE TABLE dbo.data_service_def (
    id                      UNIQUEIDENTIFIER NOT NULL PRIMARY KEY,
    tenant_id               UNIQUEIDENTIFIER NOT NULL,
    code                    NVARCHAR(64)     NOT NULL,
    name                    NVARCHAR(128)    NOT NULL,
    service_type            NVARCHAR(32)     NOT NULL,
    source_mode             NVARCHAR(32)     NOT NULL,
    permission_mode         NVARCHAR(32)     NOT NULL,
    permission_boundary_json NVARCHAR(MAX)   NULL,
    cache_policy_json       NVARCHAR(MAX)    NULL,
    status                  NVARCHAR(32)     NOT NULL,
    source_ref              NVARCHAR(256)    NULL,
    connector_id            UNIQUEIDENTIFIER NULL,
    description             NVARCHAR(512)    NULL,
    status_sequence         INT              NULL,
    activated_at            DATETIME2(6)     NULL,
    created_by              NVARCHAR(128)    NULL,
    updated_by              NVARCHAR(128)    NULL,
    deleted                 INT              NOT NULL CONSTRAINT DF_data_service_def_deleted DEFAULT ((0)),
    created_at              DATETIME2(6)     NOT NULL CONSTRAINT DF_data_service_def_created_at DEFAULT (SYSUTCDATETIME()),
    updated_at              DATETIME2(6)     NOT NULL CONSTRAINT DF_data_service_def_updated_at DEFAULT (SYSUTCDATETIME())
);

CREATE UNIQUE INDEX UX_data_service_def_tenant_code
    ON dbo.data_service_def (tenant_id, code) WHERE deleted = 0;
CREATE INDEX IX_data_service_def_status
    ON dbo.data_service_def (status, service_type) WHERE deleted = 0;

CREATE TABLE dbo.data_service_param_def (
    id                    UNIQUEIDENTIFIER NOT NULL PRIMARY KEY,
    service_id            UNIQUEIDENTIFIER NOT NULL,
    param_code            NVARCHAR(64)     NOT NULL,
    param_type            NVARCHAR(32)     NOT NULL,
    required              BIT              NULL,
    default_value         NVARCHAR(512)    NULL,
    validation_rule_json  NVARCHAR(MAX)    NULL,
    enabled               BIT              NULL,
    description           NVARCHAR(512)    NULL,
    sort_order            INT              NULL,
    created_by            NVARCHAR(128)    NULL,
    updated_by            NVARCHAR(128)    NULL,
    tenant_id             UNIQUEIDENTIFIER NOT NULL,
    deleted               INT              NOT NULL CONSTRAINT DF_data_service_param_def_deleted DEFAULT ((0)),
    created_at            DATETIME2(6)     NOT NULL CONSTRAINT DF_data_service_param_def_created_at DEFAULT (SYSUTCDATETIME()),
    updated_at            DATETIME2(6)     NOT NULL CONSTRAINT DF_data_service_param_def_updated_at DEFAULT (SYSUTCDATETIME()),
    CONSTRAINT FK_data_service_param_def_service
        FOREIGN KEY (service_id) REFERENCES dbo.data_service_def(id)
);

CREATE UNIQUE INDEX UX_data_service_param_def_service_id_param_code
    ON dbo.data_service_param_def (service_id, param_code) WHERE deleted = 0;

CREATE TABLE dbo.data_service_field_mapping (
    id                    UNIQUEIDENTIFIER NOT NULL PRIMARY KEY,
    service_id            UNIQUEIDENTIFIER NOT NULL,
    source_field          NVARCHAR(128)    NOT NULL,
    target_field          NVARCHAR(128)    NOT NULL,
    transform_rule_json   NVARCHAR(MAX)    NULL,
    masked                BIT              NULL,
    description           NVARCHAR(512)    NULL,
    sort_order            INT              NULL,
    created_by            NVARCHAR(128)    NULL,
    updated_by            NVARCHAR(128)    NULL,
    tenant_id             UNIQUEIDENTIFIER NOT NULL,
    deleted               INT              NOT NULL CONSTRAINT DF_data_service_field_mapping_deleted DEFAULT ((0)),
    created_at            DATETIME2(6)     NOT NULL CONSTRAINT DF_data_service_field_mapping_created_at DEFAULT (SYSUTCDATETIME()),
    updated_at            DATETIME2(6)     NOT NULL CONSTRAINT DF_data_service_field_mapping_updated_at DEFAULT (SYSUTCDATETIME()),
    CONSTRAINT FK_data_service_field_mapping_service
        FOREIGN KEY (service_id) REFERENCES dbo.data_service_def(id)
);

CREATE UNIQUE INDEX UX_data_service_field_mapping_service_id_source_target
    ON dbo.data_service_field_mapping (service_id, source_field, target_field) WHERE deleted = 0;
