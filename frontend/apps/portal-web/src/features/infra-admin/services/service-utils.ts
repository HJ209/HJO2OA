import { useAuthStore } from '@/stores/auth-store'
import { get } from '@/services/request'
import type { PageData, PaginationQuery } from '@/types/api'
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

interface BackendIdentityContextView {
  tenantId: string
}

function isUuid(value: string | null | undefined): value is string {
  return Boolean(
    value &&
    /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(
      value,
    ),
  )
}

function decodeJwtTenantId(token: string | null | undefined): string | null {
  if (!token) {
    return null
  }

  const segments = token.split('.')

  if (segments.length < 2) {
    return null
  }

  try {
    const normalizedPayload = segments[1].replace(/-/g, '+').replace(/_/g, '/')
    const paddedPayload = normalizedPayload.padEnd(
      normalizedPayload.length + ((4 - (normalizedPayload.length % 4)) % 4),
      '=',
    )
    const payload = JSON.parse(atob(paddedPayload)) as Record<string, unknown>
    const tenantId =
      typeof payload.tenantId === 'string' ? payload.tenantId : null

    return isUuid(tenantId) ? tenantId : null
  } catch {
    return null
  }
}

export async function resolveCurrentTenantId(): Promise<string> {
  const authState = useAuthStore.getState()
  const authTenantId = isUuid(authState.user?.tenantId)
    ? authState.user.tenantId
    : null
  const tokenTenantId = decodeJwtTenantId(authState.token)

  if (tokenTenantId ?? authTenantId) {
    return tokenTenantId ?? authTenantId ?? ''
  }

  const identityContext = await get<BackendIdentityContextView>(
    '/v1/org/identity-context/current',
  )

  return identityContext.tenantId
}

export function toPageData<T>(
  items: T[],
  query: PaginationQuery = {},
): PageData<T> {
  const page = Math.max(query.page ?? 1, 1)
  const size = Math.max(query.size ?? (items.length || 20), 1)
  const startIndex = (page - 1) * size
  const pagedItems = items.slice(startIndex, startIndex + size)
  const total = items.length

  return {
    items: pagedItems,
    pagination: {
      page,
      size,
      total,
      totalPages: total === 0 ? 0 : Math.ceil(total / size),
    },
  }
}
