import { get, post } from '@/services/request'
import type { IdentityAssignment, IdentitySnapshot } from '@/types/domain'

export interface IdentityContext extends IdentitySnapshot {
  assignments: IdentityAssignment[]
  pendingTodoCount: number
  unreadMessageCount: number
}

export interface SwitchAssignmentRequest {
  assignmentId: string
}

export async function getCurrentContext(): Promise<IdentityContext> {
  return get<IdentityContext>('/v1/identity/context')
}

export async function switchAssignment(
  assignmentId: string,
): Promise<IdentityContext> {
  return post<IdentityContext, SwitchAssignmentRequest>(
    '/v1/identity/assignments/switch',
    { assignmentId },
    {
      dedupeKey: `identity-switch:${assignmentId}`,
    },
  )
}
