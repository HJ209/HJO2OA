import type { PaginationQuery } from '@/types/api'
import { get, post, put } from '@/services/request'
import { useAuthStore } from '@/stores/auth-store'
import { useIdentityStore } from '@/stores/identity-store'
import { serializePaginationParams } from '@/utils/pagination'
import type {
  ChannelEndpoint,
  DeliveryTask,
  MessageCategory,
  MessageChannelType,
  MessageNotificationDetail,
  MessageNotificationSummary,
  MessagePriority,
  MessageProviderType,
  MessageReadStatus,
  MessageTemplate,
  MessageType,
  MessagingContext,
  MobileDeviceBinding,
  PushPreference,
  RenderedMessageTemplate,
  RoutingPolicy,
  SubscriptionPreference,
  SubscriptionRule,
  UnreadCount,
} from '@/features/messages/types/message'

const MESSAGE_API_PREFIX = '/v1/msg/messages'
const CHANNEL_API_PREFIX = '/v1/msg/channel-sender'
const SUBSCRIPTION_API_PREFIX = '/v1/msg/event-subscription'
const MOBILE_API_PREFIX = '/v1/msg/mobile-support'

const DEFAULT_TENANT_ID = '00000000-0000-4000-8000-000000000001'
const DEFAULT_PERSON_ID = '00000000-0000-4000-8000-000000000002'
const DEFAULT_ACCOUNT_ID = '00000000-0000-4000-8000-000000000003'

interface BackendNotificationSummary {
  notificationId: string
  title: string
  bodySummary: string
  category: string
  priority: string
  inboxStatus: string
  deliveryStatus: string
  sourceModule: string
  deepLink: string
  targetAssignmentId: string | null
  targetPositionId: string | null
  createdAt: string
}

interface BackendNotificationDetail extends BackendNotificationSummary {
  sourceEventType: string
  sourceBusinessId: string
  readAt: string | null
  archivedAt: string | null
  revokedAt: string | null
  expiredAt: string | null
  statusReason: string | null
}

interface BackendNotificationUnreadSummary {
  totalUnreadCount: number
  categoryUnreadCounts: Record<string, number>
  latestNotificationIds: string[]
}

interface BulkReadRequest {
  notificationIds: string[]
}

interface BulkReadResult {
  readCount: number
  notFoundIds: string[]
}

export interface CreateTemplateInput {
  code: string
  channelType: MessageChannelType
  locale: string
  version: number
  category: MessageCategory
  titleTemplate: string
  bodyTemplate: string
  variableSchema?: string
  systemLocked?: boolean
}

export interface CreateEndpointInput {
  endpointCode: string
  channelType: MessageChannelType
  providerType: MessageProviderType
  displayName: string
  configRef: string
  rateLimitPerMinute?: number
  dailyQuota?: number
}

export interface CreateRoutingPolicyInput {
  policyCode: string
  category: MessageCategory
  priorityThreshold: MessagePriority
  targetChannelOrder: MessageChannelType[]
  fallbackChannelOrder?: MessageChannelType[]
  quietWindowBehavior: 'DEFER' | 'BYPASS' | 'SUPPRESS'
  dedupWindowSeconds?: number
  escalationPolicy?: string
}

export interface CreateSubscriptionRuleInput {
  ruleCode: string
  eventTypePattern: string
  notificationCategory: MessageCategory
  targetResolverType: SubscriptionRule['targetResolverType']
  targetResolverConfig?: string
  templateCode: string
  conditionExpr?: string
  priorityMapping?: string
  defaultPriority: MessagePriority
  enabled: boolean
}

export interface SaveSubscriptionPreferenceInput {
  category: MessageCategory
  allowedChannels: MessageChannelType[]
  quietStartsAt?: string
  quietEndsAt?: string
  digestMode: SubscriptionPreference['digestMode']
  escalationOptIn: boolean
  muteNonWorkingHours: boolean
  enabled: boolean
}

export interface SavePushPreferenceInput {
  pushEnabled: boolean
  quietStartsAt?: string
  quietEndsAt?: string
  mutedCategories: string[]
}

export interface SendTestMessageInput {
  channelType: MessageChannelType
  endpointId: string
  target: string
  title: string
  body: string
  deepLink?: string
}

const CATEGORY_TO_TYPE: Record<string, MessageType> = {
  TODO_CREATED: 'TASK',
  TODO_OVERDUE: 'ALERT',
  COLLAB_MENTION: 'NOTICE',
  COLLAB_TASK_ASSIGNED: 'TASK',
  COLLAB_TASK_CHANGED: 'TASK',
  COLLAB_MEETING_REMINDER: 'NOTICE',
  PROCESS_TASK_OVERDUE: 'ALERT',
  ORG_ACCOUNT_LOCKED: 'SYSTEM',
  SYSTEM_SECURITY: 'SYSTEM',
  BUSINESS_NOTICE: 'NOTICE',
}

const INBOX_STATUS_TO_READ: Record<string, MessageReadStatus> = {
  UNREAD: 'UNREAD',
  READ: 'READ',
  ARCHIVED: 'READ',
  DELETED: 'READ',
}

function isUuid(value: string | null | undefined): value is string {
  return Boolean(
    value &&
    /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(
      value,
    ),
  )
}

function uniqueMutationKey(prefix: string): string {
  return `${prefix}:${Date.now()}:${Math.random().toString(16).slice(2)}`
}

export function resolveMessagingContext(): MessagingContext {
  const authState = useAuthStore.getState()
  const identityState = useIdentityStore.getState()
  const envTenantId = import.meta.env.VITE_TENANT_ID
  const tenantId = isUuid(authState.user?.tenantId)
    ? authState.user.tenantId
    : isUuid(envTenantId)
      ? envTenantId
      : DEFAULT_TENANT_ID
  const personId = isUuid(identityState.currentAssignment?.assignmentId)
    ? identityState.currentAssignment.assignmentId
    : isUuid(authState.user?.id)
      ? authState.user.id
      : DEFAULT_PERSON_ID
  const accountId = isUuid(authState.user?.id)
    ? authState.user.id
    : DEFAULT_ACCOUNT_ID

  return { tenantId, personId, accountId }
}

function toMessageReadStatus(inboxStatus: string): MessageReadStatus {
  return INBOX_STATUS_TO_READ[inboxStatus] ?? 'UNREAD'
}

function toMessageType(category: string): MessageType {
  return CATEGORY_TO_TYPE[category] ?? 'NOTICE'
}

function toMessageNotificationSummary(
  backend: BackendNotificationSummary,
): MessageNotificationSummary {
  return {
    id: backend.notificationId,
    type: toMessageType(backend.category),
    category: backend.category,
    title: backend.title,
    summary: backend.bodySummary,
    readStatus: toMessageReadStatus(backend.inboxStatus),
    inboxStatus: backend.inboxStatus,
    deliveryStatus: backend.deliveryStatus,
    sourceModule: backend.sourceModule,
    deepLink: backend.deepLink,
    createdAt: backend.createdAt,
  }
}

function toMessageNotificationDetail(
  backend: BackendNotificationDetail,
): MessageNotificationDetail {
  const deletedAt = backend.inboxStatus === 'DELETED' ? backend.revokedAt : null

  return {
    id: backend.notificationId,
    type: toMessageType(backend.category),
    category: backend.category,
    title: backend.title,
    body: backend.bodySummary,
    readStatus: toMessageReadStatus(backend.inboxStatus),
    inboxStatus: backend.inboxStatus,
    createdAt: backend.createdAt,
    readAt: backend.readAt ?? undefined,
    archivedAt: backend.archivedAt ?? undefined,
    deletedAt: deletedAt ?? undefined,
    deliveryStatus: backend.deliveryStatus,
    sourceModule: backend.sourceModule,
    sourceEventType: backend.sourceEventType,
    sourceBusinessId: backend.sourceBusinessId,
    deepLink: backend.deepLink,
    statusReason: backend.statusReason ?? undefined,
    metadata: {
      priority: backend.priority,
      deliveryStatus: backend.deliveryStatus,
      sourceModule: backend.sourceModule,
      sourceEventType: backend.sourceEventType,
      sourceBusinessId: backend.sourceBusinessId,
    },
  }
}

function toQueryString(query: PaginationQuery): string {
  const searchParams = serializePaginationParams(query)
  const queryString = searchParams.toString()

  return queryString ? `?${queryString}` : ''
}

function toParams(params: Record<string, string | number | boolean | null>) {
  const searchParams = new URLSearchParams()

  Object.entries(params).forEach(([key, value]) => {
    if (value !== null && value !== '') {
      searchParams.set(key, String(value))
    }
  })

  const queryString = searchParams.toString()

  return queryString ? `?${queryString}` : ''
}

function parseVariables(value: string): Record<string, unknown> {
  if (!value.trim()) {
    return {}
  }

  return JSON.parse(value) as Record<string, unknown>
}

export async function getMessageNotifications(
  query: PaginationQuery = {},
): Promise<MessageNotificationSummary[]> {
  const backendList = await get<BackendNotificationSummary[]>(
    `${MESSAGE_API_PREFIX}${toQueryString(query)}`,
  )

  return backendList.map(toMessageNotificationSummary)
}

export async function getMessageNotificationDetail(
  id: string,
): Promise<MessageNotificationDetail> {
  const backend = await get<BackendNotificationDetail>(
    `${MESSAGE_API_PREFIX}/${id}`,
  )

  return toMessageNotificationDetail(backend)
}

export async function markMessageAsRead(
  id: string,
): Promise<MessageNotificationSummary> {
  const backend = await post<BackendNotificationSummary, Record<string, never>>(
    `${MESSAGE_API_PREFIX}/${id}/read`,
    {},
    {
      dedupeKey: `message-read:${id}`,
      idempotencyKey: `message-read:${id}`,
    },
  )

  return toMessageNotificationSummary(backend)
}

export async function archiveMessage(
  id: string,
  reason = 'archived from portal',
): Promise<MessageNotificationSummary> {
  const backend = await post<BackendNotificationSummary, { reason: string }>(
    `${MESSAGE_API_PREFIX}/${id}/archive`,
    { reason },
    {
      dedupeKey: `message-archive:${id}`,
      idempotencyKey: uniqueMutationKey(`message-archive:${id}`),
    },
  )

  return toMessageNotificationSummary(backend)
}

export async function deleteMessage(
  id: string,
  reason = 'deleted from portal',
): Promise<MessageNotificationSummary> {
  const backend = await post<BackendNotificationSummary, { reason: string }>(
    `${MESSAGE_API_PREFIX}/${id}/delete`,
    { reason },
    {
      dedupeKey: `message-delete:${id}`,
      idempotencyKey: uniqueMutationKey(`message-delete:${id}`),
    },
  )

  return toMessageNotificationSummary(backend)
}

export async function markAllMessagesAsRead(
  ids: string[],
): Promise<BulkReadResult> {
  return post<BulkReadResult, BulkReadRequest>(
    `${MESSAGE_API_PREFIX}/bulk-read`,
    { notificationIds: ids },
    {
      dedupeKey: 'message-read-all',
      idempotencyKey: uniqueMutationKey('message-read-all'),
    },
  )
}

export async function getUnreadMessageCount(): Promise<UnreadCount> {
  const summary = await get<BackendNotificationUnreadSummary>(
    '/v1/msg/unread-summary',
  )

  return { count: summary.totalUnreadCount }
}

export function listMessageTemplates(): Promise<MessageTemplate[]> {
  const { tenantId } = resolveMessagingContext()

  return get<MessageTemplate[]>(
    `${CHANNEL_API_PREFIX}/templates${toParams({ tenantId })}`,
  )
}

export function createMessageTemplate(
  input: CreateTemplateInput,
): Promise<MessageTemplate> {
  const { tenantId } = resolveMessagingContext()

  return post<MessageTemplate, CreateTemplateInput & { tenantId: string }>(
    `${CHANNEL_API_PREFIX}/templates`,
    {
      ...input,
      systemLocked: input.systemLocked ?? false,
      tenantId,
    },
    {
      dedupeKey: `message-template:create:${input.code}:${input.channelType}:${input.locale}:${input.version}`,
      idempotencyKey: uniqueMutationKey('message-template:create'),
    },
  )
}

export function publishMessageTemplate(id: string): Promise<MessageTemplate> {
  return put<MessageTemplate, Record<string, never>>(
    `${CHANNEL_API_PREFIX}/templates/${id}/publish`,
    {},
    {
      dedupeKey: `message-template:publish:${id}`,
      idempotencyKey: uniqueMutationKey(`message-template:publish:${id}`),
    },
  )
}

export function renderMessageTemplate(
  templateCode: string,
  channelType: MessageChannelType,
  locale: string,
  variableJson: string,
): Promise<RenderedMessageTemplate> {
  const { tenantId } = resolveMessagingContext()

  return post<RenderedMessageTemplate, Record<string, unknown>>(
    `${CHANNEL_API_PREFIX}/templates/render`,
    {
      tenantId,
      templateCode,
      channelType,
      locale,
      variables: parseVariables(variableJson),
    },
    {
      dedupeKey: `message-template:render:${templateCode}:${channelType}:${locale}:${variableJson}`,
      idempotencyKey: uniqueMutationKey('message-template:render'),
    },
  )
}

export function listChannelEndpoints(): Promise<ChannelEndpoint[]> {
  const { tenantId } = resolveMessagingContext()

  return get<ChannelEndpoint[]>(
    `${CHANNEL_API_PREFIX}/endpoints${toParams({ tenantId })}`,
  )
}

export function createChannelEndpoint(
  input: CreateEndpointInput,
): Promise<ChannelEndpoint> {
  const { tenantId } = resolveMessagingContext()

  return post<ChannelEndpoint, CreateEndpointInput & { tenantId: string }>(
    `${CHANNEL_API_PREFIX}/endpoints`,
    {
      ...input,
      tenantId,
    },
    {
      dedupeKey: `message-endpoint:create:${input.endpointCode}`,
      idempotencyKey: uniqueMutationKey('message-endpoint:create'),
    },
  )
}

export function changeChannelEndpointStatus(
  id: string,
  status: ChannelEndpoint['status'],
): Promise<ChannelEndpoint> {
  return put<ChannelEndpoint, { status: ChannelEndpoint['status'] }>(
    `${CHANNEL_API_PREFIX}/endpoints/${id}/status`,
    { status },
    {
      dedupeKey: `message-endpoint:status:${id}:${status}`,
      idempotencyKey: uniqueMutationKey(`message-endpoint:status:${id}`),
    },
  )
}

export function sendTestMessage(
  input: SendTestMessageInput,
): Promise<DeliveryTask> {
  const { tenantId } = resolveMessagingContext()

  return post<DeliveryTask, SendTestMessageInput & { tenantId: string }>(
    `${CHANNEL_API_PREFIX}/endpoints/send-test`,
    {
      ...input,
      tenantId,
    },
    {
      dedupeKey: `message-endpoint:test:${input.endpointId}:${input.target}`,
      idempotencyKey: uniqueMutationKey('message-endpoint:test'),
    },
  )
}

export function listRoutingPolicies(): Promise<RoutingPolicy[]> {
  const { tenantId } = resolveMessagingContext()

  return get<RoutingPolicy[]>(
    `${CHANNEL_API_PREFIX}/routing-policies${toParams({ tenantId })}`,
  )
}

export function createRoutingPolicy(
  input: CreateRoutingPolicyInput,
): Promise<RoutingPolicy> {
  const { tenantId } = resolveMessagingContext()

  return post<RoutingPolicy, CreateRoutingPolicyInput & { tenantId: string }>(
    `${CHANNEL_API_PREFIX}/routing-policies`,
    {
      ...input,
      tenantId,
      fallbackChannelOrder: input.fallbackChannelOrder ?? ['INBOX'],
      dedupWindowSeconds: input.dedupWindowSeconds ?? 300,
    },
    {
      dedupeKey: `message-routing:create:${input.policyCode}`,
      idempotencyKey: uniqueMutationKey('message-routing:create'),
    },
  )
}

export function toggleRoutingPolicy(
  id: string,
  enabled: boolean,
): Promise<RoutingPolicy> {
  const endpoint = enabled ? 'enable' : 'disable'

  return put<RoutingPolicy, Record<string, never>>(
    `${CHANNEL_API_PREFIX}/routing-policies/${id}/${endpoint}`,
    {},
    {
      dedupeKey: `message-routing:${endpoint}:${id}`,
      idempotencyKey: uniqueMutationKey(`message-routing:${endpoint}:${id}`),
    },
  )
}

export function listRetryableDeliveryTasks(): Promise<DeliveryTask[]> {
  const { tenantId } = resolveMessagingContext()

  return get<DeliveryTask[]>(
    `${CHANNEL_API_PREFIX}/delivery-tasks/retryable${toParams({ tenantId })}`,
  )
}

export function retryDeliveryTask(id: string): Promise<DeliveryTask> {
  return post<DeliveryTask, Record<string, never>>(
    `${CHANNEL_API_PREFIX}/delivery-tasks/${id}/retry`,
    {},
    {
      dedupeKey: `message-delivery:retry:${id}`,
      idempotencyKey: uniqueMutationKey(`message-delivery:retry:${id}`),
    },
  )
}

export function listSubscriptionRules(): Promise<SubscriptionRule[]> {
  const { tenantId } = resolveMessagingContext()

  return get<SubscriptionRule[]>(
    `${SUBSCRIPTION_API_PREFIX}/admin/rules${toParams({ tenantId })}`,
  )
}

export function createSubscriptionRule(
  input: CreateSubscriptionRuleInput,
): Promise<SubscriptionRule> {
  const { tenantId } = resolveMessagingContext()

  return post<
    SubscriptionRule,
    CreateSubscriptionRuleInput & { tenantId: string }
  >(
    `${SUBSCRIPTION_API_PREFIX}/admin/rules`,
    {
      ...input,
      tenantId,
    },
    {
      dedupeKey: `message-subscription:create:${input.ruleCode}`,
      idempotencyKey: uniqueMutationKey('message-subscription:create'),
    },
  )
}

export function toggleSubscriptionRule(
  id: string,
  enabled: boolean,
): Promise<SubscriptionRule> {
  return put<SubscriptionRule, { enabled: boolean; reason: string }>(
    `${SUBSCRIPTION_API_PREFIX}/admin/rules/${id}/toggle`,
    {
      enabled,
      reason: enabled ? 'enabled from portal' : 'disabled from portal',
    },
    {
      dedupeKey: `message-subscription:toggle:${id}:${enabled}`,
      idempotencyKey: uniqueMutationKey(`message-subscription:toggle:${id}`),
    },
  )
}

export function listSubscriptionPreferences(): Promise<
  SubscriptionPreference[]
> {
  const { tenantId, personId } = resolveMessagingContext()

  return get<SubscriptionPreference[]>(
    `${SUBSCRIPTION_API_PREFIX}/preferences${toParams({ tenantId, personId })}`,
  )
}

export function saveSubscriptionPreference(
  input: SaveSubscriptionPreferenceInput,
): Promise<SubscriptionPreference> {
  const { tenantId, personId } = resolveMessagingContext()
  const quietWindow =
    input.quietStartsAt && input.quietEndsAt
      ? {
          startsAt: input.quietStartsAt,
          endsAt: input.quietEndsAt,
        }
      : null

  return put<SubscriptionPreference, Record<string, unknown>>(
    `${SUBSCRIPTION_API_PREFIX}/preferences/${input.category}`,
    {
      tenantId,
      personId,
      allowedChannels: input.allowedChannels,
      quietWindow,
      digestMode: input.digestMode,
      escalationOptIn: input.escalationOptIn,
      muteNonWorkingHours: input.muteNonWorkingHours,
      enabled: input.enabled,
    },
    {
      dedupeKey: `message-subscription-preference:${input.category}`,
      idempotencyKey: uniqueMutationKey(
        `message-subscription-preference:${input.category}`,
      ),
    },
  )
}

export function listMobileDevices(): Promise<MobileDeviceBinding[]> {
  const { tenantId, personId } = resolveMessagingContext()

  return get<MobileDeviceBinding[]>(
    `${MOBILE_API_PREFIX}/devices${toParams({ tenantId, personId })}`,
  )
}

export function getPushPreference(): Promise<PushPreference> {
  const { tenantId, personId } = resolveMessagingContext()

  return get<PushPreference>(
    `${MOBILE_API_PREFIX}/push-preferences${toParams({ tenantId, personId })}`,
  )
}

export function savePushPreference(
  input: SavePushPreferenceInput,
): Promise<PushPreference> {
  const { tenantId, personId } = resolveMessagingContext()

  return put<
    PushPreference,
    SavePushPreferenceInput & { tenantId: string; personId: string }
  >(
    `${MOBILE_API_PREFIX}/push-preferences`,
    {
      ...input,
      tenantId,
      personId,
    },
    {
      dedupeKey: 'message-push-preference:save',
      idempotencyKey: uniqueMutationKey('message-push-preference:save'),
    },
  )
}
