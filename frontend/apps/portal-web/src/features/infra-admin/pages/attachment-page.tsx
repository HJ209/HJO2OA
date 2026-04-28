import type { ReactElement } from 'react'
import { InfraPageSection } from '@/features/infra-admin/components/infra-page-section'
import { InfraTable } from '@/features/infra-admin/components/infra-table'
import { useAttachmentAssets } from '@/features/infra-admin/hooks/use-attachment'

export default function AttachmentPage(): ReactElement {
  const query = useAttachmentAssets({ page: 1, size: 20 })

  return (
    <InfraPageSection description="附件资产元数据和归属信息。" title="附件资产">
      <InfraTable
        columns={[
          { key: 'fileName', title: '文件名', render: (item) => item.fileName },
          {
            key: 'contentType',
            title: '类型',
            render: (item) => item.contentType,
          },
          {
            key: 'sizeBytes',
            title: '大小',
            render: (item) => `${Math.round(item.sizeBytes / 1024)} KB`,
          },
          { key: 'owner', title: '归属', render: (item) => item.owner ?? '-' },
        ]}
        getRowKey={(item) => item.id}
        isLoading={query.isLoading}
        items={query.data?.items ?? []}
      />
    </InfraPageSection>
  )
}
