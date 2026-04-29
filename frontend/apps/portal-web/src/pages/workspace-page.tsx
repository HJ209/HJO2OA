import type { ReactElement } from 'react'
import {
  getWorkspaceAppByKey,
  resolveWorkspaceAppKey,
} from '@/features/workspace/workspace-registry'
import { useLocation } from 'react-router-dom'
import { useWorkspaceShellStore } from '@/stores/workspace-shell-store'

export default function WorkspacePage(): ReactElement {
  const location = useLocation()
  const activeKey = useWorkspaceShellStore((state) => state.activeKey)
  const openKeys = useWorkspaceShellStore((state) => state.openKeys)
  const routeKey = resolveWorkspaceAppKey(location.pathname)
  const pendingRouteWindow =
    routeKey !== 'home' &&
    activeKey === 'home' &&
    openKeys.length === 1 &&
    openKeys[0] === 'home'
  const visibleActiveKey = pendingRouteWindow ? routeKey : activeKey
  const activeItem = getWorkspaceAppByKey(visibleActiveKey)
  const ActiveComponent = activeItem.component

  return (
    <div className="min-h-[calc(100vh-3.5rem)] bg-slate-100">
      <section className="p-1.5" role="tabpanel">
        <ActiveComponent />
      </section>
    </div>
  )
}
