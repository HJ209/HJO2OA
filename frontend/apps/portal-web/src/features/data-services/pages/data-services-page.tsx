import {
  useEffect,
  useMemo,
  useState,
  type FormEvent,
  type ReactElement,
} from 'react'
import {
  Activity,
  AlertTriangle,
  BarChart3,
  Database,
  Eye,
  FileDown,
  KeyRound,
  Pause,
  Play,
  Power,
  RefreshCcw,
  RotateCcw,
  Save,
  Server,
  ShieldCheck,
  Trash2,
  Webhook,
  type LucideIcon,
} from 'lucide-react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import {
  createClientId,
  dataServicesService,
  getCurrentOperatorId,
  getCurrentTenantId,
  type GovernanceListQuery,
  type ReportExportFile,
  type ReportPreviewQuery,
} from '@/features/data-services/services/data-services-service'
import type {
  AlertStatus,
  ApiCredentialGrant,
  ApiInvocationAuditLog,
  ApiRateLimitPolicy,
  CheckpointMode,
  CompensationAction,
  ConflictStrategy,
  ConnectorAuthMode,
  ConnectorDetail,
  ConnectorHealthSnapshot,
  ConnectorStatus,
  ConnectorSummary,
  ConnectorType,
  DataServiceDetail,
  DataServiceFieldMapping,
  DataServiceInvocationRequest,
  DataServiceParameter,
  DataServiceParameterType,
  DataServicePermissionMode,
  DataServiceSourceMode,
  DataServiceStatus,
  DataServiceSummary,
  DataServiceType,
  ExecutionStatus,
  ExecutionTriggerType,
  GovernanceActionType,
  GovernanceAlertRecord,
  GovernanceProfile,
  GovernanceScopeType,
  GovernanceTraceRecord,
  OpenApiAuthType,
  OpenApiEndpointDetail,
  OpenApiEndpointSummary,
  OpenApiHttpMethod,
  OpenApiStatus,
  ReportDefinition,
  ReportRefreshMode,
  ReportSnapshot,
  ReportSourceScope,
  ReportStatus,
  ReportType,
  ReportVisibilityMode,
  SaveDataServiceRequest,
  SaveSyncTaskRequest,
  SyncExecutionDetail,
  SyncExecutionSummary,
  SyncMode,
  SyncTaskDetail,
  SyncTaskStatus,
  SyncTaskSummary,
  SyncTaskType,
  UpsertConnectorRequest,
  UpsertOpenApiEndpointRequest,
} from '@/features/data-services/types/data-services'
import { isBizError } from '@/services/error-mapper'
import { useIdentityStore } from '@/stores/identity-store'
import type { PageData, Pagination } from '@/types/api'
import { cn } from '@/utils/cn'
import { formatUtcToUserTimezone } from '@/utils/format-time'

type TabKey =
  | 'connector'
  | 'sync'
  | 'service'
  | 'openApi'
  | 'report'
  | 'governance'

const PAGE_SIZE = 10

const TAB_ITEMS: Array<{
  key: TabKey
  label: string
  description: string
  icon: LucideIcon
}> = [
  {
    key: 'connector',
    label: 'Connector',
    description: '连接器定义、参数、连通性测试和健康状态',
    icon: Database,
  },
  {
    key: 'sync',
    label: 'DataSync',
    description: '同步任务、调度配置、执行记录、失败详情和补偿重试',
    icon: RefreshCcw,
  },
  {
    key: 'service',
    label: 'DataService',
    description: '数据服务定义、参数、权限、启停和调用预览',
    icon: Server,
  },
  {
    key: 'openApi',
    label: 'OpenApi',
    description: '开放接口、授权凭证、调用日志和限流策略',
    icon: Webhook,
  },
  {
    key: 'report',
    label: 'Report',
    description: '报表定义、参数预览、刷新和导出入口',
    icon: BarChart3,
  },
  {
    key: 'governance',
    label: 'Governance',
    description: '数据质量、血缘追踪、权限干预和审计视图',
    icon: ShieldCheck,
  },
]

const CONNECTOR_TYPES: ConnectorType[] = [
  'HTTP',
  'DATABASE',
  'MQ',
  'FILE',
  'SAAS',
]
const CONNECTOR_AUTH_MODES: ConnectorAuthMode[] = [
  'NONE',
  'BASIC',
  'TOKEN',
  'SECRET_REF',
]
const CONNECTOR_STATUSES: ConnectorStatus[] = ['DRAFT', 'ACTIVE', 'DISABLED']
const SYNC_TASK_TYPES: SyncTaskType[] = ['IMPORT', 'EXPORT', 'BIDIRECTIONAL']
const SYNC_MODES: SyncMode[] = ['FULL', 'INCREMENTAL', 'EVENT_DRIVEN']
const SYNC_STATUSES: SyncTaskStatus[] = ['DRAFT', 'ACTIVE', 'PAUSED', 'ERROR']
const CHECKPOINT_MODES: CheckpointMode[] = [
  'OFFSET',
  'TIMESTAMP',
  'VERSION',
  'NONE',
]
const EXECUTION_STATUSES: ExecutionStatus[] = [
  'RUNNING',
  'SUCCESS',
  'FAILED',
  'COMPENSATING',
]
const EXECUTION_TRIGGERS: ExecutionTriggerType[] = [
  'MANUAL',
  'SCHEDULED',
  'EVENT_DRIVEN',
  'RETRY',
  'RECONCILIATION',
  'COMPENSATION',
]
const SERVICE_TYPES: DataServiceType[] = [
  'QUERY',
  'COMMAND',
  'EXPORT',
  'CALLBACK',
]
const SERVICE_SOURCE_MODES: DataServiceSourceMode[] = [
  'INTERNAL_QUERY',
  'CONNECTOR',
  'MIXED',
]
const SERVICE_PERMISSION_MODES: DataServicePermissionMode[] = [
  'PUBLIC_INTERNAL',
  'APP_SCOPED',
  'SUBJECT_SCOPED',
]
const SERVICE_STATUSES: DataServiceStatus[] = [
  'DRAFT',
  'ACTIVE',
  'DEPRECATED',
  'DISABLED',
]
const PARAM_TYPES: DataServiceParameterType[] = [
  'STRING',
  'NUMBER',
  'BOOLEAN',
  'DATE',
  'JSON',
  'PAGEABLE',
]
const OPEN_API_METHODS: OpenApiHttpMethod[] = ['GET', 'POST', 'PUT', 'DELETE']
const OPEN_API_STATUSES: OpenApiStatus[] = [
  'DRAFT',
  'ACTIVE',
  'DEPRECATED',
  'OFFLINE',
]
const OPEN_API_AUTH_TYPES: OpenApiAuthType[] = [
  'APP_KEY',
  'SIGNATURE',
  'OAUTH2',
  'INTERNAL',
]
const REPORT_TYPES: ReportType[] = ['TREND', 'RANK', 'SUMMARY', 'CARD']
const REPORT_SCOPES: ReportSourceScope[] = [
  'PROCESS',
  'CONTENT',
  'MEETING',
  'TASK',
  'ATTENDANCE',
  'MESSAGE',
  'MIXED',
]
const REPORT_REFRESH_MODES: ReportRefreshMode[] = [
  'SCHEDULED',
  'EVENT_DRIVEN',
  'ON_DEMAND',
]
const REPORT_VISIBILITY_MODES: ReportVisibilityMode[] = [
  'INTERNAL',
  'PORTAL_CARD',
  'OPEN_API',
]
const REPORT_STATUSES: ReportStatus[] = ['DRAFT', 'ACTIVE', 'ARCHIVED']

function downloadReportFile(file: ReportExportFile): void {
  const url = URL.createObjectURL(file.blob)
  const link = document.createElement('a')
  link.href = url
  link.download = file.filename
  document.body.appendChild(link)
  link.click()
  link.remove()
  URL.revokeObjectURL(url)
}
const GOVERNANCE_SCOPES: GovernanceScopeType[] = [
  'API',
  'CONNECTOR',
  'SYNC',
  'REPORT',
  'MODULE',
]
const ALERT_STATUSES: AlertStatus[] = [
  'OPEN',
  'ACKNOWLEDGED',
  'ESCALATED',
  'CLOSED',
]
const GOVERNANCE_ACTIONS: GovernanceActionType[] = [
  'REQUEST_DISABLE',
  'REQUEST_RETRY',
  'REQUEST_DEGRADE',
  'REQUEST_COMPENSATION',
  'ADD_NOTE',
]

interface ConnectorForm {
  connectorId: string
  code: string
  name: string
  connectorType: ConnectorType
  vendor: string
  protocol: string
  authMode: ConnectorAuthMode
  connectTimeoutMs: number
  readTimeoutMs: number
  retryCount: number
  retryIntervalMs: number
  parametersText: string
}

interface SyncForm {
  code: string
  name: string
  description: string
  taskType: SyncTaskType
  syncMode: SyncMode
  sourceConnectorId: string
  targetConnectorId: string
  checkpointMode: CheckpointMode
  checkpointField: string
  idempotencyField: string
  manualTriggerEnabled: boolean
  scheduleEnabled: boolean
  cron: string
  mappingRulesText: string
}

interface ServiceForm {
  serviceId: string
  code: string
  name: string
  serviceType: DataServiceType
  sourceMode: DataServiceSourceMode
  permissionMode: DataServicePermissionMode
  sourceRef: string
  connectorId: string
  description: string
  allowedAppCodes: string
  allowedSubjectIds: string
  requiredRoles: string
  cacheEnabled: boolean
  cacheTtlSeconds: number
  parametersText: string
  fieldMappingsText: string
}

interface OpenApiForm {
  code: string
  version: string
  name: string
  dataServiceCode: string
  path: string
  httpMethod: OpenApiHttpMethod
  authType: OpenApiAuthType
  compatibilityNotes: string
  clientCode: string
  secretRef: string
  scopes: string
  expiresAt: string
  policyCode: string
  policyClientCode: string
  policyType: 'RATE_LIMIT' | 'QUOTA'
  windowValue: number
  windowUnit: 'SECOND' | 'MINUTE' | 'HOUR' | 'DAY' | 'MONTH'
  threshold: number
}

interface ReportForm {
  code: string
  name: string
  reportType: ReportType
  sourceScope: ReportSourceScope
  refreshMode: ReportRefreshMode
  visibilityMode: ReportVisibilityMode
  status: ReportStatus
  sourceProviderKey: string
  subjectCode: string
  dataServiceCode: string
  metricCode: string
  metricName: string
  dimensionCode: string
  dimensionName: string
  filtersText: string
}

interface GovernanceForm {
  profileCode: string
  scopeType: GovernanceScopeType
  targetCode: string
  slaPolicyJson: string
  alertPolicyJson: string
  version: string
  changeSummary: string
  approvalNote: string
  interventionTargetType: GovernanceScopeType
  interventionTargetCode: string
  interventionTraceId: string
  interventionActionType: GovernanceActionType
  interventionReason: string
}

function getErrorText(error: unknown): string {
  if (!error) {
    return ''
  }

  if (isBizError(error)) {
    const request = error.requestId ? ` RequestId: ${error.requestId}` : ''

    return `${error.message}${request}`
  }

  if (error instanceof Error) {
    return error.message
  }

  return '请求失败'
}

function emptyPagination(): Pagination {
  return {
    page: 1,
    size: PAGE_SIZE,
    total: 0,
    totalPages: 0,
  }
}

function getPagination<T>(pageData?: PageData<T>): Pagination {
  return pageData?.pagination ?? emptyPagination()
}

function formatTime(value?: string | null): string {
  return value ? formatUtcToUserTimezone(value) : '暂无'
}

function splitList(value: string): string[] {
  return value
    .split(/[,\n]/)
    .map((item) => item.trim())
    .filter(Boolean)
}

function parseKeyValueLines(value: string): Record<string, string> {
  const result: Record<string, string> = {}

  value
    .split('\n')
    .map((line) => line.trim())
    .filter(Boolean)
    .forEach((line) => {
      const [key, ...rest] = line.split('=')
      const cleanKey = key.trim()

      if (cleanKey) {
        result[cleanKey] = rest.join('=').trim()
      }
    })

  return result
}

function parseJsonObject(value: string): Record<string, unknown> {
  if (!value.trim()) {
    return {}
  }

  const parsed = JSON.parse(value) as unknown

  if (typeof parsed !== 'object' || parsed === null || Array.isArray(parsed)) {
    throw new Error('JSON 必须是对象')
  }

  return parsed as Record<string, unknown>
}

function toJson(value: unknown): string {
  return JSON.stringify(value, null, 2)
}

function parseConnectorParameters(value: string) {
  return value
    .split('\n')
    .map((line) => line.trim())
    .filter(Boolean)
    .map((line) => {
      const [left, sensitiveText = 'false'] = line.split('|')
      const [paramKey, ...rest] = left.split('=')

      return {
        paramKey: paramKey.trim(),
        paramValueRef: rest.join('=').trim(),
        sensitive: sensitiveText.trim().toLowerCase() === 'true',
      }
    })
    .filter((item) => item.paramKey)
}

function connectorParametersToText(detail?: ConnectorDetail): string {
  return (
    detail?.parameters
      .map(
        (item) =>
          `${item.paramKey}=${item.paramValueRef}|${String(item.sensitive)}`,
      )
      .join('\n') ?? ''
  )
}

function parseServiceParameters(value: string): DataServiceParameter[] {
  return value
    .split('\n')
    .map((line) => line.trim())
    .filter(Boolean)
    .map((line, index) => {
      const [paramCode, typeText = 'STRING', requiredText = 'false'] =
        line.split(':')
      const paramType = PARAM_TYPES.includes(
        typeText.trim() as DataServiceParameterType,
      )
        ? (typeText.trim() as DataServiceParameterType)
        : 'STRING'

      return {
        paramCode: paramCode.trim(),
        paramType,
        required: requiredText.trim().toLowerCase() === 'true',
        validationRule: { allowedValues: [] },
        enabled: true,
        sortOrder: index + 1,
      }
    })
    .filter((item) => item.paramCode)
}

function serviceParametersToText(parameters: DataServiceParameter[]): string {
  return parameters
    .map((item) => `${item.paramCode}:${item.paramType}:${item.required}`)
    .join('\n')
}

function parseFieldMappings(value: string): DataServiceFieldMapping[] {
  return value
    .split('\n')
    .map((line) => line.trim())
    .filter(Boolean)
    .map((line, index) => {
      const [sourceField, targetField = sourceField] = line.split('->')

      return {
        sourceField: sourceField.trim(),
        targetField: targetField.trim(),
        transformRule: { type: 'DIRECT' as const },
        masked: false,
        sortOrder: index + 1,
      }
    })
    .filter((item) => item.sourceField && item.targetField)
}

function fieldMappingsToText(mappings: DataServiceFieldMapping[]): string {
  return mappings
    .map((item) => `${item.sourceField}->${item.targetField}`)
    .join('\n')
}

function parseSyncMappings(value: string) {
  return value
    .split('\n')
    .map((line) => line.trim())
    .filter(Boolean)
    .map((line, index) => {
      const [fieldPair, strategyText = 'MERGE', keyText = 'false'] =
        line.split('|')
      const [sourceField, targetField = sourceField] = fieldPair.split('->')
      const strategy = ['OVERWRITE', 'SKIP', 'MERGE', 'MANUAL'].includes(
        strategyText.trim(),
      )
        ? (strategyText.trim() as ConflictStrategy)
        : 'MERGE'

      return {
        sourceField: sourceField.trim(),
        targetField: targetField.trim(),
        conflictStrategy: strategy,
        keyMapping: keyText.trim().toLowerCase() === 'true',
        sortOrder: index + 1,
      }
    })
    .filter((item) => item.sourceField && item.targetField)
}

function syncMappingsToText(detail?: SyncTaskDetail): string {
  return (
    detail?.mappingRules
      .map(
        (item) =>
          `${item.sourceField}->${item.targetField}|${item.conflictStrategy}|${item.keyMapping}`,
      )
      .join('\n') ?? ''
  )
}

function statusBadgeClass(status: string): string {
  if (
    [
      'ACTIVE',
      'HEALTHY',
      'SUCCESS',
      'READY',
      'PUBLISHED',
      'CONSISTENT',
    ].includes(status)
  ) {
    return 'bg-emerald-100 text-emerald-700'
  }

  if (
    [
      'FAILED',
      'ERROR',
      'UNREACHABLE',
      'UNHEALTHY',
      'OPEN',
      'CRITICAL',
      'OFFLINE',
    ].includes(status)
  ) {
    return 'bg-rose-100 text-rose-700'
  }

  if (
    ['PAUSED', 'DEGRADED', 'WARN', 'DRAFT', 'STALE', 'DEPRECATED'].includes(
      status,
    )
  ) {
    return 'bg-amber-100 text-amber-700'
  }

  return 'bg-slate-100 text-slate-700'
}

function StatusBadge({ status }: { status?: string | null }): ReactElement {
  return (
    <span
      className={cn(
        'inline-flex rounded-full px-2 py-0.5 text-xs font-semibold',
        statusBadgeClass(status ?? 'UNKNOWN'),
      )}
    >
      {status ?? 'UNKNOWN'}
    </span>
  )
}

function PanelTitle({
  title,
  description,
  icon: Icon,
}: {
  title: string
  description: string
  icon: LucideIcon
}): ReactElement {
  return (
    <div className="flex items-center justify-between gap-3 border-b border-slate-100 px-4 py-3">
      <div className="flex items-center gap-3">
        <span className="flex h-9 w-9 items-center justify-center rounded-lg bg-slate-900 text-white">
          <Icon className="h-4 w-4" />
        </span>
        <div>
          <h2 className="text-base font-semibold text-slate-950">{title}</h2>
          <p className="text-sm text-slate-500">{description}</p>
        </div>
      </div>
    </div>
  )
}

function StateBlock({
  isLoading,
  error,
  isEmpty,
  onRetry,
}: {
  isLoading: boolean
  error: unknown
  isEmpty: boolean
  onRetry: () => void
}): ReactElement | null {
  if (isLoading) {
    return (
      <div className="rounded-lg border border-slate-200 bg-white p-4 text-sm text-slate-500">
        加载中...
      </div>
    )
  }

  if (error) {
    return (
      <div className="rounded-lg border border-rose-200 bg-rose-50 p-4 text-sm text-rose-700">
        <div className="flex items-center gap-2 font-semibold">
          <AlertTriangle className="h-4 w-4" />
          加载失败
        </div>
        <p className="mt-1 break-words">{getErrorText(error)}</p>
        <Button className="mt-3" onClick={onRetry} size="sm" variant="outline">
          <RotateCcw className="h-4 w-4" />
          重试
        </Button>
      </div>
    )
  }

  if (isEmpty) {
    return (
      <div className="rounded-lg border border-dashed border-slate-300 bg-white p-4 text-sm text-slate-500">
        暂无数据
      </div>
    )
  }

  return null
}

function Field({
  label,
  children,
  error,
}: {
  label: string
  children: ReactElement
  error?: string
}): ReactElement {
  return (
    <label className="space-y-1.5 text-sm font-medium text-slate-700">
      <span>{label}</span>
      {children}
      {error ? (
        <span className="block text-xs text-rose-600">{error}</span>
      ) : null}
    </label>
  )
}

function SelectField<TValue extends string>({
  label,
  value,
  options,
  onChange,
  includeAll = false,
}: {
  label: string
  value: TValue | ''
  options: readonly TValue[]
  onChange: (value: TValue | '') => void
  includeAll?: boolean
}): ReactElement {
  return (
    <Field label={label}>
      <select
        className="h-10 w-full rounded-xl border border-slate-200 bg-white px-3 text-sm text-slate-900 shadow-sm outline-none focus:border-sky-400 focus:ring-2 focus:ring-sky-100"
        onChange={(event) => onChange(event.target.value as TValue | '')}
        value={value}
      >
        {includeAll ? <option value="">全部</option> : null}
        {options.map((option) => (
          <option key={option} value={option}>
            {option}
          </option>
        ))}
      </select>
    </Field>
  )
}

function TextareaField({
  label,
  value,
  onChange,
  rows = 4,
  error,
}: {
  label: string
  value: string
  onChange: (value: string) => void
  rows?: number
  error?: string
}): ReactElement {
  return (
    <Field error={error} label={label}>
      <textarea
        className="min-h-24 w-full rounded-xl border border-slate-200 bg-white px-3 py-2 text-sm text-slate-900 shadow-sm outline-none focus:border-sky-400 focus:ring-2 focus:ring-sky-100"
        onChange={(event) => onChange(event.target.value)}
        rows={rows}
        value={value}
      />
    </Field>
  )
}

function TextField({
  label,
  value,
  onChange,
  error,
  type = 'text',
}: {
  label: string
  value: string | number
  onChange: (value: string) => void
  error?: string
  type?: 'text' | 'number' | 'datetime-local'
}): ReactElement {
  return (
    <Field error={error} label={label}>
      <Input
        onChange={(event) => onChange(event.target.value)}
        type={type}
        value={value}
      />
    </Field>
  )
}

function CheckboxField({
  label,
  checked,
  onChange,
}: {
  label: string
  checked: boolean
  onChange: (checked: boolean) => void
}): ReactElement {
  return (
    <label className="flex h-10 items-center gap-2 rounded-xl border border-slate-200 bg-white px-3 text-sm font-medium text-slate-700 shadow-sm">
      <input
        checked={checked}
        className="h-4 w-4"
        onChange={(event) => onChange(event.target.checked)}
        type="checkbox"
      />
      {label}
    </label>
  )
}

function PaginationBar({
  pagination,
  setPage,
}: {
  pagination: Pagination
  setPage: (page: number) => void
}): ReactElement {
  const current = pagination.page || 1
  const totalPages = Math.max(pagination.totalPages || 1, 1)

  return (
    <div className="flex items-center justify-between gap-3 border-t border-slate-100 px-4 py-3 text-sm text-slate-500">
      <span>
        共 {pagination.total} 条，第 {current}/{totalPages} 页
      </span>
      <div className="flex items-center gap-2">
        <Button
          disabled={current <= 1}
          onClick={() => setPage(current - 1)}
          size="sm"
          variant="outline"
        >
          上一页
        </Button>
        <Button
          disabled={current >= totalPages}
          onClick={() => setPage(current + 1)}
          size="sm"
          variant="outline"
        >
          下一页
        </Button>
      </div>
    </div>
  )
}

function JsonBlock({ value }: { value: unknown }): ReactElement {
  return (
    <pre className="max-h-72 overflow-auto rounded-lg bg-slate-950 p-3 text-xs leading-5 text-slate-100">
      {toJson(value)}
    </pre>
  )
}

function PermissionState(): ReactElement {
  const roleIds = useIdentityStore((state) => state.roleIds)
  const canOperate = roleIds.some(
    (roleId) => roleId.includes('ADMIN') || roleId.includes('DATA'),
  )

  return (
    <div
      className={cn(
        'rounded-lg border px-4 py-3 text-sm',
        canOperate
          ? 'border-emerald-200 bg-emerald-50 text-emerald-700'
          : 'border-amber-200 bg-amber-50 text-amber-700',
      )}
    >
      当前身份：{roleIds.length ? roleIds.join(', ') : '未绑定角色'}。
      {canOperate ? '具备数据服务操作角色。' : '写操作将由后端权限继续校验。'}
    </div>
  )
}

function ConnectorPanel({ active }: { active: boolean }): ReactElement {
  const queryClient = useQueryClient()
  const [page, setPage] = useState(1)
  const [keyword, setKeyword] = useState('')
  const [type, setType] = useState<ConnectorType | ''>('')
  const [status, setStatus] = useState<ConnectorStatus | ''>('')
  const [selectedId, setSelectedId] = useState('')
  const [formError, setFormError] = useState('')
  const [form, setForm] = useState<ConnectorForm>(() => ({
    connectorId: createClientId('connector'),
    code: '',
    name: '',
    connectorType: 'HTTP',
    vendor: '',
    protocol: 'https',
    authMode: 'NONE',
    connectTimeoutMs: 5000,
    readTimeoutMs: 15000,
    retryCount: 2,
    retryIntervalMs: 1000,
    parametersText: '',
  }))

  const connectorsQuery = useQuery({
    enabled: active,
    queryKey: ['data-services', 'connectors', page, keyword, type, status],
    queryFn: () =>
      dataServicesService.listConnectors({
        keyword,
        connectorType: type || undefined,
        status: status || undefined,
        page,
        size: PAGE_SIZE,
      }),
  })
  const connectorItems = useMemo(
    () => connectorsQuery.data?.items ?? [],
    [connectorsQuery.data?.items],
  )

  useEffect(() => {
    if (!selectedId && connectorItems[0]) {
      setSelectedId(connectorItems[0].connectorId)
    }
  }, [connectorItems, selectedId])

  const detailQuery = useQuery({
    enabled: active && Boolean(selectedId),
    queryKey: ['data-services', 'connectors', selectedId, 'detail'],
    queryFn: () => dataServicesService.getConnector(selectedId),
  })
  const templatesQuery = useQuery({
    enabled: active && Boolean(selectedId),
    queryKey: ['data-services', 'connectors', selectedId, 'templates'],
    queryFn: () =>
      dataServicesService.listConnectorParameterTemplates(selectedId),
  })
  const healthQuery = useQuery({
    enabled: active && Boolean(selectedId),
    queryKey: ['data-services', 'connectors', selectedId, 'health'],
    queryFn: () => dataServicesService.connectorHealth(selectedId),
  })
  const historyQuery = useQuery({
    enabled: active && Boolean(selectedId),
    queryKey: ['data-services', 'connectors', selectedId, 'history'],
    queryFn: () =>
      dataServicesService.connectorTestHistory(selectedId, { limit: 8 }),
  })

  const invalidate = () =>
    queryClient.invalidateQueries({ queryKey: ['data-services', 'connectors'] })

  const saveMutation = useMutation({
    mutationFn: () => {
      const payload: UpsertConnectorRequest = {
        code: form.code.trim(),
        name: form.name.trim(),
        connectorType: form.connectorType,
        vendor: form.vendor.trim() || undefined,
        protocol: form.protocol.trim() || undefined,
        authMode: form.authMode,
        timeoutConfig: {
          connectTimeoutMs: Number(form.connectTimeoutMs),
          readTimeoutMs: Number(form.readTimeoutMs),
          retryCount: Number(form.retryCount),
          retryIntervalMs: Number(form.retryIntervalMs),
        },
      }

      return dataServicesService.upsertConnector(form.connectorId, payload)
    },
    onSuccess: (detail) => {
      setSelectedId(detail.connectorId)
      invalidate()
    },
  })
  const saveParametersMutation = useMutation({
    mutationFn: () =>
      dataServicesService.saveConnectorParameters(
        selectedId,
        parseConnectorParameters(form.parametersText),
      ),
    onSuccess: invalidate,
  })
  const connectorActionMutation = useMutation({
    mutationFn: async (request: {
      action: 'activate' | 'disable' | 'test' | 'refresh' | 'confirm'
      snapshot?: ConnectorHealthSnapshot
    }) => {
      if (request.action === 'activate') {
        await dataServicesService.activateConnector(selectedId)
        return
      }
      if (request.action === 'disable') {
        await dataServicesService.disableConnector(selectedId)
        return
      }
      if (request.action === 'test') {
        await dataServicesService.testConnector(selectedId)
        return
      }
      if (request.action === 'confirm' && request.snapshot) {
        await dataServicesService.confirmConnectorHealth(
          selectedId,
          request.snapshot.snapshotId,
          'portal-web-confirmed',
        )
        return
      }

      await dataServicesService.refreshConnectorHealth(selectedId)
    },
    onSuccess: invalidate,
  })

  const loadDetailToForm = (detail?: ConnectorDetail) => {
    if (!detail) {
      return
    }

    setForm({
      connectorId: detail.connectorId,
      code: detail.code,
      name: detail.name,
      connectorType: detail.connectorType,
      vendor: detail.vendor ?? '',
      protocol: detail.protocol ?? '',
      authMode: detail.authMode,
      connectTimeoutMs: detail.timeoutConfig.connectTimeoutMs,
      readTimeoutMs: detail.timeoutConfig.readTimeoutMs,
      retryCount: detail.timeoutConfig.retryCount,
      retryIntervalMs: detail.timeoutConfig.retryIntervalMs,
      parametersText: connectorParametersToText(detail),
    })
  }

  const handleSave = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    if (!form.connectorId.trim() || !form.code.trim() || !form.name.trim()) {
      setFormError('connectorId、code、name 必填')
      return
    }
    setFormError('')
    saveMutation.mutate()
  }

  return (
    <section className="rounded-lg border border-slate-200 bg-white shadow-sm">
      <PanelTitle
        description={TAB_ITEMS[0].description}
        icon={Database}
        title="Connector 工作台"
      />
      <div className="grid gap-4 p-4 xl:grid-cols-[minmax(0,1fr)_420px]">
        <div className="space-y-4">
          <div className="grid gap-3 md:grid-cols-4">
            <TextField label="关键字" onChange={setKeyword} value={keyword} />
            <SelectField
              includeAll
              label="类型"
              onChange={setType}
              options={CONNECTOR_TYPES}
              value={type}
            />
            <SelectField
              includeAll
              label="状态"
              onChange={setStatus}
              options={CONNECTOR_STATUSES}
              value={status}
            />
            <Button
              className="self-end"
              onClick={() => connectorsQuery.refetch()}
              variant="outline"
            >
              <RefreshCcw className="h-4 w-4" />
              刷新
            </Button>
          </div>
          <StateBlock
            error={connectorsQuery.error}
            isEmpty={!connectorItems.length}
            isLoading={connectorsQuery.isLoading}
            onRetry={() => connectorsQuery.refetch()}
          />
          {connectorItems.length ? (
            <div className="overflow-auto rounded-lg border border-slate-200">
              <table className="min-w-full divide-y divide-slate-100 text-sm">
                <thead className="bg-slate-50 text-left text-xs uppercase text-slate-500">
                  <tr>
                    <th className="px-3 py-2">编码</th>
                    <th className="px-3 py-2">类型</th>
                    <th className="px-3 py-2">状态</th>
                    <th className="px-3 py-2">健康</th>
                    <th className="px-3 py-2">更新时间</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {connectorItems.map((item: ConnectorSummary) => (
                    <tr
                      className={cn(
                        'cursor-pointer hover:bg-slate-50',
                        selectedId === item.connectorId && 'bg-sky-50',
                      )}
                      key={item.connectorId}
                      onClick={() => setSelectedId(item.connectorId)}
                    >
                      <td className="px-3 py-2 font-medium text-slate-900">
                        {item.code}
                        <div className="text-xs font-normal text-slate-500">
                          {item.name}
                        </div>
                      </td>
                      <td className="px-3 py-2">{item.connectorType}</td>
                      <td className="px-3 py-2">
                        <StatusBadge status={item.status} />
                      </td>
                      <td className="px-3 py-2">
                        <StatusBadge
                          status={item.latestHealthSnapshot?.healthStatus}
                        />
                      </td>
                      <td className="px-3 py-2 text-slate-500">
                        {formatTime(item.updatedAt)}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
              <PaginationBar
                pagination={getPagination(connectorsQuery.data)}
                setPage={setPage}
              />
            </div>
          ) : null}

          {detailQuery.data ? (
            <div className="grid gap-4 lg:grid-cols-2">
              <div className="rounded-lg border border-slate-200 p-4">
                <div className="flex items-center justify-between">
                  <h3 className="font-semibold text-slate-950">连接器详情</h3>
                  <Button
                    onClick={() => loadDetailToForm(detailQuery.data)}
                    size="sm"
                    variant="outline"
                  >
                    <Eye className="h-4 w-4" />
                    载入编辑
                  </Button>
                </div>
                <div className="mt-3 grid gap-2 text-sm text-slate-600">
                  <span>Vendor: {detailQuery.data.vendor ?? '暂无'}</span>
                  <span>Protocol: {detailQuery.data.protocol ?? '暂无'}</span>
                  <span>
                    参数数: {detailQuery.data.parameters.length}；模板数:{' '}
                    {templatesQuery.data?.length ?? 0}
                  </span>
                  <span>
                    测试样本: {healthQuery.data?.sampleSize ?? 0}；健康:{' '}
                    {healthQuery.data?.healthyCount ?? 0}
                  </span>
                </div>
                <div className="mt-3 flex flex-wrap gap-2">
                  <Button
                    disabled={connectorActionMutation.isPending}
                    onClick={() =>
                      connectorActionMutation.mutate({ action: 'test' })
                    }
                    size="sm"
                  >
                    <Play className="h-4 w-4" />
                    测试连接
                  </Button>
                  <Button
                    disabled={connectorActionMutation.isPending}
                    onClick={() =>
                      connectorActionMutation.mutate({ action: 'activate' })
                    }
                    size="sm"
                    variant="outline"
                  >
                    <Power className="h-4 w-4" />
                    启用
                  </Button>
                  <Button
                    disabled={connectorActionMutation.isPending}
                    onClick={() =>
                      connectorActionMutation.mutate({ action: 'disable' })
                    }
                    size="sm"
                    variant="outline"
                  >
                    <Pause className="h-4 w-4" />
                    停用
                  </Button>
                  <Button
                    disabled={connectorActionMutation.isPending}
                    onClick={() =>
                      connectorActionMutation.mutate({ action: 'refresh' })
                    }
                    size="sm"
                    variant="outline"
                  >
                    <RefreshCcw className="h-4 w-4" />
                    健康刷新
                  </Button>
                </div>
              </div>
              <div className="rounded-lg border border-slate-200 p-4">
                <h3 className="font-semibold text-slate-950">测试记录</h3>
                <div className="mt-3 space-y-2">
                  {(historyQuery.data ?? []).map((item) => (
                    <div
                      className="rounded-lg border border-slate-100 p-3 text-sm"
                      key={item.snapshotId}
                    >
                      <div className="flex items-center justify-between gap-2">
                        <StatusBadge status={item.healthStatus} />
                        <span className="text-xs text-slate-500">
                          {formatTime(item.checkedAt)}
                        </span>
                      </div>
                      <p className="mt-1 text-slate-600">
                        {item.errorSummary ?? `Latency ${item.latencyMs}ms`}
                      </p>
                      {!item.confirmedAt ? (
                        <Button
                          className="mt-2"
                          onClick={() =>
                            connectorActionMutation.mutate({
                              action: 'confirm',
                              snapshot: item,
                            })
                          }
                          size="sm"
                          variant="outline"
                        >
                          确认
                        </Button>
                      ) : null}
                    </div>
                  ))}
                </div>
              </div>
            </div>
          ) : null}
        </div>

        <form
          className="space-y-3 rounded-lg border border-slate-200 p-4"
          onSubmit={handleSave}
        >
          <h3 className="font-semibold text-slate-950">创建 / 编辑连接器</h3>
          {formError ? (
            <p className="text-sm text-rose-600">{formError}</p>
          ) : null}
          <TextField
            label="Connector ID"
            onChange={(value) =>
              setForm((prev) => ({ ...prev, connectorId: value }))
            }
            value={form.connectorId}
          />
          <TextField
            error={!form.code.trim() && formError ? '必填' : undefined}
            label="编码"
            onChange={(value) => setForm((prev) => ({ ...prev, code: value }))}
            value={form.code}
          />
          <TextField
            error={!form.name.trim() && formError ? '必填' : undefined}
            label="名称"
            onChange={(value) => setForm((prev) => ({ ...prev, name: value }))}
            value={form.name}
          />
          <div className="grid gap-3 md:grid-cols-2">
            <SelectField
              label="类型"
              onChange={(value) =>
                value && setForm((prev) => ({ ...prev, connectorType: value }))
              }
              options={CONNECTOR_TYPES}
              value={form.connectorType}
            />
            <SelectField
              label="认证"
              onChange={(value) =>
                value && setForm((prev) => ({ ...prev, authMode: value }))
              }
              options={CONNECTOR_AUTH_MODES}
              value={form.authMode}
            />
          </div>
          <div className="grid gap-3 md:grid-cols-2">
            <TextField
              label="Vendor"
              onChange={(value) =>
                setForm((prev) => ({ ...prev, vendor: value }))
              }
              value={form.vendor}
            />
            <TextField
              label="Protocol"
              onChange={(value) =>
                setForm((prev) => ({ ...prev, protocol: value }))
              }
              value={form.protocol}
            />
          </div>
          <div className="grid gap-3 md:grid-cols-2">
            <TextField
              label="连接超时 ms"
              onChange={(value) =>
                setForm((prev) => ({
                  ...prev,
                  connectTimeoutMs: Number(value),
                }))
              }
              type="number"
              value={form.connectTimeoutMs}
            />
            <TextField
              label="读取超时 ms"
              onChange={(value) =>
                setForm((prev) => ({ ...prev, readTimeoutMs: Number(value) }))
              }
              type="number"
              value={form.readTimeoutMs}
            />
          </div>
          <TextareaField
            label="参数 key=value|sensitive"
            onChange={(value) =>
              setForm((prev) => ({ ...prev, parametersText: value }))
            }
            value={form.parametersText}
          />
          <div className="flex flex-wrap gap-2">
            <Button disabled={saveMutation.isPending} type="submit">
              <Save className="h-4 w-4" />
              保存定义
            </Button>
            <Button
              disabled={!selectedId || saveParametersMutation.isPending}
              onClick={() => saveParametersMutation.mutate()}
              variant="outline"
            >
              保存参数
            </Button>
          </div>
        </form>
      </div>
    </section>
  )
}

function SyncPanel({ active }: { active: boolean }): ReactElement {
  const queryClient = useQueryClient()
  const [page, setPage] = useState(1)
  const [status, setStatus] = useState<SyncTaskStatus | ''>('')
  const [mode, setMode] = useState<SyncMode | ''>('')
  const [taskCode, setTaskCode] = useState('')
  const [selectedTaskId, setSelectedTaskId] = useState('')
  const [selectedExecutionId, setSelectedExecutionId] = useState('')
  const [executionStatus, setExecutionStatus] = useState<ExecutionStatus | ''>(
    '',
  )
  const [triggerType, setTriggerType] = useState<ExecutionTriggerType | ''>('')
  const [formError, setFormError] = useState('')
  const [form, setForm] = useState<SyncForm>({
    code: '',
    name: '',
    description: '',
    taskType: 'IMPORT',
    syncMode: 'FULL',
    sourceConnectorId: '',
    targetConnectorId: '',
    checkpointMode: 'TIMESTAMP',
    checkpointField: 'updatedAt',
    idempotencyField: 'id',
    manualTriggerEnabled: true,
    scheduleEnabled: false,
    cron: '',
    mappingRulesText: 'id->id|MERGE|true',
  })

  const tasksQuery = useQuery({
    enabled: active,
    queryKey: ['data-services', 'sync', 'tasks', page, status, mode, taskCode],
    queryFn: () =>
      dataServicesService.listSyncTasks({
        tenantId: getCurrentTenantId(),
        code: taskCode || undefined,
        status: status || undefined,
        syncMode: mode || undefined,
        page,
        size: PAGE_SIZE,
      }),
  })
  const taskItems = useMemo(
    () => tasksQuery.data?.items ?? [],
    [tasksQuery.data?.items],
  )

  useEffect(() => {
    if (!selectedTaskId && taskItems[0]) {
      setSelectedTaskId(taskItems[0].taskId)
    }
  }, [selectedTaskId, taskItems])

  const taskDetailQuery = useQuery({
    enabled: active && Boolean(selectedTaskId),
    queryKey: ['data-services', 'sync', 'tasks', selectedTaskId],
    queryFn: () => dataServicesService.getSyncTask(selectedTaskId),
  })
  const executionsQuery = useQuery({
    enabled: active && Boolean(selectedTaskId),
    queryKey: [
      'data-services',
      'sync',
      'tasks',
      selectedTaskId,
      'executions',
      executionStatus,
      triggerType,
    ],
    queryFn: () =>
      dataServicesService.listTaskExecutions(selectedTaskId, {
        executionStatus: executionStatus || undefined,
        triggerType: triggerType || undefined,
        page: 1,
        size: PAGE_SIZE,
      }),
  })
  const executionDetailQuery = useQuery({
    enabled: active && Boolean(selectedExecutionId),
    queryKey: ['data-services', 'sync', 'executions', selectedExecutionId],
    queryFn: () => dataServicesService.getSyncExecution(selectedExecutionId),
  })

  const invalidate = () =>
    queryClient.invalidateQueries({ queryKey: ['data-services', 'sync'] })

  const buildPayload = (): SaveSyncTaskRequest => ({
    tenantId: getCurrentTenantId(),
    code: form.code.trim() || undefined,
    name: form.name.trim(),
    description: form.description.trim() || undefined,
    taskType: form.taskType,
    syncMode: form.syncMode,
    sourceConnectorId: form.sourceConnectorId.trim(),
    targetConnectorId: form.targetConnectorId.trim(),
    checkpointMode: form.checkpointMode,
    checkpointConfig: {
      checkpointField: form.checkpointField.trim() || undefined,
      idempotencyField: form.idempotencyField.trim() || undefined,
      allowManualReset: true,
    },
    triggerConfig: {
      manualTriggerEnabled: form.manualTriggerEnabled,
      eventPatterns: [],
    },
    retryPolicy: {
      maxRetries: 3,
      manualRetryEnabled: true,
      automaticRetryEnabled: true,
      retryableErrorCodes: [],
    },
    compensationPolicy: {
      manualCompensationEnabled: true,
      allowIgnoreDifference: true,
      requireReason: true,
      maxCompensationAttempts: 3,
    },
    reconciliationPolicy: {
      enabled: true,
      checkExtraTargetRecords: true,
      failWhenDifferenceDetected: false,
      manualReviewThreshold: 10,
    },
    scheduleConfig: {
      enabled: form.scheduleEnabled,
      cron: form.cron.trim() || undefined,
      zoneId: Intl.DateTimeFormat().resolvedOptions().timeZone,
    },
    mappingRules: parseSyncMappings(form.mappingRulesText),
  })

  const saveMutation = useMutation({
    mutationFn: () =>
      selectedTaskId
        ? dataServicesService.updateSyncTask(selectedTaskId, buildPayload())
        : dataServicesService.createSyncTask(buildPayload()),
    onSuccess: (detail) => {
      setSelectedTaskId(detail.summary.taskId)
      invalidate()
    },
  })
  const actionMutation = useMutation({
    mutationFn: async (
      action: 'activate' | 'pause' | 'trigger' | 'reset' | 'delete',
    ) => {
      if (action === 'activate') {
        await dataServicesService.activateSyncTask(selectedTaskId)
        return
      }
      if (action === 'pause') {
        await dataServicesService.pauseSyncTask(selectedTaskId)
        return
      }
      if (action === 'trigger') {
        await dataServicesService.triggerSyncTask(selectedTaskId, {
          source: 'portal-web',
        })
        return
      }
      if (action === 'reset') {
        await dataServicesService.resetSyncCheckpoint(
          selectedTaskId,
          '',
          'portal-web manual reset',
        )
        return
      }

      await dataServicesService.deleteSyncTask(selectedTaskId)
    },
    onSuccess: invalidate,
  })
  const executionActionMutation = useMutation({
    mutationFn: (action: 'retry' | 'reconcile' | CompensationAction) => {
      const request = {
        operatorAccountId: getCurrentOperatorId(),
        operatorPersonId: getCurrentOperatorId(),
        reason: `portal-web ${action}`,
      }

      if (action === 'retry') {
        return dataServicesService.retrySyncExecution(
          selectedExecutionId,
          request,
        )
      }
      if (action === 'reconcile') {
        return dataServicesService.reconcileSyncExecution(
          selectedExecutionId,
          request,
        )
      }

      const difference = executionDetailQuery.data?.differences.find(
        (item) => item.status === 'DETECTED',
      )

      if (!difference) {
        throw new Error('没有待处理差异')
      }

      return dataServicesService.compensateSyncExecution(
        selectedExecutionId,
        difference.differenceCode,
        action,
        `portal-web ${action}`,
      )
    },
    onSuccess: invalidate,
  })

  const loadTaskToForm = (detail?: SyncTaskDetail) => {
    if (!detail) {
      return
    }

    setForm({
      code: detail.summary.code,
      name: detail.summary.name,
      description: detail.summary.description ?? '',
      taskType: detail.summary.taskType,
      syncMode: detail.summary.syncMode,
      sourceConnectorId: detail.summary.sourceConnectorId ?? '',
      targetConnectorId: detail.summary.targetConnectorId ?? '',
      checkpointMode: detail.summary.checkpointMode ?? 'NONE',
      checkpointField: detail.checkpointConfig?.checkpointField ?? '',
      idempotencyField: detail.checkpointConfig?.idempotencyField ?? '',
      manualTriggerEnabled: detail.triggerConfig?.manualTriggerEnabled ?? true,
      scheduleEnabled: detail.scheduleConfig?.enabled ?? false,
      cron: detail.scheduleConfig?.cron ?? '',
      mappingRulesText: syncMappingsToText(detail),
    })
  }

  const handleSave = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    if (
      !form.name.trim() ||
      !form.sourceConnectorId ||
      !form.targetConnectorId
    ) {
      setFormError('name、sourceConnectorId、targetConnectorId 必填')
      return
    }
    if (!selectedTaskId && !form.code.trim()) {
      setFormError('创建同步任务时 code 必填')
      return
    }
    setFormError('')
    saveMutation.mutate()
  }

  return (
    <section className="rounded-lg border border-slate-200 bg-white shadow-sm">
      <PanelTitle
        description={TAB_ITEMS[1].description}
        icon={RefreshCcw}
        title="DataSync 工作台"
      />
      <div className="grid gap-4 p-4 xl:grid-cols-[minmax(0,1fr)_420px]">
        <div className="space-y-4">
          <div className="grid gap-3 md:grid-cols-4">
            <TextField
              label="任务编码"
              onChange={setTaskCode}
              value={taskCode}
            />
            <SelectField
              includeAll
              label="同步模式"
              onChange={setMode}
              options={SYNC_MODES}
              value={mode}
            />
            <SelectField
              includeAll
              label="任务状态"
              onChange={setStatus}
              options={SYNC_STATUSES}
              value={status}
            />
            <Button
              className="self-end"
              onClick={() => tasksQuery.refetch()}
              variant="outline"
            >
              <RefreshCcw className="h-4 w-4" />
              刷新
            </Button>
          </div>
          <StateBlock
            error={tasksQuery.error}
            isEmpty={!taskItems.length}
            isLoading={tasksQuery.isLoading}
            onRetry={() => tasksQuery.refetch()}
          />
          {taskItems.length ? (
            <div className="overflow-auto rounded-lg border border-slate-200">
              <table className="min-w-full divide-y divide-slate-100 text-sm">
                <thead className="bg-slate-50 text-left text-xs uppercase text-slate-500">
                  <tr>
                    <th className="px-3 py-2">任务</th>
                    <th className="px-3 py-2">模式</th>
                    <th className="px-3 py-2">依赖</th>
                    <th className="px-3 py-2">状态</th>
                    <th className="px-3 py-2">最近执行</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {taskItems.map((item: SyncTaskSummary) => (
                    <tr
                      className={cn(
                        'cursor-pointer hover:bg-slate-50',
                        selectedTaskId === item.taskId && 'bg-sky-50',
                      )}
                      key={item.taskId}
                      onClick={() => setSelectedTaskId(item.taskId)}
                    >
                      <td className="px-3 py-2 font-medium text-slate-900">
                        {item.code}
                        <div className="text-xs font-normal text-slate-500">
                          {item.name}
                        </div>
                      </td>
                      <td className="px-3 py-2">{item.syncMode}</td>
                      <td className="px-3 py-2">
                        <StatusBadge status={item.dependencyStatus} />
                      </td>
                      <td className="px-3 py-2">
                        <StatusBadge status={item.status} />
                      </td>
                      <td className="px-3 py-2">
                        <StatusBadge
                          status={item.latestExecution?.executionStatus}
                        />
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
              <PaginationBar
                pagination={getPagination(tasksQuery.data)}
                setPage={setPage}
              />
            </div>
          ) : null}

          {taskDetailQuery.data ? (
            <div className="rounded-lg border border-slate-200 p-4">
              <div className="flex flex-wrap items-center justify-between gap-2">
                <h3 className="font-semibold text-slate-950">任务调度与执行</h3>
                <div className="flex flex-wrap gap-2">
                  <Button
                    onClick={() => loadTaskToForm(taskDetailQuery.data)}
                    size="sm"
                    variant="outline"
                  >
                    <Eye className="h-4 w-4" />
                    载入编辑
                  </Button>
                  <Button
                    disabled={actionMutation.isPending}
                    onClick={() => actionMutation.mutate('trigger')}
                    size="sm"
                  >
                    <Play className="h-4 w-4" />
                    执行
                  </Button>
                  <Button
                    disabled={actionMutation.isPending}
                    onClick={() => actionMutation.mutate('activate')}
                    size="sm"
                    variant="outline"
                  >
                    <Power className="h-4 w-4" />
                    启用
                  </Button>
                  <Button
                    disabled={actionMutation.isPending}
                    onClick={() => actionMutation.mutate('pause')}
                    size="sm"
                    variant="outline"
                  >
                    <Pause className="h-4 w-4" />
                    暂停
                  </Button>
                  <Button
                    disabled={actionMutation.isPending}
                    onClick={() => actionMutation.mutate('reset')}
                    size="sm"
                    variant="outline"
                  >
                    <RotateCcw className="h-4 w-4" />
                    重置位点
                  </Button>
                </div>
              </div>
              <div className="mt-3 grid gap-3 md:grid-cols-2">
                <SelectField
                  includeAll
                  label="执行状态"
                  onChange={setExecutionStatus}
                  options={EXECUTION_STATUSES}
                  value={executionStatus}
                />
                <SelectField
                  includeAll
                  label="触发类型"
                  onChange={setTriggerType}
                  options={EXECUTION_TRIGGERS}
                  value={triggerType}
                />
              </div>
              <div className="mt-3 overflow-auto rounded-lg border border-slate-200">
                <table className="min-w-full divide-y divide-slate-100 text-sm">
                  <thead className="bg-slate-50 text-left text-xs uppercase text-slate-500">
                    <tr>
                      <th className="px-3 py-2">批次</th>
                      <th className="px-3 py-2">状态</th>
                      <th className="px-3 py-2">失败</th>
                      <th className="px-3 py-2">差异</th>
                      <th className="px-3 py-2">时间</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-100">
                    {(executionsQuery.data?.items ?? []).map(
                      (item: SyncExecutionSummary) => (
                        <tr
                          className={cn(
                            'cursor-pointer hover:bg-slate-50',
                            selectedExecutionId === item.executionId &&
                              'bg-sky-50',
                          )}
                          key={item.executionId}
                          onClick={() =>
                            setSelectedExecutionId(item.executionId)
                          }
                        >
                          <td className="px-3 py-2">
                            {item.executionBatchNo ?? item.executionId}
                          </td>
                          <td className="px-3 py-2">
                            <StatusBadge status={item.executionStatus} />
                          </td>
                          <td className="px-3 py-2 text-rose-600">
                            {item.failureMessage ?? '-'}
                          </td>
                          <td className="px-3 py-2">
                            {item.diffSummary?.differenceCount ?? 0}
                          </td>
                          <td className="px-3 py-2 text-slate-500">
                            {formatTime(item.startedAt)}
                          </td>
                        </tr>
                      ),
                    )}
                  </tbody>
                </table>
              </div>
            </div>
          ) : null}

          {executionDetailQuery.data ? (
            <ExecutionDetail
              detail={executionDetailQuery.data}
              isPending={executionActionMutation.isPending}
              onAction={(action) => executionActionMutation.mutate(action)}
            />
          ) : null}
        </div>

        <form
          className="space-y-3 rounded-lg border border-slate-200 p-4"
          onSubmit={handleSave}
        >
          <h3 className="font-semibold text-slate-950">创建 / 编辑同步任务</h3>
          {formError ? (
            <p className="text-sm text-rose-600">{formError}</p>
          ) : null}
          <TextField
            label="Code"
            onChange={(value) => setForm((prev) => ({ ...prev, code: value }))}
            value={form.code}
          />
          <TextField
            label="名称"
            onChange={(value) => setForm((prev) => ({ ...prev, name: value }))}
            value={form.name}
          />
          <div className="grid gap-3 md:grid-cols-2">
            <SelectField
              label="任务类型"
              onChange={(value) =>
                value && setForm((prev) => ({ ...prev, taskType: value }))
              }
              options={SYNC_TASK_TYPES}
              value={form.taskType}
            />
            <SelectField
              label="同步模式"
              onChange={(value) =>
                value && setForm((prev) => ({ ...prev, syncMode: value }))
              }
              options={SYNC_MODES}
              value={form.syncMode}
            />
          </div>
          <TextField
            label="Source Connector ID"
            onChange={(value) =>
              setForm((prev) => ({ ...prev, sourceConnectorId: value }))
            }
            value={form.sourceConnectorId}
          />
          <TextField
            label="Target Connector ID"
            onChange={(value) =>
              setForm((prev) => ({ ...prev, targetConnectorId: value }))
            }
            value={form.targetConnectorId}
          />
          <SelectField
            label="Checkpoint"
            onChange={(value) =>
              value && setForm((prev) => ({ ...prev, checkpointMode: value }))
            }
            options={CHECKPOINT_MODES}
            value={form.checkpointMode}
          />
          <div className="grid gap-3 md:grid-cols-2">
            <TextField
              label="Checkpoint Field"
              onChange={(value) =>
                setForm((prev) => ({ ...prev, checkpointField: value }))
              }
              value={form.checkpointField}
            />
            <TextField
              label="Idempotency Field"
              onChange={(value) =>
                setForm((prev) => ({ ...prev, idempotencyField: value }))
              }
              value={form.idempotencyField}
            />
          </div>
          <div className="grid gap-3 md:grid-cols-2">
            <CheckboxField
              checked={form.manualTriggerEnabled}
              label="手动触发"
              onChange={(value) =>
                setForm((prev) => ({ ...prev, manualTriggerEnabled: value }))
              }
            />
            <CheckboxField
              checked={form.scheduleEnabled}
              label="调度启用"
              onChange={(value) =>
                setForm((prev) => ({ ...prev, scheduleEnabled: value }))
              }
            />
          </div>
          <TextField
            label="Cron"
            onChange={(value) => setForm((prev) => ({ ...prev, cron: value }))}
            value={form.cron}
          />
          <TextareaField
            label="字段映射 source->target|strategy|key"
            onChange={(value) =>
              setForm((prev) => ({ ...prev, mappingRulesText: value }))
            }
            rows={5}
            value={form.mappingRulesText}
          />
          <Button disabled={saveMutation.isPending} type="submit">
            <Save className="h-4 w-4" />
            保存同步任务
          </Button>
        </form>
      </div>
    </section>
  )
}

function ExecutionDetail({
  detail,
  isPending,
  onAction,
}: {
  detail: SyncExecutionDetail
  isPending: boolean
  onAction: (action: 'retry' | 'reconcile' | CompensationAction) => void
}): ReactElement {
  return (
    <div className="rounded-lg border border-slate-200 p-4">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <div>
          <h3 className="font-semibold text-slate-950">失败详情 / 差异处理</h3>
          <p className="text-sm text-slate-500">
            {detail.summary.failureMessage ?? '当前执行未返回失败信息'}
          </p>
        </div>
        <div className="flex flex-wrap gap-2">
          <Button
            disabled={isPending || !detail.summary.retryable}
            onClick={() => onAction('retry')}
            size="sm"
            variant="outline"
          >
            重试
          </Button>
          <Button
            disabled={isPending}
            onClick={() => onAction('reconcile')}
            size="sm"
            variant="outline"
          >
            对账
          </Button>
          <Button
            disabled={isPending || !detail.differences.length}
            onClick={() => onAction('RETRY_WRITE')}
            size="sm"
            variant="outline"
          >
            补偿写入
          </Button>
          <Button
            disabled={isPending || !detail.differences.length}
            onClick={() => onAction('IGNORE_DIFFERENCE')}
            size="sm"
            variant="outline"
          >
            忽略差异
          </Button>
        </div>
      </div>
      {detail.differences.length ? (
        <div className="mt-3 overflow-auto rounded-lg border border-slate-200">
          <table className="min-w-full divide-y divide-slate-100 text-sm">
            <thead className="bg-slate-50 text-left text-xs uppercase text-slate-500">
              <tr>
                <th className="px-3 py-2">差异</th>
                <th className="px-3 py-2">记录</th>
                <th className="px-3 py-2">字段</th>
                <th className="px-3 py-2">状态</th>
                <th className="px-3 py-2">说明</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {detail.differences.map((item) => (
                <tr key={item.differenceCode}>
                  <td className="px-3 py-2">{item.differenceType}</td>
                  <td className="px-3 py-2">{item.recordKey}</td>
                  <td className="px-3 py-2">{item.fieldName ?? '-'}</td>
                  <td className="px-3 py-2">
                    <StatusBadge status={item.status} />
                  </td>
                  <td className="px-3 py-2 text-slate-600">
                    {item.message ?? '-'}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      ) : null}
    </div>
  )
}

function DataServicePanel({ active }: { active: boolean }): ReactElement {
  const queryClient = useQueryClient()
  const [page, setPage] = useState(1)
  const [keyword, setKeyword] = useState('')
  const [type, setType] = useState<DataServiceType | ''>('')
  const [status, setStatus] = useState<DataServiceStatus | ''>('')
  const [selectedId, setSelectedId] = useState('')
  const [formError, setFormError] = useState('')
  const [previewJson, setPreviewJson] = useState('{}')
  const [previewResult, setPreviewResult] = useState<unknown>(null)
  const [form, setForm] = useState<ServiceForm>({
    serviceId: createClientId('service'),
    code: '',
    name: '',
    serviceType: 'QUERY',
    sourceMode: 'INTERNAL_QUERY',
    permissionMode: 'PUBLIC_INTERNAL',
    sourceRef: 'internal.query',
    connectorId: '',
    description: '',
    allowedAppCodes: '',
    allowedSubjectIds: '',
    requiredRoles: '',
    cacheEnabled: false,
    cacheTtlSeconds: 300,
    parametersText: 'keyword:STRING:false',
    fieldMappingsText: 'id->id',
  })

  const servicesQuery = useQuery({
    enabled: active,
    queryKey: ['data-services', 'services', page, keyword, type, status],
    queryFn: () =>
      dataServicesService.listDataServices({
        keyword,
        serviceType: type || undefined,
        status: status || undefined,
        page,
        size: PAGE_SIZE,
      }),
  })
  const serviceItems = useMemo(
    () => servicesQuery.data?.items ?? [],
    [servicesQuery.data?.items],
  )

  useEffect(() => {
    if (!selectedId && serviceItems[0]) {
      setSelectedId(serviceItems[0].serviceId)
    }
  }, [selectedId, serviceItems])

  const detailQuery = useQuery({
    enabled: active && Boolean(selectedId),
    queryKey: ['data-services', 'services', selectedId],
    queryFn: () => dataServicesService.getDataService(selectedId),
  })
  const paramsQuery = useQuery({
    enabled: active && Boolean(selectedId),
    queryKey: ['data-services', 'services', selectedId, 'parameters'],
    queryFn: () => dataServicesService.listDataServiceParameters(selectedId),
  })
  const mappingsQuery = useQuery({
    enabled: active && Boolean(selectedId),
    queryKey: ['data-services', 'services', selectedId, 'mappings'],
    queryFn: () => dataServicesService.listDataServiceFieldMappings(selectedId),
  })

  const invalidate = () =>
    queryClient.invalidateQueries({ queryKey: ['data-services', 'services'] })

  const buildPayload = (): SaveDataServiceRequest => ({
    serviceId: form.serviceId,
    code: form.code.trim(),
    name: form.name.trim(),
    serviceType: form.serviceType,
    sourceMode: form.sourceMode,
    permissionMode: form.permissionMode,
    sourceRef: form.sourceRef.trim(),
    connectorId: form.connectorId.trim() || undefined,
    description: form.description.trim() || undefined,
    permissionBoundary: {
      allowedAppCodes: splitList(form.allowedAppCodes),
      allowedSubjectIds: splitList(form.allowedSubjectIds),
      requiredRoles: splitList(form.requiredRoles),
    },
    cachePolicy: {
      enabled: form.cacheEnabled,
      ttlSeconds: Number(form.cacheTtlSeconds),
      scope: 'TENANT',
      cacheNullValue: false,
      invalidationEvents: [],
    },
    parameters: parseServiceParameters(form.parametersText),
    fieldMappings: parseFieldMappings(form.fieldMappingsText),
  })

  const saveMutation = useMutation({
    mutationFn: () =>
      selectedId
        ? dataServicesService.updateDataService(selectedId, buildPayload())
        : dataServicesService.createDataService(buildPayload()),
    onSuccess: (detail) => {
      setSelectedId(detail.serviceId)
      invalidate()
    },
  })
  const actionMutation = useMutation({
    mutationFn: async (action: 'activate' | 'disable' | 'delete') => {
      if (action === 'activate') {
        await dataServicesService.activateDataService(selectedId)
        return
      }
      if (action === 'disable') {
        await dataServicesService.disableDataService(selectedId)
        return
      }
      await dataServicesService.deleteDataService(selectedId)
    },
    onSuccess: invalidate,
  })
  const previewMutation = useMutation({
    mutationFn: () => {
      const parameters = parseJsonObject(previewJson)
      const payload: DataServiceInvocationRequest = {
        appCode: splitList(form.allowedAppCodes)[0],
        subjectId: splitList(form.allowedSubjectIds)[0],
        parameters,
      }

      return dataServicesService.previewDataServiceInvocation(
        detailQuery.data?.code ?? form.code,
        payload,
      )
    },
    onSuccess: setPreviewResult,
  })
  const upsertParameterMutation = useMutation({
    mutationFn: () => {
      const [first] = parseServiceParameters(form.parametersText)
      if (!first) {
        throw new Error('没有可保存的参数')
      }
      return dataServicesService.upsertDataServiceParameter(
        selectedId,
        first.paramCode,
        first,
      )
    },
    onSuccess: invalidate,
  })
  const upsertMappingMutation = useMutation({
    mutationFn: () => {
      const [first] = parseFieldMappings(form.fieldMappingsText)
      if (!first) {
        throw new Error('没有可保存的字段映射')
      }
      return dataServicesService.upsertDataServiceFieldMapping(
        selectedId,
        first.mappingId ?? createClientId('mapping'),
        first,
      )
    },
    onSuccess: invalidate,
  })

  const loadDetailToForm = (detail?: DataServiceDetail) => {
    if (!detail) {
      return
    }

    setForm({
      serviceId: detail.serviceId,
      code: detail.code,
      name: detail.name,
      serviceType: detail.serviceType,
      sourceMode: detail.sourceMode,
      permissionMode: detail.permissionMode,
      sourceRef: detail.sourceRef,
      connectorId: detail.connectorId ?? '',
      description: detail.description ?? '',
      allowedAppCodes: detail.permissionBoundary.allowedAppCodes.join(','),
      allowedSubjectIds: detail.permissionBoundary.allowedSubjectIds.join(','),
      requiredRoles: detail.permissionBoundary.requiredRoles.join(','),
      cacheEnabled: detail.cachePolicy.enabled,
      cacheTtlSeconds: detail.cachePolicy.ttlSeconds ?? 300,
      parametersText: serviceParametersToText(detail.parameters),
      fieldMappingsText: fieldMappingsToText(detail.fieldMappings),
    })
  }

  const handleSave = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    if (!form.code.trim() || !form.name.trim() || !form.sourceRef.trim()) {
      setFormError('code、name、sourceRef 必填')
      return
    }
    if (form.sourceMode !== 'INTERNAL_QUERY' && !form.connectorId.trim()) {
      setFormError('CONNECTOR/MIXED 服务必须填写 connectorId')
      return
    }
    setFormError('')
    saveMutation.mutate()
  }

  return (
    <section className="rounded-lg border border-slate-200 bg-white shadow-sm">
      <PanelTitle
        description={TAB_ITEMS[2].description}
        icon={Server}
        title="DataService 工作台"
      />
      <div className="grid gap-4 p-4 xl:grid-cols-[minmax(0,1fr)_420px]">
        <div className="space-y-4">
          <div className="grid gap-3 md:grid-cols-4">
            <TextField label="关键字" onChange={setKeyword} value={keyword} />
            <SelectField
              includeAll
              label="类型"
              onChange={setType}
              options={SERVICE_TYPES}
              value={type}
            />
            <SelectField
              includeAll
              label="状态"
              onChange={setStatus}
              options={SERVICE_STATUSES}
              value={status}
            />
            <Button
              className="self-end"
              onClick={() => servicesQuery.refetch()}
              variant="outline"
            >
              <RefreshCcw className="h-4 w-4" />
              刷新
            </Button>
          </div>
          <StateBlock
            error={servicesQuery.error}
            isEmpty={!serviceItems.length}
            isLoading={servicesQuery.isLoading}
            onRetry={() => servicesQuery.refetch()}
          />
          {serviceItems.length ? (
            <div className="overflow-auto rounded-lg border border-slate-200">
              <table className="min-w-full divide-y divide-slate-100 text-sm">
                <thead className="bg-slate-50 text-left text-xs uppercase text-slate-500">
                  <tr>
                    <th className="px-3 py-2">服务</th>
                    <th className="px-3 py-2">类型</th>
                    <th className="px-3 py-2">权限</th>
                    <th className="px-3 py-2">状态</th>
                    <th className="px-3 py-2">引用</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {serviceItems.map((item: DataServiceSummary) => (
                    <tr
                      className={cn(
                        'cursor-pointer hover:bg-slate-50',
                        selectedId === item.serviceId && 'bg-sky-50',
                      )}
                      key={item.serviceId}
                      onClick={() => setSelectedId(item.serviceId)}
                    >
                      <td className="px-3 py-2 font-medium text-slate-900">
                        {item.code}
                        <div className="text-xs font-normal text-slate-500">
                          {item.name}
                        </div>
                      </td>
                      <td className="px-3 py-2">{item.serviceType}</td>
                      <td className="px-3 py-2">{item.permissionMode}</td>
                      <td className="px-3 py-2">
                        <StatusBadge status={item.status} />
                      </td>
                      <td className="px-3 py-2 text-slate-500">
                        API {item.openApiReferenceCount} / Report{' '}
                        {item.reportReferenceCount} / Sync{' '}
                        {item.syncReferenceCount}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
              <PaginationBar
                pagination={getPagination(servicesQuery.data)}
                setPage={setPage}
              />
            </div>
          ) : null}

          {detailQuery.data ? (
            <div className="rounded-lg border border-slate-200 p-4">
              <div className="flex flex-wrap items-center justify-between gap-2">
                <h3 className="font-semibold text-slate-950">
                  服务详情与调用预览
                </h3>
                <div className="flex flex-wrap gap-2">
                  <Button
                    onClick={() => loadDetailToForm(detailQuery.data)}
                    size="sm"
                    variant="outline"
                  >
                    <Eye className="h-4 w-4" />
                    载入编辑
                  </Button>
                  <Button
                    disabled={actionMutation.isPending}
                    onClick={() => actionMutation.mutate('activate')}
                    size="sm"
                  >
                    <Power className="h-4 w-4" />
                    启用
                  </Button>
                  <Button
                    disabled={actionMutation.isPending}
                    onClick={() => actionMutation.mutate('disable')}
                    size="sm"
                    variant="outline"
                  >
                    <Pause className="h-4 w-4" />
                    停用
                  </Button>
                  <Button
                    disabled={actionMutation.isPending}
                    onClick={() => actionMutation.mutate('delete')}
                    size="sm"
                    variant="outline"
                  >
                    <Trash2 className="h-4 w-4" />
                    删除
                  </Button>
                </div>
              </div>
              <div className="mt-3 grid gap-3 md:grid-cols-3">
                <div className="rounded-lg bg-slate-50 p-3 text-sm">
                  参数{' '}
                  {paramsQuery.data?.length ??
                    detailQuery.data.parameters.length}
                </div>
                <div className="rounded-lg bg-slate-50 p-3 text-sm">
                  字段映射{' '}
                  {mappingsQuery.data?.length ??
                    detailQuery.data.fieldMappings.length}
                </div>
                <div className="rounded-lg bg-slate-50 p-3 text-sm">
                  缓存 {detailQuery.data.cacheEnabled ? 'ON' : 'OFF'}
                </div>
              </div>
              <div className="mt-4 grid gap-3 lg:grid-cols-2">
                <TextareaField
                  label="调用参数 JSON"
                  onChange={setPreviewJson}
                  value={previewJson}
                />
                <div className="space-y-2">
                  <Button
                    disabled={previewMutation.isPending}
                    onClick={() => previewMutation.mutate()}
                  >
                    <Play className="h-4 w-4" />
                    调用预览
                  </Button>
                  {previewResult ? <JsonBlock value={previewResult} /> : null}
                </div>
              </div>
            </div>
          ) : null}
        </div>

        <form
          className="space-y-3 rounded-lg border border-slate-200 p-4"
          onSubmit={handleSave}
        >
          <h3 className="font-semibold text-slate-950">创建 / 编辑数据服务</h3>
          {formError ? (
            <p className="text-sm text-rose-600">{formError}</p>
          ) : null}
          <TextField
            label="Service ID"
            onChange={(value) =>
              setForm((prev) => ({ ...prev, serviceId: value }))
            }
            value={form.serviceId}
          />
          <TextField
            label="Code"
            onChange={(value) => setForm((prev) => ({ ...prev, code: value }))}
            value={form.code}
          />
          <TextField
            label="名称"
            onChange={(value) => setForm((prev) => ({ ...prev, name: value }))}
            value={form.name}
          />
          <div className="grid gap-3 md:grid-cols-2">
            <SelectField
              label="类型"
              onChange={(value) =>
                value && setForm((prev) => ({ ...prev, serviceType: value }))
              }
              options={SERVICE_TYPES}
              value={form.serviceType}
            />
            <SelectField
              label="来源"
              onChange={(value) =>
                value && setForm((prev) => ({ ...prev, sourceMode: value }))
              }
              options={SERVICE_SOURCE_MODES}
              value={form.sourceMode}
            />
          </div>
          <SelectField
            label="权限模式"
            onChange={(value) =>
              value && setForm((prev) => ({ ...prev, permissionMode: value }))
            }
            options={SERVICE_PERMISSION_MODES}
            value={form.permissionMode}
          />
          <TextField
            label="Source Ref"
            onChange={(value) =>
              setForm((prev) => ({ ...prev, sourceRef: value }))
            }
            value={form.sourceRef}
          />
          <TextField
            label="Connector ID"
            onChange={(value) =>
              setForm((prev) => ({ ...prev, connectorId: value }))
            }
            value={form.connectorId}
          />
          <TextareaField
            label="Allowed Apps"
            onChange={(value) =>
              setForm((prev) => ({ ...prev, allowedAppCodes: value }))
            }
            rows={2}
            value={form.allowedAppCodes}
          />
          <TextareaField
            label="Allowed Subjects"
            onChange={(value) =>
              setForm((prev) => ({ ...prev, allowedSubjectIds: value }))
            }
            rows={2}
            value={form.allowedSubjectIds}
          />
          <TextareaField
            label="Required Roles"
            onChange={(value) =>
              setForm((prev) => ({ ...prev, requiredRoles: value }))
            }
            rows={2}
            value={form.requiredRoles}
          />
          <div className="grid gap-3 md:grid-cols-2">
            <CheckboxField
              checked={form.cacheEnabled}
              label="启用缓存"
              onChange={(value) =>
                setForm((prev) => ({ ...prev, cacheEnabled: value }))
              }
            />
            <TextField
              label="TTL 秒"
              onChange={(value) =>
                setForm((prev) => ({ ...prev, cacheTtlSeconds: Number(value) }))
              }
              type="number"
              value={form.cacheTtlSeconds}
            />
          </div>
          <TextareaField
            label="参数 code:type:required"
            onChange={(value) =>
              setForm((prev) => ({ ...prev, parametersText: value }))
            }
            value={form.parametersText}
          />
          <TextareaField
            label="字段映射 source->target"
            onChange={(value) =>
              setForm((prev) => ({ ...prev, fieldMappingsText: value }))
            }
            value={form.fieldMappingsText}
          />
          <div className="flex flex-wrap gap-2">
            <Button disabled={saveMutation.isPending} type="submit">
              <Save className="h-4 w-4" />
              保存服务
            </Button>
            <Button
              disabled={!selectedId || upsertParameterMutation.isPending}
              onClick={() => upsertParameterMutation.mutate()}
              variant="outline"
            >
              保存首个参数
            </Button>
            <Button
              disabled={!selectedId || upsertMappingMutation.isPending}
              onClick={() => upsertMappingMutation.mutate()}
              variant="outline"
            >
              保存首个映射
            </Button>
          </div>
        </form>
      </div>
    </section>
  )
}

function OpenApiPanel({ active }: { active: boolean }): ReactElement {
  const queryClient = useQueryClient()
  const [page, setPage] = useState(1)
  const [path, setPath] = useState('')
  const [method, setMethod] = useState<OpenApiHttpMethod | ''>('')
  const [status, setStatus] = useState<OpenApiStatus | ''>('')
  const [selected, setSelected] = useState<{
    code: string
    version: string
  } | null>(null)
  const [formError, setFormError] = useState('')
  const [form, setForm] = useState<OpenApiForm>({
    code: '',
    version: 'v1',
    name: '',
    dataServiceCode: '',
    path: '/api/v1/open/',
    httpMethod: 'GET',
    authType: 'APP_KEY',
    compatibilityNotes: '',
    clientCode: '',
    secretRef: '',
    scopes: '',
    expiresAt: '',
    policyCode: '',
    policyClientCode: '',
    policyType: 'RATE_LIMIT',
    windowValue: 1,
    windowUnit: 'MINUTE',
    threshold: 100,
  })

  const endpointsQuery = useQuery({
    enabled: active,
    queryKey: [
      'data-services',
      'open-api',
      'endpoints',
      page,
      path,
      method,
      status,
    ],
    queryFn: () =>
      dataServicesService.listOpenApiEndpoints({
        path: path || undefined,
        httpMethod: method || undefined,
        status: status || undefined,
        page,
        size: PAGE_SIZE,
      }),
  })
  const endpointItems = useMemo(
    () => endpointsQuery.data?.items ?? [],
    [endpointsQuery.data?.items],
  )

  useEffect(() => {
    if (!selected && endpointItems[0]) {
      setSelected({
        code: endpointItems[0].code,
        version: endpointItems[0].version,
      })
    }
  }, [endpointItems, selected])

  const detailQuery = useQuery({
    enabled: active && Boolean(selected),
    queryKey: [
      'data-services',
      'open-api',
      'endpoint',
      selected?.code,
      selected?.version,
    ],
    queryFn: () =>
      dataServicesService.getOpenApiEndpoint(
        selected?.code ?? '',
        selected?.version ?? '',
      ),
  })
  const credentialsQuery = useQuery({
    enabled: active,
    queryKey: ['data-services', 'open-api', 'credentials', selected?.code],
    queryFn: () =>
      dataServicesService.listOpenApiCredentials({
        clientCode: form.clientCode || undefined,
        page: 1,
        size: PAGE_SIZE,
      }),
  })
  const policiesQuery = useQuery({
    enabled: active,
    queryKey: ['data-services', 'open-api', 'policies', selected?.code],
    queryFn: () =>
      dataServicesService.listOpenApiPolicies({
        endpointCode: selected?.code,
        page: 1,
        size: PAGE_SIZE,
      }),
  })
  const auditQuery = useQuery({
    enabled: active,
    queryKey: ['data-services', 'open-api', 'audits', selected?.code],
    queryFn: () =>
      dataServicesService.listOpenApiAuditLogs({
        endpointCode: selected?.code,
        page: 1,
        size: PAGE_SIZE,
      }),
  })

  const invalidate = () =>
    queryClient.invalidateQueries({ queryKey: ['data-services', 'open-api'] })

  const saveMutation = useMutation({
    mutationFn: () => {
      const payload: UpsertOpenApiEndpointRequest = {
        name: form.name.trim(),
        dataServiceCode: form.dataServiceCode.trim(),
        path: form.path.trim(),
        httpMethod: form.httpMethod,
        authType: form.authType,
        compatibilityNotes: form.compatibilityNotes.trim() || undefined,
      }

      return dataServicesService.upsertOpenApiEndpoint(
        form.code.trim(),
        form.version.trim(),
        payload,
      )
    },
    onSuccess: (detail) => {
      setSelected({ code: detail.code, version: detail.version })
      invalidate()
    },
  })
  const endpointActionMutation = useMutation({
    mutationFn: async (
      action: 'publish' | 'deprecate' | 'offline' | 'delete',
    ) => {
      const code = selected?.code ?? ''
      const version = selected?.version ?? ''
      if (action === 'publish') {
        await dataServicesService.publishOpenApiEndpoint(code, version)
        return
      }
      if (action === 'deprecate') {
        await dataServicesService.deprecateOpenApiEndpoint(code, version)
        return
      }
      if (action === 'offline') {
        await dataServicesService.offlineOpenApiEndpoint(code, version)
        return
      }
      await dataServicesService.deleteOpenApiEndpoint(code, version)
    },
    onSuccess: invalidate,
  })
  const credentialMutation = useMutation({
    mutationFn: (action: 'save' | 'revoke') => {
      const code = selected?.code ?? form.code
      const version = selected?.version ?? form.version
      if (action === 'revoke') {
        return dataServicesService.revokeOpenApiCredential(
          code,
          version,
          form.clientCode.trim(),
        )
      }
      return dataServicesService.upsertOpenApiCredential(
        code,
        version,
        form.clientCode.trim(),
        {
          secretRef: form.secretRef.trim(),
          scopes: splitList(form.scopes),
          expiresAt: form.expiresAt || undefined,
        },
      )
    },
    onSuccess: invalidate,
  })
  const policyMutation = useMutation({
    mutationFn: (action: 'save' | 'disable') => {
      const code = selected?.code ?? form.code
      const version = selected?.version ?? form.version
      if (action === 'disable') {
        return dataServicesService.disableOpenApiPolicy(
          code,
          version,
          form.policyCode.trim(),
          form.policyClientCode.trim() || undefined,
        )
      }
      return dataServicesService.upsertOpenApiPolicy(
        code,
        version,
        form.policyCode.trim(),
        {
          clientCode: form.policyClientCode.trim() || undefined,
          policyType: form.policyType,
          windowValue: Number(form.windowValue),
          windowUnit: form.windowUnit,
          threshold: Number(form.threshold),
        },
      )
    },
    onSuccess: invalidate,
  })
  const auditReviewMutation = useMutation({
    mutationFn: (logId: string) =>
      dataServicesService.reviewOpenApiAuditLog(
        logId,
        true,
        'portal-web reviewed',
        'manual review',
      ),
    onSuccess: invalidate,
  })

  const loadDetailToForm = (detail?: OpenApiEndpointDetail) => {
    if (!detail) {
      return
    }

    setForm((prev) => ({
      ...prev,
      code: detail.code,
      version: detail.version,
      name: detail.name,
      dataServiceCode: detail.dataServiceCode,
      path: detail.path,
      httpMethod: detail.httpMethod,
      authType: detail.authType,
      compatibilityNotes: detail.compatibilityNotes ?? '',
    }))
  }

  const handleSave = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    if (!form.code.trim() || !form.version.trim() || !form.name.trim()) {
      setFormError('code、version、name 必填')
      return
    }
    if (!form.path.startsWith('/')) {
      setFormError('path 必须以 / 开头')
      return
    }
    setFormError('')
    saveMutation.mutate()
  }

  return (
    <section className="rounded-lg border border-slate-200 bg-white shadow-sm">
      <PanelTitle
        description={TAB_ITEMS[3].description}
        icon={Webhook}
        title="OpenApi 工作台"
      />
      <div className="grid gap-4 p-4 xl:grid-cols-[minmax(0,1fr)_420px]">
        <div className="space-y-4">
          <div className="grid gap-3 md:grid-cols-4">
            <TextField label="路径" onChange={setPath} value={path} />
            <SelectField
              includeAll
              label="方法"
              onChange={setMethod}
              options={OPEN_API_METHODS}
              value={method}
            />
            <SelectField
              includeAll
              label="状态"
              onChange={setStatus}
              options={OPEN_API_STATUSES}
              value={status}
            />
            <Button
              className="self-end"
              onClick={() => endpointsQuery.refetch()}
              variant="outline"
            >
              <RefreshCcw className="h-4 w-4" />
              刷新
            </Button>
          </div>
          <StateBlock
            error={endpointsQuery.error}
            isEmpty={!endpointItems.length}
            isLoading={endpointsQuery.isLoading}
            onRetry={() => endpointsQuery.refetch()}
          />
          {endpointItems.length ? (
            <div className="overflow-auto rounded-lg border border-slate-200">
              <table className="min-w-full divide-y divide-slate-100 text-sm">
                <thead className="bg-slate-50 text-left text-xs uppercase text-slate-500">
                  <tr>
                    <th className="px-3 py-2">接口</th>
                    <th className="px-3 py-2">方法</th>
                    <th className="px-3 py-2">服务</th>
                    <th className="px-3 py-2">状态</th>
                    <th className="px-3 py-2">调用</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {endpointItems.map((item: OpenApiEndpointSummary) => (
                    <tr
                      className={cn(
                        'cursor-pointer hover:bg-slate-50',
                        selected?.code === item.code &&
                          selected.version === item.version &&
                          'bg-sky-50',
                      )}
                      key={`${item.code}:${item.version}`}
                      onClick={() =>
                        setSelected({ code: item.code, version: item.version })
                      }
                    >
                      <td className="px-3 py-2 font-medium text-slate-900">
                        {item.code} {item.version}
                        <div className="text-xs font-normal text-slate-500">
                          {item.path}
                        </div>
                      </td>
                      <td className="px-3 py-2">{item.httpMethod}</td>
                      <td className="px-3 py-2">{item.dataServiceCode}</td>
                      <td className="px-3 py-2">
                        <StatusBadge status={item.status} />
                      </td>
                      <td className="px-3 py-2 text-slate-500">
                        {item.invocationSummary?.totalCount ?? 0}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
              <PaginationBar
                pagination={getPagination(endpointsQuery.data)}
                setPage={setPage}
              />
            </div>
          ) : null}

          {detailQuery.data ? (
            <div className="rounded-lg border border-slate-200 p-4">
              <div className="flex flex-wrap items-center justify-between gap-2">
                <h3 className="font-semibold text-slate-950">接口授权与限流</h3>
                <div className="flex flex-wrap gap-2">
                  <Button
                    onClick={() => loadDetailToForm(detailQuery.data)}
                    size="sm"
                    variant="outline"
                  >
                    <Eye className="h-4 w-4" />
                    载入编辑
                  </Button>
                  <Button
                    disabled={endpointActionMutation.isPending}
                    onClick={() => endpointActionMutation.mutate('publish')}
                    size="sm"
                  >
                    发布
                  </Button>
                  <Button
                    disabled={endpointActionMutation.isPending}
                    onClick={() => endpointActionMutation.mutate('deprecate')}
                    size="sm"
                    variant="outline"
                  >
                    废弃
                  </Button>
                  <Button
                    disabled={endpointActionMutation.isPending}
                    onClick={() => endpointActionMutation.mutate('offline')}
                    size="sm"
                    variant="outline"
                  >
                    下线
                  </Button>
                </div>
              </div>
              <div className="mt-3 grid gap-4 lg:grid-cols-2">
                <ListCard
                  items={
                    credentialsQuery.data?.items ??
                    detailQuery.data.credentialGrants
                  }
                  renderItem={(item: ApiCredentialGrant) => (
                    <div>
                      <div className="font-medium">{item.clientCode}</div>
                      <div className="text-xs text-slate-500">
                        scopes {item.scopes.join(',') || '-'} / {item.status}
                      </div>
                    </div>
                  )}
                  title="授权凭证"
                />
                <ListCard
                  items={
                    policiesQuery.data?.items ??
                    detailQuery.data.rateLimitPolicies
                  }
                  renderItem={(item: ApiRateLimitPolicy) => (
                    <div>
                      <div className="font-medium">{item.policyCode}</div>
                      <div className="text-xs text-slate-500">
                        {item.policyType} {item.threshold}/{item.windowValue}{' '}
                        {item.windowUnit}
                      </div>
                    </div>
                  )}
                  title="限流信息"
                />
              </div>
              <div className="mt-4">
                <h4 className="font-semibold text-slate-950">调用日志</h4>
                <div className="mt-2 space-y-2">
                  {(auditQuery.data?.items ?? []).map(
                    (item: ApiInvocationAuditLog) => (
                      <div
                        className="flex flex-wrap items-center justify-between gap-2 rounded-lg border border-slate-100 p-3 text-sm"
                        key={item.logId}
                      >
                        <div>
                          <span className="font-medium">{item.requestId}</span>
                          <span className="ml-2 text-slate-500">
                            {item.responseStatus} / {item.durationMs}ms /{' '}
                            {formatTime(item.occurredAt)}
                          </span>
                        </div>
                        <div className="flex items-center gap-2">
                          <StatusBadge status={item.outcome} />
                          <Button
                            onClick={() =>
                              auditReviewMutation.mutate(item.logId)
                            }
                            size="sm"
                            variant="outline"
                          >
                            复核
                          </Button>
                        </div>
                      </div>
                    ),
                  )}
                </div>
              </div>
            </div>
          ) : null}
        </div>

        <form
          className="space-y-3 rounded-lg border border-slate-200 p-4"
          onSubmit={handleSave}
        >
          <h3 className="font-semibold text-slate-950">接口定义</h3>
          {formError ? (
            <p className="text-sm text-rose-600">{formError}</p>
          ) : null}
          <div className="grid gap-3 md:grid-cols-2">
            <TextField
              label="Code"
              onChange={(value) =>
                setForm((prev) => ({ ...prev, code: value }))
              }
              value={form.code}
            />
            <TextField
              label="Version"
              onChange={(value) =>
                setForm((prev) => ({ ...prev, version: value }))
              }
              value={form.version}
            />
          </div>
          <TextField
            label="名称"
            onChange={(value) => setForm((prev) => ({ ...prev, name: value }))}
            value={form.name}
          />
          <TextField
            label="DataService Code"
            onChange={(value) =>
              setForm((prev) => ({ ...prev, dataServiceCode: value }))
            }
            value={form.dataServiceCode}
          />
          <TextField
            label="Path"
            onChange={(value) => setForm((prev) => ({ ...prev, path: value }))}
            value={form.path}
          />
          <div className="grid gap-3 md:grid-cols-2">
            <SelectField
              label="Method"
              onChange={(value) =>
                value && setForm((prev) => ({ ...prev, httpMethod: value }))
              }
              options={OPEN_API_METHODS}
              value={form.httpMethod}
            />
            <SelectField
              label="Auth"
              onChange={(value) =>
                value && setForm((prev) => ({ ...prev, authType: value }))
              }
              options={OPEN_API_AUTH_TYPES}
              value={form.authType}
            />
          </div>
          <TextareaField
            label="兼容说明"
            onChange={(value) =>
              setForm((prev) => ({ ...prev, compatibilityNotes: value }))
            }
            value={form.compatibilityNotes}
          />
          <Button disabled={saveMutation.isPending} type="submit">
            <Save className="h-4 w-4" />
            保存接口
          </Button>
          <div className="border-t border-slate-100 pt-3">
            <h4 className="font-semibold text-slate-950">授权</h4>
            <TextField
              label="Client Code"
              onChange={(value) =>
                setForm((prev) => ({ ...prev, clientCode: value }))
              }
              value={form.clientCode}
            />
            <TextField
              label="Secret Ref"
              onChange={(value) =>
                setForm((prev) => ({ ...prev, secretRef: value }))
              }
              value={form.secretRef}
            />
            <TextField
              label="Scopes"
              onChange={(value) =>
                setForm((prev) => ({ ...prev, scopes: value }))
              }
              value={form.scopes}
            />
            <div className="mt-2 flex gap-2">
              <Button
                disabled={!form.clientCode || credentialMutation.isPending}
                onClick={() => credentialMutation.mutate('save')}
                variant="outline"
              >
                <KeyRound className="h-4 w-4" />
                保存授权
              </Button>
              <Button
                disabled={!form.clientCode || credentialMutation.isPending}
                onClick={() => credentialMutation.mutate('revoke')}
                variant="outline"
              >
                撤销
              </Button>
            </div>
          </div>
          <div className="border-t border-slate-100 pt-3">
            <h4 className="font-semibold text-slate-950">限流</h4>
            <TextField
              label="Policy Code"
              onChange={(value) =>
                setForm((prev) => ({ ...prev, policyCode: value }))
              }
              value={form.policyCode}
            />
            <TextField
              label="Client Code"
              onChange={(value) =>
                setForm((prev) => ({ ...prev, policyClientCode: value }))
              }
              value={form.policyClientCode}
            />
            <div className="grid gap-3 md:grid-cols-2">
              <TextField
                label="Window"
                onChange={(value) =>
                  setForm((prev) => ({ ...prev, windowValue: Number(value) }))
                }
                type="number"
                value={form.windowValue}
              />
              <TextField
                label="Threshold"
                onChange={(value) =>
                  setForm((prev) => ({ ...prev, threshold: Number(value) }))
                }
                type="number"
                value={form.threshold}
              />
            </div>
            <div className="mt-2 flex gap-2">
              <Button
                disabled={!form.policyCode || policyMutation.isPending}
                onClick={() => policyMutation.mutate('save')}
                variant="outline"
              >
                保存限流
              </Button>
              <Button
                disabled={!form.policyCode || policyMutation.isPending}
                onClick={() => policyMutation.mutate('disable')}
                variant="outline"
              >
                停用限流
              </Button>
            </div>
          </div>
        </form>
      </div>
    </section>
  )
}

function ListCard<TItem>({
  title,
  items,
  renderItem,
}: {
  title: string
  items: TItem[]
  renderItem: (item: TItem) => ReactElement
}): ReactElement {
  return (
    <div className="rounded-lg border border-slate-200 p-3">
      <h4 className="font-semibold text-slate-950">{title}</h4>
      <div className="mt-2 space-y-2">
        {items.length ? (
          items.map((item, index) => (
            <div className="rounded-lg bg-slate-50 p-2 text-sm" key={index}>
              {renderItem(item)}
            </div>
          ))
        ) : (
          <p className="text-sm text-slate-500">暂无数据</p>
        )}
      </div>
    </div>
  )
}

function ReportPanel({ active }: { active: boolean }): ReactElement {
  const queryClient = useQueryClient()
  const [page, setPage] = useState(1)
  const [type, setType] = useState<ReportType | ''>('')
  const [status, setStatus] = useState<ReportStatus | ''>('')
  const [selectedCode, setSelectedCode] = useState('')
  const [formError, setFormError] = useState('')
  const [previewQuery, setPreviewQuery] = useState<ReportPreviewQuery>({})
  const [previewPayload, setPreviewPayload] = useState<unknown>(null)
  const [exportPayload, setExportPayload] = useState<unknown>(null)
  const [form, setForm] = useState<ReportForm>({
    code: '',
    name: '',
    reportType: 'SUMMARY',
    sourceScope: 'TASK',
    refreshMode: 'ON_DEMAND',
    visibilityMode: 'INTERNAL',
    status: 'DRAFT',
    sourceProviderKey: 'data-service',
    subjectCode: 'default',
    dataServiceCode: '',
    metricCode: 'total',
    metricName: 'Total',
    dimensionCode: 'time',
    dimensionName: 'Time',
    filtersText: '',
  })

  const reportsQuery = useQuery({
    enabled: active,
    queryKey: ['data-services', 'reports', page, type, status],
    queryFn: () =>
      dataServicesService.listReports({
        reportType: type || undefined,
        status: status || undefined,
        page,
        size: PAGE_SIZE,
      }),
  })
  const reportItems = useMemo(
    () => reportsQuery.data?.items ?? [],
    [reportsQuery.data?.items],
  )

  useEffect(() => {
    if (!selectedCode && reportItems[0]) {
      setSelectedCode(reportItems[0].code)
    }
  }, [reportItems, selectedCode])

  const detailQuery = useQuery({
    enabled: active && Boolean(selectedCode),
    queryKey: ['data-services', 'reports', selectedCode],
    queryFn: () => dataServicesService.getReport(selectedCode),
  })
  const snapshotsQuery = useQuery({
    enabled: active && Boolean(selectedCode),
    queryKey: ['data-services', 'reports', selectedCode, 'snapshots'],
    queryFn: () =>
      dataServicesService.reportSnapshots(selectedCode, 1, PAGE_SIZE),
  })

  const invalidate = () =>
    queryClient.invalidateQueries({ queryKey: ['data-services', 'reports'] })

  const buildReport = (): ReportDefinition => ({
    code: form.code.trim(),
    name: form.name.trim(),
    reportType: form.reportType,
    sourceScope: form.sourceScope,
    refreshMode: form.refreshMode,
    visibilityMode: form.visibilityMode,
    status: form.status,
    tenantId: getCurrentTenantId(),
    caliber: {
      sourceProviderKey: form.sourceProviderKey.trim(),
      subjectCode: form.subjectCode.trim(),
      dataServiceCode: form.dataServiceCode.trim() || undefined,
      baseFilters: parseKeyValueLines(form.filtersText),
      triggerEventTypes: [],
    },
    refreshConfig: {
      refreshIntervalSeconds: null,
      staleAfterSeconds: 3600,
      maxRows: 500,
    },
    cardProtocol: {
      cardCode: form.code.trim(),
      title: form.name.trim(),
      cardType: form.reportType === 'CARD' ? 'MIXED' : form.reportType,
      summaryMetricCode: form.metricCode.trim(),
      trendMetricCode: form.metricCode.trim(),
      rankMetricCode: form.metricCode.trim(),
      rankDimensionCode: form.dimensionCode.trim(),
      maxItems: 10,
    },
    metrics: [
      {
        metricCode: form.metricCode.trim(),
        metricName: form.metricName.trim(),
        aggregationType: 'COUNT',
        trendEnabled: true,
        rankEnabled: true,
        displayOrder: 1,
      },
    ],
    dimensions: [
      {
        dimensionCode: form.dimensionCode.trim(),
        dimensionName: form.dimensionName.trim(),
        dimensionType: 'TIME',
        sourceField: form.dimensionCode.trim(),
        timeGranularity: 'DAY',
        filterable: true,
        displayOrder: 1,
      },
    ],
  })

  const saveMutation = useMutation({
    mutationFn: () =>
      selectedCode
        ? dataServicesService.updateReport(selectedCode, buildReport())
        : dataServicesService.createReport(buildReport()),
    onSuccess: (detail) => {
      setSelectedCode(detail.code)
      invalidate()
    },
  })
  const actionMutation = useMutation({
    mutationFn: async (action: ReportStatus | 'refresh') => {
      if (action === 'refresh') {
        await dataServicesService.refreshReport(
          selectedCode,
          'portal-web manual refresh',
        )
        return
      }

      await dataServicesService.changeReportStatus(selectedCode, action)
    },
    onSuccess: invalidate,
  })
  const previewMutation = useMutation<unknown, Error, ReportType | 'EXPORT'>({
    mutationFn: (kind: ReportType | 'EXPORT') => {
      const query: ReportPreviewQuery = {
        ...previewQuery,
        filters: parseKeyValueLines(form.filtersText),
      }
      if (kind === 'TREND') {
        return dataServicesService.reportTrend(selectedCode, query)
      }
      if (kind === 'RANK') {
        return dataServicesService.reportRanking(selectedCode, query)
      }
      if (kind === 'EXPORT') {
        return dataServicesService.exportReportCsv(selectedCode, query)
      }
      return dataServicesService.reportSummary(selectedCode, query)
    },
    onSuccess: (payload, kind) => {
      if (kind === 'EXPORT') {
        const file = payload as ReportExportFile
        downloadReportFile(file)
        setExportPayload({
          filename: file.filename,
          size: file.blob.size,
          contentType: file.blob.type || 'text/csv',
        })
      } else {
        setPreviewPayload(payload)
      }
    },
  })

  const loadDetailToForm = (detail?: ReportDefinition) => {
    if (!detail) {
      return
    }

    setForm({
      code: detail.code,
      name: detail.name,
      reportType: detail.reportType,
      sourceScope: detail.sourceScope,
      refreshMode: detail.refreshMode,
      visibilityMode: detail.visibilityMode,
      status: detail.status,
      sourceProviderKey: detail.caliber.sourceProviderKey,
      subjectCode: detail.caliber.subjectCode,
      dataServiceCode: detail.caliber.dataServiceCode ?? '',
      metricCode: detail.metrics[0]?.metricCode ?? 'total',
      metricName: detail.metrics[0]?.metricName ?? 'Total',
      dimensionCode: detail.dimensions[0]?.dimensionCode ?? 'time',
      dimensionName: detail.dimensions[0]?.dimensionName ?? 'Time',
      filtersText: Object.entries(detail.caliber.baseFilters)
        .map(([key, value]) => `${key}=${value}`)
        .join('\n'),
    })
  }

  const handleSave = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    if (!form.code.trim() || !form.name.trim() || !form.metricCode.trim()) {
      setFormError('code、name、metricCode 必填')
      return
    }
    setFormError('')
    saveMutation.mutate()
  }

  return (
    <section className="rounded-lg border border-slate-200 bg-white shadow-sm">
      <PanelTitle
        description={TAB_ITEMS[4].description}
        icon={BarChart3}
        title="Report 工作台"
      />
      <div className="grid gap-4 p-4 xl:grid-cols-[minmax(0,1fr)_420px]">
        <div className="space-y-4">
          <div className="grid gap-3 md:grid-cols-4">
            <SelectField
              includeAll
              label="类型"
              onChange={setType}
              options={REPORT_TYPES}
              value={type}
            />
            <SelectField
              includeAll
              label="状态"
              onChange={setStatus}
              options={REPORT_STATUSES}
              value={status}
            />
            <div />
            <Button
              className="self-end"
              onClick={() => reportsQuery.refetch()}
              variant="outline"
            >
              <RefreshCcw className="h-4 w-4" />
              刷新
            </Button>
          </div>
          <StateBlock
            error={reportsQuery.error}
            isEmpty={!reportItems.length}
            isLoading={reportsQuery.isLoading}
            onRetry={() => reportsQuery.refetch()}
          />
          {reportItems.length ? (
            <div className="overflow-auto rounded-lg border border-slate-200">
              <table className="min-w-full divide-y divide-slate-100 text-sm">
                <thead className="bg-slate-50 text-left text-xs uppercase text-slate-500">
                  <tr>
                    <th className="px-3 py-2">报表</th>
                    <th className="px-3 py-2">范围</th>
                    <th className="px-3 py-2">可见性</th>
                    <th className="px-3 py-2">状态</th>
                    <th className="px-3 py-2">刷新</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {reportItems.map((item: ReportDefinition) => (
                    <tr
                      className={cn(
                        'cursor-pointer hover:bg-slate-50',
                        selectedCode === item.code && 'bg-sky-50',
                      )}
                      key={item.code}
                      onClick={() => setSelectedCode(item.code)}
                    >
                      <td className="px-3 py-2 font-medium text-slate-900">
                        {item.code}
                        <div className="text-xs font-normal text-slate-500">
                          {item.name}
                        </div>
                      </td>
                      <td className="px-3 py-2">{item.sourceScope}</td>
                      <td className="px-3 py-2">{item.visibilityMode}</td>
                      <td className="px-3 py-2">
                        <StatusBadge status={item.status} />
                      </td>
                      <td className="px-3 py-2">
                        <StatusBadge status={item.lastFreshnessStatus} />
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
              <PaginationBar
                pagination={getPagination(reportsQuery.data)}
                setPage={setPage}
              />
            </div>
          ) : null}

          {detailQuery.data ? (
            <div className="rounded-lg border border-slate-200 p-4">
              <div className="flex flex-wrap items-center justify-between gap-2">
                <h3 className="font-semibold text-slate-950">报表预览</h3>
                <div className="flex flex-wrap gap-2">
                  <Button
                    onClick={() => loadDetailToForm(detailQuery.data)}
                    size="sm"
                    variant="outline"
                  >
                    <Eye className="h-4 w-4" />
                    载入编辑
                  </Button>
                  <Button
                    disabled={actionMutation.isPending}
                    onClick={() => actionMutation.mutate('ACTIVE')}
                    size="sm"
                  >
                    启用
                  </Button>
                  <Button
                    disabled={actionMutation.isPending}
                    onClick={() => actionMutation.mutate('ARCHIVED')}
                    size="sm"
                    variant="outline"
                  >
                    归档
                  </Button>
                  <Button
                    disabled={actionMutation.isPending}
                    onClick={() => actionMutation.mutate('refresh')}
                    size="sm"
                    variant="outline"
                  >
                    刷新
                  </Button>
                </div>
              </div>
              <div className="mt-3 grid gap-3 md:grid-cols-4">
                <TextField
                  label="From"
                  onChange={(value) =>
                    setPreviewQuery((prev) => ({ ...prev, from: value }))
                  }
                  value={previewQuery.from ?? ''}
                />
                <TextField
                  label="To"
                  onChange={(value) =>
                    setPreviewQuery((prev) => ({ ...prev, to: value }))
                  }
                  value={previewQuery.to ?? ''}
                />
                <TextField
                  label="Metric"
                  onChange={(value) =>
                    setPreviewQuery((prev) => ({ ...prev, metricCode: value }))
                  }
                  value={previewQuery.metricCode ?? ''}
                />
                <TextField
                  label="Dimension"
                  onChange={(value) =>
                    setPreviewQuery((prev) => ({
                      ...prev,
                      dimensionCode: value,
                    }))
                  }
                  value={previewQuery.dimensionCode ?? ''}
                />
              </div>
              <div className="mt-3 flex flex-wrap gap-2">
                <Button
                  disabled={previewMutation.isPending}
                  onClick={() => previewMutation.mutate('SUMMARY')}
                  size="sm"
                >
                  摘要预览
                </Button>
                <Button
                  disabled={previewMutation.isPending}
                  onClick={() => previewMutation.mutate('TREND')}
                  size="sm"
                  variant="outline"
                >
                  趋势预览
                </Button>
                <Button
                  disabled={previewMutation.isPending}
                  onClick={() => previewMutation.mutate('RANK')}
                  size="sm"
                  variant="outline"
                >
                  排名预览
                </Button>
                <Button
                  disabled={previewMutation.isPending}
                  onClick={() => previewMutation.mutate('EXPORT')}
                  size="sm"
                  variant="outline"
                >
                  <FileDown className="h-4 w-4" />
                  导出入口
                </Button>
              </div>
              <div className="mt-3 grid gap-4 lg:grid-cols-2">
                <div>
                  <h4 className="mb-2 font-semibold text-slate-950">
                    预览结果
                  </h4>
                  {previewPayload ? <JsonBlock value={previewPayload} /> : null}
                  {exportPayload ? <JsonBlock value={exportPayload} /> : null}
                </div>
                <ListCard
                  items={snapshotsQuery.data?.items ?? []}
                  renderItem={(item: ReportSnapshot) => (
                    <div>
                      <div className="font-medium">
                        {formatTime(item.snapshotAt)}
                      </div>
                      <div className="text-xs text-slate-500">
                        {item.rowCount} rows / {item.freshnessStatus}
                      </div>
                    </div>
                  )}
                  title="刷新快照"
                />
              </div>
            </div>
          ) : null}
        </div>

        <form
          className="space-y-3 rounded-lg border border-slate-200 p-4"
          onSubmit={handleSave}
        >
          <h3 className="font-semibold text-slate-950">报表定义</h3>
          {formError ? (
            <p className="text-sm text-rose-600">{formError}</p>
          ) : null}
          <TextField
            label="Code"
            onChange={(value) => setForm((prev) => ({ ...prev, code: value }))}
            value={form.code}
          />
          <TextField
            label="名称"
            onChange={(value) => setForm((prev) => ({ ...prev, name: value }))}
            value={form.name}
          />
          <div className="grid gap-3 md:grid-cols-2">
            <SelectField
              label="类型"
              onChange={(value) =>
                value && setForm((prev) => ({ ...prev, reportType: value }))
              }
              options={REPORT_TYPES}
              value={form.reportType}
            />
            <SelectField
              label="范围"
              onChange={(value) =>
                value && setForm((prev) => ({ ...prev, sourceScope: value }))
              }
              options={REPORT_SCOPES}
              value={form.sourceScope}
            />
          </div>
          <div className="grid gap-3 md:grid-cols-2">
            <SelectField
              label="刷新"
              onChange={(value) =>
                value && setForm((prev) => ({ ...prev, refreshMode: value }))
              }
              options={REPORT_REFRESH_MODES}
              value={form.refreshMode}
            />
            <SelectField
              label="可见性"
              onChange={(value) =>
                value && setForm((prev) => ({ ...prev, visibilityMode: value }))
              }
              options={REPORT_VISIBILITY_MODES}
              value={form.visibilityMode}
            />
          </div>
          <SelectField
            label="状态"
            onChange={(value) =>
              value && setForm((prev) => ({ ...prev, status: value }))
            }
            options={REPORT_STATUSES}
            value={form.status}
          />
          <TextField
            label="Source Provider"
            onChange={(value) =>
              setForm((prev) => ({ ...prev, sourceProviderKey: value }))
            }
            value={form.sourceProviderKey}
          />
          <TextField
            label="Subject Code"
            onChange={(value) =>
              setForm((prev) => ({ ...prev, subjectCode: value }))
            }
            value={form.subjectCode}
          />
          <TextField
            label="DataService Code"
            onChange={(value) =>
              setForm((prev) => ({ ...prev, dataServiceCode: value }))
            }
            value={form.dataServiceCode}
          />
          <div className="grid gap-3 md:grid-cols-2">
            <TextField
              label="Metric Code"
              onChange={(value) =>
                setForm((prev) => ({ ...prev, metricCode: value }))
              }
              value={form.metricCode}
            />
            <TextField
              label="Metric Name"
              onChange={(value) =>
                setForm((prev) => ({ ...prev, metricName: value }))
              }
              value={form.metricName}
            />
          </div>
          <div className="grid gap-3 md:grid-cols-2">
            <TextField
              label="Dimension Code"
              onChange={(value) =>
                setForm((prev) => ({ ...prev, dimensionCode: value }))
              }
              value={form.dimensionCode}
            />
            <TextField
              label="Dimension Name"
              onChange={(value) =>
                setForm((prev) => ({ ...prev, dimensionName: value }))
              }
              value={form.dimensionName}
            />
          </div>
          <TextareaField
            label="过滤 key=value"
            onChange={(value) =>
              setForm((prev) => ({ ...prev, filtersText: value }))
            }
            value={form.filtersText}
          />
          <Button disabled={saveMutation.isPending} type="submit">
            <Save className="h-4 w-4" />
            保存报表
          </Button>
        </form>
      </div>
    </section>
  )
}

function GovernancePanel({ active }: { active: boolean }): ReactElement {
  const queryClient = useQueryClient()
  const [query, setQuery] = useState<GovernanceListQuery>({
    tenantId: getCurrentTenantId(),
    targetType: 'API',
    page: 1,
    size: PAGE_SIZE,
  })
  const [selectedAlertId, setSelectedAlertId] = useState('')
  const [formError, setFormError] = useState('')
  const [form, setForm] = useState<GovernanceForm>({
    profileCode: '',
    scopeType: 'API',
    targetCode: '',
    slaPolicyJson: '{}',
    alertPolicyJson: '{}',
    version: '1.0.0',
    changeSummary: '',
    approvalNote: '',
    interventionTargetType: 'API',
    interventionTargetCode: '',
    interventionTraceId: '',
    interventionActionType: 'ADD_NOTE',
    interventionReason: '',
  })

  const profilesQuery = useQuery({
    enabled: active,
    queryKey: ['data-services', 'governance', 'profiles', query],
    queryFn: () => dataServicesService.listGovernanceProfiles(query),
  })
  const healthQuery = useQuery({
    enabled: active,
    queryKey: ['data-services', 'governance', 'health', query],
    queryFn: () => dataServicesService.listGovernanceHealthSnapshots(query),
  })
  const alertsQuery = useQuery({
    enabled: active,
    queryKey: ['data-services', 'governance', 'alerts', query],
    queryFn: () => dataServicesService.listGovernanceAlerts(query),
  })
  const tracesQuery = useQuery({
    enabled: active,
    queryKey: ['data-services', 'governance', 'traces', query],
    queryFn: () => dataServicesService.listGovernanceTraces(query),
  })
  const auditsQuery = useQuery({
    enabled: active,
    queryKey: ['data-services', 'governance', 'audits', query],
    queryFn: () => dataServicesService.listGovernanceAudits(query),
  })
  const versionsQuery = useQuery({
    enabled: active,
    queryKey: ['data-services', 'governance', 'versions', query],
    queryFn: () => dataServicesService.listServiceVersions(query),
  })

  const invalidate = () =>
    queryClient.invalidateQueries({ queryKey: ['data-services', 'governance'] })

  const profileMutation = useMutation({
    mutationFn: () => {
      parseJsonObject(form.slaPolicyJson)
      parseJsonObject(form.alertPolicyJson)

      return dataServicesService.upsertGovernanceProfile({
        tenantId: getCurrentTenantId(),
        code: form.profileCode.trim(),
        scopeType: form.scopeType,
        targetCode: form.targetCode.trim(),
        slaPolicyJson: form.slaPolicyJson,
        alertPolicyJson: form.alertPolicyJson,
        status: 'ACTIVE',
      })
    },
    onSuccess: invalidate,
  })
  const runHealthMutation = useMutation({
    mutationFn: () =>
      dataServicesService.runGovernanceHealthChecks(
        query.targetType,
        query.targetCode,
      ),
    onSuccess: invalidate,
  })
  const alertMutation = useMutation({
    mutationFn: (action: 'acknowledge' | 'escalate' | 'close') =>
      dataServicesService.handleGovernanceAlert(selectedAlertId, action, {
        tenantId: getCurrentTenantId(),
        reason: `portal-web ${action}`,
      }),
    onSuccess: invalidate,
  })
  const interventionMutation = useMutation({
    mutationFn: () =>
      dataServicesService.submitGovernanceIntervention({
        tenantId: getCurrentTenantId(),
        targetType: form.interventionTargetType,
        targetCode: form.interventionTargetCode.trim(),
        traceId: form.interventionTraceId.trim() || undefined,
        actionType: form.interventionActionType,
        reason: form.interventionReason.trim() || undefined,
      }),
    onSuccess: invalidate,
  })
  const versionMutation = useMutation({
    mutationFn: (action: 'register' | 'publish' | 'deprecate') => {
      const payload = {
        tenantId: getCurrentTenantId(),
        profileCode: form.profileCode.trim(),
        targetType: form.scopeType,
        targetCode: form.targetCode.trim(),
        version: form.version.trim(),
        changeSummary: form.changeSummary.trim() || undefined,
        approvalNote: form.approvalNote.trim() || undefined,
      }

      if (action === 'publish') {
        return dataServicesService.publishServiceVersion(payload)
      }
      if (action === 'deprecate') {
        return dataServicesService.deprecateServiceVersion(payload)
      }
      return dataServicesService.registerServiceVersion(payload)
    },
    onSuccess: invalidate,
  })

  const handleProfileSave = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    if (!form.profileCode.trim() || !form.targetCode.trim()) {
      setFormError('profileCode、targetCode 必填')
      return
    }
    setFormError('')
    profileMutation.mutate()
  }

  return (
    <section className="rounded-lg border border-slate-200 bg-white shadow-sm">
      <PanelTitle
        description={TAB_ITEMS[5].description}
        icon={ShieldCheck}
        title="Governance 工作台"
      />
      <div className="grid gap-4 p-4 xl:grid-cols-[minmax(0,1fr)_420px]">
        <div className="space-y-4">
          <div className="grid gap-3 md:grid-cols-5">
            <SelectField
              label="对象类型"
              onChange={(value) =>
                value &&
                setQuery((prev) => ({
                  ...prev,
                  targetType: value,
                  scopeType: value,
                }))
              }
              options={GOVERNANCE_SCOPES}
              value={query.targetType ?? 'API'}
            />
            <TextField
              label="对象编码"
              onChange={(value) =>
                setQuery((prev) => ({
                  ...prev,
                  targetCode: value || undefined,
                }))
              }
              value={query.targetCode ?? ''}
            />
            <SelectField
              includeAll
              label="告警状态"
              onChange={(value) =>
                setQuery((prev) => ({ ...prev, status: value || undefined }))
              }
              options={ALERT_STATUSES}
              value={(query.status as AlertStatus | undefined) ?? ''}
            />
            <Button
              className="self-end"
              disabled={runHealthMutation.isPending}
              onClick={() => runHealthMutation.mutate()}
              variant="outline"
            >
              <Activity className="h-4 w-4" />
              质量检查
            </Button>
            <Button
              className="self-end"
              onClick={() => profilesQuery.refetch()}
              variant="outline"
            >
              <RefreshCcw className="h-4 w-4" />
              刷新
            </Button>
          </div>
          <StateBlock
            error={
              profilesQuery.error ||
              healthQuery.error ||
              alertsQuery.error ||
              tracesQuery.error ||
              auditsQuery.error ||
              versionsQuery.error
            }
            isEmpty={
              !profilesQuery.data?.items.length &&
              !healthQuery.data?.items.length &&
              !alertsQuery.data?.items.length
            }
            isLoading={
              profilesQuery.isLoading ||
              healthQuery.isLoading ||
              alertsQuery.isLoading
            }
            onRetry={() => {
              profilesQuery.refetch()
              healthQuery.refetch()
              alertsQuery.refetch()
              tracesQuery.refetch()
              auditsQuery.refetch()
              versionsQuery.refetch()
            }}
          />
          <div className="grid gap-4 lg:grid-cols-2">
            <ListCard
              items={profilesQuery.data?.items ?? []}
              renderItem={(item: GovernanceProfile) => (
                <div>
                  <div className="font-medium">{item.code}</div>
                  <div className="text-xs text-slate-500">
                    {item.scopeType}/{item.targetCode}{' '}
                    <StatusBadge status={item.status} />
                  </div>
                </div>
              )}
              title="治理配置"
            />
            <ListCard
              items={healthQuery.data?.items ?? []}
              renderItem={(item) => (
                <div>
                  <div className="font-medium">{item.ruleCode}</div>
                  <div className="text-xs text-slate-500">
                    {item.targetType}/{item.targetCode} {item.measuredValue}/
                    {item.thresholdValue}
                  </div>
                  <StatusBadge status={item.healthStatus} />
                </div>
              )}
              title="数据质量"
            />
          </div>
          <div className="rounded-lg border border-slate-200 p-4">
            <h3 className="font-semibold text-slate-950">告警与血缘</h3>
            <div className="mt-3 grid gap-4 lg:grid-cols-2">
              <div className="space-y-2">
                {(alertsQuery.data?.items ?? []).map(
                  (item: GovernanceAlertRecord) => (
                    <div
                      className={cn(
                        'cursor-pointer rounded-lg border border-slate-100 p-3 text-sm',
                        selectedAlertId === item.alertId && 'bg-sky-50',
                      )}
                      key={item.alertId}
                      onClick={() => setSelectedAlertId(item.alertId)}
                    >
                      <div className="flex items-center justify-between">
                        <span className="font-medium">{item.summary}</span>
                        <StatusBadge status={item.alertLevel} />
                      </div>
                      <p className="mt-1 text-slate-500">
                        {item.detail ?? item.alertKey}
                      </p>
                      <StatusBadge status={item.status} />
                    </div>
                  ),
                )}
                <div className="flex flex-wrap gap-2">
                  <Button
                    disabled={!selectedAlertId || alertMutation.isPending}
                    onClick={() => alertMutation.mutate('acknowledge')}
                    size="sm"
                    variant="outline"
                  >
                    确认
                  </Button>
                  <Button
                    disabled={!selectedAlertId || alertMutation.isPending}
                    onClick={() => alertMutation.mutate('escalate')}
                    size="sm"
                    variant="outline"
                  >
                    升级
                  </Button>
                  <Button
                    disabled={!selectedAlertId || alertMutation.isPending}
                    onClick={() => alertMutation.mutate('close')}
                    size="sm"
                    variant="outline"
                  >
                    关闭
                  </Button>
                </div>
              </div>
              <div className="space-y-2">
                {(tracesQuery.data?.items ?? []).map(
                  (item: GovernanceTraceRecord) => (
                    <div
                      className="rounded-lg border border-slate-100 p-3 text-sm"
                      key={item.traceId}
                    >
                      <div className="flex items-center justify-between">
                        <span className="font-medium">{item.traceType}</span>
                        <StatusBadge status={item.status} />
                      </div>
                      <p className="mt-1 text-slate-500">{item.summary}</p>
                      <div className="text-xs text-slate-500">
                        {item.correlationId ??
                          item.sourceExecutionId ??
                          item.traceId}
                      </div>
                    </div>
                  ),
                )}
              </div>
            </div>
          </div>
          <div className="grid gap-4 lg:grid-cols-2">
            <ListCard
              items={auditsQuery.data?.items ?? []}
              renderItem={(item) => (
                <div>
                  <div className="font-medium">{item.actionType}</div>
                  <div className="text-xs text-slate-500">
                    {item.operatorName ?? item.operatorId} / {item.actionResult}
                  </div>
                </div>
              )}
              title="权限与操作审计"
            />
            <ListCard
              items={versionsQuery.data?.items ?? []}
              renderItem={(item) => (
                <div>
                  <div className="font-medium">{item.version}</div>
                  <div className="text-xs text-slate-500">
                    {item.targetType}/{item.targetCode}{' '}
                    <StatusBadge status={item.status} />
                  </div>
                </div>
              )}
              title="版本治理"
            />
          </div>
        </div>

        <form
          className="space-y-3 rounded-lg border border-slate-200 p-4"
          onSubmit={handleProfileSave}
        >
          <h3 className="font-semibold text-slate-950">治理配置 / 干预</h3>
          {formError ? (
            <p className="text-sm text-rose-600">{formError}</p>
          ) : null}
          <TextField
            label="Profile Code"
            onChange={(value) =>
              setForm((prev) => ({ ...prev, profileCode: value }))
            }
            value={form.profileCode}
          />
          <SelectField
            label="Scope"
            onChange={(value) =>
              value && setForm((prev) => ({ ...prev, scopeType: value }))
            }
            options={GOVERNANCE_SCOPES}
            value={form.scopeType}
          />
          <TextField
            label="Target Code"
            onChange={(value) =>
              setForm((prev) => ({ ...prev, targetCode: value }))
            }
            value={form.targetCode}
          />
          <TextareaField
            label="SLA Policy JSON"
            onChange={(value) =>
              setForm((prev) => ({ ...prev, slaPolicyJson: value }))
            }
            value={form.slaPolicyJson}
          />
          <TextareaField
            label="Alert Policy JSON"
            onChange={(value) =>
              setForm((prev) => ({ ...prev, alertPolicyJson: value }))
            }
            value={form.alertPolicyJson}
          />
          <Button disabled={profileMutation.isPending} type="submit">
            <Save className="h-4 w-4" />
            保存治理配置
          </Button>
          <div className="border-t border-slate-100 pt-3">
            <h4 className="font-semibold text-slate-950">版本</h4>
            <TextField
              label="Version"
              onChange={(value) =>
                setForm((prev) => ({ ...prev, version: value }))
              }
              value={form.version}
            />
            <TextareaField
              label="Change Summary"
              onChange={(value) =>
                setForm((prev) => ({ ...prev, changeSummary: value }))
              }
              rows={2}
              value={form.changeSummary}
            />
            <TextareaField
              label="Approval Note"
              onChange={(value) =>
                setForm((prev) => ({ ...prev, approvalNote: value }))
              }
              rows={2}
              value={form.approvalNote}
            />
            <div className="flex flex-wrap gap-2">
              <Button
                disabled={!form.profileCode || versionMutation.isPending}
                onClick={() => versionMutation.mutate('register')}
                variant="outline"
              >
                注册
              </Button>
              <Button
                disabled={!form.profileCode || versionMutation.isPending}
                onClick={() => versionMutation.mutate('publish')}
                variant="outline"
              >
                发布
              </Button>
              <Button
                disabled={!form.profileCode || versionMutation.isPending}
                onClick={() => versionMutation.mutate('deprecate')}
                variant="outline"
              >
                废弃
              </Button>
            </div>
          </div>
          <div className="border-t border-slate-100 pt-3">
            <h4 className="font-semibold text-slate-950">权限干预</h4>
            <SelectField
              label="Target Type"
              onChange={(value) =>
                value &&
                setForm((prev) => ({ ...prev, interventionTargetType: value }))
              }
              options={GOVERNANCE_SCOPES}
              value={form.interventionTargetType}
            />
            <TextField
              label="Target Code"
              onChange={(value) =>
                setForm((prev) => ({ ...prev, interventionTargetCode: value }))
              }
              value={form.interventionTargetCode}
            />
            <TextField
              label="Trace ID"
              onChange={(value) =>
                setForm((prev) => ({ ...prev, interventionTraceId: value }))
              }
              value={form.interventionTraceId}
            />
            <SelectField
              label="Action"
              onChange={(value) =>
                value &&
                setForm((prev) => ({ ...prev, interventionActionType: value }))
              }
              options={GOVERNANCE_ACTIONS}
              value={form.interventionActionType}
            />
            <TextareaField
              label="Reason"
              onChange={(value) =>
                setForm((prev) => ({ ...prev, interventionReason: value }))
              }
              rows={2}
              value={form.interventionReason}
            />
            <Button
              disabled={
                !form.interventionTargetCode || interventionMutation.isPending
              }
              onClick={() => interventionMutation.mutate()}
              variant="outline"
            >
              提交干预
            </Button>
          </div>
        </form>
      </div>
    </section>
  )
}

export default function DataServicesPage(): ReactElement {
  const [activeTab, setActiveTab] = useState<TabKey>('connector')
  const activeItem = useMemo(
    () => TAB_ITEMS.find((item) => item.key === activeTab) ?? TAB_ITEMS[0],
    [activeTab],
  )

  return (
    <div className="space-y-4">
      <section className="rounded-lg border border-slate-200 bg-white p-4 shadow-sm">
        <div className="flex flex-wrap items-start justify-between gap-4">
          <div>
            <Badge className="w-fit">DataServices</Badge>
            <h1 className="mt-2 text-xl font-semibold text-slate-950">
              数据服务工作台
            </h1>
            <p className="mt-1 text-sm text-slate-500">
              {activeItem.description}
            </p>
          </div>
          <PermissionState />
        </div>
        <div
          aria-label="数据服务能力"
          className="mt-4 flex overflow-x-auto border-t border-slate-100 pt-3"
          role="tablist"
        >
          {TAB_ITEMS.map((item) => {
            const selected = item.key === activeTab
            const Icon = item.icon

            return (
              <button
                aria-selected={selected}
                className={cn(
                  'flex h-10 shrink-0 items-center gap-2 rounded-lg px-3 text-sm font-medium transition',
                  selected
                    ? 'bg-slate-900 text-white'
                    : 'text-slate-600 hover:bg-slate-100 hover:text-slate-950',
                )}
                key={item.key}
                onClick={() => setActiveTab(item.key)}
                role="tab"
                type="button"
              >
                <Icon className="h-4 w-4" />
                {item.label}
              </button>
            )
          })}
        </div>
      </section>

      {activeTab === 'connector' ? <ConnectorPanel active /> : null}
      {activeTab === 'sync' ? <SyncPanel active /> : null}
      {activeTab === 'service' ? <DataServicePanel active /> : null}
      {activeTab === 'openApi' ? <OpenApiPanel active /> : null}
      {activeTab === 'report' ? <ReportPanel active /> : null}
      {activeTab === 'governance' ? <GovernancePanel active /> : null}
    </div>
  )
}
