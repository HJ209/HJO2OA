import type {
  FilterOperator,
  FilterValue,
  PaginationFilter,
  PaginationQuery,
  PaginationSort,
} from '@/types/api'

function normalizePage(page: number | undefined): string | undefined {
  if (page === undefined) {
    return undefined
  }

  return String(Math.max(1, page))
}

function normalizeSize(size: number | undefined): string | undefined {
  if (size === undefined) {
    return undefined
  }

  return String(Math.min(Math.max(1, size), 100))
}

function serializeSort(sort: PaginationSort[] | undefined): string | undefined {
  if (!sort || sort.length === 0) {
    return undefined
  }

  return sort
    .map((item) => `${item.field},${item.direction ?? 'asc'}`)
    .join(';')
}

function serializeFilterKey(
  field: string,
  operator: FilterOperator | undefined,
): string {
  if (!operator || operator === 'eq') {
    return `filter[${field}]`
  }

  return `filter[${field}]${operator}`
}

function serializeFilterValue(value: FilterValue): string {
  if (Array.isArray(value)) {
    return value.join(',')
  }

  if (value === null) {
    return 'true'
  }

  return String(value)
}

function appendFilters(
  searchParams: URLSearchParams,
  filters: PaginationFilter[] | undefined,
): void {
  filters?.forEach((filterItem) => {
    searchParams.set(
      serializeFilterKey(filterItem.field, filterItem.operator),
      serializeFilterValue(filterItem.value),
    )
  })
}

export function serializePaginationParams(
  query: PaginationQuery = {},
): URLSearchParams {
  const searchParams = new URLSearchParams()
  const page = normalizePage(query.page)
  const size = normalizeSize(query.size)
  const sort = serializeSort(query.sort)

  if (page) {
    searchParams.set('page', page)
  }

  if (size) {
    searchParams.set('size', size)
  }

  if (sort) {
    searchParams.set('sort', sort)
  }

  if (query.fields && query.fields.length > 0) {
    searchParams.set('fields', query.fields.join(','))
  }

  appendFilters(searchParams, query.filters)

  return searchParams
}
