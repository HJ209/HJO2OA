import { useMemo, useState, type ReactElement } from 'react'
import {
  AlertCircle,
  Bell,
  CheckCircle2,
  Clock3,
  LayoutDashboard,
  MessageSquareMore,
  RefreshCw,
  ShieldAlert,
  UserRound,
} from 'lucide-react'
import { Link } from 'react-router-dom'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Skeleton } from '@/components/ui/skeleton'
import { usePortalHome } from '@/features/portal-home/hooks/use-portal-home'
import type {
  PortalCardData,
  PortalHomeCardView,
  PortalHomePageView,
  PortalHomeRegionView,
  PortalIdentityCard,
  PortalMessageCard,
  PortalSceneType,
  PortalTodoCard,
} from '@/features/portal/types/portal'
import { isBizError } from '@/services/error-mapper'
import { cn } from '@/utils/cn'

const SCENE_OPTIONS: Array<{
  value: PortalSceneType
  label: string
  description: string
}> = [
  {
    value: 'HOME',
    label: '门户首页',
    description: '统一工作台',
  },
  {
    value: 'OFFICE_CENTER',
    label: '办公中心',
    description: '处理队列',
  },
  {
    value: 'MOBILE_WORKBENCH',
    label: '移动工作台',
    description: '轻量入口',
  },
]

const CARD_ICON_MAP = {
  IDENTITY: UserRound,
  TODO: CheckCircle2,
  MESSAGE: MessageSquareMore,
} as const

export default function PortalHomePage(): ReactElement {
  const [sceneType, setSceneType] = useState<PortalSceneType>('HOME')
  const query = usePortalHome(sceneType)
  const noPermission =
    query.isError &&
    isBizError(query.error) &&
    (query.error.code === 'FORBIDDEN' || query.error.status === 403)

  if (query.isLoading) {
    return <PortalHomeSkeleton />
  }

  if (noPermission) {
    return <PortalHomeNoPermission />
  }

  if (query.isError || !query.data) {
    return (
      <PortalHomeError error={query.error} onRetry={() => query.refetch()} />
    )
  }

  return (
    <PortalRuntimePage
      page={query.data}
      refetching={query.isFetching}
      sceneType={sceneType}
      onRefresh={() => query.refetch()}
      onSceneChange={setSceneType}
    />
  )
}

interface PortalRuntimePageProps {
  page: PortalHomePageView
  sceneType: PortalSceneType
  refetching: boolean
  onSceneChange: (sceneType: PortalSceneType) => void
  onRefresh: () => void
}

function PortalRuntimePage({
  onRefresh,
  onSceneChange,
  page,
  refetching,
  sceneType,
}: PortalRuntimePageProps): ReactElement {
  const visibleCards = useMemo(
    () =>
      page.regions.reduce((count, region) => count + region.cards.length, 0),
    [page.regions],
  )

  return (
    <div className="space-y-5">
      <section className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
        <div className="flex flex-col gap-4 xl:flex-row xl:items-start xl:justify-between">
          <div className="min-w-0">
            <div className="flex items-center gap-3">
              <span className="flex h-11 w-11 shrink-0 items-center justify-center rounded-2xl bg-[#0b3a66] text-sm font-semibold text-white">
                {page.branding.logoText}
              </span>
              <div className="min-w-0">
                <h1 className="truncate text-2xl font-semibold text-slate-950">
                  {page.branding.title}
                </h1>
                <p className="mt-1 text-sm text-slate-500">
                  {page.branding.subtitle}
                </p>
              </div>
            </div>
            <div className="mt-4 flex flex-wrap items-center gap-2 text-xs text-slate-500">
              <StatusPill label={`场景 ${page.sceneType}`} />
              <StatusPill label={`布局 ${page.layoutType}`} />
              {page.sourceTemplateMetadata ? (
                <StatusPill
                  label={`发布 ${page.sourceTemplateMetadata.publicationId}`}
                />
              ) : (
                <StatusPill tone="amber" label="未命中发布模板" />
              )}
            </div>
          </div>

          <div className="flex flex-wrap items-center gap-2">
            {SCENE_OPTIONS.map((item) => (
              <button
                aria-pressed={item.value === sceneType}
                className={cn(
                  'rounded-xl border px-3 py-2 text-left text-xs transition',
                  item.value === sceneType
                    ? 'border-sky-500 bg-sky-50 text-sky-800'
                    : 'border-slate-200 bg-white text-slate-600 hover:border-slate-300',
                )}
                key={item.value}
                onClick={() => onSceneChange(item.value)}
                type="button"
              >
                <span className="block font-medium">{item.label}</span>
                <span className="block text-slate-400">{item.description}</span>
              </button>
            ))}
            <Button
              disabled={refetching}
              onClick={onRefresh}
              size="sm"
              variant="outline"
            >
              <RefreshCw
                className={cn('h-4 w-4', refetching ? 'animate-spin' : null)}
              />
              刷新
            </Button>
          </div>
        </div>
      </section>

      <PortalNavigation items={page.navigation} />

      {visibleCards === 0 ? (
        <PortalHomeEmpty />
      ) : (
        <div className="space-y-5">
          {page.regions.map((region) => (
            <PortalRuntimeRegion key={region.regionCode} region={region} />
          ))}
        </div>
      )}

      <footer className="rounded-2xl border border-slate-200 bg-white px-5 py-3 text-xs text-slate-500">
        {page.footer.text}
      </footer>
    </div>
  )
}

function PortalNavigation({
  items,
}: {
  items: PortalHomePageView['navigation']
}): ReactElement | null {
  if (!items.length) {
    return null
  }

  return (
    <nav className="grid gap-3 md:grid-cols-2 2xl:grid-cols-4">
      {items.map((item) => (
        <Link
          className="rounded-2xl border border-slate-200 bg-white px-4 py-3 shadow-sm transition hover:border-sky-200 hover:bg-sky-50"
          key={item.code}
          to={toWorkspacePath(item.actionLink)}
        >
          <div className="flex items-center justify-between gap-3">
            <span className="font-medium text-slate-900">{item.title}</span>
            {item.badgeCount ? (
              <span className="rounded-full bg-sky-100 px-2 py-0.5 text-xs font-medium text-sky-700">
                {item.badgeCount}
              </span>
            ) : null}
          </div>
        </Link>
      ))}
    </nav>
  )
}

function PortalRuntimeRegion({
  region,
}: {
  region: PortalHomeRegionView
}): ReactElement | null {
  if (!region.cards.length) {
    return null
  }

  return (
    <section className="space-y-3">
      <div>
        <h2 className="text-lg font-semibold text-slate-950">{region.title}</h2>
        <p className="mt-1 text-sm text-slate-500">{region.description}</p>
      </div>
      <div className="grid gap-4 lg:grid-cols-2 2xl:grid-cols-3">
        {region.cards.map((card) => (
          <PortalRuntimeCard card={card} key={card.cardCode} />
        ))}
      </div>
    </section>
  )
}

export function PortalRuntimeCard({
  card,
}: {
  card: PortalHomeCardView
}): ReactElement {
  const Icon = CARD_ICON_MAP[card.cardType] ?? LayoutDashboard
  const failed = card.state === 'FAILED'
  const stale = card.state === 'STALE'

  return (
    <Card className="h-full rounded-2xl">
      <CardHeader className="flex-row items-start justify-between gap-4">
        <div className="min-w-0">
          <div className="flex items-center gap-2">
            <span className="flex h-9 w-9 shrink-0 items-center justify-center rounded-xl bg-slate-100 text-slate-700">
              <Icon className="h-5 w-5" />
            </span>
            <CardTitle className="truncate text-base">{card.title}</CardTitle>
          </div>
          <p className="mt-2 text-sm text-slate-500">{card.description}</p>
        </div>
        <span
          className={cn(
            'shrink-0 rounded-full px-2 py-0.5 text-xs font-medium',
            failed
              ? 'bg-red-50 text-red-700'
              : stale
                ? 'bg-amber-50 text-amber-700'
                : 'bg-emerald-50 text-emerald-700',
          )}
        >
          {card.state}
        </span>
      </CardHeader>
      <CardContent>
        {failed ? (
          <DataSourceFailure message={card.message} />
        ) : (
          <CardDataView cardType={card.cardType} data={card.data} />
        )}
        {stale && card.message ? (
          <p className="mt-3 rounded-xl bg-amber-50 px-3 py-2 text-xs text-amber-700">
            {card.message}
          </p>
        ) : null}
        <Link
          className="mt-4 inline-flex h-9 w-full items-center justify-center rounded-xl bg-slate-900 px-3 text-sm font-medium text-white hover:bg-slate-800"
          to={toWorkspacePath(card.actionLink)}
        >
          打开
        </Link>
      </CardContent>
    </Card>
  )
}

function CardDataView({
  cardType,
  data,
}: {
  cardType: PortalHomeCardView['cardType']
  data: PortalCardData
}): ReactElement {
  if (cardType === 'IDENTITY' && isIdentityCard(data)) {
    return <IdentityCardData data={data} />
  }

  if (cardType === 'TODO' && isTodoCard(data)) {
    return <TodoCardData data={data} />
  }

  if (cardType === 'MESSAGE' && isMessageCard(data)) {
    return <MessageCardData data={data} />
  }

  return (
    <div className="rounded-xl bg-slate-50 px-3 py-6 text-center text-sm text-slate-500">
      当前卡片暂无可展示数据
    </div>
  )
}

function IdentityCardData({
  data,
}: {
  data: PortalIdentityCard
}): ReactElement {
  return (
    <div className="space-y-3">
      <MetricLine label="当前岗位" value={data.positionName} />
      <MetricLine label="组织" value={data.organizationName} />
      <MetricLine label="部门" value={data.departmentName} />
      <MetricLine label="身份类型" value={data.assignmentType} />
    </div>
  )
}

function TodoCardData({ data }: { data: PortalTodoCard }): ReactElement {
  return (
    <div className="space-y-4">
      <div className="grid grid-cols-2 gap-3">
        <MetricBox label="待办总数" value={data.totalCount} />
        <MetricBox label="紧急待办" tone="amber" value={data.urgentCount} />
      </div>
      <TopItems
        emptyText="暂无待处理事项"
        items={data.topItems.map((item) => ({
          id: item.todoId,
          title: item.title,
          meta: [item.category, item.urgency].filter(Boolean).join(' · '),
          time: item.dueTime ?? item.createdAt,
        }))}
      />
    </div>
  )
}

function MessageCardData({ data }: { data: PortalMessageCard }): ReactElement {
  return (
    <div className="space-y-4">
      <MetricBox label="未读消息" tone="sky" value={data.unreadCount} />
      <TopItems
        emptyText="暂无未读消息"
        items={data.topItems.map((item) => ({
          id: item.notificationId,
          title: item.title,
          meta: [item.category, item.priority].filter(Boolean).join(' · '),
          time: item.createdAt,
        }))}
      />
    </div>
  )
}

function TopItems({
  emptyText,
  items,
}: {
  emptyText: string
  items: Array<{
    id: string
    title: string
    meta: string
    time?: string | null
  }>
}): ReactElement {
  if (!items.length) {
    return (
      <div className="rounded-xl bg-slate-50 px-3 py-5 text-center text-sm text-slate-500">
        {emptyText}
      </div>
    )
  }

  return (
    <div className="space-y-2">
      {items.slice(0, 4).map((item) => (
        <div
          className="rounded-xl border border-slate-100 bg-slate-50 px-3 py-2"
          key={item.id}
        >
          <div className="truncate text-sm font-medium text-slate-900">
            {item.title}
          </div>
          <div className="mt-1 flex items-center gap-2 text-xs text-slate-500">
            <span>{item.meta}</span>
            {item.time ? (
              <>
                <Clock3 className="h-3 w-3" />
                <span>{new Date(item.time).toLocaleString()}</span>
              </>
            ) : null}
          </div>
        </div>
      ))}
    </div>
  )
}

function MetricBox({
  label,
  tone = 'slate',
  value,
}: {
  label: string
  value: number
  tone?: 'slate' | 'amber' | 'sky'
}): ReactElement {
  const toneClasses = {
    slate: 'bg-slate-50 text-slate-950',
    amber: 'bg-amber-50 text-amber-700',
    sky: 'bg-sky-50 text-sky-700',
  }

  return (
    <div className={cn('rounded-xl p-3', toneClasses[tone])}>
      <div className="text-2xl font-semibold">{value}</div>
      <div className="mt-1 text-xs text-slate-500">{label}</div>
    </div>
  )
}

function MetricLine({
  label,
  value,
}: {
  label: string
  value: string
}): ReactElement {
  return (
    <div className="flex items-center justify-between gap-3 rounded-xl bg-slate-50 px-3 py-2 text-sm">
      <span className="text-slate-500">{label}</span>
      <span className="truncate font-medium text-slate-900">{value}</span>
    </div>
  )
}

function DataSourceFailure({
  message,
}: {
  message?: string | null
}): ReactElement {
  return (
    <div className="rounded-xl border border-red-100 bg-red-50 px-3 py-4 text-sm text-red-700">
      <div className="flex items-center gap-2 font-medium">
        <AlertCircle className="h-4 w-4" />
        数据源失败
      </div>
      {message ? <p className="mt-2 text-xs">{message}</p> : null}
    </div>
  )
}

function PortalHomeSkeleton(): ReactElement {
  return (
    <div className="space-y-5" aria-label="门户数据加载中">
      <Skeleton className="h-32 w-full" />
      <div className="grid gap-3 md:grid-cols-2 2xl:grid-cols-4">
        <Skeleton className="h-20 w-full" />
        <Skeleton className="h-20 w-full" />
        <Skeleton className="h-20 w-full" />
        <Skeleton className="h-20 w-full" />
      </div>
      <div className="grid gap-4 lg:grid-cols-2 2xl:grid-cols-3">
        <Skeleton className="h-72 w-full" />
        <Skeleton className="h-72 w-full" />
        <Skeleton className="h-72 w-full" />
      </div>
    </div>
  )
}

function PortalHomeError({
  error,
  onRetry,
}: {
  error: unknown
  onRetry: () => void
}): ReactElement {
  const message = isBizError(error)
    ? (error.backendMessage ?? error.message)
    : '门户数据暂时不可用，请稍后重试。'

  return (
    <div className="rounded-2xl border border-red-100 bg-red-50 p-6 text-red-800">
      <div className="flex items-center gap-2 font-medium">
        <AlertCircle className="h-5 w-5" />
        {message}
      </div>
      <Button className="mt-4" onClick={onRetry} size="sm" variant="outline">
        <RefreshCw className="h-4 w-4" />
        重试
      </Button>
    </div>
  )
}

function PortalHomeNoPermission(): ReactElement {
  return (
    <div className="rounded-2xl border border-amber-100 bg-amber-50 p-6 text-amber-800">
      <div className="flex items-center gap-2 font-medium">
        <ShieldAlert className="h-5 w-5" />
        当前身份暂无门户访问权限
      </div>
      <p className="mt-2 text-sm">
        请切换到具备门户访问权限的身份，或联系管理员调整发布范围。
      </p>
    </div>
  )
}

function PortalHomeEmpty(): ReactElement {
  return (
    <div className="rounded-2xl border border-dashed border-slate-300 bg-white p-10 text-center">
      <Bell className="mx-auto h-8 w-8 text-slate-400" />
      <h2 className="mt-3 text-lg font-semibold text-slate-950">
        当前门户没有可展示组件
      </h2>
      <p className="mt-2 text-sm text-slate-500">
        已调用后端装配接口，但当前发布模板或个性化结果未返回任何卡片。
      </p>
    </div>
  )
}

function StatusPill({
  label,
  tone = 'slate',
}: {
  label: string
  tone?: 'slate' | 'amber'
}): ReactElement {
  return (
    <span
      className={cn(
        'rounded-full px-2 py-1',
        tone === 'amber'
          ? 'bg-amber-50 text-amber-700'
          : 'bg-slate-100 text-slate-600',
      )}
    >
      {label}
    </span>
  )
}

function toWorkspacePath(actionLink: string): string {
  if (actionLink.includes('/todo')) {
    return '/todo'
  }

  if (actionLink.includes('/msg') || actionLink.includes('/messages')) {
    return '/messages'
  }

  if (actionLink.includes('/identity') || actionLink.includes('/org')) {
    return '/org/tree'
  }

  if (actionLink.startsWith('/api/')) {
    return '/'
  }

  return actionLink || '/'
}

function isIdentityCard(data: PortalCardData): data is PortalIdentityCard {
  return Boolean(data && typeof data === 'object' && 'positionName' in data)
}

function isTodoCard(data: PortalCardData): data is PortalTodoCard {
  return Boolean(data && typeof data === 'object' && 'totalCount' in data)
}

function isMessageCard(data: PortalCardData): data is PortalMessageCard {
  return Boolean(data && typeof data === 'object' && 'unreadCount' in data)
}
