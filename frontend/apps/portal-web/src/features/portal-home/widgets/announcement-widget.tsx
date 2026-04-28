import type { ReactElement } from 'react'
import { Megaphone } from 'lucide-react'
import HomeWidgetCard from '@/features/portal-home/pages/home-widget-card'
import type { AnnouncementSummary } from '@/features/portal-home/types/portal-home'
import { formatUtcToUserTimezone } from '@/utils/format-time'

const COPY = {
  titleKey: 'portal.home.announcement.title',
  titleText: '公告摘要',
  emptyKey: 'portal.home.announcement.empty',
  emptyText: '暂无公告',
  actionKey: 'portal.home.announcement.action',
  actionText: '查看公告',
} as const

export interface AnnouncementWidgetProps {
  summary: AnnouncementSummary
}

export default function AnnouncementWidget({
  summary,
}: AnnouncementWidgetProps): ReactElement {
  const latestAnnouncements = summary.latest.slice(0, 3)

  return (
    <HomeWidgetCard
      action={{ label: COPY.actionText, href: summary.entryHref }}
      icon={<Megaphone className="h-5 w-5" />}
      title={COPY.titleText}
    >
      {latestAnnouncements.length > 0 ? (
        <ul className="divide-y divide-slate-100">
          {latestAnnouncements.map((announcement) => (
            <li className="py-3 first:pt-0 last:pb-0" key={announcement.id}>
              <p className="line-clamp-1 text-sm font-medium text-slate-900">
                {announcement.title}
              </p>
              <p className="mt-1 text-xs text-slate-500">
                {announcement.publisherName ?? '系统'} ·{' '}
                {formatUtcToUserTimezone(announcement.publishedAtUtc)}
              </p>
            </li>
          ))}
        </ul>
      ) : (
        <p className="rounded-xl bg-slate-50 p-4 text-sm text-slate-500">
          {COPY.emptyText}
        </p>
      )}
    </HomeWidgetCard>
  )
}
