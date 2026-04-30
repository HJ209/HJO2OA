import { useMemo, useState, type ReactElement } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { ConfigEntryDialog } from '@/features/infra-admin/components/config-entry-dialog'
import { InfraPageSection } from '@/features/infra-admin/components/infra-page-section'
import {
  InfraTable,
  StatusPill,
} from '@/features/infra-admin/components/infra-table'
import { useConfigEntries } from '@/features/infra-admin/hooks/use-config'
import { configService } from '@/features/infra-admin/services/config-service'
import type {
  ConfigEntry,
  ConfigOverrideScope,
  ConfigResolutionContext,
  FeatureRuleType,
  ResolvedConfigValue,
} from '@/features/infra-admin/types/infra'

const emptyOverride = {
  scopeType: 'TENANT' as ConfigOverrideScope,
  scopeId: '',
  overrideValue: '',
}

const emptyFeatureRule = {
  ruleType: 'GLOBAL' as FeatureRuleType,
  ruleValue: 'true',
  sortOrder: 0,
}

const emptyResolution: ConfigResolutionContext = {
  key: '',
  tenantId: '',
  orgId: '',
  roleId: '',
  userId: '',
}

export default function ConfigPage(): ReactElement {
  const [editingEntry, setEditingEntry] = useState<ConfigEntry | undefined>()
  const [selectedEntryId, setSelectedEntryId] = useState<string>()
  const [dialogOpen, setDialogOpen] = useState(false)
  const [overrideDraft, setOverrideDraft] = useState(emptyOverride)
  const [featureRuleDraft, setFeatureRuleDraft] = useState(emptyFeatureRule)
  const [resolutionDraft, setResolutionDraft] =
    useState<ConfigResolutionContext>(emptyResolution)
  const [resolvedValue, setResolvedValue] = useState<ResolvedConfigValue>()
  const queryClient = useQueryClient()
  const entriesQuery = useConfigEntries({ page: 1, size: 50 })
  const entries = useMemo(
    () => entriesQuery.data?.items ?? [],
    [entriesQuery.data?.items],
  )
  const selectedEntry = useMemo(
    () => entries.find((entry) => entry.id === selectedEntryId) ?? entries[0],
    [entries, selectedEntryId],
  )

  const invalidateConfig = async (): Promise<void> => {
    await queryClient.invalidateQueries({ queryKey: ['infra', 'config'] })
  }

  const entryMutation = useMutation({
    mutationFn: (entry: ConfigEntry) =>
      editingEntry
        ? configService.update(editingEntry.id ?? editingEntry.key, entry)
        : configService.create(entry),
    onSuccess: async () => {
      setDialogOpen(false)
      await invalidateConfig()
    },
  })
  const overrideMutation = useMutation({
    mutationFn: () =>
      configService.addOverride(selectedEntry?.id ?? '', overrideDraft),
    onSuccess: async () => {
      setOverrideDraft(emptyOverride)
      await invalidateConfig()
    },
  })
  const disableOverrideMutation = useMutation({
    mutationFn: (overrideId: string) =>
      configService.disableOverride(overrideId),
    onSuccess: invalidateConfig,
  })
  const featureRuleMutation = useMutation({
    mutationFn: () =>
      configService.addFeatureRule(selectedEntry?.id ?? '', featureRuleDraft),
    onSuccess: async () => {
      setFeatureRuleDraft(emptyFeatureRule)
      await invalidateConfig()
    },
  })
  const updateFeatureRuleMutation = useMutation({
    mutationFn: (ruleId: string) =>
      configService.updateFeatureRule(ruleId, { active: false }),
    onSuccess: invalidateConfig,
  })
  const resolutionMutation = useMutation({
    mutationFn: () => configService.resolve(resolutionDraft),
    onSuccess: setResolvedValue,
  })

  return (
    <InfraPageSection
      actions={
        <Button
          onClick={() => {
            setEditingEntry(undefined)
            setDialogOpen(true)
          }}
        >
          New Config
        </Button>
      }
      description="Runtime config, scoped overrides, feature flags, and final value preview."
      title="Config Center"
    >
      <div className="space-y-5">
        <InfraTable
          columns={[
            { key: 'key', title: 'Key', render: (item) => item.key },
            { key: 'type', title: 'Type', render: (item) => item.group },
            { key: 'value', title: 'Default', render: (item) => item.value },
            {
              key: 'status',
              title: 'Status',
              render: (item) => (
                <StatusPill active={item.status !== 'DISABLED'}>
                  {item.status ?? 'ACTIVE'}
                </StatusPill>
              ),
            },
            {
              key: 'scope',
              title: 'Runtime',
              render: (item) => (
                <span className="text-xs text-slate-600">
                  {item.overrides?.filter((rule) => rule.active).length ?? 0}{' '}
                  overrides /{' '}
                  {item.featureRules?.filter((rule) => rule.active).length ?? 0}{' '}
                  rules
                </span>
              ),
            },
            {
              key: 'actions',
              title: 'Actions',
              render: (item) => (
                <div className="flex flex-wrap gap-2">
                  <Button
                    onClick={() => {
                      setSelectedEntryId(item.id)
                      setResolutionDraft((current) => ({
                        ...current,
                        key: item.key,
                      }))
                    }}
                    size="sm"
                    variant="outline"
                  >
                    Select
                  </Button>
                  <Button
                    onClick={() => {
                      setEditingEntry(item)
                      setDialogOpen(true)
                    }}
                    size="sm"
                    variant="outline"
                  >
                    Edit
                  </Button>
                </div>
              ),
            },
          ]}
          getRowKey={(item) => item.id ?? item.key}
          isLoading={entriesQuery.isLoading}
          items={entries}
        />

        {selectedEntry ? (
          <div className="grid gap-5 xl:grid-cols-3">
            <section className="space-y-3 rounded-lg border border-slate-200 p-4">
              <h3 className="text-sm font-semibold text-slate-950">
                Overrides
              </h3>
              <div className="grid gap-2">
                <select
                  className="h-9 rounded-md border border-slate-200 px-3 text-sm"
                  onChange={(event) =>
                    setOverrideDraft((current) => ({
                      ...current,
                      scopeType: event.target.value as ConfigOverrideScope,
                    }))
                  }
                  value={overrideDraft.scopeType}
                >
                  <option value="TENANT">Tenant</option>
                  <option value="ORGANIZATION">Organization</option>
                  <option value="ROLE">Role</option>
                  <option value="USER">User</option>
                </select>
                <Input
                  onChange={(event) =>
                    setOverrideDraft((current) => ({
                      ...current,
                      scopeId: event.target.value,
                    }))
                  }
                  placeholder="Scope UUID"
                  value={overrideDraft.scopeId}
                />
                <Input
                  onChange={(event) =>
                    setOverrideDraft((current) => ({
                      ...current,
                      overrideValue: event.target.value,
                    }))
                  }
                  placeholder="Override value"
                  value={overrideDraft.overrideValue}
                />
                <Button
                  disabled={
                    !overrideDraft.scopeId || overrideMutation.isPending
                  }
                  onClick={() => overrideMutation.mutate()}
                >
                  Add Override
                </Button>
              </div>
              <div className="space-y-2">
                {(selectedEntry.overrides ?? []).map((override) => (
                  <div
                    className="flex items-center justify-between rounded-md bg-slate-50 px-3 py-2 text-xs"
                    key={override.id}
                  >
                    <span>
                      {override.scopeType}:{override.scopeId} ={' '}
                      {override.overrideValue}
                    </span>
                    <Button
                      disabled={!override.active}
                      onClick={() =>
                        disableOverrideMutation.mutate(override.id)
                      }
                      size="sm"
                      variant="outline"
                    >
                      Disable
                    </Button>
                  </div>
                ))}
              </div>
            </section>

            <section className="space-y-3 rounded-lg border border-slate-200 p-4">
              <h3 className="text-sm font-semibold text-slate-950">
                Feature Flags
              </h3>
              <div className="grid gap-2">
                <select
                  className="h-9 rounded-md border border-slate-200 px-3 text-sm"
                  onChange={(event) =>
                    setFeatureRuleDraft((current) => ({
                      ...current,
                      ruleType: event.target.value as FeatureRuleType,
                    }))
                  }
                  value={featureRuleDraft.ruleType}
                >
                  <option value="GLOBAL">Global</option>
                  <option value="TENANT">Tenant</option>
                  <option value="ORG">Org</option>
                  <option value="ROLE">Role</option>
                  <option value="USER">User</option>
                  <option value="PERCENTAGE">Percentage</option>
                </select>
                <Input
                  onChange={(event) =>
                    setFeatureRuleDraft((current) => ({
                      ...current,
                      ruleValue: event.target.value,
                    }))
                  }
                  placeholder='Rule value or {"percentage":25,"enabled":true}'
                  value={featureRuleDraft.ruleValue}
                />
                <Input
                  onChange={(event) =>
                    setFeatureRuleDraft((current) => ({
                      ...current,
                      sortOrder: Number(event.target.value),
                    }))
                  }
                  type="number"
                  value={featureRuleDraft.sortOrder}
                />
                <Button
                  disabled={
                    selectedEntry.configType !== 'FEATURE_FLAG' ||
                    featureRuleMutation.isPending
                  }
                  onClick={() => featureRuleMutation.mutate()}
                >
                  Add Rule
                </Button>
              </div>
              <div className="space-y-2">
                {(selectedEntry.featureRules ?? []).map((rule) => (
                  <div
                    className="flex items-center justify-between rounded-md bg-slate-50 px-3 py-2 text-xs"
                    key={rule.id}
                  >
                    <span>
                      #{rule.sortOrder} {rule.ruleType}: {rule.ruleValue}
                    </span>
                    <Button
                      disabled={!rule.active}
                      onClick={() => updateFeatureRuleMutation.mutate(rule.id)}
                      size="sm"
                      variant="outline"
                    >
                      Disable
                    </Button>
                  </div>
                ))}
              </div>
            </section>

            <section className="space-y-3 rounded-lg border border-slate-200 p-4">
              <h3 className="text-sm font-semibold text-slate-950">
                Resolution Preview
              </h3>
              <div className="grid gap-2">
                {(
                  ['key', 'tenantId', 'orgId', 'roleId', 'userId'] as const
                ).map((field) => (
                  <Input
                    key={field}
                    onChange={(event) =>
                      setResolutionDraft((current) => ({
                        ...current,
                        [field]: event.target.value,
                      }))
                    }
                    placeholder={field}
                    value={resolutionDraft[field] ?? ''}
                  />
                ))}
                <Button
                  disabled={
                    !resolutionDraft.key || resolutionMutation.isPending
                  }
                  onClick={() => resolutionMutation.mutate()}
                >
                  Preview
                </Button>
              </div>
              {resolvedValue ? (
                <div className="rounded-md bg-slate-50 p-3 text-xs text-slate-700">
                  <p className="font-semibold">{resolvedValue.resolvedValue}</p>
                  <p>{resolvedValue.sourceType}</p>
                  <p>{resolvedValue.trace.join(' -> ')}</p>
                </div>
              ) : null}
            </section>
          </div>
        ) : null}
      </div>

      <ConfigEntryDialog
        entry={editingEntry}
        isSubmitting={entryMutation.isPending}
        onClose={() => setDialogOpen(false)}
        onSubmit={(entry) => entryMutation.mutate(entry)}
        open={dialogOpen}
      />
    </InfraPageSection>
  )
}
