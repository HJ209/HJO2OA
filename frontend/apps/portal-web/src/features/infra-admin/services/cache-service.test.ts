import { beforeEach, describe, expect, it, vi } from 'vitest'
import { cacheService } from '@/features/infra-admin/services/cache-service'
import { get, post } from '@/services/request'

vi.mock('@/services/request', () => ({
  get: vi.fn(),
  post: vi.fn(),
}))

const mockedGet = vi.mocked(get)
const mockedPost = vi.mocked(post)

describe('cacheService', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('lists cache policies and maps backend policy metadata', async () => {
    mockedGet.mockResolvedValueOnce([
      {
        id: 'policy-1',
        namespace: 'infra.dictionary.runtime',
        backendType: 'HYBRID',
        ttlSeconds: 300,
        maxCapacity: 1000,
        evictionPolicy: 'LRU',
        invalidationMode: 'EVENT_DRIVEN',
        active: true,
      },
    ])

    const page = await cacheService.listPolicies({ page: 1, size: 20 })

    expect(mockedGet).toHaveBeenCalledWith('/v1/infra/cache/policies')
    expect(page.items).toEqual([
      expect.objectContaining({
        id: 'policy-1',
        name: 'infra.dictionary.runtime',
        backendType: 'HYBRID',
        ttlSeconds: 300,
        maxEntries: 1000,
        enabled: true,
      }),
    ])
  })

  it('creates cache policies through the backend create contract', async () => {
    mockedPost.mockResolvedValueOnce({
      id: 'policy-1',
      namespace: 'infra.dictionary.runtime',
      backendType: 'MEMORY',
      ttlSeconds: 300,
      maxCapacity: 1000,
      evictionPolicy: 'LRU',
      invalidationMode: 'MANUAL',
      active: true,
    })

    await cacheService.createPolicy({
      name: 'infra.dictionary.runtime',
      backendType: 'MEMORY',
      ttlSeconds: 300,
      maxEntries: 1000,
      enabled: true,
    })

    expect(mockedPost).toHaveBeenCalledWith(
      '/v1/infra/cache/policies',
      {
        namespace: 'infra.dictionary.runtime',
        backendType: 'MEMORY',
        ttlSeconds: 300,
        maxCapacity: 1000,
        evictionPolicy: 'LRU',
        invalidationMode: 'MANUAL',
      },
      { dedupeKey: 'cache-policy:create:infra.dictionary.runtime' },
    )
  })

  it('reads runtime metrics and computes hit rate', async () => {
    mockedGet.mockResolvedValueOnce([
      {
        namespace: 'infra.dictionary.runtime',
        localHitCount: 7,
        redisHitCount: 3,
        missCount: 5,
        putCount: 6,
        invalidationCount: 2,
        keyCount: 4,
      },
    ])

    const stats = await cacheService.getStats()

    expect(mockedGet).toHaveBeenCalledWith('/v1/infra/cache/metrics')
    expect(stats[0]).toEqual(
      expect.objectContaining({
        region: 'infra.dictionary.runtime',
        hitRate: 10 / 15,
        entryCount: 4,
        invalidationCount: 2,
      }),
    )
  })

  it('queries keys, invalidations and namespace operations', async () => {
    mockedGet
      .mockResolvedValueOnce([
        {
          namespace: 'infra.dictionary.runtime',
          tenantId: 'tenant-1',
          key: 'person_status:items',
          backendType: 'MEMORY',
          expiresAt: null,
        },
      ])
      .mockResolvedValueOnce([
        {
          id: 'invalidation-1',
          cachePolicyId: 'policy-1',
          namespace: 'infra.dictionary.runtime',
          invalidateKey: '*',
          reasonType: 'MANUAL',
          reasonRef: 'ops',
          invalidatedAt: '2026-04-29T01:00:00Z',
        },
      ])
    mockedPost.mockResolvedValue({
      id: 'invalidation-1',
      cachePolicyId: 'policy-1',
      namespace: 'infra.dictionary.runtime',
      invalidateKey: '*',
      reasonType: 'MANUAL',
      reasonRef: 'ops',
      invalidatedAt: '2026-04-29T01:00:00Z',
    })

    await cacheService.queryKeys({
      namespace: 'infra.dictionary.runtime',
      tenantId: 'tenant-1',
      keyword: 'person',
    })
    await cacheService.listInvalidations({
      namespace: 'infra.dictionary.runtime',
      limit: 20,
    })
    await cacheService.clearNamespace('infra.dictionary.runtime', 'ops')
    await cacheService.refreshPolicy('policy-1', 'ops')

    expect(mockedGet).toHaveBeenCalledWith('/v1/infra/cache/keys', {
      params: {
        namespace: 'infra.dictionary.runtime',
        tenantId: 'tenant-1',
        keyword: 'person',
      },
    })
    expect(mockedGet).toHaveBeenCalledWith('/v1/infra/cache/invalidations', {
      params: { namespace: 'infra.dictionary.runtime', limit: 20 },
    })
    expect(mockedPost).toHaveBeenCalledWith(
      '/v1/infra/cache/namespaces/infra.dictionary.runtime/clear',
      { reasonRef: 'ops' },
      { dedupeKey: 'cache-namespace:clear:infra.dictionary.runtime' },
    )
    expect(mockedPost).toHaveBeenCalledWith(
      '/v1/infra/cache/policies/policy-1/refresh',
      undefined,
      {
        params: { reasonRef: 'ops' },
        dedupeKey: 'cache-policy:refresh:policy-1',
      },
    )
  })
})
