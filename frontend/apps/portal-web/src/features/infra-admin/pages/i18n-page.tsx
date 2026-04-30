import { useMemo, useState, type FormEvent, type ReactElement } from 'react'
import { CheckCircle2, Plus, Save, ShieldAlert } from 'lucide-react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { InfraPageSection } from '@/features/infra-admin/components/infra-page-section'
import {
  InfraTable,
  StatusPill,
} from '@/features/infra-admin/components/infra-table'
import { useI18nResources } from '@/features/infra-admin/hooks/use-i18n'
import { i18nService } from '@/features/infra-admin/services/i18n-service'
import type { I18nBundle } from '@/features/infra-admin/types/infra'
import { getUserLocale, setUserLocale } from '@/utils/format-time'

const LOCALES = ['zh-CN', 'en-US', 'ja-JP'] as const

interface BundleFormState {
  bundleCode: string
  moduleCode: string
  locale: string
  fallbackLocale: string
  tenantId: string
}

interface EntryFormState {
  resourceKey: string
  resourceValue: string
}

function emptyBundleForm(locale = getUserLocale()): BundleFormState {
  return {
    bundleCode: 'portal.messages',
    moduleCode: 'portal',
    locale,
    fallbackLocale: locale === 'en-US' ? '' : 'en-US',
    tenantId: '',
  }
}

function entryCount(bundle: I18nBundle): string {
  return `${bundle.entries.filter((entry) => entry.active).length}`
}

export default function I18nPage(): ReactElement {
  const queryClient = useQueryClient()
  const [activeLocale, setActiveLocale] = useState(getUserLocale())
  const [moduleCode, setModuleCode] = useState('')
  const [keyword, setKeyword] = useState('')
  const [bundleForm, setBundleForm] = useState<BundleFormState>(() =>
    emptyBundleForm(activeLocale),
  )
  const [entryForm, setEntryForm] = useState<EntryFormState>({
    resourceKey: '',
    resourceValue: '',
  })
  const [selectedBundleId, setSelectedBundleId] = useState<string | null>(null)
  const [errorMessage, setErrorMessage] = useState<string | null>(null)

  const query = useI18nResources({
    page: 1,
    size: 50,
    locale: activeLocale,
    moduleCode: moduleCode || undefined,
    keyword: keyword || undefined,
  })
  const bundles = useMemo(() => query.data?.items ?? [], [query.data?.items])
  const selectedBundle = useMemo(
    () =>
      bundles.find((bundle) => bundle.id === selectedBundleId) ?? bundles[0],
    [bundles, selectedBundleId],
  )

  const invalidate = async (): Promise<void> => {
    await queryClient.invalidateQueries({ queryKey: ['infra', 'i18n'] })
  }

  const createBundle = useMutation({
    mutationFn: () =>
      i18nService.create({
        bundleCode: bundleForm.bundleCode,
        moduleCode: bundleForm.moduleCode,
        locale: bundleForm.locale,
        fallbackLocale: bundleForm.fallbackLocale || null,
        tenantId: bundleForm.tenantId || null,
      }),
    onSuccess: async (bundle) => {
      setSelectedBundleId(bundle.id)
      setErrorMessage(null)
      await invalidate()
    },
    onError: (error) =>
      setErrorMessage(
        error instanceof Error ? error.message : 'Failed to create bundle',
      ),
  })
  const activateBundle = useMutation({
    mutationFn: (bundleId: string) => i18nService.activate(bundleId),
    onSuccess: invalidate,
    onError: (error) =>
      setErrorMessage(
        error instanceof Error ? error.message : 'Failed to activate bundle',
      ),
  })
  const deprecateBundle = useMutation({
    mutationFn: (bundleId: string) => i18nService.deprecate(bundleId),
    onSuccess: invalidate,
    onError: (error) =>
      setErrorMessage(
        error instanceof Error ? error.message : 'Failed to deprecate bundle',
      ),
  })
  const saveEntry = useMutation({
    mutationFn: async () => {
      if (!selectedBundle) {
        throw new Error('Select a bundle first')
      }

      const existing = selectedBundle.entries.find(
        (entry) => entry.resourceKey === entryForm.resourceKey,
      )

      return existing
        ? i18nService.updateEntry(selectedBundle.id, entryForm)
        : i18nService.addEntry(selectedBundle.id, entryForm)
    },
    onSuccess: async () => {
      setEntryForm({ resourceKey: '', resourceValue: '' })
      setErrorMessage(null)
      await invalidate()
    },
    onError: (error) =>
      setErrorMessage(
        error instanceof Error ? error.message : 'Failed to save entry',
      ),
  })

  const handleLocaleChange = (locale: string): void => {
    setActiveLocale(locale)
    setUserLocale(locale)
    setBundleForm((current) => ({
      ...current,
      locale,
      fallbackLocale: locale === 'en-US' ? '' : 'en-US',
    }))
  }

  const handleCreate = (event: FormEvent<HTMLFormElement>): void => {
    event.preventDefault()
    createBundle.mutate()
  }

  const handleSaveEntry = (event: FormEvent<HTMLFormElement>): void => {
    event.preventDefault()
    saveEntry.mutate()
  }

  return (
    <InfraPageSection
      actions={
        <div className="flex flex-wrap items-center gap-2">
          {LOCALES.map((locale) => (
            <Button
              aria-pressed={activeLocale === locale}
              key={locale}
              onClick={() => handleLocaleChange(locale)}
              size="sm"
              variant={activeLocale === locale ? 'default' : 'outline'}
            >
              {locale}
            </Button>
          ))}
        </div>
      }
      description="Manage locale bundles, fallback chains and resource entries backed by the infra i18n API."
      title="I18n messages"
    >
      <div className="space-y-4">
        {errorMessage ? (
          <div className="flex items-center gap-2 rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">
            <ShieldAlert className="h-4 w-4" />
            {errorMessage}
          </div>
        ) : null}

        <form
          className="grid gap-3 rounded-lg border border-slate-200 p-3 md:grid-cols-6"
          onSubmit={handleCreate}
        >
          <Input
            aria-label="Bundle code"
            onChange={(event) =>
              setBundleForm((current) => ({
                ...current,
                bundleCode: event.target.value,
              }))
            }
            placeholder="bundle code"
            value={bundleForm.bundleCode}
          />
          <Input
            aria-label="Module code"
            onChange={(event) =>
              setBundleForm((current) => ({
                ...current,
                moduleCode: event.target.value,
              }))
            }
            placeholder="module"
            value={bundleForm.moduleCode}
          />
          <Input
            aria-label="Locale"
            onChange={(event) =>
              setBundleForm((current) => ({
                ...current,
                locale: event.target.value,
              }))
            }
            placeholder="locale"
            value={bundleForm.locale}
          />
          <Input
            aria-label="Fallback locale"
            onChange={(event) =>
              setBundleForm((current) => ({
                ...current,
                fallbackLocale: event.target.value,
              }))
            }
            placeholder="fallback"
            value={bundleForm.fallbackLocale}
          />
          <Input
            aria-label="Tenant ID"
            onChange={(event) =>
              setBundleForm((current) => ({
                ...current,
                tenantId: event.target.value,
              }))
            }
            placeholder="tenant optional"
            value={bundleForm.tenantId}
          />
          <Button disabled={createBundle.isPending} type="submit">
            <Plus className="h-4 w-4" />
            Create
          </Button>
        </form>

        <div className="grid gap-3 md:grid-cols-3">
          <Input
            aria-label="Filter module"
            onChange={(event) => setModuleCode(event.target.value)}
            placeholder="module filter"
            value={moduleCode}
          />
          <Input
            aria-label="Search bundles"
            onChange={(event) => setKeyword(event.target.value)}
            placeholder="search key or value"
            value={keyword}
          />
          <div className="flex items-center justify-end text-sm text-slate-500">
            <Badge variant="secondary">{activeLocale}</Badge>
          </div>
        </div>

        <InfraTable
          columns={[
            {
              key: 'bundleCode',
              title: 'Bundle',
              render: (item) => (
                <button
                  className="font-semibold text-sky-700"
                  onClick={() => setSelectedBundleId(item.id)}
                  type="button"
                >
                  {item.bundleCode}
                </button>
              ),
            },
            {
              key: 'moduleCode',
              title: 'Module',
              render: (item) => item.moduleCode,
            },
            { key: 'locale', title: 'Locale', render: (item) => item.locale },
            {
              key: 'status',
              title: 'Status',
              render: (item) => (
                <StatusPill active={item.status === 'ACTIVE'}>
                  {item.status}
                </StatusPill>
              ),
            },
            {
              key: 'fallbackLocale',
              title: 'Fallback',
              render: (item) => item.fallbackLocale ?? '-',
            },
            { key: 'entries', title: 'Entries', render: entryCount },
            {
              key: 'actions',
              title: 'Actions',
              render: (item) => (
                <div className="flex flex-wrap gap-2">
                  <Button
                    disabled={
                      item.status === 'ACTIVE' || activateBundle.isPending
                    }
                    onClick={() => activateBundle.mutate(item.id)}
                    size="sm"
                    variant="outline"
                  >
                    <CheckCircle2 className="h-4 w-4" />
                    Active
                  </Button>
                  <Button
                    disabled={
                      item.status === 'DEPRECATED' || deprecateBundle.isPending
                    }
                    onClick={() => deprecateBundle.mutate(item.id)}
                    size="sm"
                    variant="ghost"
                  >
                    Deprecate
                  </Button>
                </div>
              ),
            },
          ]}
          getRowKey={(item) => item.id}
          isLoading={query.isLoading}
          items={bundles}
        />

        {selectedBundle ? (
          <section className="space-y-3 rounded-lg border border-slate-200 p-3">
            <div className="flex flex-wrap items-center justify-between gap-2">
              <div>
                <p className="text-sm font-semibold text-slate-950">
                  {selectedBundle.bundleCode} / {selectedBundle.locale}
                </p>
                <p className="text-xs text-slate-500">
                  {selectedBundle.tenantId ?? 'global'} fallback{' '}
                  {selectedBundle.fallbackLocale ?? '-'}
                </p>
              </div>
              <StatusPill active={selectedBundle.status === 'ACTIVE'}>
                {selectedBundle.status}
              </StatusPill>
            </div>
            <form
              className="grid gap-3 md:grid-cols-[220px_1fr_auto]"
              onSubmit={handleSaveEntry}
            >
              <Input
                aria-label="Resource key"
                onChange={(event) =>
                  setEntryForm((current) => ({
                    ...current,
                    resourceKey: event.target.value,
                  }))
                }
                placeholder="resource key"
                value={entryForm.resourceKey}
              />
              <Input
                aria-label="Resource value"
                onChange={(event) =>
                  setEntryForm((current) => ({
                    ...current,
                    resourceValue: event.target.value,
                  }))
                }
                placeholder="resource value"
                value={entryForm.resourceValue}
              />
              <Button
                disabled={!entryForm.resourceKey || saveEntry.isPending}
                type="submit"
              >
                <Save className="h-4 w-4" />
                Save entry
              </Button>
            </form>
            <InfraTable
              columns={[
                {
                  key: 'resourceKey',
                  title: 'Key',
                  render: (item) => item.resourceKey,
                },
                {
                  key: 'resourceValue',
                  title: 'Value',
                  render: (item) => (
                    <button
                      className="text-left text-slate-700"
                      onClick={() =>
                        setEntryForm({
                          resourceKey: item.resourceKey,
                          resourceValue: item.resourceValue,
                        })
                      }
                      type="button"
                    >
                      {item.resourceValue}
                    </button>
                  ),
                },
                {
                  key: 'version',
                  title: 'Version',
                  render: (item) => item.version,
                },
                {
                  key: 'active',
                  title: 'Active',
                  render: (item) => (
                    <StatusPill active={item.active}>
                      {item.active ? 'yes' : 'no'}
                    </StatusPill>
                  ),
                },
              ]}
              getRowKey={(item) => item.id}
              items={selectedBundle.entries}
            />
          </section>
        ) : null}
      </div>
    </InfraPageSection>
  )
}
