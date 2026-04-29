import { get } from '@/services/request'
import type { PortalSnapshot } from '@/features/portal-home/types/portal-home'

export function fetchPortalSnapshot(): Promise<PortalSnapshot> {
  return get<PortalSnapshot>('/v1/portal/aggregation/dashboard')
}
