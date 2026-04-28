import type { ReactElement } from 'react'
import {
  Database,
  FolderTree,
  IdCard,
  Shield,
  UserRound,
  Users,
} from 'lucide-react'
import { NavLink, Outlet } from 'react-router-dom'
import { cn } from '@/utils/cn'

const NAV_ITEMS = [
  {
    key: 'org.perm.nav.tree',
    text: '组织架构',
    to: '/org/tree',
    icon: FolderTree,
  },
  {
    key: 'org.perm.nav.persons',
    text: '人员账号',
    to: '/org/persons',
    icon: UserRound,
  },
  {
    key: 'org.perm.nav.positions',
    text: '岗位任职',
    to: '/org/positions',
    icon: IdCard,
  },
  {
    key: 'org.perm.nav.roles',
    text: '角色授权',
    to: '/org/roles',
    icon: Shield,
  },
  {
    key: 'org.perm.nav.dataPermission',
    text: '数据权限',
    to: '/org/data-permission',
    icon: Database,
  },
  {
    key: 'org.perm.nav.syncAudit',
    text: '同步审计',
    to: '/org/sync-audit',
    icon: Users,
  },
] as const

export default function OrgPermPage(): ReactElement {
  return (
    <div className="grid gap-6 xl:grid-cols-[220px_minmax(0,1fr)]">
      <aside className="rounded-2xl border border-slate-200 bg-white p-3 shadow-sm">
        <div className="px-3 py-3">
          <p className="text-sm font-semibold text-slate-950">组织权限管理</p>
          <p className="mt-1 text-xs text-slate-500">org.perm.module</p>
        </div>
        <nav className="mt-2 space-y-1">
          {NAV_ITEMS.map((item) => {
            const Icon = item.icon

            return (
              <NavLink
                className={({ isActive }) =>
                  cn(
                    'flex items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-medium transition',
                    isActive
                      ? 'bg-sky-600 text-white shadow-sm'
                      : 'text-slate-600 hover:bg-slate-100 hover:text-slate-950',
                  )
                }
                key={item.key}
                to={item.to}
              >
                <Icon className="h-4 w-4" />
                <span>{item.text}</span>
              </NavLink>
            )
          })}
        </nav>
      </aside>
      <section className="min-w-0">
        <Outlet />
      </section>
    </div>
  )
}
