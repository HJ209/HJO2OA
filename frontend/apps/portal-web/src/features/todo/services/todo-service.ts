import { get, post } from '@/services/request'
import { serializePaginationParams } from '@/utils/pagination'
import type {
  ArchiveProcessSummary,
  CopiedTodoReadStatus,
  CopiedTodoSummary,
  DraftProcessSummary,
  InitiatedProcessSummary,
  TodoBatchActionResult,
  TodoCounts,
  TodoItemSummary,
} from '@/features/todo/types/todo'

const TODO_API_PREFIX = '/v1/todo'

export function getPendingTodos(): Promise<TodoItemSummary[]> {
  return get<TodoItemSummary[]>(`${TODO_API_PREFIX}/pending`)
}

export function getCompletedTodos(): Promise<TodoItemSummary[]> {
  return get<TodoItemSummary[]>(`${TODO_API_PREFIX}/completed`)
}

export function getOverdueTodos(): Promise<TodoItemSummary[]> {
  return get<TodoItemSummary[]>(`${TODO_API_PREFIX}/overdue`)
}

export function getCopiedTodos(
  readStatus?: CopiedTodoReadStatus,
): Promise<CopiedTodoSummary[]> {
  const params = readStatus
    ? serializePaginationParams({
        filters: [{ field: 'readStatus', value: readStatus }],
      })
    : undefined

  return get<CopiedTodoSummary[]>(`${TODO_API_PREFIX}/copied`, { params })
}

export function markCopiedTodoRead(todoId: string): Promise<CopiedTodoSummary> {
  return post<CopiedTodoSummary, Record<string, never>>(
    `${TODO_API_PREFIX}/copied/${todoId}/read`,
    {},
    {
      dedupeKey: `todo:copied:read:${todoId}`,
      idempotencyKey: `todo-copied-read-${todoId}`,
    },
  )
}

export function batchMarkCopiedTodosRead(
  todoIds: string[],
): Promise<TodoBatchActionResult> {
  return post<TodoBatchActionResult, { todoIds: string[] }>(
    `${TODO_API_PREFIX}/copied/batch-read`,
    { todoIds },
    {
      dedupeKey: `todo:copied:batch-read:${todoIds.join(',')}`,
      idempotencyKey: idempotencyKey('todo-copied-batch-read'),
    },
  )
}

export function completeTodo(todoId: string): Promise<TodoItemSummary> {
  return post<TodoItemSummary, Record<string, never>>(
    `${TODO_API_PREFIX}/${todoId}/complete`,
    {},
    {
      dedupeKey: `todo:complete:${todoId}`,
      idempotencyKey: idempotencyKey('todo-complete'),
    },
  )
}

export function batchCompleteTodos(
  todoIds: string[],
): Promise<TodoBatchActionResult> {
  return post<TodoBatchActionResult, { todoIds: string[] }>(
    `${TODO_API_PREFIX}/batch/complete`,
    { todoIds },
    {
      dedupeKey: `todo:batch-complete:${todoIds.join(',')}`,
      idempotencyKey: idempotencyKey('todo-batch-complete'),
    },
  )
}

export function remindTodo(
  todoId: string,
  reason?: string,
): Promise<TodoItemSummary> {
  return post<TodoItemSummary, { reason?: string }>(
    `${TODO_API_PREFIX}/${todoId}/remind`,
    reason ? { reason } : {},
    {
      dedupeKey: `todo:remind:${todoId}:${reason ?? ''}`,
      idempotencyKey: idempotencyKey('todo-remind'),
    },
  )
}

export function batchRemindTodos(
  todoIds: string[],
  reason?: string,
): Promise<TodoBatchActionResult> {
  return post<TodoBatchActionResult, { todoIds: string[]; reason?: string }>(
    `${TODO_API_PREFIX}/batch/remind`,
    reason ? { todoIds, reason } : { todoIds },
    {
      dedupeKey: `todo:batch-remind:${todoIds.join(',')}:${reason ?? ''}`,
      idempotencyKey: idempotencyKey('todo-batch-remind'),
    },
  )
}

export function getInitiatedProcesses(): Promise<InitiatedProcessSummary[]> {
  return get<InitiatedProcessSummary[]>(`${TODO_API_PREFIX}/initiated`)
}

export function getDraftProcesses(): Promise<DraftProcessSummary[]> {
  return get<DraftProcessSummary[]>(`${TODO_API_PREFIX}/drafts`)
}

export function getArchivedProcesses(): Promise<ArchiveProcessSummary[]> {
  return get<ArchiveProcessSummary[]>(`${TODO_API_PREFIX}/archives`)
}

export function getTodoCounts(): Promise<TodoCounts> {
  return get<TodoCounts>(`${TODO_API_PREFIX}/counts`)
}

function idempotencyKey(scope: string): string {
  if (typeof crypto !== 'undefined' && crypto.randomUUID) {
    return `${scope}-${crypto.randomUUID()}`
  }

  return `${scope}-${Date.now()}-${Math.random().toString(16).slice(2)}`
}
