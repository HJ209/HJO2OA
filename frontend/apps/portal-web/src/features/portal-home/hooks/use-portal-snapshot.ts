import { useQuery } from '@tanstack/react-query'
import { fetchPortalSnapshot } from '@/features/portal-home/services/portal-snapshot-service'

export const portalSnapshotQueryKey = ['portal-snapshot'] as const

export function usePortalSnapshot() {
  return useQuery({
    queryKey: portalSnapshotQueryKey,
    queryFn: fetchPortalSnapshot,
  })
}
