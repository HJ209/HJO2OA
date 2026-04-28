import { get } from '@/services/request'
import type { PortalHomePageAssembly } from '@/features/portal-home/types/portal-home'

export function fetchPortalHome(): Promise<PortalHomePageAssembly> {
  return get<PortalHomePageAssembly>('/v1/portal/home')
}
