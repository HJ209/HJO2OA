import apiClient from '@/services/api-client'
import { get, post } from '@/services/request'
import {
  resolveCurrentTenantId,
  toPageData,
} from '@/features/infra-admin/services/service-utils'
import type {
  AttachmentAsset,
  AttachmentBinding,
  AttachmentPreview,
  AttachmentVersion,
  InfraListQuery,
  InfraPageData,
} from '@/features/infra-admin/types/infra'

const BASE_URL = '/v1/infra/attachments'

export const attachmentService = {
  async list(query?: InfraListQuery): Promise<InfraPageData<AttachmentAsset>> {
    const tenantId = await resolveCurrentTenantId()
    const params = new URLSearchParams()
    params.set('tenantId', tenantId)
    const items = await get<AttachmentAsset[]>(BASE_URL, { params })

    return toPageData(items, query)
  },
  upload(payload: {
    file: File
    businessType?: string
    businessId?: string
    bindingRole?: AttachmentBinding['bindingRole']
  }): Promise<AttachmentAsset> {
    const formData = new FormData()
    formData.set('file', payload.file)
    const params = new URLSearchParams()

    if (payload.businessType) {
      params.set('businessType', payload.businessType)
    }

    if (payload.businessId) {
      params.set('businessId', payload.businessId)
    }

    params.set('bindingRole', payload.bindingRole ?? 'ATTACHMENT')

    return post<AttachmentAsset, FormData>(BASE_URL, formData, {
      params,
      dedupeKey: `attachment:upload:${payload.file.name}:${payload.file.size}`,
    })
  },
  uploadVersion(id: string, file: File): Promise<AttachmentAsset> {
    const formData = new FormData()
    formData.set('file', file)

    return post<AttachmentAsset, FormData>(
      `${BASE_URL}/${id}/versions`,
      formData,
      {
        dedupeKey: `attachment:version:${id}:${file.name}:${file.size}`,
      },
    )
  },
  bind(
    id: string,
    payload: {
      businessType: string
      businessId: string
      bindingRole: AttachmentBinding['bindingRole']
    },
  ): Promise<AttachmentAsset> {
    return post<AttachmentAsset, typeof payload>(
      `${BASE_URL}/${id}/bindings`,
      payload,
      {
        dedupeKey: `attachment:bind:${id}:${payload.businessType}:${payload.businessId}`,
      },
    )
  },
  versions(id: string): Promise<AttachmentVersion[]> {
    return get(`${BASE_URL}/${id}/versions`)
  },
  preview(id: string): Promise<AttachmentPreview> {
    return get(`${BASE_URL}/${id}/preview`)
  },
  async download(id: string, versionNo?: number): Promise<void> {
    const url =
      versionNo === undefined
        ? `${BASE_URL}/${id}/download`
        : `${BASE_URL}/${id}/versions/${versionNo}/download`
    const response = await apiClient.get(url, {
      responseType: 'blob',
    })
    const blob = (response.data as { data: Blob }).data
    const objectUrl = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = objectUrl
    link.download = `attachment-${id}${versionNo ? `-v${versionNo}` : ''}`
    document.body.append(link)
    link.click()
    link.remove()
    URL.revokeObjectURL(objectUrl)
  },
}
