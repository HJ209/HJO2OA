import { useQuery } from '@tanstack/react-query'
import {
  dataI18nService,
  type DataI18nQuery,
} from '@/features/infra-admin/services/data-i18n-service'

export function useDataI18nTranslations(query?: DataI18nQuery) {
  return useQuery({
    queryKey: ['infra', 'data-i18n', query],
    queryFn: () => dataI18nService.list(query),
  })
}
