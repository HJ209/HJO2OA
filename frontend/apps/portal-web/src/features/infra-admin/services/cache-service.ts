import { get, post } from '@/services/request'
import { buildListParams } from '@/features/infra-admin/services/service-utils'
import type {
  CachePolicy,
  CacheStats,
  InfraListQuery,
  InfraPageData,
} from '@/features/infra-admin/types/infra'

const POLICY_URL = '/v1/infra/cache/policies'
const STATS_URL = '/v1/infra/cache/stats'

export const cacheService = {
  listPolicies(query?: InfraListQuery): Promise<InfraPageData<CachePolicy>> {
    return get(POLICY_URL, { params: buildListParams(query) })
  },
  createPolicy(payload: CachePolicy): Promise<CachePolicy> {
    return post(POLICY_URL, payload, {
      dedupeKey: `cache-policy:create:${payload.name}`,
    })
  },
  getStats(): Promise<CacheStats[]> {
    return get(STATS_URL)
  },
}
