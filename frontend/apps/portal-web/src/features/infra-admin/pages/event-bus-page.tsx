import { useMemo, useState, type ReactElement } from 'react'
import {
  AlertTriangle,
  Eye,
  Inbox,
  Play,
  RefreshCw,
  RotateCcw,
} from 'lucide-react'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { InfraPageSection } from '@/features/infra-admin/components/infra-page-section'
import { InfraTable } from '@/features/infra-admin/components/infra-table'
import {
  useDeadLetterEventBusEvent,
  useEventBusDeadLetters,
  useEventBusEventDetail,
  useEventBusEvents,
  useEventBusFailedEvents,
  useEventBusStatistics,
  useReplayEventBusEvents,
  useRetryEventBusEvent,
} from '@/features/infra-admin/hooks/use-event-bus'
import type {
  EventBusEvent,
  EventBusQuery,
  EventBusReplayRequest,
  EventOutboxStatus,
} from '@/features/infra-admin/types/infra'
import { cn } from '@/utils/cn'
import { formatUtcToUserTimezone } from '@/utils/format-time'

type EventBusTab = 'all' | 'failed' | 'dead'

const TAB_ITEMS: Array<{
  key: EventBusTab
  label: string
  icon: typeof Inbox
}> = [
  { key: 'all', label: '全部事件', icon: Inbox },
  { key: 'failed', label: '失败事件', icon: AlertTriangle },
  { key: 'dead', label: '死信队列', icon: RotateCcw },
]

const STATUS_OPTIONS: Array<EventOutboxStatus | ''> = [
  '',
  'PENDING',
  'PUBLISHED',
  'FAILED',
  'DEAD',
]

const STATUS_LABEL: Record<EventOutboxStatus, string> = {
  PENDING: '待发布',
  PUBLISHED: '已发布',
  FAILED: '失败待重试',
  DEAD: '死信',
}

export default function EventBusPage(): ReactElement {
  const [activeTab, setActiveTab] = useState<EventBusTab>('all')
  const [query, setQuery] = useState<EventBusQuery>({
    page: 1,
    size: 20,
  })
  const [selectedEventId, setSelectedEventId] = useState<string>()
  const [operationReason, setOperationReason] = useState('')
  const [replayRequest, setReplayRequest] = useState<EventBusReplayRequest>({
    eventType: '',
    status: 'FAILED',
    reason: '',
  })

  const allEventsQuery = useEventBusEvents(query)
  const failedEventsQuery = useEventBusFailedEvents(query)
  const deadLettersQuery = useEventBusDeadLetters(query)
  const detailQuery = useEventBusEventDetail(selectedEventId)
  const statisticsQuery = useEventBusStatistics()
  const retryMutation = useRetryEventBusEvent()
  const deadLetterMutation = useDeadLetterEventBusEvent()
  const replayMutation = useReplayEventBusEvents()

  const activeQuery = useMemo(() => {
    if (activeTab === 'failed') {
      return failedEventsQuery
    }
    if (activeTab === 'dead') {
      return deadLettersQuery
    }
    return allEventsQuery
  }, [activeTab, allEventsQuery, deadLettersQuery, failedEventsQuery])

  const events = activeQuery.data?.items ?? []
  const operationDisabled = operationReason.trim().length === 0

  return (
    <div className="space-y-4">
      <section className="grid gap-3 sm:grid-cols-2 xl:grid-cols-5">
        <MetricTile label="待发布" value={statisticsQuery.data?.pending ?? 0} />
        <MetricTile
          label="已发布"
          value={statisticsQuery.data?.published ?? 0}
        />
        <MetricTile label="失败" value={statisticsQuery.data?.failed ?? 0} />
        <MetricTile label="死信" value={statisticsQuery.data?.dead ?? 0} />
        <MetricTile label="总量" value={statisticsQuery.data?.total ?? 0} />
      </section>

      <InfraPageSection
        actions={
          <Button
            disabled={activeQuery.isFetching}
            onClick={() => void activeQuery.refetch()}
            size="sm"
            variant="outline"
          >
            <RefreshCw className="h-4 w-4" />
            刷新
          </Button>
        }
        description="Outbox 可靠事件、失败重试和死信治理"
        title="可靠事件"
      >
        <div className="mb-4 flex flex-col gap-3">
          <div className="flex flex-wrap gap-2">
            {TAB_ITEMS.map((item) => {
              const Icon = item.icon
              const selected = activeTab === item.key

              return (
                <button
                  aria-pressed={selected}
                  className={cn(
                    'inline-flex h-9 items-center gap-2 rounded-md border px-3 text-sm font-medium transition',
                    selected
                      ? 'border-sky-500 bg-sky-50 text-sky-700'
                      : 'border-slate-200 bg-white text-slate-600 hover:bg-slate-50',
                  )}
                  key={item.key}
                  onClick={() => setActiveTab(item.key)}
                  type="button"
                >
                  <Icon className="h-4 w-4" />
                  {item.label}
                </button>
              )
            })}
          </div>

          <div className="grid gap-2 lg:grid-cols-[1.4fr_1fr_1fr_auto]">
            <Input
              onChange={(event) =>
                setQuery((current) => ({
                  ...current,
                  eventType: event.target.value,
                }))
              }
              placeholder="事件类型"
              value={query.eventType ?? ''}
            />
            <Input
              onChange={(event) =>
                setQuery((current) => ({
                  ...current,
                  tenantId: event.target.value,
                }))
              }
              placeholder="租户ID"
              value={query.tenantId ?? ''}
            />
            <select
              className="h-10 rounded-xl border border-slate-200 bg-white px-3 text-sm text-slate-900 shadow-sm"
              disabled={activeTab !== 'all'}
              onChange={(event) =>
                setQuery((current) => ({
                  ...current,
                  status: event.target.value as EventOutboxStatus | '',
                }))
              }
              value={query.status ?? ''}
            >
              {STATUS_OPTIONS.map((status) => (
                <option key={status || 'ALL'} value={status}>
                  {status ? STATUS_LABEL[status] : '全部状态'}
                </option>
              ))}
            </select>
            <Input
              onChange={(event) => setOperationReason(event.target.value)}
              placeholder="处理原因"
              value={operationReason}
            />
          </div>
        </div>

        <InfraTable
          columns={[
            {
              key: 'eventType',
              title: '事件类型',
              render: (item) => (
                <div>
                  <p className="font-medium text-slate-900">{item.eventType}</p>
                  <p className="mt-1 text-xs text-slate-500">
                    {item.aggregateType}:{item.aggregateId}
                  </p>
                </div>
              ),
            },
            {
              key: 'eventId',
              title: '事件ID',
              render: (item) => (
                <span className="break-all font-mono text-xs">
                  {item.eventId}
                </span>
              ),
            },
            {
              key: 'tenantId',
              title: '租户',
              render: (item) => item.tenantId,
            },
            {
              key: 'status',
              title: '状态',
              render: (item) => <EventStatusBadge status={item.status} />,
            },
            {
              key: 'retry',
              title: '重试',
              render: (item) => (
                <div className="text-sm">
                  <p>{item.retryCount}</p>
                  <p className="text-xs text-slate-500">
                    {formatUtcToUserTimezone(item.nextRetryAt)}
                  </p>
                </div>
              ),
            },
            {
              key: 'occurredAt',
              title: '发生时间',
              render: (item) => formatUtcToUserTimezone(item.occurredAt),
            },
            {
              key: 'actions',
              title: '操作',
              render: (item) => (
                <EventActions
                  disabled={operationDisabled}
                  event={item}
                  isDeadLettering={deadLetterMutation.isPending}
                  isRetrying={retryMutation.isPending}
                  onDeadLetter={() =>
                    deadLetterMutation.mutate({
                      eventId: item.eventId,
                      reason: operationReason,
                    })
                  }
                  onDetail={() => setSelectedEventId(item.eventId)}
                  onRetry={() =>
                    retryMutation.mutate({
                      eventId: item.eventId,
                      reason: operationReason,
                    })
                  }
                />
              ),
            },
          ]}
          getRowKey={(item) => item.eventId}
          isLoading={activeQuery.isLoading}
          items={events}
        />
      </InfraPageSection>

      <div className="grid gap-4 xl:grid-cols-[minmax(0,1fr)_minmax(360px,0.65fr)]">
        <InfraPageSection
          actions={
            <Button
              disabled={
                replayMutation.isPending || !replayRequest.reason.trim()
              }
              onClick={() =>
                replayMutation.mutate(compactReplayRequest(replayRequest))
              }
              size="sm"
            >
              <Play className="h-4 w-4" />
              重放
            </Button>
          }
          description="按 eventId、eventType、时间范围或状态重放"
          title="重放操作"
        >
          <div className="grid gap-3 md:grid-cols-2">
            <Input
              onChange={(event) =>
                setReplayRequest((current) => ({
                  ...current,
                  eventId: event.target.value,
                }))
              }
              placeholder="事件ID"
              value={replayRequest.eventId ?? ''}
            />
            <Input
              onChange={(event) =>
                setReplayRequest((current) => ({
                  ...current,
                  eventType: event.target.value,
                }))
              }
              placeholder="事件类型"
              value={replayRequest.eventType ?? ''}
            />
            <select
              className="h-10 rounded-xl border border-slate-200 bg-white px-3 text-sm text-slate-900 shadow-sm"
              onChange={(event) =>
                setReplayRequest((current) => ({
                  ...current,
                  status: event.target.value as EventOutboxStatus | '',
                }))
              }
              value={replayRequest.status ?? ''}
            >
              {STATUS_OPTIONS.map((status) => (
                <option key={status || 'ALL'} value={status}>
                  {status ? STATUS_LABEL[status] : '全部状态'}
                </option>
              ))}
            </select>
            <Input
              onChange={(event) =>
                setReplayRequest((current) => ({
                  ...current,
                  tenantId: event.target.value,
                }))
              }
              placeholder="租户ID"
              value={replayRequest.tenantId ?? ''}
            />
            <Input
              onChange={(event) =>
                setReplayRequest((current) => ({
                  ...current,
                  occurredFrom: event.target.value,
                }))
              }
              type="datetime-local"
              value={replayRequest.occurredFrom ?? ''}
            />
            <Input
              onChange={(event) =>
                setReplayRequest((current) => ({
                  ...current,
                  occurredTo: event.target.value,
                }))
              }
              type="datetime-local"
              value={replayRequest.occurredTo ?? ''}
            />
            <Input
              className="md:col-span-2"
              onChange={(event) =>
                setReplayRequest((current) => ({
                  ...current,
                  reason: event.target.value,
                }))
              }
              placeholder="重放原因"
              value={replayRequest.reason}
            />
          </div>
          {replayMutation.data ? (
            <p className="mt-3 text-sm text-slate-600">
              已重放 {replayMutation.data.replayedCount} 条
            </p>
          ) : null}
        </InfraPageSection>

        <InfraPageSection
          description="事件 envelope、payload 和最后错误"
          title="详情查看"
        >
          {detailQuery.data ? (
            <div className="space-y-3 text-sm">
              <div className="grid gap-2">
                <DetailLine label="eventId" value={detailQuery.data.eventId} />
                <DetailLine
                  label="eventType"
                  value={detailQuery.data.eventType}
                />
                <DetailLine label="traceId" value={detailQuery.data.traceId} />
                <DetailLine
                  label="schemaVersion"
                  value={detailQuery.data.schemaVersion}
                />
              </div>
              {detailQuery.data.lastError ? (
                <p className="rounded-md bg-rose-50 p-3 text-rose-700">
                  {detailQuery.data.lastError}
                </p>
              ) : null}
              <pre className="max-h-80 overflow-auto rounded-md bg-slate-950 p-3 text-xs text-slate-100">
                {formatPayload(detailQuery.data.payloadJson)}
              </pre>
            </div>
          ) : (
            <p className="text-sm text-slate-500">选择事件查看详情</p>
          )}
        </InfraPageSection>
      </div>
    </div>
  )
}

function MetricTile({
  label,
  value,
}: {
  label: string
  value: number
}): ReactElement {
  return (
    <div className="rounded-lg border border-slate-200 bg-white p-4 shadow-sm">
      <p className="text-xs font-medium text-slate-500">{label}</p>
      <p className="mt-2 text-2xl font-semibold text-slate-950">{value}</p>
    </div>
  )
}

function EventStatusBadge({
  status,
}: {
  status: EventOutboxStatus
}): ReactElement {
  const variant = status === 'PUBLISHED' ? 'success' : 'secondary'
  const className =
    status === 'FAILED'
      ? 'bg-amber-100 text-amber-700'
      : status === 'DEAD'
        ? 'bg-rose-100 text-rose-700'
        : undefined

  return (
    <Badge className={className} variant={variant}>
      {STATUS_LABEL[status]}
    </Badge>
  )
}

function EventActions({
  event,
  disabled,
  isRetrying,
  isDeadLettering,
  onDetail,
  onRetry,
  onDeadLetter,
}: {
  event: EventBusEvent
  disabled: boolean
  isRetrying: boolean
  isDeadLettering: boolean
  onDetail: () => void
  onRetry: () => void
  onDeadLetter: () => void
}): ReactElement {
  const canRetry = event.status === 'FAILED' || event.status === 'DEAD'
  const canDeadLetter = event.status !== 'DEAD'

  return (
    <div className="flex flex-wrap gap-2">
      <Button onClick={onDetail} size="sm" variant="outline">
        <Eye className="h-4 w-4" />
        详情
      </Button>
      <Button
        disabled={!canRetry || disabled || isRetrying}
        onClick={onRetry}
        size="sm"
        variant="outline"
      >
        <RotateCcw className="h-4 w-4" />
        重试
      </Button>
      <Button
        disabled={!canDeadLetter || disabled || isDeadLettering}
        onClick={onDeadLetter}
        size="sm"
        variant="outline"
      >
        <AlertTriangle className="h-4 w-4" />
        死信
      </Button>
    </div>
  )
}

function DetailLine({
  label,
  value,
}: {
  label: string
  value?: string
}): ReactElement {
  return (
    <p className="grid grid-cols-[110px_minmax(0,1fr)] gap-2">
      <span className="text-slate-500">{label}</span>
      <span className="break-all font-mono text-xs text-slate-900">
        {value ?? ''}
      </span>
    </p>
  )
}

function formatPayload(payloadJson: string): string {
  try {
    return JSON.stringify(JSON.parse(payloadJson), null, 2)
  } catch {
    return payloadJson
  }
}

function compactReplayRequest(
  request: EventBusReplayRequest,
): EventBusReplayRequest {
  return Object.fromEntries(
    Object.entries(request).filter(([, value]) => Boolean(value)),
  ) as EventBusReplayRequest
}
