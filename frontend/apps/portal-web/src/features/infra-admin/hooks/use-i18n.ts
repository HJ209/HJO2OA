import { useQuery } from '@tanstack/react-query'
import {
  i18nService,
  type I18nBundleQuery,
} from '@/features/infra-admin/services/i18n-service'

export function useI18nResources(query?: I18nBundleQuery) {
  return useQuery({
    queryKey: ['infra', 'i18n', query],
    queryFn: () => i18nService.list(query),
  })
}
