import { useMemo, useState, type ReactElement } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import {
  BellElectric,
  PlugZap,
  RefreshCw,
  Route,
  Save,
  Send,
} from 'lucide-react'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import {
  messageConfigQueryKeys,
  useChannelEndpointsQuery,
  useMessageTemplatesQuery,
  useRetryableDeliveryTasksQuery,
  useRoutingPoliciesQuery,
} from '@/features/messages/hooks/use-message-config-query'
import {
  changeChannelEndpointStatus,
  createChannelEndpoint,
  createMessageTemplate,
  createRoutingPolicy,
  publishMessageTemplate,
  renderMessageTemplate,
  retryDeliveryTask,
  sendTestMessage,
  type CreateEndpointInput,
  type CreateRoutingPolicyInput,
  type CreateTemplateInput,
  type SendTestMessageInput,
} from '@/features/messages/services/message-service'
import type {
  ChannelEndpoint,
  MessageCategory,
  MessageChannelType,
  MessagePriority,
  MessageProviderType,
  RenderedMessageTemplate,
  RoutingPolicy,
} from '@/features/messages/types/message'

const CHANNELS: MessageChannelType[] = [
  'INBOX',
  'EMAIL',
  'SMS',
  'WEBHOOK',
  'APP_PUSH',
]

const PROVIDERS: MessageProviderType[] = [
  'INTERNAL',
  'SMTP',
  'WEBHOOK',
  'ALIYUN_SMS',
  'TENCENT_SMS',
  'WECHAT_WORK',
  'DINGTALK',
  'FCM',
  'APNS',
]

const CATEGORIES: MessageCategory[] = [
  'TODO',
  'TODO_CREATED',
  'TODO_OVERDUE',
  'PROCESS_TASK_OVERDUE',
  'APPROVAL',
  'COMMENT',
  'SYSTEM',
  'SECURITY',
  'ORG_ACCOUNT_LOCKED',
  'SYSTEM_SECURITY',
  'BUSINESS_NOTICE',
  'GENERAL',
]

const PRIORITIES: MessagePriority[] = ['LOW', 'NORMAL', 'HIGH', 'URGENT']

const COPY = {
  templateTitle: '消息模板',
  endpointTitle: '通道端点',
  routingTitle: '路由策略',
  deliveryTitle: '失败投递',
  testTitle: '通道测试',
  saveText: '保存',
  publishText: '发布',
  enableText: '启用',
  disableText: '停用',
  sendText: '发送测试',
  retryText: '重试',
  renderText: '渲染',
  emptyText: '暂无数据',
} as const

const initialTemplateForm: CreateTemplateInput = {
  code: '',
  channelType: 'INBOX',
  locale: 'zh-CN',
  version: 1,
  category: 'GENERAL',
  titleTemplate: '',
  bodyTemplate: '',
  variableSchema: '',
  systemLocked: false,
}

const initialEndpointForm: CreateEndpointInput = {
  endpointCode: '',
  channelType: 'EMAIL',
  providerType: 'SMTP',
  displayName: '',
  configRef: '',
  rateLimitPerMinute: 60,
  dailyQuota: 1000,
}

const initialRoutingForm: CreateRoutingPolicyInput = {
  policyCode: '',
  category: 'GENERAL',
  priorityThreshold: 'NORMAL',
  targetChannelOrder: ['INBOX'],
  fallbackChannelOrder: ['INBOX'],
  quietWindowBehavior: 'DEFER',
  dedupWindowSeconds: 300,
  escalationPolicy: '',
}

const initialTestForm: SendTestMessageInput = {
  channelType: 'EMAIL',
  endpointId: '',
  target: '',
  title: '',
  body: '',
  deepLink: '',
}

function splitChannels(value: string): MessageChannelType[] {
  return value
    .split(',')
    .map((item) => item.trim())
    .filter((item): item is MessageChannelType =>
      CHANNELS.includes(item as MessageChannelType),
    )
}

function channelCsv(value: MessageChannelType[]): string {
  return value.join(',')
}

function EndpointRow({
  endpoint,
}: {
  endpoint: ChannelEndpoint
}): ReactElement {
  const queryClient = useQueryClient()
  const mutation = useMutation({
    mutationFn: () =>
      changeChannelEndpointStatus(
        endpoint.id,
        endpoint.status === 'ENABLED' ? 'DISABLED' : 'ENABLED',
      ),
    onSuccess: async () => {
      await queryClient.invalidateQueries({
        queryKey: messageConfigQueryKeys.endpoints(),
      })
    },
  })

  return (
    <div className="rounded-xl border border-slate-200 p-3 text-sm">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <div className="font-semibold text-slate-900">
          {endpoint.displayName}
        </div>
        <Badge
          variant={endpoint.status === 'ENABLED' ? 'success' : 'secondary'}
        >
          {endpoint.status}
        </Badge>
      </div>
      <div className="mt-2 break-all text-slate-500">
        {endpoint.endpointCode} / {endpoint.channelType} /{' '}
        {endpoint.providerType}
      </div>
      <div className="mt-2 break-all text-xs text-slate-400">
        {endpoint.configRef}
      </div>
      <Button
        className="mt-3"
        disabled={mutation.isPending}
        onClick={() => mutation.mutate()}
        size="sm"
        variant="outline"
      >
        {endpoint.status === 'ENABLED' ? COPY.disableText : COPY.enableText}
      </Button>
    </div>
  )
}

function RoutingRow({ policy }: { policy: RoutingPolicy }): ReactElement {
  return (
    <div className="rounded-xl border border-slate-200 p-3 text-sm">
      <div className="flex flex-wrap items-center gap-2">
        <span className="font-semibold text-slate-900">
          {policy.policyCode}
        </span>
        <Badge variant={policy.enabled ? 'success' : 'secondary'}>
          {policy.enabled ? 'ENABLED' : 'DISABLED'}
        </Badge>
        <Badge variant="secondary">{policy.category}</Badge>
        <Badge variant="secondary">{policy.priorityThreshold}</Badge>
      </div>
      <div className="mt-2 text-slate-500">
        {policy.targetChannelOrder.join(' -> ')}
      </div>
      <div className="mt-1 text-xs text-slate-400">
        {policy.quietWindowBehavior} / {policy.dedupWindowSeconds}s
      </div>
    </div>
  )
}

export function MessageChannelConfigPanel(): ReactElement {
  const queryClient = useQueryClient()
  const templatesQuery = useMessageTemplatesQuery()
  const endpointsQuery = useChannelEndpointsQuery()
  const routingQuery = useRoutingPoliciesQuery()
  const retryableTasksQuery = useRetryableDeliveryTasksQuery()
  const [templateForm, setTemplateForm] =
    useState<CreateTemplateInput>(initialTemplateForm)
  const [endpointForm, setEndpointForm] =
    useState<CreateEndpointInput>(initialEndpointForm)
  const [routingForm, setRoutingForm] =
    useState<CreateRoutingPolicyInput>(initialRoutingForm)
  const [targetChannelCsv, setTargetChannelCsv] = useState('INBOX')
  const [fallbackChannelCsv, setFallbackChannelCsv] = useState('INBOX')
  const [testForm, setTestForm] =
    useState<SendTestMessageInput>(initialTestForm)
  const [renderVariables, setRenderVariables] = useState('{"name":"HJO2OA"}')
  const [renderedTemplate, setRenderedTemplate] =
    useState<RenderedMessageTemplate | null>(null)

  const activeEndpoints = useMemo(
    () =>
      endpointsQuery.data?.filter(
        (endpoint) => endpoint.status === 'ENABLED',
      ) ?? [],
    [endpointsQuery.data],
  )

  const invalidateConfig = async (): Promise<void> => {
    await queryClient.invalidateQueries({
      queryKey: messageConfigQueryKeys.all,
    })
  }

  const createTemplateMutation = useMutation({
    mutationFn: createMessageTemplate,
    onSuccess: async () => {
      setTemplateForm(initialTemplateForm)
      await invalidateConfig()
    },
  })

  const publishTemplateMutation = useMutation({
    mutationFn: publishMessageTemplate,
    onSuccess: invalidateConfig,
  })

  const renderMutation = useMutation({
    mutationFn: () =>
      renderMessageTemplate(
        templateForm.code,
        templateForm.channelType,
        templateForm.locale,
        renderVariables,
      ),
    onSuccess: (result) => setRenderedTemplate(result),
  })

  const createEndpointMutation = useMutation({
    mutationFn: createChannelEndpoint,
    onSuccess: async () => {
      setEndpointForm(initialEndpointForm)
      await invalidateConfig()
    },
  })

  const createRoutingMutation = useMutation({
    mutationFn: createRoutingPolicy,
    onSuccess: async () => {
      setRoutingForm(initialRoutingForm)
      setTargetChannelCsv('INBOX')
      setFallbackChannelCsv('INBOX')
      await invalidateConfig()
    },
  })

  const sendTestMutation = useMutation({
    mutationFn: sendTestMessage,
    onSuccess: invalidateConfig,
  })

  const retryMutation = useMutation({
    mutationFn: retryDeliveryTask,
    onSuccess: invalidateConfig,
  })

  return (
    <div className="grid gap-5 xl:grid-cols-2">
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <BellElectric className="h-5 w-5 text-sky-600" />
            {COPY.templateTitle}
          </CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="grid gap-3 md:grid-cols-2">
            <Input
              onChange={(event) =>
                setTemplateForm((current) => ({
                  ...current,
                  code: event.target.value,
                }))
              }
              placeholder="templateCode"
              value={templateForm.code}
            />
            <Input
              onChange={(event) =>
                setTemplateForm((current) => ({
                  ...current,
                  locale: event.target.value,
                }))
              }
              placeholder="zh-CN"
              value={templateForm.locale}
            />
            <select
              className="h-10 rounded-xl border border-slate-200 bg-white px-3 text-sm"
              onChange={(event) =>
                setTemplateForm((current) => ({
                  ...current,
                  channelType: event.target.value as MessageChannelType,
                }))
              }
              value={templateForm.channelType}
            >
              {CHANNELS.map((channel) => (
                <option key={channel} value={channel}>
                  {channel}
                </option>
              ))}
            </select>
            <select
              className="h-10 rounded-xl border border-slate-200 bg-white px-3 text-sm"
              onChange={(event) =>
                setTemplateForm((current) => ({
                  ...current,
                  category: event.target.value as MessageCategory,
                }))
              }
              value={templateForm.category}
            >
              {CATEGORIES.map((category) => (
                <option key={category} value={category}>
                  {category}
                </option>
              ))}
            </select>
          </div>
          <Input
            onChange={(event) =>
              setTemplateForm((current) => ({
                ...current,
                titleTemplate: event.target.value,
              }))
            }
            placeholder="标题模板 {{name}}"
            value={templateForm.titleTemplate}
          />
          <textarea
            className="min-h-24 w-full rounded-xl border border-slate-200 p-3 text-sm outline-none focus:border-sky-400 focus:ring-2 focus:ring-sky-100"
            onChange={(event) =>
              setTemplateForm((current) => ({
                ...current,
                bodyTemplate: event.target.value,
              }))
            }
            placeholder="正文模板"
            value={templateForm.bodyTemplate}
          />
          <div className="grid gap-3 md:grid-cols-[minmax(0,1fr)_auto_auto]">
            <Input
              onChange={(event) => setRenderVariables(event.target.value)}
              placeholder='{"name":"HJO2OA"}'
              value={renderVariables}
            />
            <Button
              disabled={!templateForm.code || renderMutation.isPending}
              onClick={() => renderMutation.mutate()}
              variant="outline"
            >
              {COPY.renderText}
            </Button>
            <Button
              disabled={
                createTemplateMutation.isPending ||
                !templateForm.code ||
                !templateForm.titleTemplate ||
                !templateForm.bodyTemplate
              }
              onClick={() => createTemplateMutation.mutate(templateForm)}
            >
              <Save className="h-4 w-4" />
              {COPY.saveText}
            </Button>
          </div>
          {renderedTemplate ? (
            <div className="rounded-xl bg-slate-50 p-3 text-sm text-slate-700">
              <div className="font-medium">{renderedTemplate.title}</div>
              <div className="mt-1 whitespace-pre-wrap">
                {renderedTemplate.body}
              </div>
            </div>
          ) : null}
          <div className="grid gap-3">
            {templatesQuery.data?.slice(0, 6).map((template) => (
              <div
                className="rounded-xl border border-slate-200 p-3 text-sm"
                key={template.id}
              >
                <div className="flex flex-wrap items-center justify-between gap-2">
                  <div className="font-semibold text-slate-900">
                    {template.code}
                  </div>
                  <Badge
                    variant={
                      template.status === 'PUBLISHED' ? 'success' : 'secondary'
                    }
                  >
                    {template.status}
                  </Badge>
                </div>
                <div className="mt-2 text-slate-500">
                  {template.channelType} / {template.locale} / v
                  {template.version}
                </div>
                {template.status !== 'PUBLISHED' ? (
                  <Button
                    className="mt-3"
                    disabled={publishTemplateMutation.isPending}
                    onClick={() => publishTemplateMutation.mutate(template.id)}
                    size="sm"
                    variant="outline"
                  >
                    {COPY.publishText}
                  </Button>
                ) : null}
              </div>
            ))}
          </div>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <PlugZap className="h-5 w-5 text-sky-600" />
            {COPY.endpointTitle}
          </CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="grid gap-3 md:grid-cols-2">
            <Input
              onChange={(event) =>
                setEndpointForm((current) => ({
                  ...current,
                  endpointCode: event.target.value,
                }))
              }
              placeholder="endpointCode"
              value={endpointForm.endpointCode}
            />
            <Input
              onChange={(event) =>
                setEndpointForm((current) => ({
                  ...current,
                  displayName: event.target.value,
                }))
              }
              placeholder="显示名称"
              value={endpointForm.displayName}
            />
            <select
              className="h-10 rounded-xl border border-slate-200 bg-white px-3 text-sm"
              onChange={(event) =>
                setEndpointForm((current) => ({
                  ...current,
                  channelType: event.target.value as MessageChannelType,
                }))
              }
              value={endpointForm.channelType}
            >
              {CHANNELS.filter((channel) => channel !== 'INBOX').map(
                (channel) => (
                  <option key={channel} value={channel}>
                    {channel}
                  </option>
                ),
              )}
            </select>
            <select
              className="h-10 rounded-xl border border-slate-200 bg-white px-3 text-sm"
              onChange={(event) =>
                setEndpointForm((current) => ({
                  ...current,
                  providerType: event.target.value as MessageProviderType,
                }))
              }
              value={endpointForm.providerType}
            >
              {PROVIDERS.map((provider) => (
                <option key={provider} value={provider}>
                  {provider}
                </option>
              ))}
            </select>
          </div>
          <Input
            onChange={(event) =>
              setEndpointForm((current) => ({
                ...current,
                configRef: event.target.value,
              }))
            }
            placeholder="smtp://host:25?from=no-reply@hjo2oa.local 或 https://..."
            value={endpointForm.configRef}
          />
          <Button
            disabled={
              createEndpointMutation.isPending ||
              !endpointForm.endpointCode ||
              !endpointForm.displayName ||
              !endpointForm.configRef
            }
            onClick={() => createEndpointMutation.mutate(endpointForm)}
          >
            <Save className="h-4 w-4" />
            {COPY.saveText}
          </Button>
          <div className="grid gap-3">
            {endpointsQuery.data?.length === 0 ? (
              <div className="text-sm text-slate-500">{COPY.emptyText}</div>
            ) : null}
            {endpointsQuery.data?.map((endpoint) => (
              <EndpointRow endpoint={endpoint} key={endpoint.id} />
            ))}
          </div>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <Route className="h-5 w-5 text-sky-600" />
            {COPY.routingTitle}
          </CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="grid gap-3 md:grid-cols-2">
            <Input
              onChange={(event) =>
                setRoutingForm((current) => ({
                  ...current,
                  policyCode: event.target.value,
                }))
              }
              placeholder="policyCode"
              value={routingForm.policyCode}
            />
            <select
              className="h-10 rounded-xl border border-slate-200 bg-white px-3 text-sm"
              onChange={(event) =>
                setRoutingForm((current) => ({
                  ...current,
                  category: event.target.value as MessageCategory,
                }))
              }
              value={routingForm.category}
            >
              {CATEGORIES.map((category) => (
                <option key={category} value={category}>
                  {category}
                </option>
              ))}
            </select>
            <select
              className="h-10 rounded-xl border border-slate-200 bg-white px-3 text-sm"
              onChange={(event) =>
                setRoutingForm((current) => ({
                  ...current,
                  priorityThreshold: event.target.value as MessagePriority,
                }))
              }
              value={routingForm.priorityThreshold}
            >
              {PRIORITIES.map((priority) => (
                <option key={priority} value={priority}>
                  {priority}
                </option>
              ))}
            </select>
            <select
              className="h-10 rounded-xl border border-slate-200 bg-white px-3 text-sm"
              onChange={(event) =>
                setRoutingForm((current) => ({
                  ...current,
                  quietWindowBehavior: event.target
                    .value as RoutingPolicy['quietWindowBehavior'],
                }))
              }
              value={routingForm.quietWindowBehavior}
            >
              <option value="DEFER">DEFER</option>
              <option value="BYPASS">BYPASS</option>
              <option value="SUPPRESS">SUPPRESS</option>
            </select>
          </div>
          <Input
            onChange={(event) => setTargetChannelCsv(event.target.value)}
            placeholder="INBOX,EMAIL,APP_PUSH"
            value={targetChannelCsv}
          />
          <Input
            onChange={(event) => setFallbackChannelCsv(event.target.value)}
            placeholder="INBOX"
            value={fallbackChannelCsv}
          />
          <Button
            disabled={
              createRoutingMutation.isPending || !routingForm.policyCode
            }
            onClick={() =>
              createRoutingMutation.mutate({
                ...routingForm,
                targetChannelOrder: splitChannels(targetChannelCsv),
                fallbackChannelOrder: splitChannels(fallbackChannelCsv),
              })
            }
          >
            <Save className="h-4 w-4" />
            {COPY.saveText}
          </Button>
          <div className="grid gap-3">
            {routingQuery.data?.map((policy) => (
              <RoutingRow key={policy.id} policy={policy} />
            ))}
          </div>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <Send className="h-5 w-5 text-sky-600" />
            {COPY.testTitle}
          </CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="grid gap-3 md:grid-cols-2">
            <select
              className="h-10 rounded-xl border border-slate-200 bg-white px-3 text-sm"
              onChange={(event) => {
                const endpoint = activeEndpoints.find(
                  (item) => item.id === event.target.value,
                )
                setTestForm((current) => ({
                  ...current,
                  endpointId: event.target.value,
                  channelType: endpoint?.channelType ?? current.channelType,
                }))
              }}
              value={testForm.endpointId}
            >
              <option value="">选择端点</option>
              {activeEndpoints.map((endpoint) => (
                <option key={endpoint.id} value={endpoint.id}>
                  {endpoint.displayName}
                </option>
              ))}
            </select>
            <Input
              onChange={(event) =>
                setTestForm((current) => ({
                  ...current,
                  target: event.target.value,
                }))
              }
              placeholder="目标地址/手机号/URL/token"
              value={testForm.target}
            />
            <Input
              onChange={(event) =>
                setTestForm((current) => ({
                  ...current,
                  title: event.target.value,
                }))
              }
              placeholder="标题"
              value={testForm.title}
            />
            <Input
              onChange={(event) =>
                setTestForm((current) => ({
                  ...current,
                  deepLink: event.target.value,
                }))
              }
              placeholder="/messages"
              value={testForm.deepLink}
            />
          </div>
          <textarea
            className="min-h-20 w-full rounded-xl border border-slate-200 p-3 text-sm outline-none focus:border-sky-400 focus:ring-2 focus:ring-sky-100"
            onChange={(event) =>
              setTestForm((current) => ({
                ...current,
                body: event.target.value,
              }))
            }
            placeholder="测试正文"
            value={testForm.body}
          />
          <Button
            disabled={
              sendTestMutation.isPending ||
              !testForm.endpointId ||
              !testForm.target ||
              !testForm.title ||
              !testForm.body
            }
            onClick={() => sendTestMutation.mutate(testForm)}
          >
            <Send className="h-4 w-4" />
            {COPY.sendText}
          </Button>
          <div className="space-y-3">
            <div className="flex items-center gap-2 font-semibold text-slate-900">
              <RefreshCw className="h-4 w-4 text-sky-600" />
              {COPY.deliveryTitle}
            </div>
            {retryableTasksQuery.data?.length === 0 ? (
              <div className="text-sm text-slate-500">{COPY.emptyText}</div>
            ) : null}
            {retryableTasksQuery.data?.map((task) => (
              <div
                className="rounded-xl border border-slate-200 p-3 text-sm"
                key={task.id}
              >
                <div className="flex flex-wrap items-center justify-between gap-2">
                  <div className="font-semibold text-slate-900">
                    {task.channelType} / {task.status}
                  </div>
                  <Button
                    disabled={retryMutation.isPending}
                    onClick={() => retryMutation.mutate(task.id)}
                    size="sm"
                    variant="outline"
                  >
                    <RefreshCw className="h-4 w-4" />
                    {COPY.retryText}
                  </Button>
                </div>
                <div className="mt-2 break-all text-slate-500">
                  {task.lastErrorCode ?? '-'} {task.lastErrorMessage ?? ''}
                </div>
                <div className="mt-1 text-xs text-slate-400">
                  retry={task.retryCount} next={task.nextRetryAt ?? '-'} route=
                  {task.routeOrder} channels={channelCsv([task.channelType])}
                </div>
              </div>
            ))}
          </div>
        </CardContent>
      </Card>
    </div>
  )
}
