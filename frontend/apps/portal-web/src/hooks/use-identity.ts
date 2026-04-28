import { useIdentityStore } from '@/stores/identity-store'
import type { IdentityAssignment, IdentitySnapshot } from '@/types/domain'

export interface UseIdentityResult {
  currentAssignment: IdentityAssignment | null
  orgId: string | null
  roleIds: string[]
  refresh: (
    nextContext?: Partial<IdentitySnapshot>,
  ) => Promise<IdentitySnapshot>
  clear: () => void
}

export function useIdentity(): UseIdentityResult {
  const { currentAssignment, orgId, roleIds, refresh, clear } =
    useIdentityStore()

  return {
    currentAssignment,
    orgId,
    roleIds,
    refresh,
    clear,
  }
}
