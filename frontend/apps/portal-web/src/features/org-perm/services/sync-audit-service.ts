import { get } from '@/services/request'
import type { PageData } from '@/types/api'
import {
  resolveCurrentTenantId,
  toPageData,
} from '@/features/org-perm/services/service-utils'
import type {
  IdentityContext,
  ListQuery,
  SyncAuditRecord,
} from '@/features/org-perm/types/org-perm'

const IDENTITY_CONTEXT_URL = '/v1/org/identity-context/current'
const SYNC_AUDIT_SOURCE_URL = '/v1/org/sync-audit/sources'
const SYNC_AUDIT_TASK_URL = '/v1/org/sync-audit/tasks'

interface BackendIdentityContext {
  tenantId: string
  accountId: string
  personName?: string
  currentOrganizationId?: string
  roleIds?: string[]
}

interface BackendSyncSource {
  id: string
  sourceCode: string
  sourceName: string
}

interface BackendSyncTask {
  id: string
  sourceId: string
  status: 'CREATED' | 'RUNNING' | 'COMPLETED' | 'FAILED'
  successCount: number
  failureCount: number
  diffCount: number
  failureReason?: string | null
  startedAt?: string
  finishedAt?: string
  createdAt?: string
}

function mapStatus(
  taskStatus: BackendSyncTask['status'],
): SyncAuditRecord['status'] {
  switch (taskStatus) {
    case 'COMPLETED':
      return 'SUCCESS'
    case 'FAILED':
      return 'FAILED'
    default:
      return 'RUNNING'
  }
}

export async function getIdentityContext(): Promise<IdentityContext> {
  const data = await get<BackendIdentityContext>(IDENTITY_CONTEXT_URL)

  return {
    accountId: data.accountId,
    displayName: data.personName ?? data.accountId,
    orgId: data.currentOrganizationId,
    roleIds: data.roleIds ?? [],
  }
}

export async function listSyncAuditRecords(
  query: ListQuery = {},
): Promise<PageData<SyncAuditRecord>> {
  const tenantId = await resolveCurrentTenantId()
  const params = new URLSearchParams()
  params.set('tenantId', tenantId)

  const [sources, tasks] = await Promise.all([
    get<BackendSyncSource[]>(SYNC_AUDIT_SOURCE_URL, { params }),
    get<BackendSyncTask[]>(SYNC_AUDIT_TASK_URL, { params }),
  ])
  const sourceMap = new Map(sources.map((source) => [source.id, source]))
  const records = tasks.map((task) => {
    const source = sourceMap.get(task.sourceId)
    const totalCount = task.successCount + task.failureCount + task.diffCount

    return {
      id: task.id,
      sourceSystem: source?.sourceName ?? source?.sourceCode ?? task.sourceId,
      batchNo: task.id,
      status: mapStatus(task.status),
      totalCount,
      successCount: task.successCount,
      failedCount: task.failureCount,
      startedAtUtc: task.startedAt ?? task.createdAt,
      finishedAtUtc: task.finishedAt,
      message: task.failureReason ?? undefined,
    } satisfies SyncAuditRecord
  })

  return toPageData(records, query)
}
