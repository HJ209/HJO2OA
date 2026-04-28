export interface ApiErrorDetail {
  field?: string
  message: string
  rejectedValue?: unknown
}

export interface ResponseMeta {
  requestId: string
  success: boolean
  errorCode?: string
  timestamp?: string
  serverTimezone?: string
}

export interface ApiResponse<T> {
  data: T
  meta: ResponseMeta
  message?: string
  errors?: ApiErrorDetail[]
}

export interface BackendResponseMeta {
  requestId?: string
  success?: boolean
  errorCode?: string
  timestamp?: string
  serverTimezone?: string
}

export interface BackendApiResponse<T> {
  code?: string
  message?: string
  data: T
  errors?: ApiErrorDetail[]
  meta?: BackendResponseMeta
}

export interface Pagination {
  page: number
  size: number
  total: number
  totalPages: number
}

export interface PageData<T> {
  items: T[]
  pagination: Pagination
}

export type SortDirection = 'asc' | 'desc'

export type FilterOperator =
  | 'eq'
  | 'ne'
  | 'like'
  | 'start'
  | 'gt'
  | 'gte'
  | 'lt'
  | 'lte'
  | 'in'
  | 'null'

export type FilterValue =
  | string
  | number
  | boolean
  | Array<string | number | boolean>
  | null

export interface PaginationSort {
  field: string
  direction?: SortDirection
}

export interface PaginationFilter {
  field: string
  operator?: FilterOperator
  value: FilterValue
}

export interface PaginationQuery {
  page?: number
  size?: number
  fields?: string[]
  sort?: PaginationSort[]
  filters?: PaginationFilter[]
}
