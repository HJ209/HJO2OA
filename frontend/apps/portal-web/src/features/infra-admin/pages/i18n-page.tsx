import type { ReactElement } from 'react'
import { InfraPageSection } from '@/features/infra-admin/components/infra-page-section'
import { InfraTable } from '@/features/infra-admin/components/infra-table'
import { useI18nResources } from '@/features/infra-admin/hooks/use-i18n'

export default function I18nPage(): ReactElement {
  const query = useI18nResources({ page: 1, size: 20 })

  return (
    <InfraPageSection description="页面文案资源管理。" title="国际化资源">
      <InfraTable
        columns={[
          { key: 'locale', title: '语言', render: (item) => item.locale },
          {
            key: 'namespace',
            title: '命名空间',
            render: (item) => item.namespace,
          },
          {
            key: 'resourceKey',
            title: 'Key',
            render: (item) => item.resourceKey,
          },
          {
            key: 'resourceValue',
            title: '内容',
            render: (item) => item.resourceValue,
          },
        ]}
        getRowKey={(item) => item.id}
        isLoading={query.isLoading}
        items={query.data?.items ?? []}
      />
    </InfraPageSection>
  )
}
