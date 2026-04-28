import { useEffect, useMemo, useState, type ReactElement } from 'react'
import { DictItemTable } from '@/features/infra-admin/components/dict-item-table'
import { DictTypeList } from '@/features/infra-admin/components/dict-type-list'
import { InfraPageSection } from '@/features/infra-admin/components/infra-page-section'
import {
  useDictionaryItems,
  useDictionaryTypes,
} from '@/features/infra-admin/hooks/use-dictionary'

export default function DictionaryPage(): ReactElement {
  const [selectedCode, setSelectedCode] = useState('')
  const typesQuery = useDictionaryTypes({ page: 1, size: 50 })
  const typeItems = useMemo(
    () => typesQuery.data?.items ?? [],
    [typesQuery.data?.items],
  )

  useEffect(() => {
    if (!selectedCode && typeItems[0]) {
      setSelectedCode(typeItems[0].code)
    }
  }, [selectedCode, typeItems])

  const itemsQuery = useDictionaryItems(selectedCode, { page: 1, size: 50 })

  return (
    <div className="grid gap-4 xl:grid-cols-[280px_minmax(0,1fr)]">
      <DictTypeList
        isLoading={typesQuery.isLoading}
        items={typeItems}
        onSelect={setSelectedCode}
        selectedCode={selectedCode}
      />
      <InfraPageSection
        description="维护选中字典类型下的展示项、编码和值。"
        title="字典项"
      >
        <DictItemTable
          isLoading={itemsQuery.isLoading}
          items={itemsQuery.data?.items ?? []}
        />
      </InfraPageSection>
    </div>
  )
}
