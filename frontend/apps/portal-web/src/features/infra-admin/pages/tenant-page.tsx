import { useMemo, useState, type ReactElement } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { InfraPageSection } from '@/features/infra-admin/components/infra-page-section'
import {
  InfraTable,
  StatusPill,
} from '@/features/infra-admin/components/infra-table'
import {
  useTenantDetail,
  useTenants,
} from '@/features/infra-admin/hooks/use-tenant'
import { tenantService } from '@/features/infra-admin/services/tenant-service'
import type {
  TenantProfile,
  TenantQuota,
  TenantQuotaType,
} from '@/features/infra-admin/types/infra'

const emptyTenant: TenantProfile = {
  id: '',
  name: '',
  domain: '',
  status: 'draft',
  timezone: 'Asia/Shanghai',
  tenantCode: '',
  packageCode: 'basic',
  defaultLocale: 'zh-CN',
  defaultTimezone: 'Asia/Shanghai',
  isolationMode: 'SHARED_DB',
  initialized: false,
}

const quotaTypes: TenantQuotaType[] = [
  'USER_COUNT',
  'ATTACHMENT_STORAGE',
  'API_CALL',
  'DATA_SIZE',
  'JOB_COUNT',
]

export default function TenantPage(): ReactElement {
  const [draft, setDraft] = useState<TenantProfile>(emptyTenant)
  const [selectedTenantId, setSelectedTenantId] = useState<string>()
  const [quotaDraft, setQuotaDraft] = useState({
    quotaType: 'USER_COUNT' as TenantQuotaType,
    limitValue: 0,
    warningThreshold: 0,
  })
  const queryClient = useQueryClient()
  const tenantsQuery = useTenants({ page: 1, size: 50 })
  const selectedTenantQuery = useTenantDetail(selectedTenantId)
  const tenants = tenantsQuery.data?.items ?? []
  const selectedTenant = selectedTenantQuery.data
  const selectedQuota = useMemo(
    () =>
      selectedTenant?.quotas?.find(
        (quota) => quota.quotaType === quotaDraft.quotaType,
      ),
    [quotaDraft.quotaType, selectedTenant?.quotas],
  )

  const invalidateTenants = async (): Promise<void> => {
    await queryClient.invalidateQueries({ queryKey: ['infra', 'tenants'] })
  }

  const createMutation = useMutation({
    mutationFn: () => tenantService.create(draft),
    onSuccess: async (tenant) => {
      setDraft(emptyTenant)
      setSelectedTenantId(tenant.id)
      await invalidateTenants()
    },
  })
  const updateMutation = useMutation({
    mutationFn: () => tenantService.update(selectedTenant?.id ?? '', draft),
    onSuccess: invalidateTenants,
  })
  const activateMutation = useMutation({
    mutationFn: (tenantId: string) => tenantService.activate(tenantId),
    onSuccess: invalidateTenants,
  })
  const initializeMutation = useMutation({
    mutationFn: (tenantId: string) => tenantService.initialize(tenantId),
    onSuccess: invalidateTenants,
  })
  const disableMutation = useMutation({
    mutationFn: (tenantId: string) => tenantService.disable(tenantId),
    onSuccess: invalidateTenants,
  })
  const quotaMutation = useMutation({
    mutationFn: () =>
      tenantService.updateQuota(
        selectedTenant?.id ?? '',
        quotaDraft.quotaType,
        {
          limitValue: quotaDraft.limitValue,
          warningThreshold: quotaDraft.warningThreshold,
        },
      ),
    onSuccess: invalidateTenants,
  })
  const consumeQuotaMutation = useMutation({
    mutationFn: (quota: TenantQuota) =>
      tenantService.consumeQuota(selectedTenant?.id ?? '', quota.quotaType, 1),
    onSuccess: invalidateTenants,
  })

  function selectTenant(tenant: TenantProfile): void {
    setSelectedTenantId(tenant.id)
    setDraft({
      ...tenant,
      tenantCode: tenant.tenantCode ?? tenant.domain,
      defaultTimezone: tenant.defaultTimezone ?? tenant.timezone,
    })
  }

  return (
    <InfraPageSection
      description="Tenant profile, lifecycle, package, quotas, locale, timezone, and administrator metadata."
      title="Tenant Management"
    >
      <div className="space-y-5">
        <div className="grid gap-5 xl:grid-cols-[minmax(0,1.4fr)_minmax(320px,0.8fr)]">
          <InfraTable
            columns={[
              { key: 'name', title: 'Tenant', render: (item) => item.name },
              {
                key: 'code',
                title: 'Code',
                render: (item) => item.tenantCode ?? item.domain,
              },
              {
                key: 'package',
                title: 'Package',
                render: (item) => item.packageCode ?? '-',
              },
              {
                key: 'locale',
                title: 'Locale / Timezone',
                render: (item) =>
                  `${item.defaultLocale ?? '-'} / ${
                    item.defaultTimezone ?? item.timezone
                  }`,
              },
              {
                key: 'status',
                title: 'Status',
                render: (item) => (
                  <StatusPill active={item.status === 'enabled'}>
                    {item.status}
                  </StatusPill>
                ),
              },
              {
                key: 'actions',
                title: 'Actions',
                render: (item) => (
                  <div className="flex flex-wrap gap-2">
                    <Button
                      onClick={() => selectTenant(item)}
                      size="sm"
                      variant="outline"
                    >
                      Select
                    </Button>
                    <Button
                      disabled={item.status === 'enabled'}
                      onClick={() => activateMutation.mutate(item.id)}
                      size="sm"
                      variant="outline"
                    >
                      Enable
                    </Button>
                    <Button
                      disabled={item.status !== 'enabled'}
                      onClick={() => disableMutation.mutate(item.id)}
                      size="sm"
                      variant="outline"
                    >
                      Disable
                    </Button>
                  </div>
                ),
              },
            ]}
            getRowKey={(item) => item.id}
            isLoading={tenantsQuery.isLoading}
            items={tenants}
          />

          <section className="space-y-3 rounded-lg border border-slate-200 p-4">
            <h3 className="text-sm font-semibold text-slate-950">
              Tenant Profile
            </h3>
            <Input
              onChange={(event) =>
                setDraft((current) => ({
                  ...current,
                  tenantCode: event.target.value,
                  domain: event.target.value,
                }))
              }
              placeholder="Tenant code"
              value={draft.tenantCode ?? draft.domain}
            />
            <Input
              onChange={(event) =>
                setDraft((current) => ({
                  ...current,
                  name: event.target.value,
                }))
              }
              placeholder="Name"
              value={draft.name}
            />
            <div className="grid grid-cols-2 gap-2">
              <select
                className="h-9 rounded-md border border-slate-200 px-3 text-sm"
                onChange={(event) =>
                  setDraft((current) => ({
                    ...current,
                    isolationMode: event.target
                      .value as TenantProfile['isolationMode'],
                  }))
                }
                value={draft.isolationMode}
              >
                <option value="SHARED_DB">Shared DB</option>
                <option value="DEDICATED_DB">Dedicated DB</option>
              </select>
              <Input
                onChange={(event) =>
                  setDraft((current) => ({
                    ...current,
                    packageCode: event.target.value,
                  }))
                }
                placeholder="Package"
                value={draft.packageCode ?? ''}
              />
            </div>
            <div className="grid grid-cols-2 gap-2">
              <Input
                onChange={(event) =>
                  setDraft((current) => ({
                    ...current,
                    defaultLocale: event.target.value,
                  }))
                }
                placeholder="Locale"
                value={draft.defaultLocale ?? ''}
              />
              <Input
                onChange={(event) =>
                  setDraft((current) => ({
                    ...current,
                    defaultTimezone: event.target.value,
                    timezone: event.target.value,
                  }))
                }
                placeholder="Timezone"
                value={draft.defaultTimezone ?? ''}
              />
            </div>
            <div className="grid grid-cols-2 gap-2">
              <Input
                onChange={(event) =>
                  setDraft((current) => ({
                    ...current,
                    adminAccountId: event.target.value,
                  }))
                }
                placeholder="Admin account UUID"
                value={draft.adminAccountId ?? ''}
              />
              <Input
                onChange={(event) =>
                  setDraft((current) => ({
                    ...current,
                    adminPersonId: event.target.value,
                  }))
                }
                placeholder="Admin person UUID"
                value={draft.adminPersonId ?? ''}
              />
            </div>
            <div className="flex flex-wrap gap-2">
              <Button
                disabled={!draft.name || !(draft.tenantCode ?? draft.domain)}
                onClick={() => createMutation.mutate()}
              >
                Create
              </Button>
              <Button
                disabled={!selectedTenant || updateMutation.isPending}
                onClick={() => updateMutation.mutate()}
                variant="outline"
              >
                Save
              </Button>
              <Button
                disabled={!selectedTenant || selectedTenant.initialized}
                onClick={() =>
                  selectedTenant
                    ? initializeMutation.mutate(selectedTenant.id)
                    : undefined
                }
                variant="outline"
              >
                Initialize
              </Button>
            </div>
          </section>
        </div>

        {selectedTenant ? (
          <section className="space-y-3 rounded-lg border border-slate-200 p-4">
            <div className="flex flex-wrap items-end gap-2">
              <div className="grid gap-1">
                <span className="text-xs font-semibold text-slate-500">
                  Quota
                </span>
                <select
                  className="h-9 rounded-md border border-slate-200 px-3 text-sm"
                  onChange={(event) =>
                    setQuotaDraft((current) => ({
                      ...current,
                      quotaType: event.target.value as TenantQuotaType,
                    }))
                  }
                  value={quotaDraft.quotaType}
                >
                  {quotaTypes.map((quotaType) => (
                    <option key={quotaType} value={quotaType}>
                      {quotaType}
                    </option>
                  ))}
                </select>
              </div>
              <Input
                onChange={(event) =>
                  setQuotaDraft((current) => ({
                    ...current,
                    limitValue: Number(event.target.value),
                  }))
                }
                placeholder="Limit"
                type="number"
                value={quotaDraft.limitValue || selectedQuota?.limitValue || 0}
              />
              <Input
                onChange={(event) =>
                  setQuotaDraft((current) => ({
                    ...current,
                    warningThreshold: Number(event.target.value),
                  }))
                }
                placeholder="Warning"
                type="number"
                value={
                  quotaDraft.warningThreshold ||
                  selectedQuota?.warningThreshold ||
                  0
                }
              />
              <Button onClick={() => quotaMutation.mutate()}>Save Quota</Button>
            </div>

            <InfraTable
              columns={[
                {
                  key: 'type',
                  title: 'Type',
                  render: (item) => item.quotaType,
                },
                {
                  key: 'usage',
                  title: 'Usage',
                  render: (item) => `${item.usedValue} / ${item.limitValue}`,
                },
                {
                  key: 'warning',
                  title: 'Warning',
                  render: (item) => (
                    <StatusPill active={!item.warning}>
                      {item.warning ? 'warning' : 'ok'}
                    </StatusPill>
                  ),
                },
                {
                  key: 'actions',
                  title: 'Actions',
                  render: (item) => (
                    <Button
                      disabled={selectedTenant.status !== 'enabled'}
                      onClick={() => consumeQuotaMutation.mutate(item)}
                      size="sm"
                      variant="outline"
                    >
                      Consume 1
                    </Button>
                  ),
                },
              ]}
              getRowKey={(item) => item.id}
              isLoading={selectedTenantQuery.isLoading}
              items={selectedTenant.quotas ?? []}
            />
          </section>
        ) : null}
      </div>
    </InfraPageSection>
  )
}
