import { get, post, put } from '@/services/request'
import type {
  ActivatePortalPublicationRequest,
  CreatePortalTemplateRequest,
  PersonalizationProfileView,
  PortalClientType,
  PortalDesignerTemplateInitializationView,
  PortalDesignerTemplatePreviewView,
  PortalDesignerTemplatePublicationView,
  PortalDesignerTemplateStatusView,
  PortalHomePageView,
  PortalHomeRefreshState,
  PortalPublicationStatus,
  PortalPublicationView,
  PortalSceneType,
  PortalTemplateCanvasView,
  PortalTemplateView,
  ResetPersonalizationProfileRequest,
  SavePersonalizationProfileRequest,
  SavePortalTemplateCanvasRequest,
  UpsertWidgetDefinitionRequest,
  WidgetDefinitionStatus,
  WidgetDefinitionView,
} from '@/features/portal/types/portal'

interface ListPortalTemplateQuery {
  sceneType?: PortalSceneType
}

interface ListWidgetQuery {
  sceneType?: PortalSceneType
  status?: WidgetDefinitionStatus
}

interface ListPublicationQuery {
  sceneType?: PortalSceneType
  clientType?: PortalClientType
  status?: PortalPublicationStatus
}

interface PreviewDesignerTemplateQuery {
  clientType?: PortalClientType
  tenantId?: string
  personId?: string
  accountId?: string
  assignmentId?: string
  positionId?: string
}

function compactParams<TValues extends object>(
  values: TValues,
): URLSearchParams {
  const params = new URLSearchParams()

  for (const [key, value] of Object.entries(values)) {
    if (value !== undefined && value !== null && value !== '') {
      params.set(key, String(value))
    }
  }

  return params
}

function withParams(url: string, params: URLSearchParams): string {
  const queryString = params.toString()

  return queryString ? `${url}?${queryString}` : url
}

export function fetchPortalHomePage(
  sceneType: PortalSceneType = 'HOME',
): Promise<PortalHomePageView> {
  return get<PortalHomePageView>('/v1/portal/home/page', {
    params: compactParams({ sceneType }),
  })
}

export function fetchPortalHomeRefreshState(
  sceneType: PortalSceneType = 'HOME',
): Promise<PortalHomeRefreshState> {
  return get<PortalHomeRefreshState>('/v1/portal/home/refresh-state', {
    params: compactParams({ sceneType }),
  })
}

export function listPortalTemplates(
  query: ListPortalTemplateQuery = {},
): Promise<PortalTemplateView[]> {
  return get<PortalTemplateView[]>('/v1/portal/model/templates', {
    params: compactParams(query),
  })
}

export function createPortalTemplate(
  body: CreatePortalTemplateRequest,
): Promise<PortalTemplateView> {
  return post<PortalTemplateView, CreatePortalTemplateRequest>(
    '/v1/portal/model/templates',
    body,
    {
      dedupeKey: `portal-template-create:${body.templateId}`,
      idempotencyKey: `portal-template-create:${body.templateId}`,
    },
  )
}

export function fetchPortalTemplate(
  templateId: string,
): Promise<PortalTemplateView> {
  return get<PortalTemplateView>(`/v1/portal/model/templates/${templateId}`)
}

export function fetchPortalTemplateCanvas(
  templateId: string,
): Promise<PortalTemplateCanvasView> {
  return get<PortalTemplateCanvasView>(
    `/v1/portal/model/templates/${templateId}/canvas`,
  )
}

export function savePortalTemplateCanvas(
  templateId: string,
  body: SavePortalTemplateCanvasRequest,
): Promise<PortalTemplateCanvasView> {
  return put<PortalTemplateCanvasView, SavePortalTemplateCanvasRequest>(
    `/v1/portal/model/templates/${templateId}/canvas`,
    body,
    {
      dedupeKey: `portal-template-canvas:${templateId}`,
      idempotencyKey: `portal-template-canvas:${templateId}:${Date.now()}`,
    },
  )
}

export function publishPortalTemplateVersion(
  templateId: string,
  versionNo: number,
): Promise<PortalTemplateView> {
  return put<PortalTemplateView, { versionNo: number }>(
    `/v1/portal/model/templates/${templateId}/publish`,
    { versionNo },
    {
      dedupeKey: `portal-template-publish:${templateId}:${versionNo}`,
      idempotencyKey: `portal-template-publish:${templateId}:${versionNo}`,
    },
  )
}

export function deprecatePortalTemplateVersion(
  templateId: string,
  versionNo: number,
): Promise<PortalTemplateView> {
  return post<PortalTemplateView, Record<string, never>>(
    `/v1/portal/model/templates/${templateId}/versions/${versionNo}/deprecate`,
    {},
    {
      dedupeKey: `portal-template-deprecate:${templateId}:${versionNo}`,
      idempotencyKey: `portal-template-deprecate:${templateId}:${versionNo}`,
    },
  )
}

export function listPortalPublications(
  query: ListPublicationQuery = {},
): Promise<PortalPublicationView[]> {
  return get<PortalPublicationView[]>('/v1/portal/model/publications', {
    params: compactParams(query),
  })
}

export function fetchActivePortalPublication(
  sceneType: PortalSceneType,
  clientType: PortalClientType,
): Promise<PortalPublicationView> {
  return get<PortalPublicationView>('/v1/portal/model/publications/active', {
    params: compactParams({ sceneType, clientType }),
  })
}

export function activatePortalPublication(
  publicationId: string,
  body: ActivatePortalPublicationRequest,
): Promise<PortalPublicationView> {
  return put<PortalPublicationView, ActivatePortalPublicationRequest>(
    `/v1/portal/model/publications/${publicationId}/activate`,
    body,
    {
      dedupeKey: `portal-publication-activate:${publicationId}`,
      idempotencyKey: `portal-publication-activate:${publicationId}:${Date.now()}`,
    },
  )
}

export function offlinePortalPublication(
  publicationId: string,
): Promise<PortalPublicationView> {
  return post<PortalPublicationView, Record<string, never>>(
    `/v1/portal/model/publications/${publicationId}/offline`,
    {},
    {
      dedupeKey: `portal-publication-offline:${publicationId}`,
      idempotencyKey: `portal-publication-offline:${publicationId}`,
    },
  )
}

export function listWidgetDefinitions(
  query: ListWidgetQuery = {},
): Promise<WidgetDefinitionView[]> {
  return get<WidgetDefinitionView[]>('/v1/portal/widget-config/widgets', {
    params: compactParams(query),
  })
}

export function upsertWidgetDefinition(
  widgetId: string,
  body: UpsertWidgetDefinitionRequest,
): Promise<WidgetDefinitionView> {
  return put<WidgetDefinitionView, UpsertWidgetDefinitionRequest>(
    `/v1/portal/widget-config/widgets/${widgetId}`,
    body,
    {
      dedupeKey: `portal-widget-upsert:${widgetId}`,
      idempotencyKey: `portal-widget-upsert:${widgetId}:${Date.now()}`,
    },
  )
}

export function disableWidgetDefinition(
  widgetId: string,
): Promise<WidgetDefinitionView> {
  return post<WidgetDefinitionView, Record<string, never>>(
    `/v1/portal/widget-config/widgets/${widgetId}/disable`,
    {},
    {
      dedupeKey: `portal-widget-disable:${widgetId}`,
      idempotencyKey: `portal-widget-disable:${widgetId}`,
    },
  )
}

export function fetchPersonalizationProfile(
  sceneType: PortalSceneType = 'HOME',
): Promise<PersonalizationProfileView> {
  return get<PersonalizationProfileView>('/v1/portal/personalization/profile', {
    params: compactParams({ sceneType }),
  })
}

export function savePersonalizationProfile(
  body: SavePersonalizationProfileRequest,
): Promise<PersonalizationProfileView> {
  return post<PersonalizationProfileView, SavePersonalizationProfileRequest>(
    '/v1/portal/personalization/profile',
    body,
    {
      dedupeKey: `portal-personalization-save:${body.sceneType}:${body.scope}`,
      idempotencyKey: `portal-personalization-save:${body.sceneType}:${body.scope}:${Date.now()}`,
    },
  )
}

export function resetPersonalizationProfile(
  body: ResetPersonalizationProfileRequest,
): Promise<PersonalizationProfileView> {
  return post<PersonalizationProfileView, ResetPersonalizationProfileRequest>(
    '/v1/portal/personalization/profile/reset',
    body,
    {
      dedupeKey: `portal-personalization-reset:${body.sceneType}:${body.scope}`,
      idempotencyKey: `portal-personalization-reset:${body.sceneType}:${body.scope}:${Date.now()}`,
    },
  )
}

export function listDesignerTemplates(
  query: ListPortalTemplateQuery = {},
): Promise<PortalDesignerTemplateStatusView[]> {
  return get<PortalDesignerTemplateStatusView[]>(
    '/v1/portal/designer/templates',
    {
      params: compactParams(query),
    },
  )
}

export function initializeDesignerTemplate(
  templateId: string,
): Promise<PortalDesignerTemplateInitializationView> {
  return get<PortalDesignerTemplateInitializationView>(
    `/v1/portal/designer/templates/${templateId}/init`,
  )
}

export function saveDesignerDraft(
  templateId: string,
  body: SavePortalTemplateCanvasRequest,
): Promise<PortalDesignerTemplateInitializationView> {
  return put<
    PortalDesignerTemplateInitializationView,
    SavePortalTemplateCanvasRequest
  >(`/v1/portal/designer/templates/${templateId}/draft`, body, {
    dedupeKey: `portal-designer-draft:${templateId}`,
    idempotencyKey: `portal-designer-draft:${templateId}:${Date.now()}`,
  })
}

export function publishDesignerTemplate(
  templateId: string,
  versionNo: number,
): Promise<PortalDesignerTemplateStatusView> {
  return put<PortalDesignerTemplateStatusView, { versionNo: number }>(
    `/v1/portal/designer/templates/${templateId}/publish`,
    { versionNo },
    {
      dedupeKey: `portal-designer-publish:${templateId}:${versionNo}`,
      idempotencyKey: `portal-designer-publish:${templateId}:${versionNo}`,
    },
  )
}

export function previewDesignerTemplate(
  templateId: string,
  query: PreviewDesignerTemplateQuery = {},
): Promise<PortalDesignerTemplatePreviewView> {
  return get<PortalDesignerTemplatePreviewView>(
    withParams(
      `/v1/portal/designer/templates/${templateId}/preview`,
      compactParams(query),
    ),
  )
}

export function listDesignerPublications(
  templateId: string,
  query: Pick<ListPublicationQuery, 'clientType' | 'status'> = {},
): Promise<PortalDesignerTemplatePublicationView[]> {
  return get<PortalDesignerTemplatePublicationView[]>(
    `/v1/portal/designer/templates/${templateId}/publications`,
    {
      params: compactParams(query),
    },
  )
}

export function activateDesignerPublication(
  templateId: string,
  publicationId: string,
  clientType: PortalClientType,
): Promise<PortalDesignerTemplateStatusView> {
  return put<
    PortalDesignerTemplateStatusView,
    { clientType: PortalClientType }
  >(
    `/v1/portal/designer/templates/${templateId}/publications/${publicationId}/activate`,
    { clientType },
    {
      dedupeKey: `portal-designer-activate:${publicationId}`,
      idempotencyKey: `portal-designer-activate:${publicationId}:${Date.now()}`,
    },
  )
}

export function offlineDesignerPublication(
  templateId: string,
  publicationId: string,
): Promise<PortalDesignerTemplateStatusView> {
  return post<PortalDesignerTemplateStatusView, Record<string, never>>(
    `/v1/portal/designer/templates/${templateId}/publications/${publicationId}/offline`,
    {},
    {
      dedupeKey: `portal-designer-offline:${publicationId}`,
      idempotencyKey: `portal-designer-offline:${publicationId}`,
    },
  )
}
