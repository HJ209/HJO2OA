import { del, get, post } from '@/services/request'
import { buildListParams } from '@/features/infra-admin/services/service-utils'
import type {
  AttachmentAsset,
  InfraListQuery,
  InfraPageData,
} from '@/features/infra-admin/types/infra'

const BASE_URL = '/v1/infra/attachments/assets'

export const attachmentService = {
  list(query?: InfraListQuery): Promise<InfraPageData<AttachmentAsset>> {
    return get(BASE_URL, { params: buildListParams(query) })
  },
  create(payload: AttachmentAsset): Promise<AttachmentAsset> {
    return post(BASE_URL, payload, {
      dedupeKey: `attachment:create:${payload.id}`,
    })
  },
  remove(id: string): Promise<void> {
    return del(`${BASE_URL}/${id}`, { dedupeKey: `attachment:delete:${id}` })
  },
}
