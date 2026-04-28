import type { PropsWithChildren, ReactElement, ReactNode } from 'react'
import { ArrowRight } from 'lucide-react'
import { Link } from 'react-router-dom'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import type { WidgetChromeAction } from '@/features/portal-home/types/portal-home'
import { cn } from '@/utils/cn'

export interface HomeWidgetCardProps extends PropsWithChildren {
  title: string
  description?: string
  action?: WidgetChromeAction
  icon?: ReactNode
  className?: string
}

export default function HomeWidgetCard({
  action,
  children,
  className,
  description,
  icon,
  title,
}: HomeWidgetCardProps): ReactElement {
  return (
    <Card className={cn('h-full rounded-2xl', className)}>
      <CardHeader className="flex-row items-start justify-between gap-4 p-5">
        <div className="min-w-0">
          <div className="flex items-center gap-2">
            {icon ? (
              <span className="flex h-9 w-9 shrink-0 items-center justify-center rounded-xl bg-slate-100 text-slate-700">
                {icon}
              </span>
            ) : null}
            <CardTitle className="truncate text-base">{title}</CardTitle>
          </div>
          {description ? (
            <p className="mt-2 text-sm text-slate-500">{description}</p>
          ) : null}
        </div>
        {action ? (
          <Link
            className="inline-flex shrink-0 items-center gap-1 text-sm font-medium text-sky-700 hover:text-sky-800"
            to={action.href}
          >
            {action.label}
            <ArrowRight className="h-4 w-4" />
          </Link>
        ) : null}
      </CardHeader>
      <CardContent className="px-5 pb-5">{children}</CardContent>
    </Card>
  )
}
