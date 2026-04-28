import type { ReactElement } from 'react'
import { AlertCircle } from 'lucide-react'
import { Skeleton } from '@/components/ui/skeleton'
import { usePortalHome } from '@/features/portal-home/hooks/use-portal-home'
import { usePortalSnapshot } from '@/features/portal-home/hooks/use-portal-snapshot'
import HomeBanner from '@/features/portal-home/pages/home-banner'
import HomeSection from '@/features/portal-home/pages/home-section'
import type {
  HomeSection as PortalHomeSection,
  HomeWidget,
  PortalHomePageAssembly,
  PortalSnapshot,
} from '@/features/portal-home/types/portal-home'
import AnnouncementWidget from '@/features/portal-home/widgets/announcement-widget'
import MessageSummaryWidget from '@/features/portal-home/widgets/message-summary-widget'
import ShortcutWidget from '@/features/portal-home/widgets/shortcut-widget'
import StatsChartWidget from '@/features/portal-home/widgets/stats-chart-widget'
import TodoSummaryWidget from '@/features/portal-home/widgets/todo-summary-widget'

const COPY = {
  pageTitleKey: 'portal.home.page.title',
  pageTitleText: '门户首页',
  pageDescriptionKey: 'portal.home.page.description',
  pageDescriptionText: '聚合待办、公告、消息、快捷入口和统计图卡。',
  loadingKey: 'portal.home.page.loading',
  loadingText: '门户数据加载中',
  errorKey: 'portal.home.page.error',
  errorText: '门户数据暂时不可用，请稍后重试。',
  middleSectionKey: 'portal.home.section.middle',
  middleSectionText: '工作聚合',
  middleSectionDescriptionKey: 'portal.home.section.middleDescription',
  middleSectionDescriptionText: '优先处理待办和消息，并查看最新公告。',
  bottomSectionKey: 'portal.home.section.bottom',
  bottomSectionText: '信息区',
  bottomSectionDescriptionKey: 'portal.home.section.bottomDescription',
  bottomSectionDescriptionText: '常用入口与运营统计集中展示。',
} as const

const defaultAssembly: PortalHomePageAssembly = {
  sections: [
    {
      id: 'middle',
      title: COPY.middleSectionText,
      description: COPY.middleSectionDescriptionText,
      order: 10,
      layout: 'three-column',
      widgets: [
        {
          id: 'todo-summary',
          title: '待办摘要',
          type: 'todo-summary',
          order: 10,
        },
        {
          id: 'announcement',
          title: '公告摘要',
          type: 'announcement',
          order: 20,
        },
        {
          id: 'message-summary',
          title: '消息摘要',
          type: 'message-summary',
          order: 30,
        },
      ],
    },
    {
      id: 'bottom',
      title: COPY.bottomSectionText,
      description: COPY.bottomSectionDescriptionText,
      order: 20,
      layout: 'two-column',
      widgets: [
        {
          id: 'shortcut',
          title: '快捷入口',
          type: 'shortcut',
          order: 10,
        },
        {
          id: 'stats-chart',
          title: '统计图卡',
          type: 'stats-chart',
          order: 20,
        },
      ],
    },
  ],
}

export default function PortalHomePage(): ReactElement {
  const snapshotQuery = usePortalSnapshot()
  const homeQuery = usePortalHome()
  const snapshot = snapshotQuery.data
  const assembly = homeQuery.data ?? defaultAssembly

  if (snapshotQuery.isLoading || homeQuery.isLoading) {
    return <PortalHomeSkeleton />
  }

  if (!snapshot || snapshotQuery.isError || homeQuery.isError) {
    return <PortalHomeError />
  }

  return (
    <div className="space-y-8">
      <div>
        <h2 className="text-2xl font-semibold text-slate-950">
          {COPY.pageTitleText}
        </h2>
        <p className="mt-1 text-sm text-slate-500">
          {COPY.pageDescriptionText}
        </p>
      </div>

      <HomeBanner banners={snapshot.banners} />

      {orderedSections(assembly.sections).map((section) => (
        <HomeSection
          description={section.description}
          key={section.id}
          layout={section.layout}
          title={section.title}
        >
          {orderedWidgets(section.widgets).map((widget) =>
            renderWidget(widget, snapshot),
          )}
        </HomeSection>
      ))}
    </div>
  )
}

function orderedSections(sections: PortalHomeSection[]): PortalHomeSection[] {
  return [...sections].sort((first, second) => first.order - second.order)
}

function orderedWidgets(widgets: HomeWidget[]): HomeWidget[] {
  return [...widgets].sort((first, second) => first.order - second.order)
}

function renderWidget(
  widget: HomeWidget,
  snapshot: PortalSnapshot,
): ReactElement {
  if (widget.type === 'todo-summary') {
    return <TodoSummaryWidget key={widget.id} summary={snapshot.todoSummary} />
  }

  if (widget.type === 'announcement') {
    return (
      <AnnouncementWidget
        key={widget.id}
        summary={snapshot.announcementSummary}
      />
    )
  }

  if (widget.type === 'message-summary') {
    return (
      <MessageSummaryWidget key={widget.id} summary={snapshot.messageSummary} />
    )
  }

  if (widget.type === 'shortcut') {
    return <ShortcutWidget key={widget.id} shortcuts={snapshot.shortcuts} />
  }

  return <StatsChartWidget key={widget.id} statsCards={snapshot.statsCards} />
}

function PortalHomeSkeleton(): ReactElement {
  return (
    <div className="space-y-6" aria-label={COPY.loadingText}>
      <Skeleton className="h-8 w-48" />
      <Skeleton className="h-64 w-full" />
      <div className="grid gap-4 xl:grid-cols-3">
        <Skeleton className="h-64 w-full" />
        <Skeleton className="h-64 w-full" />
        <Skeleton className="h-64 w-full" />
      </div>
      <div className="grid gap-4 xl:grid-cols-2">
        <Skeleton className="h-56 w-full" />
        <Skeleton className="h-56 w-full" />
      </div>
    </div>
  )
}

function PortalHomeError(): ReactElement {
  return (
    <div className="rounded-2xl border border-red-100 bg-red-50 p-6 text-red-800">
      <div className="flex items-center gap-2 font-medium">
        <AlertCircle className="h-5 w-5" />
        {COPY.errorText}
      </div>
    </div>
  )
}
