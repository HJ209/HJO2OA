import type { ReactElement } from 'react'
import { InfraPageSection } from '@/features/infra-admin/components/infra-page-section'
import { InfraTable } from '@/features/infra-admin/components/infra-table'
import { useErrorCodes } from '@/features/infra-admin/hooks/use-error-code'

export default function ErrorCodePage(): ReactElement {
  const query = useErrorCodes({ page: 1, size: 20 })

  return (
    <InfraPageSection
      description="维护后端错误码定义、HTTP 状态和模块归属。"
      title="错误码管理"
    >
      <InfraTable
        columns={[
          { key: 'code', title: '错误码', render: (item) => item.code },
          { key: 'message', title: '消息', render: (item) => item.message },
          { key: 'module', title: '模块', render: (item) => item.module },
          {
            key: 'httpStatus',
            title: 'HTTP',
            render: (item) => item.httpStatus,
          },
        ]}
        getRowKey={(item) => item.code}
        isLoading={query.isLoading}
        items={query.data?.items ?? []}
      />
    </InfraPageSection>
  )
}
