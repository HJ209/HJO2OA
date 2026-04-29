import { get } from '@/services/request'
import type {
  MessageItem,
  PortalSnapshot,
} from '@/features/portal-home/types/portal-home'

interface PortalCardSnapshot<TData> {
  data?: TData
  state?: string
}

interface PortalDashboardResponse {
  identity?: PortalCardSnapshot<{
    positionName?: string
    organizationName?: string
    departmentName?: string
  }>
  todo?: PortalCardSnapshot<{
    totalCount?: number
    urgentCount?: number
  }>
  message?: PortalCardSnapshot<{
    unreadCount?: number
    topItems?: Array<{
      notificationId?: string
      title?: string
      createdAt?: string
      priority?: string
    }>
  }>
}

const emptySnapshot: PortalSnapshot = {
  banners: [],
  todoSummary: {
    pendingCount: 0,
    overdueCount: 0,
    todayDueCount: 0,
    entryHref: '/todo',
  },
  announcementSummary: {
    totalCount: 0,
    latest: [],
    entryHref: '/',
  },
  messageSummary: {
    unreadCount: 0,
    latest: [],
    entryHref: '/messages',
  },
  shortcuts: [
    {
      id: 'todo',
      title: '待办中心',
      href: '/todo',
      icon: 'todo',
    },
    {
      id: 'messages',
      title: '消息中心',
      href: '/messages',
      icon: 'message',
    },
    {
      id: 'org',
      title: '组织管理',
      href: '/org/tree',
      icon: 'org',
    },
    {
      id: 'admin',
      title: '系统设置',
      href: '/admin/dictionary',
      icon: 'admin',
    },
  ],
  statsCards: [],
}

export async function fetchPortalSnapshot(): Promise<PortalSnapshot> {
  const dashboard = await get<PortalSnapshot | PortalDashboardResponse>(
    '/v1/portal/aggregation/dashboard',
  )

  if (isPortalSnapshot(dashboard)) {
    return dashboard
  }

  return mapDashboardToSnapshot(dashboard)
}

function isPortalSnapshot(value: unknown): value is PortalSnapshot {
  return Boolean(
    value &&
      typeof value === 'object' &&
      'todoSummary' in value &&
      'messageSummary' in value,
  )
}

function mapDashboardToSnapshot(
  dashboard: PortalDashboardResponse,
): PortalSnapshot {
  const todoData = dashboard.todo?.data
  const messageData = dashboard.message?.data
  const identityData = dashboard.identity?.data
  const latestMessages = (messageData?.topItems ?? []).map<MessageItem>(
    (item, index) => ({
      id: item.notificationId ?? `message-${index}`,
      title: item.title ?? '未命名消息',
      sentAtUtc: item.createdAt ?? new Date(0).toISOString(),
      senderName: item.priority,
    }),
  )

  return {
    ...emptySnapshot,
    todoSummary: {
      pendingCount: todoData?.totalCount ?? 0,
      overdueCount: 0,
      todayDueCount: todoData?.urgentCount ?? 0,
      entryHref: '/todo',
    },
    messageSummary: {
      unreadCount: messageData?.unreadCount ?? 0,
      latest: latestMessages,
      entryHref: '/messages',
    },
    statsCards: [
      {
        id: 'identity',
        title: '当前岗位',
        value: identityData?.positionName ?? '未设置',
        trendText: identityData?.organizationName ?? identityData?.departmentName,
      },
      {
        id: 'todo',
        title: '待办任务',
        value: todoData?.totalCount ?? 0,
        unit: '项',
      },
      {
        id: 'message',
        title: '未读消息',
        value: messageData?.unreadCount ?? 0,
        unit: '条',
      },
    ],
  }
}
