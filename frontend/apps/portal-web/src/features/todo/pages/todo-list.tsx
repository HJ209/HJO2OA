import type { ReactElement } from 'react'
import { AlertCircle } from 'lucide-react'
import { Skeleton } from '@/components/ui/skeleton'
import { useTodoQuery } from '@/features/todo/hooks/use-todo-query'
import { useTodoStore } from '@/features/todo/stores/todo-store'
import type { TodoTab } from '@/features/todo/types/todo'
import { TodoItemRow } from '@/features/todo/pages/todo-item-row'

const COPY = {
  emptyKey: 'todo.list.empty',
  emptyText: '暂无待办数据',
  errorKey: 'todo.list.error',
  errorText: '待办列表加载失败',
} as const

export interface TodoListProps {
  tab: Exclude<TodoTab, 'copied'>
}

export function TodoList({ tab }: TodoListProps): ReactElement {
  const sort = useTodoStore((state) => state.sort)
  const { data, isError, isLoading } = useTodoQuery(tab, sort)

  if (isLoading) {
    return (
      <div className="space-y-3 rounded-lg border border-slate-200 bg-white p-5">
        <Skeleton className="h-16 w-full" />
        <Skeleton className="h-16 w-full" />
        <Skeleton className="h-16 w-full" />
      </div>
    )
  }

  if (isError) {
    return (
      <div className="flex items-center gap-2 rounded-lg border border-rose-200 bg-rose-50 p-5 text-sm text-rose-700">
        <AlertCircle className="h-5 w-5" />
        <span>{COPY.errorText}</span>
      </div>
    )
  }

  if (!data || data.length === 0) {
    return (
      <div className="rounded-lg border border-dashed border-slate-300 bg-white p-8 text-center text-sm text-slate-500">
        {COPY.emptyText}
      </div>
    )
  }

  return (
    <div className="overflow-hidden rounded-lg border border-slate-200 bg-white shadow-sm">
      {data.map((item) => (
        <TodoItemRow item={item} key={item.todoId} />
      ))}
    </div>
  )
}
