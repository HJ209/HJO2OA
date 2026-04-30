import {
  useEffect,
  useMemo,
  useState,
  type FormEvent,
  type ReactElement,
} from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { Download, Pencil, Plus, RefreshCw, Search } from 'lucide-react'
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
  sortOrder: 0,
  status: 'enabled',
}

const emptyItem: DictionaryItem = {
  code: '',
  label: '',
  value: '',
  sortOrder: 0,
  enabled: true,
  defaultItem: false,
  extensionJson: '',
}

function buildItemTree(items: DictionaryItem[]): DictionaryItem[] {
  const itemsById = new Map<string, DictionaryItem>()
  const roots: DictionaryItem[] = []

  items.forEach((item) => {
    itemsById.set(item.id ?? item.code, { ...item, children: [] })
  })

  itemsById.forEach((item) => {
    if (item.parentId && itemsById.has(item.parentId)) {
      itemsById.get(item.parentId)?.children?.push(item)
      return
    }

    roots.push(item)
  })

  const sortTree = (entries: DictionaryItem[]): DictionaryItem[] =>
    entries
      .slice()
      .sort((left, right) => left.sortOrder - right.sortOrder)
      .map((entry) => ({
        ...entry,
        children: sortTree(entry.children ?? []),
      }))

  return sortTree(roots)
}

export default function DictionaryPage(): ReactElement {
  const queryClient = useQueryClient()
  const [selectedCode, setSelectedCode] = useState('')
  const [typeDialogOpen, setTypeDialogOpen] = useState(false)
  const [editingType, setEditingType] = useState<DictionaryType>()
  const [typeDraft, setTypeDraft] = useState<DictionaryType>(emptyType)
  const [itemDialogOpen, setItemDialogOpen] = useState(false)
  const [editingItem, setEditingItem] = useState<DictionaryItem>()
  const [itemDraft, setItemDraft] = useState<DictionaryItem>(emptyItem)
  const [systemEnums, setSystemEnums] = useState<SystemEnumDictionary[]>([])
  const [systemEnumImportResult, setSystemEnumImportResult] =
    useState<SystemEnumImportResult>()

  const typesQuery = useDictionaryTypes({ page: 1, size: 200 })
  const typeItems = useMemo(
    () => typesQuery.data?.items ?? [],
    [typesQuery.data?.items],
  )
  const selectedType = typeItems.find((item) => item.code === selectedCode)
  const selectedTypeIsSystem = Boolean(selectedType?.systemManaged)

  useEffect(() => {
    if (!selectedCode && typeItems[0]) {
      setSelectedCode(typeItems[0].code)
    }
  }, [selectedCode, typeItems])

  const itemsQuery = useDictionaryItems(selectedCode, { page: 1, size: 500 })
  const flatItems = itemsQuery.data?.items ?? []
  const treeItems = selectedType?.hierarchical
    ? buildItemTree(flatItems)
    : flatItems

  const invalidateDictionaries = async (): Promise<void> => {
    await Promise.all([
      queryClient.invalidateQueries({
        queryKey: ['infra', 'dictionary-types'],
      }),
      queryClient.invalidateQueries({
        queryKey: ['infra', 'dictionary-items'],
      }),
      queryClient.invalidateQueries({ queryKey: ['infra', 'dictionary-tree'] }),
      queryClient.invalidateQueries({
        queryKey: ['infra', 'dictionary-options'],
      }),
      queryClient.invalidateQueries({ queryKey: ['infra', 'cache-stats'] }),
      queryClient.invalidateQueries({ queryKey: ['infra', 'cache-keys'] }),
    ])
  }

  const saveTypeMutation = useMutation({
    mutationFn: (payload: DictionaryType) =>
      editingType?.id
        ? dictionaryService.updateType(editingType.id, payload)
        : dictionaryService.createType(payload),
    onSuccess: async (nextType) => {
      setTypeDialogOpen(false)
      setEditingType(undefined)
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
        throw new Error('Select a dictionary type first')
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
        throw new Error('Select a dictionary item first')
      }

      return dictionaryService.deleteItem(selectedType.id, item.id)
    },
    onSuccess: invalidateDictionaries,
  })

  const toggleItemMutation = useMutation({
    mutationFn: (item: DictionaryItem) => {
      if (!selectedType?.id || !item.id) {
        throw new Error('Select a dictionary item first')
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

  const refreshCacheMutation = useMutation({
    mutationFn: () =>
      dictionaryService.refreshCache(selectedCode, { tree: true }),
    onSuccess: invalidateDictionaries,
  })

  function openCreateTypeDialog(): void {
    setEditingType(undefined)
    setTypeDraft(emptyType)
    setTypeDialogOpen(true)
  }

  function openEditTypeDialog(): void {
    if (!selectedType) {
      return
    }

    setEditingType(selectedType)
    setTypeDraft(selectedType)
    setTypeDialogOpen(true)
  }

  function submitType(event: FormEvent<HTMLFormElement>): void {
    event.preventDefault()
    saveTypeMutation.mutate(typeDraft)
  }

  function openCreateItemDialog(): void {
    setEditingItem(undefined)
    setItemDraft({
      ...emptyItem,
      sortOrder: flatItems.length + 1,
    })
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
    <div className="grid gap-4 xl:grid-cols-[300px_minmax(0,1fr)]">
      <div className="space-y-3">
        <Button className="w-full" onClick={openCreateTypeDialog}>
          <Plus className="h-4 w-4" />
          New Dictionary Type
        </Button>
        <div className="grid gap-3 rounded-2xl border border-slate-200 bg-white p-3">
          <div className="text-sm font-semibold text-slate-900">
            System Enums
          </div>
          <div className="grid grid-cols-2 gap-2">
            <Button
              disabled={previewSystemEnumsMutation.isPending}
              onClick={() => previewSystemEnumsMutation.mutate()}
              size="sm"
              variant="outline"
            >
              <Search className="h-4 w-4" />
              Scan
            </Button>
            <Button
              disabled={importSystemEnumsMutation.isPending}
              onClick={() => importSystemEnumsMutation.mutate()}
              size="sm"
              variant="outline"
            >
              <Download className="h-4 w-4" />
              Import
            </Button>
          </div>
          {systemEnums.length > 0 ? (
            <div className="max-h-44 space-y-2 overflow-auto text-xs text-slate-600">
              {systemEnums.map((item) => (
                <div
                  className="rounded-xl border border-slate-100 bg-slate-50 p-2"
                  key={item.code}
                >
                  <div className="font-medium text-slate-800">{item.name}</div>
                  <div>{item.code}</div>
                  <div>
                    new {item.newItemCodes?.length ?? 0} / changed{' '}
                    {item.changedItemCodes?.length ?? 0} / disabled{' '}
                    {item.disabledItemCodes?.length ?? 0}
                  </div>
                </div>
              ))}
            </div>
          ) : null}
          {systemEnumImportResult ? (
            <p className="text-xs text-slate-500">
              Imported {systemEnumImportResult.createdTypes} types,{' '}
              {systemEnumImportResult.createdItems} new items,{' '}
              {systemEnumImportResult.updatedItems ?? 0} updated items,{' '}
              {systemEnumImportResult.disabledItems ?? 0} disabled items.
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
          <div className="flex flex-wrap gap-2">
            <Button
              disabled={!selectedType || selectedTypeIsSystem}
              onClick={openEditTypeDialog}
              variant="outline"
            >
              <Pencil className="h-4 w-4" />
              Edit Type
            </Button>
            <Button
              disabled={!selectedType || selectedTypeIsSystem}
              onClick={openCreateItemDialog}
            >
              <Plus className="h-4 w-4" />
              New Item
            </Button>
            <Button
              disabled={
                !selectedType ||
                selectedTypeIsSystem ||
                toggleTypeMutation.isPending
              }
              onClick={() =>
                selectedType && toggleTypeMutation.mutate(selectedType)
              }
              variant="outline"
            >
              {selectedType?.status === 'enabled'
                ? 'Disable Type'
                : 'Enable Type'}
            </Button>
            <Button
              disabled={!selectedType || refreshCacheMutation.isPending}
              onClick={() => refreshCacheMutation.mutate()}
              variant="outline"
            >
              <RefreshCw className="h-4 w-4" />
              Refresh Cache
            </Button>
          </div>
        }
        description={
          selectedType
            ? `${selectedType.code} / ${selectedType.hierarchical ? 'tree' : 'flat'} / sort ${
                selectedType.sortOrder ?? 0
              }`
            : 'Select a dictionary type to manage items.'
        }
        title={selectedType?.name ?? 'Dictionary Items'}
      >
        <DictItemTable
          isLoading={itemsQuery.isLoading}
          items={treeItems}
          onDelete={
            selectedTypeIsSystem
              ? undefined
              : (item) => deleteItemMutation.mutate(item)
          }
          onEdit={selectedTypeIsSystem ? undefined : openEditItemDialog}
          onToggle={
            selectedTypeIsSystem
              ? undefined
              : (item) => toggleItemMutation.mutate(item)
          }
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
              {editingType ? 'Edit Dictionary Type' : 'New Dictionary Type'}
            </h2>
            <div className="mt-5 grid gap-4">
              <label className="grid gap-2 text-sm font-medium text-slate-700">
                Code
                <Input
                  disabled={Boolean(editingType)}
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
                Name
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
                Category
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
              <label className="grid gap-2 text-sm font-medium text-slate-700">
                Sort Order
                <Input
                  min={0}
                  onChange={(event) =>
                    setTypeDraft((current) => ({
                      ...current,
                      sortOrder: Number(event.target.value),
                    }))
                  }
                  type="number"
                  value={typeDraft.sortOrder ?? 0}
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
                Tree dictionary
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
                Cache runtime queries
              </label>
            </div>
            <div className="mt-6 flex justify-end gap-3">
              <Button
                onClick={() => setTypeDialogOpen(false)}
                variant="outline"
              >
                Cancel
              </Button>
              <Button disabled={saveTypeMutation.isPending} type="submit">
                Save
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
              {editingItem ? 'Edit Dictionary Item' : 'New Dictionary Item'}
            </h2>
            <div className="mt-5 grid gap-4">
              <label className="grid gap-2 text-sm font-medium text-slate-700">
                Code
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
                Label
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
                Runtime Value
                <Input
                  onChange={(event) =>
                    setItemDraft((current) => ({
                      ...current,
                      value: event.target.value,
                    }))
                  }
                  required
                  value={itemDraft.value}
                />
              </label>
              {selectedType?.hierarchical ? (
                <label className="grid gap-2 text-sm font-medium text-slate-700">
                  Parent
                  <select
                    className="h-10 rounded-xl border border-slate-200 bg-white px-3 text-sm"
                    onChange={(event) =>
                      setItemDraft((current) => ({
                        ...current,
                        parentId: event.target.value || undefined,
                      }))
                    }
                    value={itemDraft.parentId ?? ''}
                  >
                    <option value="">Root</option>
                    {flatItems
                      .filter((item) => item.id && item.id !== editingItem?.id)
                      .map((item) => (
                        <option key={item.id} value={item.id}>
                          {item.label}
                        </option>
                      ))}
                  </select>
                </label>
              ) : null}
              <label className="grid gap-2 text-sm font-medium text-slate-700">
                Sort Order
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
              <label className="flex items-center gap-2 text-sm text-slate-700">
                <input
                  checked={itemDraft.defaultItem ?? false}
                  onChange={(event) =>
                    setItemDraft((current) => ({
                      ...current,
                      defaultItem: event.target.checked,
                    }))
                  }
                  type="checkbox"
                />
                Default item
              </label>
              <label className="grid gap-2 text-sm font-medium text-slate-700">
                Extension JSON
                <textarea
                  className="min-h-24 rounded-xl border border-slate-200 bg-white px-3 py-2 text-sm text-slate-900 shadow-sm outline-none focus:border-sky-400 focus:ring-2 focus:ring-sky-100"
                  onChange={(event) =>
                    setItemDraft((current) => ({
                      ...current,
                      extensionJson: event.target.value,
                    }))
                  }
                  placeholder='{"color":"#2563eb"}'
                  value={itemDraft.extensionJson ?? ''}
                />
              </label>
            </div>
            <div className="mt-6 flex justify-end gap-3">
              <Button
                onClick={() => setItemDialogOpen(false)}
                variant="outline"
              >
                Cancel
              </Button>
              <Button disabled={saveItemMutation.isPending} type="submit">
                Save
              </Button>
            </div>
          </form>
        </div>
      ) : null}
    </div>
  )
}
