import type { ReactElement } from 'react'
import { FileText } from 'lucide-react'
import { Badge } from '@/components/ui/badge'
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card'
import { formatUtcToUserTimezone } from '@/utils/format-time'
import { useMessageDetailQuery } from '@/features/messages/hooks/use-messages-query'

const COPY = {
  emptyTitleText: '选择一条消息',
  emptyDescriptionText: '在左侧列表中选择消息后查看完整内容。',
  loadingText: '正在加载消息详情...',
  errorText: '消息详情加载失败，请稍后重试。',
  readAtText: '阅读时间',
  metadataText: '附加信息',
} as const

interface MessageDetailProps {
  messageId: string | null
}

export function MessageDetail({ messageId }: MessageDetailProps): ReactElement {
  const detailQuery = useMessageDetailQuery(messageId)

  if (!messageId) {
    return (
      <Card className="h-full">
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <FileText className="h-5 w-5 text-slate-400" />
            {COPY.emptyTitleText}
          </CardTitle>
          <CardDescription>{COPY.emptyDescriptionText}</CardDescription>
        </CardHeader>
      </Card>
    )
  }

  if (detailQuery.isLoading) {
    return (
      <Card className="h-full">
        <CardHeader>
          <CardTitle>{COPY.loadingText}</CardTitle>
        </CardHeader>
      </Card>
    )
  }

  if (detailQuery.isError || !detailQuery.data) {
    return (
      <Card className="h-full">
        <CardHeader>
          <CardTitle>{COPY.errorText}</CardTitle>
        </CardHeader>
      </Card>
    )
  }

  const detail = detailQuery.data
  const metadataEntries = Object.entries(detail.metadata ?? {})

  return (
    <Card className="h-full">
      <CardHeader>
        <div className="flex flex-wrap items-center gap-2">
          <Badge
            variant={detail.readStatus === 'UNREAD' ? 'default' : 'secondary'}
          >
            {detail.readStatus === 'UNREAD' ? '未读' : '已读'}
          </Badge>
          <Badge variant="secondary">{detail.type}</Badge>
        </div>
        <CardTitle className="text-xl">{detail.title}</CardTitle>
        <CardDescription>
          {formatUtcToUserTimezone(detail.createdAt)}
        </CardDescription>
      </CardHeader>
      <CardContent className="space-y-5">
        <p className="whitespace-pre-wrap text-sm leading-7 text-slate-700">
          {detail.body}
        </p>

        {detail.readAt ? (
          <div className="rounded-2xl bg-slate-50 p-4 text-sm text-slate-500">
            {COPY.readAtText}: {formatUtcToUserTimezone(detail.readAt)}
          </div>
        ) : null}

        {metadataEntries.length > 0 ? (
          <div className="space-y-2">
            <h3 className="text-sm font-semibold text-slate-900">
              {COPY.metadataText}
            </h3>
            <dl className="grid gap-2 text-sm text-slate-500">
              {metadataEntries.map(([key, value]) => (
                <div
                  className="flex items-center justify-between gap-4 rounded-xl bg-slate-50 px-3 py-2"
                  key={key}
                >
                  <dt>{key}</dt>
                  <dd className="text-slate-700">{String(value)}</dd>
                </div>
              ))}
            </dl>
          </div>
        ) : null}
      </CardContent>
    </Card>
  )
}
