import { useQuery } from '@tanstack/react-query'
import { auditService } from '@/features/infra-admin/services/audit-service'
import type { InfraListQuery } from '@/features/infra-admin/types/infra'

export function useAuditRecords(query?: InfraListQuery) {
  return useQuery({
    queryKey: ['infra', 'audit', query],
    queryFn: () => auditService.list(query),
  })
}
