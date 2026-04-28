import { useQuery, type UseQueryResult } from '@tanstack/react-query'
import { listOrgStructures } from '@/features/org-perm/services/org-structure-service'
import type { OrgStructure } from '@/features/org-perm/types/org-perm'

export const ORG_TREE_QUERY_KEY = ['org-perm', 'org-tree'] as const

export function useOrgTree(): UseQueryResult<OrgStructure[]> {
  return useQuery({
    queryKey: ORG_TREE_QUERY_KEY,
    queryFn: listOrgStructures,
  })
}
