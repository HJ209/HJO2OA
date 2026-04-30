import { useState, type FormEvent, type ReactElement } from 'react'
import { Clock3, Globe2, Save, ShieldAlert } from 'lucide-react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { InfraPageSection } from '@/features/infra-admin/components/infra-page-section'
import {
  InfraTable,
  StatusPill,
} from '@/features/infra-admin/components/infra-table'
import { useTimezoneSettings } from '@/features/infra-admin/hooks/use-timezone'
import { timezoneService } from '@/features/infra-admin/services/timezone-service'
import type { TimezoneConversion } from '@/features/infra-admin/types/infra'
import {
  formatUtcToUserTimezone,
  getUserLocale,
  getUserTimezone,
  setUserTimezone,
} from '@/utils/format-time'

export default function TimezonePage(): ReactElement {
  const queryClient = useQueryClient()
  const [timezoneId, setTimezoneId] = useState(getUserTimezone())
  const [systemTimezoneId, setSystemTimezoneId] = useState('UTC')
  const [scopeType, setScopeType] = useState<
    'SYSTEM' | 'TENANT' | 'PERSON' | ''
  >('')
  const [keyword, setKeyword] = useState('')
  const [utcInstant, setUtcInstant] = useState('2026-04-29T08:00:00Z')
  const [converted, setConverted] = useState<TimezoneConversion | null>(null)
  const [errorMessage, setErrorMessage] = useState<string | null>(null)

  const query = useTimezoneSettings({
    page: 1,
    size: 50,
    scopeType: scopeType || undefined,
    keyword: keyword || undefined,
  })

  const invalidate = async (): Promise<void> => {
    await queryClient.invalidateQueries({ queryKey: ['infra', 'timezone'] })
  }

  const setTenantTimezone = useMutation({
    mutationFn: () => timezoneService.setCurrentTenantTimezone(timezoneId),
    onSuccess: async () => {
      setUserTimezone(timezoneId)
      setErrorMessage(null)
      await invalidate()
    },
    onError: (error) =>
      setErrorMessage(
        error instanceof Error
          ? error.message
          : 'Failed to save tenant timezone',
      ),
  })

  const setSystemTimezone = useMutation({
    mutationFn: () => timezoneService.setSystemDefault(systemTimezoneId),
    onSuccess: async () => {
      setErrorMessage(null)
      await invalidate()
    },
    onError: (error) =>
      setErrorMessage(
        error instanceof Error
          ? error.message
          : 'Failed to save system timezone',
      ),
  })

  const convertFromUtc = useMutation({
    mutationFn: () => timezoneService.convertFromUtc(utcInstant, timezoneId),
    onSuccess: (result) => {
      setConverted(result)
      setErrorMessage(null)
    },
    onError: (error) =>
      setErrorMessage(
        error instanceof Error ? error.message : 'Failed to convert time',
      ),
  })

  const handleTenantSubmit = (event: FormEvent<HTMLFormElement>): void => {
    event.preventDefault()
    setTenantTimezone.mutate()
  }

  const handleSystemSubmit = (event: FormEvent<HTMLFormElement>): void => {
    event.preventDefault()
    setSystemTimezone.mutate()
  }

  const handleConvert = (event: FormEvent<HTMLFormElement>): void => {
    event.preventDefault()
    convertFromUtc.mutate()
  }

  return (
    <InfraPageSection
      actions={<Badge variant="secondary">{getUserLocale()}</Badge>}
      description="Resolve user and tenant timezones, persist UTC values, and preview local rendering."
      title="Timezone"
    >
      <div className="space-y-4">
        {errorMessage ? (
          <div className="flex items-center gap-2 rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">
            <ShieldAlert className="h-4 w-4" />
            {errorMessage}
          </div>
        ) : null}

        <div className="grid gap-3 md:grid-cols-3">
          <form
            className="rounded-lg border border-slate-200 p-3"
            onSubmit={handleTenantSubmit}
          >
            <div className="mb-3 flex items-center gap-2 text-sm font-semibold text-slate-900">
              <Globe2 className="h-4 w-4" />
              Tenant timezone
            </div>
            <div className="flex gap-2">
              <Input
                aria-label="Tenant timezone"
                onChange={(event) => setTimezoneId(event.target.value)}
                placeholder="Asia/Shanghai"
                value={timezoneId}
              />
              <Button disabled={setTenantTimezone.isPending} type="submit">
                <Save className="h-4 w-4" />
                Save
              </Button>
            </div>
            <p className="mt-2 text-xs text-slate-500">
              Current display:{' '}
              {formatUtcToUserTimezone(
                new Date().toISOString(),
                getUserLocale(),
                timezoneId,
              )}
            </p>
          </form>

          <form
            className="rounded-lg border border-slate-200 p-3"
            onSubmit={handleSystemSubmit}
          >
            <div className="mb-3 flex items-center gap-2 text-sm font-semibold text-slate-900">
              <Clock3 className="h-4 w-4" />
              System default
            </div>
            <div className="flex gap-2">
              <Input
                aria-label="System timezone"
                onChange={(event) => setSystemTimezoneId(event.target.value)}
                placeholder="UTC"
                value={systemTimezoneId}
              />
              <Button
                disabled={setSystemTimezone.isPending}
                type="submit"
                variant="outline"
              >
                Save
              </Button>
            </div>
          </form>

          <form
            className="rounded-lg border border-slate-200 p-3"
            onSubmit={handleConvert}
          >
            <div className="mb-3 text-sm font-semibold text-slate-900">
              UTC to local
            </div>
            <div className="flex gap-2">
              <Input
                aria-label="UTC instant"
                onChange={(event) => setUtcInstant(event.target.value)}
                value={utcInstant}
              />
              <Button
                disabled={convertFromUtc.isPending}
                type="submit"
                variant="outline"
              >
                Convert
              </Button>
            </div>
            <p className="mt-2 text-xs text-slate-500">
              {converted?.localDateTime
                ? `${converted.localDateTime} (${converted.timezoneId})`
                : 'No conversion yet'}
            </p>
          </form>
        </div>

        <div className="grid gap-3 md:grid-cols-3">
          <Input
            aria-label="Timezone search"
            onChange={(event) => setKeyword(event.target.value)}
            placeholder="search timezone or scope"
            value={keyword}
          />
          <select
            aria-label="Scope type"
            className="h-10 rounded-xl border border-slate-200 bg-white px-3 text-sm text-slate-900"
            onChange={(event) =>
              setScopeType(event.target.value as typeof scopeType)
            }
            value={scopeType}
          >
            <option value="">All scopes</option>
            <option value="SYSTEM">SYSTEM</option>
            <option value="TENANT">TENANT</option>
            <option value="PERSON">PERSON</option>
          </select>
          <div className="flex items-center justify-end text-sm text-slate-500">
            Browser timezone header: {getUserTimezone()}
          </div>
        </div>

        <InfraTable
          columns={[
            {
              key: 'scopeType',
              title: 'Scope',
              render: (item) => item.scopeType,
            },
            {
              key: 'scopeId',
              title: 'Scope ID',
              render: (item) => item.scopeId ?? '-',
            },
            {
              key: 'timezoneId',
              title: 'Timezone',
              render: (item) => item.timezoneId,
            },
            {
              key: 'default',
              title: 'Default',
              render: (item) => (
                <StatusPill active={item.isDefault}>
                  {item.isDefault ? 'yes' : 'no'}
                </StatusPill>
              ),
            },
            {
              key: 'active',
              title: 'Active',
              render: (item) => (
                <StatusPill active={item.active}>
                  {item.active ? 'active' : 'off'}
                </StatusPill>
              ),
            },
            {
              key: 'effectiveFrom',
              title: 'Effective',
              render: (item) =>
                formatUtcToUserTimezone(
                  item.effectiveFrom,
                  getUserLocale(),
                  timezoneId,
                ),
            },
            {
              key: 'tenantId',
              title: 'Tenant',
              render: (item) => item.tenantId ?? '-',
            },
          ]}
          getRowKey={(item) => item.id}
          isLoading={query.isLoading}
          items={query.data?.items ?? []}
        />
      </div>
    </InfraPageSection>
  )
}
