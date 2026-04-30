IF NOT EXISTS (
    SELECT 1 FROM sys.indexes
    WHERE name = 'IX_infra_audit_record_operator_account'
      AND object_id = OBJECT_ID('dbo.infra_audit_record')
)
BEGIN
    CREATE INDEX IX_infra_audit_record_operator_account
        ON dbo.infra_audit_record(operator_account_id, occurred_at DESC);
END;

IF NOT EXISTS (
    SELECT 1 FROM sys.indexes
    WHERE name = 'IX_infra_audit_record_operator_person'
      AND object_id = OBJECT_ID('dbo.infra_audit_record')
)
BEGIN
    CREATE INDEX IX_infra_audit_record_operator_person
        ON dbo.infra_audit_record(operator_person_id, occurred_at DESC);
END;

IF NOT EXISTS (
    SELECT 1 FROM sys.indexes
    WHERE name = 'IX_infra_audit_record_trace'
      AND object_id = OBJECT_ID('dbo.infra_audit_record')
)
BEGIN
    CREATE INDEX IX_infra_audit_record_trace
        ON dbo.infra_audit_record(trace_id, occurred_at DESC);
END;

IF NOT EXISTS (
    SELECT 1 FROM sys.indexes
    WHERE name = 'IX_infra_audit_record_object_action'
      AND object_id = OBJECT_ID('dbo.infra_audit_record')
)
BEGIN
    CREATE INDEX IX_infra_audit_record_object_action
        ON dbo.infra_audit_record(tenant_id, object_type, object_id, action_type, occurred_at DESC);
END;
