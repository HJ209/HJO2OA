import { useState, type ReactElement } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { Power, Save, Workflow } from 'lucide-react'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import {
  messageConfigQueryKeys,
  useSubscriptionRulesQuery,
} from '@/features/messages/hooks/use-message-config-query'
import {
  createSubscriptionRule,
  toggleSubscriptionRule,
  type CreateSubscriptionRuleInput,
} from '@/features/messages/services/message-service'
import type {
  MessageCategory,
  MessagePriority,
  SubscriptionRule,
} from '@/features/messages/types/message'

const SUBSCRIPTION_CATEGORIES: MessageCategory[] = [
  'TODO_CREATED',
  'TODO_OVERDUE',
  'PROCESS_TASK_OVERDUE',
  'ORG_ACCOUNT_LOCKED',
  'SYSTEM_SECURITY',
]

const PRIORITIES: MessagePriority[] = ['LOW', 'NORMAL', 'HIGH', 'URGENT']

const COPY = {
  title: '事件订阅配置',
  ruleCodeText: '规则编码',
  eventTypeText: '事件类型模式',
  categoryText: '消息分类',
  targetResolverText: '目标解析',
  targetConfigText: '解析配置',
  templateText: '模板编码',
  conditionText: '条件表达式',
  priorityText: '默认优先级',
  enabledText: '启用',
  saveText: '保存规则',
  emptyText: '暂无订阅规则',
} as const

const initialForm: CreateSubscriptionRuleInput = {
  ruleCode: '',
  eventTypePattern: '',
  notificationCategory: 'TODO_CREATED',
  targetResolverType: 'PAYLOAD_PERSON',
  targetResolverConfig: '{"personField":"personId"}',
  templateCode: '',
  conditionExpr: '',
  priorityMapping: '',
  defaultPriority: 'NORMAL',
  enabled: true,
}

function RuleRow({ rule }: { rule: SubscriptionRule }): ReactElement {
  const queryClient = useQueryClient()
  const toggleMutation = useMutation({
    mutationFn: () => toggleSubscriptionRule(rule.id, !rule.enabled),
    onSuccess: async () => {
      await queryClient.invalidateQueries({
        queryKey: messageConfigQueryKeys.subscriptionRules(),
      })
    },
  })

  return (
    <div className="grid gap-3 rounded-xl border border-slate-200 p-3 text-sm lg:grid-cols-[minmax(0,1fr)_auto]">
      <div className="min-w-0 space-y-2">
        <div className="flex flex-wrap items-center gap-2">
          <span className="font-semibold text-slate-900">{rule.ruleCode}</span>
          <Badge variant={rule.enabled ? 'success' : 'secondary'}>
            {rule.enabled ? 'ENABLED' : 'DISABLED'}
          </Badge>
          <Badge variant="secondary">{rule.notificationCategory}</Badge>
          <Badge variant="secondary">{rule.defaultPriority}</Badge>
        </div>
        <div className="break-all text-slate-500">{rule.eventTypePattern}</div>
        <div className="break-all text-xs text-slate-400">
          {rule.targetResolverType} / {rule.templateCode}
        </div>
      </div>
      <Button
        disabled={toggleMutation.isPending}
        onClick={() => toggleMutation.mutate()}
        size="sm"
        variant="outline"
      >
        <Power className="h-4 w-4" />
        {rule.enabled ? '停用' : '启用'}
      </Button>
    </div>
  )
}

export function MessageSubscriptionConfigPanel(): ReactElement {
  const queryClient = useQueryClient()
  const rulesQuery = useSubscriptionRulesQuery()
  const [form, setForm] = useState<CreateSubscriptionRuleInput>(initialForm)
  const saveMutation = useMutation({
    mutationFn: createSubscriptionRule,
    onSuccess: async () => {
      setForm(initialForm)
      await queryClient.invalidateQueries({
        queryKey: messageConfigQueryKeys.subscriptionRules(),
      })
    },
  })

  return (
    <div className="grid gap-5 xl:grid-cols-[420px_minmax(0,1fr)]">
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <Workflow className="h-5 w-5 text-sky-600" />
            {COPY.title}
          </CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          <label className="space-y-1 text-sm text-slate-600">
            <span>{COPY.ruleCodeText}</span>
            <Input
              onChange={(event) =>
                setForm((current) => ({
                  ...current,
                  ruleCode: event.target.value,
                }))
              }
              value={form.ruleCode}
            />
          </label>
          <label className="space-y-1 text-sm text-slate-600">
            <span>{COPY.eventTypeText}</span>
            <Input
              onChange={(event) =>
                setForm((current) => ({
                  ...current,
                  eventTypePattern: event.target.value,
                }))
              }
              placeholder="wf.process.*"
              value={form.eventTypePattern}
            />
          </label>
          <div className="grid gap-3 md:grid-cols-2">
            <label className="space-y-1 text-sm text-slate-600">
              <span>{COPY.categoryText}</span>
              <select
                className="h-10 w-full rounded-xl border border-slate-200 bg-white px-3 text-sm"
                onChange={(event) =>
                  setForm((current) => ({
                    ...current,
                    notificationCategory: event.target.value as MessageCategory,
                  }))
                }
                value={form.notificationCategory}
              >
                {SUBSCRIPTION_CATEGORIES.map((category) => (
                  <option key={category} value={category}>
                    {category}
                  </option>
                ))}
              </select>
            </label>
            <label className="space-y-1 text-sm text-slate-600">
              <span>{COPY.priorityText}</span>
              <select
                className="h-10 w-full rounded-xl border border-slate-200 bg-white px-3 text-sm"
                onChange={(event) =>
                  setForm((current) => ({
                    ...current,
                    defaultPriority: event.target.value as MessagePriority,
                  }))
                }
                value={form.defaultPriority}
              >
                {PRIORITIES.map((priority) => (
                  <option key={priority} value={priority}>
                    {priority}
                  </option>
                ))}
              </select>
            </label>
          </div>
          <label className="space-y-1 text-sm text-slate-600">
            <span>{COPY.targetResolverText}</span>
            <select
              className="h-10 w-full rounded-xl border border-slate-200 bg-white px-3 text-sm"
              onChange={(event) =>
                setForm((current) => ({
                  ...current,
                  targetResolverType: event.target
                    .value as SubscriptionRule['targetResolverType'],
                }))
              }
              value={form.targetResolverType}
            >
              <option value="PAYLOAD_PERSON">PAYLOAD_PERSON</option>
              <option value="PAYLOAD_ASSIGNMENT">PAYLOAD_ASSIGNMENT</option>
              <option value="INITIATOR">INITIATOR</option>
              <option value="ROLE_MEMBERS">ROLE_MEMBERS</option>
              <option value="ORG_MEMBERS">ORG_MEMBERS</option>
              <option value="EXPRESSION">EXPRESSION</option>
            </select>
          </label>
          <label className="space-y-1 text-sm text-slate-600">
            <span>{COPY.targetConfigText}</span>
            <Input
              onChange={(event) =>
                setForm((current) => ({
                  ...current,
                  targetResolverConfig: event.target.value,
                }))
              }
              value={form.targetResolverConfig ?? ''}
            />
          </label>
          <label className="space-y-1 text-sm text-slate-600">
            <span>{COPY.templateText}</span>
            <Input
              onChange={(event) =>
                setForm((current) => ({
                  ...current,
                  templateCode: event.target.value,
                }))
              }
              value={form.templateCode}
            />
          </label>
          <label className="space-y-1 text-sm text-slate-600">
            <span>{COPY.conditionText}</span>
            <Input
              onChange={(event) =>
                setForm((current) => ({
                  ...current,
                  conditionExpr: event.target.value,
                }))
              }
              value={form.conditionExpr ?? ''}
            />
          </label>
          <label className="flex items-center gap-3 rounded-xl border border-slate-200 px-3 py-2 text-sm">
            <input
              checked={form.enabled}
              onChange={(event) =>
                setForm((current) => ({
                  ...current,
                  enabled: event.target.checked,
                }))
              }
              type="checkbox"
            />
            {COPY.enabledText}
          </label>
          <Button
            disabled={
              saveMutation.isPending ||
              !form.ruleCode ||
              !form.eventTypePattern ||
              !form.templateCode
            }
            onClick={() => saveMutation.mutate(form)}
          >
            <Save className="h-4 w-4" />
            {COPY.saveText}
          </Button>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>规则列表</CardTitle>
        </CardHeader>
        <CardContent className="space-y-3">
          {rulesQuery.data?.length === 0 ? (
            <div className="text-sm text-slate-500">{COPY.emptyText}</div>
          ) : null}
          {rulesQuery.data?.map((rule) => (
            <RuleRow key={rule.id} rule={rule} />
          ))}
        </CardContent>
      </Card>
    </div>
  )
}
