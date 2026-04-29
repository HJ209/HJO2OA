import type { ReactElement } from 'react'
import {
  InfraTable,
  StatusPill,
} from '@/features/infra-admin/components/infra-table'
import { Button } from '@/components/ui/button'
import type { DictionaryItem } from '@/features/infra-admin/types/infra'

export interface DictItemTableProps {
  items: DictionaryItem[]
  isLoading?: boolean
  onEdit?: (item: DictionaryItem) => void
  onToggle?: (item: DictionaryItem) => void
  onDelete?: (item: DictionaryItem) => void
}

export function DictItemTable({
  items,
  isLoading = false,
  onEdit,
  onToggle,
  onDelete,
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
        {
          key: 'actions',
          title: '操作',
          render: (item) => (
            <div className="flex gap-2">
              <Button
                disabled={!onEdit}
                onClick={() => onEdit?.(item)}
                size="sm"
                variant="outline"
              >
                编辑
              </Button>
              <Button
                disabled={!onToggle}
                onClick={() => onToggle?.(item)}
                size="sm"
                variant="outline"
              >
                {item.enabled ? '停用' : '启用'}
              </Button>
              <Button
                disabled={!onDelete}
                onClick={() => onDelete?.(item)}
                size="sm"
                variant="outline"
              >
                删除
              </Button>
            </div>
          ),
        },
      ]}
      getRowKey={(item) => item.id ?? item.code}
      isLoading={isLoading}
      items={items}
    />
  )
}
