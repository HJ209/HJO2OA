import { useMemo, useState, type ReactElement } from 'react'
import {
  Activity,
  CalendarClock,
  Clock,
  Database,
  FileCode2,
  FileText,
  Globe2,
  HardDrive,
  KeyRound,
  Languages,
  ListTree,
  Paperclip,
  Settings2,
} from 'lucide-react'
import { useLocation } from 'react-router-dom'
import { Badge } from '@/components/ui/badge'
import {
  Card,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card'
import AttachmentPage from '@/features/infra-admin/pages/attachment-page'
import AuditPage from '@/features/infra-admin/pages/audit-page'
import CachePage from '@/features/infra-admin/pages/cache-page'
import ConfigPage from '@/features/infra-admin/pages/config-page'
import DataI18nPage from '@/features/infra-admin/pages/data-i18n-page'
import DictionaryPage from '@/features/infra-admin/pages/dictionary-page'
import ErrorCodePage from '@/features/infra-admin/pages/error-code-page'
import EventBusPage from '@/features/infra-admin/pages/event-bus-page'
import I18nPage from '@/features/infra-admin/pages/i18n-page'
import SchedulerPage from '@/features/infra-admin/pages/scheduler-page'
import SecurityPage from '@/features/infra-admin/pages/security-page'
import TenantPage from '@/features/infra-admin/pages/tenant-page'
import TimezonePage from '@/features/infra-admin/pages/timezone-page'
import { INFRA_COPY } from '@/features/infra-admin/infra-copy'
import { cn } from '@/utils/cn'

const TAB_ITEMS = [
  {
    key: 'dictionary',
    label: INFRA_COPY.nav.dictionary,
    description: '统一维护业务字典和枚举项。',
    path: '/admin/dictionary',
    icon: ListTree,
    panel: <DictionaryPage />,
  },
  {
    key: 'config',
    label: INFRA_COPY.nav.config,
    description: '运行参数、灰度开关和配置版本。',
    path: '/admin/config',
    icon: Settings2,
    panel: <ConfigPage />,
  },
  {
    key: 'error-codes',
    label: INFRA_COPY.nav.errorCodes,
    description: '错误码、提示文案和严重级别。',
    path: '/admin/error-codes',
    icon: FileCode2,
    panel: <ErrorCodePage />,
  },
  {
    key: 'cache',
    label: INFRA_COPY.nav.cache,
    description: '缓存策略、失效记录和刷新控制。',
    path: '/admin/cache',
    icon: HardDrive,
    panel: <CachePage />,
  },
  {
    key: 'audit',
    label: INFRA_COPY.nav.audit,
    description: '关键操作、访问记录和追踪信息。',
    path: '/admin/audit',
    icon: FileText,
    panel: <AuditPage />,
  },
  {
    key: 'tenant',
    label: INFRA_COPY.nav.tenant,
    description: '租户资料、隔离策略和启停状态。',
    path: '/admin/tenant',
    icon: Database,
    panel: <TenantPage />,
  },
  {
    key: 'security',
    label: INFRA_COPY.nav.security,
    description: '密码策略、会话规则和安全基线。',
    path: '/admin/security',
    icon: KeyRound,
    panel: <SecurityPage />,
  },
  {
    key: 'scheduler',
    label: INFRA_COPY.nav.scheduler,
    description: '计划任务、触发记录和运行结果。',
    path: '/admin/scheduler',
    icon: CalendarClock,
    panel: <SchedulerPage />,
  },
  {
    key: 'i18n',
    label: INFRA_COPY.nav.i18n,
    description: '界面多语言资源和翻译状态。',
    path: '/admin/i18n',
    icon: Languages,
    panel: <I18nPage />,
  },
  {
    key: 'data-i18n',
    label: INFRA_COPY.nav.dataI18n,
    description: '业务数据翻译和字段本地化。',
    path: '/admin/data-i18n',
    icon: Globe2,
    panel: <DataI18nPage />,
  },
  {
    key: 'timezone',
    label: INFRA_COPY.nav.timezone,
    description: '用户、组织和租户时区策略。',
    path: '/admin/timezone',
    icon: Clock,
    panel: <TimezonePage />,
  },
  {
    key: 'attachment',
    label: INFRA_COPY.nav.attachment,
    description: '附件策略、存储桶和上传限制。',
    path: '/admin/attachment',
    icon: Paperclip,
    panel: <AttachmentPage />,
  },
  {
    key: 'event-bus',
    label: INFRA_COPY.nav.eventBus,
    description: '事件订阅、投递状态和重试链路。',
    path: '/admin/event-bus',
    icon: Activity,
    panel: <EventBusPage />,
  },
] as const

type TabKey = (typeof TAB_ITEMS)[number]['key']

export interface InfraAdminPageProps {
  embedded?: boolean
  initialTab?: TabKey
}

function resolveInitialTab(pathname: string): TabKey {
  return (
    TAB_ITEMS.find((item) => pathname.startsWith(item.path))?.key ??
    TAB_ITEMS[0].key
  )
}

export default function InfraAdminPage({
  embedded = false,
  initialTab,
}: InfraAdminPageProps): ReactElement {
  const location = useLocation()
  const [activeTab, setActiveTab] = useState<TabKey>(
    () => initialTab ?? resolveInitialTab(location.pathname),
  )
  const activeItem = useMemo(
    () => TAB_ITEMS.find((item) => item.key === activeTab) ?? TAB_ITEMS[0],
    [activeTab],
  )

  return (
    <div className="space-y-3.5">
      {!embedded ? (
        <Card className="rounded-lg">
          <CardHeader>
            <Badge className="w-fit">{INFRA_COPY.title}</Badge>
            <CardTitle className="mt-2 text-xl">{INFRA_COPY.title}</CardTitle>
            <CardDescription>{INFRA_COPY.description}</CardDescription>
          </CardHeader>
        </Card>
      ) : null}

      <section className="rounded-lg border border-slate-200 bg-white shadow-sm">
        <div className="flex flex-col gap-1 border-b border-slate-100 px-4 py-2.5">
          <p className="text-sm font-semibold text-slate-950">系统能力分组</p>
          <p className="text-sm text-slate-500">{activeItem.description}</p>
        </div>
        <div
          aria-label="系统管理内容分组"
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
                  <span className="block font-semibold">{item.label}</span>
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
