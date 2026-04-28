import { useState, type ReactElement } from 'react'
import { AuditFilter } from '@/features/infra-admin/components/audit-filter'
import { InfraPageSection } from '@/features/infra-admin/components/infra-page-section'
import { InfraTable } from '@/features/infra-admin/components/infra-table'
import { useAuditRecords } from '@/features/infra-admin/hooks/use-audit'
import { buildAuditQuery } from '@/features/infra-admin/services/audit-service'
import type {
  AuditFilterValues,
  InfraListQuery,
} from '@/features/infra-admin/types/infra'
import { formatUtcToUserTimezone } from '@/utils/format-time'

export default function AuditPage(): ReactElement {
  const [query, setQuery] = useState<InfraListQuery>({
    page: 1,
    size: 20,
    sort: [{ field: 'createdAt', direction: 'desc' }],
  })
  const recordsQuery = useAuditRecords(query)

  function handleSearch(values: AuditFilterValues): void {
    setQuery(buildAuditQuery(values))
  }

  return (
    <InfraPageSection
      description="只读审计日志查询，UTC 时间按用户时区展示。"
      title="审计日志"
    >
      <div className="mb-4">
        <AuditFilter onSearch={handleSearch} />
      </div>
      <InfraTable
        columns={[
          { key: 'actor', title: '操作者', render: (item) => item.actor },
          { key: 'action', title: '操作', render: (item) => item.action },
          { key: 'resource', title: '资源', render: (item) => item.resource },
          {
            key: 'clientIp',
            title: 'IP',
            render: (item) => item.clientIp ?? '-',
          },
          {
            key: 'createdAt',
            title: '时间',
            render: (item) => formatUtcToUserTimezone(item.createdAt),
          },
        ]}
        getRowKey={(item) => item.id}
        isLoading={recordsQuery.isLoading}
        items={recordsQuery.data?.items ?? []}
      />
    </InfraPageSection>
  )
}
