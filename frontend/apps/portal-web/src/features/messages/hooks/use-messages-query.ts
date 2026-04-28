import { useQuery } from '@tanstack/react-query'
import type { PageData, PaginationFilter, PaginationQuery } from '@/types/api'
import {
  getMessageNotificationDetail,
  getMessageNotifications,
} from '@/features/messages/services/message-service'
import { useMessageStore } from '@/features/messages/stores/message-store'
import type {
  MessageNotificationDetail,
  MessageNotificationSummary,
} from '@/features/messages/types/message'

export const messageQueryKeys = {
  all: ['messages'] as const,
  lists: () => [...messageQueryKeys.all, 'list'] as const,
  list: (query: PaginationQuery) =>
    [...messageQueryKeys.lists(), query] as const,
  details: () => [...messageQueryKeys.all, 'detail'] as const,
  detail: (id: string | null) => [...messageQueryKeys.details(), id] as const,
  unreadCount: () => [...messageQueryKeys.all, 'unread-count'] as const,
}

function buildFilterQuery(page = 1, size = 20): PaginationQuery {
  const { readStatus, type } = useMessageStore.getState()
  const filters: PaginationFilter[] = []

  if (type !== 'ALL') {
    filters.push({ field: 'type', value: type })
  }

  if (readStatus !== 'ALL') {
    filters.push({ field: 'readStatus', value: readStatus })
  }

  return {
    page,
    size,
    filters,
    sort: [{ field: 'createdAt', direction: 'desc' }],
  }
}

export function useMessagesQuery(page = 1, size = 20) {
  const type = useMessageStore((state) => state.type)
  const readStatus = useMessageStore((state) => state.readStatus)
  const query = buildFilterQuery(page, size)

  return useQuery<PageData<MessageNotificationSummary>>({
    queryKey: messageQueryKeys.list(query),
    queryFn: () => getMessageNotifications(query),
    placeholderData: (previousData) => previousData,
    staleTime: 15000,
    enabled: Boolean(type && readStatus),
  })
}

export function useMessageDetailQuery(messageId: string | null) {
  return useQuery<MessageNotificationDetail>({
    queryKey: messageQueryKeys.detail(messageId),
    queryFn: () => getMessageNotificationDetail(messageId ?? ''),
    enabled: Boolean(messageId),
    staleTime: 15000,
  })
}
