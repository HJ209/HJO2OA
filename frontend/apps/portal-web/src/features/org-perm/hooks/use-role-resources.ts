import { useQuery, type UseQueryResult } from '@tanstack/react-query'
import { getRoleResources } from '@/features/org-perm/services/role-resource-service'
import type { ResourceNode } from '@/features/org-perm/types/org-perm'

export function useRoleResources(
  roleId: string | undefined,
): UseQueryResult<ResourceNode[]> {
  return useQuery({
    enabled: Boolean(roleId),
    queryKey: ['org-perm', 'role-resources', roleId],
    queryFn: () => getRoleResources(roleId ?? ''),
  })
}
