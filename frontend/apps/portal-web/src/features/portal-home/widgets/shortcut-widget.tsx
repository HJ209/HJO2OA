import type { ReactElement } from 'react'
import { AppWindow, Bell, Building2, Settings, Workflow } from 'lucide-react'
import { Link } from 'react-router-dom'
import HomeWidgetCard from '@/features/portal-home/pages/home-widget-card'
import type {
  PortalShortcutIconName,
  ShortcutEntry,
} from '@/features/portal-home/types/portal-home'

const COPY = {
  titleKey: 'portal.home.shortcut.title',
  titleText: '快捷入口',
  emptyKey: 'portal.home.shortcut.empty',
  emptyText: '暂无快捷入口',
} as const

const iconMap: Record<PortalShortcutIconName, typeof AppWindow> = {
  todo: Workflow,
  message: Bell,
  org: Building2,
  admin: Settings,
  app: AppWindow,
}

export interface ShortcutWidgetProps {
  shortcuts: ShortcutEntry[]
}

export default function ShortcutWidget({
  shortcuts,
}: ShortcutWidgetProps): ReactElement {
  return (
    <HomeWidgetCard
      icon={<AppWindow className="h-5 w-5" />}
      title={COPY.titleText}
    >
      {shortcuts.length > 0 ? (
        <div className="grid grid-cols-2 gap-3 md:grid-cols-4">
          {shortcuts.map((shortcut) => {
            const Icon = iconMap[shortcut.icon ?? 'app']

            return (
              <Link
                className="flex min-h-24 flex-col items-center justify-center gap-3 rounded-xl border border-slate-200 bg-white p-3 text-center text-sm font-medium text-slate-800 hover:border-sky-200 hover:bg-sky-50"
                key={shortcut.id}
                to={shortcut.href}
              >
                <span className="flex h-10 w-10 items-center justify-center rounded-xl bg-slate-100 text-slate-700">
                  <Icon className="h-5 w-5" />
                </span>
                <span className="line-clamp-2">{shortcut.title}</span>
              </Link>
            )
          })}
        </div>
      ) : (
        <p className="rounded-xl bg-slate-50 p-4 text-sm text-slate-500">
          {COPY.emptyText}
        </p>
      )}
    </HomeWidgetCard>
  )
}
