import { get, post } from '@/services/request'
import { serializePaginationParams } from '@/utils/pagination'
import type {
  CopiedTodoReadStatus,
  CopiedTodoSummary,
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

export function getTodoCounts(): Promise<TodoCounts> {
  return get<TodoCounts>(`${TODO_API_PREFIX}/counts`)
}
