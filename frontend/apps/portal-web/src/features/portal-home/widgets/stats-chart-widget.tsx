import type { ReactElement } from 'react'
import { BarChart3 } from 'lucide-react'
import HomeWidgetCard from '@/features/portal-home/pages/home-widget-card'
import type { StatsChartCard } from '@/features/portal-home/types/portal-home'

const COPY = {
  titleKey: 'portal.home.stats.title',
  titleText: '统计图卡',
  placeholderKey: 'portal.home.stats.placeholder',
  placeholderText: 'ECharts 图表后续接入',
  emptyKey: 'portal.home.stats.empty',
  emptyText: '暂无统计数据',
} as const

export interface StatsChartWidgetProps {
  statsCards: StatsChartCard[]
}

export default function StatsChartWidget({
  statsCards,
}: StatsChartWidgetProps): ReactElement {
  return (
    <HomeWidgetCard
      description={COPY.placeholderText}
      icon={<BarChart3 className="h-5 w-5" />}
      title={COPY.titleText}
    >
      {statsCards.length > 0 ? (
        <div className="grid gap-3 md:grid-cols-3">
          {statsCards.map((card) => (
            <div className="rounded-xl bg-slate-50 p-4" key={card.id}>
              <p className="text-sm text-slate-500">{card.title}</p>
              <div className="mt-3 flex items-end gap-1">
                <strong className="text-2xl text-slate-950">
                  {card.value}
                </strong>
                {card.unit ? (
                  <span className="pb-1 text-sm text-slate-500">
                    {card.unit}
                  </span>
                ) : null}
              </div>
              {card.trendText ? (
                <p className="mt-2 text-xs text-emerald-700">
                  {card.trendText}
                </p>
              ) : null}
            </div>
          ))}
        </div>
      ) : (
        <p className="rounded-xl bg-slate-50 p-4 text-sm text-slate-500">
          {COPY.emptyText}
        </p>
      )}
    </HomeWidgetCard>
  )
}
