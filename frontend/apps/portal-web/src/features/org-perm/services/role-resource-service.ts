import { get, post, put } from '@/services/request'
import type { PageData } from '@/types/api'
import {
  resolveCurrentTenantId,
  toPageData,
} from '@/features/org-perm/services/service-utils'
import type {
  ListQuery,
  ResourceNode,
  Role,
  RolePayload,
} from '@/features/org-perm/types/org-perm'

const ROLE_URL = '/v1/org-perm/roles'

interface BackendRole {
  id: string
  code: string
  name: string
  description?: string | null
  status: 'ENABLED' | 'DISABLED' | 'DRAFT' | 'ACTIVE' | 'INACTIVE'
}

interface BackendResourcePermission {
  id: string
  resourceType: 'MENU' | 'BUTTON' | 'API' | 'PAGE' | 'RESOURCE_ACTION'
  resourceCode: string
  effect: 'ALLOW' | 'DENY'
}

function mapRole(item: BackendRole): Role {
  return {
    id: item.id,
    code: item.code,
    name: item.name,
    description: item.description ?? undefined,
    enabled: item.status === 'ACTIVE' || item.status === 'ENABLED',
  }
}

function mapResourceType(
  value: BackendResourcePermission['resourceType'],
): ResourceNode['type'] {
  switch (value) {
    case 'PAGE':
      return 'MENU'
    case 'RESOURCE_ACTION':
      return 'BUTTON'
    default:
      return value
  }
}

function mapResourceNode(item: BackendResourcePermission): ResourceNode {
  return {
    id: item.id,
    name: item.resourceCode,
    code: item.resourceCode,
    type: mapResourceType(item.resourceType),
    effect: item.effect,
    checked: item.effect === 'ALLOW',
  }
}

function filterRoles(items: Role[], query: ListQuery): Role[] {
  const keyword = query.keyword?.trim().toLowerCase()

  if (!keyword) {
    return items
  }

  return items.filter((item) =>
    [item.code, item.name, item.description]
      .filter(Boolean)
      .some((value) => value?.toLowerCase().includes(keyword)),
  )
}

export async function listRoles(
  query: ListQuery = {},
): Promise<PageData<Role>> {
  const tenantId = await resolveCurrentTenantId()
  const params = new URLSearchParams()
  params.set('tenantId', tenantId)
  const items = await get<BackendRole[]>(ROLE_URL, { params })

  return toPageData(filterRoles(items.map(mapRole), query), query)
}

export async function getRoleResources(
  roleId: string,
): Promise<ResourceNode[]> {
  const items = await get<BackendResourcePermission[]>(
    `${ROLE_URL}/${roleId}/resource-permissions`,
  )

  return items.map(mapResourceNode)
}

export async function createRole(
  payload: RolePayload,
  idempotencyKey?: string,
): Promise<Role> {
  const tenantId = await resolveCurrentTenantId()
  const item = await post<
    BackendRole,
    {
      code: string
      name: string
      category: 'BUSINESS'
      scope: 'ORGANIZATION'
      description?: string
      tenantId: string
    }
  >(
    ROLE_URL,
    {
      code: payload.code,
      name: payload.name,
      category: 'BUSINESS',
      scope: 'ORGANIZATION',
      description: payload.description,
      tenantId,
    },
    {
      dedupeKey: `role:create:${payload.code}`,
      idempotencyKey,
    },
  )

  return mapRole(item)
}

export async function updateRole(
  id: string,
  payload: RolePayload,
  idempotencyKey?: string,
): Promise<Role> {
  const item = await put<
    BackendRole,
    {
      code: string
      name: string
      category: 'BUSINESS'
      scope: 'ORGANIZATION'
      description?: string
    }
  >(
    `${ROLE_URL}/${id}`,
    {
      code: payload.code,
      name: payload.name,
      category: 'BUSINESS',
      scope: 'ORGANIZATION',
      description: payload.description,
    },
    {
      dedupeKey: `role:update:${id}`,
      idempotencyKey,
    },
  )

  return mapRole(item)
}
