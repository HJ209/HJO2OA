import type { ReactElement } from 'react'
import { CheckCheck, RefreshCw } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { cn } from '@/utils/cn'
import { useMessageAction } from '@/features/messages/hooks/use-message-action'
import { useMessagesQuery } from '@/features/messages/hooks/use-messages-query'
import { useMessageStore } from '@/features/messages/stores/message-store'
import { MessageItemRow } from '@/features/messages/pages/message-item-row'
import type {
  MessageNotificationSummary,
  MessageReadStatus,
} from '@/features/messages/types/message'

const COPY = {
  allText: '全部',
  unreadText: '未读',
  readText: '已读',
  markAllText: '全部已读',
  loadingText: '正在加载消息...',
  emptyText: '暂无符合条件的消息。',
  errorText: '消息列表加载失败，请稍后重试。',
} as const

const READ_STATUS_OPTIONS: Array<{
  label: string
  value: MessageReadStatus | 'ALL'
}> = [
  { label: COPY.allText, value: 'ALL' },
  { label: COPY.unreadText, value: 'UNREAD' },
  { label: COPY.readText, value: 'READ' },
]

export function MessageList(): ReactElement {
  const readStatus = useMessageStore((state) => state.readStatus)
  const selectedMessageId = useMessageStore((state) => state.selectedMessageId)
  const setReadStatus = useMessageStore((state) => state.setReadStatus)
  const setSelectedMessageId = useMessageStore(
    (state) => state.setSelectedMessageId,
  )
  const messagesQuery = useMessagesQuery()
  const { isMarkingAllRead, markAllRead, markRead } = useMessageAction()

  function handleSelect(message: MessageNotificationSummary): void {
    setSelectedMessageId(message.id)

    if (message.readStatus === 'UNREAD') {
      markRead(message.id)
    }
  }

  return (
    <section className="overflow-hidden rounded-2xl border border-slate-200 bg-white">
      <div className="flex flex-col gap-3 border-b border-slate-200 p-4 md:flex-row md:items-center md:justify-between">
        <div className="flex rounded-xl bg-slate-100 p-1">
          {READ_STATUS_OPTIONS.map((option) => (
            <button
              className={cn(
                'rounded-lg px-3 py-1.5 text-sm font-medium transition',
                readStatus === option.value
                  ? 'bg-white text-slate-950 shadow-sm'
                  : 'text-slate-500 hover:text-slate-900',
              )}
              key={option.value}
              onClick={() => setReadStatus(option.value)}
              type="button"
            >
              {option.label}
            </button>
          ))}
        </div>

        <Button
          disabled={isMarkingAllRead}
          onClick={() => {
            const unreadIds =
              messagesQuery.data?.items
                .filter((m) => m.readStatus === 'UNREAD')
                .map((m) => m.id) ?? []

            if (unreadIds.length > 0) {
              markAllRead(unreadIds)
            }
          }}
          size="sm"
          variant="outline"
        >
          {isMarkingAllRead ? (
            <RefreshCw className="h-4 w-4 animate-spin" />
          ) : (
            <CheckCheck className="h-4 w-4" />
          )}
          {COPY.markAllText}
        </Button>
      </div>

      {messagesQuery.isLoading ? (
        <div className="p-6 text-sm text-slate-500">{COPY.loadingText}</div>
      ) : null}

      {messagesQuery.isError ? (
        <div className="p-6 text-sm text-rose-600">{COPY.errorText}</div>
      ) : null}

      {messagesQuery.data?.items.length === 0 ? (
        <div className="p-6 text-sm text-slate-500">{COPY.emptyText}</div>
      ) : null}

      <div>
        {messagesQuery.data?.items.map((message) => (
          <MessageItemRow
            active={message.id === selectedMessageId}
            key={message.id}
            message={message}
            onSelect={handleSelect}
          />
        ))}
      </div>
    </section>
  )
}
