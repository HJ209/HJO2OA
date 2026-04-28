import { useQuery } from '@tanstack/react-query'
import { securityService } from '@/features/infra-admin/services/security-service'
import type { InfraListQuery } from '@/features/infra-admin/types/infra'

export function useSecurityPolicies(query?: InfraListQuery) {
  return useQuery({
    queryKey: ['infra', 'security', query],
    queryFn: () => securityService.list(query),
  })
}
