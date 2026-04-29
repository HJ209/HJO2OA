import {
  useEffect,
  useMemo,
  useState,
  type FormEvent,
  type ReactElement,
} from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { DictItemTable } from '@/features/infra-admin/components/dict-item-table'
import { DictTypeList } from '@/features/infra-admin/components/dict-type-list'
import { InfraPageSection } from '@/features/infra-admin/components/infra-page-section'
import {
  useDictionaryItems,
  useDictionaryTypes,
} from '@/features/infra-admin/hooks/use-dictionary'
import { dictionaryService } from '@/features/infra-admin/services/dictionary-service'
import type {
  DictionaryItem,
  DictionaryType,
  SystemEnumDictionary,
  SystemEnumImportResult,
} from '@/features/infra-admin/types/infra'

const emptyType: DictionaryType = {
  code: '',
  name: '',
  category: '',
  hierarchical: false,
  cacheable: true,
  status: 'enabled',
}

const emptyItem: DictionaryItem = {
  code: '',
  label: '',
  value: '',
  sortOrder: 0,
  enabled: true,
}

export default function DictionaryPage(): ReactElement {
  const queryClient = useQueryClient()
  const [selectedCode, setSelectedCode] = useState('')
  const [typeDialogOpen, setTypeDialogOpen] = useState(false)
  const [typeDraft, setTypeDraft] = useState<DictionaryType>(emptyType)
  const [itemDialogOpen, setItemDialogOpen] = useState(false)
  const [editingItem, setEditingItem] = useState<DictionaryItem>()
  const [itemDraft, setItemDraft] = useState<DictionaryItem>(emptyItem)
  const [systemEnums, setSystemEnums] = useState<SystemEnumDictionary[]>([])
  const [systemEnumImportResult, setSystemEnumImportResult] =
    useState<SystemEnumImportResult>()
  const typesQuery = useDictionaryTypes({ page: 1, size: 50 })
  const typeItems = useMemo(
    () => typesQuery.data?.items ?? [],
    [typesQuery.data?.items],
  )
  const selectedType = typeItems.find((item) => item.code === selectedCode)

  useEffect(() => {
    if (!selectedCode && typeItems[0]) {
      setSelectedCode(typeItems[0].code)
    }
  }, [selectedCode, typeItems])

  const itemsQuery = useDictionaryItems(selectedCode, { page: 1, size: 50 })

  const invalidateDictionaries = async (): Promise<void> => {
    await Promise.all([
      queryClient.invalidateQueries({ queryKey: ['infra', 'dictionary-types'] }),
      queryClient.invalidateQueries({ queryKey: ['infra', 'dictionary-items'] }),
    ])
  }

  const createTypeMutation = useMutation({
    mutationFn: (payload: DictionaryType) => dictionaryService.createType(payload),
    onSuccess: async (nextType) => {
      setTypeDialogOpen(false)
      setSelectedCode(nextType.code)
      await invalidateDictionaries()
    },
  })

  const toggleTypeMutation = useMutation({
    mutationFn: (type: DictionaryType) =>
      type.status === 'enabled'
        ? dictionaryService.disableType(type.id ?? type.code)
        : dictionaryService.enableType(type.id ?? type.code),
    onSuccess: invalidateDictionaries,
  })

  const saveItemMutation = useMutation({
    mutationFn: (payload: DictionaryItem) => {
      if (!selectedType?.id) {
        throw new Error('请选择字典类型')
      }

      return editingItem?.id
        ? dictionaryService.updateItem(selectedType.id, editingItem.id, payload)
        : dictionaryService.createItem(selectedType.id, payload)
    },
    onSuccess: async () => {
      setItemDialogOpen(false)
      setEditingItem(undefined)
      await invalidateDictionaries()
    },
  })

  const deleteItemMutation = useMutation({
    mutationFn: (item: DictionaryItem) => {
      if (!selectedType?.id || !item.id) {
        throw new Error('请选择字典项')
      }

      return dictionaryService.deleteItem(selectedType.id, item.id)
    },
    onSuccess: invalidateDictionaries,
  })

  const toggleItemMutation = useMutation({
    mutationFn: (item: DictionaryItem) => {
      if (!selectedType?.id || !item.id) {
        throw new Error('请选择字典项')
      }

      return item.enabled
        ? dictionaryService.disableItem(selectedType.id, item.id)
        : dictionaryService.enableItem(selectedType.id, item.id)
    },
    onSuccess: invalidateDictionaries,
  })

  const previewSystemEnumsMutation = useMutation({
    mutationFn: dictionaryService.previewSystemEnums,
    onSuccess: setSystemEnums,
  })

  const importSystemEnumsMutation = useMutation({
    mutationFn: dictionaryService.importSystemEnums,
    onSuccess: async (result) => {
      setSystemEnumImportResult(result)
      await invalidateDictionaries()
    },
  })

  function openCreateTypeDialog(): void {
    setTypeDraft(emptyType)
    setTypeDialogOpen(true)
  }

  function submitType(event: FormEvent<HTMLFormElement>): void {
    event.preventDefault()
    createTypeMutation.mutate(typeDraft)
  }

  function openCreateItemDialog(): void {
    setEditingItem(undefined)
    setItemDraft(emptyItem)
    setItemDialogOpen(true)
  }

  function openEditItemDialog(item: DictionaryItem): void {
    setEditingItem(item)
    setItemDraft(item)
    setItemDialogOpen(true)
  }

  function submitItem(event: FormEvent<HTMLFormElement>): void {
    event.preventDefault()
    saveItemMutation.mutate(itemDraft)
  }

  return (
    <div className="grid gap-4 xl:grid-cols-[280px_minmax(0,1fr)]">
      <div className="space-y-3">
        <Button className="w-full" onClick={openCreateTypeDialog}>
          新增字典类型
        </Button>
        <div className="grid gap-2 rounded-2xl border border-slate-200 bg-white p-3">
          <div className="text-sm font-semibold text-slate-900">系统枚举</div>
          <div className="grid grid-cols-2 gap-2">
            <Button
              disabled={previewSystemEnumsMutation.isPending}
              onClick={() => previewSystemEnumsMutation.mutate()}
              size="sm"
              variant="outline"
            >
              扫描
            </Button>
            <Button
              disabled={importSystemEnumsMutation.isPending}
              onClick={() => importSystemEnumsMutation.mutate()}
              size="sm"
              variant="outline"
            >
              导入
            </Button>
          </div>
          {systemEnums.length > 0 ? (
            <p className="text-xs text-slate-500">
              已扫描 {systemEnums.length} 个枚举，约{' '}
              {systemEnums.reduce((total, item) => total + item.items.length, 0)}{' '}
              个枚举项。
            </p>
          ) : null}
          {systemEnumImportResult ? (
            <p className="text-xs text-slate-500">
              本次新增 {systemEnumImportResult.createdTypes} 个类型、{' '}
              {systemEnumImportResult.createdItems} 个枚举项。
            </p>
          ) : null}
        </div>
        <DictTypeList
          isLoading={typesQuery.isLoading}
          items={typeItems}
          onSelect={setSelectedCode}
          selectedCode={selectedCode}
        />
      </div>
      <InfraPageSection
        actions={
          <div className="flex gap-2">
            <Button disabled={!selectedType} onClick={openCreateItemDialog}>
              新增字典项
            </Button>
            <Button
              disabled={!selectedType || toggleTypeMutation.isPending}
              onClick={() => selectedType && toggleTypeMutation.mutate(selectedType)}
              variant="outline"
            >
              {selectedType?.status === 'enabled' ? '停用类型' : '启用类型'}
            </Button>
          </div>
        }
        description="维护选中字典类型下的展示项、编码和值。"
        title="字典项"
      >
        <DictItemTable
          isLoading={itemsQuery.isLoading}
          items={itemsQuery.data?.items ?? []}
          onDelete={(item) => deleteItemMutation.mutate(item)}
          onEdit={openEditItemDialog}
          onToggle={(item) => toggleItemMutation.mutate(item)}
        />
      </InfraPageSection>
      {typeDialogOpen ? (
        <div
          aria-modal="true"
          className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/40 p-4"
          role="dialog"
        >
          <form
            className="w-full max-w-lg rounded-2xl bg-white p-6 shadow-xl"
            onSubmit={submitType}
          >
            <h2 className="text-lg font-semibold text-slate-950">
              新增字典类型
            </h2>
            <div className="mt-5 grid gap-4">
              <label className="grid gap-2 text-sm font-medium text-slate-700">
                编码
                <Input
                  onChange={(event) =>
                    setTypeDraft((current) => ({
                      ...current,
                      code: event.target.value,
                    }))
                  }
                  required
                  value={typeDraft.code}
                />
              </label>
              <label className="grid gap-2 text-sm font-medium text-slate-700">
                名称
                <Input
                  onChange={(event) =>
                    setTypeDraft((current) => ({
                      ...current,
                      name: event.target.value,
                    }))
                  }
                  required
                  value={typeDraft.name}
                />
              </label>
              <label className="grid gap-2 text-sm font-medium text-slate-700">
                分类
                <Input
                  onChange={(event) =>
                    setTypeDraft((current) => ({
                      ...current,
                      category: event.target.value,
                    }))
                  }
                  value={typeDraft.category ?? ''}
                />
              </label>
              <label className="flex items-center gap-2 text-sm text-slate-700">
                <input
                  checked={typeDraft.hierarchical ?? false}
                  onChange={(event) =>
                    setTypeDraft((current) => ({
                      ...current,
                      hierarchical: event.target.checked,
                    }))
                  }
                  type="checkbox"
                />
                层级字典
              </label>
              <label className="flex items-center gap-2 text-sm text-slate-700">
                <input
                  checked={typeDraft.cacheable ?? true}
                  onChange={(event) =>
                    setTypeDraft((current) => ({
                      ...current,
                      cacheable: event.target.checked,
                    }))
                  }
                  type="checkbox"
                />
                允许缓存
              </label>
            </div>
            <div className="mt-6 flex justify-end gap-3">
              <Button onClick={() => setTypeDialogOpen(false)} variant="outline">
                取消
              </Button>
              <Button disabled={createTypeMutation.isPending} type="submit">
                保存
              </Button>
            </div>
          </form>
        </div>
      ) : null}
      {itemDialogOpen ? (
        <div
          aria-modal="true"
          className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/40 p-4"
          role="dialog"
        >
          <form
            className="w-full max-w-lg rounded-2xl bg-white p-6 shadow-xl"
            onSubmit={submitItem}
          >
            <h2 className="text-lg font-semibold text-slate-950">
              {editingItem ? '编辑字典项' : '新增字典项'}
            </h2>
            <div className="mt-5 grid gap-4">
              <label className="grid gap-2 text-sm font-medium text-slate-700">
                编码
                <Input
                  disabled={Boolean(editingItem)}
                  onChange={(event) =>
                    setItemDraft((current) => ({
                      ...current,
                      code: event.target.value,
                      value: event.target.value,
                    }))
                  }
                  required
                  value={itemDraft.code}
                />
              </label>
              <label className="grid gap-2 text-sm font-medium text-slate-700">
                名称
                <Input
                  onChange={(event) =>
                    setItemDraft((current) => ({
                      ...current,
                      label: event.target.value,
                    }))
                  }
                  required
                  value={itemDraft.label}
                />
              </label>
              <label className="grid gap-2 text-sm font-medium text-slate-700">
                排序
                <Input
                  min={0}
                  onChange={(event) =>
                    setItemDraft((current) => ({
                      ...current,
                      sortOrder: Number(event.target.value),
                    }))
                  }
                  type="number"
                  value={itemDraft.sortOrder}
                />
              </label>
            </div>
            <div className="mt-6 flex justify-end gap-3">
              <Button onClick={() => setItemDialogOpen(false)} variant="outline">
                取消
              </Button>
              <Button disabled={saveItemMutation.isPending} type="submit">
                保存
              </Button>
            </div>
          </form>
        </div>
      ) : null}
    </div>
  )
}
