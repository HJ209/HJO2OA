import type { PaginationQuery } from '@/types/api'
import { get, post } from '@/services/request'
import { serializePaginationParams } from '@/utils/pagination'
import type {
  MessageNotificationDetail,
  MessageNotificationSummary,
  MessageReadStatus,
  MessageType,
  UnreadCount,
} from '@/features/messages/types/message'

const MESSAGE_API_PREFIX = '/v1/msg/messages'

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

interface BackendNotificationDetail {
  notificationId: string
  title: string
  bodySummary: string
  category: string
  priority: string
  inboxStatus: string
  deliveryStatus: string
  sourceModule: string
  sourceEventType: string
  sourceBusinessId: string
  deepLink: string
  targetAssignmentId: string | null
  targetPositionId: string | null
  createdAt: string
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

const CATEGORY_TO_TYPE: Record<string, MessageType> = {
  TODO_CREATED: 'TASK',
  TODO_OVERDUE: 'ALERT',
  PROCESS_TASK_OVERDUE: 'ALERT',
  ORG_ACCOUNT_LOCKED: 'SYSTEM',
  SYSTEM_SECURITY: 'SYSTEM',
}

const INBOX_STATUS_TO_READ: Record<string, MessageReadStatus> = {
  UNREAD: 'UNREAD',
  READ: 'READ',
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
    title: backend.title,
    summary: backend.bodySummary,
    readStatus: toMessageReadStatus(backend.inboxStatus),
    createdAt: backend.createdAt,
  }
}

function toMessageNotificationDetail(
  backend: BackendNotificationDetail,
): MessageNotificationDetail {
  return {
    id: backend.notificationId,
    type: toMessageType(backend.category),
    title: backend.title,
    body: backend.bodySummary,
    readStatus: toMessageReadStatus(backend.inboxStatus),
    createdAt: backend.createdAt,
    readAt: backend.readAt ?? undefined,
  }
}

function toQueryString(query: PaginationQuery): string {
  const searchParams = serializePaginationParams(query)
  const queryString = searchParams.toString()

  return queryString ? `?${queryString}` : ''
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

export async function markAllMessagesAsRead(
  ids: string[],
): Promise<BulkReadResult> {
  return post<BulkReadResult, BulkReadRequest>(
    `${MESSAGE_API_PREFIX}/bulk-read`,
    { notificationIds: ids },
    {
      dedupeKey: 'message-read-all',
      idempotencyKey: 'message-read-all',
    },
  )
}

export async function getUnreadMessageCount(): Promise<UnreadCount> {
  const summary = await get<BackendNotificationUnreadSummary>(
    '/v1/msg/unread-summary',
  )

  return { count: summary.totalUnreadCount }
}
