-- ============================================================
-- V119: Align report snapshots with tenant line interception
-- data_report_snapshot is tenant scoped in runtime, so the table
-- must carry tenant_id just like data_report_def.
-- ============================================================

ALTER TABLE dbo.data_report_snapshot
    ADD tenant_id NVARCHAR(64) NULL;

EXEC(N'
UPDATE snapshot
   SET tenant_id = definition.tenant_id
  FROM dbo.data_report_snapshot snapshot
  JOIN dbo.data_report_def definition
    ON definition.id = snapshot.report_id
 WHERE snapshot.tenant_id IS NULL
');

EXEC(N'
UPDATE dbo.data_report_snapshot
   SET tenant_id = ''11111111-1111-1111-1111-111111111111''
 WHERE tenant_id IS NULL
');

EXEC(N'
ALTER TABLE dbo.data_report_snapshot
    ALTER COLUMN tenant_id NVARCHAR(64) NOT NULL
');

EXEC(N'
CREATE INDEX IX_data_report_snapshot_tenant_report
    ON dbo.data_report_snapshot (tenant_id, report_id, snapshot_at DESC)
    WHERE deleted = 0
');
