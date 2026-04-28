import type { ReactElement, ReactNode } from 'react'
import { INFRA_COPY } from '@/features/infra-admin/infra-copy'
import { cn } from '@/utils/cn'

export interface InfraTableColumn<TItem> {
  key: string
  title: string
  render: (item: TItem) => ReactNode
}

export interface InfraTableProps<TItem> {
  columns: InfraTableColumn<TItem>[]
  getRowKey: (item: TItem) => string
  items: TItem[]
  isLoading?: boolean
  emptyText?: string
}

export function InfraTable<TItem>({
  columns,
  getRowKey,
  items,
  isLoading = false,
  emptyText = INFRA_COPY.state.empty,
}: InfraTableProps<TItem>): ReactElement {
  return (
    <div className="overflow-hidden rounded-2xl border border-slate-200 bg-white">
      <table className="w-full border-collapse text-left text-sm">
        <thead className="bg-slate-50 text-xs font-semibold uppercase text-slate-500">
          <tr>
            {columns.map((column) => (
              <th className="px-4 py-3" key={column.key}>
                {column.title}
              </th>
            ))}
          </tr>
        </thead>
        <tbody className="divide-y divide-slate-100">
          {isLoading ? (
            <tr>
              <td className="px-4 py-6 text-slate-500" colSpan={columns.length}>
                {INFRA_COPY.state.loading}
              </td>
            </tr>
          ) : null}
          {!isLoading && items.length === 0 ? (
            <tr>
              <td className="px-4 py-6 text-slate-500" colSpan={columns.length}>
                {emptyText}
              </td>
            </tr>
          ) : null}
          {!isLoading
            ? items.map((item) => (
                <tr className="text-slate-700" key={getRowKey(item)}>
                  {columns.map((column) => (
                    <td className="px-4 py-3 align-top" key={column.key}>
                      {column.render(item)}
                    </td>
                  ))}
                </tr>
              ))
            : null}
        </tbody>
      </table>
    </div>
  )
}

export function StatusPill({
  active,
  children,
}: {
  active: boolean
  children: ReactNode
}): ReactElement {
  return (
    <span
      className={cn(
        'inline-flex rounded-full px-2 py-1 text-xs font-medium',
        active
          ? 'bg-emerald-100 text-emerald-700'
          : 'bg-slate-100 text-slate-600',
      )}
    >
      {children}
    </span>
  )
}
