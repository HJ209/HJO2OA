export type MessageReadStatus = 'UNREAD' | 'READ'

export type MessageType = 'SYSTEM' | 'APPROVAL' | 'NOTICE' | 'TASK' | 'ALERT'

export type MessageInboxStatus = 'UNREAD' | 'READ' | 'ARCHIVED' | 'DELETED'

export type MessageChannelType =
  | 'INBOX'
  | 'EMAIL'
  | 'SMS'
  | 'WEBHOOK'
  | 'APP_PUSH'

export type MessageProviderType =
  | 'INTERNAL'
  | 'SMTP'
  | 'WEBHOOK'
  | 'ALIYUN_SMS'
  | 'TENCENT_SMS'
  | 'WECHAT_WORK'
  | 'DINGTALK'
  | 'FCM'
  | 'APNS'

export type MessagePriority = 'LOW' | 'NORMAL' | 'HIGH' | 'URGENT'

export type MessageCategory =
  | 'TODO'
  | 'TODO_CREATED'
  | 'TODO_OVERDUE'
  | 'COLLAB_MENTION'
  | 'COLLAB_TASK_ASSIGNED'
  | 'COLLAB_TASK_CHANGED'
  | 'COLLAB_MEETING_REMINDER'
  | 'PROCESS_TASK_OVERDUE'
  | 'APPROVAL'
  | 'COMMENT'
  | 'SYSTEM'
  | 'SECURITY'
  | 'ORG_ACCOUNT_LOCKED'
  | 'SYSTEM_SECURITY'
  | 'BUSINESS_NOTICE'
  | 'GENERAL'

export interface MessageNotificationSummary {
  id: string
  type: MessageType
  category?: MessageCategory | string
  title: string
  summary: string
  readStatus: MessageReadStatus
  inboxStatus?: MessageInboxStatus | string
  deliveryStatus?: string
  sourceModule?: string
  deepLink?: string
  createdAt: string
}

export interface MessageNotificationDetail {
  id: string
  type: MessageType
  category?: MessageCategory | string
  title: string
  body: string
  readStatus: MessageReadStatus
  inboxStatus?: MessageInboxStatus | string
  createdAt: string
  readAt?: string
  archivedAt?: string
  deletedAt?: string
  deliveryStatus?: string
  sourceModule?: string
  sourceEventType?: string
  sourceBusinessId?: string
  deepLink?: string
  statusReason?: string
  metadata?: Record<string, string | number | boolean | null>
}

export interface UnreadCount {
  count: number
}

export interface MessagingContext {
  tenantId: string
  personId: string
  accountId: string
}

export interface MessageTemplate {
  id: string
  code: string
  channelType: MessageChannelType
  locale: string
  version: number
  category: MessageCategory
  titleTemplate: string
  bodyTemplate: string
  variableSchema?: string | null
  status: 'DRAFT' | 'PUBLISHED' | 'DISABLED'
  systemLocked: boolean
  tenantId?: string | null
  createdAt: string
  updatedAt: string
}

export interface RenderedMessageTemplate {
  templateId: string
  templateCode: string
  channelType: MessageChannelType
  locale: string
  version: number
  title: string
  body: string
}

export interface ChannelEndpoint {
  id: string
  endpointCode: string
  channelType: MessageChannelType
  providerType: MessageProviderType
  displayName: string
  status: 'ENABLED' | 'DISABLED' | 'DEGRADED'
  configRef: string
  rateLimitPerMinute?: number | null
  dailyQuota?: number | null
  tenantId?: string | null
  createdAt: string
  updatedAt: string
}

export interface RoutingPolicy {
  id: string
  policyCode: string
  category: MessageCategory
  priorityThreshold: MessagePriority
  targetChannelOrder: MessageChannelType[]
  fallbackChannelOrder: MessageChannelType[]
  quietWindowBehavior: 'DEFER' | 'BYPASS' | 'SUPPRESS'
  dedupWindowSeconds: number
  escalationPolicy?: string | null
  enabled: boolean
  tenantId?: string | null
  createdAt: string
  updatedAt: string
}

export interface DeliveryAttempt {
  id: string
  deliveryTaskId: string
  attemptNo: number
  requestPayloadSnapshot?: string | null
  providerResponse?: string | null
  resultStatus: 'SUCCESS' | 'FAILED' | 'TIMEOUT'
  errorCode?: string | null
  errorMessage?: string | null
  requestedAt: string
  completedAt?: string | null
}

export interface DeliveryTask {
  id: string
  notificationId: string
  channelType: MessageChannelType
  endpointId?: string | null
  routeOrder: number
  status:
    | 'PENDING'
    | 'SENDING'
    | 'DELIVERED'
    | 'FAILED'
    | 'GAVE_UP'
    | 'CANCELLED'
  retryCount: number
  nextRetryAt?: string | null
  providerMessageId?: string | null
  lastErrorCode?: string | null
  lastErrorMessage?: string | null
  deliveredAt?: string | null
  tenantId: string
  createdAt: string
  updatedAt: string
  attempts: DeliveryAttempt[]
}

export interface PushPreference {
  id: string
  tenantId: string
  personId: string
  pushEnabled: boolean
  quietStartsAt?: string | null
  quietEndsAt?: string | null
  mutedCategories: string[]
  createdAt: string
  updatedAt: string
}

export interface MobileDeviceBinding {
  id: string
  personId: string
  accountId: string
  deviceId: string
  deviceFingerprint?: string | null
  platform: 'IOS' | 'ANDROID' | 'H5' | 'PWA'
  appType: 'NATIVE_APP' | 'WECHAT_H5' | 'DINGTALK_H5' | 'ENTERPRISE_APP' | 'PWA'
  pushToken?: string | null
  bindStatus: 'ACTIVE' | 'REVOKED' | 'LOST' | 'DISABLED'
  riskLevel: 'LOW' | 'MEDIUM' | 'HIGH'
  lastLoginAt?: string | null
  lastSeenAt?: string | null
  tenantId: string
  createdAt: string
  updatedAt: string
}

export interface SubscriptionRule {
  id: string
  ruleCode: string
  eventTypePattern: string
  notificationCategory: MessageCategory
  targetResolverType:
    | 'PAYLOAD_PERSON'
    | 'PAYLOAD_ASSIGNMENT'
    | 'INITIATOR'
    | 'ROLE_MEMBERS'
    | 'ORG_MEMBERS'
    | 'EXPRESSION'
  targetResolverConfig?: string | null
  templateCode: string
  conditionExpr?: string | null
  priorityMapping?: string | null
  defaultPriority: MessagePriority
  enabled: boolean
  tenantId: string
  createdAt: string
  updatedAt: string
}

export interface SubscriptionPreference {
  id: string
  personId: string
  category: MessageCategory
  allowedChannels: MessageChannelType[]
  quietWindow?: {
    startsAt: string
    endsAt: string
  } | null
  digestMode: 'IMMEDIATE' | 'PERIODIC_DIGEST' | 'DISABLED'
  escalationOptIn: boolean
  muteNonWorkingHours: boolean
  enabled: boolean
  tenantId: string
  createdAt: string
  updatedAt: string
}
