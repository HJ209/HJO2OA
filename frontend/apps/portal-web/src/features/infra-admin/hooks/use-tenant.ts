import { useQuery } from '@tanstack/react-query'
import { tenantService } from '@/features/infra-admin/services/tenant-service'
import type { InfraListQuery } from '@/features/infra-admin/types/infra'

export function useTenants(query?: InfraListQuery) {
  return useQuery({
    queryKey: ['infra', 'tenants', query],
    queryFn: () => tenantService.list(query),
  })
}

export function useTenantDetail(tenantId?: string) {
  return useQuery({
    enabled: Boolean(tenantId),
    queryKey: ['infra', 'tenants', tenantId],
    queryFn: () => tenantService.detail(tenantId ?? ''),
  })
}
