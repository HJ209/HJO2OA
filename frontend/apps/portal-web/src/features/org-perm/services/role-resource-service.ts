import { del, get, post, put } from '@/services/request'
import type { PageData } from '@/types/api'
import {
  resolveCurrentTenantId,
  toPageData,
} from '@/features/org-perm/services/service-utils'
import type {
  ApiPermissionDecision,
  ListQuery,
  ResourceAction,
  ResourceDefinitionPayload,
  ResourceNode,
  ResourcePermissionPayload,
  ResourceType,
  Role,
  RolePayload,
} from '@/features/org-perm/types/org-perm'

const ROLE_URL = '/v1/org/roles'
const RESOURCE_URL = '/v1/org/resources'
const PERMISSION_URL = '/v1/org/permissions'

interface BackendRole {
  id: string
  code: string
  name: string
  description?: string | null
  status: 'ENABLED' | 'DISABLED' | 'DRAFT' | 'ACTIVE' | 'INACTIVE'
}

interface BackendResource {
  id: string
  resourceType: ResourceType | 'PAGE' | 'RESOURCE_ACTION'
  resourceCode: string
  name: string
  parentCode?: string | null
  status: 'ACTIVE' | 'DISABLED'
}

interface BackendResourcePermission {
  id: string
  resourceType: ResourceType | 'PAGE' | 'RESOURCE_ACTION'
  resourceCode: string
  action: ResourceAction
  effect: 'ALLOW' | 'DENY'
}

interface BackendApiPermissionDecision {
  allowed: boolean
  effect: 'ALLOW' | 'DENY'
  matchedPermissions: BackendResourcePermission[]
  snapshot: {
    roleIds: string[]
    version: number
  }
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

function mapResourceType(value: BackendResource['resourceType']): ResourceType {
  switch (value) {
    case 'PAGE':
      return 'MENU'
    case 'RESOURCE_ACTION':
      return 'BUTTON'
    default:
      return value
  }
}

function resourceNodeId(
  type: ResourceType,
  code: string,
  action: ResourceAction,
): string {
  return `${type}:${code}:${action}`
}

function mapResourcePermission(item: BackendResourcePermission): ResourceNode {
  const type = mapResourceType(item.resourceType)

  return {
    id: resourceNodeId(type, item.resourceCode, item.action),
    name: item.resourceCode,
    code: item.resourceCode,
    type,
    action: item.action,
    effect: item.effect,
    checked: item.effect === 'ALLOW',
  }
}

function buildResourceTree(
  resources: BackendResource[],
  permissions: BackendResourcePermission[],
): ResourceNode[] {
  const permissionMap = new Map(
    permissions.map((item) => [
      resourceNodeId(
        mapResourceType(item.resourceType),
        item.resourceCode,
        item.action,
      ),
      item,
    ]),
  )
  const nodes = new Map<string, ResourceNode>()

  for (const resource of resources) {
    const type = mapResourceType(resource.resourceType)
    const action: ResourceAction = type === 'API' ? 'READ' : 'READ'
    const id = resourceNodeId(type, resource.resourceCode, action)
    const permission = permissionMap.get(id)

    nodes.set(id, {
      id,
      parentId: resource.parentCode ?? null,
      name: resource.name,
      code: resource.resourceCode,
      type,
      action,
      effect: permission?.effect ?? 'DENY',
      checked: permission?.effect === 'ALLOW',
      children: [],
    })
  }

  for (const permission of permissions) {
    const type = mapResourceType(permission.resourceType)
    const id = resourceNodeId(type, permission.resourceCode, permission.action)

    if (!nodes.has(id)) {
      nodes.set(id, mapResourcePermission(permission))
    }
  }

  const roots: ResourceNode[] = []
  const byCode = new Map(
    Array.from(nodes.values()).map((node) => [node.code, node]),
  )

  for (const node of nodes.values()) {
    if (node.parentId && byCode.has(node.parentId)) {
      byCode.get(node.parentId)?.children?.push(node)
    } else {
      roots.push(node)
    }
  }

  return roots
}

function flattenResourceNodes(nodes: ResourceNode[]): ResourceNode[] {
  return nodes.flatMap((node) => [
    node,
    ...flattenResourceNodes(node.children ?? []),
  ])
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

export async function listResourceDefinitions(): Promise<BackendResource[]> {
  const tenantId = await resolveCurrentTenantId()
  const params = new URLSearchParams()
  params.set('tenantId', tenantId)
  params.set('status', 'ACTIVE')

  return get<BackendResource[]>(RESOURCE_URL, { params })
}

export async function getRoleResources(
  roleId: string,
): Promise<ResourceNode[]> {
  const [resources, permissions] = await Promise.all([
    listResourceDefinitions(),
    get<BackendResourcePermission[]>(
      `${ROLE_URL}/${roleId}/resource-permissions`,
    ),
  ])

  return buildResourceTree(resources, permissions)
}

export async function saveRoleResources(
  roleId: string,
  resources: ResourceNode[],
  checkedIds: string[],
): Promise<ResourceNode[]> {
  const checkedSet = new Set(checkedIds)
  const permissions: ResourcePermissionPayload[] = flattenResourceNodes(
    resources,
  )
    .filter((node) => checkedSet.has(node.id))
    .map((node) => ({
      resourceType: node.type,
      resourceCode: node.code,
      action: node.action ?? 'READ',
      effect: 'ALLOW',
    }))

  await put<
    BackendResourcePermission[],
    { permissions: ResourcePermissionPayload[] }
  >(
    `${ROLE_URL}/${roleId}/resource-permissions`,
    { permissions },
    {
      dedupeKey: `role:resources:${roleId}`,
    },
  )

  return getRoleResources(roleId)
}

export async function saveResourceDefinition(
  payload: ResourceDefinitionPayload,
): Promise<BackendResource> {
  const tenantId = await resolveCurrentTenantId()

  return post<
    BackendResource,
    ResourceDefinitionPayload & { tenantId: string }
  >(
    RESOURCE_URL,
    {
      ...payload,
      tenantId,
      status: payload.status ?? 'ACTIVE',
    },
    {
      dedupeKey: `resource:${payload.resourceType}:${payload.resourceCode}`,
    },
  )
}

export async function deleteResourceDefinition(
  resourceId: string,
): Promise<void> {
  await del<void>(`${RESOURCE_URL}/${resourceId}`)
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

export async function bindPositionRoles(
  positionId: string,
  roleIds: string[],
  reason: string,
): Promise<void> {
  const tenantId = await resolveCurrentTenantId()

  await post(
    `${PERMISSION_URL.replace('/permissions', '')}/positions/${positionId}/roles`,
    {
      tenantId,
      roleIds,
      reason,
    },
  )
}

export async function decideApiPermission(input: {
  tenantId: string
  personId: string
  positionId: string
  resourceCode: string
  action: ResourceAction
}): Promise<ApiPermissionDecision> {
  const response = await post<BackendApiPermissionDecision, unknown>(
    `${PERMISSION_URL}/decisions/resource`,
    {
      ...input,
      resourceType: 'API',
    },
  )

  return {
    allowed: response.allowed,
    effect: response.effect,
    matchedPermissions: response.matchedPermissions.map(mapResourcePermission),
    snapshot: response.snapshot,
  }
}
