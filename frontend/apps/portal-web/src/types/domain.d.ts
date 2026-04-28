export interface AuthenticatedUser {
  id: string
  accountName: string
  displayName: string
  tenantId: string
  locale: string
}

export interface IdentityAssignment {
  assignmentId: string
  positionId: string
  orgId: string
  departmentId?: string
  positionName: string
  orgName: string
  departmentName?: string
}

export interface IdentitySnapshot {
  currentAssignment: IdentityAssignment | null
  orgId: string | null
  roleIds: string[]
}

export interface TodoItemSummary {
  id: string
  title: string
  status: 'PENDING' | 'COMPLETED' | 'CANCELLED'
  urgency: string
  dueAtUtc?: string
  assigneeId: string
}

export interface MessageNotification {
  id: string
  title: string
  summary: string
  category: 'SYSTEM' | 'APPROVAL' | 'NOTICE'
  createdAtUtc: string
  read: boolean
}

export interface PortalSnapshot {
  pendingTodoCount: number
  unreadMessageCount: number
  activeAssignments: number
  latestSyncAtUtc: string
}
