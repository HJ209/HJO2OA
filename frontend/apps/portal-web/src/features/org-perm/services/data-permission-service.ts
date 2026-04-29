import { get, post, put } from '@/services/request'
import type { PageData } from '@/types/api'
import {
  resolveCurrentTenantId,
  toPageData,
} from '@/features/org-perm/services/service-utils'
import type {
  DataPermissionPolicy,
  DataPermissionPolicyPayload,
  ListQuery,
} from '@/features/org-perm/types/org-perm'

const POLICY_URL = '/v1/org/data-permissions/policies'
const ROW_POLICY_URL = '/org-perm/data-permissions/row-policies'

interface BackendRowPolicy {
  id: string
  subjectType: 'ROLE' | 'PERSON' | 'POSITION'
  subjectId: string
  businessObject: string
  scopeType:
    | 'ALL'
    | 'ORG_AND_CHILDREN'
    | 'DEPT_AND_CHILDREN'
    | 'SELF'
    | 'CUSTOM'
    | 'CONDITION'
  effect?: 'ALLOW' | 'DENY'
  updatedAt?: string
}

function mapScopeType(
  value: BackendRowPolicy['scopeType'],
): DataPermissionPolicy['scopeType'] {
  switch (value) {
    case 'SELF':
      return 'SELF'
    case 'ORG_AND_CHILDREN':
      return 'ORG_AND_CHILDREN'
    default:
      return 'CUSTOM'
  }
}

function mapRowPolicy(item: BackendRowPolicy): DataPermissionPolicy {
  return {
    id: item.id,
    name: item.businessObject,
    code: item.businessObject,
    scopeType: mapScopeType(item.scopeType),
    targetRoleId: item.subjectType === 'ROLE' ? item.subjectId : undefined,
    enabled: item.effect !== 'DENY',
    updatedAtUtc: item.updatedAt,
  }
}

function filterPolicies(
  items: DataPermissionPolicy[],
  query: ListQuery,
): DataPermissionPolicy[] {
  const keyword = query.keyword?.trim().toLowerCase()

  if (!keyword) {
    return items
  }

  return items.filter((item) =>
    [item.name, item.code, item.scopeType]
      .filter(Boolean)
      .some((value) => value?.toLowerCase().includes(keyword)),
  )
}

export async function listDataPermissionPolicies(
  query: ListQuery = {},
): Promise<PageData<DataPermissionPolicy>> {
  const tenantId = await resolveCurrentTenantId()
  const params = new URLSearchParams()
  params.set('tenantId', tenantId)
  const items = await get<BackendRowPolicy[]>(ROW_POLICY_URL, { params })
  const mappedItems = filterPolicies(items.map(mapRowPolicy), query)

  return toPageData(mappedItems, query)
}

export function createDataPermissionPolicy(
  payload: DataPermissionPolicyPayload,
  idempotencyKey?: string,
): Promise<DataPermissionPolicy> {
  return post<DataPermissionPolicy, DataPermissionPolicyPayload>(
    POLICY_URL,
    payload,
    {
      dedupeKey: `data-permission:create:${payload.code}`,
      idempotencyKey,
    },
  )
}

export function updateDataPermissionPolicy(
  id: string,
  payload: DataPermissionPolicyPayload,
  idempotencyKey?: string,
): Promise<DataPermissionPolicy> {
  return put<DataPermissionPolicy, DataPermissionPolicyPayload>(
    `${POLICY_URL}/${id}`,
    payload,
    {
      dedupeKey: `data-permission:update:${id}`,
      idempotencyKey,
    },
  )
}
