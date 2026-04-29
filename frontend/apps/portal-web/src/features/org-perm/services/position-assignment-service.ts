import { get, post, put } from '@/services/request'
import type { PageData } from '@/types/api'
import {
  resolveCurrentTenantId,
  toPageData,
} from '@/features/org-perm/services/service-utils'
import type {
  ListQuery,
  PositionAssignment,
  PositionAssignmentPayload,
} from '@/features/org-perm/types/org-perm'

const POSITION_URL = '/v1/org/position-assignments/positions'

interface BackendPositionResponse {
  id: string
  name: string
  organizationId: string
  status: string
  createdAt?: string
}

function mapPositionToAssignment(
  item: BackendPositionResponse,
): PositionAssignment {
  return {
    id: item.id,
    personId: '',
    personName: '-',
    orgId: item.organizationId,
    orgName: item.organizationId,
    positionId: item.id,
    positionName: item.name,
    primary: false,
    effectiveFromUtc: item.createdAt,
  }
}

function filterAssignments(
  items: PositionAssignment[],
  query: ListQuery,
): PositionAssignment[] {
  const keyword = query.keyword?.trim().toLowerCase()

  if (!keyword) {
    return items
  }

  return items.filter((item) =>
    [item.personName, item.orgName, item.positionName]
      .filter(Boolean)
      .some((value) => value?.toLowerCase().includes(keyword)),
  )
}

export async function listPositionAssignments(
  query: ListQuery = {},
): Promise<PageData<PositionAssignment>> {
  const tenantId = await resolveCurrentTenantId()
  const params = new URLSearchParams()
  params.set('tenantId', tenantId)
  const items = await get<BackendPositionResponse[]>(POSITION_URL, { params })
  const mappedItems = filterAssignments(
    items.map(mapPositionToAssignment),
    query,
  )

  return toPageData(mappedItems, query)
}

interface BackendAssignmentResponse {
  id: string
  personId: string
  positionId: string
  type: 'PRIMARY' | 'SECONDARY' | 'PART_TIME'
  startDate?: string
  endDate?: string
}

export async function getPositionAssignments(
  positionId: string,
): Promise<PositionAssignment[]> {
  const tenantId = await resolveCurrentTenantId()
  const params = new URLSearchParams()
  params.set('tenantId', tenantId)
  const items = await get<BackendAssignmentResponse[]>(
    `${POSITION_URL}/${positionId}/assignments`,
    { params },
  )

  return items.map((item) => ({
    id: item.id,
    personId: item.personId,
    personName: item.personId,
    orgId: '',
    orgName: '-',
    positionId: item.positionId,
    positionName: item.positionId,
    primary: item.type === 'PRIMARY',
    effectiveFromUtc: item.startDate,
    effectiveToUtc: item.endDate,
  }))
}

export function createPositionAssignment(
  payload: PositionAssignmentPayload,
  idempotencyKey?: string,
): Promise<PositionAssignment> {
  return post<PositionAssignment, PositionAssignmentPayload>(
    POSITION_URL,
    payload,
    {
      dedupeKey: `position:create:${payload.personId}:${payload.positionId}`,
      idempotencyKey,
    },
  )
}

export function updatePositionAssignment(
  id: string,
  payload: PositionAssignmentPayload,
  idempotencyKey?: string,
): Promise<PositionAssignment> {
  return put<PositionAssignment, PositionAssignmentPayload>(
    `${POSITION_URL}/${id}`,
    payload,
    {
      dedupeKey: `position:update:${id}`,
      idempotencyKey,
    },
  )
}
