import { useQuery, type UseQueryResult } from '@tanstack/react-query'
import { getTodoCounts } from '@/features/todo/services/todo-service'
import type { TodoCounts } from '@/features/todo/types/todo'

export const TODO_COUNTS_QUERY_KEY = ['todo-counts'] as const

export function useTodoCounts(): UseQueryResult<TodoCounts> {
  return useQuery({
    queryKey: TODO_COUNTS_QUERY_KEY,
    queryFn: getTodoCounts,
  })
}
