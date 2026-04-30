/* eslint-disable react-refresh/only-export-components */
import { lazy, type ComponentType, type LazyExoticComponent } from 'react'
import {
  ClipboardCheck,
  DatabaseZap,
  FileText,
  FolderKanban,
  Handshake,
  Home,
  MessageSquareMore,
  PanelsTopLeft,
  Settings,
  UsersRound,
  Workflow,
  type LucideIcon,
} from 'lucide-react'
const CollaborationPage = lazy(
  () => import('@/features/collaboration/pages/collaboration-page'),
)
const ContentManagementPage = lazy(
  () => import('@/features/content/pages/content-management-page'),
)
const DataServicesPage = lazy(
  () => import('@/features/data-services/pages/data-services-page'),
)
const InfraAdminPage = lazy(
  () => import('@/features/infra-admin/pages/infra-admin-page'),
)
const MessageCenterPage = lazy(
  () => import('@/features/messages/pages/message-center-page'),
)
const OrgPermPage = lazy(
  () => import('@/features/org-perm/pages/org-perm-page'),
)
const PortalAdminPage = lazy(
  () => import('@/features/portal/pages/portal-admin-page'),
)
const PortalHomePage = lazy(
  () => import('@/features/portal-home/pages/portal-home-page'),
)
const TodoCenterPage = lazy(
  () => import('@/features/todo/pages/todo-center-page'),
)
const WorkflowPage = lazy(
  () => import('@/features/workflow/pages/workflow-page'),
)
const WorkspaceDocsPage = lazy(() =>
  import('@/features/infra-admin/pages/infra-admin-page').then((module) => ({
    default: function WorkspaceDocsPage() {
      const EmbeddedInfraAdminPage = module.default

      return <EmbeddedInfraAdminPage embedded initialTab="attachment" />
    },
  })),
)

type WorkspaceAppComponent =
  | ComponentType
  | LazyExoticComponent<ComponentType<Record<string, never>>>

export type WorkspaceAppKey =
  | 'home'
  | 'messages'
  | 'collaboration'
  | 'content'
  | 'docs'
  | 'todo'
  | 'workflow'
  | 'portal'
  | 'data'
  | 'org'
  | 'admin'

export interface WorkspaceAppDefinition {
  key: WorkspaceAppKey
  railLabel: string
  menuLabel: string
  windowTitle: string
  description: string
  defaultPath: string
  paths: string[]
  icon: LucideIcon
  sidebarItems: Array<{
    label: string
    path: string
  }>
  component: WorkspaceAppComponent
}

export const WORKSPACE_APPS: WorkspaceAppDefinition[] = [
  {
    key: 'home',
    railLabel: '首页',
    menuLabel: '首页',
    windowTitle: '首页',
    description: '门户应用、公告、待办和运营看板',
    defaultPath: '/',
    paths: ['/'],
    icon: Home,
    sidebarItems: [
      {
        label: '首页',
        path: '/',
      },
    ],
    component: PortalHomePage,
  },
  {
    key: 'messages',
    railLabel: '消息',
    menuLabel: '信息发布',
    windowTitle: '信息发布',
    description: '系统通知、审批提醒和业务播报',
    defaultPath: '/messages',
    paths: ['/messages'],
    icon: MessageSquareMore,
    sidebarItems: [
      {
        label: '消息中心',
        path: '/messages',
      },
    ],
    component: MessageCenterPage,
  },
  {
    key: 'collaboration',
    railLabel: '协同',
    menuLabel: '团队协同',
    windowTitle: '团队协同',
    description: '空间、讨论、评论、任务、会议、通知和审计',
    defaultPath: '/collaboration',
    paths: ['/collaboration'],
    icon: Handshake,
    sidebarItems: [
      {
        label: '协同工作台',
        path: '/collaboration',
      },
    ],
    component: CollaborationPage,
  },
  {
    key: 'content',
    railLabel: 'Content',
    menuLabel: 'Content management',
    windowTitle: 'Content management',
    description:
      'Categories, articles, publishing, approval, versions and portal content feed',
    defaultPath: '/content/articles',
    paths: ['/content'],
    icon: FileText,
    sidebarItems: [
      {
        label: 'Articles',
        path: '/content/articles',
      },
      {
        label: 'Categories',
        path: '/content/categories',
      },
      {
        label: 'Versions',
        path: '/content/versions',
      },
    ],
    component: ContentManagementPage,
  },
  {
    key: 'docs',
    railLabel: '文档',
    menuLabel: '文件管理',
    windowTitle: '文件管理',
    description: '常用资料和附件能力',
    defaultPath: '/docs',
    paths: ['/docs'],
    icon: FolderKanban,
    sidebarItems: [
      {
        label: '附件资产',
        path: '/docs',
      },
    ],
    component: WorkspaceDocsPage,
  },
  {
    key: 'todo',
    railLabel: '审批',
    menuLabel: '审批流程',
    windowTitle: '流程应用管理',
    description: '待办、已办、逾期和抄送',
    defaultPath: '/todo',
    paths: ['/todo'],
    icon: ClipboardCheck,
    sidebarItems: [
      {
        label: '待办中心',
        path: '/todo',
      },
    ],
    component: TodoCenterPage,
  },
  {
    key: 'workflow',
    railLabel: '流程',
    menuLabel: '流程引擎',
    windowTitle: '流程引擎',
    description: '流程定义、发布、发起、动作和轨迹',
    defaultPath: '/workflow',
    paths: ['/workflow'],
    icon: Workflow,
    sidebarItems: [
      {
        label: '流程引擎',
        path: '/workflow',
      },
      {
        label: '表单设计',
        path: '/workflow/forms',
      },
      {
        label: '表单渲染',
        path: '/workflow/render',
      },
      {
        label: '流程监控',
        path: '/workflow/monitor',
      },
    ],
    component: WorkflowPage,
  },
  {
    key: 'portal',
    railLabel: '门户',
    menuLabel: '门户配置',
    windowTitle: '门户配置',
    description: '门户模型、组件配置、个性化和设计器',
    defaultPath: '/portal',
    paths: ['/portal'],
    icon: PanelsTopLeft,
    sidebarItems: [
      {
        label: '门户配置',
        path: '/portal',
      },
    ],
    component: PortalAdminPage,
  },
  {
    key: 'data',
    railLabel: '数据',
    menuLabel: '数据服务',
    windowTitle: '数据服务',
    description: '连接器、同步、服务、开放接口、报表和治理',
    defaultPath: '/data',
    paths: ['/data'],
    icon: DatabaseZap,
    sidebarItems: [
      {
        label: '数据服务工作台',
        path: '/data',
      },
    ],
    component: DataServicesPage,
  },
  {
    key: 'org',
    railLabel: '组织',
    menuLabel: '工作管理',
    windowTitle: '组织管理',
    description: '组织、人员、岗位和角色权限',
    defaultPath: '/org/tree',
    paths: ['/org'],
    icon: UsersRound,
    sidebarItems: [
      {
        label: '组织架构',
        path: '/org/tree',
      },
      {
        label: '人员账号',
        path: '/org/persons',
      },
      {
        label: '岗位任职',
        path: '/org/positions',
      },
      {
        label: '角色授权',
        path: '/org/roles',
      },
      {
        label: '数据权限',
        path: '/org/data-permission',
      },
      {
        label: '同步审计',
        path: '/org/sync-audit',
      },
    ],
    component: OrgPermPage,
  },
  {
    key: 'admin',
    railLabel: '系统',
    menuLabel: '系统设置',
    windowTitle: '系统管理',
    description: '配置、字典、审计和运行治理',
    defaultPath: '/admin/dictionary',
    paths: ['/admin'],
    icon: Settings,
    sidebarItems: [
      {
        label: '字典',
        path: '/admin/dictionary',
      },
      {
        label: '配置',
        path: '/admin/config',
      },
      {
        label: '错误码',
        path: '/admin/error-codes',
      },
      {
        label: '缓存',
        path: '/admin/cache',
      },
      {
        label: '审计',
        path: '/admin/audit',
      },
      {
        label: '租户',
        path: '/admin/tenant',
      },
      {
        label: '安全',
        path: '/admin/security',
      },
      {
        label: '调度',
        path: '/admin/scheduler',
      },
      {
        label: 'i18n',
        path: '/admin/i18n',
      },
      {
        label: '数据 i18n',
        path: '/admin/data-i18n',
      },
      {
        label: '时区',
        path: '/admin/timezone',
      },
      {
        label: '附件',
        path: '/admin/attachment',
      },
      {
        label: '事件总线',
        path: '/admin/event-bus',
      },
    ],
    component: InfraAdminPage,
  },
] as const

export function getWorkspaceAppByKey(
  key: WorkspaceAppKey,
): WorkspaceAppDefinition {
  return WORKSPACE_APPS.find((item) => item.key === key) ?? WORKSPACE_APPS[0]
}

export function getDefaultWorkspacePath(key: WorkspaceAppKey): string {
  return getWorkspaceAppByKey(key).defaultPath
}

export function resolveWorkspaceAppKey(pathname: string): WorkspaceAppKey {
  if (pathname === '/') {
    return 'home'
  }

  return (
    WORKSPACE_APPS.filter((item) => item.key !== 'home').find((item) =>
      item.paths.some((path) => pathname.startsWith(path)),
    )?.key ?? 'home'
  )
}
