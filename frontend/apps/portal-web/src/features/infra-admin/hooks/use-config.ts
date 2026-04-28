import { useQuery } from '@tanstack/react-query'
import { configService } from '@/features/infra-admin/services/config-service'
import type { InfraListQuery } from '@/features/infra-admin/types/infra'

export function useConfigEntries(query?: InfraListQuery) {
  return useQuery({
    queryKey: ['infra', 'config', query],
    queryFn: () => configService.list(query),
  })
}
