import { useEffect, useState, type FormEvent, type ReactElement } from 'react'
import { X } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import type {
  AccountStatus,
  PersonAccount,
  PersonAccountPayload,
} from '@/features/org-perm/types/org-perm'

const DEFAULT_FORM: PersonAccountPayload = {
  accountName: '',
  displayName: '',
  email: '',
  mobile: '',
  orgId: '',
  departmentId: '',
  status: 'ACTIVE',
}

const ACCOUNT_STATUS_OPTIONS: Array<{ value: AccountStatus; label: string }> = [
  { value: 'ACTIVE', label: 'Active' },
  { value: 'LOCKED', label: 'Locked' },
  { value: 'DISABLED', label: 'Disabled' },
]

export interface PersonFormDialogProps {
  open: boolean
  person?: PersonAccount
  submitting?: boolean
  onClose: () => void
  onSubmit: (payload: PersonAccountPayload) => void
}

function toFormValue(person?: PersonAccount): PersonAccountPayload {
  if (!person) {
    return DEFAULT_FORM
  }

  return {
    accountName: person.accountName,
    displayName: person.displayName,
    email: person.email ?? '',
    mobile: person.mobile ?? '',
    orgId: person.orgId ?? '',
    departmentId: person.departmentId ?? '',
    status: person.status,
  }
}

export function PersonFormDialog({
  open,
  person,
  submitting = false,
  onClose,
  onSubmit,
}: PersonFormDialogProps): ReactElement | null {
  const [formValue, setFormValue] = useState<PersonAccountPayload>(() =>
    toFormValue(person),
  )

  useEffect(() => {
    if (open) {
      setFormValue(toFormValue(person))
    }
  }, [open, person])

  if (!open) {
    return null
  }

  function updateField<Key extends keyof PersonAccountPayload>(
    key: Key,
    value: PersonAccountPayload[Key],
  ): void {
    setFormValue((current) => ({
      ...current,
      [key]: value,
    }))
  }

  function handleSubmit(event: FormEvent<HTMLFormElement>): void {
    event.preventDefault()
    onSubmit(formValue)
  }

  return (
    <div
      aria-modal="true"
      className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/40 p-4"
      role="dialog"
    >
      <form
        className="w-full max-w-xl rounded-lg bg-white p-6 shadow-xl"
        onSubmit={handleSubmit}
      >
        <div className="flex items-start justify-between gap-4">
          <div>
            <h2 className="text-lg font-semibold text-slate-950">
              {person ? 'Edit person' : 'Create person'}
            </h2>
          </div>
          <Button
            aria-label="Close dialog"
            onClick={onClose}
            size="icon"
            variant="ghost"
          >
            <X className="h-4 w-4" />
          </Button>
        </div>

        <div className="mt-6 grid gap-4 md:grid-cols-2">
          <label className="space-y-2 text-sm font-medium text-slate-700">
            Login account
            <Input
              onChange={(event) =>
                updateField('accountName', event.target.value)
              }
              required
              value={formValue.accountName}
            />
          </label>
          <label className="space-y-2 text-sm font-medium text-slate-700">
            Display name
            <Input
              onChange={(event) =>
                updateField('displayName', event.target.value)
              }
              required
              value={formValue.displayName}
            />
          </label>
          <label className="space-y-2 text-sm font-medium text-slate-700">
            Email
            <Input
              onChange={(event) => updateField('email', event.target.value)}
              type="email"
              value={formValue.email}
            />
          </label>
          <label className="space-y-2 text-sm font-medium text-slate-700">
            Mobile
            <Input
              onChange={(event) => updateField('mobile', event.target.value)}
              value={formValue.mobile}
            />
          </label>
          <label className="space-y-2 text-sm font-medium text-slate-700">
            Organization ID
            <Input
              onChange={(event) => updateField('orgId', event.target.value)}
              required
              value={formValue.orgId}
            />
          </label>
          <label className="space-y-2 text-sm font-medium text-slate-700">
            Department ID
            <Input
              onChange={(event) =>
                updateField('departmentId', event.target.value)
              }
              value={formValue.departmentId}
            />
          </label>
          <label className="space-y-2 text-sm font-medium text-slate-700">
            Status
            <select
              className="h-10 w-full rounded-lg border border-slate-200 bg-white px-3 text-sm text-slate-900 shadow-sm focus:border-sky-400 focus:outline-none focus:ring-2 focus:ring-sky-100"
              onChange={(event) =>
                updateField('status', event.target.value as AccountStatus)
              }
              value={formValue.status}
            >
              {ACCOUNT_STATUS_OPTIONS.map((option) => (
                <option key={option.value} value={option.value}>
                  {option.label}
                </option>
              ))}
            </select>
          </label>
        </div>

        <div className="mt-6 flex justify-end gap-3">
          <Button onClick={onClose} variant="outline">
            Cancel
          </Button>
          <Button disabled={submitting} type="submit">
            Save
          </Button>
        </div>
      </form>
    </div>
  )
}
