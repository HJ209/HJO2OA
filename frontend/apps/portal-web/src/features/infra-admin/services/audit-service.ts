import { get } from '@/services/request'
import {
  buildListParams,
  toPageData,
} from '@/features/infra-admin/services/service-utils'
import type {
  AuditFilterValues,
  AuditRecord,
  AuditRecordDetail,
  InfraListQuery,
  InfraPageData,
} from '@/features/infra-admin/types/infra'

const BASE_URL = '/v1/infra/audits'

export interface AuditQuery extends InfraListQuery {
  tenantId?: string
  moduleCode?: string
  objectType?: string
  objectId?: string
  actionType?: string
  operatorAccountId?: string
  operatorPersonId?: string
  requestId?: string
  from?: string
  to?: string
}

interface BackendAuditFieldChange {
  id: string
  auditRecordId: string
  fieldName: string
  oldValue?: string | null
  newValue?: string | null
  sensitivityLevel?: 'LOW' | 'MEDIUM' | 'HIGH' | null
}

interface BackendAuditRecord {
  id: string
  moduleCode: string
  objectType: string
  objectId: string
  actionType: string
  operatorAccountId?: string | null
  operatorPersonId?: string | null
  tenantId?: string | null
  traceId?: string | null
  summary?: string | null
  occurredAt: string
  archiveStatus: 'ACTIVE' | 'ARCHIVED'
  createdAt: string
  fieldChanges?: BackendAuditFieldChange[]
}

function normalizeDateTime(value?: string): string | undefined {
  if (!value) {
    return undefined
  }

  const date = new Date(value)

  return Number.isNaN(date.getTime()) ? undefined : date.toISOString()
}

export function buildAuditQuery(filters: AuditFilterValues): AuditQuery {
  return {
    page: 1,
    size: 20,
    moduleCode: filters.moduleCode,
    actionType: filters.action || undefined,
    objectType: filters.objectType ?? filters.resource,
    objectId: filters.objectId,
    operatorAccountId: filters.operatorAccountId ?? filters.actor,
    requestId: filters.requestId,
    from: normalizeDateTime(filters.from),
    to: normalizeDateTime(filters.to),
  }
}

function buildAuditParams(query: AuditQuery = {}): URLSearchParams {
  const params = buildListParams(query)

  for (const key of [
    'tenantId',
    'moduleCode',
    'objectType',
    'objectId',
    'actionType',
    'operatorAccountId',
    'operatorPersonId',
    'requestId',
    'from',
    'to',
  ] as const) {
    const value = query[key]

    if (value) {
      params.set(key, value)
    }
  }

  return params
}

function mapAuditRecord(item: BackendAuditRecord): AuditRecord {
  return {
    id: item.id,
    moduleCode: item.moduleCode,
    objectType: item.objectType,
    objectId: item.objectId,
    actionType: item.actionType,
    actor:
      item.operatorAccountId ?? item.operatorPersonId ?? item.tenantId ?? '-',
    action: item.actionType,
    resource: [item.objectType, item.objectId].filter(Boolean).join(':'),
    operatorAccountId: item.operatorAccountId,
    operatorPersonId: item.operatorPersonId,
    tenantId: item.tenantId,
    traceId: item.traceId,
    summary: item.summary,
    occurredAt: item.occurredAt,
    archiveStatus: item.archiveStatus,
    createdAt: item.createdAt,
  }
}

function mapAuditDetail(item: BackendAuditRecord): AuditRecordDetail {
  return {
    ...mapAuditRecord(item),
    fieldChanges: item.fieldChanges ?? [],
  }
}

export const auditService = {
  async list(query?: AuditQuery): Promise<InfraPageData<AuditRecord>> {
    const items = await get<BackendAuditRecord[]>(BASE_URL, {
      params: buildAuditParams(query),
    })

    return toPageData(items.map(mapAuditRecord), query)
  },
  async detail(id: string): Promise<AuditRecordDetail> {
    return mapAuditDetail(await get<BackendAuditRecord>(`${BASE_URL}/${id}`))
  },
}
