import type { ReactElement } from 'react'
import { Archive, ExternalLink, FileText, Trash2 } from 'lucide-react'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card'
import { formatUtcToUserTimezone } from '@/utils/format-time'
import { useMessageAction } from '@/features/messages/hooks/use-message-action'
import { useMessageDetailQuery } from '@/features/messages/hooks/use-messages-query'
import { useMessageStore } from '@/features/messages/stores/message-store'

const COPY = {
  emptyTitleText: '选择一条消息',
  emptyDescriptionText: '从列表中打开消息后可查看内容、来源和投递状态。',
  loadingText: '正在加载消息详情...',
  errorText: '消息详情加载失败，请稍后重试。',
  readAtText: '阅读时间',
  archivedAtText: '归档时间',
  deletedAtText: '删除时间',
  metadataText: '上下文',
  archiveText: '归档',
  deleteText: '删除',
  openLinkText: '打开链接',
  unreadText: '未读',
  readText: '已读',
} as const

interface MessageDetailProps {
  messageId: string | null
}

export function MessageDetail({ messageId }: MessageDetailProps): ReactElement {
  const detailQuery = useMessageDetailQuery(messageId)
  const setSelectedMessageId = useMessageStore(
    (state) => state.setSelectedMessageId,
  )
  const { archiveMessage, deleteMessage, isArchiving, isDeleting } =
    useMessageAction()

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
  const canHide =
    detail.inboxStatus !== 'ARCHIVED' && detail.inboxStatus !== 'DELETED'

  return (
    <Card className="h-full">
      <CardHeader>
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div className="flex flex-wrap items-center gap-2">
            <Badge
              variant={detail.readStatus === 'UNREAD' ? 'default' : 'secondary'}
            >
              {detail.readStatus === 'UNREAD' ? COPY.unreadText : COPY.readText}
            </Badge>
            <Badge variant="secondary">{detail.category ?? detail.type}</Badge>
            {detail.deliveryStatus ? (
              <Badge variant="secondary">{detail.deliveryStatus}</Badge>
            ) : null}
          </div>
          <div className="flex flex-wrap items-center gap-2">
            {detail.deepLink ? (
              <Button
                onClick={() => {
                  window.location.assign(detail.deepLink ?? '/messages')
                }}
                size="sm"
                variant="ghost"
              >
                <ExternalLink className="h-4 w-4" />
                {COPY.openLinkText}
              </Button>
            ) : null}
            <Button
              disabled={!canHide || isArchiving}
              onClick={() => {
                archiveMessage(detail.id, {
                  onSuccess: () => setSelectedMessageId(null),
                })
              }}
              size="sm"
              variant="outline"
            >
              <Archive className="h-4 w-4" />
              {COPY.archiveText}
            </Button>
            <Button
              disabled={detail.inboxStatus === 'DELETED' || isDeleting}
              onClick={() => {
                deleteMessage(detail.id, {
                  onSuccess: () => setSelectedMessageId(null),
                })
              }}
              size="sm"
              variant="outline"
            >
              <Trash2 className="h-4 w-4" />
              {COPY.deleteText}
            </Button>
          </div>
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

        <div className="grid gap-2 text-sm text-slate-500">
          {detail.readAt ? (
            <div className="rounded-xl bg-slate-50 px-3 py-2">
              {COPY.readAtText}: {formatUtcToUserTimezone(detail.readAt)}
            </div>
          ) : null}
          {detail.archivedAt ? (
            <div className="rounded-xl bg-slate-50 px-3 py-2">
              {COPY.archivedAtText}:{' '}
              {formatUtcToUserTimezone(detail.archivedAt)}
            </div>
          ) : null}
          {detail.deletedAt ? (
            <div className="rounded-xl bg-slate-50 px-3 py-2">
              {COPY.deletedAtText}: {formatUtcToUserTimezone(detail.deletedAt)}
            </div>
          ) : null}
        </div>

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
                  <dd className="break-all text-right text-slate-700">
                    {String(value)}
                  </dd>
                </div>
              ))}
            </dl>
          </div>
        ) : null}
      </CardContent>
    </Card>
  )
}
