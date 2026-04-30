import {
  useEffect,
  useMemo,
  useState,
  type FormEvent,
  type ReactElement,
} from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { PauseCircle, Save } from 'lucide-react'
import { Button } from '@/components/ui/button'
import {
  disableWidgetDefinition,
  listWidgetDefinitions,
  upsertWidgetDefinition,
} from '@/features/portal/services/portal-service'
import type {
  PortalCardType,
  PortalSceneType,
  UpsertWidgetDefinitionRequest,
  WidgetDataSourceType,
  WidgetDefinitionStatus,
  WidgetDefinitionView,
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
  ToggleField,
} from '@/features/portal/pages/portal-ui'

const CARD_OPTIONS: Array<{ value: PortalCardType; label: string }> = [
  { value: 'IDENTITY', label: 'IDENTITY' },
  { value: 'TODO', label: 'TODO' },
  { value: 'MESSAGE', label: 'MESSAGE' },
]

const SCENE_OPTIONS: Array<{ value: PortalSceneType; label: string }> = [
  { value: 'HOME', label: 'HOME' },
  { value: 'OFFICE_CENTER', label: 'OFFICE_CENTER' },
  { value: 'MOBILE_WORKBENCH', label: 'MOBILE_WORKBENCH' },
]

const DATA_SOURCE_OPTIONS: Array<{
  value: WidgetDataSourceType
  label: string
}> = [
  { value: 'AGGREGATION_QUERY', label: 'AGGREGATION_QUERY' },
  { value: 'STATIC_LINK', label: 'STATIC_LINK' },
  { value: 'EXTERNAL_API', label: 'EXTERNAL_API' },
]

const STATUS_OPTIONS: Array<{ value: WidgetDefinitionStatus; label: string }> =
  [
    { value: 'ACTIVE', label: 'ACTIVE' },
    { value: 'DISABLED', label: 'DISABLED' },
  ]

export default function PortalWidgetConfigPanel({
  canManage,
}: {
  canManage: boolean
}): ReactElement {
  const queryClient = useQueryClient()
  const [sceneType, setSceneType] = useState<PortalSceneType>('HOME')
  const [status, setStatus] = useState<WidgetDefinitionStatus>('ACTIVE')
  const widgetsQuery = useQuery({
    queryKey: ['portal-widget-config', sceneType, status],
    queryFn: () => listWidgetDefinitions({ sceneType, status }),
  })
  const widgets = useMemo(() => widgetsQuery.data ?? [], [widgetsQuery.data])
  const [selectedWidgetId, setSelectedWidgetId] = useState<string>('')
  const selectedWidget = useMemo(
    () =>
      widgets.find((widget) => widget.widgetId === selectedWidgetId) ??
      widgets[0],
    [selectedWidgetId, widgets],
  )

  const invalidate = async (): Promise<void> => {
    await queryClient.invalidateQueries({ queryKey: ['portal-widget-config'] })
    await queryClient.invalidateQueries({ queryKey: ['portal-designer'] })
    await queryClient.invalidateQueries({ queryKey: ['portal-home'] })
  }
  const upsertMutation = useMutation({
    mutationFn: ({
      body,
      widgetId,
    }: {
      widgetId: string
      body: UpsertWidgetDefinitionRequest
    }) => upsertWidgetDefinition(widgetId, body),
    onSuccess: async (widget) => {
      setSelectedWidgetId(widget.widgetId)
      await invalidate()
    },
  })
  const disableMutation = useMutation({
    mutationFn: disableWidgetDefinition,
    onSuccess: invalidate,
  })

  if (!canManage) {
    return <NoPermissionPanel />
  }

  return (
    <div className="grid gap-4 2xl:grid-cols-[380px_minmax(0,1fr)]">
      <div className="space-y-4">
        <PortalPanel
          title="卡片筛选"
          description="读取 widget-config 后端定义列表。"
        >
          <div className="grid gap-3 md:grid-cols-2 2xl:grid-cols-1">
            <Field label="sceneType">
              <SelectInput
                options={SCENE_OPTIONS}
                value={sceneType}
                onChange={setSceneType}
              />
            </Field>
            <Field label="status">
              <SelectInput
                options={STATUS_OPTIONS}
                value={status}
                onChange={setStatus}
              />
            </Field>
          </div>
        </PortalPanel>

        <WidgetDefinitionForm
          pending={upsertMutation.isPending}
          sceneType={sceneType}
          widget={selectedWidget}
          onSubmit={(widgetId, body) =>
            upsertMutation.mutate({ widgetId, body })
          }
        />
        {upsertMutation.isError ? (
          <ErrorBanner error={upsertMutation.error} title="卡片保存失败" />
        ) : null}
      </div>

      <PortalPanel
        title="WidgetConfig 定义"
        description="数据源、展示策略、权限显隐和错误态均按后端字段展示。"
      >
        {widgetsQuery.isLoading ? <LoadingBlock /> : null}
        {widgetsQuery.isError ? (
          <ErrorBanner error={widgetsQuery.error} title="卡片加载失败" />
        ) : null}
        {!widgetsQuery.isLoading && !widgets.length ? (
          <EmptyState
            title="当前过滤条件下没有卡片"
            description="创建或调整卡片定义后，设计器组件面板会自动消费该配置。"
          />
        ) : null}
        {widgets.length ? (
          <div className="space-y-3">
            {widgets.map((widget) => (
              <WidgetDefinitionRow
                disabling={disableMutation.isPending}
                key={widget.widgetId}
                selected={widget.widgetId === selectedWidget?.widgetId}
                widget={widget}
                onDisable={() => disableMutation.mutate(widget.widgetId)}
                onSelect={() => setSelectedWidgetId(widget.widgetId)}
              />
            ))}
          </div>
        ) : null}
        {disableMutation.isError ? (
          <ErrorBanner error={disableMutation.error} title="卡片停用失败" />
        ) : null}
      </PortalPanel>
    </div>
  )
}

function WidgetDefinitionForm({
  onSubmit,
  pending,
  sceneType,
  widget,
}: {
  widget?: WidgetDefinitionView
  sceneType: PortalSceneType
  pending: boolean
  onSubmit: (widgetId: string, body: UpsertWidgetDefinitionRequest) => void
}): ReactElement {
  const [widgetId, setWidgetId] = useState(
    widget?.widgetId ?? buildId('widget'),
  )
  const [widgetCode, setWidgetCode] = useState(
    widget?.widgetCode ?? 'todo-card',
  )
  const [displayName, setDisplayName] = useState(
    widget?.displayName ?? '待办卡片',
  )
  const [cardType, setCardType] = useState<PortalCardType>(
    widget?.cardType ?? 'TODO',
  )
  const [sourceModule, setSourceModule] = useState(
    widget?.sourceModule ?? 'todo-center',
  )
  const [dataSourceType, setDataSourceType] = useState<WidgetDataSourceType>(
    widget?.dataSourceType ?? 'AGGREGATION_QUERY',
  )
  const [allowHide, setAllowHide] = useState(widget?.allowHide ?? true)
  const [allowCollapse, setAllowCollapse] = useState(
    widget?.allowCollapse ?? true,
  )
  const [maxItems, setMaxItems] = useState(String(widget?.maxItems ?? 10))

  useEffect(() => {
    if (!widget) {
      return
    }

    setWidgetId(widget.widgetId)
    setWidgetCode(widget.widgetCode)
    setDisplayName(widget.displayName)
    setCardType(widget.cardType)
    setSourceModule(widget.sourceModule)
    setDataSourceType(widget.dataSourceType)
    setAllowHide(widget.allowHide)
    setAllowCollapse(widget.allowCollapse)
    setMaxItems(String(widget.maxItems))
  }, [widget])

  function handleSubmit(event: FormEvent<HTMLFormElement>): void {
    event.preventDefault()
    onSubmit(widgetId, {
      widgetCode,
      displayName,
      cardType,
      sceneType,
      sourceModule,
      dataSourceType,
      allowHide,
      allowCollapse,
      maxItems: Math.max(Number(maxItems) || 1, 1),
    })
  }

  return (
    <PortalPanel
      title="编辑卡片"
      description="保存调用 PUT /portal/widget-config/widgets/{widgetId}。"
    >
      <form className="space-y-3" onSubmit={handleSubmit}>
        <Field label="widgetId">
          <TextInput required value={widgetId} onChange={setWidgetId} />
        </Field>
        <Field label="widgetCode">
          <TextInput required value={widgetCode} onChange={setWidgetCode} />
        </Field>
        <Field label="displayName">
          <TextInput required value={displayName} onChange={setDisplayName} />
        </Field>
        <Field label="cardType">
          <SelectInput
            options={CARD_OPTIONS}
            value={cardType}
            onChange={setCardType}
          />
        </Field>
        <Field label="dataSourceType">
          <SelectInput
            options={DATA_SOURCE_OPTIONS}
            value={dataSourceType}
            onChange={setDataSourceType}
          />
        </Field>
        <Field label="sourceModule">
          <TextInput required value={sourceModule} onChange={setSourceModule} />
        </Field>
        <Field label="maxItems / 刷新窗口容量">
          <TextInput
            min={1}
            type="number"
            value={maxItems}
            onChange={setMaxItems}
          />
        </Field>
        <div className="grid gap-2 md:grid-cols-2">
          <ToggleField
            checked={allowHide}
            label="允许用户隐藏"
            onChange={setAllowHide}
          />
          <ToggleField
            checked={allowCollapse}
            label="允许用户折叠"
            onChange={setAllowCollapse}
          />
        </div>
        <SubmitButton pending={pending}>
          <Save className="h-4 w-4" />
          保存卡片
        </SubmitButton>
      </form>
    </PortalPanel>
  )
}

function WidgetDefinitionRow({
  disabling,
  onDisable,
  onSelect,
  selected,
  widget,
}: {
  widget: WidgetDefinitionView
  selected: boolean
  disabling: boolean
  onSelect: () => void
  onDisable: () => void
}): ReactElement {
  return (
    <div
      className={`rounded-2xl border p-4 ${
        selected ? 'border-sky-500 bg-sky-50' : 'border-slate-200 bg-white'
      }`}
    >
      <div className="flex flex-col gap-3 xl:flex-row xl:items-start xl:justify-between">
        <button className="min-w-0 text-left" onClick={onSelect} type="button">
          <div className="flex flex-wrap items-center gap-2">
            <span className="font-semibold text-slate-950">
              {widget.displayName}
            </span>
            <StatusBadge tone={widget.status === 'ACTIVE' ? 'green' : 'red'}>
              {widget.status}
            </StatusBadge>
            <StatusBadge>{widget.cardType}</StatusBadge>
            {widget.sceneType ? (
              <StatusBadge>{widget.sceneType}</StatusBadge>
            ) : null}
          </div>
          <p className="mt-1 text-sm text-slate-500">
            {widget.widgetCode} · {widget.sourceModule} ·{' '}
            {widget.dataSourceType}
          </p>
          <div className="mt-2 flex flex-wrap gap-2">
            <StatusBadge tone={widget.allowHide ? 'green' : 'amber'}>
              {widget.allowHide ? '可隐藏' : '不可隐藏'}
            </StatusBadge>
            <StatusBadge tone={widget.allowCollapse ? 'green' : 'amber'}>
              {widget.allowCollapse ? '可折叠' : '不可折叠'}
            </StatusBadge>
            <StatusBadge>max {widget.maxItems}</StatusBadge>
          </div>
        </button>
        <Button
          disabled={disabling || widget.status === 'DISABLED'}
          onClick={onDisable}
          size="sm"
          variant="outline"
        >
          <PauseCircle className="h-4 w-4" />
          停用
        </Button>
      </div>
    </div>
  )
}

function buildId(prefix: string): string {
  if (typeof crypto !== 'undefined' && crypto.randomUUID) {
    return `${prefix}-${crypto.randomUUID()}`
  }

  return `${prefix}-${Date.now()}`
}
