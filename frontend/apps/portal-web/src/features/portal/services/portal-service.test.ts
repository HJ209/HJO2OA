import { beforeEach, describe, expect, it, vi } from 'vitest'
import { get, post, put } from '@/services/request'
import {
  fetchPortalTemplateCanvas,
  listPortalTemplates,
  publishDesignerTemplate,
  resetPersonalizationProfile,
  saveDesignerDraft,
  savePersonalizationProfile,
  upsertWidgetDefinition,
} from '@/features/portal/services/portal-service'

vi.mock('@/services/request', () => ({
  get: vi.fn(),
  post: vi.fn(),
  put: vi.fn(),
}))

const mockedGet = vi.mocked(get)
const mockedPost = vi.mocked(post)
const mockedPut = vi.mocked(put)

beforeEach(() => {
  mockedGet.mockReset()
  mockedPost.mockReset()
  mockedPut.mockReset()
})

describe('portal-service', () => {
  it('calls portal-model list and canvas endpoints', async () => {
    mockedGet.mockResolvedValueOnce([])
    mockedGet.mockResolvedValueOnce({ pages: [] })

    await listPortalTemplates({ sceneType: 'HOME' })
    await fetchPortalTemplateCanvas('template-1')

    expect(mockedGet).toHaveBeenNthCalledWith(1, '/v1/portal/model/templates', {
      params: new URLSearchParams('sceneType=HOME'),
    })
    expect(mockedGet).toHaveBeenNthCalledWith(
      2,
      '/v1/portal/model/templates/template-1/canvas',
    )
  })

  it('saves widget config through the real upsert endpoint', async () => {
    mockedPut.mockResolvedValueOnce({ widgetId: 'widget-1' })

    await upsertWidgetDefinition('widget-1', {
      widgetCode: 'todo-card',
      displayName: '待办卡片',
      cardType: 'TODO',
      sceneType: 'HOME',
      sourceModule: 'todo-center',
      dataSourceType: 'AGGREGATION_QUERY',
      allowHide: true,
      allowCollapse: true,
      maxItems: 10,
    })

    expect(mockedPut).toHaveBeenCalledWith(
      '/v1/portal/widget-config/widgets/widget-1',
      expect.objectContaining({
        widgetCode: 'todo-card',
        dataSourceType: 'AGGREGATION_QUERY',
      }),
      expect.objectContaining({
        dedupeKey: 'portal-widget-upsert:widget-1',
      }),
    )
  })

  it('saves and publishes designer drafts via backend APIs', async () => {
    mockedPut.mockResolvedValue({})

    await saveDesignerDraft('template-1', { pages: [] })
    await publishDesignerTemplate('template-1', 3)

    expect(mockedPut).toHaveBeenNthCalledWith(
      1,
      '/v1/portal/designer/templates/template-1/draft',
      { pages: [] },
      expect.objectContaining({
        dedupeKey: 'portal-designer-draft:template-1',
      }),
    )
    expect(mockedPut).toHaveBeenNthCalledWith(
      2,
      '/v1/portal/designer/templates/template-1/publish',
      { versionNo: 3 },
      expect.objectContaining({
        dedupeKey: 'portal-designer-publish:template-1:3',
      }),
    )
  })

  it('persists and resets personalization against profile endpoints', async () => {
    mockedPost.mockResolvedValue({})

    await savePersonalizationProfile({
      sceneType: 'HOME',
      scope: 'ASSIGNMENT',
      assignmentId: 'assign-1',
      themeCode: 'compact',
      widgetOrderOverride: ['placement-2', 'placement-1'],
      hiddenPlacementCodes: ['placement-3'],
      quickAccessEntries: [],
    })
    await resetPersonalizationProfile({
      sceneType: 'HOME',
      scope: 'ASSIGNMENT',
      assignmentId: 'assign-1',
    })

    expect(mockedPost).toHaveBeenNthCalledWith(
      1,
      '/v1/portal/personalization/profile',
      expect.objectContaining({
        widgetOrderOverride: ['placement-2', 'placement-1'],
      }),
      expect.objectContaining({
        dedupeKey: 'portal-personalization-save:HOME:ASSIGNMENT',
      }),
    )
    expect(mockedPost).toHaveBeenNthCalledWith(
      2,
      '/v1/portal/personalization/profile/reset',
      expect.objectContaining({
        sceneType: 'HOME',
        scope: 'ASSIGNMENT',
      }),
      expect.objectContaining({
        dedupeKey: 'portal-personalization-reset:HOME:ASSIGNMENT',
      }),
    )
  })
})
