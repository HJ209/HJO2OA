import type { ReactElement } from 'react'
import { ClipboardCheck, Clock3, CopyCheck, TimerOff } from 'lucide-react'
import { Skeleton } from '@/components/ui/skeleton'
import type { TodoCounts } from '@/features/todo/types/todo'

const COPY = {
  pendingKey: 'todo.counts.pending',
  pendingText: '待办数',
  completedKey: 'todo.counts.completed',
  completedText: '已办数',
  overdueKey: 'todo.counts.overdue',
  overdueText: '逾期数',
  copiedUnreadKey: 'todo.counts.copiedUnread',
  copiedUnreadText: '抄送未读数',
} as const

interface CountItem {
  key: string
  label: string
  value: number
  icon: typeof Clock3
}

export interface TodoCountsBarProps {
  counts?: TodoCounts
  isLoading?: boolean
}

export function TodoCountsBar({
  counts,
  isLoading = false,
}: TodoCountsBarProps): ReactElement {
  if (isLoading) {
    return (
      <div className="grid gap-3 md:grid-cols-4">
        {[
          COPY.pendingKey,
          COPY.completedKey,
          COPY.overdueKey,
          COPY.copiedUnreadKey,
        ].map((key) => (
          <Skeleton className="h-20 rounded-lg" key={key} />
        ))}
      </div>
    )
  }

  const safeCounts = counts ?? {
    pendingCount: 0,
    completedCount: 0,
    overdueCount: 0,
    cancelledCount: 0,
    copiedUnreadCount: 0,
    copiedTotalCount: 0,
    total: 0,
  }

  const items: CountItem[] = [
    {
      key: COPY.pendingKey,
      label: COPY.pendingText,
      value: safeCounts.pendingCount,
      icon: ClipboardCheck,
    },
    {
      key: COPY.completedKey,
      label: COPY.completedText,
      value: safeCounts.completedCount,
      icon: CopyCheck,
    },
    {
      key: COPY.overdueKey,
      label: COPY.overdueText,
      value: safeCounts.overdueCount,
      icon: TimerOff,
    },
    {
      key: COPY.copiedUnreadKey,
      label: COPY.copiedUnreadText,
      value: safeCounts.copiedUnreadCount,
      icon: Clock3,
    },
  ]

  return (
    <div className="grid gap-3 md:grid-cols-4">
      {items.map((item) => {
        const Icon = item.icon

        return (
          <section
            className="rounded-lg border border-slate-200 bg-white p-4 shadow-sm"
            key={item.key}
          >
            <div className="flex items-center justify-between gap-3">
              <span className="text-sm text-slate-500">{item.label}</span>
              <Icon className="h-5 w-5 text-sky-600" />
            </div>
            <p className="mt-2 text-2xl font-semibold text-slate-950">
              {item.value}
            </p>
          </section>
        )
      })}
    </div>
  )
}
