import { Suspense, lazy, useMemo, useState, type ReactElement } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useLocation } from 'react-router-dom'
import { GitBranch, History, Play, Rocket, RotateCw, Send } from 'lucide-react'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { formService } from '@/features/workflow/services/form-service'
import { workflowService } from '@/features/workflow/services/workflow-service'
import type {
  ActionDefinition,
  ProcessInstanceDetail,
  SaveWorkflowDefinitionRequest,
  TaskInstance,
  WorkflowDefinition,
} from '@/features/workflow/types/workflow'
import { cn } from '@/utils/cn'

const SAMPLE_OPERATOR_ACCOUNT_ID = 'admin'
const SAMPLE_TENANT_ID = '11111111-1111-1111-1111-111111111111'
const SAMPLE_PERSON_ID = '99999999-9999-9999-9999-999999999999'
const SAMPLE_ORG_ID = '11111111-1111-1111-1111-111111111111'
const SAMPLE_DEPT_ID = '22222222-2222-2222-2222-222222222222'
const SAMPLE_POSITION_ID = '33333333-3333-3333-3333-333333333333'
const SAMPLE_FORM_DATA_ID = '44444444-4444-4444-4444-444444444444'
const UUID_PATTERN =
  /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i

const DEFAULT_NODES = `[
  {"nodeId":"start","type":"START","name":"Start"},
  {"nodeId":"approve","type":"USER_TASK","name":"Approve",
   "participantRule":{"type":"INITIATOR"},
   "actionCodes":["approve","reject","transfer","add_sign"]},
  {"nodeId":"end","type":"END","name":"End"}
]`

const DEFAULT_ROUTES = `[
  {"routeId":"r1","sourceNodeId":"start","targetNodeId":"approve"},
  {"routeId":"r2","sourceNodeId":"approve","targetNodeId":"end"}
]`

const EMPTY_DETAIL: ProcessInstanceDetail | null = null

const FormDesignerPage = lazy(
  () => import('@/features/workflow/pages/form-designer-page'),
)
const FormRendererPage = lazy(
  () => import('@/features/workflow/pages/form-renderer-page'),
)
const ProcessMonitorPage = lazy(
  () => import('@/features/workflow/pages/process-monitor-page'),
)

function parseJsonObject(value: string): Record<string, unknown> {
  if (!value.trim()) {
    return {}
  }

  const parsed = JSON.parse(value) as unknown

  if (typeof parsed !== 'object' || parsed === null || Array.isArray(parsed)) {
    throw new Error('JSON must be an object')
  }

  return parsed as Record<string, unknown>
}

function parseJson(value: string): unknown {
  return JSON.parse(value)
}

function optionalUuid(value: string, label: string): string | undefined {
  const nextValue = value.trim()

  if (!nextValue) {
    return undefined
  }

  if (!UUID_PATTERN.test(nextValue)) {
    throw new Error(`${label} 必须是 UUID 格式`)
  }

  return nextValue
}

function requiredUuid(value: string, label: string): string {
  const nextValue = optionalUuid(value, label)

  if (!nextValue) {
    throw new Error(`${label} 不能为空`)
  }

  return nextValue
}

function requiredText(value: string, label: string): string {
  const nextValue = value.trim()

  if (!nextValue) {
    throw new Error(`${label} 不能为空`)
  }

  return nextValue
}

function parseUuidList(value: string, label: string): string[] {
  return value
    .split(',')
    .map((item) => item.trim())
    .filter(Boolean)
    .map((item, index) => requiredUuid(item, `${label} #${index + 1}`))
}

function sampleMetadataCode(baseCode: string): string {
  const suffix =
    typeof crypto !== 'undefined' && crypto.randomUUID
      ? crypto.randomUUID().slice(0, 8)
      : Date.now().toString(36)
  const normalizedBase = (baseCode.trim() || 'workflow')
    .replace(/[^a-zA-Z0-9_.-]/g, '_')
    .slice(0, 42)

  return `${normalizedBase}.form.${suffix}`.slice(0, 64)
}

function formatTime(value?: string | null): string {
  if (!value) {
    return '-'
  }

  return new Intl.DateTimeFormat('zh-CN', {
    dateStyle: 'short',
    timeStyle: 'short',
  }).format(new Date(value))
}

function statusVariant(status: string): 'default' | 'secondary' | 'success' {
  if (status === 'PUBLISHED' || status === 'COMPLETED') {
    return 'success'
  }

  if (status === 'RUNNING' || status === 'CLAIMED' || status === 'CREATED') {
    return 'default'
  }

  return 'secondary'
}

function requiresTargetAssignee(action: ActionDefinition): boolean {
  return ['TRANSFER', 'DELEGATE', 'ADD_SIGN'].includes(action.category)
}

function ErrorLine({ error }: { error: unknown }): ReactElement | null {
  if (!error) {
    return null
  }

  return (
    <p className="rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">
      {error instanceof Error ? error.message : String(error)}
    </p>
  )
}

function Field({
  label,
  hint,
  children,
}: {
  label: string
  hint?: string
  children: ReactElement
}): ReactElement {
  return (
    <label className="grid gap-1 text-sm font-medium text-slate-700">
      <span>{label}</span>
      {children}
      {hint ? <span className="text-xs font-normal text-slate-500">{hint}</span> : null}
    </label>
  )
}

function DefinitionRow({
  definition,
  selected,
  onSelect,
  onPublish,
  publishing,
}: {
  definition: WorkflowDefinition
  selected: boolean
  onSelect: () => void
  onPublish: () => void
  publishing: boolean
}): ReactElement {
  return (
    <div
      className={cn(
        'grid gap-3 border-b border-slate-100 px-3 py-3 last:border-b-0 lg:grid-cols-[1fr_auto]',
        selected ? 'bg-sky-50' : 'bg-white',
      )}
    >
      <button className="min-w-0 text-left" onClick={onSelect} type="button">
        <div className="flex flex-wrap items-center gap-2">
          <span className="font-semibold text-slate-950">
            {definition.name}
          </span>
          <Badge variant={statusVariant(definition.status)}>
            {definition.status}
          </Badge>
          <span className="text-xs text-slate-500">v{definition.version}</span>
        </div>
        <p className="mt-1 truncate text-sm text-slate-500">
          {definition.code} · {definition.category ?? '-'} ·{' '}
          {definition.formMetadataId ?? '-'}
        </p>
      </button>
      <div className="flex items-center gap-2">
        <Button onClick={onSelect} size="sm" variant="outline">
          <GitBranch className="h-4 w-4" />
          选择
        </Button>
        <Button
          disabled={definition.status === 'PUBLISHED' || publishing}
          onClick={onPublish}
          size="sm"
        >
          <Rocket className="h-4 w-4" />
          发布
        </Button>
      </div>
    </div>
  )
}

function TaskLine({
  task,
  selected,
  onSelect,
}: {
  task: TaskInstance
  selected: boolean
  onSelect: () => void
}): ReactElement {
  return (
    <button
      className={cn(
        'w-full rounded-lg border px-3 py-2 text-left transition',
        selected
          ? 'border-sky-300 bg-sky-50'
          : 'border-slate-200 bg-white hover:border-slate-300',
      )}
      onClick={onSelect}
      type="button"
    >
      <div className="flex flex-wrap items-center gap-2">
        <span className="font-medium text-slate-950">{task.nodeName}</span>
        <Badge variant={statusVariant(task.status)}>{task.status}</Badge>
      </div>
      <p className="mt-1 text-xs text-slate-500">
        {task.nodeId} · {task.assigneeId ?? 'candidate'} ·{' '}
        {task.candidateIds.join(', ') || '-'}
      </p>
    </button>
  )
}

function ProcessEnginePage(): ReactElement {
  const queryClient = useQueryClient()
  const [selectedDefinitionId, setSelectedDefinitionId] = useState('')
  const [instanceIdInput, setInstanceIdInput] = useState('')
  const [detail, setDetail] = useState<ProcessInstanceDetail | null>(
    EMPTY_DETAIL,
  )
  const [selectedTaskId, setSelectedTaskId] = useState('')
  const [definitionForm, setDefinitionForm] = useState({
    code: 'expense',
    name: 'Expense Approval',
    category: 'FINANCE',
    formMetadataId: '',
    startNodeId: 'start',
    endNodeId: 'end',
    nodes: DEFAULT_NODES,
    routes: DEFAULT_ROUTES,
    publishedBy: SAMPLE_PERSON_ID,
  })
  const [startForm, setStartForm] = useState({
    title: 'Expense Approval',
    businessKey: '',
    initiatorId: SAMPLE_PERSON_ID,
    initiatorOrgId: SAMPLE_ORG_ID,
    initiatorDeptId: SAMPLE_DEPT_ID,
    initiatorPositionId: SAMPLE_POSITION_ID,
    formDataId: SAMPLE_FORM_DATA_ID,
    variables: '{\n  "amount": 1280,\n  "reason": "travel"\n}',
  })
  const [actionForm, setActionForm] = useState({
    operatorAccountId: SAMPLE_OPERATOR_ACCOUNT_ID,
    operatorPersonId: SAMPLE_PERSON_ID,
    operatorOrgId: SAMPLE_ORG_ID,
    operatorPositionId: SAMPLE_POSITION_ID,
    opinion: '',
    targetNodeId: '',
    targetAssigneeIds: '',
    formDataPatch: '{}',
  })

  const definitionsQuery = useQuery({
    queryKey: ['workflow', 'definitions'],
    queryFn: workflowService.listDefinitions,
  })

  const selectedDefinition = useMemo(
    () =>
      definitionsQuery.data?.find((item) => item.id === selectedDefinitionId) ??
      definitionsQuery.data?.find((item) => item.status === 'PUBLISHED') ??
      null,
    [definitionsQuery.data, selectedDefinitionId],
  )

  const openTasks = useMemo(
    () =>
      detail?.tasks.filter((task) =>
        ['CREATED', 'CLAIMED'].includes(task.status),
      ) ?? [],
    [detail],
  )

  const activeTask = useMemo(
    () =>
      openTasks.find((task) => task.id === selectedTaskId) ??
      openTasks[0] ??
      null,
    [openTasks, selectedTaskId],
  )

  const actionsQuery = useQuery({
    enabled: Boolean(activeTask?.id),
    queryKey: ['workflow', 'task-actions', activeTask?.id],
    queryFn: () => workflowService.listActions(activeTask?.id ?? ''),
  })

  function buildDefinitionPayload(): SaveWorkflowDefinitionRequest {
    return {
      code: requiredText(definitionForm.code, '流程编码'),
      name: requiredText(definitionForm.name, '流程名称'),
      category: definitionForm.category.trim(),
      formMetadataId: optionalUuid(
        definitionForm.formMetadataId,
        '表单元数据 ID',
      ),
      startNodeId: requiredText(definitionForm.startNodeId, '开始节点'),
      endNodeId: requiredText(definitionForm.endNodeId, '结束节点'),
      nodes: parseJson(definitionForm.nodes),
      routes: parseJson(definitionForm.routes),
    }
  }

  function selectDefinition(definition: WorkflowDefinition): void {
    setSelectedDefinitionId(definition.id)
    setDefinitionForm((value) => ({
      ...value,
      code: definition.code,
      name: definition.name,
      category: definition.category ?? '',
      formMetadataId: definition.formMetadataId ?? '',
      startNodeId: definition.startNodeId ?? 'start',
      endNodeId: definition.endNodeId ?? 'end',
      nodes: definition.nodes,
      routes: definition.routes,
      publishedBy: definition.publishedBy ?? SAMPLE_PERSON_ID,
    }))
    setStartForm((value) => ({
      ...value,
      title: definition.name,
    }))
  }

  const createDefinitionMutation = useMutation({
    mutationFn: () => {
      const payload = buildDefinitionPayload()
      const canUpdateSelectedDefinition =
        selectedDefinition?.status === 'DRAFT' &&
        selectedDefinition.code === payload.code

      return canUpdateSelectedDefinition
        ? workflowService.updateDefinition(selectedDefinition.id, payload)
        : workflowService.createDefinition(payload)
    },
    onSuccess: (definition) => {
      setSelectedDefinitionId(definition.id)
      void queryClient.invalidateQueries({
        queryKey: ['workflow', 'definitions'],
      })
    },
  })

  const createSampleMetadataMutation = useMutation({
    mutationFn: async () => {
      const draft = await formService.createMetadata({
        code: sampleMetadataCode(definitionForm.code),
        name: `${requiredText(definitionForm.name, '流程名称')} Form`,
        tenantId: SAMPLE_TENANT_ID,
        fieldSchema: [
          {
            fieldCode: 'amount',
            fieldName: 'Amount',
            fieldType: 'NUMBER',
            required: true,
            multiValue: false,
            visible: true,
            editable: true,
            min: 0,
          },
          {
            fieldCode: 'reason',
            fieldName: 'Reason',
            fieldType: 'TEXT',
            required: false,
            multiValue: false,
            visible: true,
            editable: true,
            maxLength: 512,
          },
        ],
        layout: { type: 'vertical', fields: ['amount', 'reason'] },
        fieldPermissionMap: {
          start: {
            amount: { visible: true, editable: true, required: true },
            reason: { visible: true, editable: true, required: false },
          },
          approve: {
            amount: { visible: true, editable: false, required: true },
            reason: { visible: true, editable: true, required: false },
          },
        },
      })

      return formService.publishMetadata(draft.id)
    },
    onSuccess: (metadata) => {
      setDefinitionForm((value) => ({
        ...value,
        formMetadataId: metadata.id,
      }))
    },
  })

  const publishDefinitionMutation = useMutation({
    mutationFn: async (definition: WorkflowDefinition) => {
      const shouldSaveBeforePublish =
        definition.status === 'DRAFT' &&
        definition.id === selectedDefinitionId &&
        definition.formMetadataId !== definitionForm.formMetadataId.trim()
      const targetDefinition = shouldSaveBeforePublish
        ? await workflowService.updateDefinition(
            definition.id,
            buildDefinitionPayload(),
          )
        : definition

      return workflowService.publishDefinition(
        targetDefinition.id,
        optionalUuid(definitionForm.publishedBy, '发布人 ID'),
      )
    },
    onSuccess: (definition) => {
      setSelectedDefinitionId(definition.id)
      void queryClient.invalidateQueries({
        queryKey: ['workflow', 'definitions'],
      })
    },
  })

  const startMutation = useMutation({
    mutationFn: () =>
      workflowService.startProcess({
        definitionId: selectedDefinition?.id ?? selectedDefinitionId,
        title: requiredText(startForm.title, '流程标题'),
        businessKey: startForm.businessKey.trim() || undefined,
        initiatorId: requiredUuid(startForm.initiatorId, '发起人 ID'),
        initiatorOrgId: requiredUuid(startForm.initiatorOrgId, '发起组织 ID'),
        initiatorDeptId: optionalUuid(startForm.initiatorDeptId, '发起部门 ID'),
        initiatorPositionId: requiredUuid(
          startForm.initiatorPositionId,
          '发起岗位 ID',
        ),
        formDataId: requiredUuid(startForm.formDataId, '表单数据 ID'),
        variables: parseJsonObject(startForm.variables),
      }),
    onSuccess: (nextDetail) => {
      setDetail(nextDetail)
      setInstanceIdInput(nextDetail.instance.id)
      setSelectedTaskId(nextDetail.tasks[0]?.id ?? '')
    },
  })

  const loadInstanceMutation = useMutation({
    mutationFn: (instanceId: string) => workflowService.getTimeline(instanceId),
    onSuccess: (nextDetail) => {
      setDetail(nextDetail)
      setSelectedTaskId(
        nextDetail.tasks.find((task) => task.status === 'CLAIMED')?.id ??
          nextDetail.tasks[0]?.id ??
          '',
      )
    },
  })

  const executeActionMutation = useMutation({
    mutationFn: (action: ActionDefinition) =>
      workflowService.executeAction(activeTask?.id ?? '', {
        actionCode: action.code,
        opinion: actionForm.opinion.trim() || undefined,
        targetNodeId: actionForm.targetNodeId.trim() || undefined,
        targetAssigneeIds: parseUuidList(
          actionForm.targetAssigneeIds,
          '目标处理人 ID',
        ),
        formDataPatch: parseJsonObject(actionForm.formDataPatch),
        operatorAccountId: requiredText(
          actionForm.operatorAccountId,
          '操作账号',
        ),
        operatorPersonId: requiredUuid(
          actionForm.operatorPersonId,
          '操作人 ID',
        ),
        operatorOrgId: requiredUuid(actionForm.operatorOrgId, '操作组织 ID'),
        operatorPositionId: requiredUuid(
          actionForm.operatorPositionId,
          '操作岗位 ID',
        ),
      }),
    onSuccess: async () => {
      if (!detail?.instance.id) {
        return
      }

      const nextDetail = await workflowService.getTimeline(detail.instance.id)
      setDetail(nextDetail)
      setSelectedTaskId(
        nextDetail.tasks.find((task) => task.status === 'CLAIMED')?.id ??
          nextDetail.tasks[0]?.id ??
          '',
      )
    },
  })

  return (
    <div className="space-y-4">
      <section className="rounded-lg border border-slate-200 bg-white shadow-sm">
        <div className="flex flex-col gap-3 border-b border-slate-100 px-4 py-3 lg:flex-row lg:items-center lg:justify-between">
          <div>
            <h1 className="text-base font-semibold text-slate-950">
              流程引擎核心
            </h1>
            <p className="mt-1 text-sm text-slate-500">
              定义发布、实例发起、任务动作和节点轨迹
            </p>
          </div>
          <Button
            disabled={definitionsQuery.isFetching}
            onClick={() =>
              void queryClient.invalidateQueries({
                queryKey: ['workflow', 'definitions'],
              })
            }
            variant="outline"
          >
            <RotateCw className="h-4 w-4" />
            刷新
          </Button>
        </div>

        <div className="grid gap-0 lg:grid-cols-[minmax(0,1fr)_26rem]">
          <div className="min-h-64 border-b border-slate-100 lg:border-b-0 lg:border-r">
            {definitionsQuery.data?.length ? (
              definitionsQuery.data.map((definition) => (
                <DefinitionRow
                  definition={definition}
                  key={definition.id}
                  onPublish={() =>
                    publishDefinitionMutation.mutate(definition)
                  }
                  onSelect={() => selectDefinition(definition)}
                  publishing={publishDefinitionMutation.isPending}
                  selected={selectedDefinition?.id === definition.id}
                />
              ))
            ) : (
              <div className="px-4 py-8 text-sm text-slate-500">
                {definitionsQuery.isLoading ? '加载中' : '暂无流程定义'}
              </div>
            )}
          </div>

          <form
            className="space-y-3 p-4"
            onSubmit={(event) => {
              event.preventDefault()
              createDefinitionMutation.mutate()
            }}
          >
            <div className="grid gap-2 sm:grid-cols-2">
              <Field label="流程编码">
                <Input
                  onChange={(event) =>
                    setDefinitionForm((value) => ({
                      ...value,
                      code: event.target.value,
                    }))
                  }
                  placeholder="code"
                  value={definitionForm.code}
                />
              </Field>
              <Field label="流程名称">
                <Input
                  onChange={(event) =>
                    setDefinitionForm((value) => ({
                      ...value,
                      name: event.target.value,
                    }))
                  }
                  placeholder="name"
                  value={definitionForm.name}
                />
              </Field>
              <Field label="分类">
                <Input
                  onChange={(event) =>
                    setDefinitionForm((value) => ({
                      ...value,
                      category: event.target.value,
                    }))
                  }
                  placeholder="category"
                  value={definitionForm.category}
                />
              </Field>
              <Field hint="可留空；填写时必须是 UUID" label="表单元数据 ID">
                <div className="grid gap-2">
                  <Input
                    onChange={(event) =>
                      setDefinitionForm((value) => ({
                        ...value,
                        formMetadataId: event.target.value,
                      }))
                    }
                    placeholder="formMetadataId"
                    value={definitionForm.formMetadataId}
                  />
                  <Button
                    disabled={createSampleMetadataMutation.isPending}
                    onClick={() => createSampleMetadataMutation.mutate()}
                    size="sm"
                    type="button"
                    variant="outline"
                  >
                    创建并绑定示例表单
                  </Button>
                </div>
              </Field>
              <Field label="开始节点">
                <Input
                  onChange={(event) =>
                    setDefinitionForm((value) => ({
                      ...value,
                      startNodeId: event.target.value,
                    }))
                  }
                  placeholder="startNodeId"
                  value={definitionForm.startNodeId}
                />
              </Field>
              <Field label="结束节点">
                <Input
                  onChange={(event) =>
                    setDefinitionForm((value) => ({
                      ...value,
                      endNodeId: event.target.value,
                    }))
                  }
                  placeholder="endNodeId"
                  value={definitionForm.endNodeId}
                />
              </Field>
            </div>
            <textarea
              className="min-h-32 w-full rounded-lg border border-slate-200 px-3 py-2 font-mono text-xs outline-none focus:border-sky-400 focus:ring-2 focus:ring-sky-100"
              onChange={(event) =>
                setDefinitionForm((value) => ({
                  ...value,
                  nodes: event.target.value,
                }))
              }
              value={definitionForm.nodes}
            />
            <textarea
              className="min-h-24 w-full rounded-lg border border-slate-200 px-3 py-2 font-mono text-xs outline-none focus:border-sky-400 focus:ring-2 focus:ring-sky-100"
              onChange={(event) =>
                setDefinitionForm((value) => ({
                  ...value,
                  routes: event.target.value,
                }))
              }
              value={definitionForm.routes}
            />
            <div className="flex flex-col gap-2 sm:flex-row">
              <Field hint="可留空；填写时必须是 UUID" label="发布人 ID">
                <Input
                  onChange={(event) =>
                    setDefinitionForm((value) => ({
                      ...value,
                      publishedBy: event.target.value,
                    }))
                  }
                  placeholder="publishedBy"
                  value={definitionForm.publishedBy}
                />
              </Field>
              <Button
                className="self-end"
                disabled={createDefinitionMutation.isPending}
                type="submit"
              >
                <GitBranch className="h-4 w-4" />
                保存定义
              </Button>
            </div>
            <ErrorLine
              error={
                definitionsQuery.error ??
                createDefinitionMutation.error ??
                createSampleMetadataMutation.error ??
                publishDefinitionMutation.error
              }
            />
          </form>
        </div>
      </section>

      <section className="grid gap-4 lg:grid-cols-[26rem_1fr]">
        <div className="rounded-lg border border-slate-200 bg-white p-4 shadow-sm">
          <div className="mb-3 flex items-center gap-2">
            <Play className="h-5 w-5 text-sky-600" />
            <h2 className="font-semibold text-slate-950">发起流程</h2>
          </div>
          <div className="mb-3 rounded-lg bg-slate-50 px-3 py-2 text-sm text-slate-600">
            {selectedDefinition
              ? `${selectedDefinition.name} v${selectedDefinition.version}`
              : '未选择已发布定义'}
          </div>
          <form
            className="space-y-2"
            onSubmit={(event) => {
              event.preventDefault()
              startMutation.mutate()
            }}
          >
            {[
              ['title', '流程标题', 'title', ''],
              ['businessKey', '业务键', 'businessKey', '可留空'],
              ['initiatorId', '发起人 ID', 'initiatorId', '必填 UUID'],
              ['initiatorOrgId', '发起组织 ID', 'initiatorOrgId', '必填 UUID'],
              ['initiatorDeptId', '发起部门 ID', 'initiatorDeptId', '可留空 UUID'],
              [
                'initiatorPositionId',
                '发起岗位 ID',
                'initiatorPositionId',
                '必填 UUID',
              ],
              ['formDataId', '表单数据 ID', 'formDataId', '必填 UUID'],
            ].map(([key, label, placeholder, hint]) => (
              <Field hint={hint} key={key} label={label}>
                <Input
                  onChange={(event) =>
                    setStartForm((value) => ({
                      ...value,
                      [key]: event.target.value,
                    }))
                  }
                  placeholder={placeholder}
                  value={startForm[key as keyof typeof startForm]}
                />
              </Field>
            ))}
            <textarea
              className="min-h-24 w-full rounded-lg border border-slate-200 px-3 py-2 font-mono text-xs outline-none focus:border-sky-400 focus:ring-2 focus:ring-sky-100"
              onChange={(event) =>
                setStartForm((value) => ({
                  ...value,
                  variables: event.target.value,
                }))
              }
              value={startForm.variables}
            />
            <Button
              disabled={!selectedDefinition || startMutation.isPending}
              type="submit"
            >
              <Play className="h-4 w-4" />
              发起
            </Button>
            <ErrorLine error={startMutation.error} />
          </form>
        </div>

        <div className="rounded-lg border border-slate-200 bg-white shadow-sm">
          <div className="flex flex-col gap-3 border-b border-slate-100 p-4 lg:flex-row lg:items-center">
            <div className="flex flex-1 items-center gap-2">
              <History className="h-5 w-5 text-sky-600" />
              <h2 className="font-semibold text-slate-950">实例详情</h2>
            </div>
            <Input
              className="lg:max-w-md"
              onChange={(event) => setInstanceIdInput(event.target.value)}
              placeholder="instanceId"
              value={instanceIdInput}
            />
            <Button
              disabled={!instanceIdInput || loadInstanceMutation.isPending}
              onClick={() => loadInstanceMutation.mutate(instanceIdInput)}
              variant="outline"
            >
              <RotateCw className="h-4 w-4" />
              载入
            </Button>
          </div>

          {detail ? (
            <div className="grid gap-4 p-4 xl:grid-cols-[1fr_24rem]">
              <div className="space-y-4">
                <div className="rounded-lg border border-slate-200 p-3">
                  <div className="flex flex-wrap items-center gap-2">
                    <span className="font-semibold text-slate-950">
                      {detail.instance.title}
                    </span>
                    <Badge variant={statusVariant(detail.instance.status)}>
                      {detail.instance.status}
                    </Badge>
                    <span className="text-xs text-slate-500">
                      {detail.instance.definitionCode} v
                      {detail.instance.definitionVersion}
                    </span>
                  </div>
                  <p className="mt-1 text-xs text-slate-500">
                    {detail.instance.id} · {detail.instance.businessKey ?? '-'}
                  </p>
                </div>

                <div className="grid gap-2 sm:grid-cols-2">
                  {detail.tasks.map((task) => (
                    <TaskLine
                      key={task.id}
                      onSelect={() => setSelectedTaskId(task.id)}
                      selected={activeTask?.id === task.id}
                      task={task}
                    />
                  ))}
                </div>

                <div className="rounded-lg border border-slate-200">
                  <div className="border-b border-slate-100 px-3 py-2 text-sm font-semibold text-slate-950">
                    节点轨迹
                  </div>
                  <div className="divide-y divide-slate-100">
                    {detail.nodeHistory.map((item) => (
                      <div className="px-3 py-2 text-sm" key={item.id}>
                        <div className="flex flex-wrap items-center gap-2">
                          <span className="font-medium">{item.nodeName}</span>
                          <Badge variant={statusVariant(item.status)}>
                            {item.status}
                          </Badge>
                          <span className="text-xs text-slate-500">
                            {formatTime(item.occurredAt)}
                          </span>
                        </div>
                        <p className="mt-1 text-xs text-slate-500">
                          {item.nodeId} · {item.actionCode ?? '-'} ·{' '}
                          {item.operatorId ?? '-'}
                        </p>
                      </div>
                    ))}
                  </div>
                </div>

                <div className="rounded-lg border border-slate-200">
                  <div className="border-b border-slate-100 px-3 py-2 text-sm font-semibold text-slate-950">
                    变量历史
                  </div>
                  <div className="divide-y divide-slate-100">
                    {detail.variableHistory.map((item) => (
                      <div className="px-3 py-2 text-sm" key={item.id}>
                        <span className="font-medium">{item.variableName}</span>
                        <span className="ml-2 text-slate-500">
                          {item.oldValue ?? '-'} → {item.newValue ?? '-'}
                        </span>
                      </div>
                    ))}
                  </div>
                </div>
              </div>

              <div className="space-y-3">
                <div className="rounded-lg border border-slate-200 p-3">
                  <p className="mb-2 text-sm font-semibold text-slate-950">
                    任务动作
                  </p>
                  <div className="grid gap-2">
                    {[
                      ['operatorAccountId', '操作账号', 'operatorAccountId', '必填'],
                      ['operatorPersonId', '操作人 ID', 'operatorPersonId', '必填 UUID'],
                      ['operatorOrgId', '操作组织 ID', 'operatorOrgId', '必填 UUID'],
                      [
                        'operatorPositionId',
                        '操作岗位 ID',
                        'operatorPositionId',
                        '必填 UUID',
                      ],
                      ['opinion', '审批意见', 'opinion', '驳回等动作可能必填'],
                      ['targetNodeId', '目标节点', 'targetNodeId', '退回类动作使用'],
                      [
                        'targetAssigneeIds',
                        '目标处理人 ID',
                        'targetAssigneeIds',
                        '多个 UUID 用逗号分隔',
                      ],
                    ].map(([key, label, placeholder, hint]) => (
                      <Field hint={hint} key={key} label={label}>
                        <Input
                          onChange={(event) =>
                            setActionForm((value) => ({
                              ...value,
                              [key]: event.target.value,
                            }))
                          }
                          placeholder={placeholder}
                          value={actionForm[key as keyof typeof actionForm]}
                        />
                      </Field>
                    ))}
                    <textarea
                      className="min-h-24 w-full rounded-lg border border-slate-200 px-3 py-2 font-mono text-xs outline-none focus:border-sky-400 focus:ring-2 focus:ring-sky-100"
                      onChange={(event) =>
                        setActionForm((value) => ({
                          ...value,
                          formDataPatch: event.target.value,
                        }))
                      }
                      value={actionForm.formDataPatch}
                    />
                  </div>
                  <div className="mt-3 flex flex-wrap gap-2">
                    {(actionsQuery.data ?? []).map((action) => (
                      <Button
                        disabled={
                          !activeTask ||
                          executeActionMutation.isPending ||
                          (action.requireOpinion && !actionForm.opinion) ||
                          (action.requireTarget &&
                            !actionForm.targetNodeId &&
                            !actionForm.targetAssigneeIds) ||
                          (requiresTargetAssignee(action) &&
                            !actionForm.targetAssigneeIds.trim())
                        }
                        key={action.code}
                        onClick={() => executeActionMutation.mutate(action)}
                        size="sm"
                        variant={
                          action.category === 'APPROVE' ? 'default' : 'outline'
                        }
                      >
                        <Send className="h-4 w-4" />
                        {action.name}
                      </Button>
                    ))}
                  </div>
                  <ErrorLine
                    error={actionsQuery.error ?? executeActionMutation.error}
                  />
                </div>

                <div className="rounded-lg border border-slate-200">
                  <div className="border-b border-slate-100 px-3 py-2 text-sm font-semibold text-slate-950">
                    操作历史
                  </div>
                  <div className="divide-y divide-slate-100">
                    {detail.actions.map((action) => (
                      <div className="px-3 py-2 text-sm" key={action.id}>
                        <div className="font-medium text-slate-950">
                          {action.actionName}
                        </div>
                        <p className="mt-1 text-xs text-slate-500">
                          {action.operatorId} · {formatTime(action.createdAt)}
                        </p>
                      </div>
                    ))}
                  </div>
                </div>
              </div>
            </div>
          ) : (
            <div className="px-4 py-10 text-sm text-slate-500">
              暂无实例详情
            </div>
          )}
          <ErrorLine error={loadInstanceMutation.error} />
        </div>
      </section>
    </div>
  )
}

function WorkflowSubpageFallback(): ReactElement {
  return (
    <div className="rounded-lg border border-slate-200 bg-white p-6 text-sm text-slate-500 shadow-sm">
      Loading workflow workspace...
    </div>
  )
}

export default function WorkflowPage(): ReactElement {
  const location = useLocation()

  if (location.pathname.startsWith('/workflow/forms')) {
    return (
      <Suspense fallback={<WorkflowSubpageFallback />}>
        <FormDesignerPage />
      </Suspense>
    )
  }

  if (location.pathname.startsWith('/workflow/render')) {
    return (
      <Suspense fallback={<WorkflowSubpageFallback />}>
        <FormRendererPage />
      </Suspense>
    )
  }

  if (location.pathname.startsWith('/workflow/monitor')) {
    return (
      <Suspense fallback={<WorkflowSubpageFallback />}>
        <ProcessMonitorPage />
      </Suspense>
    )
  }

  return <ProcessEnginePage />
}
