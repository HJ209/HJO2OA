import type { ReactElement } from 'react'
import { InfraPageSection } from '@/features/infra-admin/components/infra-page-section'
import {
  InfraTable,
  StatusPill,
} from '@/features/infra-admin/components/infra-table'
import { useTimezoneSettings } from '@/features/infra-admin/hooks/use-timezone'

export default function TimezonePage(): ReactElement {
  const query = useTimezoneSettings({ page: 1, size: 20 })

  return (
    <InfraPageSection description="租户和用户展示时区设置。" title="时区设置">
      <InfraTable
        columns={[
          { key: 'tenantId', title: '租户', render: (item) => item.tenantId },
          { key: 'timezone', title: '时区', render: (item) => item.timezone },
          {
            key: 'displayName',
            title: '显示名',
            render: (item) => item.displayName,
          },
          {
            key: 'defaultEnabled',
            title: '默认',
            render: (item) => (
              <StatusPill active={item.defaultEnabled}>
                {item.defaultEnabled ? '是' : '否'}
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
