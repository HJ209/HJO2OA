import type { PageData, PaginationQuery } from '@/types/api'
import { get, post } from '@/services/request'
import { serializePaginationParams } from '@/utils/pagination'
import type {
  MessageNotificationDetail,
  MessageNotificationSummary,
  UnreadCount,
} from '@/features/messages/types/message'

const MESSAGE_API_PREFIX = '/v1/messages/notifications'

function toQueryString(query: PaginationQuery): string {
  const searchParams = serializePaginationParams(query)
  const queryString = searchParams.toString()

  return queryString ? `?${queryString}` : ''
}

export function getMessageNotifications(
  query: PaginationQuery = {},
): Promise<PageData<MessageNotificationSummary>> {
  return get<PageData<MessageNotificationSummary>>(
    `${MESSAGE_API_PREFIX}${toQueryString(query)}`,
  )
}

export function getMessageNotificationDetail(
  id: string,
): Promise<MessageNotificationDetail> {
  return get<MessageNotificationDetail>(`${MESSAGE_API_PREFIX}/${id}`)
}

export function markMessageAsRead(
  id: string,
): Promise<MessageNotificationSummary> {
  return post<MessageNotificationSummary, Record<string, never>>(
    `${MESSAGE_API_PREFIX}/${id}/read`,
    {},
    {
      dedupeKey: `message-read:${id}`,
      idempotencyKey: `message-read:${id}`,
    },
  )
}

export function markAllMessagesAsRead(): Promise<void> {
  return post<void, Record<string, never>>(
    `${MESSAGE_API_PREFIX}/read-all`,
    {},
    {
      dedupeKey: 'message-read-all',
      idempotencyKey: 'message-read-all',
    },
  )
}

export function getUnreadMessageCount(): Promise<UnreadCount> {
  return get<UnreadCount>(`${MESSAGE_API_PREFIX}/unread-count`)
}
