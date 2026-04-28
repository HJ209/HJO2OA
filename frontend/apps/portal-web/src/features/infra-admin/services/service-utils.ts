import type { PaginationQuery } from '@/types/api'
import { serializePaginationParams } from '@/utils/pagination'
import type { InfraListQuery } from '@/features/infra-admin/types/infra'

export function buildListParams(query: InfraListQuery = {}): URLSearchParams {
  const params = serializePaginationParams(query as PaginationQuery)

  if (query.keyword) {
    params.set('keyword', query.keyword)
  }

  return params
}

export function buildIdempotencyKey(scope: string, id: string): string {
  return `${scope}:${id}:${Date.now()}`
}
