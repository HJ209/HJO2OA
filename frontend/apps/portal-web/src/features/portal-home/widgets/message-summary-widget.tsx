import type { ReactElement } from 'react'
import { Bell } from 'lucide-react'
import HomeWidgetCard from '@/features/portal-home/pages/home-widget-card'
import type { MessageSummary } from '@/features/portal-home/types/portal-home'
import { formatUtcToUserTimezone } from '@/utils/format-time'

const COPY = {
  titleKey: 'portal.home.message.title',
  titleText: '消息摘要',
  unreadKey: 'portal.home.message.unread',
  unreadText: '未读消息',
  emptyKey: 'portal.home.message.empty',
  emptyText: '暂无消息',
  actionKey: 'portal.home.message.action',
  actionText: '进入消息中心',
} as const

export interface MessageSummaryWidgetProps {
  summary: MessageSummary
}

export default function MessageSummaryWidget({
  summary,
}: MessageSummaryWidgetProps): ReactElement {
  return (
    <HomeWidgetCard
      action={{ label: COPY.actionText, href: summary.entryHref }}
      icon={<Bell className="h-5 w-5" />}
      title={COPY.titleText}
    >
      <div className="mb-4 rounded-xl bg-sky-50 p-4 text-sky-800">
        <div className="text-3xl font-semibold">{summary.unreadCount}</div>
        <div className="mt-1 text-sm">{COPY.unreadText}</div>
      </div>
      {summary.latest.length > 0 ? (
        <ul className="space-y-3">
          {summary.latest.slice(0, 3).map((message) => (
            <li key={message.id}>
              <p className="line-clamp-1 text-sm font-medium text-slate-900">
                {message.title}
              </p>
              <p className="mt-1 text-xs text-slate-500">
                {message.senderName ?? '系统'} ·{' '}
                {formatUtcToUserTimezone(message.sentAtUtc)}
              </p>
            </li>
          ))}
        </ul>
      ) : (
        <p className="text-sm text-slate-500">{COPY.emptyText}</p>
      )}
    </HomeWidgetCard>
  )
}
