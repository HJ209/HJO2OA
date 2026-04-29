import { get, post } from '@/services/request'
import { toPageData } from '@/features/infra-admin/services/service-utils'
import type {
  CachePolicy,
  CacheStats,
  InfraListQuery,
  InfraPageData,
} from '@/features/infra-admin/types/infra'

const POLICY_URL = '/v1/infra/cache/policies'

export const cacheService = {
  async listPolicies(
    query?: InfraListQuery,
  ): Promise<InfraPageData<CachePolicy>> {
    const items = await get<
      Array<{
        namespace: string
        ttlSeconds: number
        maxCapacity?: number | null
        active: boolean
      }>
    >(POLICY_URL)

    return toPageData(
      items.map((item) => ({
        name: item.namespace,
        ttlSeconds: item.ttlSeconds,
        maxEntries: item.maxCapacity ?? 0,
        enabled: item.active,
      })),
      query,
    )
  },
  createPolicy(payload: CachePolicy): Promise<CachePolicy> {
    return post(POLICY_URL, payload, {
      dedupeKey: `cache-policy:create:${payload.name}`,
    })
  },
  async getStats(): Promise<CacheStats[]> {
    return []
  },
}
