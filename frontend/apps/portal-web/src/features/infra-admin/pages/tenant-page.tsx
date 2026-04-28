import type { ReactElement } from 'react'
import { InfraPageSection } from '@/features/infra-admin/components/infra-page-section'
import {
  InfraTable,
  StatusPill,
} from '@/features/infra-admin/components/infra-table'
import { useTenants } from '@/features/infra-admin/hooks/use-tenant'

export default function TenantPage(): ReactElement {
  const query = useTenants({ page: 1, size: 20 })

  return (
    <InfraPageSection description="租户档案、域名和默认时区。" title="租户管理">
      <InfraTable
        columns={[
          { key: 'name', title: '租户', render: (item) => item.name },
          { key: 'domain', title: '域名', render: (item) => item.domain },
          { key: 'timezone', title: '时区', render: (item) => item.timezone },
          {
            key: 'status',
            title: '状态',
            render: (item) => (
              <StatusPill active={item.status === 'enabled'}>
                {item.status === 'enabled' ? '启用' : '停用'}
              </StatusPill>
            ),
          },
        ]}
        getRowKey={(item) => item.id}
        isLoading={query.isLoading}
        items={query.data?.items ?? []}
      />
    </InfraPageSection>
  )
}
