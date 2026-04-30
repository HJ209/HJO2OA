import { useQuery } from '@tanstack/react-query'
import {
  timezoneService,
  type TimezoneQuery,
} from '@/features/infra-admin/services/timezone-service'

export function useTimezoneSettings(query?: TimezoneQuery) {
  return useQuery({
    queryKey: ['infra', 'timezone', query],
    queryFn: () => timezoneService.list(query),
  })
}
