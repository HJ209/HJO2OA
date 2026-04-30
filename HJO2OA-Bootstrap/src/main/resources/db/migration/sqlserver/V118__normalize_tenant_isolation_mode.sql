-- Normalize legacy tenant isolation mode values to the runtime enum contract.

UPDATE dbo.infra_tenant_profile
SET isolation_mode = 'SHARED_DB'
WHERE isolation_mode = 'SHARED_SCHEMA';

UPDATE dbo.infra_tenant_profile
SET isolation_mode = 'DEDICATED_DB'
WHERE isolation_mode = 'DEDICATED_SCHEMA';
