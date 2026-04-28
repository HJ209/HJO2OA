import type { PropsWithChildren, ReactElement } from 'react'
import type { HomeSectionLayout } from '@/features/portal-home/types/portal-home'
import { cn } from '@/utils/cn'

const gridClassMap: Record<HomeSectionLayout, string> = {
  'one-column': 'grid-cols-1',
  'two-column': 'grid-cols-1 xl:grid-cols-2',
  'three-column': 'grid-cols-1 lg:grid-cols-2 2xl:grid-cols-3',
}

export interface HomeSectionProps extends PropsWithChildren {
  title: string
  description?: string
  layout?: HomeSectionLayout
}

export default function HomeSection({
  children,
  description,
  layout = 'two-column',
  title,
}: HomeSectionProps): ReactElement {
  return (
    <section className="space-y-4">
      <div>
        <h2 className="text-xl font-semibold text-slate-950">{title}</h2>
        {description ? (
          <p className="mt-1 text-sm text-slate-500">{description}</p>
        ) : null}
      </div>
      <div className={cn('grid gap-4', gridClassMap[layout])}>{children}</div>
    </section>
  )
}
