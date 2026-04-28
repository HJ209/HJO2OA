import type { ReactElement } from 'react'
import {
  InfraTable,
  StatusPill,
} from '@/features/infra-admin/components/infra-table'
import type { DictionaryItem } from '@/features/infra-admin/types/infra'

export interface DictItemTableProps {
  items: DictionaryItem[]
  isLoading?: boolean
}

export function DictItemTable({
  items,
  isLoading = false,
}: DictItemTableProps): ReactElement {
  return (
    <InfraTable
      columns={[
        { key: 'label', title: '名称', render: (item) => item.label },
        { key: 'code', title: '编码', render: (item) => item.code },
        { key: 'value', title: '值', render: (item) => item.value },
        { key: 'sortOrder', title: '排序', render: (item) => item.sortOrder },
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
      getRowKey={(item) => item.code}
      isLoading={isLoading}
      items={items}
    />
  )
}
