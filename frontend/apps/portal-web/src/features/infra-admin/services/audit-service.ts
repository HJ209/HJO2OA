import { get } from '@/services/request'
import { buildListParams } from '@/features/infra-admin/services/service-utils'
import type {
  AuditFilterValues,
  AuditRecord,
  InfraListQuery,
  InfraPageData,
} from '@/features/infra-admin/types/infra'
import type { PaginationFilter } from '@/types/api'

const BASE_URL = '/v1/infra/audit/records'

export function buildAuditQuery(filters: AuditFilterValues): InfraListQuery {
  const queryFilters: PaginationFilter[] = []

  if (filters.actor) {
    queryFilters.push({
      field: 'actor',
      operator: 'like',
      value: filters.actor,
    })
  }

  if (filters.action) {
    queryFilters.push({ field: 'action', value: filters.action })
  }

  if (filters.resource) {
    queryFilters.push({
      field: 'resource',
      operator: 'like',
      value: filters.resource,
    })
  }

  if (filters.from) {
    queryFilters.push({
      field: 'createdAt',
      operator: 'gte',
      value: filters.from,
    })
  }

  if (filters.to) {
    queryFilters.push({
      field: 'createdAt',
      operator: 'lte',
      value: filters.to,
    })
  }

  return {
    page: 1,
    size: 20,
    filters: queryFilters,
    sort: [{ field: 'createdAt', direction: 'desc' }],
  }
}

export const auditService = {
  list(query?: InfraListQuery): Promise<InfraPageData<AuditRecord>> {
    return get(BASE_URL, { params: buildListParams(query) })
  },
}
