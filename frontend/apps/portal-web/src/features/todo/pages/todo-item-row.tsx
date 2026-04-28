import type { ReactElement } from 'react'
import { AlertCircle, CheckCircle2, Clock3 } from 'lucide-react'
import { Badge } from '@/components/ui/badge'
import { formatUtcToUserTimezone } from '@/utils/format-time'
import type {
  TodoItemStatus,
  TodoItemSummary,
  TodoUrgency,
} from '@/features/todo/types/todo'

const COPY = {
  categoryKey: 'todo.item.category',
  categoryText: '分类',
  dueTimeKey: 'todo.item.dueTime',
  dueTimeText: '截止',
  createdAtKey: 'todo.item.createdAt',
  createdAtText: '创建',
  completedAtKey: 'todo.item.completedAt',
  completedAtText: '完成',
} as const

const urgencyTextMap: Record<TodoUrgency, string> = {
  LOW: '低',
  MEDIUM: '中',
  HIGH: '高',
  URGENT: '紧急',
}

const statusTextMap: Record<TodoItemStatus, string> = {
  PENDING: '待办',
  IN_PROGRESS: '处理中',
  COMPLETED: '已办',
  OVERDUE: '逾期',
  CANCELLED: '已取消',
}

function resolveUrgencyVariant(
  urgency: TodoUrgency,
): 'default' | 'secondary' | 'success' {
  if (urgency === 'HIGH' || urgency === 'URGENT') {
    return 'default'
  }

  if (urgency === 'MEDIUM') {
    return 'secondary'
  }

  return 'success'
}

function StatusIcon({ status }: { status: TodoItemStatus }): ReactElement {
  if (status === 'COMPLETED') {
    return <CheckCircle2 className="h-5 w-5 text-emerald-500" />
  }

  if (status === 'OVERDUE') {
    return <AlertCircle className="h-5 w-5 text-rose-500" />
  }

  return <Clock3 className="h-5 w-5 text-amber-500" />
}

export interface TodoItemRowProps {
  item: TodoItemSummary
}

export function TodoItemRow({ item }: TodoItemRowProps): ReactElement {
  const primaryTime = item.completedAt ?? item.dueTime ?? item.createdAt
  const primaryTimeLabel = item.completedAt
    ? COPY.completedAtText
    : item.dueTime
      ? COPY.dueTimeText
      : COPY.createdAtText

  return (
    <article className="flex flex-col gap-4 border-b border-slate-100 px-5 py-4 last:border-b-0 lg:flex-row lg:items-center lg:justify-between">
      <div className="min-w-0 space-y-2">
        <div className="flex flex-wrap items-center gap-2">
          <h3 className="truncate text-base font-semibold text-slate-950">
            {item.title}
          </h3>
          <Badge variant={resolveUrgencyVariant(item.urgency)}>
            {urgencyTextMap[item.urgency]}
          </Badge>
          <Badge variant="secondary">{item.type}</Badge>
        </div>
        <div className="flex flex-wrap gap-x-4 gap-y-1 text-sm text-slate-500">
          <span>
            {COPY.categoryText}：{item.category}
          </span>
          <span>
            {primaryTimeLabel}：{formatUtcToUserTimezone(primaryTime)}
          </span>
        </div>
      </div>
      <div className="flex shrink-0 items-center gap-2 text-sm font-medium text-slate-600">
        <StatusIcon status={item.status} />
        <span>{statusTextMap[item.status]}</span>
      </div>
    </article>
  )
}
