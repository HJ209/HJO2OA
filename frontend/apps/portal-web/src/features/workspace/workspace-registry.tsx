/* eslint-disable react-refresh/only-export-components */
import type { ComponentType, ReactElement } from 'react'
import {
  ClipboardCheck,
  FolderKanban,
  Home,
  MessageSquareMore,
  Settings,
  UsersRound,
  type LucideIcon,
} from 'lucide-react'
import InfraAdminPage from '@/features/infra-admin/pages/infra-admin-page'
import MessageCenterPage from '@/features/messages/pages/message-center-page'
import OrgPermPage from '@/features/org-perm/pages/org-perm-page'
import PortalHomePage from '@/features/portal-home/pages/portal-home-page'
import TodoCenterPage from '@/features/todo/pages/todo-center-page'

function WorkspaceDocsPage(): ReactElement {
  return <InfraAdminPage embedded initialTab="attachment" />
}

export type WorkspaceAppKey =
  | 'home'
  | 'messages'
  | 'docs'
  | 'todo'
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
  component: ComponentType
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
