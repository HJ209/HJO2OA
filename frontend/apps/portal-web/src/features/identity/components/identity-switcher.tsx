import { useState, type ReactElement } from 'react'
import { Check, ChevronDown, Loader2 } from 'lucide-react'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { useIdentityContext } from '@/features/identity/hooks/use-identity-context'
import { useAuth } from '@/hooks/use-auth'
import { cn } from '@/utils/cn'

const COPY = {
  triggerKey: 'identity.switcher.trigger',
  triggerText: '切换身份',
  emptyKey: 'identity.switcher.empty',
  emptyText: '身份未绑定',
  loadingKey: 'identity.switcher.loading',
  loadingText: '身份切换中',
  userFallbackKey: 'identity.switcher.userFallback',
  userFallbackText: '未登录用户',
  todoKey: 'identity.switcher.todo',
  todoText: '待办',
  messageKey: 'identity.switcher.message',
  messageText: '消息',
} as const

export function IdentitySwitcher(): ReactElement {
  const [isOpen, setIsOpen] = useState(false)
  const { user } = useAuth()
  const { context, isSwitching, errorMessage, switchContext } =
    useIdentityContext()
  const currentAssignment = context.currentAssignment

  async function handleSwitch(assignmentId: string): Promise<void> {
    const isSuccess = await switchContext(assignmentId)

    if (isSuccess) {
      setIsOpen(false)
    }
  }

  return (
    <div className="relative">
      <Button
        aria-expanded={isOpen}
        aria-haspopup="menu"
        className="h-auto min-w-[260px] justify-between gap-3 rounded-xl border-slate-200 bg-white px-3 py-2 shadow-sm"
        onClick={() => setIsOpen((previousState) => !previousState)}
        variant="outline"
      >
        <span className="min-w-0 text-left">
          <span className="block truncate text-sm font-medium text-slate-900">
            {currentAssignment?.positionName ?? COPY.triggerText}
          </span>
          <span className="block truncate text-xs text-slate-500">
            {user?.displayName ?? COPY.userFallbackText}
          </span>
        </span>
        <span className="flex shrink-0 items-center gap-2">
          <Badge variant="secondary">
            {currentAssignment?.orgName ?? COPY.emptyText}
          </Badge>
          <ChevronDown className="h-4 w-4 text-slate-400" />
        </span>
      </Button>

      {isOpen ? (
        <div
          className="absolute right-0 z-30 mt-2 w-[320px] rounded-xl border border-slate-200 bg-white p-2 shadow-lg"
          role="menu"
        >
          <div className="border-b border-slate-100 px-3 py-2 text-xs text-slate-500">
            {COPY.todoText} {context.pendingTodoCount} · {COPY.messageText}{' '}
            {context.unreadMessageCount}
          </div>

          {context.assignments.length > 0 ? (
            <div className="mt-2 space-y-1">
              {context.assignments.map((assignment) => {
                const isActive =
                  currentAssignment?.assignmentId === assignment.assignmentId

                return (
                  <button
                    className={cn(
                      'flex w-full items-center justify-between rounded-xl px-3 py-2 text-left text-sm transition hover:bg-slate-50 disabled:cursor-wait disabled:opacity-70',
                      isActive ? 'bg-sky-50 text-sky-700' : 'text-slate-700',
                    )}
                    disabled={isSwitching}
                    key={assignment.assignmentId}
                    onClick={() => void handleSwitch(assignment.assignmentId)}
                    role="menuitem"
                    type="button"
                  >
                    <span className="min-w-0">
                      <span className="block truncate font-medium">
                        {assignment.positionName}
                      </span>
                      <span className="block truncate text-xs text-slate-500">
                        {assignment.orgName}
                      </span>
                    </span>
                    {isActive ? <Check className="h-4 w-4" /> : null}
                  </button>
                )
              })}
            </div>
          ) : (
            <div className="px-3 py-4 text-sm text-slate-500">
              {COPY.emptyText}
            </div>
          )}

          {isSwitching ? (
            <div className="mt-2 flex items-center gap-2 px-3 py-2 text-xs text-slate-500">
              <Loader2 className="h-3.5 w-3.5 animate-spin" />
              {COPY.loadingText}
            </div>
          ) : null}

          {errorMessage ? (
            <div className="mt-2 rounded-xl border border-red-200 bg-red-50 px-3 py-2 text-xs text-red-700">
              {errorMessage}
            </div>
          ) : null}
        </div>
      ) : null}
    </div>
  )
}
