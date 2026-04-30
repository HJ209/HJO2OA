import { get, post } from '@/services/request'
import type {
  FormMetadataDetail,
  FormRenderSchema,
  FormSubmission,
  MetadataSnapshotRequest,
  RenderedForm,
} from '@/features/workflow/types/form'

const FORM_API = '/v1/form'
const RENDERER_API = '/v1/form/renderer'

function idempotencyKey(scope: string): string {
  if (typeof crypto !== 'undefined' && crypto.randomUUID) {
    return `${scope}-${crypto.randomUUID()}`
  }

  return `${scope}-${Date.now()}-${Math.random().toString(16).slice(2)}`
}

export interface CreateFormMetadataRequest {
  code: string
  name: string
  nameI18nKey?: string
  fieldSchema: FormMetadataDetail['fieldSchema']
  layout: unknown
  validations?: unknown
  fieldPermissionMap?: unknown
  tenantId: string
}

export interface RenderFormRequest {
  metadataSnapshot: MetadataSnapshotRequest
  processInstanceId?: string
  formDataId?: string
  nodeId?: string
  locale: string
  fallbackLocale?: string
  formData: Record<string, unknown>
  validateData?: boolean
}

export const formService = {
  createMetadata(
    payload: CreateFormMetadataRequest,
  ): Promise<FormMetadataDetail> {
    return post<FormMetadataDetail, CreateFormMetadataRequest>(
      `${FORM_API}/metadata`,
      payload,
      {
        dedupeKey: `form:metadata:create:${payload.tenantId}:${payload.code}`,
        idempotencyKey: idempotencyKey('form-metadata-create'),
      },
    )
  },

  publishMetadata(metadataId: string): Promise<FormMetadataDetail> {
    return post<FormMetadataDetail, Record<string, never>>(
      `${FORM_API}/metadata/${metadataId}/publish`,
      {},
      {
        dedupeKey: `form:metadata:publish:${metadataId}`,
        idempotencyKey: idempotencyKey('form-metadata-publish'),
      },
    )
  },

  getLatestRenderSchema(
    code: string,
    tenantId: string,
  ): Promise<FormRenderSchema> {
    return get<FormRenderSchema>(`${FORM_API}/render-schemas/${code}`, {
      params: { tenantId },
    })
  },

  render(payload: RenderFormRequest): Promise<RenderedForm> {
    return post<RenderedForm, RenderFormRequest>(
      `${RENDERER_API}/render`,
      payload,
      {
        dedupeKey: `form:renderer:render:${payload.metadataSnapshot.metadataId}:${payload.nodeId ?? ''}`,
      },
    )
  },

  createDraft(payload: {
    metadataSnapshot: MetadataSnapshotRequest
    processInstanceId?: string
    formDataId?: string
    nodeId?: string
    formData: Record<string, unknown>
    submittedBy: string
  }): Promise<FormSubmission> {
    return post<FormSubmission, typeof payload>(
      `${RENDERER_API}/submissions/drafts`,
      payload,
      {
        dedupeKey: `form:submission:draft:${payload.metadataSnapshot.metadataId}:${payload.submittedBy}`,
        idempotencyKey: idempotencyKey('form-submission-draft'),
      },
    )
  },

  submitDraft(
    submissionId: string,
    payload: {
      metadataSnapshot: MetadataSnapshotRequest
      formData: Record<string, unknown>
    },
  ): Promise<FormSubmission> {
    return post<FormSubmission, typeof payload>(
      `${RENDERER_API}/submissions/drafts/${submissionId}/submit`,
      payload,
      {
        dedupeKey: `form:submission:submit:${submissionId}`,
        idempotencyKey: idempotencyKey('form-submission-submit'),
      },
    )
  },
}

export function toMetadataSnapshot(
  schema: FormRenderSchema,
): MetadataSnapshotRequest {
  return {
    metadataId: schema.metadataId,
    code: schema.code,
    name: schema.name,
    version: schema.version,
    fields: schema.fieldSchema,
    layout: schema.layout,
    validations: Array.isArray(schema.validations) ? schema.validations : [],
    fieldPermissionMap: schema.fieldPermissionMap,
    tenantId: schema.tenantId,
  }
}
