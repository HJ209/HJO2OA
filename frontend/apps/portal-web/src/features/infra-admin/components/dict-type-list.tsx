import type { ReactElement } from 'react'
import { Database } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { INFRA_COPY } from '@/features/infra-admin/infra-copy'
import type { DictionaryType } from '@/features/infra-admin/types/infra'
import { cn } from '@/utils/cn'

export interface DictTypeListProps {
  items: DictionaryType[]
  selectedCode?: string
  isLoading?: boolean
  onSelect: (code: string) => void
}

export function DictTypeList({
  items,
  selectedCode,
  isLoading = false,
  onSelect,
}: DictTypeListProps): ReactElement {
  return (
    <div className="rounded-2xl border border-slate-200 bg-white p-3">
      <div className="mb-3 flex items-center gap-2 px-1 text-sm font-semibold text-slate-900">
        <Database className="h-4 w-4 text-sky-600" />
        字典类型
      </div>
      <div className="space-y-2">
        {isLoading ? (
          <p className="px-2 py-4 text-sm text-slate-500">
            {INFRA_COPY.state.loading}
          </p>
        ) : null}
        {!isLoading && items.length === 0 ? (
          <p className="px-2 py-4 text-sm text-slate-500">
            {INFRA_COPY.state.empty}
          </p>
        ) : null}
        {!isLoading
          ? items.map((item) => (
              <Button
                className={cn(
                  'h-auto w-full justify-start rounded-xl px-3 py-3 text-left',
                  selectedCode === item.code
                    ? 'border-sky-200 bg-sky-50 text-sky-700'
                    : '',
                )}
                key={item.code}
                onClick={() => onSelect(item.code)}
                variant="outline"
              >
                <span className="min-w-0">
                  <span className="block truncate font-medium">
                    {item.name}
                  </span>
                  <span className="block truncate text-xs text-slate-500">
                    {item.code}
                  </span>
                </span>
              </Button>
            ))
          : null}
      </div>
    </div>
  )
}
