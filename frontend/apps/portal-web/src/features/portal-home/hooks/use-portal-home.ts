import { useQuery } from '@tanstack/react-query'
import { fetchPortalHome } from '@/features/portal-home/services/portal-home-service'

export const portalHomeQueryKey = ['portal-home'] as const

export function usePortalHome() {
  return useQuery({
    queryKey: portalHomeQueryKey,
    queryFn: fetchPortalHome,
  })
}
