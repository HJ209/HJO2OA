import { useMemo, useState, type ReactElement } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { RefreshCw, Search, Trash2 } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { InfraPageSection } from '@/features/infra-admin/components/infra-page-section'
import {
  InfraTable,
  StatusPill,
} from '@/features/infra-admin/components/infra-table'
import {
  useCacheInvalidations,
  useCacheKeys,
  useCachePolicies,
  useCacheStats,
} from '@/features/infra-admin/hooks/use-cache'
import { cacheService } from '@/features/infra-admin/services/cache-service'

export default function CachePage(): ReactElement {
  const queryClient = useQueryClient()
  const [namespace, setNamespace] = useState('')
  const [keyword, setKeyword] = useState('')
  const [tenantId, setTenantId] = useState('')
  const [reasonRef, setReasonRef] = useState('manual-refresh')

  const policiesQuery = useCachePolicies({ page: 1, size: 100 })
  const statsQuery = useCacheStats()
  const keyQuery = useCacheKeys({
    namespace: namespace || undefined,
    tenantId: tenantId || undefined,
    keyword: keyword || undefined,
  })
  const invalidationQuery = useCacheInvalidations({
    namespace: namespace || undefined,
    limit: 50,
  })
  const policies = useMemo(
    () => policiesQuery.data?.items ?? [],
    [policiesQuery.data?.items],
  )
  const selectedPolicy = useMemo(
    () => policies.find((item) => item.name === namespace),
    [namespace, policies],
  )

  const invalidateCacheQueries = async (): Promise<void> => {
    await Promise.all([
      queryClient.invalidateQueries({ queryKey: ['infra', 'cache-policies'] }),
      queryClient.invalidateQueries({ queryKey: ['infra', 'cache-stats'] }),
      queryClient.invalidateQueries({ queryKey: ['infra', 'cache-keys'] }),
      queryClient.invalidateQueries({
        queryKey: ['infra', 'cache-invalidations'],
      }),
    ])
  }

  const clearNamespaceMutation = useMutation({
    mutationFn: () => cacheService.clearNamespace(namespace, reasonRef),
    onSuccess: invalidateCacheQueries,
  })

  const refreshPolicyMutation = useMutation({
    mutationFn: () => {
      if (!selectedPolicy?.id) {
        throw new Error('Select a cache policy first')
      }

      return cacheService.refreshPolicy(selectedPolicy.id, reasonRef)
    },
    onSuccess: invalidateCacheQueries,
  })

  return (
    <div className="space-y-4">
      <InfraPageSection
        actions={
          <div className="flex flex-wrap gap-2">
            <Button
              disabled={!namespace || clearNamespaceMutation.isPending}
              onClick={() => clearNamespaceMutation.mutate()}
              variant="outline"
            >
              <Trash2 className="h-4 w-4" />
              Clear Namespace
            </Button>
            <Button
              disabled={!selectedPolicy?.id || refreshPolicyMutation.isPending}
              onClick={() => refreshPolicyMutation.mutate()}
              variant="outline"
            >
              <RefreshCw className="h-4 w-4" />
              Refresh Policy
            </Button>
          </div>
        }
        description="Runtime cache policies, TTL, namespace capacity and backend status."
        title="Cache Policies"
      >
        <InfraTable
          columns={[
            { key: 'name', title: 'Namespace', render: (item) => item.name },
            {
              key: 'backendType',
              title: 'Backend',
              render: (item) => item.backendType ?? 'MEMORY',
            },
            {
              key: 'ttlSeconds',
              title: 'TTL(s)',
              render: (item) => item.ttlSeconds,
            },
            {
              key: 'maxEntries',
              title: 'Capacity',
              render: (item) => item.maxEntries,
            },
            {
              key: 'enabled',
              title: 'Status',
              render: (item) => (
                <StatusPill active={item.enabled}>
                  {item.enabled ? 'Active' : 'Inactive'}
                </StatusPill>
              ),
            },
          ]}
          getRowKey={(item) => item.id ?? item.name}
          isLoading={policiesQuery.isLoading}
          items={policies}
        />
      </InfraPageSection>
      <InfraPageSection
        description="Query runtime keys by namespace, tenant and keyword."
        title="Runtime Keys"
      >
        <div className="mb-4 grid gap-3 lg:grid-cols-[1fr_1fr_1fr_auto]">
          <select
            className="h-10 rounded-xl border border-slate-200 bg-white px-3 text-sm"
            onChange={(event) => setNamespace(event.target.value)}
            value={namespace}
          >
            <option value="">All namespaces</option>
            {policies.map((policy) => (
              <option key={policy.name} value={policy.name}>
                {policy.name}
              </option>
            ))}
          </select>
          <Input
            onChange={(event) => setTenantId(event.target.value)}
            placeholder="Tenant ID"
            value={tenantId}
          />
          <Input
            onChange={(event) => setKeyword(event.target.value)}
            placeholder="Key keyword"
            value={keyword}
          />
          <Button
            onClick={() =>
              queryClient.invalidateQueries({
                queryKey: ['infra', 'cache-keys'],
              })
            }
            variant="outline"
          >
            <Search className="h-4 w-4" />
            Query
          </Button>
        </div>
        <InfraTable
          columns={[
            {
              key: 'namespace',
              title: 'Namespace',
              render: (item) => item.namespace,
            },
            {
              key: 'tenantId',
              title: 'Tenant',
              render: (item) => item.tenantId ?? '-',
            },
            { key: 'key', title: 'Key', render: (item) => item.key },
            {
              key: 'backendType',
              title: 'Backend',
              render: (item) => item.backendType,
            },
            {
              key: 'expiresAt',
              title: 'Expires',
              render: (item) => item.expiresAt ?? '-',
            },
          ]}
          getRowKey={(item) =>
            `${item.namespace}:${item.tenantId ?? ''}:${item.key}`
          }
          isLoading={keyQuery.isLoading}
          items={keyQuery.data ?? []}
        />
      </InfraPageSection>
      <InfraPageSection
        description="Hit/miss counters are collected from the local runtime layer and Redis adapter."
        title="Runtime Metrics"
      >
        <InfraTable
          columns={[
            {
              key: 'region',
              title: 'Namespace',
              render: (item) => item.region,
            },
            {
              key: 'hitRate',
              title: 'Hit Rate',
              render: (item) => `${Math.round((item.hitRate ?? 0) * 100)}%`,
            },
            {
              key: 'localHitCount',
              title: 'L1 Hits',
              render: (item) => item.localHitCount ?? 0,
            },
            {
              key: 'redisHitCount',
              title: 'Redis Hits',
              render: (item) => item.redisHitCount ?? 0,
            },
            {
              key: 'missCount',
              title: 'Misses',
              render: (item) => item.missCount ?? 0,
            },
            {
              key: 'entryCount',
              title: 'Keys',
              render: (item) => item.entryCount,
            },
            {
              key: 'invalidationCount',
              title: 'Invalidations',
              render: (item) => item.invalidationCount ?? 0,
            },
          ]}
          getRowKey={(item) => item.region}
          isLoading={statsQuery.isLoading}
          items={statsQuery.data ?? []}
        />
      </InfraPageSection>
      <InfraPageSection
        description="Recent namespace, key and dictionary-driven invalidation records."
        title="Invalidations"
      >
        <div className="mb-4 max-w-md">
          <Input
            onChange={(event) => setReasonRef(event.target.value)}
            placeholder="Reason reference"
            value={reasonRef}
          />
        </div>
        <InfraTable
          columns={[
            {
              key: 'namespace',
              title: 'Namespace',
              render: (item) => item.namespace,
            },
            {
              key: 'invalidateKey',
              title: 'Invalidated Key',
              render: (item) => item.invalidateKey,
            },
            {
              key: 'reasonType',
              title: 'Reason',
              render: (item) => item.reasonType,
            },
            {
              key: 'reasonRef',
              title: 'Reference',
              render: (item) => item.reasonRef ?? '-',
            },
            {
              key: 'invalidatedAt',
              title: 'Invalidated At',
              render: (item) => item.invalidatedAt,
            },
          ]}
          getRowKey={(item) => item.id}
          isLoading={invalidationQuery.isLoading}
          items={invalidationQuery.data ?? []}
        />
      </InfraPageSection>
    </div>
  )
}
