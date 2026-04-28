import {
  useMutation,
  useQueryClient,
  type UseMutationResult,
} from '@tanstack/react-query'
import {
  getCopiedTodos,
  markCopiedTodoRead,
} from '@/features/todo/services/todo-service'
import { TODO_COUNTS_QUERY_KEY } from '@/features/todo/hooks/use-todo-counts'
import type {
  CopiedTodoReadStatus,
  CopiedTodoSummary,
} from '@/features/todo/types/todo'

export const COPIED_TODOS_QUERY_KEY = 'copied-todos'

export function copiedTodosQueryOptions(readStatus?: CopiedTodoReadStatus) {
  return {
    queryKey: [COPIED_TODOS_QUERY_KEY, readStatus] as const,
    queryFn: () => getCopiedTodos(readStatus),
  }
}

export function useCopiedTodoAction(): UseMutationResult<
  CopiedTodoSummary,
  Error,
  string
> {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: markCopiedTodoRead,
    onSuccess: () => {
      void queryClient.invalidateQueries({
        queryKey: [COPIED_TODOS_QUERY_KEY],
      })
      void queryClient.invalidateQueries({
        queryKey: TODO_COUNTS_QUERY_KEY,
      })
    },
  })
}
