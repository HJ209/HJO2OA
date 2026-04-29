import { del, get, post } from '@/services/request'
import {
  resolveCurrentTenantId,
  toPageData,
} from '@/features/infra-admin/services/service-utils'
import type {
  AttachmentAsset,
  InfraListQuery,
  InfraPageData,
} from '@/features/infra-admin/types/infra'

const BASE_URL = '/v1/infra/attachments'

interface BackendAttachmentAsset {
  id: string
  originalFilename: string
  contentType: string
  sizeBytes: number
  createdBy?: string | null
}

function mapAttachmentAsset(item: BackendAttachmentAsset): AttachmentAsset {
  return {
    id: item.id,
    fileName: item.originalFilename,
    contentType: item.contentType,
    sizeBytes: item.sizeBytes,
    owner: item.createdBy ?? undefined,
  }
}

export const attachmentService = {
  async list(query?: InfraListQuery): Promise<InfraPageData<AttachmentAsset>> {
    const tenantId = await resolveCurrentTenantId()
    const params = new URLSearchParams()
    params.set('tenantId', tenantId)
    const items = await get<BackendAttachmentAsset[]>(BASE_URL, { params })

    return toPageData(items.map(mapAttachmentAsset), query)
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
