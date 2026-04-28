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
  'LOGIN',
  'READ',
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
    <form className="grid gap-3 md:grid-cols-6" onSubmit={handleSubmit}>
      <Input
        aria-label="操作者"
        className="md:col-span-1"
        onChange={(event) =>
          setValues((current) => ({ ...current, actor: event.target.value }))
        }
        placeholder="操作者"
        value={values.actor ?? ''}
      />
      <select
        aria-label="操作"
        className="h-10 rounded-xl border border-slate-200 bg-white px-3 text-sm"
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
            {action || '全部操作'}
          </option>
        ))}
      </select>
      <Input
        aria-label="资源"
        onChange={(event) =>
          setValues((current) => ({ ...current, resource: event.target.value }))
        }
        placeholder="资源"
        value={values.resource ?? ''}
      />
      <Input
        aria-label="开始时间"
        onChange={(event) =>
          setValues((current) => ({ ...current, from: event.target.value }))
        }
        type="datetime-local"
        value={values.from ?? ''}
      />
      <Input
        aria-label="结束时间"
        onChange={(event) =>
          setValues((current) => ({ ...current, to: event.target.value }))
        }
        type="datetime-local"
        value={values.to ?? ''}
      />
      <div className="flex gap-2">
        <Button className="flex-1" type="submit">
          查询
        </Button>
        <Button className="flex-1" onClick={reset} variant="outline">
          重置
        </Button>
      </div>
    </form>
  )
}
