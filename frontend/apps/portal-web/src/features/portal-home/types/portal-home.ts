import type { LucideIcon } from 'lucide-react'

export type HomeWidgetType =
  | 'todo-summary'
  | 'announcement'
  | 'message-summary'
  | 'shortcut'
  | 'stats-chart'

export type HomeSectionLayout = 'one-column' | 'two-column' | 'three-column'

export interface PortalBanner {
  id: string
  title: string
  subtitle?: string
  imageUrl?: string
  actionText?: string
  actionHref?: string
  priority?: number
}

export interface TodoSummary {
  pendingCount: number
  overdueCount: number
  todayDueCount?: number
  entryHref: string
}

export interface AnnouncementItem {
  id: string
  title: string
  publishedAtUtc: string
  publisherName?: string
}

export interface AnnouncementSummary {
  totalCount: number
  latest: AnnouncementItem[]
  entryHref: string
}

export interface MessageItem {
  id: string
  title: string
  sentAtUtc: string
  senderName?: string
}

export interface MessageSummary {
  unreadCount: number
  latest: MessageItem[]
  entryHref: string
}

export interface ShortcutEntry {
  id: string
  title: string
  href: string
  icon?: PortalShortcutIconName
}

export type PortalShortcutIconName =
  | 'todo'
  | 'message'
  | 'org'
  | 'admin'
  | 'app'

export interface StatsChartCard {
  id: string
  title: string
  value: number | string
  unit?: string
  trendText?: string
}

export interface PortalSnapshot {
  banners: PortalBanner[]
  todoSummary: TodoSummary
  announcementSummary: AnnouncementSummary
  messageSummary: MessageSummary
  shortcuts: ShortcutEntry[]
  statsCards: StatsChartCard[]
}

export interface HomeWidget {
  id: string
  title: string
  type: HomeWidgetType
  order: number
}

export interface HomeSection {
  id: string
  title: string
  description?: string
  order: number
  layout?: HomeSectionLayout
  widgets: HomeWidget[]
}

export interface PortalHomePageAssembly {
  sections: HomeSection[]
}

export interface WidgetChromeAction {
  label: string
  href: string
}

export interface ShortcutIconDefinition {
  name: PortalShortcutIconName
  icon: LucideIcon
}
