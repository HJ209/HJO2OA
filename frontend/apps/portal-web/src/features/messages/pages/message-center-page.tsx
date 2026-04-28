import { useState, type ReactElement } from 'react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { Bell, Inbox } from 'lucide-react'
import { Badge } from '@/components/ui/badge'
import {
  Card,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card'
import { cn } from '@/utils/cn'
import { MessageDetail } from '@/features/messages/pages/message-detail'
import { MessageList } from '@/features/messages/pages/message-list'
import {
  useMessageStore,
  type MessageTypeFilter,
} from '@/features/messages/stores/message-store'

const COPY = {
  titleKey: 'message.center.title',
  titleText: '消息中心',
  descriptionKey: 'message.center.description',
  descriptionText: '统一查看系统通知、审批提醒和业务播报。',
  filterAllText: '全部消息',
  filterSystemText: '系统通知',
  filterApprovalText: '审批提醒',
  filterNoticeText: '业务播报',
  filterTaskText: '待办消息',
  filterAlertText: '风险预警',
} as const

const TYPE_OPTIONS: Array<{ label: string; value: MessageTypeFilter }> = [
  { label: COPY.filterAllText, value: 'ALL' },
  { label: COPY.filterSystemText, value: 'SYSTEM' },
  { label: COPY.filterApprovalText, value: 'APPROVAL' },
  { label: COPY.filterNoticeText, value: 'NOTICE' },
  { label: COPY.filterTaskText, value: 'TASK' },
  { label: COPY.filterAlertText, value: 'ALERT' },
]

function MessageCenterContent(): ReactElement {
  const type = useMessageStore((state) => state.type)
  const selectedMessageId = useMessageStore((state) => state.selectedMessageId)
  const setType = useMessageStore((state) => state.setType)

  return (
    <div className="space-y-6">
      <Card>
        <CardHeader>
          <Badge>{COPY.titleText}</Badge>
          <CardTitle className="mt-3 flex items-center gap-2 text-2xl">
            <Inbox className="h-6 w-6 text-sky-600" />
            {COPY.titleText}
          </CardTitle>
          <CardDescription className="mt-2 text-base">
            {COPY.descriptionText}
          </CardDescription>
        </CardHeader>
      </Card>

      <div className="grid gap-5 xl:grid-cols-[240px_minmax(0,1fr)_420px]">
        <aside className="rounded-2xl border border-slate-200 bg-white p-3">
          <div className="mb-3 flex items-center gap-2 px-2 text-sm font-semibold text-slate-900">
            <Bell className="h-4 w-4 text-sky-600" />
            消息分类
          </div>
          <div className="space-y-1">
            {TYPE_OPTIONS.map((option) => (
              <button
                className={cn(
                  'flex w-full items-center justify-between rounded-xl px-3 py-2 text-left text-sm font-medium transition',
                  type === option.value
                    ? 'bg-sky-50 text-sky-700'
                    : 'text-slate-600 hover:bg-slate-50 hover:text-slate-950',
                )}
                key={option.value}
                onClick={() => setType(option.value)}
                type="button"
              >
                {option.label}
              </button>
            ))}
          </div>
        </aside>

        <MessageList />

        <MessageDetail messageId={selectedMessageId} />
      </div>
    </div>
  )
}

export default function MessageCenterPage(): ReactElement {
  const [queryClient] = useState(
    () =>
      new QueryClient({
        defaultOptions: {
          queries: {
            retry: 1,
            refetchOnWindowFocus: false,
            staleTime: 30000,
          },
        },
      }),
  )

  return (
    <QueryClientProvider client={queryClient}>
      <MessageCenterContent />
    </QueryClientProvider>
  )
}
