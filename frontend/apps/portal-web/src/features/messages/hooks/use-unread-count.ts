import { useQuery } from '@tanstack/react-query'
import { getUnreadMessageCount } from '@/features/messages/services/message-service'
import { messageQueryKeys } from '@/features/messages/hooks/use-messages-query'
import type { UnreadCount } from '@/features/messages/types/message'

const UNREAD_COUNT_REFETCH_INTERVAL = 30000

export function useUnreadCount() {
  return useQuery<UnreadCount>({
    queryKey: messageQueryKeys.unreadCount(),
    queryFn: getUnreadMessageCount,
    refetchInterval: UNREAD_COUNT_REFETCH_INTERVAL,
    staleTime: UNREAD_COUNT_REFETCH_INTERVAL,
  })
}
