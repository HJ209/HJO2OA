import { get, post } from '@/services/request'
import type { IdentityAssignment, IdentitySnapshot } from '@/types/domain'

export interface IdentityContext extends IdentitySnapshot {
  assignments: IdentityAssignment[]
  pendingTodoCount: number
  unreadMessageCount: number
}

export interface SwitchAssignmentRequest {
  targetPositionId: string
  reason?: string
}

interface BackendIdentityContextView {
  tenantId: string
  personId: string
  accountId: string
  currentAssignmentId: string
  currentPositionId: string
  currentOrganizationId: string
  currentDepartmentId: string
  currentPositionName: string
  currentOrganizationName: string
  currentDepartmentName: string
  assignmentType: 'PRIMARY' | 'SECONDARY'
  roleIds: string[]
  permissionSnapshotVersion: number
  effectiveAt: string
}

interface BackendAvailableIdentityOption {
  assignmentId: string
  positionId: string
  organizationId: string
  departmentId: string
  positionName: string
  organizationName: string
  departmentName: string
  assignmentType: 'PRIMARY' | 'SECONDARY'
  current: boolean
  switchable: boolean
  unavailableReason: string | null
}

function toIdentityAssignment(
  view: BackendIdentityContextView,
): IdentityAssignment {
  return {
    assignmentId: view.currentAssignmentId,
    positionId: view.currentPositionId,
    orgId: view.currentOrganizationId,
    positionName: view.currentPositionName,
    orgName: view.currentOrganizationName,
  }
}

function toAvailableAssignment(
  option: BackendAvailableIdentityOption,
): IdentityAssignment {
  return {
    assignmentId: option.assignmentId,
    positionId: option.positionId,
    orgId: option.organizationId,
    departmentId: option.departmentId,
    positionName: option.positionName,
    orgName: option.organizationName,
    departmentName: option.departmentName,
  }
}

function toIdentityContext(
  view: BackendIdentityContextView,
  assignments?: IdentityAssignment[],
): IdentityContext {
  const currentAssignment = toIdentityAssignment(view)

  return {
    currentAssignment,
    orgId: view.currentOrganizationId,
    roleIds: view.roleIds,
    assignments: assignments ?? [currentAssignment],
    pendingTodoCount: 0,
    unreadMessageCount: 0,
  }
}

export async function getCurrentContext(): Promise<IdentityContext> {
  const [view, options] = await Promise.all([
    get<BackendIdentityContextView>('/v1/org/identity-context/current'),
    get<BackendAvailableIdentityOption[]>('/v1/org/identity-context/available'),
  ])

  const assignments = options.map(toAvailableAssignment)

  return toIdentityContext(view, assignments)
}

export async function switchAssignment(
  targetPositionId: string,
  reason?: string,
): Promise<IdentityContext> {
  const view = await post<BackendIdentityContextView, SwitchAssignmentRequest>(
    '/v1/org/identity-context/switch',
    { targetPositionId, reason },
    {
      dedupeKey: `identity-switch:${targetPositionId}`,
    },
  )

  return toIdentityContext(view)
}
