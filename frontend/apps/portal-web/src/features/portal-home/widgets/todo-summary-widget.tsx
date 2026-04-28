import type { ReactElement } from 'react'
import { AlertTriangle, CheckCircle2 } from 'lucide-react'
import { Link } from 'react-router-dom'
import HomeWidgetCard from '@/features/portal-home/pages/home-widget-card'
import type { TodoSummary } from '@/features/portal-home/types/portal-home'

const COPY = {
  titleKey: 'portal.home.todo.title',
  titleText: '待办摘要',
  descriptionKey: 'portal.home.todo.description',
  descriptionText: '集中查看审批、流程和提醒事项',
  pendingKey: 'portal.home.todo.pending',
  pendingText: '待处理',
  overdueKey: 'portal.home.todo.overdue',
  overdueText: '已逾期',
  todayDueKey: 'portal.home.todo.todayDue',
  todayDueText: '今日到期',
  actionKey: 'portal.home.todo.action',
  actionText: '进入待办中心',
} as const

export interface TodoSummaryWidgetProps {
  summary: TodoSummary
}

export default function TodoSummaryWidget({
  summary,
}: TodoSummaryWidgetProps): ReactElement {
  return (
    <HomeWidgetCard
      description={COPY.descriptionText}
      icon={<CheckCircle2 className="h-5 w-5" />}
      title={COPY.titleText}
    >
      <div className="grid grid-cols-3 gap-3">
        <Metric
          label={COPY.pendingText}
          tone="slate"
          value={summary.pendingCount}
        />
        <Metric
          label={COPY.overdueText}
          tone="red"
          value={summary.overdueCount}
        />
        <Metric
          label={COPY.todayDueText}
          tone="amber"
          value={summary.todayDueCount ?? 0}
        />
      </div>
      <Link
        className="mt-5 inline-flex h-10 w-full items-center justify-center gap-2 rounded-xl bg-slate-900 px-4 py-2 text-sm font-medium text-white shadow-sm transition-colors hover:bg-slate-800"
        to={summary.entryHref}
      >
        <AlertTriangle className="h-4 w-4" />
        {COPY.actionText}
      </Link>
    </HomeWidgetCard>
  )
}

interface MetricProps {
  label: string
  value: number
  tone: 'slate' | 'red' | 'amber'
}

const toneClassMap: Record<MetricProps['tone'], string> = {
  slate: 'bg-slate-50 text-slate-950',
  red: 'bg-red-50 text-red-700',
  amber: 'bg-amber-50 text-amber-700',
}

function Metric({ label, tone, value }: MetricProps): ReactElement {
  return (
    <div className={`${toneClassMap[tone]} rounded-xl p-3`}>
      <div className="text-2xl font-semibold">{value}</div>
      <div className="mt-1 text-xs text-slate-500">{label}</div>
    </div>
  )
}
