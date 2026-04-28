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
  positionName: string
  orgName: string
}

export interface IdentitySnapshot {
  currentAssignment: IdentityAssignment | null
  orgId: string | null
  roleIds: string[]
}

export interface TodoItemSummary {
  id: string
  title: string
  status: 'PENDING' | 'IN_PROGRESS' | 'COMPLETED'
  priority: 'LOW' | 'MEDIUM' | 'HIGH'
  dueAtUtc?: string
  assigneeName: string
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
