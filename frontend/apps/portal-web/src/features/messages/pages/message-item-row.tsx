import type { ReactElement } from 'react'
import { BellDot, CheckCircle2 } from 'lucide-react'
import { Badge } from '@/components/ui/badge'
import { cn } from '@/utils/cn'
import { formatUtcToUserTimezone } from '@/utils/format-time'
import type { MessageNotificationSummary } from '@/features/messages/types/message'

const COPY = {
  unreadText: '未读',
  readText: '已读',
  openText: '查看详情',
} as const

interface MessageItemRowProps {
  message: MessageNotificationSummary
  active?: boolean
  onSelect: (message: MessageNotificationSummary) => void
}

export function MessageItemRow({
  active = false,
  message,
  onSelect,
}: MessageItemRowProps): ReactElement {
  const unread = message.readStatus === 'UNREAD'

  return (
    <button
      className={cn(
        'flex w-full items-start gap-3 border-b border-slate-100 px-4 py-4 text-left transition hover:bg-slate-50',
        active ? 'bg-sky-50' : 'bg-white',
      )}
      onClick={() => onSelect(message)}
      type="button"
    >
      <span
        aria-hidden="true"
        className={cn(
          'mt-2 h-2.5 w-2.5 shrink-0 rounded-full',
          unread ? 'bg-sky-500' : 'bg-transparent',
        )}
      />

      <span className="min-w-0 flex-1 space-y-2">
        <span className="flex flex-wrap items-center gap-2">
          <span
            className={cn(
              'truncate text-sm text-slate-950',
              unread ? 'font-semibold' : 'font-medium',
            )}
          >
            {message.title}
          </span>
          <Badge variant={unread ? 'default' : 'secondary'}>
            {unread ? COPY.unreadText : COPY.readText}
          </Badge>
          <Badge variant="secondary">{message.type}</Badge>
        </span>
        <span className="line-clamp-2 block text-sm text-slate-500">
          {message.summary}
        </span>
        <span className="flex items-center gap-2 text-xs text-slate-400">
          {unread ? (
            <BellDot className="h-3.5 w-3.5 text-sky-500" />
          ) : (
            <CheckCircle2 className="h-3.5 w-3.5" />
          )}
          {formatUtcToUserTimezone(message.createdAt)}
        </span>
      </span>

      <span className="inline-flex h-9 shrink-0 items-center justify-center rounded-xl px-3 py-2 text-sm font-medium text-slate-700">
        {COPY.openText}
      </span>
    </button>
  )
}
