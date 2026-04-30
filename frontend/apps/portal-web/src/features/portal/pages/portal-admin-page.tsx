import { useState, type ReactElement } from 'react'
import {
  LayoutDashboard,
  PanelsTopLeft,
  Settings2,
  SlidersHorizontal,
} from 'lucide-react'
import PortalDesignerPanel from '@/features/portal/pages/portal-designer-panel'
import PortalModelPanel from '@/features/portal/pages/portal-model-panel'
import PortalPersonalizationPanel from '@/features/portal/pages/portal-personalization-panel'
import PortalWidgetConfigPanel from '@/features/portal/pages/portal-widget-config-panel'
import { useIdentityStore } from '@/stores/identity-store'
import { cn } from '@/utils/cn'

type PortalTab = 'model' | 'widgets' | 'personalization' | 'designer'

const TABS: Array<{
  key: PortalTab
  label: string
  description: string
  icon: typeof LayoutDashboard
  adminOnly: boolean
}> = [
  {
    key: 'model',
    label: 'PortalModel',
    description: '模板、页面、区域、版本和发布',
    icon: LayoutDashboard,
    adminOnly: true,
  },
  {
    key: 'widgets',
    label: 'WidgetConfig',
    description: '卡片定义、数据源和展示策略',
    icon: Settings2,
    adminOnly: true,
  },
  {
    key: 'personalization',
    label: 'Personalization',
    description: '个性化布局、显隐、快捷入口',
    icon: SlidersHorizontal,
    adminOnly: false,
  },
  {
    key: 'designer',
    label: 'PortalDesigner',
    description: '拖拽画布、保存、预览、发布',
    icon: PanelsTopLeft,
    adminOnly: true,
  },
]

export default function PortalAdminPage(): ReactElement {
  const [activeTab, setActiveTab] = useState<PortalTab>('model')
  const roleIds = useIdentityStore((state) => state.roleIds)
  const canManagePortal = hasPortalManagementRole(roleIds)
  const active = TABS.find((item) => item.key === activeTab) ?? TABS[0]

  return (
    <div className="space-y-4">
      <section className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
        <div className="flex flex-col gap-4 xl:flex-row xl:items-center xl:justify-between">
          <div>
            <h1 className="text-2xl font-semibold text-slate-950">
              门户工作台配置
            </h1>
            <p className="mt-1 text-sm text-slate-500">
              所有入口均调用 Portal 后端真实
              API，覆盖运行态、设计态和个性化闭环。
            </p>
          </div>
          <div className="grid gap-2 md:grid-cols-2 xl:flex">
            {TABS.map((tab) => {
              const Icon = tab.icon
              const selected = tab.key === activeTab
              const disabled = tab.adminOnly && !canManagePortal

              return (
                <button
                  aria-pressed={selected}
                  className={cn(
                    'rounded-xl border px-3 py-2 text-left text-sm transition',
                    selected
                      ? 'border-sky-500 bg-sky-50 text-sky-800'
                      : 'border-slate-200 bg-white text-slate-600 hover:border-slate-300',
                    disabled ? 'opacity-60' : null,
                  )}
                  key={tab.key}
                  onClick={() => setActiveTab(tab.key)}
                  type="button"
                >
                  <span className="flex items-center gap-2 font-medium">
                    <Icon className="h-4 w-4" />
                    {tab.label}
                  </span>
                  <span className="mt-1 block text-xs text-slate-500">
                    {tab.description}
                  </span>
                </button>
              )
            })}
          </div>
        </div>
      </section>

      {active.key === 'model' ? (
        <PortalModelPanel canManage={canManagePortal} />
      ) : null}
      {active.key === 'widgets' ? (
        <PortalWidgetConfigPanel canManage={canManagePortal} />
      ) : null}
      {active.key === 'personalization' ? <PortalPersonalizationPanel /> : null}
      {active.key === 'designer' ? (
        <PortalDesignerPanel canManage={canManagePortal} />
      ) : null}
    </div>
  )
}

function hasPortalManagementRole(roleIds: string[]): boolean {
  return roleIds.some((roleId) =>
    ['ROLE_PORTAL_ADMIN', 'ROLE_PORTAL_DESIGNER', 'ROLE_ADMIN'].includes(
      roleId,
    ),
  )
}
