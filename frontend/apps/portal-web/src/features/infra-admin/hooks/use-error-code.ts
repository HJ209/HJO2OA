import { useQuery } from '@tanstack/react-query'
import { errorCodeService } from '@/features/infra-admin/services/error-code-service'
import type { InfraListQuery } from '@/features/infra-admin/types/infra'

export function useErrorCodes(query?: InfraListQuery) {
  return useQuery({
    queryKey: ['infra', 'error-codes', query],
    queryFn: () => errorCodeService.list(query),
  })
}
