import type { ReactElement } from 'react'
import { InfraPageSection } from '@/features/infra-admin/components/infra-page-section'
import {
  InfraTable,
  StatusPill,
} from '@/features/infra-admin/components/infra-table'
import {
  useCachePolicies,
  useCacheStats,
} from '@/features/infra-admin/hooks/use-cache'

export default function CachePage(): ReactElement {
  const policiesQuery = useCachePolicies({ page: 1, size: 20 })
  const statsQuery = useCacheStats()

  return (
    <div className="space-y-4">
      <InfraPageSection description="缓存策略配置。" title="缓存策略">
        <InfraTable
          columns={[
            { key: 'name', title: '名称', render: (item) => item.name },
            {
              key: 'ttlSeconds',
              title: 'TTL(秒)',
              render: (item) => item.ttlSeconds,
            },
            {
              key: 'maxEntries',
              title: '容量',
              render: (item) => item.maxEntries,
            },
            {
              key: 'enabled',
              title: '状态',
              render: (item) => (
                <StatusPill active={item.enabled}>
                  {item.enabled ? '启用' : '停用'}
                </StatusPill>
              ),
            },
          ]}
          getRowKey={(item) => item.name}
          isLoading={policiesQuery.isLoading}
          items={policiesQuery.data?.items ?? []}
        />
      </InfraPageSection>
      <InfraPageSection description="缓存命中率和容量监控。" title="缓存监控">
        <InfraTable
          columns={[
            { key: 'region', title: '区域', render: (item) => item.region },
            {
              key: 'hitRate',
              title: '命中率',
              render: (item) => `${Math.round(item.hitRate * 100)}%`,
            },
            {
              key: 'entryCount',
              title: '条目',
              render: (item) => item.entryCount,
            },
            {
              key: 'memoryBytes',
              title: '内存',
              render: (item) => `${Math.round(item.memoryBytes / 1024)} KB`,
            },
          ]}
          getRowKey={(item) => item.region}
          isLoading={statsQuery.isLoading}
          items={statsQuery.data ?? []}
        />
      </InfraPageSection>
    </div>
  )
}
