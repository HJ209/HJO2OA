import type { PageData } from '@/types/api'

export type PortalSceneType = 'HOME' | 'OFFICE_CENTER' | 'MOBILE_WORKBENCH'
export type PortalClientType = 'PC' | 'MOBILE' | 'ALL'
export type PortalLayoutMode = 'THREE_SECTION' | 'OFFICE_SPLIT' | 'MOBILE_LIGHT'
export type PortalCardType = 'IDENTITY' | 'TODO' | 'MESSAGE'
export type PortalCardState = 'READY' | 'STALE' | 'FAILED'
export type WidgetDataSourceType =
  | 'AGGREGATION_QUERY'
  | 'STATIC_LINK'
  | 'EXTERNAL_API'
export type WidgetDefinitionStatus = 'ACTIVE' | 'DISABLED'
export type PortalPublicationStatus = 'ACTIVE' | 'OFFLINE'
export type PortalTemplateVersionStatus = 'DRAFT' | 'PUBLISHED' | 'DEPRECATED'
export type PortalAudienceType =
  | 'TENANT_DEFAULT'
  | 'ASSIGNMENT'
  | 'PERSON'
  | 'POSITION'
export type PersonalizationScope = 'ASSIGNMENT' | 'GLOBAL'
export type PersonalizationStatus = 'ACTIVE' | 'RESET' | 'LOCKED'
export type QuickAccessEntryType = 'PROCESS' | 'APP' | 'LINK' | 'CONTENT'

export interface PortalIdentityCard {
  tenantId: string
  personId: string
  accountId: string
  assignmentId: string
  positionId: string
  organizationId: string
  departmentId: string
  positionName: string
  organizationName: string
  departmentName: string
  assignmentType: string
  effectiveAt: string
}

export interface PortalTodoItem {
  todoId: string
  title: string
  category: string
  urgency: string
  dueTime?: string | null
  createdAt?: string | null
}

export interface PortalTodoCard {
  totalCount: number
  urgentCount: number
  categoryStats: Record<string, number>
  topItems: PortalTodoItem[]
}

export interface PortalMessageItem {
  notificationId: string
  title: string
  category: string
  priority: string
  deepLink?: string | null
  createdAt?: string | null
}

export interface PortalMessageCard {
  unreadCount: number
  categoryStats: Record<string, number>
  topItems: PortalMessageItem[]
}

export type PortalCardData =
  | PortalIdentityCard
  | PortalTodoCard
  | PortalMessageCard
  | Record<string, unknown>
  | null

export interface PortalHomeCardView {
  cardCode: string
  cardType: PortalCardType
  title: string
  description: string
  actionLink: string
  state: PortalCardState
  message?: string | null
  data: PortalCardData
}

export interface PortalHomeRegionView {
  regionCode: string
  title: string
  description: string
  cards: PortalHomeCardView[]
}

export interface PortalHomeBranding {
  title: string
  subtitle: string
  logoText: string
}

export interface PortalHomeNavigationItem {
  code: string
  title: string
  badgeCount?: number | null
  actionLink: string
}

export interface PortalHomeRefreshState {
  sceneType: PortalSceneType
  status: 'IDLE' | 'REFRESHING' | 'STALE' | 'FAILED'
  triggerEvent?: string | null
  cardType?: string | null
  message?: string | null
  updatedAt: string
}

export interface PortalHomeFooter {
  text: string
}

export interface PortalHomeSourceTemplateMetadata {
  publicationId: string
  templateId: string
  templateCode: string
  templateDisplayName: string
  sceneType: PortalSceneType
  clientType: PortalClientType
}

export interface PortalHomePageView {
  sceneType: PortalSceneType
  layoutType: PortalLayoutMode
  branding: PortalHomeBranding
  navigation: PortalHomeNavigationItem[]
  regions: PortalHomeRegionView[]
  footer: PortalHomeFooter
  refreshState: PortalHomeRefreshState
  sourceTemplateMetadata?: PortalHomeSourceTemplateMetadata | null
  assembledAt: string
}

export interface WidgetDefinitionView {
  widgetId: string
  tenantId: string
  widgetCode: string
  displayName: string
  cardType: PortalCardType
  sceneType?: PortalSceneType | null
  sourceModule: string
  dataSourceType: WidgetDataSourceType
  allowHide: boolean
  allowCollapse: boolean
  maxItems: number
  status: WidgetDefinitionStatus
  createdAt: string
  updatedAt: string
}

export interface UpsertWidgetDefinitionRequest {
  widgetCode: string
  displayName: string
  cardType: PortalCardType
  sceneType?: PortalSceneType | null
  sourceModule: string
  dataSourceType: WidgetDataSourceType
  allowHide: boolean
  allowCollapse: boolean
  maxItems: number
}

export interface PortalWidgetPlacementView {
  placementId: string
  placementCode: string
  widgetCode: string
  cardType: PortalCardType
  orderNo: number
  hiddenByDefault: boolean
  collapsedByDefault: boolean
  overrideProps: Record<string, string>
}

export interface PortalLayoutRegionView {
  regionId: string
  regionCode: string
  title: string
  required: boolean
  placements: PortalWidgetPlacementView[]
}

export interface PortalPageView {
  pageId: string
  pageCode: string
  title: string
  defaultPage: boolean
  layoutMode: PortalLayoutMode
  regions: PortalLayoutRegionView[]
}

export interface PortalTemplateCanvasView {
  templateId: string
  templateCode: string
  sceneType: PortalSceneType
  latestVersionNo?: number | null
  publishedVersionNo?: number | null
  pages: PortalPageView[]
}

export interface PortalTemplateVersionView {
  versionNo: number
  status: PortalTemplateVersionStatus
  createdAt: string
  publishedAt?: string | null
  deprecatedAt?: string | null
}

export interface PortalTemplateView {
  templateId: string
  tenantId: string
  templateCode: string
  displayName: string
  sceneType: PortalSceneType
  latestVersionNo?: number | null
  publishedVersionNo?: number | null
  createdAt: string
  updatedAt: string
  versions: PortalTemplateVersionView[]
}

export interface CreatePortalTemplateRequest {
  templateId: string
  templateCode: string
  displayName: string
  sceneType: PortalSceneType
}

export interface SavePortalTemplateCanvasRequest {
  pages: PortalPageView[]
}

export interface PortalPublicationAudience {
  type: PortalAudienceType
  subjectId?: string | null
}

export interface PortalPublicationView {
  publicationId: string
  tenantId: string
  templateId: string
  sceneType: PortalSceneType
  clientType: PortalClientType
  audience: PortalPublicationAudience
  status: PortalPublicationStatus
  createdAt: string
  updatedAt: string
  activatedAt?: string | null
  offlinedAt?: string | null
}

export interface ActivatePortalPublicationRequest {
  templateId: string
  sceneType: PortalSceneType
  clientType: PortalClientType
  assignmentId?: string
  positionId?: string
  personId?: string
}

export interface QuickAccessEntry {
  entryType: QuickAccessEntryType
  targetCode: string
  targetLink?: string | null
  icon?: string | null
  sortOrder: number
  pinned: boolean
}

export interface PersonalizationProfileView {
  profileId?: string | null
  tenantId: string
  personId: string
  assignmentId?: string | null
  sceneType: PortalSceneType
  resolvedScope: PersonalizationScope
  basePublicationId: string
  themeCode?: string | null
  widgetOrderOverride: string[]
  hiddenPlacementCodes: string[]
  quickAccessEntries: QuickAccessEntry[]
  status: PersonalizationStatus
  lastResolvedAt: string
  createdAt?: string | null
  updatedAt?: string | null
}

export interface SavePersonalizationProfileRequest {
  sceneType: PortalSceneType
  scope: PersonalizationScope
  assignmentId?: string
  themeCode?: string
  widgetOrderOverride: string[]
  hiddenPlacementCodes: string[]
  quickAccessEntries: QuickAccessEntry[]
}

export interface ResetPersonalizationProfileRequest {
  sceneType: PortalSceneType
  scope: PersonalizationScope
  assignmentId?: string
}

export type PortalDesignerTemplateVersionView = PortalTemplateVersionView

export interface PortalDesignerTemplateStatusView {
  templateId: string
  templateCode: string
  sceneType: PortalSceneType
  latestVersionNo?: number | null
  publishedVersionNo?: number | null
  versions: PortalDesignerTemplateVersionView[]
  activePublicationIds: string[]
  hasActivePublication: boolean
  createdAt: string
  updatedAt: string
}

export interface PortalDesignerWidgetPaletteItemView {
  widgetId: string
  widgetCode: string
  displayName: string
  cardType: PortalCardType
  sceneType?: PortalSceneType | null
  sourceModule: string
  dataSourceType: WidgetDataSourceType
  allowHide: boolean
  allowCollapse: boolean
  maxItems: number
  status: WidgetDefinitionStatus
}

export interface PortalDesignerTemplateWidgetPaletteView {
  templateId: string
  templateCode: string
  sceneType: PortalSceneType
  widgets: PortalDesignerWidgetPaletteItemView[]
}

export interface PortalDesignerTemplateInitializationView {
  templateId: string
  templateCode: string
  sceneType: PortalSceneType
  status: PortalDesignerTemplateStatusView
  canvas: PortalTemplateCanvasView
  widgetPalette: PortalDesignerTemplateWidgetPaletteView
}

export interface PortalDesignerPreviewOverlayView {
  status: 'applied' | 'bypassed' | string
  baselinePublicationId?: string | null
  resolvedLivePublicationId?: string | null
  reason: string
}

export interface PortalDesignerPreviewIdentityView {
  tenantId: string
  personId: string
  accountId: string
  assignmentId: string
  positionId: string
}

export interface PortalDesignerTemplatePreviewView {
  templateId: string
  templateCode: string
  templateDisplayName: string
  sceneType: PortalSceneType
  clientType: PortalClientType
  latestVersionNo?: number | null
  publishedVersionNo?: number | null
  previewIdentity: PortalDesignerPreviewIdentityView
  overlay: PortalDesignerPreviewOverlayView
  page: PortalHomePageView
  previewedAt: string
}

export interface PortalDesignerTemplatePublicationView {
  publicationId: string
  templateId: string
  sceneType: PortalSceneType
  clientType: PortalClientType
  status: PortalPublicationStatus
  createdAt: string
  updatedAt: string
  activatedAt?: string | null
  offlinedAt?: string | null
}

export interface PortalMessageListItem {
  notificationId: string
  title: string
  bodySummary: string
  category: string
  priority: string
  inboxStatus: string
  deliveryStatus: string
  sourceModule: string
  deepLink?: string | null
  targetAssignmentId?: string | null
  targetPositionId?: string | null
  createdAt: string
}

export interface PortalTodoListItem {
  todoId: string
  taskId: string
  instanceId: string
  title: string
  category: string
  urgency: string
  viewType: string
  status: string
  dueTime?: string | null
  overdueAt?: string | null
  createdAt?: string | null
  updatedAt?: string | null
  completedAt?: string | null
  readAt?: string | null
}

export interface PortalMessageListView {
  unreadSummary: {
    totalUnreadCount: number
    categoryUnreadCounts: Record<string, number>
    latestNotificationIds: string[]
  }
  messages: PageData<PortalMessageListItem>
  generatedAt: string
}

export interface PortalTodoListView {
  viewType: string
  summary: {
    pendingCount: number
    completedCount: number
    overdueCount: number
    copiedUnreadCount: number
  }
  todos: PageData<PortalTodoListItem>
  generatedAt: string
}
