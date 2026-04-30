import { get, post } from '@/services/request'
import { toPageData } from '@/features/infra-admin/services/service-utils'
import type {
  CacheInvalidation,
  CachePolicy,
  CacheRuntimeKey,
  CacheStats,
  InfraListQuery,
  InfraPageData,
} from '@/features/infra-admin/types/infra'

const POLICY_URL = '/v1/infra/cache/policies'
const RUNTIME_URL = '/v1/infra/cache'

interface BackendCachePolicy {
  id: string
  namespace: string
  backendType: 'MEMORY' | 'REDIS' | 'HYBRID'
  ttlSeconds: number
  maxCapacity?: number | null
  evictionPolicy?: string
  invalidationMode?: string
  active: boolean
  updatedAt?: string
}

interface BackendCacheRuntimeMetrics {
  namespace: string
  localHitCount: number
  redisHitCount: number
  missCount: number
  putCount: number
  invalidationCount: number
  keyCount: number
}

function mapPolicy(item: BackendCachePolicy): CachePolicy {
  return {
    id: item.id,
    name: item.namespace,
    backendType: item.backendType,
    ttlSeconds: item.ttlSeconds,
    maxEntries: item.maxCapacity ?? 0,
    evictionPolicy: item.evictionPolicy,
    invalidationMode: item.invalidationMode,
    enabled: item.active,
    updatedAt: item.updatedAt,
  }
}

function mapMetrics(item: BackendCacheRuntimeMetrics): CacheStats {
  const hits = item.localHitCount + item.redisHitCount
  const totalReads = hits + item.missCount

  return {
    region: item.namespace,
    hitRate: totalReads === 0 ? 0 : hits / totalReads,
    localHitCount: item.localHitCount,
    redisHitCount: item.redisHitCount,
    missCount: item.missCount,
    putCount: item.putCount,
    invalidationCount: item.invalidationCount,
    entryCount: item.keyCount,
    memoryBytes: 0,
  }
}

export const cacheService = {
  async listPolicies(
    query?: InfraListQuery,
  ): Promise<InfraPageData<CachePolicy>> {
    const items = await get<BackendCachePolicy[]>(POLICY_URL)

    return toPageData(items.map(mapPolicy), query)
  },
  async createPolicy(payload: CachePolicy): Promise<CachePolicy> {
    const item = await post<
      BackendCachePolicy,
      {
        namespace: string
        backendType: 'MEMORY' | 'REDIS' | 'HYBRID'
        ttlSeconds: number
        maxCapacity: number
        evictionPolicy: string
        invalidationMode: string
      }
    >(
      POLICY_URL,
      {
        namespace: payload.name,
        backendType: payload.backendType ?? 'MEMORY',
        ttlSeconds: payload.ttlSeconds,
        maxCapacity: payload.maxEntries,
        evictionPolicy: payload.evictionPolicy ?? 'LRU',
        invalidationMode: payload.invalidationMode ?? 'MANUAL',
      },
      {
        dedupeKey: `cache-policy:create:${payload.name}`,
      },
    )

    return mapPolicy(item)
  },
  async getStats(): Promise<CacheStats[]> {
    const items = await get<BackendCacheRuntimeMetrics[]>(
      `${RUNTIME_URL}/metrics`,
    )

    return items.map(mapMetrics)
  },
  queryKeys(query: {
    namespace?: string
    tenantId?: string
    keyword?: string
  }): Promise<CacheRuntimeKey[]> {
    return get<CacheRuntimeKey[]>(`${RUNTIME_URL}/keys`, {
      params: query,
    })
  },
  listInvalidations(
    query: {
      namespace?: string
      limit?: number
    } = {},
  ): Promise<CacheInvalidation[]> {
    return get<CacheInvalidation[]>(`${RUNTIME_URL}/invalidations`, {
      params: query,
    })
  },
  clearNamespace(
    namespace: string,
    reasonRef?: string,
  ): Promise<CacheInvalidation> {
    return post<CacheInvalidation, { reasonRef?: string }>(
      `${RUNTIME_URL}/namespaces/${namespace}/clear`,
      { reasonRef },
      {
        dedupeKey: `cache-namespace:clear:${namespace}`,
      },
    )
  },
  refreshPolicy(
    policyId: string,
    reasonRef?: string,
  ): Promise<CacheInvalidation> {
    return post<CacheInvalidation, undefined>(
      `${POLICY_URL}/${policyId}/refresh`,
      undefined,
      {
        params: { reasonRef },
        dedupeKey: `cache-policy:refresh:${policyId}`,
      },
    )
  },
}
