-- ============================================================
-- V120: Keep full report refresh failure diagnostics
-- ============================================================

ALTER TABLE dbo.data_report_snapshot
    ALTER COLUMN error_message NVARCHAR(MAX) NULL;
