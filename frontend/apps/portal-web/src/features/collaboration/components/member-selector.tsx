import { useMemo, useState, type KeyboardEvent, type ReactElement } from 'react'
import { Plus, UsersRound, X } from 'lucide-react'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { usePersonList } from '@/features/org-perm/hooks/use-person-list'
import type { WorkspaceMember } from '@/features/collaboration/types/collaboration'
import { cn } from '@/utils/cn'

interface MemberCandidate {
  personId: string
  label: string
  hint?: string
}

export interface MemberSelectorProps {
  selectedIds: string[]
  workspaceMembers?: WorkspaceMember[]
  onChange: (nextIds: string[]) => void
  label: string
  placeholder?: string
  disabled?: boolean
}

function normalizeIds(ids: string[]): string[] {
  return Array.from(
    new Set(ids.map((id) => id.trim()).filter((id) => id.length > 0)),
  )
}

export function MemberSelector({
  selectedIds,
  workspaceMembers = [],
  onChange,
  label,
  placeholder = '输入人员 ID',
  disabled = false,
}: MemberSelectorProps): ReactElement {
  const [draftId, setDraftId] = useState('')
  const peopleQuery = usePersonList({ page: 1, size: 50 })
  const selectedSet = useMemo(() => new Set(selectedIds), [selectedIds])

  const candidates = useMemo(() => {
    const merged = new Map<string, MemberCandidate>()

    for (const person of peopleQuery.data?.items ?? []) {
      merged.set(person.id, {
        personId: person.id,
        label: person.displayName,
        hint: person.employeeNo ?? person.accountName,
      })
    }

    for (const member of workspaceMembers) {
      if (!merged.has(member.personId)) {
        merged.set(member.personId, {
          personId: member.personId,
          label: member.personId,
          hint: member.roleCode,
        })
      }
    }

    for (const personId of selectedIds) {
      if (!merged.has(personId)) {
        merged.set(personId, {
          personId,
          label: personId,
          hint: '已选',
        })
      }
    }

    return Array.from(merged.values())
  }, [peopleQuery.data?.items, selectedIds, workspaceMembers])

  function addPerson(personId: string): void {
    if (disabled) {
      return
    }

    const nextIds = normalizeIds([...selectedIds, personId])
    onChange(nextIds)
    setDraftId('')
  }

  function removePerson(personId: string): void {
    if (disabled) {
      return
    }

    onChange(selectedIds.filter((id) => id !== personId))
  }

  function handleDraftKeyDown(event: KeyboardEvent<HTMLInputElement>): void {
    if (event.key === 'Enter') {
      event.preventDefault()
      addPerson(draftId)
    }
  }

  return (
    <div className="space-y-3">
      <div className="flex items-center gap-2 text-sm font-semibold text-slate-900">
        <UsersRound className="h-4 w-4 text-sky-600" />
        {label}
      </div>

      <div className="flex gap-2">
        <Input
          disabled={disabled}
          onChange={(event) => setDraftId(event.target.value)}
          onKeyDown={handleDraftKeyDown}
          placeholder={placeholder}
          value={draftId}
        />
        <Button
          aria-label="添加成员"
          disabled={disabled || !draftId.trim()}
          onClick={() => addPerson(draftId)}
          size="icon"
          title="添加成员"
          type="button"
          variant="outline"
        >
          <Plus className="h-4 w-4" />
        </Button>
      </div>

      {selectedIds.length ? (
        <div className="flex flex-wrap gap-2">
          {selectedIds.map((personId) => (
            <Badge className="gap-1 pr-1" key={personId} variant="secondary">
              <span>{personId}</span>
              <button
                aria-label={`移除 ${personId}`}
                className="rounded-full p-0.5 text-slate-500 hover:bg-white hover:text-slate-900"
                disabled={disabled}
                onClick={() => removePerson(personId)}
                title="移除成员"
                type="button"
              >
                <X className="h-3 w-3" />
              </button>
            </Badge>
          ))}
        </div>
      ) : null}

      <div className="grid max-h-48 gap-2 overflow-y-auto rounded-xl border border-slate-200 bg-slate-50 p-2 sm:grid-cols-2">
        {candidates.length ? (
          candidates.map((candidate) => {
            const selected = selectedSet.has(candidate.personId)

            return (
              <button
                className={cn(
                  'min-w-0 rounded-lg border px-3 py-2 text-left text-sm transition',
                  selected
                    ? 'border-sky-200 bg-sky-50 text-sky-700'
                    : 'border-white bg-white text-slate-700 hover:border-slate-200 hover:text-slate-950',
                )}
                disabled={disabled}
                key={candidate.personId}
                onClick={() =>
                  selected
                    ? removePerson(candidate.personId)
                    : addPerson(candidate.personId)
                }
                type="button"
              >
                <span className="block truncate font-medium">
                  {candidate.label}
                </span>
                <span className="block truncate text-xs text-slate-500">
                  {candidate.hint ?? candidate.personId}
                </span>
              </button>
            )
          })
        ) : (
          <div className="col-span-full px-2 py-4 text-sm text-slate-500">
            暂无可选人员，可直接输入人员 ID。
          </div>
        )}
      </div>
    </div>
  )
}
