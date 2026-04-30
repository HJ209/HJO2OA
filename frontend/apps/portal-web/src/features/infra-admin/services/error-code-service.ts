import { get, post, put } from '@/services/request'
import {
  buildListParams,
  toPageData,
} from '@/features/infra-admin/services/service-utils'
import type {
  ErrorCodeDefinition,
  ErrorSeverity,
  InfraListQuery,
  InfraPageData,
} from '@/features/infra-admin/types/infra'

const BASE_URL = '/v1/infra/error-codes'

export interface ErrorCodeQuery extends InfraListQuery {
  moduleCode?: string
  severity?: ErrorSeverity
  deprecated?: boolean
}

export interface ErrorCodeSaveRequest {
  code: string
  moduleCode: string
  category?: string | null
  severity: ErrorSeverity
  httpStatus: number
  messageKey: string
  retryable?: boolean
}

export type ErrorCodeUpdateRequest = Omit<
  ErrorCodeSaveRequest,
  'code' | 'moduleCode'
>

function filterDefinitions(
  items: ErrorCodeDefinition[],
  query: ErrorCodeQuery,
): ErrorCodeDefinition[] {
  const keyword = query.keyword?.trim().toLowerCase()

  if (!keyword) {
    return items
  }

  return items.filter((item) =>
    [
      item.code,
      item.moduleCode,
      item.category ?? '',
      item.messageKey,
      item.message,
      item.severity,
      String(item.httpStatus),
    ].some((value) => value.toLowerCase().includes(keyword)),
  )
}

function buildErrorCodeParams(query: ErrorCodeQuery): URLSearchParams {
  const params = buildListParams(query)

  if (query.moduleCode) {
    params.set('moduleCode', query.moduleCode)
  }

  if (query.severity) {
    params.set('severity', query.severity)
  }

  if (typeof query.deprecated === 'boolean') {
    params.set('deprecated', String(query.deprecated))
  }

  return params
}

export const errorCodeService = {
  async list(
    query: ErrorCodeQuery = {},
  ): Promise<InfraPageData<ErrorCodeDefinition>> {
    const items = await get<ErrorCodeDefinition[]>(BASE_URL, {
      params: buildErrorCodeParams(query),
    })

    return toPageData(filterDefinitions(items, query), query)
  },
  create(payload: ErrorCodeSaveRequest): Promise<ErrorCodeDefinition> {
    return post<ErrorCodeDefinition, ErrorCodeSaveRequest>(BASE_URL, payload, {
      dedupeKey: `error-code:create:${payload.code}`,
    })
  },
  update(
    id: string,
    payload: ErrorCodeUpdateRequest,
  ): Promise<ErrorCodeDefinition> {
    return put<ErrorCodeDefinition, ErrorCodeUpdateRequest>(
      `${BASE_URL}/${id}`,
      payload,
      { dedupeKey: `error-code:update:${id}` },
    )
  },
  deprecate(id: string): Promise<ErrorCodeDefinition> {
    return put<ErrorCodeDefinition, undefined>(
      `${BASE_URL}/${id}/deprecate`,
      undefined,
      { dedupeKey: `error-code:deprecate:${id}` },
    )
  },
  getByCode(code: string): Promise<ErrorCodeDefinition> {
    return get<ErrorCodeDefinition>(`${BASE_URL}/code/${code}`)
  },
}
