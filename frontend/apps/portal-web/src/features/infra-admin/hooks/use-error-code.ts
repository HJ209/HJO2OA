import { useQuery } from '@tanstack/react-query'
import {
  errorCodeService,
  type ErrorCodeQuery,
} from '@/features/infra-admin/services/error-code-service'

export function useErrorCodes(query?: ErrorCodeQuery) {
  return useQuery({
    queryKey: ['infra', 'error-codes', query],
    queryFn: () => errorCodeService.list(query),
  })
}
