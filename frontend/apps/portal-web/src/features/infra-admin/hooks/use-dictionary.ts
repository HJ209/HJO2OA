import { useQuery } from '@tanstack/react-query'
import { dictionaryService } from '@/features/infra-admin/services/dictionary-service'
import type { InfraListQuery } from '@/features/infra-admin/types/infra'

export function useDictionaryTypes(query?: InfraListQuery) {
  return useQuery({
    queryKey: ['infra', 'dictionary-types', query],
    queryFn: () => dictionaryService.listTypes(query),
  })
}

export function useDictionaryItems(typeCode: string, query?: InfraListQuery) {
  return useQuery({
    enabled: typeCode.length > 0,
    queryKey: ['infra', 'dictionary-items', typeCode, query],
    queryFn: () => dictionaryService.listItems(typeCode, query),
  })
}

export function useDictionaryOptions(typeCode: string) {
  return useQuery({
    enabled: typeCode.length > 0,
    queryKey: ['infra', 'dictionary-options', typeCode],
    queryFn: async () => {
      const items = await dictionaryService.listRuntimeItems(typeCode, {
        enabledOnly: true,
      })

      return items
        .filter((item) => item.enabled)
        .sort((left, right) => left.sortOrder - right.sortOrder)
        .map((item) => ({
          label: item.label,
          value: item.value,
          code: item.code,
        }))
    },
    staleTime: 5 * 60 * 1000,
  })
}

export function useDictionaryTree(typeCode: string) {
  return useQuery({
    enabled: typeCode.length > 0,
    queryKey: ['infra', 'dictionary-tree', typeCode],
    queryFn: () => dictionaryService.tree(typeCode, { enabledOnly: false }),
  })
}

export function useSystemEnumOptions(className: string) {
  return useQuery({
    enabled: className.length > 0,
    queryKey: ['infra', 'system-enum-options', className],
    queryFn: async () => {
      const systemEnums = await dictionaryService.previewSystemEnums()
      const systemEnum = systemEnums.find(
        (item) => item.className === className,
      )

      if (!systemEnum) {
        return []
      }

      try {
        const page = await dictionaryService.listItems(systemEnum.code, {
          page: 1,
          size: 200,
        })

        return page.items
          .filter((item) => item.enabled)
          .sort((left, right) => left.sortOrder - right.sortOrder)
          .map((item) => ({
            label: item.label,
            value: item.code,
            code: item.code,
          }))
      } catch {
        return systemEnum.items
          .sort((left, right) => left.sortOrder - right.sortOrder)
          .map((item) => ({
            label: item.name,
            value: item.code,
            code: item.code,
          }))
      }
    },
    staleTime: 5 * 60 * 1000,
  })
}
