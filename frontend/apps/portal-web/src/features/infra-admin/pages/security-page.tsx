import type { ReactElement } from 'react'
import { InfraPageSection } from '@/features/infra-admin/components/infra-page-section'
import {
  InfraTable,
  StatusPill,
} from '@/features/infra-admin/components/infra-table'
import { useSecurityPolicies } from '@/features/infra-admin/hooks/use-security'

export default function SecurityPage(): ReactElement {
  const query = useSecurityPolicies({ page: 1, size: 20 })

  return (
    <InfraPageSection
      description="密码、多因子和会话安全策略。"
      title="安全策略"
    >
      <InfraTable
        columns={[
          { key: 'name', title: '名称', render: (item) => item.name },
          {
            key: 'minPasswordLength',
            title: '密码长度',
            render: (item) => item.minPasswordLength,
          },
          {
            key: 'mfaRequired',
            title: 'MFA',
            render: (item) => (
              <StatusPill active={item.mfaRequired}>
                {item.mfaRequired ? '要求' : '不要求'}
              </StatusPill>
            ),
          },
          {
            key: 'sessionTimeoutMinutes',
            title: '会话超时',
            render: (item) => `${item.sessionTimeoutMinutes} 分钟`,
          },
        ]}
        getRowKey={(item) => item.id}
        isLoading={query.isLoading}
        items={query.data?.items ?? []}
      />
    </InfraPageSection>
  )
}
