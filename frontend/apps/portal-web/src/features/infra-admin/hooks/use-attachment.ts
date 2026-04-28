import { useQuery } from '@tanstack/react-query'
import { attachmentService } from '@/features/infra-admin/services/attachment-service'
import type { InfraListQuery } from '@/features/infra-admin/types/infra'

export function useAttachmentAssets(query?: InfraListQuery) {
  return useQuery({
    queryKey: ['infra', 'attachments', query],
    queryFn: () => attachmentService.list(query),
  })
}
