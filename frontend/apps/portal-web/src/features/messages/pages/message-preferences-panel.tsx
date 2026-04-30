import { useEffect, useMemo, useState, type ReactElement } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { BellRing, Moon, Save, Smartphone } from 'lucide-react'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import {
  messageConfigQueryKeys,
  useMobileDevicesQuery,
  usePushPreferenceQuery,
  useSubscriptionPreferencesQuery,
} from '@/features/messages/hooks/use-message-config-query'
import {
  savePushPreference,
  saveSubscriptionPreference,
} from '@/features/messages/services/message-service'
import type {
  MessageCategory,
  MessageChannelType,
  SubscriptionPreference,
} from '@/features/messages/types/message'

const EVENT_CATEGORIES: MessageCategory[] = [
  'TODO_CREATED',
  'TODO_OVERDUE',
  'PROCESS_TASK_OVERDUE',
  'ORG_ACCOUNT_LOCKED',
  'SYSTEM_SECURITY',
]

const CHANNELS: MessageChannelType[] = [
  'INBOX',
  'EMAIL',
  'SMS',
  'WEBHOOK',
  'APP_PUSH',
]

const COPY = {
  pushTitle: '移动推送偏好',
  subscriptionTitle: '事件订阅偏好',
  devicesTitle: '设备与推送 Token',
  enabledText: '启用',
  disabledText: '停用',
  quietStartText: '免打扰开始',
  quietEndText: '免打扰结束',
  mutedCategoriesText: '静音分类',
  saveText: '保存',
  categoryText: '分类',
  digestText: '摘要',
  escalationText: '升级',
  muteNonWorkingHoursText: '非工作时间静音',
  activeText: '启用订阅',
  emptyDevicesText: '暂无已绑定设备',
} as const

interface SubscriptionFormState {
  category: MessageCategory
  allowedChannels: MessageChannelType[]
  quietStartsAt: string
  quietEndsAt: string
  digestMode: SubscriptionPreference['digestMode']
  escalationOptIn: boolean
  muteNonWorkingHours: boolean
  enabled: boolean
}

function splitCsv(value: string): string[] {
  return value
    .split(',')
    .map((item) => item.trim())
    .filter(Boolean)
}

function joinCsv(value: string[] | null | undefined): string {
  return (value ?? []).join(', ')
}

function toggleChannel(
  channels: MessageChannelType[],
  channel: MessageChannelType,
): MessageChannelType[] {
  return channels.includes(channel)
    ? channels.filter((item) => item !== channel)
    : [...channels, channel]
}

function toSubscriptionForm(
  preference?: SubscriptionPreference,
): SubscriptionFormState {
  return {
    category: preference?.category ?? 'TODO_CREATED',
    allowedChannels: preference?.allowedChannels ?? ['INBOX', 'APP_PUSH'],
    quietStartsAt: preference?.quietWindow?.startsAt ?? '',
    quietEndsAt: preference?.quietWindow?.endsAt ?? '',
    digestMode: preference?.digestMode ?? 'IMMEDIATE',
    escalationOptIn: preference?.escalationOptIn ?? true,
    muteNonWorkingHours: preference?.muteNonWorkingHours ?? false,
    enabled: preference?.enabled ?? true,
  }
}

export function MessagePreferencesPanel(): ReactElement {
  const queryClient = useQueryClient()
  const pushPreferenceQuery = usePushPreferenceQuery()
  const subscriptionPreferencesQuery = useSubscriptionPreferencesQuery()
  const devicesQuery = useMobileDevicesQuery()
  const [pushEnabled, setPushEnabled] = useState(true)
  const [quietStartsAt, setQuietStartsAt] = useState('')
  const [quietEndsAt, setQuietEndsAt] = useState('')
  const [mutedCategories, setMutedCategories] = useState('')
  const [subscriptionForm, setSubscriptionForm] =
    useState<SubscriptionFormState>(toSubscriptionForm())

  const preferencesByCategory = useMemo(() => {
    const map = new Map<MessageCategory, SubscriptionPreference>()

    subscriptionPreferencesQuery.data?.forEach((preference) => {
      map.set(preference.category, preference)
    })

    return map
  }, [subscriptionPreferencesQuery.data])

  useEffect(() => {
    if (!pushPreferenceQuery.data) {
      return
    }

    setPushEnabled(pushPreferenceQuery.data.pushEnabled)
    setQuietStartsAt(pushPreferenceQuery.data.quietStartsAt ?? '')
    setQuietEndsAt(pushPreferenceQuery.data.quietEndsAt ?? '')
    setMutedCategories(joinCsv(pushPreferenceQuery.data.mutedCategories))
  }, [pushPreferenceQuery.data])

  useEffect(() => {
    const preference = preferencesByCategory.get(subscriptionForm.category)
    setSubscriptionForm((current) => ({
      ...toSubscriptionForm(preference),
      category: current.category,
    }))
  }, [preferencesByCategory, subscriptionForm.category])

  const savePushMutation = useMutation({
    mutationFn: savePushPreference,
    onSuccess: async () => {
      await queryClient.invalidateQueries({
        queryKey: messageConfigQueryKeys.pushPreference(),
      })
    },
  })

  const saveSubscriptionMutation = useMutation({
    mutationFn: saveSubscriptionPreference,
    onSuccess: async () => {
      await queryClient.invalidateQueries({
        queryKey: messageConfigQueryKeys.subscriptionPreferences(),
      })
    },
  })

  return (
    <div className="grid gap-5 xl:grid-cols-[minmax(0,1fr)_360px]">
      <div className="space-y-5">
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <BellRing className="h-5 w-5 text-sky-600" />
              {COPY.pushTitle}
            </CardTitle>
          </CardHeader>
          <CardContent className="grid gap-4 md:grid-cols-2">
            <label className="flex items-center gap-3 rounded-xl border border-slate-200 px-3 py-2 text-sm">
              <input
                checked={pushEnabled}
                onChange={(event) => setPushEnabled(event.target.checked)}
                type="checkbox"
              />
              {pushEnabled ? COPY.enabledText : COPY.disabledText}
            </label>
            <label className="space-y-1 text-sm text-slate-600">
              <span>{COPY.mutedCategoriesText}</span>
              <Input
                onChange={(event) => setMutedCategories(event.target.value)}
                placeholder="SYSTEM_SECURITY,TODO_OVERDUE"
                value={mutedCategories}
              />
            </label>
            <label className="space-y-1 text-sm text-slate-600">
              <span>{COPY.quietStartText}</span>
              <Input
                onChange={(event) => setQuietStartsAt(event.target.value)}
                type="time"
                value={quietStartsAt}
              />
            </label>
            <label className="space-y-1 text-sm text-slate-600">
              <span>{COPY.quietEndText}</span>
              <Input
                onChange={(event) => setQuietEndsAt(event.target.value)}
                type="time"
                value={quietEndsAt}
              />
            </label>
            <div className="md:col-span-2">
              <Button
                disabled={savePushMutation.isPending}
                onClick={() =>
                  savePushMutation.mutate({
                    pushEnabled,
                    quietStartsAt: quietStartsAt || undefined,
                    quietEndsAt: quietEndsAt || undefined,
                    mutedCategories: splitCsv(mutedCategories),
                  })
                }
              >
                <Save className="h-4 w-4" />
                {COPY.saveText}
              </Button>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <Moon className="h-5 w-5 text-sky-600" />
              {COPY.subscriptionTitle}
            </CardTitle>
          </CardHeader>
          <CardContent className="grid gap-4 md:grid-cols-2">
            <label className="space-y-1 text-sm text-slate-600">
              <span>{COPY.categoryText}</span>
              <select
                className="h-10 w-full rounded-xl border border-slate-200 bg-white px-3 text-sm"
                onChange={(event) =>
                  setSubscriptionForm((current) => ({
                    ...current,
                    category: event.target.value as MessageCategory,
                  }))
                }
                value={subscriptionForm.category}
              >
                {EVENT_CATEGORIES.map((category) => (
                  <option key={category} value={category}>
                    {category}
                  </option>
                ))}
              </select>
            </label>
            <label className="space-y-1 text-sm text-slate-600">
              <span>{COPY.digestText}</span>
              <select
                className="h-10 w-full rounded-xl border border-slate-200 bg-white px-3 text-sm"
                onChange={(event) =>
                  setSubscriptionForm((current) => ({
                    ...current,
                    digestMode: event.target
                      .value as SubscriptionPreference['digestMode'],
                  }))
                }
                value={subscriptionForm.digestMode}
              >
                <option value="IMMEDIATE">IMMEDIATE</option>
                <option value="PERIODIC_DIGEST">PERIODIC_DIGEST</option>
                <option value="DISABLED">DISABLED</option>
              </select>
            </label>
            <div className="space-y-2 md:col-span-2">
              <span className="text-sm text-slate-600">通道</span>
              <div className="flex flex-wrap gap-2">
                {CHANNELS.map((channel) => (
                  <button
                    className={
                      subscriptionForm.allowedChannels.includes(channel)
                        ? 'rounded-xl bg-sky-100 px-3 py-2 text-sm font-medium text-sky-700'
                        : 'rounded-xl bg-slate-100 px-3 py-2 text-sm font-medium text-slate-600'
                    }
                    key={channel}
                    onClick={() =>
                      setSubscriptionForm((current) => ({
                        ...current,
                        allowedChannels: toggleChannel(
                          current.allowedChannels,
                          channel,
                        ),
                      }))
                    }
                    type="button"
                  >
                    {channel}
                  </button>
                ))}
              </div>
            </div>
            <label className="space-y-1 text-sm text-slate-600">
              <span>{COPY.quietStartText}</span>
              <Input
                onChange={(event) =>
                  setSubscriptionForm((current) => ({
                    ...current,
                    quietStartsAt: event.target.value,
                  }))
                }
                type="time"
                value={subscriptionForm.quietStartsAt}
              />
            </label>
            <label className="space-y-1 text-sm text-slate-600">
              <span>{COPY.quietEndText}</span>
              <Input
                onChange={(event) =>
                  setSubscriptionForm((current) => ({
                    ...current,
                    quietEndsAt: event.target.value,
                  }))
                }
                type="time"
                value={subscriptionForm.quietEndsAt}
              />
            </label>
            <label className="flex items-center gap-3 rounded-xl border border-slate-200 px-3 py-2 text-sm">
              <input
                checked={subscriptionForm.enabled}
                onChange={(event) =>
                  setSubscriptionForm((current) => ({
                    ...current,
                    enabled: event.target.checked,
                  }))
                }
                type="checkbox"
              />
              {COPY.activeText}
            </label>
            <label className="flex items-center gap-3 rounded-xl border border-slate-200 px-3 py-2 text-sm">
              <input
                checked={subscriptionForm.escalationOptIn}
                onChange={(event) =>
                  setSubscriptionForm((current) => ({
                    ...current,
                    escalationOptIn: event.target.checked,
                  }))
                }
                type="checkbox"
              />
              {COPY.escalationText}
            </label>
            <label className="flex items-center gap-3 rounded-xl border border-slate-200 px-3 py-2 text-sm">
              <input
                checked={subscriptionForm.muteNonWorkingHours}
                onChange={(event) =>
                  setSubscriptionForm((current) => ({
                    ...current,
                    muteNonWorkingHours: event.target.checked,
                  }))
                }
                type="checkbox"
              />
              {COPY.muteNonWorkingHoursText}
            </label>
            <div className="md:col-span-2">
              <Button
                disabled={saveSubscriptionMutation.isPending}
                onClick={() =>
                  saveSubscriptionMutation.mutate(subscriptionForm)
                }
              >
                <Save className="h-4 w-4" />
                {COPY.saveText}
              </Button>
            </div>
          </CardContent>
        </Card>
      </div>

      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <Smartphone className="h-5 w-5 text-sky-600" />
            {COPY.devicesTitle}
          </CardTitle>
        </CardHeader>
        <CardContent className="space-y-3">
          {devicesQuery.data?.length === 0 ? (
            <div className="text-sm text-slate-500">
              {COPY.emptyDevicesText}
            </div>
          ) : null}
          {devicesQuery.data?.map((device) => (
            <div
              className="space-y-2 rounded-xl border border-slate-200 p-3 text-sm"
              key={device.id}
            >
              <div className="flex items-center justify-between gap-3">
                <div className="font-medium text-slate-900">
                  {device.platform} / {device.appType}
                </div>
                <Badge
                  variant={
                    device.bindStatus === 'ACTIVE' ? 'success' : 'secondary'
                  }
                >
                  {device.bindStatus}
                </Badge>
              </div>
              <div className="break-all text-slate-500">{device.deviceId}</div>
              <div className="break-all text-xs text-slate-400">
                {device.pushToken ?? '-'}
              </div>
            </div>
          ))}
        </CardContent>
      </Card>
    </div>
  )
}
