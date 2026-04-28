import { useQuery } from '@tanstack/react-query'
import { timezoneService } from '@/features/infra-admin/services/timezone-service'
import type { InfraListQuery } from '@/features/infra-admin/types/infra'

export function useTimezoneSettings(query?: InfraListQuery) {
  return useQuery({
    queryKey: ['infra', 'timezone', query],
    queryFn: () => timezoneService.list(query),
  })
}
