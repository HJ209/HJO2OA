import { useEffect, useState, type MouseEvent, type ReactElement } from 'react'
import { Bell, MessageSquareMore, Search, X } from 'lucide-react'
import { Outlet, useLocation, useNavigate } from 'react-router-dom'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { IdentitySwitcher } from '@/features/identity/components/identity-switcher'
import {
  getWorkspaceAppByKey,
  resolveWorkspaceAppKey,
  WORKSPACE_APPS,
  type WorkspaceAppKey,
} from '@/features/workspace/workspace-registry'
import { UnreadBadge } from '@/features/messages/pages/unread-badge'
import { useAuth } from '@/hooks/use-auth'
import { useIdentity } from '@/hooks/use-identity'
import { useWorkspaceShellStore } from '@/stores/workspace-shell-store'
import { cn } from '@/utils/cn'

interface WindowContextMenuState {
  key: WorkspaceAppKey
  x: number
  y: number
}

export default function AppShell(): ReactElement {
  const location = useLocation()
  const navigate = useNavigate()
  const { logout } = useAuth()
  const { clear } = useIdentity()
  const activeKey = useWorkspaceShellStore((state) => state.activeKey)
  const openKeys = useWorkspaceShellStore((state) => state.openKeys)
  const openWindow = useWorkspaceShellStore((state) => state.openWindow)
  const closeWindow = useWorkspaceShellStore((state) => state.closeWindow)
  const closeOtherWindows = useWorkspaceShellStore(
    (state) => state.closeOtherWindows,
  )
  const closeAllWindows = useWorkspaceShellStore(
    (state) => state.closeAllWindows,
  )
  const syncFromPath = useWorkspaceShellStore((state) => state.syncFromPath)
  const [contextMenu, setContextMenu] = useState<WindowContextMenuState | null>(
    null,
  )
  const routeKey = resolveWorkspaceAppKey(location.pathname)
  const pendingRouteWindow =
    routeKey !== 'home' &&
    activeKey === 'home' &&
    openKeys.length === 1 &&
    openKeys[0] === 'home'
  const visibleActiveKey: WorkspaceAppKey = pendingRouteWindow
    ? routeKey
    : activeKey
  const visibleOpenKeys: WorkspaceAppKey[] = pendingRouteWindow
    ? ['home', routeKey]
    : openKeys

  useEffect(() => {
    syncFromPath(location.pathname)
  }, [location.pathname, syncFromPath])

  useEffect(() => {
    if (!contextMenu) {
      return
    }

    function dismissContextMenu(): void {
      setContextMenu(null)
    }

    window.addEventListener('click', dismissContextMenu)
    window.addEventListener('scroll', dismissContextMenu, true)

    return () => {
      window.removeEventListener('click', dismissContextMenu)
      window.removeEventListener('scroll', dismissContextMenu, true)
    }
  }, [contextMenu])

  function handleLogout(): void {
    clear()
    logout()
    navigate('/login', { replace: true })
  }

  function handleOpenWorkspace(key: WorkspaceAppKey): void {
    openWindow(key)
  }

  function handleTabContextMenu(
    event: MouseEvent<HTMLButtonElement>,
    key: WorkspaceAppKey,
  ): void {
    event.preventDefault()
    setContextMenu({
      key,
      x: event.clientX,
      y: event.clientY,
    })
  }

  return (
    <div className="min-h-screen bg-slate-100 text-slate-950">
      <aside className="fixed inset-y-0 left-0 z-30 flex w-14 flex-col bg-[#0b3a66] py-3 text-white shadow-lg">
        <div className="mb-4 flex justify-center">
          <button
            aria-label="返回首页"
            className="flex h-9 w-9 items-center justify-center rounded-xl bg-sky-500 text-sm font-semibold text-white"
            onClick={() => handleOpenWorkspace('home')}
            type="button"
          >
            门
          </button>
        </div>

        <nav className="flex-1 space-y-2">
          {WORKSPACE_APPS.map((item) => {
            const Icon = item.icon
            const selected = item.key === visibleActiveKey

            return (
              <button
                aria-label={item.railLabel}
                className={cn(
                  'mx-auto flex h-10 w-10 items-center justify-center rounded-xl transition',
                  selected
                    ? 'bg-white text-[#0b3a66] shadow-sm'
                    : 'text-sky-100 hover:bg-white/10 hover:text-white',
                )}
                key={item.key}
                onClick={() => handleOpenWorkspace(item.key)}
                title={item.menuLabel}
                type="button"
              >
                <Icon className="h-4 w-4 shrink-0" />
              </button>
            )
          })}
        </nav>
      </aside>

      <div className="min-h-screen pl-14">
        <header className="sticky top-0 z-20 border-b border-slate-200 bg-white shadow-sm">
          <div className="flex h-12 items-center gap-3 px-4">
            <div className="min-w-0 flex-1">
              <div className="flex items-center gap-2 overflow-x-auto py-1">
                {visibleOpenKeys.map((key) => {
                  const app = getWorkspaceAppByKey(key)
                  const selected = key === visibleActiveKey

                  return (
                    <button
                      aria-selected={selected}
                      className={cn(
                        'group flex h-8 shrink-0 items-center gap-2 rounded-full border px-3 text-xs font-medium transition',
                        selected
                          ? 'border-sky-500 bg-sky-500 text-white shadow-sm'
                          : 'border-slate-200 bg-white text-slate-600 hover:border-slate-300 hover:text-slate-950',
                      )}
                      key={key}
                      onClick={() => handleOpenWorkspace(key)}
                      onContextMenu={(event) =>
                        handleTabContextMenu(event, key)
                      }
                      role="tab"
                      type="button"
                    >
                      <span className="truncate">{app.windowTitle}</span>
                      {key !== 'home' ? (
                        <span
                          className={cn(
                            'flex h-4 w-4 items-center justify-center rounded-full transition',
                            selected
                              ? 'hover:bg-white/20'
                              : 'text-slate-400 hover:bg-slate-100 hover:text-slate-700',
                          )}
                          onClick={(event) => {
                            event.stopPropagation()
                            closeWindow(key)
                          }}
                        >
                          <X className="h-3 w-3" />
                        </span>
                      ) : null}
                    </button>
                  )
                })}
              </div>
            </div>

            <div className="flex shrink-0 items-center gap-3">
              <div className="relative hidden w-72 md:block">
                <Search className="pointer-events-none absolute right-3 top-1/2 h-3.5 w-3.5 -translate-y-1/2 text-slate-400" />
                <Input
                  className="h-8 rounded-full border-slate-200 pr-9 text-xs"
                  placeholder="请输入搜索关键字"
                />
              </div>

              <Button
                aria-label="打开消息中心"
                className="relative h-9 w-9 rounded-full text-slate-500 hover:text-slate-950"
                onClick={() => handleOpenWorkspace('messages')}
                size="icon"
                variant="ghost"
              >
                <MessageSquareMore className="h-4 w-4" />
                <UnreadBadge />
              </Button>

              <Button
                aria-label="打开待办中心"
                className="h-9 w-9 rounded-full text-slate-500 hover:text-slate-950"
                onClick={() => handleOpenWorkspace('todo')}
                size="icon"
                variant="ghost"
              >
                <Bell className="h-4 w-4" />
              </Button>

              <IdentitySwitcher onLogout={handleLogout} />
            </div>
          </div>
        </header>

        <main>
          <Outlet />
        </main>
      </div>

      {contextMenu ? (
        <div
          className="fixed z-50 min-w-40 rounded-lg border border-slate-200 bg-white py-1 shadow-xl"
          style={{ left: contextMenu.x, top: contextMenu.y }}
        >
          {contextMenu.key !== 'home' ? (
            <button
              className="flex w-full items-center px-3 py-2 text-left text-xs text-slate-700 hover:bg-slate-50"
              onClick={() => {
                closeWindow(contextMenu.key)
                setContextMenu(null)
              }}
              type="button"
            >
              关闭当前窗口
            </button>
          ) : null}
          <button
            className="flex w-full items-center px-3 py-2 text-left text-xs text-slate-700 hover:bg-slate-50"
            onClick={() => {
              closeOtherWindows(contextMenu.key)
              setContextMenu(null)
            }}
            type="button"
          >
            关闭其他窗口
          </button>
          <button
            className="flex w-full items-center px-3 py-2 text-left text-xs text-slate-700 hover:bg-slate-50"
            onClick={() => {
              closeAllWindows()
              setContextMenu(null)
            }}
            type="button"
          >
            关闭全部窗口
          </button>
        </div>
      ) : null}
    </div>
  )
}
