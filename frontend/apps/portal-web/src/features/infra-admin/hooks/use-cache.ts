import { useQuery } from '@tanstack/react-query'
import { cacheService } from '@/features/infra-admin/services/cache-service'
import type { InfraListQuery } from '@/features/infra-admin/types/infra'

export function useCachePolicies(query?: InfraListQuery) {
  return useQuery({
    queryKey: ['infra', 'cache-policies', query],
    queryFn: () => cacheService.listPolicies(query),
  })
}

export function useCacheStats() {
  return useQuery({
    queryKey: ['infra', 'cache-stats'],
    queryFn: () => cacheService.getStats(),
  })
}
