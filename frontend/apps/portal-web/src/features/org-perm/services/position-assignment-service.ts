import { del, get, post, put } from '@/services/request'
import type { PageData } from '@/types/api'
import {
  resolveCurrentTenantId,
  toPageData,
} from '@/features/org-perm/services/service-utils'
import type {
  Assignment,
  AssignmentPayload,
  AssignmentType,
  ListQuery,
  OrgPosition,
  PositionAssignment,
  PositionAssignmentPayload,
  PositionPayload,
  PositionRoleBinding,
} from '@/features/org-perm/types/org-perm'

const BASE_URL = '/v1/org/position-assignments'
const POSITION_URL = `${BASE_URL}/positions`
const ASSIGNMENT_URL = `${BASE_URL}/assignments`

interface BackendExportResponse {
  positions: OrgPosition[]
  assignments: Assignment[]
  positionRoles: PositionRoleBinding[]
}

function tenantParams(tenantId: string): URLSearchParams {
  const params = new URLSearchParams()
  params.set('tenantId', tenantId)
  return params
}

function filterByKeyword<T>(
  items: T[],
  query: ListQuery,
  getFields: (item: T) => Array<string | undefined | null>,
): T[] {
  const keyword = query.keyword?.trim().toLowerCase()

  if (!keyword) {
    return items
  }

  return items.filter((item) =>
    getFields(item)
      .filter(Boolean)
      .some((value) => value?.toLowerCase().includes(keyword)),
  )
}

function toPositionAssignment(
  assignment: Assignment,
  position?: OrgPosition,
): PositionAssignment {
  return {
    id: assignment.id,
    personId: assignment.personId,
    personName: assignment.personId,
    orgId: position?.organizationId ?? '',
    orgName: position?.organizationId ?? '',
    positionId: assignment.positionId,
    positionName: position?.name ?? assignment.positionId,
    primary: assignment.type === 'PRIMARY',
    status: assignment.status,
    effectiveFromUtc: assignment.startDate ?? undefined,
    effectiveToUtc: assignment.endDate ?? undefined,
  }
}

export async function listPositions(
  query: ListQuery = {},
  scope: { organizationId?: string; departmentId?: string } = {},
): Promise<PageData<OrgPosition>> {
  const tenantId = await resolveCurrentTenantId()
  const params = tenantParams(tenantId)
  if (scope.organizationId) {
    params.set('organizationId', scope.organizationId)
  }
  if (scope.departmentId) {
    params.set('departmentId', scope.departmentId)
  }
  const items = await get<OrgPosition[]>(POSITION_URL, { params })
  const filtered = filterByKeyword(items, query, (item) => [
    item.name,
    item.code,
    item.organizationId,
    item.departmentId,
  ])

  return toPageData(filtered, query)
}

export async function createPosition(
  payload: PositionPayload,
  idempotencyKey?: string,
): Promise<OrgPosition> {
  const tenantId = await resolveCurrentTenantId()

  return post<OrgPosition, PositionPayload & { tenantId: string }>(
    POSITION_URL,
    { ...payload, tenantId },
    {
      dedupeKey: `position:create:${payload.code}`,
      idempotencyKey,
    },
  )
}

export function updatePosition(
  id: string,
  payload: PositionPayload,
  idempotencyKey?: string,
): Promise<OrgPosition> {
  return put<OrgPosition, PositionPayload>(`${POSITION_URL}/${id}`, payload, {
    dedupeKey: `position:update:${id}`,
    idempotencyKey,
  })
}

export function setPositionEnabled(
  id: string,
  enabled: boolean,
): Promise<OrgPosition> {
  return put<OrgPosition, Record<string, never>>(
    `${POSITION_URL}/${id}/${enabled ? 'activate' : 'disable'}`,
    {},
    { dedupeKey: `position:status:${id}:${enabled}` },
  )
}

export async function listPositionAssignments(
  query: ListQuery = {},
): Promise<PageData<PositionAssignment>> {
  const exported = await exportPositionAssignments()
  const positionMap = new Map(
    exported.positions.map((position) => [position.id, position]),
  )
  const items = exported.assignments.map((assignment) =>
    toPositionAssignment(assignment, positionMap.get(assignment.positionId)),
  )
  const filtered = filterByKeyword(items, query, (item) => [
    item.personId,
    item.positionName,
    item.orgId,
    item.status,
  ])

  return toPageData(filtered, query)
}

export async function listAssignmentsByPosition(
  positionId: string,
): Promise<Assignment[]> {
  const tenantId = await resolveCurrentTenantId()
  return get<Assignment[]>(`${POSITION_URL}/${positionId}/assignments`, {
    params: tenantParams(tenantId),
  })
}

export async function listAssignmentsByPerson(
  personId: string,
): Promise<Assignment[]> {
  const tenantId = await resolveCurrentTenantId()
  return get<Assignment[]>(`${BASE_URL}/persons/${personId}/assignments`, {
    params: tenantParams(tenantId),
  })
}

export async function createAssignment(
  payload: AssignmentPayload,
  idempotencyKey?: string,
): Promise<Assignment> {
  const tenantId = await resolveCurrentTenantId()

  return post<Assignment, AssignmentPayload & { tenantId: string }>(
    ASSIGNMENT_URL,
    { ...payload, tenantId },
    {
      dedupeKey: `assignment:create:${payload.personId}:${payload.positionId}`,
      idempotencyKey,
    },
  )
}

export async function createPositionAssignment(
  payload: PositionAssignmentPayload,
  idempotencyKey?: string,
): Promise<PositionAssignment> {
  const assignment = await createAssignment(
    {
      personId: payload.personId,
      positionId: payload.positionId,
      type: payload.primary ? 'PRIMARY' : 'SECONDARY',
      startDate: payload.effectiveFromUtc,
      endDate: payload.effectiveToUtc,
    },
    idempotencyKey,
  )

  return toPositionAssignment(assignment)
}

export async function updatePositionAssignment(
  id: string,
  payload: PositionAssignmentPayload,
  idempotencyKey?: string,
): Promise<PositionAssignment> {
  const assignment = await put<Assignment, { type: AssignmentType }>(
    `${ASSIGNMENT_URL}/${id}/type`,
    { type: payload.primary ? 'PRIMARY' : 'SECONDARY' },
    {
      dedupeKey: `assignment:type:${id}`,
      idempotencyKey,
    },
  )

  return toPositionAssignment(assignment)
}

export function changePrimaryAssignment(
  personId: string,
  assignmentId: string,
): Promise<Assignment> {
  return put<Assignment, Record<string, never>>(
    `${BASE_URL}/persons/${personId}/primary-assignment/${assignmentId}`,
    {},
    { dedupeKey: `assignment:primary:${personId}:${assignmentId}` },
  )
}

export function deactivateAssignment(
  assignmentId: string,
  endDate?: string | null,
): Promise<Assignment> {
  return put<Assignment, { endDate?: string | null }>(
    `${ASSIGNMENT_URL}/${assignmentId}/deactivate`,
    { endDate },
    { dedupeKey: `assignment:deactivate:${assignmentId}` },
  )
}

export async function listPositionRoles(
  positionId: string,
): Promise<PositionRoleBinding[]> {
  const tenantId = await resolveCurrentTenantId()
  return get<PositionRoleBinding[]>(`${POSITION_URL}/${positionId}/roles`, {
    params: tenantParams(tenantId),
  })
}

export async function addPositionRole(
  positionId: string,
  roleId: string,
): Promise<PositionRoleBinding> {
  const tenantId = await resolveCurrentTenantId()
  return post<PositionRoleBinding, { roleId: string; tenantId: string }>(
    `${POSITION_URL}/${positionId}/roles`,
    { roleId, tenantId },
    { dedupeKey: `position-role:add:${positionId}:${roleId}` },
  )
}

export async function removePositionRole(
  positionId: string,
  roleId: string,
): Promise<void> {
  const tenantId = await resolveCurrentTenantId()
  const params = tenantParams(tenantId)
  await del<void>(`${POSITION_URL}/${positionId}/roles/${roleId}`, {
    params,
    dedupeKey: `position-role:remove:${positionId}:${roleId}`,
  })
}

export async function exportPositionAssignments(): Promise<{
  positions: OrgPosition[]
  assignments: Assignment[]
  positionRoles: PositionRoleBinding[]
}> {
  return get<BackendExportResponse>(`${BASE_URL}/export`)
}
