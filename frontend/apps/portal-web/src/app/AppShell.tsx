import { useMemo, useState, type ReactElement } from 'react'
import {
  Bell,
  LogOut,
  PanelLeftClose,
  PanelLeftOpen,
  Search,
} from 'lucide-react'
import { Outlet, useLocation, useNavigate } from 'react-router-dom'
import { AppShellNav } from '@/app/AppShellNav'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { UnreadBadge } from '@/features/messages/pages/unread-badge'
import { IdentitySwitcher } from '@/features/identity/components/identity-switcher'
import { useAuth } from '@/hooks/use-auth'
import { useIdentity } from '@/hooks/use-identity'

const COPY = {
  brandKey: 'app.shell.brand',
  brandText: 'HJO2OA',
  searchKey: 'app.shell.searchPlaceholder',
  searchText: '搜索待办、消息、组织或系统功能',
  messageKey: 'app.shell.messages',
  messageText: '消息铃铛',
  logoutKey: 'app.shell.logout',
  logoutText: '退出',
  currentPageKey: 'app.shell.currentPage',
  currentPageText: '当前页面',
} as const

function resolvePageTitle(pathname: string): string {
  if (pathname === '/todo') {
    return '待办中心'
  }

  if (pathname === '/messages') {
    return '消息中心'
  }

  if (pathname === '/org') {
    return '组织与权限'
  }

  if (pathname === '/admin') {
    return '系统管理'
  }

  return '门户首页'
}

export default function AppShell(): ReactElement {
  const navigate = useNavigate()
  const location = useLocation()
  const [collapsed, setCollapsed] = useState(false)
  const { logout } = useAuth()
  const { clear } = useIdentity()
  const pageTitle = useMemo(
    () => resolvePageTitle(location.pathname),
    [location.pathname],
  )

  function handleLogout(): void {
    clear()
    logout()
    navigate('/login', { replace: true })
  }

  return (
    <div className="flex min-h-screen bg-slate-100 text-slate-950">
      <aside
        className={`hidden border-r border-slate-200 bg-white/90 px-4 py-5 shadow-sm backdrop-blur lg:flex lg:flex-col ${
          collapsed ? 'w-[88px]' : 'w-[280px]'
        }`}
      >
        <div
          className={`flex items-center ${collapsed ? 'justify-center' : 'justify-between'} gap-3`}
        >
          {!collapsed ? (
            <div>
              <p className="text-lg font-semibold text-slate-950">
                {COPY.brandText}
              </p>
              <p className="text-sm text-slate-500">Portal Workspace</p>
            </div>
          ) : (
            <div className="flex h-11 w-11 items-center justify-center rounded-xl bg-sky-50 text-sky-600">
              {COPY.brandText.slice(0, 1)}
            </div>
          )}
          <Button
            aria-label={collapsed ? '展开侧边栏' : '折叠侧边栏'}
            onClick={() => setCollapsed((previousState) => !previousState)}
            size="icon"
            variant="ghost"
          >
            {collapsed ? (
              <PanelLeftOpen className="h-4 w-4" />
            ) : (
              <PanelLeftClose className="h-4 w-4" />
            )}
          </Button>
        </div>

        <div className="mt-6 flex-1">
          <AppShellNav collapsed={collapsed} />
        </div>

        {!collapsed ? (
          <div className="rounded-xl bg-slate-50 p-4">
            <p className="text-sm font-medium text-slate-900">
              {COPY.currentPageText}
            </p>
            <p className="mt-2 text-sm text-slate-500">{pageTitle}</p>
          </div>
        ) : null}
      </aside>

      <div className="flex min-h-screen flex-1 flex-col">
        <header className="sticky top-0 z-20 border-b border-slate-200 bg-white/90 px-4 py-4 backdrop-blur lg:px-8">
          <div className="flex flex-col gap-4 xl:flex-row xl:items-center xl:justify-between">
            <div>
              <p className="text-sm text-slate-500">{COPY.brandText} Portal</p>
              <h1 className="text-2xl font-semibold text-slate-950">
                {pageTitle}
              </h1>
            </div>

            <div className="flex flex-col gap-3 lg:flex-row lg:items-center">
              <div className="relative min-w-[260px] flex-1 lg:w-[360px]">
                <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
                <Input className="pl-9" placeholder={COPY.searchText} />
              </div>

              <Button className="relative" size="icon" variant="outline">
                <Bell className="h-4 w-4" />
                <span className="sr-only">{COPY.messageText}</span>
                <UnreadBadge />
              </Button>

              <IdentitySwitcher />

              <Button onClick={handleLogout} variant="ghost">
                <LogOut className="h-4 w-4" />
                {COPY.logoutText}
              </Button>
            </div>
          </div>
        </header>

        <main className="flex-1 px-4 py-6 lg:px-8 lg:py-8">
          <Outlet />
        </main>
      </div>
    </div>
  )
}
