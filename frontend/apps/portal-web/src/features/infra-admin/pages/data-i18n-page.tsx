import type { ReactElement } from 'react'
import { InfraPageSection } from '@/features/infra-admin/components/infra-page-section'
import { InfraTable } from '@/features/infra-admin/components/infra-table'
import { useDataI18nTranslations } from '@/features/infra-admin/hooks/use-data-i18n'

export default function DataI18nPage(): ReactElement {
  const query = useDataI18nTranslations({ page: 1, size: 20 })

  return (
    <InfraPageSection description="业务数据字段翻译管理。" title="数据国际化">
      <InfraTable
        columns={[
          {
            key: 'entityType',
            title: '实体',
            render: (item) => item.entityType,
          },
          { key: 'entityId', title: '实体ID', render: (item) => item.entityId },
          { key: 'field', title: '字段', render: (item) => item.field },
          { key: 'locale', title: '语言', render: (item) => item.locale },
          {
            key: 'translatedValue',
            title: '译文',
            render: (item) => item.translatedValue,
          },
        ]}
        getRowKey={(item) => item.id}
        isLoading={query.isLoading}
        items={query.data?.items ?? []}
      />
    </InfraPageSection>
  )
}
