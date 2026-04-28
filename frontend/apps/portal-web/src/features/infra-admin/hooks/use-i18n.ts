import { useQuery } from '@tanstack/react-query'
import { i18nService } from '@/features/infra-admin/services/i18n-service'
import type { InfraListQuery } from '@/features/infra-admin/types/infra'

export function useI18nResources(query?: InfraListQuery) {
  return useQuery({
    queryKey: ['infra', 'i18n', query],
    queryFn: () => i18nService.list(query),
  })
}
