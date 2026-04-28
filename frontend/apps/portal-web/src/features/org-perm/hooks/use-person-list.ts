import { useQuery, type UseQueryResult } from '@tanstack/react-query'
import type { PageData } from '@/types/api'
import { listPersonAccounts } from '@/features/org-perm/services/person-account-service'
import type {
  ListQuery,
  PersonAccount,
} from '@/features/org-perm/types/org-perm'

export function usePersonList(
  query: ListQuery,
): UseQueryResult<PageData<PersonAccount>> {
  return useQuery({
    queryKey: ['org-perm', 'persons', query],
    queryFn: () => listPersonAccounts(query),
  })
}
