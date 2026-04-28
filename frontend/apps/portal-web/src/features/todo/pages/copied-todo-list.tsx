import type { ReactElement } from 'react'
import { useQuery } from '@tanstack/react-query'
import { CheckCheck, MailOpen } from 'lucide-react'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Skeleton } from '@/components/ui/skeleton'
import {
  copiedTodosQueryOptions,
  useCopiedTodoAction,
} from '@/features/todo/hooks/use-copied-todo-action'
import { formatUtcToUserTimezone } from '@/utils/format-time'

const COPY = {
  unreadText: '未读',
  readText: '已读',
  markReadText: '标记已读',
  emptyText: '暂无抄送数据',
  errorText: '抄送列表加载失败',
  createdAtText: '抄送',
  readAtText: '阅读',
} as const

export function CopiedTodoList(): ReactElement {
  const { data, isError, isLoading } = useQuery(copiedTodosQueryOptions())
  const markReadMutation = useCopiedTodoAction()

  if (isLoading) {
    return (
      <div className="space-y-3 rounded-lg border border-slate-200 bg-white p-5">
        <Skeleton className="h-16 w-full" />
        <Skeleton className="h-16 w-full" />
      </div>
    )
  }

  if (isError) {
    return (
      <div className="rounded-lg border border-rose-200 bg-rose-50 p-5 text-sm text-rose-700">
        {COPY.errorText}
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
      {data.map((item) => {
        const isUnread = item.readStatus === 'UNREAD'
        const isSubmitting =
          markReadMutation.isPending &&
          markReadMutation.variables === item.todoId

        return (
          <article
            className="flex flex-col gap-4 border-b border-slate-100 px-5 py-4 last:border-b-0 lg:flex-row lg:items-center lg:justify-between"
            key={item.todoId}
          >
            <div className="min-w-0 space-y-2">
              <div className="flex flex-wrap items-center gap-2">
                <h3 className="truncate text-base font-semibold text-slate-950">
                  {item.title}
                </h3>
                <Badge variant={isUnread ? 'default' : 'secondary'}>
                  {isUnread ? COPY.unreadText : COPY.readText}
                </Badge>
                <Badge variant="secondary">{item.category}</Badge>
              </div>
              <div className="flex flex-wrap gap-x-4 gap-y-1 text-sm text-slate-500">
                <span>
                  {COPY.createdAtText}：
                  {formatUtcToUserTimezone(item.createdAt)}
                </span>
                <span>
                  {COPY.readAtText}：{formatUtcToUserTimezone(item.readAt)}
                </span>
              </div>
            </div>
            <Button
              className="w-full lg:w-auto"
              disabled={!isUnread || isSubmitting}
              onClick={() => markReadMutation.mutate(item.todoId)}
              size="sm"
              variant={isUnread ? 'default' : 'outline'}
            >
              {isUnread ? (
                <MailOpen className="h-4 w-4" />
              ) : (
                <CheckCheck className="h-4 w-4" />
              )}
              {COPY.markReadText}
            </Button>
          </article>
        )
      })}
    </div>
  )
}
