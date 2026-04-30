export type TodoItemStatus = 'PENDING' | 'COMPLETED' | 'CANCELLED'

export type CopiedTodoReadStatus = 'READ' | 'UNREAD'

export type TodoUrgency = 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT'

export interface TodoItemSummary {
  todoId: string
  taskId: string
  instanceId: string
  tenantId?: string
  title: string
  category: string
  urgency: string
  status: TodoItemStatus
  assigneeId: string
  dueTime?: string
  overdueAt?: string
  createdAt: string
  updatedAt?: string
  completedAt?: string
}

export interface CopiedTodoSummary {
  todoId: string
  taskId: string
  instanceId: string
  tenantId?: string
  type: string
  category: string
  title: string
  urgency: TodoUrgency
  readStatus: CopiedTodoReadStatus
  createdAt: string
  updatedAt: string
  readAt?: string
}

export interface TodoCounts {
  pendingCount: number
  completedCount: number
  overdueCount: number
  initiatedCount: number
  copiedUnreadCount: number
  draftCount: number
  archivedCount: number
}

export interface InitiatedProcessSummary {
  instanceId: string
  definitionId: string
  definitionCode: string
  title: string
  category?: string
  status: string
  startTime: string
  endTime?: string
  updatedAt?: string
}

export interface DraftProcessSummary {
  submissionId: string
  metadataId: string
  metadataCode: string
  metadataVersion: number
  processInstanceId?: string
  nodeId?: string
  createdAt: string
  updatedAt: string
}

export interface ArchiveProcessSummary {
  instanceId: string
  definitionId: string
  definitionCode: string
  title: string
  category?: string
  status: string
  startTime: string
  endTime?: string
  updatedAt?: string
}

export interface TodoBatchActionResult {
  requestedCount: number
  successCount: number
  skippedCount: number
  succeededTodoIds: string[]
  skippedTodoIds: string[]
}

export type TodoItemTab = 'pending' | 'completed' | 'overdue'

export type TodoTab =
  | TodoItemTab
  | 'copied'
  | 'initiated'
  | 'drafts'
  | 'archives'

export type TodoSortField = 'createdAt' | 'dueTime' | 'urgency'

export interface TodoSortOption {
  field: TodoSortField
  direction: 'asc' | 'desc'
}
