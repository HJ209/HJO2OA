export type TodoItemStatus = 'PENDING' | 'COMPLETED' | 'CANCELLED'

export type CopiedTodoReadStatus = 'READ' | 'UNREAD'

export type TodoUrgency = 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT'

export interface TodoItemSummary {
  todoId: string
  taskId: string
  instanceId: string
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

export type TodoTab = 'pending' | 'completed' | 'overdue' | 'copied'

export type TodoSortField = 'createdAt' | 'dueTime' | 'urgency'

export interface TodoSortOption {
  field: TodoSortField
  direction: 'asc' | 'desc'
}
