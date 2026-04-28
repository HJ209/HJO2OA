import { get, post, put } from '@/services/request'
import type { PageData } from '@/types/api'
import { serializePaginationParams } from '@/utils/pagination'
import type {
  DataPermissionPolicy,
  DataPermissionPolicyPayload,
  ListQuery,
} from '@/features/org-perm/types/org-perm'

const POLICY_URL = '/v1/org/data-permissions/policies'

export function listDataPermissionPolicies(
  query: ListQuery = {},
): Promise<PageData<DataPermissionPolicy>> {
  const params = serializePaginationParams(query)

  if (query.keyword) {
    params.set('keyword', query.keyword)
  }

  return get<PageData<DataPermissionPolicy>>(POLICY_URL, { params })
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
