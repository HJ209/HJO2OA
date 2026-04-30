import { useQuery } from '@tanstack/react-query'
import { fetchPortalHome } from '@/features/portal-home/services/portal-home-service'
import type { PortalSceneType } from '@/features/portal/types/portal'

export const portalHomeQueryKey = (sceneType: PortalSceneType) =>
  ['portal-home', sceneType] as const

export function usePortalHome(sceneType: PortalSceneType = 'HOME') {
  return useQuery({
    queryKey: portalHomeQueryKey(sceneType),
    queryFn: () => fetchPortalHome(sceneType),
  })
}
