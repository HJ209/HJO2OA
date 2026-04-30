import { useState, type FormEvent, type ReactElement } from 'react'
import { CheckCircle2, Languages, Save, ShieldAlert } from 'lucide-react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { InfraPageSection } from '@/features/infra-admin/components/infra-page-section'
import {
  InfraTable,
  StatusPill,
} from '@/features/infra-admin/components/infra-table'
import { useDataI18nTranslations } from '@/features/infra-admin/hooks/use-data-i18n'
import {
  dataI18nService,
  type TranslationResolveResult,
} from '@/features/infra-admin/services/data-i18n-service'
import { resolveCurrentTenantId } from '@/features/infra-admin/services/service-utils'
import type { DataI18nTranslation } from '@/features/infra-admin/types/infra'
import { getUserLocale, setUserLocale } from '@/utils/format-time'

const EDIT_LOCALES = ['zh-CN', 'en-US'] as const

type LocaleValues = Record<(typeof EDIT_LOCALES)[number], string>

function emptyValues(): LocaleValues {
  return {
    'zh-CN': '',
    'en-US': '',
  }
}

export default function DataI18nPage(): ReactElement {
  const queryClient = useQueryClient()
  const [activeLocale, setActiveLocale] = useState(getUserLocale())
  const [tenantId, setTenantId] = useState('')
  const [entityType, setEntityType] = useState('article')
  const [entityId, setEntityId] = useState('A-100')
  const [fieldName, setFieldName] = useState('title')
  const [fallbackLocale, setFallbackLocale] = useState('en-US')
  const [originalValue, setOriginalValue] = useState('Original title')
  const [values, setValues] = useState<LocaleValues>(emptyValues)
  const [resolveResult, setResolveResult] =
    useState<TranslationResolveResult | null>(null)
  const [errorMessage, setErrorMessage] = useState<string | null>(null)

  const query = useDataI18nTranslations({
    page: 1,
    size: 50,
    tenantId: tenantId || undefined,
    entityType: entityType || undefined,
    locale: activeLocale || undefined,
  })

  const invalidate = async (): Promise<void> => {
    await queryClient.invalidateQueries({ queryKey: ['infra', 'data-i18n'] })
  }

  const resolveTenant = async (): Promise<string> =>
    tenantId || (await resolveCurrentTenantId())

  const saveTranslations = useMutation({
    mutationFn: async () => {
      const resolvedTenantId = await resolveTenant()
      const entries = EDIT_LOCALES.flatMap((locale) => {
        const value = values[locale].trim()

        return value
          ? [
              {
                entityType,
                entityId,
                fieldName,
                locale,
                value,
                tenantId: resolvedTenantId,
              },
            ]
          : []
      })

      if (entries.length === 0) {
        throw new Error('Enter at least one localized value')
      }

      return dataI18nService.batchSave(entries)
    },
    onSuccess: async () => {
      setErrorMessage(null)
      await invalidate()
    },
    onError: (error) =>
      setErrorMessage(
        error instanceof Error ? error.message : 'Failed to save translations',
      ),
  })

  const reviewTranslation = useMutation({
    mutationFn: (id: string) => dataI18nService.review(id),
    onSuccess: invalidate,
    onError: (error) =>
      setErrorMessage(
        error instanceof Error ? error.message : 'Failed to review translation',
      ),
  })

  const resolveTranslation = useMutation({
    mutationFn: async () =>
      dataI18nService.resolve({
        entityType,
        entityId,
        fieldName,
        locale: activeLocale,
        tenantId: await resolveTenant(),
        fallbackLocale: fallbackLocale || undefined,
        originalValue,
      }),
    onSuccess: (result) => {
      setResolveResult(result)
      setErrorMessage(null)
    },
    onError: (error) =>
      setErrorMessage(
        error instanceof Error
          ? error.message
          : 'Failed to resolve translation',
      ),
  })

  const handleLocaleChange = (locale: string): void => {
    setActiveLocale(locale)
    setUserLocale(locale)
  }

  const handleSave = (event: FormEvent<HTMLFormElement>): void => {
    event.preventDefault()
    saveTranslations.mutate()
  }

  const handleResolve = (event: FormEvent<HTMLFormElement>): void => {
    event.preventDefault()
    resolveTranslation.mutate()
  }

  const loadTranslation = (translation: DataI18nTranslation): void => {
    setTenantId(translation.tenantId)
    setEntityType(translation.entityType)
    setEntityId(translation.entityId)
    setFieldName(translation.fieldName)
    if (translation.locale === 'zh-CN' || translation.locale === 'en-US') {
      setValues((current) => ({
        ...current,
        [translation.locale]: translation.translatedValue,
      }))
    }
  }

  return (
    <InfraPageSection
      actions={
        <div className="flex flex-wrap gap-2">
          {EDIT_LOCALES.map((locale) => (
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
      description="Store tenant-scoped multilingual business fields, query with fallback, and batch-edit locale values."
      title="Data i18n"
    >
      <div className="space-y-4">
        {errorMessage ? (
          <div className="flex items-center gap-2 rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">
            <ShieldAlert className="h-4 w-4" />
            {errorMessage}
          </div>
        ) : null}

        <form
          className="space-y-3 rounded-lg border border-slate-200 p-3"
          onSubmit={handleSave}
        >
          <div className="grid gap-3 md:grid-cols-5">
            <Input
              aria-label="Tenant ID"
              onChange={(event) => setTenantId(event.target.value)}
              placeholder="tenant auto"
              value={tenantId}
            />
            <Input
              aria-label="Entity type"
              onChange={(event) => setEntityType(event.target.value)}
              placeholder="entity type"
              value={entityType}
            />
            <Input
              aria-label="Entity ID"
              onChange={(event) => setEntityId(event.target.value)}
              placeholder="entity id"
              value={entityId}
            />
            <Input
              aria-label="Field name"
              onChange={(event) => setFieldName(event.target.value)}
              placeholder="field"
              value={fieldName}
            />
            <Button disabled={saveTranslations.isPending} type="submit">
              <Save className="h-4 w-4" />
              Save locales
            </Button>
          </div>
          <div className="grid gap-3 md:grid-cols-2">
            {EDIT_LOCALES.map((locale) => (
              <label
                className="space-y-1 text-sm font-medium text-slate-700"
                key={locale}
              >
                <span className="flex items-center gap-2">
                  <Languages className="h-4 w-4" />
                  {locale}
                </span>
                <textarea
                  className="min-h-24 w-full rounded-xl border border-slate-200 px-3 py-2 text-sm outline-none focus:border-sky-400 focus:ring-2 focus:ring-sky-100"
                  onChange={(event) =>
                    setValues((current) => ({
                      ...current,
                      [locale]: event.target.value,
                    }))
                  }
                  value={values[locale]}
                />
              </label>
            ))}
          </div>
        </form>

        <form
          className="grid gap-3 rounded-lg border border-slate-200 p-3 md:grid-cols-[1fr_160px_1fr_auto]"
          onSubmit={handleResolve}
        >
          <Input
            aria-label="Fallback locale"
            onChange={(event) => setFallbackLocale(event.target.value)}
            placeholder="fallback locale"
            value={fallbackLocale}
          />
          <Badge className="justify-center" variant="secondary">
            Request {activeLocale}
          </Badge>
          <Input
            aria-label="Original value"
            onChange={(event) => setOriginalValue(event.target.value)}
            placeholder="original value"
            value={originalValue}
          />
          <Button
            disabled={resolveTranslation.isPending}
            type="submit"
            variant="outline"
          >
            Resolve
          </Button>
        </form>

        {resolveResult ? (
          <div className="rounded-lg border border-sky-200 bg-sky-50 px-3 py-2 text-sm text-sky-800">
            {resolveResult.resolvedValue} · {resolveResult.resolveSource} ·{' '}
            {resolveResult.resolvedLocale ?? 'original'}
          </div>
        ) : null}

        <InfraTable
          columns={[
            {
              key: 'entityType',
              title: 'Entity',
              render: (item) => item.entityType,
            },
            {
              key: 'entityId',
              title: 'Entity ID',
              render: (item) => item.entityId,
            },
            {
              key: 'fieldName',
              title: 'Field',
              render: (item) => item.fieldName,
            },
            { key: 'locale', title: 'Locale', render: (item) => item.locale },
            {
              key: 'translatedValue',
              title: 'Value',
              render: (item) => (
                <button
                  className="text-left"
                  onClick={() => loadTranslation(item)}
                  type="button"
                >
                  {item.translatedValue}
                </button>
              ),
            },
            {
              key: 'status',
              title: 'Status',
              render: (item) => (
                <StatusPill active={item.translationStatus === 'REVIEWED'}>
                  {item.translationStatus}
                </StatusPill>
              ),
            },
            {
              key: 'tenant',
              title: 'Tenant',
              render: (item) => item.tenantId,
            },
            {
              key: 'actions',
              title: 'Actions',
              render: (item) => (
                <Button
                  disabled={
                    item.translationStatus === 'REVIEWED' ||
                    reviewTranslation.isPending
                  }
                  onClick={() => reviewTranslation.mutate(item.id)}
                  size="sm"
                  variant="outline"
                >
                  <CheckCircle2 className="h-4 w-4" />
                  Review
                </Button>
              ),
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
