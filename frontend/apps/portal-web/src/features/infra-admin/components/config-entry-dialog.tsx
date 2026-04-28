import { useEffect, useState, type FormEvent, type ReactElement } from 'react'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { INFRA_COPY } from '@/features/infra-admin/infra-copy'
import type { ConfigEntry } from '@/features/infra-admin/types/infra'

export interface ConfigEntryDialogProps {
  open: boolean
  entry?: ConfigEntry
  isSubmitting?: boolean
  onClose: () => void
  onSubmit: (entry: ConfigEntry) => void
}

const emptyEntry: ConfigEntry = {
  key: '',
  value: '',
  group: '',
  encrypted: false,
}

export function ConfigEntryDialog({
  open,
  entry,
  isSubmitting = false,
  onClose,
  onSubmit,
}: ConfigEntryDialogProps): ReactElement | null {
  const [draft, setDraft] = useState<ConfigEntry>(entry ?? emptyEntry)

  useEffect(() => {
    setDraft(entry ?? emptyEntry)
  }, [entry, open])

  if (!open) {
    return null
  }

  function handleSubmit(event: FormEvent<HTMLFormElement>): void {
    event.preventDefault()
    onSubmit(draft)
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
        <h2 className="text-lg font-semibold text-slate-950">配置项编辑</h2>
        <div className="mt-5 grid gap-4">
          <label className="grid gap-2 text-sm font-medium text-slate-700">
            键
            <Input
              onChange={(event) =>
                setDraft((current) => ({ ...current, key: event.target.value }))
              }
              required
              value={draft.key}
            />
          </label>
          <label className="grid gap-2 text-sm font-medium text-slate-700">
            分组
            <Input
              onChange={(event) =>
                setDraft((current) => ({
                  ...current,
                  group: event.target.value,
                }))
              }
              required
              value={draft.group}
            />
          </label>
          <label className="grid gap-2 text-sm font-medium text-slate-700">
            值
            <Input
              onChange={(event) =>
                setDraft((current) => ({
                  ...current,
                  value: event.target.value,
                }))
              }
              required
              value={draft.value}
            />
          </label>
          <label className="flex items-center gap-2 text-sm text-slate-700">
            <input
              checked={draft.encrypted}
              onChange={(event) =>
                setDraft((current) => ({
                  ...current,
                  encrypted: event.target.checked,
                }))
              }
              type="checkbox"
            />
            敏感配置
          </label>
        </div>
        <div className="mt-6 flex justify-end gap-3">
          <Button onClick={onClose} variant="outline">
            {INFRA_COPY.action.cancel}
          </Button>
          <Button disabled={isSubmitting} type="submit">
            {INFRA_COPY.action.save}
          </Button>
        </div>
      </form>
    </div>
  )
}
