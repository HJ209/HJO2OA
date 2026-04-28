import { useMutation, useQueryClient } from '@tanstack/react-query'
import {
  markAllMessagesAsRead,
  markMessageAsRead,
} from '@/features/messages/services/message-service'
import { messageQueryKeys } from '@/features/messages/hooks/use-messages-query'

export function useMessageAction() {
  const queryClient = useQueryClient()

  const invalidateMessages = async (): Promise<void> => {
    await Promise.all([
      queryClient.invalidateQueries({ queryKey: messageQueryKeys.all }),
    ])
  }

  const markReadMutation = useMutation({
    mutationFn: markMessageAsRead,
    onSuccess: invalidateMessages,
  })

  const markAllReadMutation = useMutation({
    mutationFn: markAllMessagesAsRead,
    onSuccess: invalidateMessages,
  })

  return {
    markRead: markReadMutation.mutate,
    markReadAsync: markReadMutation.mutateAsync,
    markAllRead: markAllReadMutation.mutate,
    markAllReadAsync: markAllReadMutation.mutateAsync,
    isMarkingRead: markReadMutation.isPending,
    isMarkingAllRead: markAllReadMutation.isPending,
  }
}
