import { get, post, put } from '@/services/request'
import { buildListParams } from '@/features/infra-admin/services/service-utils'
import type {
  ErrorCodeDefinition,
  InfraListQuery,
  InfraPageData,
} from '@/features/infra-admin/types/infra'

const BASE_URL = '/v1/infra/error-codes/definitions'

export const errorCodeService = {
  list(query?: InfraListQuery): Promise<InfraPageData<ErrorCodeDefinition>> {
    return get(BASE_URL, { params: buildListParams(query) })
  },
  create(payload: ErrorCodeDefinition): Promise<ErrorCodeDefinition> {
    return post(BASE_URL, payload, {
      dedupeKey: `error-code:create:${payload.code}`,
    })
  },
  update(
    code: string,
    payload: ErrorCodeDefinition,
  ): Promise<ErrorCodeDefinition> {
    return put(`${BASE_URL}/${code}`, payload, {
      dedupeKey: `error-code:update:${code}`,
    })
  },
}
