import {
  useEffect,
  useMemo,
  useState,
  type DragEvent,
  type ReactElement,
} from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Eye, GripVertical, Play, Save, Trash2 } from 'lucide-react'
import { Button } from '@/components/ui/button'
import {
  activateDesignerPublication,
  initializeDesignerTemplate,
  listDesignerPublications,
  listDesignerTemplates,
  offlineDesignerPublication,
  previewDesignerTemplate,
  publishDesignerTemplate,
  saveDesignerDraft,
} from '@/features/portal/services/portal-service'
import { PortalRuntimeCard } from '@/features/portal-home/pages/portal-home-page'
import type {
  PortalDesignerTemplatePreviewView,
  PortalDesignerWidgetPaletteItemView,
  PortalLayoutRegionView,
  PortalPageView,
  PortalTemplateCanvasView,
  PortalWidgetPlacementView,
} from '@/features/portal/types/portal'
import {
  EmptyState,
  ErrorBanner,
  LoadingBlock,
  NoPermissionPanel,
  PortalPanel,
  StatusBadge,
  TextInput,
  ToggleField,
} from '@/features/portal/pages/portal-ui'

export default function PortalDesignerPanel({
  canManage,
}: {
  canManage: boolean
}): ReactElement {
  const queryClient = useQueryClient()
  const [selectedTemplateId, setSelectedTemplateId] = useState('')
  const templatesQuery = useQuery({
    queryKey: ['portal-designer', 'templates'],
    queryFn: () => listDesignerTemplates(),
  })
  const templates = useMemo(
    () => templatesQuery.data ?? [],
    [templatesQuery.data],
  )
  const selectedTemplate =
    templates.find((item) => item.templateId === selectedTemplateId) ??
    templates[0]
  const initQuery = useQuery({
    queryKey: ['portal-designer', 'init', selectedTemplate?.templateId],
    queryFn: () => initializeDesignerTemplate(selectedTemplate.templateId),
    enabled: Boolean(selectedTemplate),
  })
  const publicationsQuery = useQuery({
    queryKey: ['portal-designer', 'publications', selectedTemplate?.templateId],
    queryFn: () => listDesignerPublications(selectedTemplate.templateId),
    enabled: Boolean(selectedTemplate),
  })
  const [canvas, setCanvas] = useState<PortalTemplateCanvasView | null>(null)
  const palette = initQuery.data?.widgetPalette.widgets ?? []
  const activeWidgets = palette.filter((widget) => widget.status === 'ACTIVE')
  const canvasValidation = useMemo(() => validateCanvas(canvas), [canvas])

  useEffect(() => {
    if (!selectedTemplateId && templates[0]) {
      setSelectedTemplateId(templates[0].templateId)
    }
  }, [selectedTemplateId, templates])

  useEffect(() => {
    if (initQuery.data?.canvas) {
      setCanvas(cloneCanvas(initQuery.data.canvas))
    }
  }, [initQuery.data?.canvas])

  const invalidate = async (): Promise<void> => {
    await queryClient.invalidateQueries({ queryKey: ['portal-designer'] })
    await queryClient.invalidateQueries({ queryKey: ['portal-model'] })
    await queryClient.invalidateQueries({ queryKey: ['portal-home'] })
  }
  const saveMutation = useMutation({
    mutationFn: () =>
      saveDesignerDraft(selectedTemplate.templateId, {
        pages: normalizeCanvas(canvas).pages,
      }),
    onSuccess: async (payload) => {
      setCanvas(cloneCanvas(payload.canvas))
      await invalidate()
    },
  })
  const publishMutation = useMutation({
    mutationFn: () =>
      publishDesignerTemplate(
        selectedTemplate.templateId,
        selectedTemplate.latestVersionNo ?? canvas?.latestVersionNo ?? 1,
      ),
    onSuccess: invalidate,
  })
  const previewMutation = useMutation({
    mutationFn: () =>
      previewDesignerTemplate(selectedTemplate.templateId, {
        clientType: 'PC',
      }),
  })
  const activateMutation = useMutation({
    mutationFn: ({
      clientType,
      publicationId,
    }: {
      publicationId: string
      clientType: 'PC' | 'MOBILE' | 'ALL'
    }) =>
      activateDesignerPublication(
        selectedTemplate.templateId,
        publicationId,
        clientType,
      ),
    onSuccess: invalidate,
  })
  const offlineMutation = useMutation({
    mutationFn: (publicationId: string) =>
      offlineDesignerPublication(selectedTemplate.templateId, publicationId),
    onSuccess: invalidate,
  })

  if (!canManage) {
    return <NoPermissionPanel />
  }

  return (
    <div className="grid gap-4 2xl:grid-cols-[320px_minmax(0,1fr)_360px]">
      <div className="space-y-4">
        <PortalPanel
          title="模板"
          description="读取 designer template projection。"
        >
          {templatesQuery.isLoading ? <LoadingBlock /> : null}
          {templatesQuery.isError ? (
            <ErrorBanner error={templatesQuery.error} title="模板加载失败" />
          ) : null}
          {!templates.length && !templatesQuery.isLoading ? (
            <EmptyState
              title="暂无可设计模板"
              description="请先在 PortalModel 创建模板，designer 会消费投影。"
            />
          ) : null}
          <div className="space-y-2">
            {templates.map((template) => (
              <button
                className={`w-full rounded-xl border px-3 py-2 text-left text-sm ${
                  template.templateId === selectedTemplate?.templateId
                    ? 'border-sky-500 bg-sky-50'
                    : 'border-slate-200 bg-white'
                }`}
                key={template.templateId}
                onClick={() => setSelectedTemplateId(template.templateId)}
                type="button"
              >
                <span className="block font-medium text-slate-900">
                  {template.templateCode}
                </span>
                <span className="mt-1 flex gap-2">
                  <StatusBadge>{template.sceneType}</StatusBadge>
                  <StatusBadge
                    tone={template.hasActivePublication ? 'green' : 'amber'}
                  >
                    {template.hasActivePublication ? '已生效' : '未生效'}
                  </StatusBadge>
                </span>
              </button>
            ))}
          </div>
        </PortalPanel>

        <PortalPanel title="组件面板" description="拖拽 ACTIVE 卡片进入区域。">
          {initQuery.isLoading ? <LoadingBlock /> : null}
          {initQuery.isError ? (
            <ErrorBanner error={initQuery.error} title="设计器初始化失败" />
          ) : null}
          <div className="space-y-2">
            {activeWidgets.map((widget) => (
              <div
                className="cursor-grab rounded-xl border border-slate-200 bg-white p-3 text-sm shadow-sm active:cursor-grabbing"
                draggable
                key={widget.widgetId}
                onDragStart={(event) => {
                  event.dataTransfer.setData('widgetId', widget.widgetId)
                }}
              >
                <div className="flex items-center gap-2">
                  <GripVertical className="h-4 w-4 text-slate-400" />
                  <span className="font-medium text-slate-900">
                    {widget.displayName}
                  </span>
                </div>
                <div className="mt-2 flex flex-wrap gap-2">
                  <StatusBadge>{widget.cardType}</StatusBadge>
                  <StatusBadge>{widget.dataSourceType}</StatusBadge>
                </div>
              </div>
            ))}
          </div>
        </PortalPanel>
      </div>

      <PortalPanel
        title="画布"
        description="页面、区域和组件实例保存到 portal-designer draft API。"
        actions={
          <div className="flex flex-wrap gap-2">
            <Button
              disabled={
                saveMutation.isPending ||
                !canvas ||
                canvasValidation.status === 'invalid'
              }
              onClick={() => saveMutation.mutate()}
              size="sm"
            >
              <Save className="h-4 w-4" />
              保存
            </Button>
            <Button
              disabled={previewMutation.isPending || !canvas}
              onClick={() => previewMutation.mutate()}
              size="sm"
              variant="outline"
            >
              <Eye className="h-4 w-4" />
              预览
            </Button>
            <Button
              disabled={publishMutation.isPending || !selectedTemplate}
              onClick={() => publishMutation.mutate()}
              size="sm"
              variant="outline"
            >
              <Play className="h-4 w-4" />
              发布
            </Button>
          </div>
        }
      >
        {!canvas ? (
          <EmptyState
            title="尚未加载画布"
            description="选择模板后会调用设计器初始化接口加载草稿。"
          />
        ) : (
          <CanvasEditor
            activeWidgets={activeWidgets}
            canvas={canvas}
            onChange={setCanvas}
          />
        )}
        {canvasValidation.status === 'invalid' ? (
          <div className="mt-3 rounded-xl border border-amber-100 bg-amber-50 px-4 py-3 text-sm text-amber-700">
            {canvasValidation.message}
          </div>
        ) : null}
        {saveMutation.isError ? (
          <ErrorBanner error={saveMutation.error} title="草稿保存失败" />
        ) : null}
        {publishMutation.isError ? (
          <ErrorBanner error={publishMutation.error} title="模板发布失败" />
        ) : null}
      </PortalPanel>

      <div className="space-y-4">
        <PortalPanel
          title="预览"
          description="预览读取最近一次成功保存的草稿。"
        >
          {previewMutation.isError ? (
            <ErrorBanner error={previewMutation.error} title="预览失败" />
          ) : null}
          {previewMutation.data ? (
            <DesignerPreview preview={previewMutation.data} />
          ) : (
            <EmptyState
              title="尚未生成预览"
              description="保存草稿后点击预览，后端会返回可渲染页面结构。"
            />
          )}
        </PortalPanel>

        <PortalPanel
          title="发布记录"
          description="激活和下线通过 designer publication API。"
        >
          {publicationsQuery.isLoading ? <LoadingBlock /> : null}
          {publicationsQuery.data?.length ? (
            <div className="space-y-2">
              {publicationsQuery.data.map((publication) => (
                <div
                  className="rounded-xl border border-slate-200 bg-white p-3 text-sm"
                  key={publication.publicationId}
                >
                  <div className="flex flex-wrap items-center gap-2">
                    <span className="font-medium text-slate-900">
                      {publication.publicationId}
                    </span>
                    <StatusBadge
                      tone={publication.status === 'ACTIVE' ? 'green' : 'slate'}
                    >
                      {publication.status}
                    </StatusBadge>
                    <StatusBadge>{publication.clientType}</StatusBadge>
                  </div>
                  <div className="mt-3 flex gap-2">
                    <Button
                      disabled={activateMutation.isPending}
                      onClick={() =>
                        activateMutation.mutate({
                          publicationId: publication.publicationId,
                          clientType: publication.clientType,
                        })
                      }
                      size="sm"
                      variant="outline"
                    >
                      激活
                    </Button>
                    <Button
                      disabled={offlineMutation.isPending}
                      onClick={() =>
                        offlineMutation.mutate(publication.publicationId)
                      }
                      size="sm"
                      variant="outline"
                    >
                      下线
                    </Button>
                  </div>
                </div>
              ))}
            </div>
          ) : (
            <EmptyState
              title="暂无发布记录"
              description="发布模板后可在这里激活或下线。"
            />
          )}
          {activateMutation.isError ? (
            <ErrorBanner error={activateMutation.error} title="激活失败" />
          ) : null}
          {offlineMutation.isError ? (
            <ErrorBanner error={offlineMutation.error} title="下线失败" />
          ) : null}
        </PortalPanel>
      </div>
    </div>
  )
}

function CanvasEditor({
  activeWidgets,
  canvas,
  onChange,
}: {
  canvas: PortalTemplateCanvasView
  activeWidgets: PortalDesignerWidgetPaletteItemView[]
  onChange: (canvas: PortalTemplateCanvasView) => void
}): ReactElement {
  function updatePage(pageIndex: number, patch: Partial<PortalPageView>): void {
    onChange({
      ...canvas,
      pages: canvas.pages.map((page, index) =>
        index === pageIndex ? { ...page, ...patch } : page,
      ),
    })
  }

  function updateRegion(
    pageIndex: number,
    regionIndex: number,
    patch: Partial<PortalLayoutRegionView>,
  ): void {
    updatePage(pageIndex, {
      regions: canvas.pages[pageIndex].regions.map((region, index) =>
        index === regionIndex ? { ...region, ...patch } : region,
      ),
    })
  }

  function handleDrop(
    event: DragEvent<HTMLDivElement>,
    pageIndex: number,
    regionIndex: number,
  ): void {
    event.preventDefault()
    const widgetId = event.dataTransfer.getData('widgetId')
    const widget = activeWidgets.find((item) => item.widgetId === widgetId)

    if (!widget) {
      return
    }

    const region = canvas.pages[pageIndex].regions[regionIndex]
    const placement: PortalWidgetPlacementView = {
      placementId: buildId('placement'),
      placementCode: `${region.regionCode}-${widget.widgetCode}-${Date.now()}`,
      widgetCode: widget.widgetCode,
      cardType: widget.cardType,
      orderNo: region.placements.length + 1,
      hiddenByDefault: false,
      collapsedByDefault: false,
      overrideProps: {},
    }

    updateRegion(pageIndex, regionIndex, {
      placements: [...region.placements, placement],
    })
  }

  function removePlacement(
    pageIndex: number,
    regionIndex: number,
    placementIndex: number,
  ): void {
    const region = canvas.pages[pageIndex].regions[regionIndex]

    updateRegion(pageIndex, regionIndex, {
      placements: region.placements.filter(
        (_, index) => index !== placementIndex,
      ),
    })
  }

  return (
    <div className="space-y-4">
      {canvas.pages.map((page, pageIndex) => (
        <div
          className="rounded-2xl border border-slate-200 bg-slate-50 p-4"
          key={page.pageId}
        >
          <div className="grid gap-3 md:grid-cols-3">
            <TextInput
              aria-label="页面标题"
              value={page.title}
              onChange={(title) => updatePage(pageIndex, { title })}
            />
            <TextInput
              aria-label="页面编码"
              value={page.pageCode}
              onChange={(pageCode) => updatePage(pageIndex, { pageCode })}
            />
            <ToggleField
              checked={page.defaultPage}
              label="默认页"
              onChange={(defaultPage) =>
                onChange({
                  ...canvas,
                  pages: canvas.pages.map((item, index) => ({
                    ...item,
                    defaultPage: index === pageIndex ? defaultPage : false,
                  })),
                })
              }
            />
          </div>
          <div className="mt-4 grid gap-3 xl:grid-cols-2">
            {page.regions.map((region, regionIndex) => (
              <div
                className="min-h-48 rounded-xl border border-dashed border-slate-300 bg-white p-3"
                key={region.regionId}
                onDragOver={(event) => event.preventDefault()}
                onDrop={(event) => handleDrop(event, pageIndex, regionIndex)}
              >
                <div className="grid gap-2 md:grid-cols-[1fr_1fr_auto]">
                  <TextInput
                    aria-label="区域标题"
                    value={region.title}
                    onChange={(title) =>
                      updateRegion(pageIndex, regionIndex, { title })
                    }
                  />
                  <TextInput
                    aria-label="区域编码"
                    value={region.regionCode}
                    onChange={(regionCode) =>
                      updateRegion(pageIndex, regionIndex, { regionCode })
                    }
                  />
                  <ToggleField
                    checked={region.required}
                    label="必需"
                    onChange={(required) =>
                      updateRegion(pageIndex, regionIndex, { required })
                    }
                  />
                </div>
                <div className="mt-3 space-y-2">
                  {region.placements.map((placement, placementIndex) => (
                    <div
                      className="rounded-lg border border-slate-200 bg-slate-50 p-3 text-sm"
                      key={placement.placementId}
                    >
                      <div className="flex items-center justify-between gap-2">
                        <span className="font-medium text-slate-900">
                          {placement.widgetCode}
                        </span>
                        <StatusBadge>{placement.cardType}</StatusBadge>
                      </div>
                      <TextInput
                        className="mt-2"
                        value={placement.placementCode}
                        onChange={(placementCode) =>
                          updatePlacement(
                            canvas,
                            onChange,
                            pageIndex,
                            regionIndex,
                            placementIndex,
                            { placementCode },
                          )
                        }
                      />
                      <div className="mt-2 flex flex-wrap gap-2">
                        <Button
                          disabled={placementIndex === 0}
                          onClick={() =>
                            movePlacement(
                              canvas,
                              onChange,
                              pageIndex,
                              regionIndex,
                              placementIndex,
                              placementIndex - 1,
                            )
                          }
                          size="sm"
                          variant="outline"
                        >
                          上移
                        </Button>
                        <Button
                          disabled={
                            placementIndex === region.placements.length - 1
                          }
                          onClick={() =>
                            movePlacement(
                              canvas,
                              onChange,
                              pageIndex,
                              regionIndex,
                              placementIndex,
                              placementIndex + 1,
                            )
                          }
                          size="sm"
                          variant="outline"
                        >
                          下移
                        </Button>
                        <Button
                          onClick={() =>
                            removePlacement(
                              pageIndex,
                              regionIndex,
                              placementIndex,
                            )
                          }
                          size="sm"
                          variant="outline"
                        >
                          <Trash2 className="h-4 w-4" />
                          删除
                        </Button>
                      </div>
                    </div>
                  ))}
                  {!region.placements.length ? (
                    <div className="rounded-lg bg-slate-50 px-3 py-6 text-center text-sm text-slate-500">
                      将左侧组件拖入此区域
                    </div>
                  ) : null}
                </div>
              </div>
            ))}
          </div>
        </div>
      ))}
    </div>
  )
}

function DesignerPreview({
  preview,
}: {
  preview: PortalDesignerTemplatePreviewView
}): ReactElement {
  return (
    <div className="space-y-3">
      <div className="rounded-xl bg-slate-50 p-3 text-sm">
        <div className="flex flex-wrap gap-2">
          <StatusBadge>{preview.clientType}</StatusBadge>
          <StatusBadge
            tone={preview.overlay.status === 'applied' ? 'green' : 'amber'}
          >
            overlay {preview.overlay.status}
          </StatusBadge>
        </div>
        <p className="mt-2 text-xs text-slate-500">{preview.overlay.reason}</p>
      </div>
      {preview.page.regions.map((region) => (
        <div className="space-y-2" key={region.regionCode}>
          <h3 className="font-medium text-slate-900">{region.title}</h3>
          {region.cards.map((card) => (
            <PortalRuntimeCard card={card} key={card.cardCode} />
          ))}
        </div>
      ))}
    </div>
  )
}

function updatePlacement(
  canvas: PortalTemplateCanvasView,
  onChange: (canvas: PortalTemplateCanvasView) => void,
  pageIndex: number,
  regionIndex: number,
  placementIndex: number,
  patch: Partial<PortalWidgetPlacementView>,
): void {
  onChange({
    ...canvas,
    pages: canvas.pages.map((page, currentPageIndex) =>
      currentPageIndex !== pageIndex
        ? page
        : {
            ...page,
            regions: page.regions.map((region, currentRegionIndex) =>
              currentRegionIndex !== regionIndex
                ? region
                : {
                    ...region,
                    placements: region.placements.map(
                      (placement, currentPlacementIndex) =>
                        currentPlacementIndex === placementIndex
                          ? { ...placement, ...patch }
                          : placement,
                    ),
                  },
            ),
          },
    ),
  })
}

function movePlacement(
  canvas: PortalTemplateCanvasView,
  onChange: (canvas: PortalTemplateCanvasView) => void,
  pageIndex: number,
  regionIndex: number,
  fromIndex: number,
  toIndex: number,
): void {
  const region = canvas.pages[pageIndex].regions[regionIndex]

  if (toIndex < 0 || toIndex >= region.placements.length) {
    return
  }

  const placements = [...region.placements]
  const [placement] = placements.splice(fromIndex, 1)

  placements.splice(toIndex, 0, placement)

  onChange({
    ...canvas,
    pages: canvas.pages.map((page, currentPageIndex) =>
      currentPageIndex !== pageIndex
        ? page
        : {
            ...page,
            regions: page.regions.map((currentRegion, currentRegionIndex) =>
              currentRegionIndex === regionIndex
                ? { ...currentRegion, placements }
                : currentRegion,
            ),
          },
    ),
  })
}

function validateCanvas(canvas: PortalTemplateCanvasView | null): {
  status: 'valid' | 'invalid'
  message?: string
} {
  if (!canvas) {
    return { status: 'invalid', message: '画布未加载' }
  }

  if (!canvas.pages.some((page) => page.defaultPage)) {
    return { status: 'invalid', message: '必须设置一个默认页' }
  }

  const emptyRegion = canvas.pages
    .flatMap((page) => page.regions)
    .find((region) => !region.placements.length)

  if (emptyRegion) {
    return {
      status: 'invalid',
      message: `区域 ${emptyRegion.regionCode} 还没有组件实例，后端保存要求区域非空。`,
    }
  }

  return { status: 'valid' }
}

function normalizeCanvas(canvas: PortalTemplateCanvasView | null): {
  pages: PortalPageView[]
} {
  return {
    pages: (canvas?.pages ?? []).map((page) => ({
      ...page,
      regions: page.regions.map((region) => ({
        ...region,
        placements: region.placements.map((placement, index) => ({
          ...placement,
          orderNo: index + 1,
        })),
      })),
    })),
  }
}

function cloneCanvas(
  canvas: PortalTemplateCanvasView,
): PortalTemplateCanvasView {
  return {
    ...canvas,
    pages: canvas.pages.map((page) => ({
      ...page,
      regions: page.regions.map((region) => ({
        ...region,
        placements: region.placements.map((placement) => ({
          ...placement,
          overrideProps: { ...placement.overrideProps },
        })),
      })),
    })),
  }
}

function buildId(prefix: string): string {
  if (typeof crypto !== 'undefined' && crypto.randomUUID) {
    return `${prefix}-${crypto.randomUUID()}`
  }

  return `${prefix}-${Date.now()}`
}
