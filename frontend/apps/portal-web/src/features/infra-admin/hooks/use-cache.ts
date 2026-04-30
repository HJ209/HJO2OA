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

export function useCacheKeys(query: {
  namespace?: string
  tenantId?: string
  keyword?: string
}) {
  return useQuery({
    queryKey: ['infra', 'cache-keys', query],
    queryFn: () => cacheService.queryKeys(query),
  })
}

export function useCacheInvalidations(query: {
  namespace?: string
  limit?: number
}) {
  return useQuery({
    queryKey: ['infra', 'cache-invalidations', query],
    queryFn: () => cacheService.listInvalidations(query),
  })
}
