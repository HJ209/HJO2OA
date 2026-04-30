import type {
  PortalHomePageView,
  PortalSceneType,
} from '@/features/portal/types/portal'
import { fetchPortalHomePage } from '@/features/portal/services/portal-service'

export function fetchPortalHome(
  sceneType: PortalSceneType = 'HOME',
): Promise<PortalHomePageView> {
  return fetchPortalHomePage(sceneType)
}
