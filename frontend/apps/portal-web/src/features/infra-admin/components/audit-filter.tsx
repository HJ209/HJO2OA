import { useState, type FormEvent, type ReactElement } from 'react'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import type {
  AuditAction,
  AuditFilterValues,
} from '@/features/infra-admin/types/infra'

export interface AuditFilterProps {
  initialValues?: AuditFilterValues
  onSearch: (values: AuditFilterValues) => void
}

const actions: Array<AuditAction | ''> = [
  '',
  'CREATE',
  'UPDATE',
  'DELETE',
  'REGISTER',
  'TRIGGER',
  'PAUSE',
  'RESUME',
  'DISABLE',
]

export function AuditFilter({
  initialValues = {},
  onSearch,
}: AuditFilterProps): ReactElement {
  const [values, setValues] = useState<AuditFilterValues>(initialValues)

  function handleSubmit(event: FormEvent<HTMLFormElement>): void {
    event.preventDefault()
    onSearch(values)
  }

  function reset(): void {
    setValues({})
    onSearch({})
  }

  return (
    <form className="grid gap-3 lg:grid-cols-8" onSubmit={handleSubmit}>
      <Input
        aria-label="Module"
        onChange={(event) =>
          setValues((current) => ({
            ...current,
            moduleCode: event.target.value,
          }))
        }
        placeholder="Module"
        value={values.moduleCode ?? ''}
      />
      <select
        aria-label="Action"
        className="h-10 rounded-lg border border-slate-200 bg-white px-3 text-sm"
        onChange={(event) =>
          setValues((current) => ({
            ...current,
            action: event.target.value as AuditAction | '',
          }))
        }
        value={values.action ?? ''}
      >
        {actions.map((action) => (
          <option key={action || 'all'} value={action}>
            {action || 'All actions'}
          </option>
        ))}
      </select>
      <Input
        aria-label="Operator"
        onChange={(event) =>
          setValues((current) => ({
            ...current,
            operatorAccountId: event.target.value,
          }))
        }
        placeholder="Operator UUID"
        value={values.operatorAccountId ?? ''}
      />
      <Input
        aria-label="Object type"
        onChange={(event) =>
          setValues((current) => ({
            ...current,
            objectType: event.target.value,
          }))
        }
        placeholder="Object type"
        value={values.objectType ?? ''}
      />
      <Input
        aria-label="Object ID"
        onChange={(event) =>
          setValues((current) => ({ ...current, objectId: event.target.value }))
        }
        placeholder="Object ID"
        value={values.objectId ?? ''}
      />
      <Input
        aria-label="Request ID"
        onChange={(event) =>
          setValues((current) => ({
            ...current,
            requestId: event.target.value,
          }))
        }
        placeholder="Request ID"
        value={values.requestId ?? ''}
      />
      <Input
        aria-label="From"
        onChange={(event) =>
          setValues((current) => ({ ...current, from: event.target.value }))
        }
        type="datetime-local"
        value={values.from ?? ''}
      />
      <Input
        aria-label="To"
        onChange={(event) =>
          setValues((current) => ({ ...current, to: event.target.value }))
        }
        type="datetime-local"
        value={values.to ?? ''}
      />
      <div className="flex gap-2 lg:col-span-8">
        <Button className="min-w-24" type="submit">
          Search
        </Button>
        <Button className="min-w-24" onClick={reset} variant="outline">
          Reset
        </Button>
      </div>
    </form>
  )
}
