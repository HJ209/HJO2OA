import type { ReactElement } from 'react'
import { Bell, Building2, CheckSquare, Home, Settings } from 'lucide-react'
import { NavLink } from 'react-router-dom'
import { cn } from '@/utils/cn'

export interface AppShellNavProps {
  collapsed: boolean
}

interface NavItem {
  titleKey: string
  titleText: string
  to: string
  icon: typeof Home
}

const NAV_ITEMS: NavItem[] = [
  {
    titleKey: 'app.nav.home',
    titleText: '首页',
    to: '/',
    icon: Home,
  },
  {
    titleKey: 'app.nav.todo',
    titleText: '待办',
    to: '/todo',
    icon: CheckSquare,
  },
  {
    titleKey: 'app.nav.messages',
    titleText: '消息',
    to: '/messages',
    icon: Bell,
  },
  {
    titleKey: 'app.nav.org',
    titleText: '组织',
    to: '/org',
    icon: Building2,
  },
  {
    titleKey: 'app.nav.admin',
    titleText: '系统',
    to: '/admin',
    icon: Settings,
  },
]

export function AppShellNav({ collapsed }: AppShellNavProps): ReactElement {
  return (
    <nav className="space-y-2">
      {NAV_ITEMS.map((navItem) => {
        const Icon = navItem.icon

        return (
          <NavLink
            className={({ isActive }) =>
              cn(
                'flex items-center gap-3 rounded-2xl px-3 py-3 text-sm font-medium transition',
                collapsed ? 'justify-center px-2' : 'justify-start',
                isActive
                  ? 'bg-sky-600 text-white shadow-sm'
                  : 'text-slate-600 hover:bg-slate-100 hover:text-slate-950',
              )
            }
            end={navItem.to === '/'}
            key={navItem.titleKey}
            title={navItem.titleText}
            to={navItem.to}
          >
            <Icon className="h-5 w-5 shrink-0" />
            {!collapsed ? <span>{navItem.titleText}</span> : null}
          </NavLink>
        )
      })}
    </nav>
  )
}
