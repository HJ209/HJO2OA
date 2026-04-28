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
