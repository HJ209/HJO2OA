import {
  useEffect,
  useMemo,
  useState,
  type FormEvent,
  type ReactElement,
} from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { CheckCircle2, RadioTower, Trash2 } from 'lucide-react'
import { Button } from '@/components/ui/button'
import {
  activatePortalPublication,
  createPortalTemplate,
  deprecatePortalTemplateVersion,
  fetchPortalTemplateCanvas,
  listPortalPublications,
  listPortalTemplates,
  offlinePortalPublication,
  publishPortalTemplateVersion,
} from '@/features/portal/services/portal-service'
import type {
  PortalClientType,
  PortalPublicationView,
  PortalSceneType,
  PortalTemplateView,
} from '@/features/portal/types/portal'
import {
  EmptyState,
  ErrorBanner,
  Field,
  LoadingBlock,
  NoPermissionPanel,
  PortalPanel,
  SelectInput,
  StatusBadge,
  SubmitButton,
  TextInput,
} from '@/features/portal/pages/portal-ui'

const SCENE_OPTIONS: Array<{ value: PortalSceneType; label: string }> = [
  { value: 'HOME', label: 'HOME' },
  { value: 'OFFICE_CENTER', label: 'OFFICE_CENTER' },
  { value: 'MOBILE_WORKBENCH', label: 'MOBILE_WORKBENCH' },
]

export default function PortalModelPanel({
  canManage,
}: {
  canManage: boolean
}): ReactElement {
  const queryClient = useQueryClient()
  const [sceneType, setSceneType] = useState<PortalSceneType>('HOME')
  const [selectedTemplateId, setSelectedTemplateId] = useState<string>('')
  const templatesQuery = useQuery({
    queryKey: ['portal-model', 'templates', sceneType],
    queryFn: () => listPortalTemplates({ sceneType }),
  })
  const publicationsQuery = useQuery({
    queryKey: ['portal-model', 'publications', sceneType],
    queryFn: () => listPortalPublications({ sceneType }),
  })
  const templates = useMemo(
    () => templatesQuery.data ?? [],
    [templatesQuery.data],
  )
  const selectedTemplate =
    templates.find((item) => item.templateId === selectedTemplateId) ??
    templates[0]
  const canvasQuery = useQuery({
    queryKey: ['portal-model', 'canvas', selectedTemplate?.templateId],
    queryFn: () => fetchPortalTemplateCanvas(selectedTemplate.templateId),
    enabled: Boolean(selectedTemplate),
  })

  useEffect(() => {
    if (!selectedTemplateId && templates[0]) {
      setSelectedTemplateId(templates[0].templateId)
    }
  }, [selectedTemplateId, templates])

  const invalidate = async (): Promise<void> => {
    await queryClient.invalidateQueries({ queryKey: ['portal-model'] })
    await queryClient.invalidateQueries({ queryKey: ['portal-designer'] })
    await queryClient.invalidateQueries({ queryKey: ['portal-home'] })
  }

  const createMutation = useMutation({
    mutationFn: createPortalTemplate,
    onSuccess: async (template) => {
      setSelectedTemplateId(template.templateId)
      await invalidate()
    },
  })
  const publishMutation = useMutation({
    mutationFn: ({
      templateId,
      versionNo,
    }: {
      templateId: string
      versionNo: number
    }) => publishPortalTemplateVersion(templateId, versionNo),
    onSuccess: invalidate,
  })
  const deprecateMutation = useMutation({
    mutationFn: ({
      templateId,
      versionNo,
    }: {
      templateId: string
      versionNo: number
    }) => deprecatePortalTemplateVersion(templateId, versionNo),
    onSuccess: invalidate,
  })
  const activateMutation = useMutation({
    mutationFn: ({
      publication,
      clientType,
    }: {
      publication: PortalPublicationView
      clientType: PortalClientType
    }) =>
      activatePortalPublication(publication.publicationId, {
        templateId: publication.templateId,
        sceneType: publication.sceneType,
        clientType,
      }),
    onSuccess: invalidate,
  })
  const offlineMutation = useMutation({
    mutationFn: (publicationId: string) =>
      offlinePortalPublication(publicationId),
    onSuccess: invalidate,
  })

  if (!canManage) {
    return <NoPermissionPanel />
  }

  return (
    <div className="grid gap-4 2xl:grid-cols-[360px_minmax(0,1fr)]">
      <div className="space-y-4">
        <PortalPanel
          title="模板过滤"
          description="按场景读取 portal-model 模板和发布列表。"
        >
          <Field label="场景">
            <SelectInput
              options={SCENE_OPTIONS}
              value={sceneType}
              onChange={(nextScene) => {
                setSceneType(nextScene)
                setSelectedTemplateId('')
              }}
            />
          </Field>
        </PortalPanel>

        <TemplateCreateForm
          pending={createMutation.isPending}
          sceneType={sceneType}
          onCreate={(body) => createMutation.mutate(body)}
        />

        {createMutation.isError ? (
          <ErrorBanner error={createMutation.error} />
        ) : null}
      </div>

      <div className="space-y-4">
        <PortalPanel
          title="模板、页面、区域、组件、版本"
          description="模板结构来自 portal-model；页面与区域只展示后端当前草稿画布。"
        >
          {templatesQuery.isLoading ? <LoadingBlock /> : null}
          {templatesQuery.isError ? (
            <ErrorBanner error={templatesQuery.error} title="模板加载失败" />
          ) : null}
          {!templatesQuery.isLoading && !templates.length ? (
            <EmptyState
              title="当前场景暂无模板"
              description="创建模板后会调用后端初始化默认画布，再进入设计器维护布局。"
            />
          ) : null}
          {templates.length ? (
            <div className="grid gap-4 xl:grid-cols-[280px_minmax(0,1fr)]">
              <TemplateList
                selectedTemplateId={selectedTemplate?.templateId}
                templates={templates}
                onSelect={setSelectedTemplateId}
              />
              <div className="space-y-4">
                {selectedTemplate ? (
                  <TemplateVersions
                    deprecating={deprecateMutation.isPending}
                    publishing={publishMutation.isPending}
                    template={selectedTemplate}
                    onDeprecate={(versionNo) =>
                      deprecateMutation.mutate({
                        templateId: selectedTemplate.templateId,
                        versionNo,
                      })
                    }
                    onPublish={(versionNo) =>
                      publishMutation.mutate({
                        templateId: selectedTemplate.templateId,
                        versionNo,
                      })
                    }
                  />
                ) : null}
                {canvasQuery.isLoading ? <LoadingBlock /> : null}
                {canvasQuery.isError ? (
                  <ErrorBanner error={canvasQuery.error} title="画布加载失败" />
                ) : null}
                {canvasQuery.data ? (
                  <CanvasReadModel canvas={canvasQuery.data} />
                ) : null}
              </div>
            </div>
          ) : null}
        </PortalPanel>

        <PortalPanel
          title="发布状态"
          description="发布生效和下线直接调用 portal-model publication API。"
        >
          {publicationsQuery.isLoading ? <LoadingBlock /> : null}
          {publicationsQuery.isError ? (
            <ErrorBanner
              error={publicationsQuery.error}
              title="发布列表加载失败"
            />
          ) : null}
          {publicationsQuery.data?.length ? (
            <PublicationTable
              activating={activateMutation.isPending}
              clientType="PC"
              offlining={offlineMutation.isPending}
              publications={publicationsQuery.data}
              onActivate={(publication, clientType) =>
                activateMutation.mutate({ publication, clientType })
              }
              onOffline={(publicationId) =>
                offlineMutation.mutate(publicationId)
              }
            />
          ) : (
            <EmptyState
              title="暂无发布记录"
              description="发布模板后会在这里看到可激活或下线的发布。"
            />
          )}
          {activateMutation.isError ? (
            <ErrorBanner error={activateMutation.error} title="发布激活失败" />
          ) : null}
          {offlineMutation.isError ? (
            <ErrorBanner error={offlineMutation.error} title="发布下线失败" />
          ) : null}
        </PortalPanel>
      </div>
    </div>
  )
}

function TemplateCreateForm({
  onCreate,
  pending,
  sceneType,
}: {
  sceneType: PortalSceneType
  pending: boolean
  onCreate: (body: {
    templateId: string
    templateCode: string
    displayName: string
    sceneType: PortalSceneType
  }) => void
}): ReactElement {
  const [templateId, setTemplateId] = useState(() => buildId('template'))
  const [templateCode, setTemplateCode] = useState('home-default')
  const [displayName, setDisplayName] = useState('默认门户模板')

  function handleSubmit(event: FormEvent<HTMLFormElement>): void {
    event.preventDefault()
    onCreate({
      templateId,
      templateCode,
      displayName,
      sceneType,
    })
    setTemplateId(buildId('template'))
  }

  return (
    <PortalPanel
      title="创建模板"
      description="创建请求走 portal-model/templates，后端负责初始化页面骨架。"
    >
      <form className="space-y-3" onSubmit={handleSubmit}>
        <Field label="templateId">
          <TextInput required value={templateId} onChange={setTemplateId} />
        </Field>
        <Field label="templateCode">
          <TextInput required value={templateCode} onChange={setTemplateCode} />
        </Field>
        <Field label="displayName">
          <TextInput required value={displayName} onChange={setDisplayName} />
        </Field>
        <SubmitButton pending={pending}>创建模板</SubmitButton>
      </form>
    </PortalPanel>
  )
}

function TemplateList({
  onSelect,
  selectedTemplateId,
  templates,
}: {
  templates: PortalTemplateView[]
  selectedTemplateId?: string
  onSelect: (templateId: string) => void
}): ReactElement {
  return (
    <div className="space-y-2">
      {templates.map((template) => (
        <button
          className={`w-full rounded-xl border px-3 py-2 text-left text-sm ${
            template.templateId === selectedTemplateId
              ? 'border-sky-500 bg-sky-50'
              : 'border-slate-200 bg-white'
          }`}
          key={template.templateId}
          onClick={() => onSelect(template.templateId)}
          type="button"
        >
          <span className="block font-medium text-slate-900">
            {template.displayName}
          </span>
          <span className="mt-1 block text-xs text-slate-500">
            {template.templateCode}
          </span>
          <span className="mt-2 flex gap-2">
            <StatusBadge tone="sky">{template.sceneType}</StatusBadge>
            <StatusBadge>v{template.latestVersionNo ?? '-'}</StatusBadge>
          </span>
        </button>
      ))}
    </div>
  )
}

function TemplateVersions({
  deprecating,
  onDeprecate,
  onPublish,
  publishing,
  template,
}: {
  template: PortalTemplateView
  publishing: boolean
  deprecating: boolean
  onPublish: (versionNo: number) => void
  onDeprecate: (versionNo: number) => void
}): ReactElement {
  const latestVersionNo = template.latestVersionNo ?? 1

  return (
    <div className="rounded-2xl border border-slate-200 bg-slate-50 p-4">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h3 className="font-semibold text-slate-950">
            {template.displayName}
          </h3>
          <p className="text-sm text-slate-500">
            最新版本 v{template.latestVersionNo ?? '-'}，已发布 v
            {template.publishedVersionNo ?? '-'}
          </p>
        </div>
        <Button
          disabled={publishing}
          onClick={() => onPublish(latestVersionNo)}
          size="sm"
        >
          <RadioTower className="h-4 w-4" />
          发布 v{latestVersionNo}
        </Button>
      </div>
      <div className="mt-3 grid gap-2 md:grid-cols-2">
        {template.versions.map((version) => (
          <div
            className="rounded-xl border border-slate-200 bg-white px-3 py-2 text-sm"
            key={version.versionNo}
          >
            <div className="flex items-center justify-between gap-2">
              <span className="font-medium">v{version.versionNo}</span>
              <StatusBadge
                tone={
                  version.status === 'PUBLISHED'
                    ? 'green'
                    : version.status === 'DEPRECATED'
                      ? 'red'
                      : 'slate'
                }
              >
                {version.status}
              </StatusBadge>
            </div>
            {version.status !== 'DEPRECATED' ? (
              <Button
                className="mt-2"
                disabled={deprecating}
                onClick={() => onDeprecate(version.versionNo)}
                size="sm"
                variant="outline"
              >
                <Trash2 className="h-4 w-4" />
                废弃
              </Button>
            ) : null}
          </div>
        ))}
      </div>
    </div>
  )
}

function CanvasReadModel({
  canvas,
}: {
  canvas: Awaited<ReturnType<typeof fetchPortalTemplateCanvas>>
}): ReactElement {
  return (
    <div className="space-y-3">
      {canvas.pages.map((page) => (
        <div
          className="rounded-2xl border border-slate-200 bg-white p-4"
          key={page.pageId}
        >
          <div className="flex flex-wrap items-center gap-2">
            <h3 className="font-semibold text-slate-950">{page.title}</h3>
            <StatusBadge tone={page.defaultPage ? 'green' : 'slate'}>
              {page.defaultPage ? '默认页' : page.pageCode}
            </StatusBadge>
            <StatusBadge>{page.layoutMode}</StatusBadge>
          </div>
          <div className="mt-3 grid gap-3 xl:grid-cols-2">
            {page.regions.map((region) => (
              <div
                className="rounded-xl border border-slate-200 bg-slate-50 p-3"
                key={region.regionId}
              >
                <div className="flex items-center justify-between gap-2">
                  <span className="font-medium text-slate-900">
                    {region.title}
                  </span>
                  <StatusBadge tone={region.required ? 'amber' : 'slate'}>
                    {region.required ? 'required' : region.regionCode}
                  </StatusBadge>
                </div>
                <div className="mt-2 space-y-2">
                  {region.placements.map((placement) => (
                    <div
                      className="rounded-lg bg-white px-3 py-2 text-sm"
                      key={placement.placementId}
                    >
                      <div className="flex items-center justify-between gap-2">
                        <span>{placement.widgetCode}</span>
                        <StatusBadge>{placement.cardType}</StatusBadge>
                      </div>
                      <p className="mt-1 text-xs text-slate-500">
                        {placement.placementCode} · order {placement.orderNo}
                      </p>
                    </div>
                  ))}
                </div>
              </div>
            ))}
          </div>
        </div>
      ))}
    </div>
  )
}

function PublicationTable({
  activating,
  clientType,
  offlining,
  onActivate,
  onOffline,
  publications,
}: {
  publications: PortalPublicationView[]
  clientType: PortalClientType
  activating: boolean
  offlining: boolean
  onActivate: (
    publication: PortalPublicationView,
    clientType: PortalClientType,
  ) => void
  onOffline: (publicationId: string) => void
}): ReactElement {
  return (
    <div className="space-y-2">
      {publications.map((publication) => (
        <div
          className="grid gap-3 rounded-xl border border-slate-200 bg-white p-3 text-sm xl:grid-cols-[1fr_auto]"
          key={publication.publicationId}
        >
          <div className="min-w-0">
            <div className="flex flex-wrap items-center gap-2">
              <span className="truncate font-medium text-slate-900">
                {publication.publicationId}
              </span>
              <StatusBadge
                tone={publication.status === 'ACTIVE' ? 'green' : 'slate'}
              >
                {publication.status}
              </StatusBadge>
              <StatusBadge>{publication.clientType}</StatusBadge>
              <StatusBadge>{publication.audience.type}</StatusBadge>
            </div>
            <p className="mt-1 text-xs text-slate-500">
              template {publication.templateId}
            </p>
          </div>
          <div className="flex flex-wrap items-center gap-2">
            <Button
              disabled={activating}
              onClick={() => onActivate(publication, clientType)}
              size="sm"
              variant="outline"
            >
              <CheckCircle2 className="h-4 w-4" />
              激活
            </Button>
            <Button
              disabled={offlining}
              onClick={() => onOffline(publication.publicationId)}
              size="sm"
              variant="outline"
            >
              下线
            </Button>
          </div>
        </div>
      ))}
    </div>
  )
}

function buildId(prefix: string): string {
  if (typeof crypto !== 'undefined' && crypto.randomUUID) {
    return `${prefix}-${crypto.randomUUID()}`
  }

  return `${prefix}-${Date.now()}`
}
