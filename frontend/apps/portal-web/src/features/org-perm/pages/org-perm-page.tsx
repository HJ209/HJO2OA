import { useMemo, useState, type ReactElement } from 'react'
import {
  Database,
  FolderTree,
  IdCard,
  Shield,
  UserRound,
  Users,
} from 'lucide-react'
import { useLocation } from 'react-router-dom'
import DataPermissionPage from '@/features/org-perm/pages/data-permission-page'
import OrgTreePage from '@/features/org-perm/pages/org-tree-page'
import PersonListPage from '@/features/org-perm/pages/person-list-page'
import PositionPage from '@/features/org-perm/pages/position-page'
import RoleAuthPage from '@/features/org-perm/pages/role-auth-page'
import SyncAuditPage from '@/features/org-perm/pages/sync-audit-page'
import { cn } from '@/utils/cn'

const TAB_ITEMS = [
  {
    key: 'tree',
    text: '组织架构',
    description: '组织树、部门节点和基础资料维护',
    path: '/org/tree',
    icon: FolderTree,
    panel: <OrgTreePage />,
  },
  {
    key: 'persons',
    text: '人员账号',
    description: '账号资料、所属组织和状态管理',
    path: '/org/persons',
    icon: UserRound,
    panel: <PersonListPage />,
  },
  {
    key: 'positions',
    text: '岗位任职',
    description: '人员岗位、主岗和生效周期',
    path: '/org/positions',
    icon: IdCard,
    panel: <PositionPage />,
  },
  {
    key: 'roles',
    text: '角色授权',
    description: '角色资源树和权限勾选',
    path: '/org/roles',
    icon: Shield,
    panel: <RoleAuthPage />,
  },
  {
    key: 'data-permission',
    text: '数据权限',
    description: '数据范围、字段权限和授权对象',
    path: '/org/data-permission',
    icon: Database,
    panel: <DataPermissionPage />,
  },
  {
    key: 'sync-audit',
    text: '同步审计',
    description: '主数据同步记录和差异追踪',
    path: '/org/sync-audit',
    icon: Users,
    panel: <SyncAuditPage />,
  },
] as const

type TabKey = (typeof TAB_ITEMS)[number]['key']

function resolveInitialTab(pathname: string): TabKey {
  return (
    TAB_ITEMS.find((item) => pathname.startsWith(item.path))?.key ??
    TAB_ITEMS[0].key
  )
}

export default function OrgPermPage(): ReactElement {
  const location = useLocation()
  const [activeTab, setActiveTab] = useState<TabKey>(() =>
    resolveInitialTab(location.pathname),
  )
  const activeItem = useMemo(
    () => TAB_ITEMS.find((item) => item.key === activeTab) ?? TAB_ITEMS[0],
    [activeTab],
  )

  return (
    <div className="space-y-3.5">
      <section className="rounded-lg border border-slate-200 bg-white shadow-sm">
        <div className="flex flex-col gap-1.5 border-b border-slate-100 px-4 py-2.5 lg:flex-row lg:items-end lg:justify-between">
          <div>
            <p className="text-sm font-semibold text-slate-950">组织权限管理</p>
            <p className="mt-0.5 text-sm text-slate-500">
              把组织、人员、岗位、角色和数据权限归在同一个工作区内处理。
            </p>
          </div>
          <p className="text-xs text-slate-500">{activeItem.description}</p>
        </div>

        <div
          aria-label="组织权限内容分组"
          className="flex overflow-x-auto px-2"
          role="tablist"
        >
          {TAB_ITEMS.map((item) => {
            const Icon = item.icon
            const selected = item.key === activeTab

            return (
              <button
                aria-selected={selected}
                className={cn(
                  'relative flex h-10 shrink-0 items-center gap-2 px-3 text-left text-xs transition',
                  selected
                    ? 'text-sky-600'
                    : 'text-slate-600 hover:bg-slate-50 hover:text-slate-950',
                )}
                key={item.key}
                onClick={() => setActiveTab(item.key)}
                role="tab"
                type="button"
              >
                <span
                  className={cn(
                    'flex h-6 w-6 shrink-0 items-center justify-center rounded-md',
                    selected
                      ? 'bg-sky-600 text-white'
                      : 'bg-slate-100 text-slate-600',
                  )}
                >
                  <Icon className="h-3.5 w-3.5" />
                </span>
                <span className="min-w-0">
                  <span className="block font-semibold">{item.text}</span>
                </span>
                {selected ? (
                  <span className="absolute inset-x-2 bottom-0 h-0.5 rounded-full bg-sky-500" />
                ) : null}
              </button>
            )
          })}
        </div>
      </section>

      <section aria-labelledby={`${activeItem.key}-tab`} role="tabpanel">
        {activeItem.panel}
      </section>
    </div>
  )
}
