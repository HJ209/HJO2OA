import { useEffect, useState, type FormEvent, type ReactElement } from 'react'
import { X } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { useSystemEnumOptions } from '@/features/infra-admin/hooks/use-dictionary'
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
  status: 'ACTIVE',
}

const ACCOUNT_STATUS_ENUM_CLASS =
  'com.hjo2oa.org.person.account.domain.AccountStatus'
const ACCOUNT_STATUS_FALLBACK_OPTIONS = [
  { value: 'ACTIVE', label: '启用' },
  { value: 'LOCKED', label: '锁定' },
  { value: 'DISABLED', label: '停用' },
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
  const accountStatusOptionsQuery = useSystemEnumOptions(ACCOUNT_STATUS_ENUM_CLASS)
  const accountStatusOptions =
    accountStatusOptionsQuery.data && accountStatusOptionsQuery.data.length > 0
      ? accountStatusOptionsQuery.data
      : ACCOUNT_STATUS_FALLBACK_OPTIONS

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
        className="w-full max-w-xl rounded-2xl bg-white p-6 shadow-xl"
        onSubmit={handleSubmit}
      >
        <div className="flex items-start justify-between gap-4">
          <div>
            <h2 className="text-lg font-semibold text-slate-950">
              {person ? '编辑人员账号' : '新增人员账号'}
            </h2>
            <p className="mt-1 text-sm text-slate-500">
              维护账号基础资料，提交时由请求层写入幂等键。
            </p>
          </div>
          <Button
            aria-label="关闭弹窗"
            onClick={onClose}
            size="icon"
            variant="ghost"
          >
            <X className="h-4 w-4" />
          </Button>
        </div>

        <div className="mt-6 grid gap-4 md:grid-cols-2">
          <label className="space-y-2 text-sm font-medium text-slate-700">
            登录账号
            <Input
              onChange={(event) =>
                updateField('accountName', event.target.value)
              }
              required
              value={formValue.accountName}
            />
          </label>
          <label className="space-y-2 text-sm font-medium text-slate-700">
            显示名称
            <Input
              onChange={(event) =>
                updateField('displayName', event.target.value)
              }
              required
              value={formValue.displayName}
            />
          </label>
          <label className="space-y-2 text-sm font-medium text-slate-700">
            邮箱
            <Input
              onChange={(event) => updateField('email', event.target.value)}
              type="email"
              value={formValue.email}
            />
          </label>
          <label className="space-y-2 text-sm font-medium text-slate-700">
            手机
            <Input
              onChange={(event) => updateField('mobile', event.target.value)}
              value={formValue.mobile}
            />
          </label>
          <label className="space-y-2 text-sm font-medium text-slate-700">
            组织 ID
            <Input
              onChange={(event) => updateField('orgId', event.target.value)}
              value={formValue.orgId}
            />
          </label>
          <label className="space-y-2 text-sm font-medium text-slate-700">
            状态
            <select
              className="h-10 w-full rounded-xl border border-slate-200 bg-white px-3 text-sm text-slate-900 shadow-sm focus:border-sky-400 focus:outline-none focus:ring-2 focus:ring-sky-100"
              onChange={(event) =>
                updateField('status', event.target.value as AccountStatus)
              }
              value={formValue.status}
            >
              {accountStatusOptions.map((option) => (
                <option key={option.value} value={option.value}>
                  {option.label}
                </option>
              ))}
            </select>
          </label>
        </div>

        <div className="mt-6 flex justify-end gap-3">
          <Button onClick={onClose} variant="outline">
            取消
          </Button>
          <Button disabled={submitting} type="submit">
            {submitting ? '提交中' : '保存'}
          </Button>
        </div>
      </form>
    </div>
  )
}
