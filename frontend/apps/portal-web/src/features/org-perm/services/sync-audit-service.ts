import { get } from '@/services/request'
import type { PageData } from '@/types/api'
import { serializePaginationParams } from '@/utils/pagination'
import type {
  IdentityContext,
  ListQuery,
  SyncAuditRecord,
} from '@/features/org-perm/types/org-perm'

const IDENTITY_CONTEXT_URL = '/v1/org/identity-context'
const SYNC_AUDIT_URL = '/v1/org/sync-audit/records'

export function getIdentityContext(): Promise<IdentityContext> {
  return get<IdentityContext>(IDENTITY_CONTEXT_URL)
}

export function listSyncAuditRecords(
  query: ListQuery = {},
): Promise<PageData<SyncAuditRecord>> {
  const params = serializePaginationParams(query)

  if (query.keyword) {
    params.set('keyword', query.keyword)
  }

  return get<PageData<SyncAuditRecord>>(SYNC_AUDIT_URL, { params })
}
