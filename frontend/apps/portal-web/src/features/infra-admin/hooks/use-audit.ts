import { useQuery } from '@tanstack/react-query'
import {
  auditService,
  type AuditQuery,
} from '@/features/infra-admin/services/audit-service'

export function useAuditRecords(query?: AuditQuery) {
  return useQuery({
    queryKey: ['infra', 'audit', query],
    queryFn: () => auditService.list(query),
  })
}

export function useAuditRecordDetail(recordId?: string) {
  return useQuery({
    enabled: Boolean(recordId),
    queryKey: ['infra', 'audit', 'detail', recordId],
    queryFn: () => auditService.detail(recordId ?? ''),
  })
}
