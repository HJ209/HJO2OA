import { useQuery } from '@tanstack/react-query'
import { dataI18nService } from '@/features/infra-admin/services/data-i18n-service'
import type { InfraListQuery } from '@/features/infra-admin/types/infra'

export function useDataI18nTranslations(query?: InfraListQuery) {
  return useQuery({
    queryKey: ['infra', 'data-i18n', query],
    queryFn: () => dataI18nService.list(query),
  })
}
