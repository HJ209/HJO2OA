import { useState, type FormEvent, type ReactElement } from 'react'
import { Ban, FileCode2, Save, ShieldAlert } from 'lucide-react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { InfraPageSection } from '@/features/infra-admin/components/infra-page-section'
import {
  InfraTable,
  StatusPill,
} from '@/features/infra-admin/components/infra-table'
import { useErrorCodes } from '@/features/infra-admin/hooks/use-error-code'
import {
  errorCodeService,
  type ErrorCodeSaveRequest,
} from '@/features/infra-admin/services/error-code-service'
import type {
  ErrorCodeDefinition,
  ErrorSeverity,
} from '@/features/infra-admin/types/infra'
import { getUserLocale, setUserLocale } from '@/utils/format-time'

const SEVERITIES: ErrorSeverity[] = ['INFO', 'WARN', 'ERROR', 'FATAL']
const LOCALES = ['zh-CN', 'en-US'] as const

function defaultForm(): ErrorCodeSaveRequest {
  return {
    code: 'INFRA_SAMPLE_ERROR',
    moduleCode: 'infra',
    category: 'i18n',
    severity: 'WARN',
    httpStatus: 400,
    messageKey: 'infra.sample.error',
    retryable: false,
  }
}

export default function ErrorCodePage(): ReactElement {
  const queryClient = useQueryClient()
  const [locale, setLocale] = useState(getUserLocale())
  const [moduleCode, setModuleCode] = useState('')
  const [severity, setSeverity] = useState<ErrorSeverity | ''>('')
  const [keyword, setKeyword] = useState('')
  const [editingId, setEditingId] = useState<string | null>(null)
  const [form, setForm] = useState<ErrorCodeSaveRequest>(defaultForm)
  const [errorMessage, setErrorMessage] = useState<string | null>(null)

  const query = useErrorCodes({
    page: 1,
    size: 50,
    moduleCode: moduleCode || undefined,
    severity: severity || undefined,
    keyword: keyword || undefined,
  })

  const invalidate = async (): Promise<void> => {
    await queryClient.invalidateQueries({ queryKey: ['infra', 'error-codes'] })
  }

  const saveDefinition = useMutation({
    mutationFn: () =>
      editingId
        ? errorCodeService.update(editingId, {
            category: form.category || null,
            severity: form.severity,
            httpStatus: form.httpStatus,
            messageKey: form.messageKey,
            retryable: form.retryable,
          })
        : errorCodeService.create(form),
    onSuccess: async () => {
      setErrorMessage(null)
      setEditingId(null)
      await invalidate()
    },
    onError: (error) =>
      setErrorMessage(
        error instanceof Error ? error.message : 'Failed to save error code',
      ),
  })

  const deprecateDefinition = useMutation({
    mutationFn: (id: string) => errorCodeService.deprecate(id),
    onSuccess: invalidate,
    onError: (error) =>
      setErrorMessage(
        error instanceof Error
          ? error.message
          : 'Failed to deprecate error code',
      ),
  })

  const handleLocaleChange = async (nextLocale: string): Promise<void> => {
    setLocale(nextLocale)
    setUserLocale(nextLocale)
    await invalidate()
  }

  const handleSubmit = (event: FormEvent<HTMLFormElement>): void => {
    event.preventDefault()
    saveDefinition.mutate()
  }

  const editDefinition = (definition: ErrorCodeDefinition): void => {
    setEditingId(definition.id)
    setForm({
      code: definition.code,
      moduleCode: definition.moduleCode,
      category: definition.category ?? '',
      severity: definition.severity,
      httpStatus: definition.httpStatus,
      messageKey: definition.messageKey,
      retryable: definition.retryable,
    })
  }

  return (
    <InfraPageSection
      actions={
        <div className="flex flex-wrap gap-2">
          {LOCALES.map((item) => (
            <Button
              aria-pressed={locale === item}
              key={item}
              onClick={() => void handleLocaleChange(item)}
              size="sm"
              variant={locale === item ? 'default' : 'outline'}
            >
              {item}
            </Button>
          ))}
        </div>
      }
      description="Register module-owned error codes and consume localized backend messages through Accept-Language."
      title="Error codes"
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
          onSubmit={handleSubmit}
        >
          <div className="flex items-center gap-2 text-sm font-semibold text-slate-900">
            <FileCode2 className="h-4 w-4" />
            {editingId ? 'Edit definition' : 'Create definition'}
          </div>
          <div className="grid gap-3 md:grid-cols-4">
            <Input
              aria-label="Error code"
              disabled={Boolean(editingId)}
              onChange={(event) =>
                setForm((current) => ({ ...current, code: event.target.value }))
              }
              placeholder="code"
              value={form.code}
            />
            <Input
              aria-label="Module code"
              disabled={Boolean(editingId)}
              onChange={(event) =>
                setForm((current) => ({
                  ...current,
                  moduleCode: event.target.value,
                }))
              }
              placeholder="module"
              value={form.moduleCode}
            />
            <Input
              aria-label="Category"
              onChange={(event) =>
                setForm((current) => ({
                  ...current,
                  category: event.target.value,
                }))
              }
              placeholder="category"
              value={form.category ?? ''}
            />
            <Input
              aria-label="HTTP status"
              onChange={(event) =>
                setForm((current) => ({
                  ...current,
                  httpStatus: Number(event.target.value),
                }))
              }
              placeholder="HTTP"
              type="number"
              value={form.httpStatus}
            />
          </div>
          <div className="grid gap-3 md:grid-cols-[160px_1fr_140px_140px]">
            <select
              aria-label="Severity"
              className="h-10 rounded-xl border border-slate-200 bg-white px-3 text-sm text-slate-900"
              onChange={(event) =>
                setForm((current) => ({
                  ...current,
                  severity: event.target.value as ErrorSeverity,
                }))
              }
              value={form.severity}
            >
              {SEVERITIES.map((item) => (
                <option key={item} value={item}>
                  {item}
                </option>
              ))}
            </select>
            <Input
              aria-label="Message key"
              onChange={(event) =>
                setForm((current) => ({
                  ...current,
                  messageKey: event.target.value,
                }))
              }
              placeholder="message key"
              value={form.messageKey}
            />
            <label className="inline-flex h-10 items-center gap-2 rounded-xl border border-slate-200 px-3 text-sm text-slate-700">
              <input
                checked={form.retryable}
                onChange={(event) =>
                  setForm((current) => ({
                    ...current,
                    retryable: event.target.checked,
                  }))
                }
                type="checkbox"
              />
              Retryable
            </label>
            <Button disabled={saveDefinition.isPending} type="submit">
              <Save className="h-4 w-4" />
              Save
            </Button>
          </div>
        </form>

        <div className="grid gap-3 md:grid-cols-4">
          <Input
            aria-label="Filter module"
            onChange={(event) => setModuleCode(event.target.value)}
            placeholder="module filter"
            value={moduleCode}
          />
          <select
            aria-label="Filter severity"
            className="h-10 rounded-xl border border-slate-200 bg-white px-3 text-sm text-slate-900"
            onChange={(event) =>
              setSeverity(event.target.value as ErrorSeverity | '')
            }
            value={severity}
          >
            <option value="">All severity</option>
            {SEVERITIES.map((item) => (
              <option key={item} value={item}>
                {item}
              </option>
            ))}
          </select>
          <Input
            aria-label="Search error codes"
            onChange={(event) => setKeyword(event.target.value)}
            placeholder="search code or message"
            value={keyword}
          />
          <div className="flex items-center justify-end">
            <Badge variant="secondary">{locale}</Badge>
          </div>
        </div>

        <InfraTable
          columns={[
            {
              key: 'code',
              title: 'Code',
              render: (item) => (
                <button
                  className="font-semibold text-sky-700"
                  onClick={() => editDefinition(item)}
                  type="button"
                >
                  {item.code}
                </button>
              ),
            },
            {
              key: 'moduleCode',
              title: 'Module',
              render: (item) => item.moduleCode,
            },
            {
              key: 'category',
              title: 'Category',
              render: (item) => item.category ?? '-',
            },
            {
              key: 'severity',
              title: 'Severity',
              render: (item) => item.severity,
            },
            {
              key: 'httpStatus',
              title: 'HTTP',
              render: (item) => item.httpStatus,
            },
            {
              key: 'messageKey',
              title: 'Message key',
              render: (item) => item.messageKey,
            },
            {
              key: 'message',
              title: 'Localized message',
              render: (item) => item.message,
            },
            {
              key: 'flags',
              title: 'Flags',
              render: (item) => (
                <div className="flex flex-wrap gap-1">
                  <StatusPill active={item.retryable}>
                    {item.retryable ? 'retry' : 'no retry'}
                  </StatusPill>
                  <StatusPill active={!item.deprecated}>
                    {item.deprecated ? 'deprecated' : 'active'}
                  </StatusPill>
                </div>
              ),
            },
            {
              key: 'actions',
              title: 'Actions',
              render: (item) => (
                <Button
                  disabled={item.deprecated || deprecateDefinition.isPending}
                  onClick={() => deprecateDefinition.mutate(item.id)}
                  size="sm"
                  variant="outline"
                >
                  <Ban className="h-4 w-4" />
                  Deprecate
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
