import { del, get, post, put } from '@/services/request'
import type { PageData } from '@/types/api'
import {
  resolveCurrentTenantId,
  toPageData,
} from '@/features/org-perm/services/service-utils'
import type {
  DataPermissionPolicy,
  DataPermissionPolicyPayload,
  FieldMaskResult,
  FieldPermissionPayload,
  FieldPermissionPolicy,
  ListQuery,
  PermissionDecision,
  PermissionIdentityPayload,
  PermissionSubjectType,
} from '@/features/org-perm/types/org-perm'

const ROW_POLICY_URL = '/v1/org/data-permissions/row-policies'
const FIELD_POLICY_URL = '/v1/org/data-permissions/field-policies'
const ROW_DECISION_URL = '/v1/org/data-permissions/decisions/row'
const FIELD_MASK_URL = '/v1/org/data-permissions/decisions/field-mask'

interface BackendRowPolicy {
  id: string
  subjectType: PermissionSubjectType
  subjectId: string
  businessObject: string
  scopeType:
    | 'ALL'
    | 'ORG_AND_CHILDREN'
    | 'DEPT_AND_CHILDREN'
    | 'SELF'
    | 'CUSTOM'
    | 'CONDITION'
  conditionExpr?: string | null
  effect?: 'ALLOW' | 'DENY'
  priority: number
  updatedAt?: string
}

interface BackendFieldPolicy {
  id: string
  subjectType: PermissionSubjectType
  subjectId: string
  businessObject: string
  usageScenario: string
  fieldCode: string
  action: FieldPermissionPolicy['action']
  effect?: 'ALLOW' | 'DENY'
  updatedAt?: string
}

interface BackendRowDecision {
  allowed: boolean
  scopeType?: DataPermissionPolicy['scopeType']
  sqlCondition?: string
  effect: 'ALLOW' | 'DENY'
  matchedPolicies: BackendRowPolicy[]
}

interface BackendFieldMask {
  row: Record<string, unknown>
  decision: {
    hiddenFields: string[]
    desensitizedFields: string[]
    matchedPolicies: BackendFieldPolicy[]
  }
}

function mapRowPolicy(item: BackendRowPolicy): DataPermissionPolicy {
  return {
    id: item.id,
    name: item.businessObject,
    code: item.businessObject,
    subjectType: item.subjectType,
    subjectId: item.subjectId,
    businessObject: item.businessObject,
    scopeType: item.scopeType === 'CUSTOM' ? 'CONDITION' : item.scopeType,
    conditionExpr: item.conditionExpr,
    effect: item.effect ?? 'ALLOW',
    priority: item.priority,
    targetRoleId: item.subjectType === 'ROLE' ? item.subjectId : undefined,
    enabled: item.effect !== 'DENY',
    updatedAtUtc: item.updatedAt,
  }
}

function mapFieldPolicy(item: BackendFieldPolicy): FieldPermissionPolicy {
  return {
    id: item.id,
    subjectType: item.subjectType,
    subjectId: item.subjectId,
    businessObject: item.businessObject,
    usageScenario: item.usageScenario,
    fieldCode: item.fieldCode,
    action: item.action,
    effect: item.effect ?? 'ALLOW',
    updatedAtUtc: item.updatedAt,
  }
}

function filterPolicies<
  T extends {
    name?: string
    code?: string
    businessObject?: string
    fieldCode?: string
  },
>(items: T[], query: ListQuery): T[] {
  const keyword = query.keyword?.trim().toLowerCase()

  if (!keyword) {
    return items
  }

  return items.filter((item) =>
    [item.name, item.code, item.businessObject, item.fieldCode]
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

export async function listFieldPermissionPolicies(
  query: ListQuery = {},
): Promise<PageData<FieldPermissionPolicy>> {
  const tenantId = await resolveCurrentTenantId()
  const params = new URLSearchParams()
  params.set('tenantId', tenantId)
  const items = await get<BackendFieldPolicy[]>(FIELD_POLICY_URL, { params })
  const mappedItems = filterPolicies(items.map(mapFieldPolicy), query)

  return toPageData(mappedItems, query)
}

export async function createDataPermissionPolicy(
  payload: DataPermissionPolicyPayload,
): Promise<DataPermissionPolicy> {
  const tenantId = await resolveCurrentTenantId()
  const item = await post<
    BackendRowPolicy,
    DataPermissionPolicyPayload & { tenantId: string }
  >(
    ROW_POLICY_URL,
    {
      ...payload,
      tenantId,
    },
    {
      dedupeKey: `data-permission:create:${payload.businessObject}:${payload.subjectId}`,
    },
  )

  return mapRowPolicy(item)
}

export async function updateDataPermissionPolicy(
  id: string,
  payload: DataPermissionPolicyPayload,
): Promise<DataPermissionPolicy> {
  const tenantId = await resolveCurrentTenantId()
  const item = await put<
    BackendRowPolicy,
    DataPermissionPolicyPayload & { tenantId: string }
  >(
    `${ROW_POLICY_URL}/${id}`,
    {
      ...payload,
      tenantId,
    },
    {
      dedupeKey: `data-permission:update:${id}`,
    },
  )

  return mapRowPolicy(item)
}

export async function deleteDataPermissionPolicy(id: string): Promise<void> {
  await del<void>(`${ROW_POLICY_URL}/${id}`)
}

export async function createFieldPermissionPolicy(
  payload: FieldPermissionPayload,
): Promise<FieldPermissionPolicy> {
  const tenantId = await resolveCurrentTenantId()
  const item = await post<
    BackendFieldPolicy,
    FieldPermissionPayload & { tenantId: string }
  >(
    FIELD_POLICY_URL,
    {
      ...payload,
      tenantId,
    },
    {
      dedupeKey: `field-permission:create:${payload.businessObject}:${payload.fieldCode}`,
    },
  )

  return mapFieldPolicy(item)
}

export async function previewRowPermission(input: {
  businessObject: string
  identityContext: PermissionIdentityPayload
}): Promise<PermissionDecision> {
  const decision = await post<BackendRowDecision, unknown>(
    ROW_DECISION_URL,
    input,
  )

  return {
    allowed: decision.allowed,
    effect: decision.effect,
    scopeType: decision.scopeType,
    sqlCondition: decision.sqlCondition,
    matchedPolicies: decision.matchedPolicies.map(mapRowPolicy),
  }
}

export async function previewFieldMask(input: {
  businessObject: string
  usageScenario: string
  identityContext: PermissionIdentityPayload
  row: Record<string, unknown>
}): Promise<FieldMaskResult> {
  const result = await post<BackendFieldMask, unknown>(FIELD_MASK_URL, input)

  return {
    row: result.row,
    decision: {
      hiddenFields: result.decision.hiddenFields,
      desensitizedFields: result.decision.desensitizedFields,
      matchedPolicies: result.decision.matchedPolicies.map(mapFieldPolicy),
    },
  }
}
