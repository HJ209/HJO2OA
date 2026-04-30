import { useMemo, useState, type DragEvent, type ReactElement } from 'react'
import { useMutation } from '@tanstack/react-query'
import {
  Eye,
  EyeOff,
  GripVertical,
  LayoutGrid,
  LockKeyhole,
  Plus,
  Rocket,
  Save,
  Settings2,
  Trash2,
  Unlock,
} from 'lucide-react'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { formService } from '@/features/workflow/services/form-service'
import type {
  FieldPermission,
  FormFieldDefinition,
  FormFieldType,
  FormMetadataDetail,
} from '@/features/workflow/types/form'
import { cn } from '@/utils/cn'

const FIELD_TYPE_TRANSFER = 'application/x-hjo2oa-form-field-type'
const FIELD_INDEX_TRANSFER = 'application/x-hjo2oa-form-field-index'

const fieldPalette: Array<{
  type: FormFieldType
  label: string
  description: string
}> = [
  { type: 'TEXT', label: 'Text', description: 'Single line text' },
  { type: 'NUMBER', label: 'Number', description: 'Numeric amount or count' },
  { type: 'DATE', label: 'Date', description: 'Calendar date' },
  { type: 'DATETIME', label: 'Date time', description: 'Timestamp value' },
  { type: 'SELECT', label: 'Select', description: 'Dictionary option' },
  {
    type: 'MULTI_SELECT',
    label: 'Multi select',
    description: 'Multiple dictionary options',
  },
  { type: 'PERSON', label: 'Person', description: 'Organization person' },
  { type: 'ORG', label: 'Organization', description: 'Organization unit' },
  { type: 'DEPT', label: 'Department', description: 'Department selector' },
  { type: 'POSITION', label: 'Position', description: 'Position selector' },
  { type: 'ROLE', label: 'Role', description: 'Role selector' },
  { type: 'ATTACHMENT', label: 'Attachment', description: 'File asset IDs' },
  { type: 'RICH_TEXT', label: 'Rich text', description: 'Long formatted text' },
  { type: 'IMAGE', label: 'Image', description: 'Image asset' },
  { type: 'TABLE', label: 'Table', description: 'Repeated child rows' },
  {
    type: 'REFERENCE',
    label: 'Reference',
    description: 'Business object link',
  },
]

const fieldTypeLabels = fieldPalette.reduce<Record<FormFieldType, string>>(
  (labels, item) => ({
    ...labels,
    [item.type]: item.label,
  }),
  {} as Record<FormFieldType, string>,
)

type PermissionMap = Record<string, Record<string, FieldPermission>>

function parseJson(value: string): unknown {
  if (!value.trim()) {
    return {}
  }

  return JSON.parse(value) as unknown
}

function parsePermissionMap(value: string): PermissionMap {
  const parsed = parseJson(value)

  if (typeof parsed !== 'object' || parsed === null || Array.isArray(parsed)) {
    return {}
  }

  return parsed as PermissionMap
}

function formatJson(value: unknown): string {
  return JSON.stringify(value, null, 2)
}

function normalizeFieldCode(value: string): string {
  const normalized = value
    .trim()
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, '_')
    .replace(/^_+|_+$/g, '')

  return normalized || 'field'
}

function nextFieldCode(
  type: FormFieldType,
  fields: FormFieldDefinition[],
): string {
  const base = normalizeFieldCode(type.replace(/_/g, ' '))
  let index = fields.filter((field) => field.fieldCode.startsWith(base)).length

  do {
    index += 1
  } while (fields.some((field) => field.fieldCode === `${base}${index}`))

  return `${base}${index}`
}

function createFieldFromType(
  type: FormFieldType,
  fields: FormFieldDefinition[],
): FormFieldDefinition {
  const label = fieldTypeLabels[type]
  const baseField: FormFieldDefinition = {
    fieldCode: nextFieldCode(type, fields),
    fieldName: `${label} field`,
    fieldType: type,
    required: false,
    multiValue:
      type === 'MULTI_SELECT' || type === 'ATTACHMENT' || type === 'TABLE',
    visible: true,
    editable: true,
  }

  if (type === 'TEXT' || type === 'RICH_TEXT') {
    return { ...baseField, maxLength: type === 'TEXT' ? 200 : 2000 }
  }

  if (type === 'NUMBER') {
    return { ...baseField, min: 0, max: 10000 }
  }

  if (type === 'SELECT' || type === 'MULTI_SELECT') {
    return { ...baseField, dictionaryCode: 'common.options' }
  }

  if (type === 'TABLE') {
    return { ...baseField, childFields: [] }
  }

  return baseField
}

function moveField(
  fields: FormFieldDefinition[],
  fromIndex: number,
  targetIndex: number,
): FormFieldDefinition[] {
  if (
    fromIndex < 0 ||
    fromIndex >= fields.length ||
    targetIndex < 0 ||
    fromIndex === targetIndex
  ) {
    return fields
  }

  const nextFields = [...fields]
  const [movedField] = nextFields.splice(fromIndex, 1)
  const adjustedIndex = fromIndex < targetIndex ? targetIndex - 1 : targetIndex
  nextFields.splice(Math.min(adjustedIndex, nextFields.length), 0, movedField)

  return nextFields
}

function numericValue(value: string): number | undefined {
  if (!value.trim()) {
    return undefined
  }

  const parsed = Number(value)

  return Number.isFinite(parsed) ? parsed : undefined
}

function FieldDesignerRow({
  field,
  index,
  selected,
  onDragStart,
  onDrop,
  onRemove,
  onSelect,
}: {
  field: FormFieldDefinition
  index: number
  selected: boolean
  onDragStart: (event: DragEvent<HTMLDivElement>, index: number) => void
  onDrop: (event: DragEvent<HTMLDivElement>, index: number) => void
  onRemove: () => void
  onSelect: () => void
}): ReactElement {
  return (
    <div
      className={cn(
        'grid cursor-pointer gap-3 border-b border-slate-100 px-4 py-3 last:border-b-0 md:grid-cols-[2rem_1fr_8rem_8rem_auto]',
        selected && 'bg-sky-50',
      )}
      data-testid={`field-row-${index}`}
      draggable
      onClick={onSelect}
      onDragOver={(event) => event.preventDefault()}
      onDragStart={(event) => onDragStart(event, index)}
      onDrop={(event) => onDrop(event, index)}
    >
      <div className="flex h-10 items-center justify-center text-slate-400">
        <GripVertical className="h-4 w-4" />
      </div>
      <div className="min-w-0">
        <div className="flex flex-wrap items-center gap-2">
          <span className="truncate font-medium text-slate-950">
            {field.fieldName}
          </span>
          {field.required ? <Badge variant="secondary">Required</Badge> : null}
          {field.multiValue ? <Badge variant="secondary">Multi</Badge> : null}
        </div>
        <div className="mt-1 truncate font-mono text-xs text-slate-500">
          {field.fieldCode}
        </div>
      </div>
      <div className="flex items-center text-sm text-slate-600">
        {fieldTypeLabels[field.fieldType]}
      </div>
      <div className="flex items-center gap-2 text-sm text-slate-600">
        {field.visible === false ? (
          <EyeOff className="h-4 w-4 text-slate-400" />
        ) : (
          <Eye className="h-4 w-4 text-slate-400" />
        )}
        {field.editable === false ? (
          <LockKeyhole className="h-4 w-4 text-slate-400" />
        ) : (
          <Unlock className="h-4 w-4 text-slate-400" />
        )}
      </div>
      <Button
        aria-label={`Remove ${field.fieldName}`}
        onClick={(event) => {
          event.stopPropagation()
          onRemove()
        }}
        size="icon"
        type="button"
        variant="ghost"
      >
        <Trash2 className="h-4 w-4" />
      </Button>
    </div>
  )
}

function ToggleField({
  checked,
  label,
  onChange,
}: {
  checked: boolean
  label: string
  onChange: (checked: boolean) => void
}): ReactElement {
  return (
    <label className="flex items-center gap-2 rounded-lg border border-slate-200 px-3 py-2 text-sm text-slate-700">
      <input
        checked={checked}
        onChange={(event) => onChange(event.target.checked)}
        type="checkbox"
      />
      {label}
    </label>
  )
}

export default function FormDesignerPage(): ReactElement {
  const [tenantId, setTenantId] = useState(
    '11111111-1111-1111-1111-111111111111',
  )
  const [code, setCode] = useState('expense.request')
  const [name, setName] = useState('Expense Request')
  const [layoutText, setLayoutText] = useState(
    formatJson({ type: 'vertical', fields: ['amount', 'attachments'] }),
  )
  const [permissionText, setPermissionText] = useState(
    formatJson({
      start: {
        amount: { visible: true, editable: true, required: true },
      },
    }),
  )
  const [permissionNodeId, setPermissionNodeId] = useState('start')
  const [lastMetadata, setLastMetadata] = useState<FormMetadataDetail | null>(
    null,
  )
  const [selectedFieldIndex, setSelectedFieldIndex] = useState(0)
  const [fields, setFields] = useState<FormFieldDefinition[]>([
    {
      fieldCode: 'amount',
      fieldName: 'Amount',
      fieldType: 'NUMBER',
      required: true,
      multiValue: false,
      visible: true,
      editable: true,
      min: 0,
      max: 10000,
    },
    {
      fieldCode: 'attachments',
      fieldName: 'Attachments',
      fieldType: 'ATTACHMENT',
      required: false,
      multiValue: true,
      visible: true,
      editable: true,
    },
  ])

  const selectedField = fields[selectedFieldIndex] ?? null
  const permissionMap = useMemo(
    () => parsePermissionMap(permissionText),
    [permissionText],
  )
  const selectedPermission =
    selectedField && permissionMap[permissionNodeId]?.[selectedField.fieldCode]
      ? permissionMap[permissionNodeId][selectedField.fieldCode]
      : {}

  const createMutation = useMutation({
    mutationFn: () =>
      formService.createMetadata({
        code,
        name,
        tenantId,
        fieldSchema: fields,
        layout: parseJson(layoutText),
        fieldPermissionMap: parseJson(permissionText),
      }),
    onSuccess: setLastMetadata,
  })

  const publishMutation = useMutation({
    mutationFn: () => formService.publishMetadata(lastMetadata?.id ?? ''),
    onSuccess: setLastMetadata,
  })

  const canPublish = useMemo(
    () => Boolean(lastMetadata && lastMetadata.status === 'DRAFT'),
    [lastMetadata],
  )

  function insertField(type: FormFieldType, insertAt = fields.length): void {
    const targetIndex = Math.min(Math.max(insertAt, 0), fields.length)
    const nextField = createFieldFromType(type, fields)

    setFields((items) => {
      const nextItems = [...items]
      nextItems.splice(targetIndex, 0, nextField)
      return nextItems
    })
    setSelectedFieldIndex(targetIndex)
  }

  function updateSelectedField(patch: Partial<FormFieldDefinition>): void {
    if (!selectedField) {
      return
    }

    setFields((items) =>
      items.map((item, index) =>
        index === selectedFieldIndex ? { ...item, ...patch } : item,
      ),
    )
  }

  function removeField(index: number): void {
    setFields((items) => items.filter((_, itemIndex) => itemIndex !== index))
    setSelectedFieldIndex((current) => {
      if (current <= 0) {
        return 0
      }

      return current >= index ? current - 1 : current
    })
  }

  function handlePaletteDragStart(
    event: DragEvent<HTMLButtonElement>,
    type: FormFieldType,
  ): void {
    event.dataTransfer.effectAllowed = 'copy'
    event.dataTransfer.setData(FIELD_TYPE_TRANSFER, type)
    event.dataTransfer.setData('text/plain', type)
  }

  function handleFieldDragStart(
    event: DragEvent<HTMLDivElement>,
    index: number,
  ): void {
    event.dataTransfer.effectAllowed = 'move'
    event.dataTransfer.setData(FIELD_INDEX_TRANSFER, String(index))
    event.dataTransfer.setData('text/plain', String(index))
  }

  function handleCanvasDrop(
    event: DragEvent<HTMLElement>,
    targetIndex = fields.length,
  ): void {
    event.preventDefault()
    const type = event.dataTransfer.getData(FIELD_TYPE_TRANSFER)
    const sourceIndex = event.dataTransfer.getData(FIELD_INDEX_TRANSFER)

    if (type) {
      insertField(type as FormFieldType, targetIndex)
      return
    }

    if (sourceIndex) {
      const parsedIndex = Number(sourceIndex)
      setFields((items) => moveField(items, parsedIndex, targetIndex))
      setSelectedFieldIndex(Math.min(targetIndex, fields.length - 1))
    }
  }

  function syncLayoutFromCanvas(): void {
    setLayoutText(
      formatJson({
        type: 'vertical',
        fields: fields.map((field) => field.fieldCode),
      }),
    )
  }

  function updatePermissionFlag(
    flag: keyof FieldPermission,
    checked: boolean,
  ): void {
    if (!selectedField) {
      return
    }

    setPermissionText((current) => {
      const nextPermissionMap = parsePermissionMap(current)
      const nodePermissions = nextPermissionMap[permissionNodeId] ?? {}
      const fieldPermissions = nodePermissions[selectedField.fieldCode] ?? {}

      return formatJson({
        ...nextPermissionMap,
        [permissionNodeId]: {
          ...nodePermissions,
          [selectedField.fieldCode]: {
            ...fieldPermissions,
            [flag]: checked,
          },
        },
      })
    })
  }

  return (
    <div className="space-y-4">
      <section className="rounded-lg border border-slate-200 bg-white shadow-sm">
        <div className="flex flex-wrap items-center justify-between gap-3 border-b border-slate-100 px-4 py-3">
          <div>
            <h1 className="text-base font-semibold text-slate-950">
              Form designer
            </h1>
            <p className="mt-1 text-sm text-slate-500">
              Build metadata schema, field rules and node permissions.
            </p>
          </div>
          <div className="flex gap-2">
            <Button
              disabled={createMutation.isPending}
              onClick={() => createMutation.mutate()}
            >
              <Save className="h-4 w-4" />
              Save
            </Button>
            <Button
              disabled={!canPublish || publishMutation.isPending}
              onClick={() => publishMutation.mutate()}
              variant="outline"
            >
              <Rocket className="h-4 w-4" />
              Publish
            </Button>
          </div>
        </div>
        <div className="grid gap-3 p-4 lg:grid-cols-3">
          <Input
            aria-label="Tenant ID"
            onChange={(event) => setTenantId(event.target.value)}
            value={tenantId}
          />
          <Input
            aria-label="Form code"
            onChange={(event) => setCode(event.target.value)}
            value={code}
          />
          <Input
            aria-label="Form name"
            onChange={(event) => setName(event.target.value)}
            value={name}
          />
        </div>
      </section>

      <section className="grid gap-4 xl:grid-cols-[18rem_minmax(0,1fr)_22rem]">
        <div className="rounded-lg border border-slate-200 bg-white shadow-sm">
          <div className="border-b border-slate-100 px-4 py-3">
            <h2 className="font-semibold text-slate-950">Field palette</h2>
          </div>
          <div className="grid gap-2 p-3">
            {fieldPalette.map((item) => (
              <button
                className="rounded-lg border border-slate-200 bg-white px-3 py-2 text-left transition hover:border-sky-300 hover:bg-sky-50"
                data-testid={`field-palette-${item.type}`}
                draggable
                key={item.type}
                onClick={() => insertField(item.type)}
                onDragStart={(event) =>
                  handlePaletteDragStart(event, item.type)
                }
                type="button"
              >
                <span className="block text-sm font-medium text-slate-950">
                  {item.label}
                </span>
                <span className="mt-0.5 block text-xs text-slate-500">
                  {item.description}
                </span>
              </button>
            ))}
          </div>
        </div>

        <div className="overflow-hidden rounded-lg border border-slate-200 bg-white shadow-sm">
          <div className="flex flex-wrap items-center justify-between gap-2 border-b border-slate-100 px-4 py-3">
            <div>
              <h2 className="font-semibold text-slate-950">Canvas</h2>
              <p className="mt-1 text-sm text-slate-500">
                Drag fields here and reorder them by dragging rows.
              </p>
            </div>
            <Button
              onClick={() => insertField('TEXT')}
              type="button"
              variant="outline"
            >
              <Plus className="h-4 w-4" />
              Field
            </Button>
          </div>
          <div
            className={cn(
              'min-h-64',
              fields.length === 0 &&
                'flex items-center justify-center bg-slate-50 p-6 text-sm text-slate-500',
            )}
            data-testid="form-designer-canvas"
            onDragOver={(event) => event.preventDefault()}
            onDrop={(event) => handleCanvasDrop(event)}
          >
            {fields.length === 0 ? (
              <span>Drop a field from the palette.</span>
            ) : (
              fields.map((field, index) => (
                <FieldDesignerRow
                  field={field}
                  index={index}
                  key={`${field.fieldCode}-${index}`}
                  onDragStart={handleFieldDragStart}
                  onDrop={handleCanvasDrop}
                  onRemove={() => removeField(index)}
                  onSelect={() => setSelectedFieldIndex(index)}
                  selected={index === selectedFieldIndex}
                />
              ))
            )}
            {fields.length > 0 ? (
              <div
                className="border-t border-dashed border-slate-200 px-4 py-3 text-center text-xs text-slate-400"
                onDragOver={(event) => event.preventDefault()}
                onDrop={(event) => handleCanvasDrop(event, fields.length)}
              >
                Drop here to move to the end
              </div>
            ) : null}
          </div>
        </div>

        <div className="space-y-4">
          <section className="rounded-lg border border-slate-200 bg-white shadow-sm">
            <div className="flex items-center gap-2 border-b border-slate-100 px-4 py-3">
              <Settings2 className="h-4 w-4 text-slate-500" />
              <h2 className="font-semibold text-slate-950">Properties</h2>
            </div>
            {selectedField ? (
              <div className="space-y-3 p-4">
                <label className="block">
                  <span className="mb-1 block text-xs font-medium text-slate-500">
                    Field code
                  </span>
                  <Input
                    onChange={(event) =>
                      updateSelectedField({
                        fieldCode: normalizeFieldCode(event.target.value),
                      })
                    }
                    value={selectedField.fieldCode}
                  />
                </label>
                <label className="block">
                  <span className="mb-1 block text-xs font-medium text-slate-500">
                    Field name
                  </span>
                  <Input
                    onChange={(event) =>
                      updateSelectedField({ fieldName: event.target.value })
                    }
                    value={selectedField.fieldName}
                  />
                </label>
                <label className="block">
                  <span className="mb-1 block text-xs font-medium text-slate-500">
                    Type
                  </span>
                  <select
                    className="h-10 w-full rounded-xl border border-slate-200 bg-white px-3 text-sm outline-none focus:border-sky-400 focus:ring-2 focus:ring-sky-100"
                    onChange={(event) =>
                      updateSelectedField({
                        fieldType: event.target.value as FormFieldType,
                      })
                    }
                    value={selectedField.fieldType}
                  >
                    {fieldPalette.map((item) => (
                      <option key={item.type} value={item.type}>
                        {item.label}
                      </option>
                    ))}
                  </select>
                </label>
                <div className="grid gap-2 sm:grid-cols-2">
                  <ToggleField
                    checked={selectedField.required ?? false}
                    label="Required"
                    onChange={(checked) =>
                      updateSelectedField({ required: checked })
                    }
                  />
                  <ToggleField
                    checked={selectedField.visible ?? true}
                    label="Visible"
                    onChange={(checked) =>
                      updateSelectedField({ visible: checked })
                    }
                  />
                  <ToggleField
                    checked={selectedField.editable ?? true}
                    label="Editable"
                    onChange={(checked) =>
                      updateSelectedField({ editable: checked })
                    }
                  />
                  <ToggleField
                    checked={selectedField.multiValue ?? false}
                    label="Multi value"
                    onChange={(checked) =>
                      updateSelectedField({ multiValue: checked })
                    }
                  />
                </div>
                <label className="block">
                  <span className="mb-1 block text-xs font-medium text-slate-500">
                    Dictionary code
                  </span>
                  <Input
                    onChange={(event) =>
                      updateSelectedField({
                        dictionaryCode: event.target.value || undefined,
                      })
                    }
                    value={selectedField.dictionaryCode ?? ''}
                  />
                </label>
                <div className="grid gap-2 sm:grid-cols-3">
                  <label className="block">
                    <span className="mb-1 block text-xs font-medium text-slate-500">
                      Max length
                    </span>
                    <Input
                      onChange={(event) =>
                        updateSelectedField({
                          maxLength: numericValue(event.target.value),
                        })
                      }
                      type="number"
                      value={selectedField.maxLength ?? ''}
                    />
                  </label>
                  <label className="block">
                    <span className="mb-1 block text-xs font-medium text-slate-500">
                      Min
                    </span>
                    <Input
                      onChange={(event) =>
                        updateSelectedField({
                          min: numericValue(event.target.value),
                        })
                      }
                      type="number"
                      value={selectedField.min ?? ''}
                    />
                  </label>
                  <label className="block">
                    <span className="mb-1 block text-xs font-medium text-slate-500">
                      Max
                    </span>
                    <Input
                      onChange={(event) =>
                        updateSelectedField({
                          max: numericValue(event.target.value),
                        })
                      }
                      type="number"
                      value={selectedField.max ?? ''}
                    />
                  </label>
                </div>
                <label className="block">
                  <span className="mb-1 block text-xs font-medium text-slate-500">
                    Pattern
                  </span>
                  <Input
                    onChange={(event) =>
                      updateSelectedField({
                        pattern: event.target.value || undefined,
                      })
                    }
                    value={selectedField.pattern ?? ''}
                  />
                </label>
              </div>
            ) : (
              <div className="p-4 text-sm text-slate-500">
                Select a field from the canvas.
              </div>
            )}
          </section>

          <section className="rounded-lg border border-slate-200 bg-white shadow-sm">
            <div className="flex items-center gap-2 border-b border-slate-100 px-4 py-3">
              <LockKeyhole className="h-4 w-4 text-slate-500" />
              <h2 className="font-semibold text-slate-950">Node permission</h2>
            </div>
            <div className="space-y-3 p-4">
              <label className="block">
                <span className="mb-1 block text-xs font-medium text-slate-500">
                  Node ID
                </span>
                <Input
                  onChange={(event) => setPermissionNodeId(event.target.value)}
                  value={permissionNodeId}
                />
              </label>
              <div className="grid gap-2 sm:grid-cols-3">
                <ToggleField
                  checked={selectedPermission.visible ?? true}
                  label="Visible"
                  onChange={(checked) =>
                    updatePermissionFlag('visible', checked)
                  }
                />
                <ToggleField
                  checked={selectedPermission.editable ?? true}
                  label="Editable"
                  onChange={(checked) =>
                    updatePermissionFlag('editable', checked)
                  }
                />
                <ToggleField
                  checked={selectedPermission.required ?? false}
                  label="Required"
                  onChange={(checked) =>
                    updatePermissionFlag('required', checked)
                  }
                />
              </div>
            </div>
          </section>
        </div>
      </section>

      <section className="grid gap-4 lg:grid-cols-2">
        <label className="block">
          <span className="mb-2 flex items-center justify-between gap-2 text-sm font-medium text-slate-700">
            <span className="inline-flex items-center gap-2">
              <LayoutGrid className="h-4 w-4" />
              Layout JSON
            </span>
            <Button onClick={syncLayoutFromCanvas} size="sm" variant="outline">
              Sync
            </Button>
          </span>
          <textarea
            className="min-h-40 w-full rounded-lg border border-slate-200 px-3 py-2 font-mono text-xs outline-none focus:border-sky-400 focus:ring-2 focus:ring-sky-100"
            onChange={(event) => setLayoutText(event.target.value)}
            value={layoutText}
          />
        </label>
        <label className="block">
          <span className="mb-2 block text-sm font-medium text-slate-700">
            Permission JSON
          </span>
          <textarea
            className="min-h-40 w-full rounded-lg border border-slate-200 px-3 py-2 font-mono text-xs outline-none focus:border-sky-400 focus:ring-2 focus:ring-sky-100"
            onChange={(event) => setPermissionText(event.target.value)}
            value={permissionText}
          />
        </label>
      </section>

      {lastMetadata ? (
        <section className="rounded-lg border border-slate-200 bg-white p-4 shadow-sm">
          <div className="flex flex-wrap items-center gap-2">
            <span className="font-semibold text-slate-950">
              {lastMetadata.name}
            </span>
            <Badge>{lastMetadata.status}</Badge>
            <span className="text-sm text-slate-500">
              {lastMetadata.id} v{lastMetadata.version}
            </span>
          </div>
        </section>
      ) : null}
      {createMutation.error || publishMutation.error ? (
        <p className="rounded-lg border border-rose-200 bg-rose-50 px-3 py-2 text-sm text-rose-700">
          {String(createMutation.error ?? publishMutation.error)}
        </p>
      ) : null}
    </div>
  )
}
