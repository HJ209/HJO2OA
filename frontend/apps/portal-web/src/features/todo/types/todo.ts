export type TodoItemStatus =
  | 'PENDING'
  | 'IN_PROGRESS'
  | 'COMPLETED'
  | 'OVERDUE'
  | 'CANCELLED'

export type CopiedTodoReadStatus = 'READ' | 'UNREAD'

export type TodoUrgency = 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT'

export interface TodoItemSummary {
  todoId: string
  taskId: string
  instanceId: string
  assigneeId: string
  type: string
  category: string
  title: string
  urgency: TodoUrgency
  status: TodoItemStatus
  dueTime?: string
  overdueAt?: string
  createdAt: string
  completedAt?: string
}

export interface CopiedTodoSummary {
  todoId: string
  taskId: string
  instanceId: string
  recipientAssignmentId: string
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
  cancelledCount: number
  copiedUnreadCount: number
  copiedTotalCount: number
  total: number
}

export type TodoTab = 'pending' | 'completed' | 'overdue' | 'copied'

export type TodoSortField = 'createdAt' | 'dueTime' | 'urgency'

export interface TodoSortOption {
  field: TodoSortField
  direction: 'asc' | 'desc'
}
