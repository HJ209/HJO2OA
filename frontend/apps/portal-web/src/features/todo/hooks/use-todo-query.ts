import { useQuery, type UseQueryResult } from '@tanstack/react-query'
import {
  getCompletedTodos,
  getOverdueTodos,
  getPendingTodos,
} from '@/features/todo/services/todo-service'
import type {
  TodoItemSummary,
  TodoSortOption,
  TodoTab,
} from '@/features/todo/types/todo'

const TODO_QUERY_KEY = 'todo-items'

function resolveQuery(tab: Exclude<TodoTab, 'copied'>) {
  if (tab === 'completed') {
    return getCompletedTodos
  }

  if (tab === 'overdue') {
    return getOverdueTodos
  }

  return getPendingTodos
}

function urgencyRank(urgency: TodoItemSummary['urgency']): number {
  const ranks: Record<TodoItemSummary['urgency'], number> = {
    LOW: 1,
    MEDIUM: 2,
    HIGH: 3,
    URGENT: 4,
  }

  return ranks[urgency]
}

function readComparableValue(
  item: TodoItemSummary,
  field: TodoSortOption['field'],
): number {
  if (field === 'urgency') {
    return urgencyRank(item.urgency)
  }

  return new Date(item[field] ?? item.createdAt).getTime()
}

function sortTodos(
  items: TodoItemSummary[],
  sort: TodoSortOption,
): TodoItemSummary[] {
  const direction = sort.direction === 'asc' ? 1 : -1

  return [...items].sort(
    (left, right) =>
      (readComparableValue(left, sort.field) -
        readComparableValue(right, sort.field)) *
      direction,
  )
}

export function useTodoQuery(
  tab: Exclude<TodoTab, 'copied'>,
  sort: TodoSortOption,
): UseQueryResult<TodoItemSummary[]> {
  return useQuery({
    queryKey: [TODO_QUERY_KEY, tab, sort],
    queryFn: resolveQuery(tab),
    select: (items) => sortTodos(items, sort),
  })
}
