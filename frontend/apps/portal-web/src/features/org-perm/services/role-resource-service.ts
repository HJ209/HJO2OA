import { get, post, put } from '@/services/request'
import type { PageData } from '@/types/api'
import { serializePaginationParams } from '@/utils/pagination'
import type {
  ListQuery,
  ResourceNode,
  Role,
  RolePayload,
} from '@/features/org-perm/types/org-perm'

const ROLE_URL = '/v1/org/roles'

export function listRoles(query: ListQuery = {}): Promise<PageData<Role>> {
  const params = serializePaginationParams(query)

  if (query.keyword) {
    params.set('keyword', query.keyword)
  }

  return get<PageData<Role>>(ROLE_URL, { params })
}

export function getRoleResources(roleId: string): Promise<ResourceNode[]> {
  return get<ResourceNode[]>(`${ROLE_URL}/${roleId}/resources`)
}

export function createRole(
  payload: RolePayload,
  idempotencyKey?: string,
): Promise<Role> {
  return post<Role, RolePayload>(ROLE_URL, payload, {
    dedupeKey: `role:create:${payload.code}`,
    idempotencyKey,
  })
}

export function updateRole(
  id: string,
  payload: RolePayload,
  idempotencyKey?: string,
): Promise<Role> {
  return put<Role, RolePayload>(`${ROLE_URL}/${id}`, payload, {
    dedupeKey: `role:update:${id}`,
    idempotencyKey,
  })
}
