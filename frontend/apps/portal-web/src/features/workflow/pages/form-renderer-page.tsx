import { useMemo, useState, type ReactElement } from 'react'
import { useMutation, useQuery } from '@tanstack/react-query'
import { FileCheck, Save, Send } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import {
  formService,
  toMetadataSnapshot,
} from '@/features/workflow/services/form-service'
import type {
  FormValidationResult,
  FormRenderSchema,
  FormSubmission,
  RenderedField,
  RenderedForm,
  ValidationError,
} from '@/features/workflow/types/form'

function normalizeValue(field: RenderedField, value: string): unknown {
  if (field.multiValue) {
    return value
      .split(',')
      .map((item) => item.trim())
      .filter(Boolean)
  }

  if (field.fieldType === 'NUMBER') {
    return value === '' ? undefined : Number(value)
  }

  return value
}

function FieldInput({
  field,
  value,
  onChange,
}: {
  field: RenderedField
  value: unknown
  onChange: (value: string) => void
}): ReactElement | null {
  if (!field.visible) {
    return null
  }

  const stringValue = Array.isArray(value)
    ? value.join(',')
    : String(value ?? '')
  const inputType =
    field.fieldType === 'NUMBER'
      ? 'number'
      : field.fieldType === 'DATE'
        ? 'date'
        : field.fieldType === 'DATETIME'
          ? 'datetime-local'
          : 'text'

  return (
    <label className="block">
      <span className="mb-1 block text-sm font-medium text-slate-700">
        {field.displayName}
        {field.required ? <span className="text-rose-500"> *</span> : null}
      </span>
      <Input
        disabled={!field.editable}
        max={typeof field.max === 'number' ? field.max : undefined}
        min={typeof field.min === 'number' ? field.min : undefined}
        onChange={(event) => onChange(event.target.value)}
        placeholder={
          field.multiValue ? 'comma separated values' : field.fieldCode
        }
        type={inputType}
        value={stringValue}
      />
    </label>
  )
}

function buildInitialData(rendered?: RenderedForm): Record<string, unknown> {
  if (!rendered) {
    return {}
  }

  return Object.fromEntries(
    rendered.fields.map((field) => [field.fieldCode, field.value ?? '']),
  )
}

function hasValue(value: unknown): boolean {
  if (Array.isArray(value)) {
    return value.length > 0
  }

  if (typeof value === 'string') {
    return value.trim().length > 0
  }

  return value !== undefined && value !== null && value !== ''
}

function validateRenderedField(
  field: RenderedField,
  value: unknown,
): ValidationError[] {
  if (!field.visible) {
    return []
  }

  const errors: ValidationError[] = []
  const fieldValue = value

  if (field.required && !hasValue(fieldValue)) {
    errors.push({
      fieldCode: field.fieldCode,
      message: `${field.displayName} is required`,
    })
  }

  if (!hasValue(fieldValue)) {
    return errors
  }

  if (field.fieldType === 'NUMBER') {
    const numeric = Number(fieldValue)

    if (!Number.isFinite(numeric)) {
      errors.push({
        fieldCode: field.fieldCode,
        message: `${field.displayName} must be a number`,
      })
      return errors
    }

    if (typeof field.min === 'number' && numeric < field.min) {
      errors.push({
        fieldCode: field.fieldCode,
        message: `${field.displayName} must be at least ${field.min}`,
      })
    }

    if (typeof field.max === 'number' && numeric > field.max) {
      errors.push({
        fieldCode: field.fieldCode,
        message: `${field.displayName} must be at most ${field.max}`,
      })
    }
  }

  if (typeof field.maxLength === 'number') {
    const textValue = Array.isArray(fieldValue)
      ? fieldValue.join(',')
      : String(fieldValue)

    if (textValue.length > field.maxLength) {
      errors.push({
        fieldCode: field.fieldCode,
        message: `${field.displayName} exceeds ${field.maxLength} characters`,
      })
    }
  }

  if (field.pattern) {
    try {
      const pattern = new RegExp(field.pattern)

      if (!pattern.test(String(fieldValue))) {
        errors.push({
          fieldCode: field.fieldCode,
          message: `${field.displayName} format is invalid`,
        })
      }
    } catch {
      errors.push({
        fieldCode: field.fieldCode,
        message: `${field.displayName} pattern is invalid`,
      })
    }
  }

  return errors
}

function validateCurrentFormData(
  rendered: RenderedForm | undefined,
  formData: Record<string, unknown>,
): FormValidationResult {
  if (!rendered) {
    return { valid: true, errors: [] }
  }

  const errors = rendered.fields.flatMap((field) =>
    validateRenderedField(field, formData[field.fieldCode]),
  )

  return {
    valid: errors.length === 0,
    errors,
  }
}

export default function FormRendererPage(): ReactElement {
  const [tenantId, setTenantId] = useState(
    '11111111-1111-1111-1111-111111111111',
  )
  const [code, setCode] = useState('expense.request')
  const [nodeId, setNodeId] = useState('start')
  const [submittedBy, setSubmittedBy] = useState(
    '22222222-2222-2222-2222-222222222222',
  )
  const [formData, setFormData] = useState<Record<string, unknown>>({})
  const [lastSubmission, setLastSubmission] = useState<FormSubmission | null>(
    null,
  )

  const schemaQuery = useQuery({
    enabled: Boolean(code && tenantId),
    queryKey: ['form-render-schema', code, tenantId],
    queryFn: () => formService.getLatestRenderSchema(code, tenantId),
  })

  const snapshot = useMemo(() => {
    const schema = schemaQuery.data as FormRenderSchema | undefined

    return schema ? toMetadataSnapshot(schema) : null
  }, [schemaQuery.data])

  const renderMutation = useMutation({
    mutationFn: () =>
      formService.render({
        metadataSnapshot: snapshot!,
        nodeId,
        locale: navigator.language || 'zh-CN',
        fallbackLocale: 'zh-CN',
        formData,
        validateData: true,
      }),
    onSuccess: (rendered) => setFormData(buildInitialData(rendered)),
  })

  const draftMutation = useMutation({
    mutationFn: () =>
      formService.createDraft({
        metadataSnapshot: snapshot!,
        nodeId,
        formData,
        submittedBy,
      }),
    onSuccess: setLastSubmission,
  })

  const submitMutation = useMutation({
    mutationFn: () =>
      formService.submitDraft(lastSubmission?.submissionId ?? '', {
        metadataSnapshot: snapshot!,
        formData,
      }),
    onSuccess: setLastSubmission,
  })

  const rendered = renderMutation.data
  const liveValidation = useMemo(
    () => validateCurrentFormData(rendered, formData),
    [formData, rendered],
  )

  return (
    <div className="space-y-4">
      <section className="rounded-lg border border-slate-200 bg-white shadow-sm">
        <div className="border-b border-slate-100 px-4 py-3">
          <h1 className="text-base font-semibold text-slate-950">
            Form renderer
          </h1>
          <p className="mt-1 text-sm text-slate-500">
            Load published schema, render fields, save a draft and submit it.
          </p>
        </div>
        <div className="grid gap-3 p-4 lg:grid-cols-[1fr_1fr_12rem_1fr_auto]">
          <Input
            onChange={(event) => setTenantId(event.target.value)}
            value={tenantId}
          />
          <Input
            onChange={(event) => setCode(event.target.value)}
            value={code}
          />
          <Input
            onChange={(event) => setNodeId(event.target.value)}
            value={nodeId}
          />
          <Input
            onChange={(event) => setSubmittedBy(event.target.value)}
            value={submittedBy}
          />
          <Button
            disabled={!snapshot || renderMutation.isPending}
            onClick={() => renderMutation.mutate()}
          >
            <FileCheck className="h-4 w-4" />
            Render
          </Button>
        </div>
      </section>

      {rendered ? (
        <section className="rounded-lg border border-slate-200 bg-white shadow-sm">
          <div className="flex flex-col gap-2 border-b border-slate-100 px-4 py-3 lg:flex-row lg:items-center lg:justify-between">
            <div>
              <h2 className="font-semibold text-slate-950">
                {rendered.displayName} v{rendered.version}
              </h2>
              <p className="text-sm text-slate-500">
                {liveValidation.valid
                  ? 'Validation passed'
                  : `${liveValidation.errors.length} validation errors`}
              </p>
            </div>
            <div className="flex gap-2">
              <Button
                disabled={!snapshot || draftMutation.isPending}
                onClick={() => draftMutation.mutate()}
                variant="outline"
              >
                <Save className="h-4 w-4" />
                Draft
              </Button>
              <Button
                disabled={
                  !snapshot ||
                  !liveValidation.valid ||
                  !lastSubmission ||
                  submitMutation.isPending ||
                  lastSubmission.status === 'SUBMITTED'
                }
                onClick={() => submitMutation.mutate()}
              >
                <Send className="h-4 w-4" />
                Submit
              </Button>
            </div>
          </div>
          <div className="grid gap-4 p-4 lg:grid-cols-2">
            {rendered.fields.map((field) => (
              <FieldInput
                field={field}
                key={field.fieldCode}
                onChange={(nextValue) =>
                  setFormData((value) => ({
                    ...value,
                    [field.fieldCode]: normalizeValue(field, nextValue),
                  }))
                }
                value={formData[field.fieldCode]}
              />
            ))}
          </div>
        </section>
      ) : null}

      {lastSubmission ? (
        <section className="rounded-lg border border-slate-200 bg-white p-4 text-sm shadow-sm">
          <div className="font-semibold text-slate-950">
            {lastSubmission.status} / {lastSubmission.submissionId}
          </div>
          <p className="mt-1 text-slate-500">
            Attachments: {lastSubmission.attachmentIds.join(', ') || '-'}
          </p>
        </section>
      ) : null}

      {schemaQuery.error ||
      renderMutation.error ||
      draftMutation.error ||
      submitMutation.error ? (
        <p className="rounded-lg border border-rose-200 bg-rose-50 px-3 py-2 text-sm text-rose-700">
          {String(
            schemaQuery.error ??
              renderMutation.error ??
              draftMutation.error ??
              submitMutation.error,
          )}
        </p>
      ) : null}
    </div>
  )
}
