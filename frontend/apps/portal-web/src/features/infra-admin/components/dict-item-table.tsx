import type { ReactElement } from 'react'
import { Button } from '@/components/ui/button'
import {
  InfraTable,
  StatusPill,
} from '@/features/infra-admin/components/infra-table'
import type { DictionaryItem } from '@/features/infra-admin/types/infra'

export interface DictItemTableProps {
  items: DictionaryItem[]
  isLoading?: boolean
  onEdit?: (item: DictionaryItem) => void
  onToggle?: (item: DictionaryItem) => void
  onDelete?: (item: DictionaryItem) => void
}

interface FlatDictionaryItem extends DictionaryItem {
  depth: number
}

function flattenItems(
  items: DictionaryItem[],
  depth = 0,
  output: FlatDictionaryItem[] = [],
): FlatDictionaryItem[] {
  items
    .slice()
    .sort((left, right) => left.sortOrder - right.sortOrder)
    .forEach((item) => {
      output.push({ ...item, depth })
      flattenItems(item.children ?? [], depth + 1, output)
    })

  return output
}

export function DictItemTable({
  items,
  isLoading = false,
  onEdit,
  onToggle,
  onDelete,
}: DictItemTableProps): ReactElement {
  const rows = flattenItems(items)

  return (
    <InfraTable
      columns={[
        {
          key: 'label',
          title: 'Name',
          render: (item) => (
            <span
              className="block min-w-[160px]"
              style={{ paddingLeft: `${item.depth * 20}px` }}
            >
              {item.depth > 0 ? '- ' : ''}
              {item.label}
            </span>
          ),
        },
        { key: 'code', title: 'Code', render: (item) => item.code },
        { key: 'value', title: 'Value', render: (item) => item.value },
        {
          key: 'parentId',
          title: 'Parent',
          render: (item) => item.parentId ?? '-',
        },
        {
          key: 'defaultItem',
          title: 'Default',
          render: (item) => (item.defaultItem ? 'Yes' : 'No'),
        },
        { key: 'sortOrder', title: 'Sort', render: (item) => item.sortOrder },
        {
          key: 'enabled',
          title: 'Status',
          render: (item) => (
            <StatusPill active={item.enabled}>
              {item.enabled ? 'Enabled' : 'Disabled'}
            </StatusPill>
          ),
        },
        {
          key: 'actions',
          title: 'Actions',
          render: (item) => (
            <div className="flex flex-wrap gap-2">
              <Button
                disabled={!onEdit}
                onClick={() => onEdit?.(item)}
                size="sm"
                variant="outline"
              >
                Edit
              </Button>
              <Button
                disabled={!onToggle}
                onClick={() => onToggle?.(item)}
                size="sm"
                variant="outline"
              >
                {item.enabled ? 'Disable' : 'Enable'}
              </Button>
              <Button
                disabled={!onDelete}
                onClick={() => onDelete?.(item)}
                size="sm"
                variant="outline"
              >
                Delete
              </Button>
            </div>
          ),
        },
      ]}
      getRowKey={(item) => item.id ?? item.code}
      isLoading={isLoading}
      items={rows}
    />
  )
}
