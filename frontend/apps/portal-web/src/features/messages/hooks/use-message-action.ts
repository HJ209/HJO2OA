import { useMutation, useQueryClient } from '@tanstack/react-query'
import {
  archiveMessage,
  deleteMessage,
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
    mutationFn: (ids: string[]) => markAllMessagesAsRead(ids),
    onSuccess: invalidateMessages,
  })

  const archiveMutation = useMutation({
    mutationFn: (id: string) => archiveMessage(id),
    onSuccess: invalidateMessages,
  })

  const deleteMutation = useMutation({
    mutationFn: (id: string) => deleteMessage(id),
    onSuccess: invalidateMessages,
  })

  return {
    markRead: markReadMutation.mutate,
    markReadAsync: markReadMutation.mutateAsync,
    markAllRead: markAllReadMutation.mutate,
    markAllReadAsync: markAllReadMutation.mutateAsync,
    archiveMessage: archiveMutation.mutate,
    archiveMessageAsync: archiveMutation.mutateAsync,
    deleteMessage: deleteMutation.mutate,
    deleteMessageAsync: deleteMutation.mutateAsync,
    isMarkingRead: markReadMutation.isPending,
    isMarkingAllRead: markAllReadMutation.isPending,
    isArchiving: archiveMutation.isPending,
    isDeleting: deleteMutation.isPending,
  }
}
